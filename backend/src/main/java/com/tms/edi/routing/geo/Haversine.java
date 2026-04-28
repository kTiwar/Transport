package com.tms.edi.routing.geo;

/**
 * Great-circle distance (WGS84 sphere approximation) in meters.
 */
public final class Haversine {

    private static final double R_EARTH_M = 6_371_000.0;

    private Haversine() {}

    public static double meters(double lat1, double lon1, double lat2, double lon2) {
        double φ1 = Math.toRadians(lat1);
        double φ2 = Math.toRadians(lat2);
        double Δφ = Math.toRadians(lat2 - lat1);
        double Δλ = Math.toRadians(lon2 - lon1);

        double a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2)
                + Math.cos(φ1) * Math.cos(φ2) * Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R_EARTH_M * c;
    }
}
