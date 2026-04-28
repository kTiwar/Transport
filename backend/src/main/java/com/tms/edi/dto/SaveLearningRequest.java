package com.tms.edi.dto;

import lombok.Data;

/**
 * Request body for POST /api/v1/ai-mapping/save-mapping (learning feedback).
 */
@Data
public class SaveLearningRequest {
    private String partnerCode;
    private String fileType;
    private String sourceFieldPath;
    private String targetField;
    private String transformationRule;
    /** true = user accepted this mapping; false = user rejected it */
    private boolean accepted;
}