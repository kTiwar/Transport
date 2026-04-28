package com.tms.edi.entity.master;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "master_party")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MasterParty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "party_type", nullable = false, length = 32)
    private String partyType;

    @Column(name = "party_code", nullable = false, length = 64)
    private String partyCode;

    @Column(name = "name", nullable = false, length = 256)
    private String name;

    @Column(name = "legal_name", length = 256)
    private String legalName;

    @Column(name = "vat_number", length = 64)
    private String vatNumber;

    @Column(name = "country_code", length = 8)
    private String countryCode;

    @Column(name = "city", length = 128)
    private String city;

    @Column(name = "email", length = 256)
    private String email;

    @Column(name = "phone", length = 64)
    private String phone;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime n = LocalDateTime.now();
        if (createdAt == null) createdAt = n;
        if (updatedAt == null) updatedAt = n;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}