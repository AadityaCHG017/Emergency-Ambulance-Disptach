package com.example.model;

/**
 * Represents an ambulance unit in the dispatch system.
 *
 * Retains the original simulated (x, y) position used by the internal
 * distance-calculation engine, and adds real geographic (lat, lon)
 * coordinates for Leaflet.js map display.
 */
public class Ambulance {

    private final String id;
    private final String numberPlate;
    private final String driverName;
    private final String driverPhone;

    /** Simulated position for internal ETA calculations. */
    private double x;
    private double y;

    /** Real-world position for map display. */
    private double lat;
    private double lon;

    /** Whether this ambulance is available for dispatch. */
    private boolean available;

    // ── Constructors ──────────────────────────────────────────────────────────

    /**
     * Full constructor with real geographic coordinates.
     */
    public Ambulance(String id, String numberPlate, String driverName,
                     String driverPhone, double x, double y,
                     double lat, double lon) {
        this.id          = id;
        this.numberPlate = numberPlate;
        this.driverName  = driverName;
        this.driverPhone = driverPhone;
        this.x           = x;
        this.y           = y;
        this.lat         = lat;
        this.lon         = lon;
        this.available   = true;
    }

    /**
     * Legacy 6-arg constructor (x, y only) — defaults lat/lon to Bangalore centre.
     * Preserved so any existing call-sites compile without changes.
     */
    public Ambulance(String id, String numberPlate, String driverName,
                     String driverPhone, double x, double y) {
        this(id, numberPlate, driverName, driverPhone, x, y, 12.9716, 77.5946);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String  getId()          { return id; }
    public String  getNumberPlate() { return numberPlate; }
    public String  getDriverName()  { return driverName; }
    public String  getDriverPhone() { return driverPhone; }
    public double  getX()           { return x; }
    public double  getY()           { return y; }
    public double  getLat()         { return lat; }
    public double  getLon()         { return lon; }
    public boolean isAvailable()    { return available; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setX(double x)                  { this.x = x; }
    public void setY(double y)                  { this.y = y; }
    public void setLat(double lat)              { this.lat = lat; }
    public void setLon(double lon)              { this.lon = lon; }
    public void setAvailable(boolean available) { this.available = available; }

    // ── Display helpers ───────────────────────────────────────────────────────

    /** Returns a display-friendly simulated position string. */
    public String positionDisplay() {
        return String.format("(%.1f, %.1f)", x, y);
    }
}
