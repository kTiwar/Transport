package com.tms.edi.repository;

import com.tms.edi.entity.EdiErrorLog;
import com.tms.edi.enums.ErrorType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EdiErrorLogRepository extends JpaRepository<EdiErrorLog, Long> {

    List<EdiErrorLog> findByTmsFile_EntryNo(Long entryNo);

    List<EdiErrorLog> findByTmsFile_EntryNoOrderByTimestampDesc(Long entryNo);

    Page<EdiErrorLog> findByResolvedFlagFalse(Pageable pageable);

    Page<EdiErrorLog> findByErrorType(ErrorType errorType, Pageable pageable);

    long countByResolvedFlagFalse();

    @Query("""
           SELECT e.errorType, COUNT(e) FROM EdiErrorLog e
           WHERE e.resolvedFlag = false
           GROUP BY e.errorType
           ORDER BY COUNT(e) DESC
           """)
    List<Object[]> countByErrorTypeGrouped();
}
