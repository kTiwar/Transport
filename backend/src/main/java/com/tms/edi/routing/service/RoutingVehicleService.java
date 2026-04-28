package com.tms.edi.routing.service;

import com.tms.edi.routing.dto.RoutingVehicleRequest;
import com.tms.edi.routing.dto.RoutingVehicleResponse;
import com.tms.edi.routing.entity.RoutingLocation;
import com.tms.edi.routing.entity.RoutingVehicle;
import com.tms.edi.routing.repository.RoutingLocationRepository;
import com.tms.edi.routing.repository.RoutingVehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoutingVehicleService {

    private final RoutingVehicleRepository vehicleRepo;
    private final RoutingLocationRepository locationRepo;

    @Transactional
    public RoutingVehicleResponse create(RoutingVehicleRequest req) {
        RoutingVehicle v = RoutingVehicle.builder()
                .code(req.getCode())
                .vehicleType(req.getVehicleType())
                .capacityWeightKg(req.getCapacityWeightKg())
                .capacityVolumeM3(req.getCapacityVolumeM3())
                .startLocation(resolveLoc(req.getStartLocationId()))
                .endLocation(resolveLoc(req.getEndLocationId()))
                .shiftStart(req.getShiftStart())
                .shiftEnd(req.getShiftEnd())
                .active(true)
                .build();
        return toDto(vehicleRepo.save(v));
    }

    @Transactional(readOnly = true)
    public List<RoutingVehicleResponse> list() {
        return vehicleRepo.findByActiveTrue().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public RoutingVehicleResponse get(Long id) {
        return vehicleRepo.findById(id).map(this::toDto).orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
    }

    private RoutingLocation resolveLoc(Long id) {
        if (id == null) {
            return null;
        }
        return locationRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Location not found: " + id));
    }

    private RoutingVehicleResponse toDto(RoutingVehicle v) {
        return RoutingVehicleResponse.builder()
                .vehicleId(v.getVehicleId())
                .code(v.getCode())
                .vehicleType(v.getVehicleType())
                .capacityWeightKg(v.getCapacityWeightKg())
                .capacityVolumeM3(v.getCapacityVolumeM3())
                .startLocationId(v.getStartLocation() != null ? v.getStartLocation().getId() : null)
                .endLocationId(v.getEndLocation() != null ? v.getEndLocation().getId() : null)
                .shiftStart(v.getShiftStart())
                .shiftEnd(v.getShiftEnd())
                .active(v.getActive())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
