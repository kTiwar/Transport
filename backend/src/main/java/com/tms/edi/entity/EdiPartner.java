package com.tms.edi.entity;

import com.tms.edi.enums.FileType;
import com.tms.edi.enums.ProcessingMode;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "edi_partners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EdiPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "partner_id")
    private Long partnerId;

    @Column(name = "partner_code", nullable = false, unique = true, length = 50)
    private String partnerCode;

    @Column(name = "partner_name", nullable = false, length = 200)
    private String partnerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_format", length = 20)
    private FileType defaultFormat;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_mode", length = 10)
    @Builder.Default
    private ProcessingMode processingMode = ProcessingMode.AUTO;

    /** Stored as JSON text; use Jackson in service layer to parse/serialize. */
    @Column(name = "sftp_config", columnDefinition = "TEXT")
    private String sftpConfig;

    @Column(name = "api_key", length = 100)
    private String apiKey;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "sla_hours")
    @Builder.Default
    private Integer slaHours = 24;

    @Column(name = "contact_email", length = 200)
    private String contactEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
