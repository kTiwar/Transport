package com.tms.edi.repository.imp;

import com.tms.edi.entity.imp.ImportOrderReference;
import com.tms.edi.entity.imp.ImportOrderRefId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportOrderReferenceRepository extends JpaRepository<ImportOrderReference, ImportOrderRefId> {

    List<ImportOrderReference> findByIdEntryNoAndOriginalFalse(Long entryNo);

    List<ImportOrderReference> findByIdEntryNo(Long entryNo);
}
