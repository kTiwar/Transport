package com.tms.edi.routing.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.tms.edi.routing.config.RoutingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * OSRM Table service — pairwise durations & distances (meters, seconds).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OsrmClient {

    private final RestTemplate osrmRestTemplate;
    private final RoutingProperties props;

    public Optional<OsrmTableResult> table(List<double[]> lonLatPairs) {
        if (!props.isOsrmEnabled() || props.getOsrmBaseUrl() == null || props.getOsrmBaseUrl().isBlank()) {
            return Optional.empty();
        }
        if (lonLatPairs.size() < 2) {
            return Optional.empty();
        }
        StringBuilder coords = new StringBuilder();
        for (int i = 0; i < lonLatPairs.size(); i++) {
            if (i > 0) coords.append(';');
            double lon = lonLatPairs.get(i)[0];
            double lat = lonLatPairs.get(i)[1];
            coords.append(lon).append(',').append(lat);
        }
        String base = props.getOsrmBaseUrl().replaceAll("/$", "");
        String url = base + "/table/v1/driving/" + coords + "?annotations=duration,distance";

        try {
            JsonNode root = osrmRestTemplate.getForObject(url, JsonNode.class);
            if (root == null || root.path("code").asText("").equals("NoRoute")) {
                return Optional.empty();
            }
            JsonNode durations = root.get("durations");
            JsonNode distances = root.get("distances");
            if (durations == null || distances == null) {
                return Optional.empty();
            }
            int n = durations.size();
            long[][] durSec = new long[n][n];
            long[][] distM = new long[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    double ds = durations.get(i).get(j).asDouble();
                    double dm = distances.get(i).get(j).asDouble();
                    durSec[i][j] = Double.isFinite(ds) ? Math.round(ds) : 0L;
                    distM[i][j] = Double.isFinite(dm) ? Math.round(dm) : 0L;
                }
            }
            return Optional.of(new OsrmTableResult(durSec, distM));
        } catch (Exception e) {
            log.warn("OSRM table call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** Lon,lat pairs in OSRM order. */
    public static List<double[]> fromLatLonPairs(List<double[]> latLon) {
        List<double[]> out = new ArrayList<>();
        for (double[] ll : latLon) {
            out.add(new double[] { ll[1], ll[0] });
        }
        return out;
    }

    public record OsrmTableResult(long[][] durationsSeconds, long[][] distancesMeters) {}
}
