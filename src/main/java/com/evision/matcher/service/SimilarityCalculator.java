package com.evision.matcher.service;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SimilarityCalculator {

    public double score(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        // iterate over the smaller set, look up in the bigger one (O(1) per lookup)
        Set<String> small = a.size() <= b.size() ? a : b;
        Set<String> large = small == a ? b : a;

        int intersection = 0;
        for (String word : small) {
            if (large.contains(word)) {
                intersection++;
            }
        }
        int union = a.size() + b.size() - intersection;
        return 100.0 * intersection / union;
    }
}
