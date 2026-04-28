package com.tms.edi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "edi_order_header", indexes = {
    @Index(name = "idx_eoh_entry_no",    columnList = "entry_no"),
    @Index(name = "idx_eoh_partner_id",  columnList = "partner_id"),
    @Index(name = "idx_eoh_ext_order",   columnList = "external_order_id, partner_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EdiOrderHeader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_no", nullable = false)
    private TmsFile tmsFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private EdiPartner partner;

    @Column(name = "external_order_id", nullable = false, length = 100)
    private String externalOrderId;

    @Column(name = "customer_code", length = 50)
    private String customerCode;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "requested_delivery_date")
    private LocalDate requestedDeliveryDate;

    @Column(name = "origin_address", columnDefinition = "TEXT")
    private String originAddress;

    @Column(name = "destination_address", columnDefinition = "TEXT")
    private String destinationAddress;

    @Column(name = "incoterm", length = 10)
    private String incoterm;

    @Column(name = "priority", length = 20)
    @Builder.Default
    private String priority = "NORMAL";

    @Column(name = "status", length = 30)
    @Builder.Default
    private String status = "NEW";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = OffsetDateTime.now();
    }
}
