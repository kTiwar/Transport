package com.tms.edi.routing.repository;

import com.tms.edi.routing.entity.RoutingRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutingRouteRepository extends JpaRepository<RoutingRoute, Long> {
    List<RoutingRoute> findAllByOrderByCreatedAtDesc();
}
