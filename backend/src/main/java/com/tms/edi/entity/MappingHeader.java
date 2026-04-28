package com.tms.edi.entity;

import com.tms.edi.enums.FileType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mapping_header", indexes = {
    @Index(name = "idx_mapping_partner_type", columnList = "partner_id, file_type"),
    @Index(name = "idx_mapping_active",       columnList = "active_flag")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MappingHeader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mapping_id")
    private Long mappingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private EdiPartner partner;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 20)
    private FileType fileType;

    @Column(name = "mapping_name", nullable = false, length = 200)
    private String mappingName;

    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "active_flag", nullable = false)
    @Builder.Default
    private Boolean activeFlag = false;

    @Column(name = "status", length = 10, nullable = false)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_date", nullable = false, updatable = false)
    private OffsetDateTime createdDate;

    @Column(name = "updated_date")
    private OffsetDateTime updatedDate;

    @OneToMany(mappedBy = "mappingHeader", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequence ASC")
    @Builder.Default
    private List<MappingLine> lines = new ArrayList<>();

    @PrePersist
    void prePersist() {
        this.createdDate = OffsetDateTime.now();
        this.updatedDate = OffsetDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedDate = OffsetDateTime.now();
    }
}
