package com.tms.edi.routing.service;

import com.tms.edi.routing.dto.TrackingPointRequest;
import com.tms.edi.routing.entity.RoutingVehicle;
import com.tms.edi.routing.entity.RoutingVehicleTracking;
import com.tms.edi.routing.repository.RoutingVehicleRepository;
import com.tms.edi.routing.repository.RoutingVehicleTrackingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoutingTrackingService {

    private final RoutingVehicleRepository vehicleRepo;
    private final RoutingVehicleTrackingRepository trackingRepo;

    @Transactional
    public void recordPosition(Long vehicleId, TrackingPointRequest req) {
        RoutingVehicle v = vehicleRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + vehicleId));
        trackingRepo.save(RoutingVehicleTracking.builder()
                .vehicle(v)
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .build());
    }

    @Transactional(readOnly = true)
    public List<RoutingVehicleTracking> history(Long vehicleId) {
        return trackingRepo.findTop200ByVehicleVehicleIdOrderByRecordedAtDesc(vehicleId);
    }
}
