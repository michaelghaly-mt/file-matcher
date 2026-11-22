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
@Service
public class MatchingService {

    @Value("${matcher.fileAPath}")
    private String mainFile;
    @Value("${matcher.filesDirectory}")
    private String filesDirectory;
    @Value("${matcher.threads:0}")
    private int initialThreads;
    @Value("${matcher.fileExtensions:txt}")
    private List<String> fileExtensions;


    private final WordTokenizer tokenizer;
    private final SimilarityCalculator calculator;

    public MatchingService(WordTokenizer tokenizer, SimilarityCalculator calculator) {
        this.tokenizer = tokenizer;
        this.calculator = calculator;
    }

    public List<MatchResult> matchAll() throws IOException {
        Path fileA = Path.of(mainFile);
        Path poolDir = Path.of(filesDirectory);

        filesValidations(fileA, poolDir);

        Set<String> wordsOfA = tokenizer.extractWords(fileA);
        log.info("File A: {} distinct words", wordsOfA.size());

        List<Path> poolFiles = filterDirectoryFiles(poolDir);

        if (poolFiles.isEmpty()) {
            throw new IllegalArgumentException(
                    "Pool directory has no files with extensions " + fileExtensions + ": " + poolDir.toAbsolutePath());
        }

        int threads = Math.min(effectiveThreads(), poolFiles.size());
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        return asyncMatch(wordsOfA, executor, poolFiles);
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

    private boolean isTextFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return fileExtensions.stream().anyMatch(ext -> name.endsWith("." + ext.toLowerCase()));
    }

    private void filesValidations(Path fileA, Path poolDir) {
        if (!Files.isRegularFile(fileA)) {
            throw new IllegalArgumentException("File A not found: " + fileA.toAbsolutePath());
        }
        if (!isTextFile(fileA)) {
            throw new IllegalArgumentException("File A has an unsupported extension: " + fileA.getFileName());
        }
        if (!Files.isDirectory(poolDir)) {
            throw new IllegalArgumentException("Pool directory not found: " + poolDir.toAbsolutePath());
        }
    }

    private List<Path> filterDirectoryFiles(Path poolDir) {
        try (Stream<Path> stream = Files.list(poolDir)) {
            return stream.filter(Files::isRegularFile).filter(this::isTextFile).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not list files in " + poolDir, e);
        }
    }

    private List<MatchResult> asyncMatch(Set<String> wordsOfA, ExecutorService executor,
                                                             List<Path> poolFiles) {
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

}