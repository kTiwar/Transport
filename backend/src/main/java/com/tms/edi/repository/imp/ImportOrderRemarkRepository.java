package com.tms.edi.repository.imp;

import com.tms.edi.entity.imp.ImportOrderRemark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportOrderRemarkRepository extends JpaRepository<ImportOrderRemark, Long> {

    List<ImportOrderRemark> findByEntryNoOrderByLineNoAscIdAsc(Long entryNo);
}