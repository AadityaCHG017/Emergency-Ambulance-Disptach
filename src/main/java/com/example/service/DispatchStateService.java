package com.example.service;

/**
 * Application-scoped singleton that tracks whether an ambulance is
 * currently active (dispatched).
 *
 * Guards the EMERGENCY DISPATCH button so that a second tap while
 * an ambulance is already en route shows a warning instead of
 * dispatching again.
 */
public final class DispatchStateService {

    private static DispatchStateService instance;

    /** True while an ambulance dispatch is active. */
    private boolean ambulanceActive = false;

    private DispatchStateService() {}

    public static DispatchStateService getInstance() {
        if (instance == null) {
            instance = new DispatchStateService();
        }
        return instance;
    }

    /** Returns {@code true} if an ambulance is already dispatched. */
    public boolean isAmbulanceActive() {
        return ambulanceActive;
    }

    /** Call when an ambulance is dispatched. */
    public void markActive() {
        ambulanceActive = true;
    }

    /** Call when the dispatch cycle completes or is cancelled. */
    public void clear() {
        ambulanceActive = false;
    }
}
