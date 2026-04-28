package com.tms.edi.entity.imp;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Processing error and audit log for import orders.
 * Maps to AL: "Go4IMP Error Log"
 */
@Entity
@Table(name = "imp_processing_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportProcessingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_no", nullable = false)
    private Long entryNo;

    @Column(name = "tms_order_no", length = 20)
    private String tmsOrderNo;

    @Column(name = "log_type", length = 20, nullable = false)
    @Builder.Default
    private String logType = "ERROR";   // ERROR | WARNING | INFO

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "field_name", length = 100)
    private String fieldName;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by", length = 50)
    private String createdBy;
}
