package com.tms.edi.routing.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.tms.edi.routing.config.RoutingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

/**
 * OpenStreetMap Nominatim search (free; respect usage policy — rate-limit in production).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NominatimClient {

    private final RestTemplate nominatimRestTemplate;
    private final RoutingProperties props;

    public Optional<NominatimHit> search(String freeTextQuery) {
        String url = UriComponentsBuilder
                .fromUriString(props.getNominatimBaseUrl() + "/search")
                .queryParam("q", freeTextQuery)
                .queryParam("format", "json")
                .queryParam("limit", 1)
                .queryParam("addressdetails", 1)
                .encode()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", props.getNominatimUserAgent());
        headers.set("Accept", "application/json");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> resp = nominatimRestTemplate.exchange(
                    url, HttpMethod.GET, entity, JsonNode.class);
            JsonNode body = resp.getBody();
            if (body == null || !body.isArray() || body.isEmpty()) {
                return Optional.empty();
            }
            JsonNode first = body.get(0);
            double lat = first.path("lat").asDouble();
            double lon = first.path("lon").asDouble();
            String display = first.path("display_name").asText("");
            String city = first.path("address").path("city").asText(
                    first.path("address").path("town").asText(
                            first.path("address").path("village").asText("")));
            String country = first.path("address").path("country_code").asText("");
            return Optional.of(new NominatimHit(lat, lon, display, city, country));
        } catch (Exception e) {
            log.warn("Nominatim search failed for query snippet: {} — {}", truncate(freeTextQuery, 80), e.getMessage());
            return Optional.empty();
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    public record NominatimHit(double lat, double lon, String displayName, String city, String country) {}
}
