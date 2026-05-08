package com.example.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * Represents the registered user of the application.
 * Stored on disk via {@code UserPersistenceService} so data survives restarts.
 */
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String  userId;
    private String  name;
    private String  phoneNumber;
    private String  location;        // Human-readable address
    private double  x;               // Simulated coordinate (matches DistanceCalculator)
    private double  y;
    private String  emergencyContact;
    private String  bloodGroup;
    private long    lastLoginTime;

    // ── Constructors ──────────────────────────────────────────────────────────

    public User() {}

    /**
     * Full constructor used at registration.
     * userId is auto-generated.
     */
    public User(String name, String phoneNumber, String location, double x, double y) {
        this.userId        = "USER_" + System.currentTimeMillis();
        this.name          = name;
        this.phoneNumber   = phoneNumber;
        this.location      = location;
        this.x             = x;
        this.y             = y;
        this.lastLoginTime = System.currentTimeMillis();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getUserId()              { return userId; }
    public void   setUserId(String v)      { this.userId = v; }

    public String getName()                { return name; }
    public void   setName(String v)        { this.name = v; }

    public String getPhoneNumber()         { return phoneNumber; }
    public void   setPhoneNumber(String v) { this.phoneNumber = v; }

    public String getLocation()            { return location; }
    public void   setLocation(String v)    { this.location = v; }

    public double getX()                   { return x; }
    public void   setX(double v)           { this.x = v; }

    public double getY()                   { return y; }
    public void   setY(double v)           { this.y = v; }

    public String getEmergencyContact()         { return emergencyContact; }
    public void   setEmergencyContact(String v) { this.emergencyContact = v; }

    public String getBloodGroup()          { return bloodGroup; }
    public void   setBloodGroup(String v)  { this.bloodGroup = v; }

    public long   getLastLoginTime()       { return lastLoginTime; }
    public void   setLastLoginTime(long v) { this.lastLoginTime = v; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Display name with blood group if available. */
    public String displayName() {
        if (bloodGroup != null && !bloodGroup.isBlank()) {
            return name + "  (" + bloodGroup + ")";
        }
        return name;
    }

    @Override
    public String toString() {
        return "User{id=" + userId + ", name=" + name + ", phone=" + phoneNumber + "}";
    }
}
