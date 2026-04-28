package com.tms.edi.dto;

import lombok.Data;

/**
 * Request body for POST /api/v1/ai-mapping/generate-mapping.
 */
@Data
public class AiGenerateMappingRequest {
    /** TmsFile entry number to use as the source schema */
    private Long fileEntryNo;
    /** Optional: restrict historical learning to this partner code */
    private String partnerCode;
    /** Optional: restrict historical learning to this file type */
    private String fileType;
}