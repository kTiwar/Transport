package com.tms.edi.entity.imp;

import jakarta.persistence.*;
import lombok.*;

/**
 * Staging table for EDI order references (customer ref, SSCC, etc.).
 * Maps to AL: "Go4Imp Import Order Ref."
 */
@Entity
@Table(name = "imp_order_reference")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportOrderReference {

    @EmbeddedId
    private ImportOrderRefId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_no", insertable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ImportOrderHeader header;

    @Column(name = "communication_partner", length = 20)
    private String communicationPartner;

    @Column(name = "external_order_no", length = 80)
    private String externalOrderNo;

    @Column(name = "reference_code", length = 20)
    private String referenceCode;

    @Column(name = "reference", length = 100)
    private String reference;

    /** If 0, it is an order-level reference; otherwise it is a line-level reference */
    @Column(name = "order_line_no")
    @Builder.Default
    private Integer orderLineNo = 0;

    @Column(name = "original")
    @Builder.Default
    private Boolean original = false;
}
