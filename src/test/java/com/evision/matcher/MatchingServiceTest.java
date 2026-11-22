package com.evision.matcher;

import com.evision.matcher.model.MatchResult;
import com.evision.matcher.service.MatchingService;
import com.evision.matcher.service.SimilarityCalculator;
import com.evision.matcher.service.WordTokenizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchingServiceTest {

    @TempDir
    Path tmp;

    private MatchingService service(Path fileA, Path pool) {
        return service(fileA, pool, List.of("txt"));
    }

    private MatchingService service(Path fileA, Path pool, List<String> extensions) {
        MatchingService matchingService = new MatchingService(new WordTokenizer(), new SimilarityCalculator());
        ReflectionTestUtils.setField(matchingService, "mainFile", fileA.toString());
        ReflectionTestUtils.setField(matchingService, "filesDirectory", pool.toString());
        ReflectionTestUtils.setField(matchingService, "initialThreads", 2);
        ReflectionTestUtils.setField(matchingService, "fileExtensions", extensions);
        return matchingService;
    }

    @Test
    void ranksPoolFilesBestFirst() throws Exception {
        Path a = Files.writeString(tmp.resolve("A.txt"), "The quick brown fox jumps over the lazy dog");
        Path pool = Files.createDirectory(tmp.resolve("pool"));
        Files.writeString(pool.resolve("perfect.txt"), "The lazy dog jumps over the quick brown fox");
        Files.writeString(pool.resolve("partial.txt"), "the quick brown cat");
        Files.writeString(pool.resolve("none.txt"), "hello world");
        Files.writeString(pool.resolve("ignored.pdf"), "should be ignored");

        List<MatchResult> results = service(a, pool).matchAll();

        assertThat(results).extracting(MatchResult::fileName)
                .containsExactly("perfect.txt", "partial.txt", "none.txt");
        assertThat(results.get(0).score()).isEqualTo(100.0);
        assertThat(results.get(2).score()).isZero();
    }

    @Test
    void includesConfiguredExtensionsOnly() throws Exception {
        Path a = Files.writeString(tmp.resolve("A.md"), "hello world");
        Path pool = Files.createDirectory(tmp.resolve("pool"));
        Files.writeString(pool.resolve("match.md"), "hello world");
        Files.writeString(pool.resolve("skip.txt"), "hello world");

        List<MatchResult> results = service(a, pool, List.of("md")).matchAll();

        assertThat(results).extracting(MatchResult::fileName)
                .containsExactly("match.md");
    }

    @Test
    void failsClearlyWhenFileAIsMissing() throws Exception {
        Path pool = Files.createDirectory(tmp.resolve("pool"));
        Files.writeString(pool.resolve("any.txt"), "x");

        assertThatThrownBy(() -> service(tmp.resolve("missing.txt"), pool).matchAll())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File A not found");
    }

    @Test
    void failsWhenFileAHasUnsupportedExtension() throws Exception {
        Path a = Files.writeString(tmp.resolve("A.pdf"), "hello");
        Path pool = Files.createDirectory(tmp.resolve("pool"));
        Files.writeString(pool.resolve("any.txt"), "x");

        assertThatThrownBy(() -> service(a, pool).matchAll())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported extension");
    }

    @Test
    void failsWhenPoolDirectoryIsMissing() throws Exception {
        Path a = Files.writeString(tmp.resolve("A.txt"), "hello");

        assertThatThrownBy(() -> service(a, tmp.resolve("missing-pool")).matchAll())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pool directory not found");
    }

    @Test
    void failsClearlyWhenPoolIsEmpty() throws Exception {
        Path a = Files.writeString(tmp.resolve("A.txt"), "hello");
        Path pool = Files.createDirectory(tmp.resolve("pool"));

        assertThatThrownBy(() -> service(a, pool).matchAll())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no files");
    }

    @Test
    void failsWhenPoolHasOnlyNonMatchingExtensions() throws Exception {
        Path a = Files.writeString(tmp.resolve("A.txt"), "hello");
        Path pool = Files.createDirectory(tmp.resolve("pool"));
        Files.writeString(pool.resolve("notes.pdf"), "hello");

        assertThatThrownBy(() -> service(a, pool).matchAll())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no files");
    }
}
