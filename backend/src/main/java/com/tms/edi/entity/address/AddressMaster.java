package com.tms.edi.entity.address;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "address_master")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long addressId;

    @Column(name = "address_code", nullable = false, length = 64)
    private String addressCode;

    @Column(name = "address_type", length = 64)
    private String addressType;

    @Column(name = "entity_type", length = 64)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "address_line1", length = 512)
    private String addressLine1;

    @Column(name = "address_line2", length = 512)
    private String addressLine2;

    @Column(name = "address_line3", length = 512)
    private String addressLine3;

    @Column(name = "landmark", length = 256)
    private String landmark;

    @Column(name = "city", length = 128)
    private String city;

    @Column(name = "district", length = 128)
    private String district;

    @Column(name = "state_province", length = 128)
    private String stateProvince;

    @Column(name = "postal_code", length = 32)
    private String postalCode;

    @Column(name = "country_code", length = 8)
    private String countryCode;

    @Column(name = "country_name", length = 128)
    private String countryName;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "timezone", length = 64)
    private String timezone;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "validation_status", length = 32)
    private String validationStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}