package com.tms.edi.dto.address;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressAttributeDto {
    private Long attrId;
    private String attrKey;
    private String attrValue;
}