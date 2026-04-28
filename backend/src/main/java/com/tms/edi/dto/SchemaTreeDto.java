package com.tms.edi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaTreeDto {

    private String path;
    private String name;
    private String type;
    private String sampleValue;
    private Boolean isArray;
    private Integer arrayCount;

    @Builder.Default
    private List<SchemaTreeDto> children = new ArrayList<>();
}
