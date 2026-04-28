package com.tms.edi.routing.repository;

import com.tms.edi.routing.entity.RoutingVehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutingVehicleRepository extends JpaRepository<RoutingVehicle, Long> {
    List<RoutingVehicle> findByActiveTrue();
}
