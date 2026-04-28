package com.tms.edi.dto.tms;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TmsOrderCargoDto {
    private Long id;
    private Integer lineNo;
    private String goodNo;
    private String goodTypeCode;
    private String goodSubTypeCode;
    private BigDecimal quantity;
    private String unitOfMeasureCode;
    private String description;
    private BigDecimal netWeight;
    private BigDecimal grossWeight;
    private String adrType;
    private Boolean dangerousGoods;
}
