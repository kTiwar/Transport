package com.tms.edi.dto;

import lombok.Data;
import java.util.Map;

/**
 * Request body for POST /api/v1/ai-mapping/transform.
 * Applies one or more transformation rules to a raw source value.
 */
@Data
public class AiTransformRequest {
    /** Raw value from the source document */
    private String value;
    /** Transformation rule identifier (e.g. "UPPER", "DATE_FORMAT") */
    private String rule;
    /** Optional parameters for the rule (e.g. dateFormat, from/to for REPLACE) */
    private Map<String, Object> params;
    /** Optional JSON rule-chain string; if present, overrides {@code rule} */
    private String ruleChain;
}