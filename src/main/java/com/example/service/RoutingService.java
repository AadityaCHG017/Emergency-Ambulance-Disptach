package com.example.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Calls the free OSRM public API for driving routes and ETAs.
 *
 * API:
 *   https://router.project-osrm.org/route/v1/driving/{lon1},{lat1};{lon2},{lat2}
 *   ?overview=full&geometries=geojson
 *
 * No API key required.
 */
public class RoutingService {

    private static final String OSRM_BASE =
        "https://router.project-osrm.org/route/v1/driving/";

    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    // ── Result record ─────────────────────────────────────────────────────────

    /**
     * Holds all data from a single routing query.
     *
     * @param coordsJson   Raw GeoJSON coords string — pass to {@code MapService.drawRoute()}.
     * @param distanceKm   Road distance in km.
     * @param etaMinutes   Estimated driving time in minutes.
     * @param waypoints    Parsed list of [lat, lon] pairs along the route — used by
     *                     {@link AmbulanceTracker} for real-time movement simulation.
     */
    public record RouteResult(
        String         coordsJson,
        double         distanceKm,
        int            etaMinutes,
        List<double[]> waypoints   // each element: {lat, lon}
    ) {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Fetches the driving route from (lat1,lon1) to (lat2,lon2).
     * <b>Blocking</b> — always call on a background thread.
     */
    public Optional<RouteResult> getRoute(double lat1, double lon1,
                                          double lat2, double lon2) {
        // OSRM expects lon,lat order
        String url = String.format(
            "%s%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson",
            OSRM_BASE, lon1, lat1, lon2, lat2
        );

        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "GuardianElite-JavaFX/1.0")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

            HttpResponse<String> resp = http.send(req,
                HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                System.err.println("[RoutingService] HTTP " + resp.statusCode());
                return Optional.empty();
            }

            return parseResponse(resp.body());

        } catch (Exception ex) {
            System.err.println("[RoutingService] " + ex.getMessage());
            return Optional.empty();
        }
    }

    // ── Parser ────────────────────────────────────────────────────────────────

    private Optional<RouteResult> parseResponse(String json) {
        // Extract coordinates array: "coordinates":[[lon,lat],...]
        Pattern coordPat = Pattern.compile(
            "\"coordinates\":\\s*(\\[\\[.*?\\]\\])", Pattern.DOTALL);
        Matcher m = coordPat.matcher(json);
        if (!m.find()) return Optional.empty();

        String coordsJson  = m.group(1);
        double distanceKm  = extractDouble(json, "\"distance\"") / 1000.0;
        double durationSec = extractDouble(json, "\"duration\"");
        int    etaMinutes  = Math.max(1, (int) Math.ceil(durationSec / 60.0));

        List<double[]> waypoints = parseWaypoints(coordsJson);

        return Optional.of(new RouteResult(coordsJson, distanceKm, etaMinutes, waypoints));
    }

    /**
     * Parses "[[lon,lat],[lon,lat],...]" into a List of {lat, lon} double arrays.
     * Uses regex to extract each coordinate pair efficiently.
     */
    private List<double[]> parseWaypoints(String coordsJson) {
        List<double[]> result = new ArrayList<>();
        // Each pair is [lon, lat] — match numbers inside brackets
        Pattern pair = Pattern.compile(
            "\\[\\s*(-?\\d+\\.?\\d*)\\s*,\\s*(-?\\d+\\.?\\d*)\\s*\\]");
        Matcher m = pair.matcher(coordsJson);
        while (m.find()) {
            double lon = Double.parseDouble(m.group(1));
            double lat = Double.parseDouble(m.group(2));
            result.add(new double[]{lat, lon}); // store as [lat, lon]
        }
        return result;
    }

    /** Extracts the first numeric value following {@code key}. */
    private double extractDouble(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return 0;
        int start = idx + key.length();
        while (start < json.length() &&
               (json.charAt(start) == ':' || json.charAt(start) == ' ')) start++;
        int end = start;
        while (end < json.length() &&
               (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) end++;
        try { return Double.parseDouble(json.substring(start, end)); }
        catch (NumberFormatException e) { return 0; }
    }
}
