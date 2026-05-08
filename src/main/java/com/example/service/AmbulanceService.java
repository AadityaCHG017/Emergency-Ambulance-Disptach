package com.example.service;

import com.example.model.Ambulance;
import com.example.util.DistanceCalculator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Manages the ambulance fleet: preloads ambulances, finds the nearest
 * available one, and assigns it to the user's location.
 *
 * Each ambulance now carries both:
 *   • (x, y) simulated coordinates — used by the internal ETA engine
 *   • (lat, lon) real Bangalore coordinates — used by the Leaflet.js map
 */
public class AmbulanceService {

    /** Mutable list — ambulances can be marked unavailable after dispatch. */
    private final List<Ambulance> fleet = new ArrayList<>();

    /**
     * Constructor signature:
     *   Ambulance(id, plate, driver, phone, x, y, lat, lon)
     *
     * Real Bangalore lat/lon approximate to each ambulance's starting zone.
     */
    public AmbulanceService() {
        fleet.add(new Ambulance(
            "AMB-01", "MH 12 AB 1234", "Rajesh Kumar",  "+91-98200-11111",
            20.0, 20.0,
            12.9784, 77.5757   // Malleshwaram
        ));
        fleet.add(new Ambulance(
            "AMB-02", "MH 14 CD 5678", "Priya Sharma",  "+91-98200-22222",
            80.0, 30.0,
            13.0200, 77.6400   // Yelahanka
        ));
        fleet.add(new Ambulance(
            "AMB-03", "MH 04 EF 9012", "Sunil Mehta",   "+91-98200-33333",
            60.0, 80.0,
            12.8800, 77.6400   // BTM Layout
        ));
        fleet.add(new Ambulance(
            "AMB-04", "DL 05 GH 3456", "Anita Desai",   "+91-98200-44444",
            10.0, 70.0,
            12.9200, 77.5000   // Kengeri
        ));
        fleet.add(new Ambulance(
            "AMB-05", "KA 09 IJ 7890", "Vikram Singh",  "+91-98200-55555",
            90.0, 90.0,
            12.8400, 77.6700   // Electronic City
        ));
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns all ambulances (for display / testing). */
    public List<Ambulance> getFleet() {
        return fleet;
    }

    /**
     * Finds the nearest available ambulance to the user's fixed location
     * using simulated (x, y) Euclidean distance.
     */
    public Optional<Ambulance> findNearest() {
        return fleet.stream()
                .filter(Ambulance::isAvailable)
                .min(Comparator.comparingDouble(a ->
                        DistanceCalculator.distanceFromUser(a.getX(), a.getY())));
    }

    /**
     * Marks the given ambulance as unavailable (assigned to this dispatch).
     */
    public void assignAmbulance(Ambulance ambulance) {
        ambulance.setAvailable(false);
    }

    /**
     * Convenience: finds nearest and assigns in one call.
     *
     * @return the dispatched Ambulance, or {@code null} if none available.
     */
    public Ambulance dispatch() {
        Optional<Ambulance> nearest = findNearest();
        nearest.ifPresent(this::assignAmbulance);
        return nearest.orElse(null);
    }

    /**
     * Calculates ETA (minutes) for a given ambulance to reach the user,
     * based on simulated distance at emergency speed.
     */
    public int calculateETA(Ambulance ambulance) {
        double dist = DistanceCalculator.distanceFromUser(ambulance.getX(), ambulance.getY());
        // 1 simulated unit ≈ 0.5 minutes at emergency speed
        return Math.max(1, (int) Math.round(dist * 0.5));
    }

    /** Returns the raw simulated distance (units) from ambulance to user. */
    public double calculateDistance(Ambulance ambulance) {
        return DistanceCalculator.distanceFromUser(ambulance.getX(), ambulance.getY());
    }
}
