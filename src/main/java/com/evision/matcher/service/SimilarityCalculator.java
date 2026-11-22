package com.evision.matcher.service;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SimilarityCalculator {

    private static final double PERCENT = 100.0;
    private static final double ZERO_SIMILARITY = 0.0;

    public double score(Set<String> fileAWords, Set<String> comparedFileWords) {
        if (fileAWords.isEmpty() || comparedFileWords.isEmpty()) {
            return ZERO_SIMILARITY;
        }

        int intersection = countSharedWords(fileAWords, comparedFileWords);
        int union = fileAWords.size() + comparedFileWords.size() - intersection;
        return PERCENT * intersection / union;
    }

    private int countSharedWords(Set<String> fileAWords, Set<String> comparedFileWords) {
        Set<String> smaller = fileAWords.size() <= comparedFileWords.size() ? fileAWords : comparedFileWords;
        Set<String> larger = smaller == fileAWords ? comparedFileWords : fileAWords;

        int shared = 0;
        for (String word : smaller) {
            if (larger.contains(word)) {
                shared++;
            }
        }
        return shared;
    }
}
