package com.tms.edi.repository.imp;

import com.tms.edi.entity.imp.ImportOrderCargo;
import com.tms.edi.entity.imp.ImportOrderCargoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportOrderCargoRepository extends JpaRepository<ImportOrderCargo, ImportOrderCargoId> {

    List<ImportOrderCargo> findByIdEntryNoAndOriginalFalse(Long entryNo);

    List<ImportOrderCargo> findByIdEntryNo(Long entryNo);
}
