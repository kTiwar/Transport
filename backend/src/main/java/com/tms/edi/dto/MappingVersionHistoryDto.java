package com.tms.edi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MappingVersionHistoryDto {
    private Long id;
    private Long mappingId;
    private Integer version;
    private String savedBy;
    private OffsetDateTime savedAt;
    private String changeSummary;
    private Integer lineCount;
}
