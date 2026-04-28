package com.tms.edi.repository.imp;

import com.tms.edi.entity.imp.ImportOrderEquipment;
import com.tms.edi.entity.imp.ImportOrderEquipmentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportOrderEquipmentRepository extends JpaRepository<ImportOrderEquipment, ImportOrderEquipmentId> {

    List<ImportOrderEquipment> findByIdEntryNo(Long entryNo);
}
