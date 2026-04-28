package com.tms.edi.repository;

import com.tms.edi.entity.EdiOrderHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EdiOrderHeaderRepository extends JpaRepository<EdiOrderHeader, Long> {

    List<EdiOrderHeader> findByTmsFile_EntryNo(Long entryNo);

    Optional<EdiOrderHeader> findByExternalOrderIdAndPartner_PartnerId(
            String externalOrderId, Long partnerId);

    boolean existsByExternalOrderIdAndPartner_PartnerId(
            String externalOrderId, Long partnerId);
}
