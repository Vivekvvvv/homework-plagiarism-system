package com.example.homework.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class TextSimilarityUtilTest {

    @Test
    void jaccardShouldBeHighForSimilarText() {
        BigDecimal score = TextSimilarityUtil.jaccardSimilarity(
            "system architecture design focuses on modularity and maintainability",
            "system architecture design emphasizes modularity and maintainability"
        );
        Assertions.assertTrue(score.compareTo(BigDecimal.valueOf(0.30)) > 0);
    }

    @Test
    void fusedShouldRespectWeightNormalization() {
        BigDecimal fused = TextSimilarityUtil.fusedSimilarity(
            BigDecimal.valueOf(0.8),
            BigDecimal.valueOf(0.2),
            BigDecimal.valueOf(7),
            BigDecimal.valueOf(3)
        );
        Assertions.assertEquals(new BigDecimal("0.6200"), fused);
    }
}
