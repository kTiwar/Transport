package com.tms.edi.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response body for POST /api/v1/ai-mapping/transform.
 */
@Data
@Builder
public class AiTransformResponse {
    private String originalValue;
    private String transformedValue;
    private String ruleApplied;
    private boolean success;
    private String errorMessage;
}