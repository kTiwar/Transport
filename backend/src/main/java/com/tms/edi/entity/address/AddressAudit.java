package com.tms.edi.entity.address;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "address_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "address_id", nullable = false)
    private Long addressId;

    @Column(name = "changed_by", length = 128)
    private String changedBy;

    @Column(name = "old_value", columnDefinition = "text")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "text")
    private String newValue;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @PrePersist
    void onCreate() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }
}