package com.tms.edi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "edi_order_lines", indexes = {
    @Index(name = "idx_eol_order_header", columnList = "order_header_id"),
    @Index(name = "idx_eol_entry_no",     columnList = "entry_no")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EdiOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_header_id", nullable = false)
    private EdiOrderHeader orderHeader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_no", nullable = false)
    private TmsFile tmsFile;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "item_code", length = 100)
    private String itemCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "quantity", precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_of_measure", length = 20)
    @Builder.Default
    private String unitOfMeasure = "KG";

    @Column(name = "weight_kg", precision = 12, scale = 3)
    private BigDecimal weightKg;

    @Column(name = "volume_m3", precision = 12, scale = 4)
    private BigDecimal volumeM3;

    @Column(name = "external_line_ref", length = 50)
    private String externalLineRef;
}
