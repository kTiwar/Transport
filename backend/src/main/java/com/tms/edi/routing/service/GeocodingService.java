package com.tms.edi.routing.service;

import com.tms.edi.routing.client.NominatimClient;
import com.tms.edi.routing.entity.RoutingLocation;
import com.tms.edi.routing.repository.RoutingLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class GeocodingService {

    private final RoutingLocationRepository locationRepo;
    private final NominatimClient nominatimClient;

    @Transactional
    public RoutingLocation resolveAndCache(String addressLine, String postcode) {
        String key = buildKey(addressLine, postcode);
        return locationRepo.findByAddressKey(key).orElseGet(() -> geocodeAndSave(key, addressLine, postcode));
    }

    private String buildKey(String address, String postcode) {
        String a = address == null ? "" : address.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        String p = postcode == null ? "" : postcode.trim().toLowerCase(Locale.ROOT);
        String raw = a + "|" + p;
        if (raw.length() > 512) {
            raw = raw.substring(0, 512);
        }
        return raw;
    }

    private RoutingLocation geocodeAndSave(String key, String addressLine, String postcode) {
        String q = (addressLine == null ? "" : addressLine) + ", " + (postcode == null ? "" : postcode);
        var hit = nominatimClient.search(q.trim())
                .orElseThrow(() -> new IllegalArgumentException("Geocoding failed for: " + truncate(q, 120)));

        RoutingLocation loc = RoutingLocation.builder()
                .addressKey(key)
                .addressLine(addressLine)
                .postcode(postcode)
                .latitude(hit.lat())
                .longitude(hit.lon())
                .city(hit.city())
                .country(hit.country())
                .source("NOMINATIM")
                .build();
        return locationRepo.save(loc);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    /** Persist a known coordinate (e.g. depot) without calling Nominatim. */
    @Transactional
    public RoutingLocation ensurePoint(String label, double latitude, double longitude) {
        String key = String.format(java.util.Locale.US, "coord:%.5f|%.5f", latitude, longitude);
        return locationRepo.findByAddressKey(key).orElseGet(() -> locationRepo.save(RoutingLocation.builder()
                .addressKey(key)
                .addressLine(label)
                .postcode("")
                .latitude(latitude)
                .longitude(longitude)
                .city("")
                .country("")
                .source("MANUAL")
                .build()));
    }
}
