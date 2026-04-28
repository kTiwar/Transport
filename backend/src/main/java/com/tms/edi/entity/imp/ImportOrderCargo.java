package com.tms.edi.entity.imp;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Staging table for EDI cargo/goods data.
 * Maps to AL: "Go4IMP Import Order Cargo"
 */
@Entity
@Table(name = "imp_order_cargo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportOrderCargo {

    @EmbeddedId
    private ImportOrderCargoId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_no", insertable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ImportOrderHeader header;

    @Column(name = "communication_partner", length = 20)
    private String communicationPartner;

    @Column(name = "external_order_no", length = 80)
    private String externalOrderNo;

    @Column(name = "order_line_no")
    private Integer orderLineNo;

    // Good identification
    @Column(name = "external_good_no", length = 50)
    private String externalGoodNo;

    @Column(name = "external_good_type", length = 50)
    private String externalGoodType;

    @Column(name = "external_good_sub_type", length = 50)
    private String externalGoodSubType;

    // Quantities
    @Column(name = "quantity", precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_of_measure_code", length = 20)
    private String unitOfMeasureCode;

    @Column(name = "pallet_places", precision = 18, scale = 4)
    private BigDecimal palletPlaces;

    @Column(name = "loading_meters", precision = 18, scale = 4)
    private BigDecimal loadingMeters;

    @Column(name = "force_loading_meters")
    private Boolean forceLoadingMeters;

    // Description
    @Column(name = "description", length = 50)
    private String description;

    @Column(name = "description2", length = 50)
    private String description2;

    // Weights & dimensions
    @Column(name = "net_weight", precision = 18, scale = 4)
    private BigDecimal netWeight;

    @Column(name = "gross_weight", precision = 18, scale = 4)
    private BigDecimal grossWeight;

    @Column(name = "width", precision = 18, scale = 4)
    private BigDecimal width;

    @Column(name = "length", precision = 18, scale = 4)
    private BigDecimal length;

    @Column(name = "height", precision = 18, scale = 4)
    private BigDecimal height;

    @Column(name = "diameter", precision = 18, scale = 4)
    private BigDecimal diameter;

    // ADR / Dangerous goods
    @Column(name = "adr_type", length = 20)
    private String adrType;

    @Column(name = "dangerous_goods")
    private Boolean dangerousGoods;

    @Column(name = "adr_dangerous_for_environment")
    private Boolean adrDangerousForEnvironment;

    @Column(name = "adr_un_no", length = 20)
    private String adrUnNo;

    @Column(name = "adr_hazard_class", length = 20)
    private String adrHazardClass;

    @Column(name = "adr_packing_group", length = 20)
    private String adrPackingGroup;

    @Column(name = "adr_tunnel_restriction_code", length = 20)
    private String adrTunnelRestrictionCode;

    // Temperature
    @Column(name = "set_temperature", precision = 10, scale = 2)
    private BigDecimal setTemperature;

    @Column(name = "temperature", precision = 10, scale = 2)
    private BigDecimal temperature;

    @Column(name = "min_temperature", precision = 10, scale = 2)
    private BigDecimal minTemperature;

    @Column(name = "max_temperature", precision = 10, scale = 2)
    private BigDecimal maxTemperature;

    // Tracing
    @Column(name = "tracing_no1", length = 50)
    private String tracingNo1;

    @Column(name = "tracing_no2", length = 50)
    private String tracingNo2;

    @Column(name = "original")
    @Builder.Default
    private Boolean original = false;
}
