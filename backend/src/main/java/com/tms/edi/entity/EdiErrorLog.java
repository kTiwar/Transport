package com.tms.edi.entity;

import com.tms.edi.enums.ErrorType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "edi_error_log", indexes = {
    @Index(name = "idx_eel_entry_no",       columnList = "entry_no"),
    @Index(name = "idx_eel_resolved",       columnList = "resolved_flag"),
    @Index(name = "idx_eel_error_type",     columnList = "error_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EdiErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "error_id")
    private Long errorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_no", nullable = false)
    private TmsFile tmsFile;

    @Column(name = "mapping_line_id")
    private Long mappingLineId;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_type", nullable = false, length = 50)
    private ErrorType errorType;

    @Column(name = "error_code", length = 20)
    private String errorCode;

    @Column(name = "error_message", nullable = false, columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "field_path", length = 500)
    private String fieldPath;

    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    @Column(name = "resolved_flag", nullable = false)
    @Builder.Default
    private Boolean resolvedFlag = false;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @PrePersist
    void prePersist() {
        if (this.timestamp == null) {
            this.timestamp = OffsetDateTime.now();
        }
    }
}
