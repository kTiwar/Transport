package com.tms.edi.canonical;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CanonicalCost {

    private String chargeType;
    private BigDecimal amount;
    private String currency;
    private BigDecimal vatAmount;
    private String externalChargeCode;
}
