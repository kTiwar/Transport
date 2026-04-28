package com.tms.edi.dto;

import com.tms.edi.enums.FileType;
import com.tms.edi.enums.ProcessingMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerDto {

    private Long partnerId;
    private String partnerCode;
    private String partnerName;
    private FileType defaultFormat;
    private ProcessingMode processingMode;
    private Boolean active;
    private Integer slaHours;
    private String contactEmail;
    private String sftpConfig;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
