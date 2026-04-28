package com.tms.edi.entity.master;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reference_master")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferenceMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category", nullable = false, length = 64)
    private String category;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 256)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "extra_json", columnDefinition = "text")
    private String extraJson;

    @Column(name = "sort_order")
    private Integer sortOrder;

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