package com.example.service;

import javafx.application.Platform;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Caching wrapper around {@link RoutingService} that provides
 * road-based ETA calculation with a 5-second refresh window.
 *
 * Designed for real-time ambulance tracking:
 *   - Synchronous {@code get()} returns cached result if fresh.
 *   - Asynchronous {@code refreshAsync()} always fetches from OSRM
 *     on a daemon thread and invokes the callback on the FX thread.
 */
public class ETAService {

    /** How long a cached result stays valid (ms). */
    private static final long CACHE_MS = 5_000;

    private final RoutingService routingService = new RoutingService();

    private volatile RoutingService.RouteResult lastResult;
    private volatile long                        lastFetchMs = 0;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns a (possibly cached) route result.
     * <b>Blocking</b> — call on a background thread.
     *
     * @param fromLat  Current ambulance latitude
     * @param fromLon  Current ambulance longitude
     * @param toLat    Destination latitude
     * @param toLon    Destination longitude
     */
    public Optional<RoutingService.RouteResult> get(
            double fromLat, double fromLon, double toLat, double toLon) {
        if (isCacheFresh()) return Optional.ofNullable(lastResult);
        return fetchAndCache(fromLat, fromLon, toLat, toLon);
    }

    /**
     * Always fetches a fresh result on a daemon background thread.
     * Invokes {@code callback} on the JavaFX Application Thread.
     *
     * This is the method called every 5 seconds by {@link AmbulanceTracker}.
     */
    public void refreshAsync(double fromLat, double fromLon,
                              double toLat,  double toLon,
                              Consumer<RoutingService.RouteResult> callback) {
        Thread t = new Thread(() -> {
            Optional<RoutingService.RouteResult> result =
                fetchAndCache(fromLat, fromLon, toLat, toLon);
            result.ifPresent(r -> Platform.runLater(() -> callback.accept(r)));
        }, "eta-refresh");
        t.setDaemon(true);
        t.start();
    }

    /** Force-invalidate the cache (e.g., when ambulance jumps position). */
    public void invalidate() {
        lastFetchMs = 0;
    }

    /** Returns the last cached result without triggering a network call. */
    public Optional<RoutingService.RouteResult> getCached() {
        return Optional.ofNullable(lastResult);
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private boolean isCacheFresh() {
        return lastResult != null &&
               (System.currentTimeMillis() - lastFetchMs) < CACHE_MS;
    }

    private Optional<RoutingService.RouteResult> fetchAndCache(
            double fromLat, double fromLon, double toLat, double toLon) {
        Optional<RoutingService.RouteResult> result =
            routingService.getRoute(fromLat, fromLon, toLat, toLon);
        result.ifPresent(r -> {
            lastResult   = r;
            lastFetchMs  = System.currentTimeMillis();
        });
        return result;
    }
}
