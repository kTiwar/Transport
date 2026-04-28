package com.tms.edi.repository.imp;

import com.tms.edi.entity.imp.ImportOrderLine;
import com.tms.edi.entity.imp.ImportOrderLineId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportOrderLineRepository extends JpaRepository<ImportOrderLine, ImportOrderLineId> {

    List<ImportOrderLine> findByIdEntryNoAndOriginalFalse(Long entryNo);

    List<ImportOrderLine> findByIdEntryNo(Long entryNo);
}
