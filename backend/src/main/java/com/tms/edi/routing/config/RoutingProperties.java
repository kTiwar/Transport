package com.tms.edi.routing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "tms.routing")
public class RoutingProperties {

    /**
     * Nominatim base URL (no trailing slash). Use your own instance for production volume.
     */
    private String nominatimBaseUrl = "https://nominatim.openstreetmap.org";

    /**
     * Required by OSM usage policy: identify your application.
     */
    private String nominatimUserAgent = "TMS-RoutePlanner/1.0 (internal logistics; contact ops@example.com)";

    /** OSRM public HTTP API base, e.g. http://localhost:5000 — empty disables OSRM (Haversine fallback). */
    private String osrmBaseUrl = "";

    /** If false, never call OSRM; build matrix from Haversine × road factor. */
    private boolean osrmEnabled = false;

    /** Multiplier on straight-line distance when OSRM is off. */
    private double haversineRoadFactor = 1.35;

    /** Assumed average speed (km/h) for time matrix when OSRM is off. */
    private double fallbackAvgSpeedKmh = 45.0;

    /** Default depot coordinates when not supplied (Copenhagen area — override per tenant). */
    private double defaultDepotLatitude = 55.6761;
    private double defaultDepotLongitude = 12.5683;

    /** OR-Tools search time budget. */
    private int optimizerTimeLimitSeconds = 45;

    /** Service / dwell time at each stop (seconds), applied in time dimension. */
    private int serviceTimeSeconds = 300;

    /** Max cumulative time (seconds) from planning-day midnight for OR-Tools "Time" dimension. */
    private long timeHorizonSeconds = 172_800;

    /**
     * Max waiting slack (seconds) at each node for time dimension — allows arriving early for a TW.
     * Capped to {@link Integer#MAX_VALUE} when registering with OR-Tools.
     */
    private long timeSlackSeconds = 86_400;

    /** Interpret order time windows against this zone (e.g. Europe/Copenhagen, UTC). */
    private String routingTimeZoneId = "UTC";

    /** When true and {@code KafkaTemplate} is present, publish after successful optimize. */
    private boolean kafkaEnabled = false;

    /** Kafka topic for {@link com.tms.edi.routing.event.RoutesPlannedEvent}. */
    private String kafkaTopic = "tms.routing.routes-planned";
}
