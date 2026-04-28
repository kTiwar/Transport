package com.tms.edi.routing.controller;

import com.tms.edi.routing.dto.TrackingPointRequest;
import com.tms.edi.routing.entity.RoutingVehicleTracking;
import com.tms.edi.routing.service.RoutingTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/routing/tracking")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RoutingTrackingController {

    private final RoutingTrackingService trackingService;

    @PostMapping("/{vehicleId}")
    public ResponseEntity<Void> postPosition(
            @PathVariable Long vehicleId,
            @Valid @RequestBody TrackingPointRequest body) {
        trackingService.recordPosition(vehicleId, body);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<List<TrackingPointResponse>> getHistory(@PathVariable Long vehicleId) {
        List<TrackingPointResponse> rows = trackingService.history(vehicleId).stream()
                .map(t -> new TrackingPointResponse(
                        t.getLatitude(), t.getLongitude(), t.getRecordedAt().toString()))
                .toList();
        return ResponseEntity.ok(rows);
    }

    public record TrackingPointResponse(double latitude, double longitude, String recordedAt) {}
}
