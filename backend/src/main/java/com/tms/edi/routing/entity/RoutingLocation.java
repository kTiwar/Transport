package com.tms.edi.routing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "routing_location")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutingLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "address_key", nullable = false, unique = true, length = 512)
    private String addressKey;

    @Column(name = "address_line", columnDefinition = "TEXT")
    private String addressLine;

    @Column(name = "postcode", length = 32)
    private String postcode;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "city", length = 128)
    private String city;

    @Column(name = "country", length = 8)
    private String country;

    @Column(name = "source", length = 32, nullable = false)
    @Builder.Default
    private String source = "NOMINATIM";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        var now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
