package com.example.service;

import com.example.model.User;

/**
 * In-memory session store — holds the currently logged-in user for the
 * lifetime of the application process.
 *
 * Singleton: use {@code UserSessionManager.getInstance()}.
 */
public final class UserSessionManager {

    private static volatile UserSessionManager instance;

    private User    currentUser;
    private boolean loggedIn = false;

    private UserSessionManager() {}

    /** Thread-safe double-checked singleton accessor. */
    public static UserSessionManager getInstance() {
        if (instance == null) {
            synchronized (UserSessionManager.class) {
                if (instance == null) {
                    instance = new UserSessionManager();
                }
            }
        }
        return instance;
    }

    // ── Session operations ────────────────────────────────────────────────────

    /** Stores {@code user} as the active session user. */
    public void login(User user) {
        user.setLastLoginTime(System.currentTimeMillis());
        this.currentUser = user;
        this.loggedIn    = true;
    }

    /** Clears the active session. */
    public void logout() {
        this.currentUser = null;
        this.loggedIn    = false;
    }

    /** Updates the in-memory profile without changing login state. */
    public void updateProfile(User user) {
        this.currentUser = user;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public User    getCurrentUser() { return currentUser; }
    public boolean isLoggedIn()     { return loggedIn; }

    /**
     * Returns the current user's display location, or a sensible default if no
     * user is logged in.
     */
    public String locationDisplay() {
        if (currentUser != null && currentUser.getLocation() != null
                && !currentUser.getLocation().isBlank()) {
            return currentUser.getLocation();
        }
        return "Sector 17, Chandigarh";
    }

    /**
     * Returns the current user's simulated X coordinate (for dispatch),
     * defaulting to the fixed system user location if no user is logged in.
     */
    public double userX() {
        return (currentUser != null) ? currentUser.getX() : 50.0;
    }

    /** @see #userX() */
    public double userY() {
        return (currentUser != null) ? currentUser.getY() : 50.0;
    }
}
