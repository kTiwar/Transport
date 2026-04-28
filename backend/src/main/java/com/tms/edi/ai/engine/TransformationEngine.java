package com.tms.edi.ai.engine;

import com.tms.edi.ai.processor.RuleProcessor;
import com.tms.edi.service.TransformationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Advanced Transformation Engine that delegates single-rule execution to the
 * existing {@link TransformationService} and adds:
 * <ul>
 *   <li>Rule <em>chaining</em>: apply a JSON array of ordered rule steps</li>
 *   <li>Transform <em>inference</em>: suggest the best rule from field name + sample value</li>
 *   <li>Graceful fallback: returns the raw value on rule failure (no exception propagation)</li>
 * </ul>
 *
 * <p>Plugin extension point: register {@link TransformPlugin} beans to support
 * custom or future ML-based transformation rules without modifying this class.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransformationEngine {

    private final TransformationService transformationService;
    private final RuleProcessor         ruleProcessor;

    /** SPI for plugging in custom or ML-based transformation rules. */
    public interface TransformPlugin {
        boolean canHandle(String rule);
        Object apply(String rule, Object value, Map<String, Object> params);
    }

    // ── Single rule ───────────────────────────────────────────────────────────

    /**
     * Applies a single transformation rule.
     *
     * @param rule   Rule identifier (UPPER, DATE_FORMAT, CONCAT, …)
     * @param value  Raw source value
     * @param params Optional rule parameters
     * @return Transformed value; original value on failure
     */
    public Object applyRule(String rule, Object value, Map<String, Object> params) {
        if (rule == null || rule.isBlank()) return value;
        log.debug("applyRule rule={} value=[{}]", rule, value);
        try {
            return transformationService.apply(rule, value, params);
        } catch (Exception ex) {
            log.warn("Transformation rule {} failed on [{}]: {}", rule, value, ex.getMessage());
            return value;
        }
    }

    // ── Rule chain ────────────────────────────────────────────────────────────

    /**
     * Applies a JSON-configured rule chain sequentially to a value.
     *
     * <pre>
     * Example chain JSON:
     * [{"rule":"TRIM"},{"rule":"UPPER"},{"rule":"REPLACE","params":{"from":"-","to":""}}]
     * </pre>
     *
     * @param ruleChainJson JSON array (or single object) of rule steps
     * @param value         Raw source value
     * @return Final value after all steps; original value if chain is empty/invalid
     */
    public Object applyChain(String ruleChainJson, Object value) {
        if (ruleChainJson == null || ruleChainJson.isBlank()) return value;
        List<RuleProcessor.RuleStep> steps = ruleProcessor.parseChain(ruleChainJson);
        Object current = value;
        for (RuleProcessor.RuleStep step : steps) {
            current = applyRule(step.getRule(), current, step.getParams());
        }
        return current;
    }

    // ── Transform inference ───────────────────────────────────────────────────

    /**
     * Infers the most appropriate transformation rule based on the target field name
     * and a representative sample value from the source file.
     *
     * @param targetField  Canonical target field name
     * @param sampleValue  Sample value from the source document
     * @return Suggested rule identifier (never null; defaults to "DIRECT")
     */
    public String inferTransform(String targetField, String sampleValue) {
        if (sampleValue == null || sampleValue.isBlank()) return "DIRECT";
        String t = targetField.toLowerCase();
        if ((t.contains("date") || t.endsWith("_dt") || t.equals("eta") || t.equals("etd"))
                && sampleValue.matches("\\d{8}")) return "DATE_FORMAT";
        if (t.contains("country") || t.endsWith("_code") || t.contains("partner")) return "UPPER";
        if ((t.contains("weight") || t.contains("quantity") || t.contains("amount"))
                && sampleValue.contains(",")) return "TO_NUMBER";
        if (t.contains("name") || t.contains("description")) return "TRIM";
        return "DIRECT";
    }
}