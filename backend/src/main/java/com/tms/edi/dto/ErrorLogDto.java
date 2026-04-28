package com.tms.edi.dto;

import com.tms.edi.enums.ErrorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorLogDto {

    private Long errorId;
    private Long entryNo;
    private String fileName;
    private Long mappingLineId;
    private ErrorType errorType;
    private String errorCode;
    private String errorMessage;
    private String fieldPath;
    private Boolean resolvedFlag;
    private String resolvedBy;
    private OffsetDateTime resolvedAt;
    private String resolutionNote;
    private OffsetDateTime timestamp;
}
