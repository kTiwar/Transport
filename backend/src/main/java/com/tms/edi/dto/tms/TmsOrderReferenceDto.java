package com.tms.edi.dto.tms;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TmsOrderReferenceDto {
    private Long id;
    private String referenceCode;
    private String reference;
    private Integer orderLineNo;
}
