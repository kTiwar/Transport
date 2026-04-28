package com.tms.edi.routing.event;

import java.time.Instant;
import java.util.List;

/**
 * JSON-friendly payload for Kafka / async consumers after route optimization.
 */
public record RoutesPlannedEvent(
        String eventType,
        String optimizerRunId,
        List<Long> routeIds,
        List<Long> vehicleIds,
        List<Long> orderIds,
        String routeDate,
        Instant plannedAt
) {
    public static final String TYPE = "ROUTES_PLANNED";

    public static RoutesPlannedEvent of(
            String runId,
            List<Long> routeIds,
            List<Long> vehicleIds,
            List<Long> orderIds,
            String routeDateIso) {
        return new RoutesPlannedEvent(TYPE, runId, routeIds, vehicleIds, orderIds, routeDateIso, Instant.now());
    }
}
