package com.evision.matcher;

import com.evision.matcher.service.SimilarityCalculator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SimilarityCalculatorTest {

    private final SimilarityCalculator calculator = new SimilarityCalculator();

    @Test
    void sameWordsAnyOrderIsPerfectMatch() {
        Set<String> a = Set.of("the", "quick", "brown", "fox", "jumps", "over", "lazy", "dog");
        Set<String> b = Set.of("dog", "lazy", "the", "over", "jumps", "fox", "brown", "quick");
        assertThat(calculator.score(a, b)).isEqualTo(100.0);
    }

    @Test
    void noSharedWordsIsZero() {
        assertThat(calculator.score(Set.of("a", "b"), Set.of("c", "d"))).isZero();
    }

    @Test
    void extraWordMeansLessThan100() {
        assertThat(calculator.score(Set.of("a", "b"), Set.of("a", "b", "c"))).isLessThan(100.0);
    }

    @Test
    void missingWordMeansLessThan100() {
        assertThat(calculator.score(Set.of("a", "b", "c"), Set.of("a", "b"))).isLessThan(100.0);
    }

    @Test
    void partialOverlapExample() {
        // shared = 3 (the, quick, brown), union = 5 -> 60%
        Set<String> a = Set.of("the", "quick", "brown", "fox");
        Set<String> b = Set.of("the", "quick", "brown", "dog");
        assertThat(calculator.score(a, b)).isCloseTo(60.0, within(0.0001));
    }

    @Test
    void emptySetGivesZero() {
        assertThat(calculator.score(Set.of(), Set.of("a"))).isZero();
        assertThat(calculator.score(Set.of(), Set.of())).isZero();
    }
}
