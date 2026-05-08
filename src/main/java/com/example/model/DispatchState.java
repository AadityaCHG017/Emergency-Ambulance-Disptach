package com.example.model;

/**
 * Ordered states of a single emergency ambulance dispatch cycle.
 *
 * Valid transitions:
 *
 *   IDLE → DISPATCHED → EN_ROUTE → ARRIVED_AT_PATIENT
 *       → PATIENT_PICKED_UP → EN_ROUTE_TO_HOSPITAL → COMPLETED
 *
 * Any state can transition back to IDLE (dispatch cancelled / reset).
 */
public enum DispatchState {

    /** No active dispatch. */
    IDLE,

    /** Ambulance has been assigned and will start moving. */
    DISPATCHED,

    /** Ambulance is moving toward the patient location. */
    EN_ROUTE,

    /**
     * Ambulance is within {@value com.example.service.DispatchStateManager#ARRIVAL_THRESHOLD_METERS}
     * metres of the patient — automatically detected by {@link com.example.service.AmbulanceTracker}.
     */
    ARRIVED_AT_PATIENT,

    /**
     * User confirmed the pickup via the UI button.
     * Hospital selection is now unlocked.
     */
    PATIENT_PICKED_UP,

    /** Ambulance + patient are travelling to the selected hospital. */
    EN_ROUTE_TO_HOSPITAL,

    /** Arrived at hospital — dispatch cycle complete. */
    COMPLETED;

    // ── Convenience predicates ────────────────────────────────────────────────

    /** Returns true if hospital navigation should be enabled. */
    public boolean isHospitalUnlocked() {
        return this == PATIENT_PICKED_UP
            || this == EN_ROUTE_TO_HOSPITAL
            || this == COMPLETED;
    }

    /** Returns true if an active dispatch cycle is running. */
    public boolean isActive() {
        return this != IDLE && this != COMPLETED;
    }
}
