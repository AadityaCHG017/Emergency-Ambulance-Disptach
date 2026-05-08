package com.example.service;

import javafx.application.Platform;
import javafx.scene.web.WebEngine;

/**
 * Thin wrapper around a {@link WebEngine} that provides type-safe,
 * thread-safe helpers for calling JavaScript functions defined in
 * {@code map.html}.
 *
 * All {@code executeScript} calls are dispatched on the JavaFX
 * Application Thread via {@link Platform#runLater}.
 */
public class MapService {

    private WebEngine engine;

    /** Attach the WebEngine after the WebView has been created. */
    public void setEngine(WebEngine engine) {
        this.engine = engine;
    }

    // ── Public helpers ────────────────────────────────────────────────────────

    /** Move/place the ambulance marker on the map. */
    public void updateAmbulance(double lat, double lon, String popupHtml) {
        exec("updateAmbulance(%f, %f, %s)", lat, lon, jsStr(popupHtml));
    }

    /** Move/place the patient (user) marker on the map. */
    public void updatePatient(double lat, double lon, String popupHtml) {
        exec("updatePatient(%f, %f, %s)", lat, lon, jsStr(popupHtml));
    }

    /** Move/place the hospital destination marker on the map. */
    public void updateHospital(double lat, double lon, String popupHtml) {
        exec("updateHospital(%f, %f, %s)", lat, lon, jsStr(popupHtml));
    }

    /**
     * Draws a route polyline.
     *
     * @param coordsJson  JSON array of [lon, lat] pairs from OSRM,
     *                    e.g. {@code "[[77.60,12.97],[77.61,12.98]]"}
     */
    public void drawRoute(String coordsJson) {
        // Wrap the raw JSON in single-quotes so it becomes a JS string literal
        exec("drawRoute('%s')", coordsJson.replace("'", "\\'"));
    }

    /** Remove the route polyline. */
    public void clearRoute() {
        exec("clearRoute()");
    }

    /**
     * Updates the overlay info panel (top-right corner of the map).
     *
     * @param distance  e.g. "4.2 km"
     * @param eta       e.g. "8 min"
     * @param status    e.g. "Ambulance en route"
     */
    public void updateInfoPanel(String distance, String eta, String status) {
        exec("updateInfoPanel(%s, %s, %s)", jsStr(distance), jsStr(eta), jsStr(status));
    }

    /**
     * Updates ETA display — semantic alias for {@link #updateInfoPanel}.
     * Exposed as a distinct method to satisfy the JavaScript bridge contract
     * ({@code updateETA()} in map.html).
     *
     * @param distance  road distance string, e.g. "3.1 km"
     * @param eta       time string, e.g. "6 min"
     * @param status    status label, e.g. "🚑 En route"
     */
    public void updateETA(String distance, String eta, String status) {
        exec("updateETA(%s, %s, %s)", jsStr(distance), jsStr(eta), jsStr(status));
    }

    /** Pan + zoom the map to a given coordinate. */
    public void setCenter(double lat, double lon, int zoom) {
        exec("setCenter(%f, %f, %d)", lat, lon, zoom);
    }

    /** Auto-fit the viewport to show all placed markers. */
    public void fitAll() {
        exec("fitAll()");
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /**
     * Formats a script string and schedules it on the FX Application Thread.
     * Safe to call from any thread.
     */
    private void exec(String template, Object... args) {
        if (engine == null) return;
        String script = String.format(template, args);
        if (Platform.isFxApplicationThread()) {
            run(script);
        } else {
            Platform.runLater(() -> run(script));
        }
    }

    private void run(String script) {
        try {
            engine.executeScript(script);
        } catch (Exception ex) {
            System.err.println("[MapService] executeScript error: " + ex.getMessage());
        }
    }

    /**
     * Wraps a Java string as a JavaScript single-quoted string literal,
     * escaping internal single-quotes and newlines.
     */
    private static String jsStr(String value) {
        if (value == null) return "null";
        return "'" + value
                .replace("\\", "\\\\")
                .replace("'",  "\\'")
                .replace("\n", "\\n")
                .replace("\r", "")
            + "'";
    }
}
