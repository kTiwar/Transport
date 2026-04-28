package com.tms.edi.repository.imp;

import com.tms.edi.entity.imp.ImportTransportCost;
import com.tms.edi.entity.imp.ImportTransportCostId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportTransportCostRepository extends JpaRepository<ImportTransportCost, ImportTransportCostId> {

    List<ImportTransportCost> findByIdEntryNo(Long entryNo);
}
