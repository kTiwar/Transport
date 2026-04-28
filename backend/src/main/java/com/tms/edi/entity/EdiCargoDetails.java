package com.tms.edi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "edi_cargo_details", indexes = {
    @Index(name = "idx_ecd_entry_no", columnList = "entry_no")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EdiCargoDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_no", nullable = false)
    private TmsFile tmsFile;

    @Column(name = "cargo_type", length = 50)
    private String cargoType;

    @Column(name = "total_weight", precision = 14, scale = 3)
    private BigDecimal totalWeight;

    @Column(name = "total_volume", precision = 14, scale = 4)
    private BigDecimal totalVolume;

    @Column(name = "pallet_count")
    private Integer palletCount;

    @Column(name = "hazmat_flag")
    @Builder.Default
    private Boolean hazmatFlag = false;

    @Column(name = "temperature_req", length = 30)
    private String temperatureReq;

    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = OffsetDateTime.now();
    }
}
