package com.tms.edi.ai.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses and validates JSON-configured transformation rule chains.
 *
 * <p>A <em>rule chain</em> is a JSON array of {@link RuleStep} objects stored in
 * {@code mapping_lines.transformation_params} and evaluated at runtime:
 * <pre>
 * [
 *   { "rule": "TRIM" },
 *   { "rule": "UPPER" },
 *   { "rule": "REPLACE", "params": { "from": "-", "to": "" } }
 * ]
 * </pre>
 *
 * <p>A single rule object (not wrapped in an array) is also accepted and normalised
 * automatically.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleProcessor {

    private static final Set<String> KNOWN_RULES = Set.of(
            "DIRECT", "CONSTANT", "UPPER", "LOWER", "TRIM",
            "SUBSTRING", "CONCAT", "REPLACE", "DATE_FORMAT", "DATE_NOW",
            "ROUND", "MATH", "TO_NUMBER", "IF", "IF_NULL", "LOOKUP"
    );

    private final ObjectMapper objectMapper;

    // ── Inner model ───────────────────────────────────────────────────────────

    /**
     * One step in a transformation rule chain.
     */
    @Data
    @Builder
    public static class RuleStep {
        /** Rule identifier (case-insensitive match against TransformationService switch). */
        private String rule;
        /** Optional parameter map; may be null for argument-less rules like UPPER, LOWER, TRIM. */
        private Map<String, Object> params;
    }

    // ── Parse ─────────────────────────────────────────────────────────────────

    /**
     * Parses a JSON rule chain string into an ordered list of {@link RuleStep}s.
     * Returns an empty list on null / blank input or a parse failure.
     */
    public List<RuleStep> parseChain(String ruleChainJson) {
        if (ruleChainJson == null || ruleChainJson.isBlank()) return Collections.emptyList();
        try {
            String src = ruleChainJson.trim();
            if (src.startsWith("{")) src = "[" + src + "]";  // normalise single-object form
            return objectMapper.readValue(src, new TypeReference<List<RuleStep>>() {});
        } catch (Exception ex) {
            log.warn("Failed to parse rule chain [{}]: {}", ruleChainJson, ex.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Serialises a rule chain to compact JSON for database storage.
     */
    public String serializeChain(List<RuleStep> steps) {
        try {
            return objectMapper.writeValueAsString(steps);
        } catch (Exception ex) {
            log.warn("Failed to serialize rule chain: {}", ex.getMessage());
            return "[]";
        }
    }

    // ── Validate ──────────────────────────────────────────────────────────────

    /**
     * Returns true when every step in the chain uses a known rule identifier.
     */
    public boolean validateChain(List<RuleStep> steps) {
        return steps.stream()
                .allMatch(s -> s.getRule() != null && KNOWN_RULES.contains(s.getRule().toUpperCase()));
    }

    /**
     * Returns the set of all supported rule identifiers.
     */
    public Set<String> supportedRules() {
        return KNOWN_RULES;
    }
}