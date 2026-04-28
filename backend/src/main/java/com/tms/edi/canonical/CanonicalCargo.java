package com.tms.edi.canonical;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CanonicalCargo {

    private String cargoType;
    private BigDecimal totalWeight;
    private BigDecimal totalVolume;
    private Integer palletCount;
    private Boolean hazmatFlag;
    private String temperatureReq;
    private String specialInstructions;
}
