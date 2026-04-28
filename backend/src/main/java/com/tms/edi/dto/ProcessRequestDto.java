package com.tms.edi.dto;

import lombok.Data;

@Data
public class ProcessRequestDto {
    private Long mappingId;
    private String mode;
    private boolean dryRun;
}
