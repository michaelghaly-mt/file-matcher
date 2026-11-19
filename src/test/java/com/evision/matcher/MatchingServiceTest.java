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
        MatchingService matchingService = new MatchingService(new WordTokenizer(), new SimilarityCalculator());
        ReflectionTestUtils.setField(matchingService, "mainFile", fileA.toString());
        ReflectionTestUtils.setField(matchingService, "filesDirectory", pool.toString());
        ReflectionTestUtils.setField(matchingService, "initialThreads", 2);
        return matchingService;
    }

    @Test
    void ranksPoolFilesBestFirst() throws Exception {
        Path a = Files.writeString(tmp.resolve("A.txt"), "The quick brown fox jumps over the lazy dog");
        Path pool = Files.createDirectory(tmp.resolve("pool"));
        Files.writeString(pool.resolve("perfect.txt"), "The lazy dog jumps over the quick brown fox");
        Files.writeString(pool.resolve("partial.txt"), "the quick brown cat");
        Files.writeString(pool.resolve("none.txt"), "hello world");

        List<MatchResult> results = service(a, pool).matchAll();

        assertThat(results).extracting(MatchResult::fileName)
                .containsExactly("perfect.txt", "partial.txt", "none.txt");
        assertThat(results.get(0).score()).isEqualTo(100.0);
        assertThat(results.get(2).score()).isZero();
    }

    @Test
    void failsClearlyWhenFileAIsMissing() throws Exception {
        Path pool = Files.createDirectory(tmp.resolve("pool"));
        assertThatThrownBy(() -> service(tmp.resolve("missing.txt"), pool).matchAll())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File A not found");
    }

    @Test
    void failsClearlyWhenPoolIsEmpty() throws Exception {
        Path a = Files.writeString(tmp.resolve("A.txt"), "hello");
        Path pool = Files.createDirectory(tmp.resolve("pool"));
        assertThatThrownBy(() -> service(a, pool).matchAll())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no files");
    }
}
