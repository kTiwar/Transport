package com.tms.edi.dto.address;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressUsageDto {
    private Long usageId;
    private String usageType;
    private Integer priority;
}