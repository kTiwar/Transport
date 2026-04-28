package com.tms.edi.ai.util;

import lombok.experimental.UtilityClass;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility for computing field-name similarity scores.
 *
 * Combines Levenshtein edit distance with Jaccard token overlap to produce a
 * robust similarity measure suitable for schema field auto-mapping.
 *
 * All methods are thread-safe and stateless.
 */
@UtilityClass
public class SimilarityUtil {

    // ── Levenshtein ───────────────────────────────────────────────────────────

    /** Classic dynamic-programming Levenshtein distance. */
    public static int levenshteinDistance(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        int m = a.length(), n = b.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i-1][j] + 1, dp[i][j-1] + 1), dp[i-1][j-1] + cost);
            }
        }
        return dp[m][n];
    }

    /** Normalised Levenshtein similarity in [0.0, 1.0]. 1.0 = identical. */
    public static double normalizedLevenshtein(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        int maxLen = Math.max(a.length(), b.length());
        return maxLen == 0 ? 1.0 : 1.0 - (double) levenshteinDistance(a, b) / maxLen;
    }

    // ── Jaccard token similarity ──────────────────────────────────────────────

    /**
     * Jaccard similarity based on word-token sets.
     * Tokens are extracted by splitting on underscores, dots, brackets and camelCase boundaries.
     */
    public static double jaccardSimilarity(String a, String b) {
        Set<String> tA = tokenize(a);
        Set<String> tB = tokenize(b);
        if (tA.isEmpty() && tB.isEmpty()) return 1.0;
        if (tA.isEmpty() || tB.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(tA);
        intersection.retainAll(tB);
        Set<String> union = new HashSet<>(tA);
        union.addAll(tB);
        return (double) intersection.size() / union.size();
    }

    // ── Combined score ────────────────────────────────────────────────────────

    /**
     * Weighted combination of Levenshtein + Jaccard with prefix/suffix bonuses.
     * Returns a value in [0.0, 1.0].
     */
    public static double combinedScore(String source, String target) {
        if (source == null || target == null) return 0.0;
        String ns = normalize(source);
        String nt = normalize(target);
        if (ns.equals(nt)) return 1.0;

        double lev     = normalizedLevenshtein(ns, nt);
        double jaccard = jaccardSimilarity(ns, nt);
        double base    = 0.40 * lev + 0.60 * jaccard;

        if (ns.startsWith(nt) || nt.startsWith(ns)) base = Math.min(1.0, base + 0.10);
        if (ns.endsWith(nt)   || nt.endsWith(ns))   base = Math.min(1.0, base + 0.05);

        return base;
    }

    // ── Normalisation ─────────────────────────────────────────────────────────

    /**
     * Normalises a field identifier to a flat lowercase snake_case string,
     * stripping path prefixes, array brackets and camelCase.
     */
    public static String normalize(String fieldName) {
        if (fieldName == null) return "";
        String s = fieldName;

        // Strip path prefix (take last segment after / or .)
        int slash = s.lastIndexOf('/');
        if (slash >= 0) s = s.substring(slash + 1);
        int dot = s.lastIndexOf('.');
        if (dot >= 0) s = s.substring(dot + 1);

        // Strip array brackets
        s = s.replaceAll("\\[\\d*]", "");

        // camelCase -> snake_case
        s = camelToSnake(s);

        // Replace non-alphanumeric with underscore
        s = s.replaceAll("[^a-z0-9]", "_");
        s = s.replaceAll("_+", "_").replaceAll("^_|_$", "");
        return s.toLowerCase(Locale.ENGLISH);
    }

    /** Splits a field name into lowercase tokens. */
    public static Set<String> tokenize(String s) {
        if (s == null || s.isBlank()) return Collections.emptySet();
        return Arrays.stream(camelToSnake(s).split("[^a-zA-Z0-9]+"))
                .map(t -> t.toLowerCase(Locale.ENGLISH))
                .filter(t -> !t.isBlank() && t.length() > 1)
                .collect(Collectors.toSet());
    }

    private static String camelToSnake(String s) {
        return s.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2");
    }
}