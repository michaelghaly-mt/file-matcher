package com.evision.matcher.service;

import com.evision.matcher.model.MatchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

@Validated
@Service
public class MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);


    @Value("${matcher.fileAPath}")
    private String mainFile;
    @Value("${matcher.filesDirectory}")
    private String filesDirectory;
    @Value("${matcher.threads:0}")
    private int initialThreads;


    private final WordTokenizer tokenizer;
    private final SimilarityCalculator calculator;

    public MatchingService(WordTokenizer tokenizer, SimilarityCalculator calculator) {
        this.tokenizer = tokenizer;
        this.calculator = calculator;
    }

    public List<MatchResult> matchAll() throws IOException {
        Path fileA = Path.of(mainFile);
        Path poolDir = Path.of(filesDirectory);

        if (!Files.isRegularFile(fileA)) {
            throw new IllegalArgumentException("File A not found: " + fileA.toAbsolutePath());
        }
        if (!Files.isDirectory(poolDir)) {
            throw new IllegalArgumentException("Pool directory not found: " + poolDir.toAbsolutePath());
        }

        // File A is read once; workers only READ this set, so sharing it is thread-safe.
        Set<String> wordsOfA = tokenizer.extractWords(fileA);
        log.info("File A: {} distinct words", wordsOfA.size());

        List<Path> poolFiles;
        try (Stream<Path> stream = Files.list(poolDir)) {
            poolFiles = stream.filter(Files::isRegularFile).sorted().toList();
        }
        if (poolFiles.isEmpty()) {
            throw new IllegalArgumentException("Pool directory has no files: " + poolDir.toAbsolutePath());
        }

        int threads = Math.min(effectiveThreads(), poolFiles.size());
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<CompletableFuture<MatchResult>> futures = new ArrayList<>();
            for (Path file : poolFiles) {
                futures.add(CompletableFuture.supplyAsync(() -> scoreFile(file, wordsOfA), executor));
            }
            return futures.stream()
                    .map(CompletableFuture::join)
                    .sorted(Comparator.comparingDouble(MatchResult::score).reversed()
                            .thenComparing(MatchResult::fileName))
                    .toList();
        } finally {
            executor.shutdown();
        }
    }

    private MatchResult scoreFile(Path file, Set<String> wordsOfA) {
        try {
            long start = System.nanoTime();
            Set<String> words = tokenizer.extractWords(file);
            double score = calculator.score(wordsOfA, words);
            log.debug("{} processed in {} ms", file.getFileName(), (System.nanoTime() - start) / 1_000_000);
            return new MatchResult(file.getFileName().toString(), score);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }
    public int effectiveThreads() {
        return initialThreads > 0 ? initialThreads : Runtime.getRuntime().availableProcessors();
    }

}
