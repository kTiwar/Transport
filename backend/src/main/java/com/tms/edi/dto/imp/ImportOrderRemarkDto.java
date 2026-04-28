package com.tms.edi.dto.imp;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportOrderRemarkDto {

    private Long id;
    private Long entryNo;
    private String externalOrderNo;
    private String remarkType;
    private Integer lineNo;
    private String remarks;
    private String externalRemarkCode;
    private LocalDateTime importDatetime;
    private LocalDateTime processedDatetime;
    private String communicationPartner;
    private String externalOrderLineId;
    private Integer orderLineNo;
    private String createdBy;
    private LocalDateTime creationDatetime;
    private String lastModifiedBy;
    private LocalDateTime lastModificationDatetime;
}