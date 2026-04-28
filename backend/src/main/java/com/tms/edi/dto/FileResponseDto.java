package com.tms.edi.dto;

import com.tms.edi.enums.FileStatus;
import com.tms.edi.enums.FileType;
import com.tms.edi.enums.ProcessingMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileResponseDto {

    private Long entryNo;
    private String fileName;
    private FileType fileType;
    private Long partnerId;
    private String partnerCode;
    private String partnerName;
    private FileStatus status;
    private ProcessingMode processingMode;
    private Long fileSize;
    private String checksum;
    private String storagePath;
    private String errorMessage;
    private Integer retryCount;
    private Integer orderCount;
    private OffsetDateTime receivedTimestamp;
    private OffsetDateTime processedTimestamp;
}
