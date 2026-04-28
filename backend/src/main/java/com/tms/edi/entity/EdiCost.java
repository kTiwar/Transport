package com.tms.edi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "edi_costs", indexes = {
    @Index(name = "idx_ec_entry_no", columnList = "entry_no")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EdiCost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_no", nullable = false)
    private TmsFile tmsFile;

    @Column(name = "charge_type", length = 50)
    private String chargeType;

    @Column(name = "amount", precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "EUR";

    @Column(name = "vat_amount", precision = 12, scale = 2)
    private BigDecimal vatAmount;

    @Column(name = "external_charge_code", length = 50)
    private String externalChargeCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = OffsetDateTime.now();
    }
}
