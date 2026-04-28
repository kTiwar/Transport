package com.tms.edi.repository;

import com.tms.edi.entity.EdiOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EdiOrderLineRepository extends JpaRepository<EdiOrderLine, Long> {
    List<EdiOrderLine> findByOrderHeader_Id(Long orderHeaderId);
}
