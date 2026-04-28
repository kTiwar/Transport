package com.tms.edi.routing.service;

import com.tms.edi.routing.client.OsrmClient;
import com.tms.edi.routing.config.RoutingProperties;
import com.tms.edi.routing.geo.Haversine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DistanceMatrixService {

    private final OsrmClient osrmClient;
    private final RoutingProperties props;

    /**
     * Coordinates as [lat, lon] per row, same order as VRP nodes.
     */
    public MatrixBundle buildMatrices(List<double[]> latLonNodes) {
        List<double[]> lonLat = OsrmClient.fromLatLonPairs(latLonNodes);
        var osrm = osrmClient.table(lonLat);

        int n = latLonNodes.size();
        long[][] distM = new long[n][n];
        long[][] timeS = new long[n][n];

        if (osrm.isPresent()) {
            distM = osrm.get().distancesMeters();
            timeS = osrm.get().durationsSeconds();
        } else {
            double factor = props.getHaversineRoadFactor();
            double speedMs = props.getFallbackAvgSpeedKmh() * 1000.0 / 3600.0;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i == j) {
                        distM[i][j] = 0;
                        timeS[i][j] = 0;
                    } else {
                        double d = Haversine.meters(
                                latLonNodes.get(i)[0], latLonNodes.get(i)[1],
                                latLonNodes.get(j)[0], latLonNodes.get(j)[1]) * factor;
                        distM[i][j] = Math.max(1, Math.round(d));
                        timeS[i][j] = Math.max(1, Math.round(d / speedMs));
                    }
                }
            }
        }
        return new MatrixBundle(distM, timeS, osrm.isPresent());
    }

    public record MatrixBundle(long[][] distanceMeters, long[][] durationSeconds, boolean fromOsrm) {}
}
