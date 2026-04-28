package com.tms.edi.repository.imp;

import com.tms.edi.entity.imp.ImportOrderHeader;
import com.tms.edi.enums.ImportStatus;
import com.tms.edi.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImportOrderHeaderRepository extends JpaRepository<ImportOrderHeader, Long> {

    List<ImportOrderHeader> findByCommunicationPartnerAndStatus(String partner, ImportStatus status);

    List<ImportOrderHeader> findByStatus(ImportStatus status);

    List<ImportOrderHeader> findByImportFileEntryNoOrderByEntryNoAsc(Long importFileEntryNo);

    Page<ImportOrderHeader> findAllByOrderByReceivedAtDesc(Pageable pageable);

    Optional<ImportOrderHeader> findByCommunicationPartnerAndExternalOrderNo(
            String partner, String externalOrderNo);

    List<ImportOrderHeader> findByTransactionTypeAndStatus(TransactionType type, ImportStatus status);

    @Query("SELECT h FROM ImportOrderHeader h WHERE h.status IN (:statuses)")
    List<ImportOrderHeader> findByStatusIn(@Param("statuses") List<ImportStatus> statuses);

    /**
     * Latest processed import (excluding the current staging row) with the same partner + external order no.
     * Used to resolve TMS order for FINALIZE_ORDER / UPDATE after a prior INSERT import.
     */
    @Query("""
            SELECT h FROM ImportOrderHeader h
            WHERE h.communicationPartner = :partner
              AND h.externalOrderNo = :externalOrderNo
              AND h.status = :status
              AND h.tmsOrderNo IS NOT NULL
              AND h.entryNo <> :excludeEntryNo
            ORDER BY h.entryNo DESC
            """)
    Page<ImportOrderHeader> findPreviousProcessedByPartnerAndExternalOrderNo(
            @Param("partner") String partner,
            @Param("externalOrderNo") String externalOrderNo,
            @Param("status") ImportStatus status,
            @Param("excludeEntryNo") Long excludeEntryNo,
            Pageable pageable);

    long countByStatus(ImportStatus status);
}
