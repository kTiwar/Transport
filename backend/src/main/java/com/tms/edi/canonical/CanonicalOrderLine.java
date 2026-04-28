package com.tms.edi.canonical;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CanonicalOrderLine {

    private Integer lineNumber;
    private String itemCode;
    private String description;
    private BigDecimal quantity;
    private String unitOfMeasure;
    private BigDecimal weightKg;
    private BigDecimal volumeM3;
    private String externalLineRef;
}
