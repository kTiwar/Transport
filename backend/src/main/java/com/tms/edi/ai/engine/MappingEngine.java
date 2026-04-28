package com.tms.edi.ai.engine;

import com.tms.edi.ai.model.MappingCandidate;
import com.tms.edi.ai.model.SchemaField;
import com.tms.edi.ai.util.SimilarityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Core AI Mapping Engine.
 *
 * For each canonical target field the engine scores every source field using a
 * multi-factor similarity algorithm and returns the highest-confidence candidate.
 *
 * Scoring factors (in priority order):
 *   1. Leaf-name similarity (Levenshtein + Jaccard token overlap, weighted)
 *   2. Data-type / sample-value compatibility bonus
 *   3. Historical learning boost (confidence delta from accepted past mappings)
 *   4. Array depth penalty (deeply-nested array fields penalised slightly)
 *
 * This class is stateless and thread-safe.
 *
 * Future extension: swap in an ML inference model by replacing or wrapping
 * the {@link #computeScore} method via a Spring {@code @Primary} override.
 */
@Slf4j
@Component
public class MappingEngine {

    /** Suggestions below this threshold are suppressed. */
    private static final double MIN_CONFIDENCE = 0.25;

    /**
     * Suggests the best source-to-target mappings for a set of canonical fields.
     *
     * @param sourceFields      Flattened source schema fields
     * @param canonicalTargets  List of canonical target field names
     * @param historicalBoosts  Map keyed by "sourcePath|targetField" -> confidence boost [0..0.30]
     * @param requiredTargets   Set of target field names that are marked required
     * @return One best MappingCandidate per canonical target (only those exceeding MIN_CONFIDENCE)
     */
    public List<MappingCandidate> suggest(
            List<SchemaField> sourceFields,
            List<String> canonicalTargets,
            Map<String, Double> historicalBoosts,
            Set<String> requiredTargets) {

        List<MappingCandidate> results = new ArrayList<>();

        for (String target : canonicalTargets) {
            String targetLeaf = extractLeafName(target);
            MappingCandidate best = null;
            double bestScore = -1.0;

            for (SchemaField src : sourceFields) {
                double score = computeScore(src, target, targetLeaf, historicalBoosts);
                if (score > bestScore) {
                    bestScore = score;
                    best = buildCandidate(src, target, score, historicalBoosts,
                                          requiredTargets.contains(target));
                }
            }

            if (best != null && bestScore >= MIN_CONFIDENCE) {
                best.setConfidenceScore(Math.min(1.0, bestScore));
                results.add(best);
                log.debug("Mapped {} -> {} (confidence={})",
                    best.getSourceField().getPath(), target,
                    String.format("%.3f", bestScore));
            }
        }

        results.sort(Comparator.comparingDouble(MappingCandidate::getConfidenceScore).reversed());
        return results;
    }

    // ── Score computation ─────────────────────────────────────────────────────

    private double computeScore(SchemaField src, String target, String targetLeaf,
                                Map<String, Double> historicalBoosts) {

        String normSrcName = SimilarityUtil.normalize(src.getName());
        String normLeaf    = SimilarityUtil.normalize(targetLeaf);

        // Name similarity (main signal)
        double nameSim = SimilarityUtil.combinedScore(normSrcName, normLeaf);

        // Also check last segment of the XPath
        String pathLeaf  = SimilarityUtil.normalize(lastPathSegment(src.getPath()));
        double pathSim   = SimilarityUtil.combinedScore(pathLeaf, normLeaf);

        double base = Math.max(nameSim, pathSim);

        // Data-type compatibility bonus
        base += dataTypeBonus(src, target);

        // Historical learning boost
        String histKey = src.getPath() + "|" + target;
        Double boost = historicalBoosts.getOrDefault(histKey, 0.0);
        if (boost > 0) base = Math.min(1.0, base + boost);

        // Depth penalty for very deeply nested arrays
        if (src.isArray() && src.getDepth() > 4) base -= 0.05;

        return Math.max(0.0, base);
    }

    private double dataTypeBonus(SchemaField src, String target) {
        String t  = target.toLowerCase(Locale.ENGLISH);
        String sv = src.getSampleValue();
        if (sv == null || sv.isBlank()) return 0.0;
        if ((t.contains("date") || t.endsWith("_dt") || t.equals("eta") || t.equals("etd"))
                && sv.matches("\\d{4}-\\d{2}-\\d{2}.*")) return 0.06;
        if ((t.contains("weight") || t.contains("quantity") || t.contains("volume")
                || t.contains("amount") || t.contains("price"))
                && sv.matches("[\\d.,+\\-]+")) return 0.05;
        if ((t.contains("dangerous") || t.endsWith("_flag") || t.startsWith("set_"))
                && (sv.equalsIgnoreCase("true") || sv.equalsIgnoreCase("false"))) return 0.05;
        return 0.0;
    }

    private MappingCandidate buildCandidate(SchemaField src, String target, double score,
                                             Map<String, Double> historicalBoosts, boolean required) {
        boolean fromHistory = historicalBoosts.containsKey(src.getPath() + "|" + target);
        return MappingCandidate.builder()
                .sourceField(src)
                .targetField(target)
                .confidenceScore(score)
                .reason(buildReason(src, target, score, fromHistory))
                .suggestedTransform(suggestTransform(src, target))
                .fromHistory(fromHistory)
                .required(required)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Extracts the leaf name from a canonical field path.
     * "stop_lines[].address_city"  -> "address_city"
     * "container.container_type"   -> "container_type"
     * "communication_partner"      -> "communication_partner"
     */
    public String extractLeafName(String canonicalField) {
        String s = canonicalField;
        int lastBracket = s.lastIndexOf("].");
        if (lastBracket >= 0) s = s.substring(lastBracket + 2);
        int lastDot = s.lastIndexOf('.');
        if (lastDot >= 0) s = s.substring(lastDot + 1);
        return s;
    }

    private String lastPathSegment(String path) {
        if (path == null) return "";
        int i = Math.max(path.lastIndexOf('/'), path.lastIndexOf('.'));
        return i >= 0 ? path.substring(i + 1) : path;
    }

    private String suggestTransform(SchemaField src, String target) {
        String t  = target.toLowerCase(Locale.ENGLISH);
        String sv = src.getSampleValue();
        if (sv != null && (t.contains("date") || t.endsWith("_dt"))
                && sv.matches("\\d{8}")) return "DATE_FORMAT";
        if (sv != null && (t.contains("weight") || t.contains("quantity"))
                && sv.contains(",")) return "TO_NUMBER";
        if (t.contains("country_code") || t.contains("partner")) return "UPPER";
        if (t.contains("name") || t.contains("description")) return "TRIM";
        return "DIRECT";
    }

    private String buildReason(SchemaField src, String target, double score, boolean fromHistory) {
        String leaf    = extractLeafName(target);
        double nameSim = SimilarityUtil.combinedScore(
                SimilarityUtil.normalize(src.getName()), SimilarityUtil.normalize(leaf));
        List<String> reasons = new ArrayList<>();
        if (fromHistory)       reasons.add("historical match");
        if (nameSim > 0.80)    reasons.add("high name similarity (" + String.format("%.0f%%", nameSim * 100) + ")");
        else if (nameSim > 0.50) reasons.add("partial name similarity (" + String.format("%.0f%%", nameSim * 100) + ")");
        if (src.getSampleValue() != null && !src.getSampleValue().isBlank())
                               reasons.add("sample data type match");
        if (reasons.isEmpty()) reasons.add("token overlap");
        return String.join(", ", reasons);
    }
}