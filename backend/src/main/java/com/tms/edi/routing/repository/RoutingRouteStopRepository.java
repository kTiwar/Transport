package com.tms.edi.routing.repository;

import com.tms.edi.routing.entity.RoutingRouteStop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutingRouteStopRepository extends JpaRepository<RoutingRouteStop, Long> {
    List<RoutingRouteStop> findByRouteRouteIdOrderBySequenceNumberAsc(Long routeId);
}
