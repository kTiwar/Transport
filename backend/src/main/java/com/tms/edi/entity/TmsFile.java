package com.tms.edi.entity;

import com.tms.edi.enums.FileStatus;
import com.tms.edi.enums.FileType;
import com.tms.edi.enums.ProcessingMode;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "tms_files", indexes = {
    @Index(name = "idx_tms_files_partner_id",    columnList = "partner_id"),
    @Index(name = "idx_tms_files_status",         columnList = "status"),
    @Index(name = "idx_tms_files_checksum",       columnList = "checksum"),
    @Index(name = "idx_tms_files_received",       columnList = "received_timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TmsFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entry_no")
    private Long entryNo;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 20)
    private FileType fileType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private EdiPartner partner;

    @Column(name = "received_timestamp", nullable = false)
    private OffsetDateTime receivedTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_mode", nullable = false, length = 10)
    @Builder.Default
    private ProcessingMode processingMode = ProcessingMode.AUTO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private FileStatus status = FileStatus.RECEIVED;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "storage_path", length = 1000)
    private String storagePath;

    @Column(name = "file_content", columnDefinition = "BYTEA")
    private byte[] fileContent;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "processed_timestamp")
    private OffsetDateTime processedTimestamp;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "original_mapping_id")
    private Long originalMappingId;

    @Column(name = "order_count")
    private Integer orderCount;
}
