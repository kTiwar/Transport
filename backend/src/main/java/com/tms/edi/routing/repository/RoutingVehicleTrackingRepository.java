package com.tms.edi.routing.repository;

import com.tms.edi.routing.entity.RoutingVehicleTracking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutingVehicleTrackingRepository extends JpaRepository<RoutingVehicleTracking, Long> {
    List<RoutingVehicleTracking> findTop200ByVehicleVehicleIdOrderByRecordedAtDesc(Long vehicleId);
}
