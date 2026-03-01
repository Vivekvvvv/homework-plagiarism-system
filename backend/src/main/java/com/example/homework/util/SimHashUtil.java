package com.example.homework.util;

import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public final class SimHashUtil {

    private SimHashUtil() {
    }

    public static long simHash64(String text) {
        List<String> tokens = tokenize(text);
        if (tokens.isEmpty()) {
            return 0L;
        }

        int[] bits = new int[64];
        for (String token : tokens) {
            long h = tokenHash64(token);
            for (int i = 0; i < 64; i++) {
                if (((h >>> i) & 1L) == 1L) {
                    bits[i] += 1;
                } else {
                    bits[i] -= 1;
                }
            }
        }

        long result = 0L;
        for (int i = 0; i < 64; i++) {
            if (bits[i] >= 0) {
                result |= (1L << i);
            }
        }
        return result;
    }

    public static int hammingDistance(long a, long b) {
        return Long.bitCount(a ^ b);
    }

    public static double similarityByHamming(int distance) {
        return 1.0d - ((double) distance / 64.0d);
    }

    private static List<String> tokenize(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }

        String normalized = text.toLowerCase()
            .replaceAll("[\\p{Punct}\\r\\n\\t]+", " ")
            .replaceAll("\\s+", " ")
            .trim();

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

    private static long tokenHash64(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            long value = 0L;
            for (int i = 0; i < 8; i++) {
                value = (value << 8) | (bytes[i] & 0xffL);
            }
            return value;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
