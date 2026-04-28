package com.tms.edi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MappingLineDto {

    private Long mappingLineId;
    private String sourceFieldPath;
    private String targetField;
    private String transformationRule;

    @JsonDeserialize(using = TransformationParamsStringDeserializer.class)
    private String transformationParams;
    private String defaultValue;
    private Boolean isRequired;
    private Integer sequence;
    private String conditionRule;
    private String lookupTableName;
}
