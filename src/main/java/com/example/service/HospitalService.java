package com.example.service;

import com.example.model.Hospital;
import com.example.util.DistanceCalculator;

import java.util.Comparator;
import java.util.List;

/**
 * Manages the hospital registry and suggestion logic.
 *
 * Each hospital now carries both:
 *   • (x, y) simulated coordinates — used by the internal triage engine
 *   • (lat, lon) real Bangalore coordinates — used by the Leaflet.js map
 *
 * Suggestion logic:
 *   - Cardiac symptoms  → prefer Cardiology facilities
 *   - Accident/bleeding → prefer Trauma facilities
 *   - Default           → nearest 3 hospitals by Euclidean distance
 */
public class HospitalService {

    /**
     * Pre-loaded hospital registry.
     *
     * Constructor signature:
     *   Hospital(name, x, y, facilities, phone, rating, beds, address, lat, lon)
     *
     * Real Bangalore lat/lon sourced from well-known landmarks in each locality.
     */
    private static final List<Hospital> ALL_HOSPITALS = List.of(

        new Hospital(
            "City Heart Institute",
            20.0, 25.0,
            List.of("Cardiology", "ICU", "Emergency"),
            "+91-80-41001111", 4.8f, 42,
            "Indiranagar, Bangalore",
            12.9784, 77.6408      // Indiranagar
        ),

        new Hospital(
            "Metro Trauma Center",
            70.0, 30.0,
            List.of("Trauma", "Surgery", "ICU", "Emergency"),
            "+91-80-66625000", 4.7f, 38,
            "Cunningham Road, Bangalore",
            12.9951, 77.5960      // Cunningham Road
        ),

        new Hospital(
            "Sunrise General Hospital",
            50.0, 60.0,
            List.of("General", "Emergency", "Radiology"),
            "+91-80-47555000", 4.5f, 55,
            "Whitefield, Bangalore",
            12.9698, 77.7480      // Whitefield
        ),

        new Hospital(
            "Apollo Neuro & Spine",
            80.0, 80.0,
            List.of("Neurology", "ICU", "Emergency"),
            "+91-80-40611111", 4.6f, 28,
            "Jayanagar, Bangalore",
            12.9259, 77.5936      // Jayanagar 4th Block
        ),

        new Hospital(
            "Green Valley Clinic",
            10.0, 90.0,
            List.of("General", "Cardiology", "Radiology"),
            "+91-80-68911111", 4.4f, 30,
            "Rajajinagar, Bangalore",
            12.9989, 77.5516      // Rajajinagar
        ),

        new Hospital(
            "Westside Trauma Hospital",
            30.0, 10.0,
            List.of("Trauma", "Orthopedics", "Emergency"),
            "+91-80-62992000", 4.5f, 35,
            "Banashankari, Bangalore",
            12.9130, 77.5480      // Banashankari
        ),

        new Hospital(
            "Eastbrook Medical Center",
            90.0, 55.0,
            List.of("General", "Cardiology", "ICU"),
            "+91-80-40501234", 4.3f, 22,
            "HSR Layout, Bangalore",
            12.9081, 77.6476      // HSR Layout
        ),

        new Hospital(
            "Northgate Health Hub",
            55.0, 15.0,
            List.of("General", "Emergency", "Orthopedics"),
            "+91-80-40789000", 4.2f, 18,
            "Hebbal, Bangalore",
            13.0358, 77.5970      // Hebbal
        )
    );

    /** Returns the full preloaded hospital list. */
    public List<Hospital> getAllHospitals() {
        return ALL_HOSPITALS;
    }

    /**
     * Suggests the top-3 most relevant hospitals for the given symptoms.
     *
     * Routing rules:
     *   - cardiac keywords  → prefer Cardiology facilities
     *   - accident/bleeding → prefer Trauma facilities
     *   - fallback          → nearest by simulated distance
     *
     * @param symptoms  list of symptom name strings
     * @param hospitals full hospital list
     * @return top-3 suggested hospitals
     */
    public static List<Hospital> suggestHospitals(List<String> symptoms,
                                                   List<Hospital> hospitals) {
        boolean isCardiac = symptoms.stream().anyMatch(s ->
                s.contains("chest pain") || s.contains("breathing difficulty"));
        boolean isTrauma  = symptoms.stream().anyMatch(s ->
                s.contains("heavy bleeding") || s.contains("accident") || s.contains("fracture"));

        String preferredFacility = isCardiac ? "Cardiology"
                                 : isTrauma  ? "Trauma"
                                 : "General";

        return hospitals.stream()
                .sorted(Comparator
                        .<Hospital, Integer>comparing(h ->
                                h.facilities().stream()
                                 .anyMatch(f -> f.equalsIgnoreCase(preferredFacility)) ? 0 : 1)
                        .thenComparingDouble(h -> DistanceCalculator.distanceFromUser(h.x(), h.y())))
                .limit(3)
                .toList();
    }
}
