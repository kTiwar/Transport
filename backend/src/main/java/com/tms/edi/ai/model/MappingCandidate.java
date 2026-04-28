package com.tms.edi.ai.model;

import lombok.Builder;
import lombok.Data;

/**
 * A ranked candidate mapping produced by the AI Mapping Engine.
 * Represents one possible source-field -> canonical-target pairing.
 */
@Data
@Builder
public class MappingCandidate {

    /** Source schema field proposed for this mapping */
    private SchemaField sourceField;

    /** Canonical target field name (e.g. "container.container_type") */
    private String targetField;

    /** AI confidence score in [0.0, 1.0] */
    private double confidenceScore;

    /** Human-readable explanation of why this pairing was chosen */
    private String reason;

    /** Suggested transformation rule (e.g. "DIRECT", "UPPER", "DATE_FORMAT") */
    private String suggestedTransform;

    /** True if confidence was boosted because this pairing was accepted before */
    private boolean fromHistory;

    /** True if this canonical target field is marked as required */
    private boolean required;
}