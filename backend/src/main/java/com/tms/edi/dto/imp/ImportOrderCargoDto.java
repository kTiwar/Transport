package com.tms.edi.dto.imp;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportOrderCargoDto {
    private Long entryNo;
    /** Primary key line_no on imp_order_cargo (cargo row index). */
    private Integer lineNo;
    /** Links cargo to an import order line when set. */
    private Integer orderLineNo;
    private String externalOrderNo;
    private String communicationPartner;
    private String externalGoodNo;
    private String externalGoodType;
    private String externalGoodSubType;
    private BigDecimal quantity;
    private String unitOfMeasureCode;
    private String description;
    private String description2;
    private BigDecimal netWeight;
    private BigDecimal grossWeight;
    private BigDecimal width;
    private BigDecimal length;
    private BigDecimal height;
    private BigDecimal diameter;
    private BigDecimal palletPlaces;
    private BigDecimal loadingMeters;
    private Boolean forceLoadingMeters;
    private String adrType;
    private Boolean dangerousGoods;
    private Boolean adrDangerousForEnvironment;
    private String adrUnNo;
    private String adrHazardClass;
    private String adrPackingGroup;
    private String adrTunnelRestrictionCode;
    private String tracingNo1;
    private String tracingNo2;
    private BigDecimal temperature;
    private BigDecimal minTemperature;
    private BigDecimal maxTemperature;
}
