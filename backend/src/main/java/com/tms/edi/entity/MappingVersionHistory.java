package com.tms.edi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "mapping_version_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MappingVersionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mapping_id", nullable = false)
    private Long mappingId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "saved_by", length = 100)
    private String savedBy;

    @Column(name = "saved_at", nullable = false)
    private OffsetDateTime savedAt;

    @Column(name = "lines_snapshot", columnDefinition = "TEXT")
    private String linesSnapshot;

    @Column(name = "change_summary", length = 500)
    private String changeSummary;

    @PrePersist
    void prePersist() {
        if (this.savedAt == null) this.savedAt = OffsetDateTime.now();
    }
}
