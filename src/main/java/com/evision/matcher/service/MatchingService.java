package com.evision.matcher.service;

import com.evision.matcher.model.MatchResult;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Validated
@Service
public class MatchingService {

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

        if (!Files.isRegularFile(fileA) && isTxtFile(fileA)) {
            throw new IllegalArgumentException("File A not found: " + fileA.toAbsolutePath());
        }
        if (!Files.isDirectory(poolDir)) {
            throw new IllegalArgumentException("Pool directory not found: " + poolDir.toAbsolutePath());
        }

        Set<String> wordsOfA = tokenizer.extractWords(fileA);
        log.info("File A: {} distinct words", wordsOfA.size());

        List<Path> poolFiles;
        try (Stream<Path> stream = Files.list(poolDir)) {
            poolFiles = stream.filter(Files::isRegularFile).filter(this::isTxtFile).sorted().toList();
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
            Set<String> words = tokenizer.extractWords(file);
            double score = calculator.score(wordsOfA, words);
            return new MatchResult(file.getFileName().toString(), score);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }
    private int effectiveThreads() {
        return initialThreads > 0 ? initialThreads : Runtime.getRuntime().availableProcessors();
    }

    private boolean isTxtFile(Path path) {
        String name = path.getFileName().toString();
        return name.toLowerCase().endsWith(".txt");
    }

}
