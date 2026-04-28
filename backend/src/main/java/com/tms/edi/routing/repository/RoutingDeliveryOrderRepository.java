package com.tms.edi.routing.repository;

import com.tms.edi.routing.entity.RoutingDeliveryOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoutingDeliveryOrderRepository extends JpaRepository<RoutingDeliveryOrder, Long> {
    List<RoutingDeliveryOrder> findByIdIn(List<Long> ids);

    List<RoutingDeliveryOrder> findByStatus(String status);

    Optional<RoutingDeliveryOrder> findByPublicOrderId(String publicOrderId);
}
