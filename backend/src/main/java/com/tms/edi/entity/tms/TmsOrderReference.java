package com.tms.edi.entity.tms;

import jakarta.persistence.*;
import lombok.*;

/**
 * TMS Order reference (order-level or line-level).
 * Maps to AL: "Go4TMS Order Reference" / "Go4TMS Order Line Reference"
 */
@Entity
@Table(name = "tms_order_reference")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TmsOrderReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tms_order_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TmsOrder tmsOrder;

    /** Mapped TMS Reference Code */
    @Column(name = "reference_code", nullable = false, length = 20)
    private String referenceCode;

    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "customer_no", length = 20)
    private String customerNo;

    /** 0 = order-level reference, >0 = line-level reference */
    @Column(name = "order_line_no")
    @Builder.Default
    private Integer orderLineNo = 0;
}
