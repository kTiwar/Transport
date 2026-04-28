package com.tms.edi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tms.edi.enums.FileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MappingDto {

    private Long mappingId;
    private Long partnerId;
    private String partnerCode;
    private FileType fileType;
    private String mappingName;
    private Integer version;
    private Boolean activeFlag;
    private String status;
    private String description;
    private String createdBy;
    private OffsetDateTime createdDate;
    private OffsetDateTime updatedDate;
    private List<MappingLineDto> lines;
}
