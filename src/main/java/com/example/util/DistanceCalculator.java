package com.example.util;

/**
 * Utility class for geographic distance calculations.
 * Uses Euclidean distance on simulated (x, y) coordinates.
 */
public final class DistanceCalculator {

    // Simulated fixed user location coordinates
    public static final double USER_X = 50.0;
    public static final double USER_Y = 50.0;

    // Prevent instantiation
    private DistanceCalculator() {}

    /**
     * Calculates Euclidean distance from the fixed user location to (x, y).
     *
     * @param x target x-coordinate
     * @param y target y-coordinate
     * @return Euclidean distance
     */
    public static double distanceFromUser(double x, double y) {
        double dx = USER_X - x;
        double dy = USER_Y - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Simulates an ETA in minutes based on distance (1 unit ≈ 1 minute).
     *
     * @param distance calculated distance
     * @return estimated minutes rounded to nearest integer
     */
    public static int estimatedETA(double distance) {
        return (int) Math.round(distance);
    }
}
