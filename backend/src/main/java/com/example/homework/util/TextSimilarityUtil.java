package com.example.homework.util;

import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TextSimilarityUtil {

    private TextSimilarityUtil() {
    }

    public static BigDecimal jaccardSimilarity(String textA, String textB) {
        Set<String> setA = new HashSet<>(tokenize(textA));
        Set<String> setB = new HashSet<>(tokenize(textB));

        if (setA.isEmpty() && setB.isEmpty()) {
            return BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
        }
        if (setA.isEmpty() || setB.isEmpty()) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }

        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);
        if (union.isEmpty()) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(intersection.size())
            .divide(BigDecimal.valueOf(union.size()), 4, RoundingMode.HALF_UP);
    }

    public static BigDecimal fusedSimilarity(BigDecimal simhashSimilarity,
                                             BigDecimal jaccardSimilarity,
                                             BigDecimal simhashWeight,
                                             BigDecimal jaccardWeight) {
        BigDecimal safeSimhash = simhashSimilarity == null ? BigDecimal.ZERO : simhashSimilarity;
        BigDecimal safeJaccard = jaccardSimilarity == null ? BigDecimal.ZERO : jaccardSimilarity;
        BigDecimal safeSimhashWeight = simhashWeight == null ? BigDecimal.valueOf(0.70) : simhashWeight;
        BigDecimal safeJaccardWeight = jaccardWeight == null ? BigDecimal.valueOf(0.30) : jaccardWeight;

        BigDecimal weightSum = safeSimhashWeight.add(safeJaccardWeight);
        if (weightSum.compareTo(BigDecimal.ZERO) <= 0) {
            safeSimhashWeight = BigDecimal.valueOf(0.70);
            safeJaccardWeight = BigDecimal.valueOf(0.30);
            weightSum = BigDecimal.ONE;
        }

        BigDecimal normalizedSimhash = safeSimhashWeight.divide(weightSum, 6, RoundingMode.HALF_UP);
        BigDecimal normalizedJaccard = safeJaccardWeight.divide(weightSum, 6, RoundingMode.HALF_UP);

        return safeSimhash.multiply(normalizedSimhash)
            .add(safeJaccard.multiply(normalizedJaccard))
            .setScale(4, RoundingMode.HALF_UP);
    }

    public static List<String> tokenize(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }

        String normalized = text.toLowerCase()
            .replaceAll("[\\p{Punct}\\r\\n\\t]+", " ")
            .replaceAll("\\s+", " ")
            .trim();
        if (normalized.isEmpty()) {
            return List.of();
        }

        String[] parts = normalized.split(" ");
        List<String> tokens = new ArrayList<>();
        for (String p : parts) {
            if (p.length() > 1) {
                tokens.add(p);
            }
        }

        if (!tokens.isEmpty()) {
            return tokens;
        }

        List<String> biGrams = new ArrayList<>();
        for (int i = 0; i < normalized.length() - 1; i++) {
            biGrams.add(normalized.substring(i, i + 2));
        }
        return biGrams;
    }
}
