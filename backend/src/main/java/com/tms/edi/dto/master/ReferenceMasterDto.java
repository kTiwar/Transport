package com.tms.edi.dto.master;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferenceMasterDto {
    private Long id;
    private String category;
    private String code;
    private String name;
    private String description;
    private String extraJson;
    private Integer sortOrder;
    private Boolean isActive;
}