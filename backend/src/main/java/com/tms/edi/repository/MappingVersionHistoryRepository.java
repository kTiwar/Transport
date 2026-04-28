package com.tms.edi.repository;

import com.tms.edi.entity.MappingVersionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MappingVersionHistoryRepository extends JpaRepository<MappingVersionHistory, Long> {

    List<MappingVersionHistory> findByMappingIdOrderBySavedAtDesc(Long mappingId);
}
