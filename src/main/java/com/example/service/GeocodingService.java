package com.example.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Calls the free Nominatim geocoding API (OpenStreetMap) to convert a
 * place name or address into geographic coordinates.
 *
 * API:  https://nominatim.openstreetmap.org/search?q=...&format=json
 *
 * No API key required — Nominatim is free and open.
 */
public class GeocodingService {

    private static final String NOMINATIM_BASE =
        "https://nominatim.openstreetmap.org/search";

    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    // ── Result record ─────────────────────────────────────────────────────────

    /** Holds a geocoded coordinate pair. */
    public record GeoPoint(double lat, double lon, String displayName) {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Geocodes a free-text address into the first matching coordinate.
     *
     * <b>Blocking call</b> — run on a background thread.
     *
     * @param address  e.g. "Indiranagar, Bangalore"
     * @return Optional containing the first result, or empty on failure.
     */
    public Optional<GeoPoint> geocode(String address) {
        String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);
        String url = NOMINATIM_BASE + "?q=" + encoded + "&format=json&limit=1";

        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                // Nominatim requires a meaningful User-Agent
                .header("User-Agent", "GuardianElite-JavaFX/1.0 contact@guardianelite.app")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                System.err.println("[GeocodingService] HTTP " + resp.statusCode());
                return Optional.empty();
            }

            return parseFirstResult(resp.body());

        } catch (Exception ex) {
            System.err.println("[GeocodingService] Error: " + ex.getMessage());
            return Optional.empty();
        }
    }

    // ── Parser ────────────────────────────────────────────────────────────────

    /**
     * Extracts lat, lon and display_name from the first JSON object in a
     * Nominatim response array, without a JSON library.
     */
    private Optional<GeoPoint> parseFirstResult(String json) {
        // The response is a JSON array; grab the first object's lat/lon/display_name
        double lat  = extractJsonDouble(json, "\"lat\"");
        double lon  = extractJsonDouble(json, "\"lon\"");
        String name = extractJsonString(json, "\"display_name\"");

        if (lat == 0.0 && lon == 0.0) return Optional.empty();
        return Optional.of(new GeoPoint(lat, lon, name));
    }

    /** Extracts a quoted numeric string value following {@code key}. */
    private double extractJsonDouble(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return 0.0;
        // Value is a quoted string: "lat":"12.9716"
        int colon = json.indexOf(':', idx + key.length());
        if (colon < 0) return 0.0;
        int q1 = json.indexOf('"', colon + 1);
        int q2 = json.indexOf('"', q1 + 1);
        if (q1 < 0 || q2 < 0) return 0.0;
        try {
            return Double.parseDouble(json.substring(q1 + 1, q2));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /** Extracts a quoted string value following {@code key}. */
    private String extractJsonString(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return "";
        int colon = json.indexOf(':', idx + key.length());
        if (colon < 0) return "";
        int q1 = json.indexOf('"', colon + 1);
        int q2 = json.indexOf('"', q1 + 1);
        if (q1 < 0 || q2 < 0) return "";
        return json.substring(q1 + 1, q2);
    }
}
