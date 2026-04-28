package com.tms.edi.repository.imp;

import com.tms.edi.entity.imp.ImportProcessingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportProcessingLogRepository extends JpaRepository<ImportProcessingLog, Long> {

    List<ImportProcessingLog> findByEntryNoOrderByCreatedAtDesc(Long entryNo);

    List<ImportProcessingLog> findByLogType(String logType);
}
