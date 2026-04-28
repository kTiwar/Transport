package com.tms.edi.repository.master;

import com.tms.edi.entity.master.ReferenceMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReferenceMasterRepository extends JpaRepository<ReferenceMaster, Long> {

    Page<ReferenceMaster> findByCategoryAndIsActiveOrderBySortOrderAscCodeAsc(
            String category, Boolean isActive, Pageable pageable);

    Page<ReferenceMaster> findByCategoryOrderBySortOrderAscCodeAsc(String category, Pageable pageable);

    Optional<ReferenceMaster> findByCategoryAndCode(String category, String code);

    List<ReferenceMaster> findByCategoryAndIsActiveOrderBySortOrderAscCodeAsc(String category, Boolean isActive);
}