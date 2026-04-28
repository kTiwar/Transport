package com.tms.edi.entity.tms;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * TMS Order cargo/goods entry.
 * Maps to AL: "Go4TMS Order Cargo"
 */
@Entity
@Table(name = "tms_order_cargo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TmsOrderCargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tms_order_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TmsOrder tmsOrder;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    /** Mapped TMS Good No. */
    @Column(name = "good_no", length = 20)
    private String goodNo;

    /** Mapped Good Type Code */
    @Column(name = "good_type_code", length = 20)
    private String goodTypeCode;

    /** Mapped Good Sub Type Code */
    @Column(name = "good_sub_type_code", length = 20)
    private String goodSubTypeCode;

    @Column(name = "quantity", precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "qty_per_uom", precision = 18, scale = 4)
    private BigDecimal qtyPerUom;

    @Column(name = "unit_of_measure_code", length = 20)
    private String unitOfMeasureCode;

    @Column(name = "description", length = 50)
    private String description;

    @Column(name = "description2", length = 50)
    private String description2;

    // ADR
    @Column(name = "adr_type", length = 20)
    private String adrType;

    @Column(name = "dangerous_goods")
    private Boolean dangerousGoods;

    @Column(name = "adr_dangerous_for_environment")
    private Boolean adrDangerousForEnvironment;

    @Column(name = "adr_un_no", length = 20)
    private String adrUnNo;

    @Column(name = "hazard_class", length = 20)
    private String hazardClass;

    @Column(name = "packing_group", length = 20)
    private String packingGroup;

    @Column(name = "tunnel_restriction_code", length = 20)
    private String tunnelRestrictionCode;

    // Temperature
    @Column(name = "set_temperature", precision = 10, scale = 2)
    private BigDecimal setTemperature;

    @Column(name = "temperature", precision = 10, scale = 2)
    private BigDecimal temperature;

    @Column(name = "min_temperature", precision = 10, scale = 2)
    private BigDecimal minTemperature;

    @Column(name = "max_temperature", precision = 10, scale = 2)
    private BigDecimal maxTemperature;

    // Physical
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

    @Column(name = "pallet_places", precision = 18, scale = 4)
    private BigDecimal palletPlaces;

    @Column(name = "loading_meters", precision = 18, scale = 4)
    private BigDecimal loadingMeters;

    @Column(name = "tracing_no1", length = 50)
    private String tracingNo1;

    @Column(name = "tracing_no2", length = 50)
    private String tracingNo2;
}
