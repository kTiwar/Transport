package com.tms.edi.repository;

import com.tms.edi.entity.EdiCost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EdiCostRepository extends JpaRepository<EdiCost, Long> {
    List<EdiCost> findByTmsFile_EntryNo(Long entryNo);
}
