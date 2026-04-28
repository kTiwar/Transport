package com.tms.edi.repository;

import com.tms.edi.entity.EdiCargoDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EdiCargoDetailsRepository extends JpaRepository<EdiCargoDetails, Long> {
    Optional<EdiCargoDetails> findByTmsFile_EntryNo(Long entryNo);
}
