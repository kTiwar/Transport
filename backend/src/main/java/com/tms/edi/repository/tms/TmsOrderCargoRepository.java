package com.tms.edi.repository.tms;

import com.tms.edi.entity.tms.TmsOrderCargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TmsOrderCargoRepository extends JpaRepository<TmsOrderCargo, Long> {

    List<TmsOrderCargo> findByTmsOrderId(Long orderId);

    Optional<TmsOrderCargo> findByTmsOrderIdAndLineNo(Long orderId, Integer lineNo);
}
