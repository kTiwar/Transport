package com.tms.edi.repository;

import com.tms.edi.entity.TmsFile;
import com.tms.edi.enums.FileStatus;
import com.tms.edi.enums.ProcessingMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TmsFileRepository
        extends JpaRepository<TmsFile, Long>, JpaSpecificationExecutor<TmsFile> {

    boolean existsByChecksumAndIsDeletedFalse(String checksum);

    boolean existsByChecksumAndPartner_PartnerIdAndIsDeletedFalse(String checksum, Long partnerId);

    Page<TmsFile> findByIsDeletedFalseOrderByReceivedTimestampDesc(Pageable pageable);

    Page<TmsFile> findByPartner_PartnerIdAndIsDeletedFalse(Long partnerId, Pageable pageable);

    Page<TmsFile> findByStatusAndIsDeletedFalse(FileStatus status, Pageable pageable);

    List<TmsFile> findByStatusAndProcessingModeAndIsDeletedFalse(
            FileStatus status, ProcessingMode mode);

    @Query("""
           SELECT f FROM TmsFile f
           WHERE f.status = :status
             AND f.processingMode = :mode
             AND f.isDeleted = false
             AND f.receivedTimestamp BETWEEN :from AND :to
           """)
    List<TmsFile> findScheduledFiles(
            @Param("status") FileStatus status,
            @Param("mode")   ProcessingMode mode,
            @Param("from")   OffsetDateTime from,
            @Param("to")     OffsetDateTime to);

    @Modifying
    @Query("UPDATE TmsFile f SET f.status = :status WHERE f.entryNo = :entryNo")
    void updateStatus(@Param("entryNo") Long entryNo, @Param("status") FileStatus status);

    @Query("SELECT COUNT(f) FROM TmsFile f WHERE f.status = :status AND f.isDeleted = false")
    long countByStatus(@Param("status") FileStatus status);

    @Query("""
           SELECT COUNT(f) FROM TmsFile f
           WHERE f.receivedTimestamp >= :from
             AND f.isDeleted = false
           """)
    long countReceivedSince(@Param("from") OffsetDateTime from);

    @Query("""
           SELECT f FROM TmsFile f
           JOIN FETCH f.partner
           WHERE f.entryNo = :entryNo AND f.isDeleted = false
           """)
    Optional<TmsFile> findByEntryNoAndIsDeletedFalse(@Param("entryNo") Long entryNo);
}
