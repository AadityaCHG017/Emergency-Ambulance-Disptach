package com.example.model;

import java.util.List;

/**
 * Represents a hospital with both simulated (x, y) coordinates (used by
 * the internal triage/distance engine) AND real geographic (lat, lon)
 * coordinates (used by the Leaflet.js map and OSRM routing).
 *
 * Backward-compatible 4-arg constructor is preserved for any legacy callers.
 */
public record Hospital(
        String       name,
        double       x,
        double       y,
        List<String> facilities,
        String       phone,
        float        rating,
        int          bedsAvailable,
        String       address,
        double       lat,          // Real-world latitude
        double       lon           // Real-world longitude
) {

    /**
     * Legacy 4-arg constructor — uses safe defaults for all extended fields.
     * Existing callers that omit the new fields still compile unchanged.
     */
    public Hospital(String name, double x, double y, List<String> facilities) {
        this(name, x, y, facilities, "N/A", 4.0f, 20, "Bangalore", 12.9716, 77.5946);
    }

    /**
     * 8-arg constructor (pre-map era) — defaults lat/lon to Bangalore centre.
     */
    public Hospital(String name, double x, double y, List<String> facilities,
                    String phone, float rating, int bedsAvailable, String address) {
        this(name, x, y, facilities, phone, rating, bedsAvailable, address,
             12.9716, 77.5946);
    }

    // ── Display helpers ───────────────────────────────────────────────────────

    /** Comma-separated list of facility tags, e.g. "ICU, Cardiology, Emergency". */
    public String facilitiesDisplay() {
        return String.join(", ", facilities);
    }

    /** Star string for rating, e.g. "★★★★½  4.5". */
    public String ratingDisplay() {
        int full     = (int) rating;
        boolean half = (rating - full) >= 0.5f;
        return "★".repeat(full) + (half ? "½" : "") + "  " + String.format("%.1f", rating);
    }
}
