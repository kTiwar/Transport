package com.tms.edi.repository.tms;

import com.tms.edi.entity.tms.TmsOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TmsOrderLineRepository extends JpaRepository<TmsOrderLine, Long> {

    List<TmsOrderLine> findByTmsOrderId(Long orderId);

    Optional<TmsOrderLine> findByTmsOrderIdAndLineNo(Long orderId, Integer lineNo);
}
