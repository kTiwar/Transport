package com.tms.edi.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

/**
 * Persists accepted source-to-target field mappings so the AI engine can learn
 * from past decisions and boost confidence on future suggestions.
 */
@Entity
@Table(name = "ai_mapping_learning",
    uniqueConstraints = @UniqueConstraint(name = "uq_ai_learn",
        columnNames = {"partner_code","file_type","source_field_path","target_field"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MappingLearning {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_code", length = 50)
    private String partnerCode;

    @Column(name = "file_type", length = 20)
    private String fileType;

    @Column(name = "source_field_path", length = 500, nullable = false)
    private String sourceFieldPath;

    @Column(name = "target_field", length = 200, nullable = false)
    private String targetField;

    @Column(name = "transformation_rule", length = 100)
    private String transformationRule;

    @Column(name = "transformation_params", columnDefinition = "TEXT")
    private String transformationParams;

    @Column(name = "accepted_count", nullable = false)
    @Builder.Default private Integer acceptedCount = 1;

    @Column(name = "rejected_count", nullable = false)
    @Builder.Default private Integer rejectedCount = 0;

    @Column(name = "confidence_boost", nullable = false)
    @Builder.Default private Double confidenceBoost = 0.10;

    @Column(name = "last_accepted_at")
    private OffsetDateTime lastAcceptedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default private OffsetDateTime createdAt = OffsetDateTime.now();

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = OffsetDateTime.now(); }

    public void recordAcceptance() {
        this.acceptedCount++;
        this.lastAcceptedAt = OffsetDateTime.now();
        this.confidenceBoost = Math.min(0.30, 0.05 * Math.log(acceptedCount + 1));
    }

    public void recordRejection() {
        this.rejectedCount++;
        double net = Math.max(0, acceptedCount - rejectedCount);
        this.confidenceBoost = Math.max(0.0, Math.min(0.30, 0.05 * Math.log(net + 1)));
    }
}