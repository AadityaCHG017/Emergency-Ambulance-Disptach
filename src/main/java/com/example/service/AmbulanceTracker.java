package com.example.service;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Simulates live ambulance movement along a real OSRM road route.
 *
 * <h3>Per-tick behaviour (every 1 second on the FX thread)</h3>
 * <ol>
 *   <li>Advances the ambulance N waypoints along the stored route.</li>
 *   <li>Calls {@link MapService#updateAmbulance} to move the map marker.</li>
 *   <li>Redraws the remaining route segment.</li>
 *   <li>Checks Haversine distance to destination — fires
 *       {@code onProximityReached} once when below threshold.</li>
 *   <li>Every {@value #ETA_REFRESH_TICKS}s: asks {@link ETAService} for a
 *       fresh OSRM ETA and optionally reroutes.</li>
 * </ol>
 *
 * <h3>Offline fallback</h3>
 * If no waypoints are provided, straight-line synthetic waypoints are
 * generated so the UI degrades gracefully without crashes.
 */
public class AmbulanceTracker {

    // ── Configuration ─────────────────────────────────────────────────────────

    private static final double TICK_SECONDS            = 1.0;
    private static final int    ANIMATION_DURATION_TICKS = 60;
    private static final int    ETA_REFRESH_TICKS        = 5;

    // ── Dependencies ──────────────────────────────────────────────────────────

    private final MapService mapService;
    private final ETAService etaService;

    // ── Destination ───────────────────────────────────────────────────────────

    private final double destLat;
    private final double destLon;

    // ── Route state ───────────────────────────────────────────────────────────

    private List<double[]> waypoints      = new ArrayList<>(); // {lat, lon}
    private int            waypointsPerTick = 1;
    private int            currentIndex    = 0;
    private int            tickCount       = 0;

    private double startLat;
    private double startLon;
    private String popupHtml = "🚑";

    // ── Proximity detection ───────────────────────────────────────────────────

    /** Threshold in metres — fires onProximityReached when below this. */
    private double proximityThresholdMeters = 50.0;

    /** Ensures onProximityReached fires only once. */
    private boolean proximityFired = false;

    // ── JavaFX timeline ───────────────────────────────────────────────────────

    private Timeline movementTimeline;

    // ── Callbacks (all invoked on the FX thread) ──────────────────────────────

    private Runnable                   onArrived;
    private Runnable                   onProximityReached; // fires when within threshold
    private BiConsumer<String, String> onETAUpdate;        // (distance, eta)
    private Consumer<double[]>         onPositionUpdate;   // {lat, lon}

    // ── Constructor ───────────────────────────────────────────────────────────

    public AmbulanceTracker(MapService mapService, ETAService etaService,
                             double destLat, double destLon) {
        this.mapService = mapService;
        this.etaService = etaService;
        this.destLat    = destLat;
        this.destLon    = destLon;
    }

    // ── Callback setters ──────────────────────────────────────────────────────

    public void setOnArrived(Runnable cb)                    { this.onArrived = cb; }
    public void setOnProximityReached(Runnable cb)           { this.onProximityReached = cb; }
    public void setProximityThresholdMeters(double meters)   { this.proximityThresholdMeters = meters; }
    public void setOnETAUpdate(BiConsumer<String,String> cb) { this.onETAUpdate = cb; }
    public void setOnPositionUpdate(Consumer<double[]> cb)   { this.onPositionUpdate = cb; }

    // ── Control ───────────────────────────────────────────────────────────────

    /**
     * Starts real-time movement simulation.
     *
     * @param initialWaypoints Parsed waypoints from {@link RoutingService.RouteResult}.
     *                         Empty list triggers straight-line fallback.
     * @param sLat             Ambulance starting latitude.
     * @param sLon             Ambulance starting longitude.
     * @param popup            HTML for the ambulance map marker popup.
     */
    public void startTracking(List<double[]> initialWaypoints,
                               double sLat, double sLon, String popup) {
        stop();
        this.startLat     = sLat;
        this.startLon     = sLon;
        this.popupHtml    = popup;
        this.currentIndex = 0;
        this.tickCount    = 0;
        this.proximityFired = false;

        if (initialWaypoints == null || initialWaypoints.isEmpty()) {
            waypoints = generateStraightLine(sLat, sLon, destLat, destLon, 60);
        } else {
            waypoints = new ArrayList<>(initialWaypoints);
        }

        waypointsPerTick = Math.max(1, waypoints.size() / ANIMATION_DURATION_TICKS);

        movementTimeline = new Timeline(
            new KeyFrame(Duration.seconds(TICK_SECONDS), e -> tick()));
        movementTimeline.setCycleCount(Timeline.INDEFINITE);
        movementTimeline.play();
    }

    /** Stops movement and releases the timeline. */
    public void stop() {
        if (movementTimeline != null) {
            movementTimeline.stop();
            movementTimeline = null;
        }
    }

    public boolean isRunning() { return movementTimeline != null; }

    // ── Tick (FX thread, 1 s interval) ───────────────────────────────────────

    private void tick() {
        tickCount++;

        // 1. Advance position along waypoints
        currentIndex = Math.min(currentIndex + waypointsPerTick, waypoints.size() - 1);
        boolean routeEnd = (currentIndex >= waypoints.size() - 1);

        double[] pos   = waypoints.get(currentIndex);
        double currLat = pos[0];
        double currLon = pos[1];

        // 2. Update ambulance marker on map
        mapService.updateAmbulance(currLat, currLon, popupHtml);

        // 3. Fire position callback
        if (onPositionUpdate != null) {
            onPositionUpdate.accept(new double[]{currLat, currLon});
        }

        // 4. Redraw remaining route segment
        if (!routeEnd && currentIndex < waypoints.size() - 1) {
            List<double[]> remaining = waypoints.subList(currentIndex, waypoints.size());
            mapService.drawRoute(toGeoJsonCoords(remaining));
        }

        // 5. Proximity detection (Haversine distance to destination)
        double distToDestMeters = haversineMeters(currLat, currLon, destLat, destLon);
        if (!proximityFired && distToDestMeters <= proximityThresholdMeters) {
            proximityFired = true;
            if (onProximityReached != null) onProximityReached.run();
        }

        // 6. ETA refresh every 5 ticks from OSRM
        if (!routeEnd && tickCount % ETA_REFRESH_TICKS == 0) {
            final double lat = currLat;
            final double lon = currLon;
            etaService.refreshAsync(lat, lon, destLat, destLon, result -> {
                String distStr = String.format("%.1f km", result.distanceKm());
                String etaStr  = result.etaMinutes() + " min";
                mapService.updateInfoPanel(distStr, etaStr, "🚑  En route");
                mapService.updateETA(distStr, etaStr, "🚑  En route");
                if (onETAUpdate != null) onETAUpdate.accept(distStr, etaStr);

                // Reroute with fresh waypoints
                if (!result.waypoints().isEmpty()) {
                    waypoints        = new ArrayList<>(result.waypoints());
                    currentIndex     = 0;
                    waypointsPerTick = Math.max(1, waypoints.size() / ANIMATION_DURATION_TICKS);
                    mapService.drawRoute(result.coordsJson());
                }
            });
        }

        // 7. Arrival (end of route)
        if (routeEnd) {
            stop();
            mapService.updateAmbulance(destLat, destLon, popupHtml);
            mapService.updateInfoPanel("Arrived", "0 min", "✅  Arrived!");
            mapService.updateETA("0 km", "0 min", "✅  Arrived!");
            if (onArrived != null) onArrived.run();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Haversine formula — returns distance in metres between two lat/lon points.
     */
    public static double haversineMeters(double lat1, double lon1,
                                          double lat2, double lon2) {
        final double R = 6_371_000.0;
        double phi1   = Math.toRadians(lat1);
        double phi2   = Math.toRadians(lat2);
        double dPhi   = Math.toRadians(lat2 - lat1);
        double dLambda= Math.toRadians(lon2 - lon1);
        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
                 + Math.cos(phi1) * Math.cos(phi2)
                 * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Converts a list of [lat, lon] pairs to a GeoJSON coordinates JSON string:
     * {@code [[lon0,lat0],[lon1,lat1],...]}
     */
    private String toGeoJsonCoords(List<double[]> latLonPairs) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < latLonPairs.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("[")
              .append(latLonPairs.get(i)[1]).append(",")
              .append(latLonPairs.get(i)[0]).append("]");
        }
        return sb.append("]").toString();
    }

    /**
     * Generates N evenly-spaced waypoints on a straight line between two points.
     * Used as a fallback when OSRM is unreachable.
     */
    private List<double[]> generateStraightLine(double sLat, double sLon,
                                                  double dLat, double dLon,
                                                  int steps) {
        List<double[]> pts = new ArrayList<>(steps + 1);
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            pts.add(new double[]{sLat + (dLat - sLat) * t, sLon + (dLon - sLon) * t});
        }
        return pts;
    }
}
