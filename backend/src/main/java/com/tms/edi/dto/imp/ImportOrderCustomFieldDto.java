package com.tms.edi.dto.imp;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportOrderCustomFieldDto {

    private Long id;
    private Long entryNo;
    private Integer lineNo;
    private String fieldName;
    private String fieldValue;
    private String externalOrderNo;
    private String communicationPartner;
    private String createdBy;
    private LocalDateTime creationDatetime;
    private String lastModifiedBy;
    private LocalDateTime lastModificationDatetime;
}