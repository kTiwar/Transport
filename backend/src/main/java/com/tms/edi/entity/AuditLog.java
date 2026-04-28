package com.tms.edi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_al_username",  columnList = "username"),
    @Index(name = "idx_al_timestamp", columnList = "timestamp"),
    @Index(name = "idx_al_action",    columnList = "action")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    @PrePersist
    void prePersist() {
        if (this.timestamp == null) {
            this.timestamp = OffsetDateTime.now();
        }
    }
}
