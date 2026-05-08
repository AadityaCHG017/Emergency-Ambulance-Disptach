package com.example.service;

import com.example.model.DispatchState;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Application-scoped singleton that owns the current {@link DispatchState}
 * and the active {@link AmbulanceTracker}.
 *
 * <h3>Design</h3>
 * <ul>
 *   <li>State is stored in a JavaFX {@link ObjectProperty} so any controller
 *       can add a listener via {@link #stateProperty()} and react without
 *       polling or scene reloads.</li>
 *   <li>All transitions must go through {@link #transition(DispatchState)}
 *       which enforces ordering and dispatches listeners on the FX thread.</li>
 *   <li>The active {@link AmbulanceTracker} is held here so it survives
 *       scene navigation (DispatchController → DispatchResultController)
 *       and keeps reporting proximity events.</li>
 * </ul>
 *
 * <h3>Transition rules</h3>
 * <pre>
 *   IDLE → DISPATCHED → EN_ROUTE → ARRIVED_AT_PATIENT
 *       → PATIENT_PICKED_UP → EN_ROUTE_TO_HOSPITAL → COMPLETED → IDLE
 * </pre>
 *
 * Arrival is automatically detected when the ambulance is within
 * {@value #ARRIVAL_THRESHOLD_METERS} metres of the patient.
 */
public final class DispatchStateManager {

    /** Distance (metres) below which arrival is auto-detected. */
    public static final double ARRIVAL_THRESHOLD_METERS = 50.0;

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static DispatchStateManager instance;

    public static synchronized DispatchStateManager getInstance() {
        if (instance == null) instance = new DispatchStateManager();
        return instance;
    }

    private DispatchStateManager() {}

    // ── State ─────────────────────────────────────────────────────────────────

    private final ObjectProperty<DispatchState> state =
        new SimpleObjectProperty<>(DispatchState.IDLE);

    /** Observable state — add a listener to react to changes on the FX thread. */
    public ObjectProperty<DispatchState> stateProperty() { return state; }

    /** Returns the current dispatch state. */
    public DispatchState getState() { return state.get(); }

    // ── Active tracker ────────────────────────────────────────────────────────

    private AmbulanceTracker activeTracker;

    public void setActiveTracker(AmbulanceTracker t) { activeTracker = t; }
    public AmbulanceTracker getActiveTracker()        { return activeTracker; }

    // ── External state listeners ──────────────────────────────────────────────

    /** Additional raw listeners (used by DispatchResultController to wire UI). */
    private final List<Consumer<DispatchState>> externalListeners = new ArrayList<>();

    public void addStateListener(Consumer<DispatchState> listener) {
        externalListeners.add(listener);
        // Immediately fire with current state so late-joining controllers sync up
        Platform.runLater(() -> listener.accept(getState()));
    }

    public void removeStateListener(Consumer<DispatchState> listener) {
        externalListeners.remove(listener);
    }

    // ── Transition API ────────────────────────────────────────────────────────

    /**
     * Transitions to {@code next} if the move is legal, then notifies all
     * listeners on the JavaFX Application Thread.
     *
     * Illegal transitions (e.g. going backwards) are silently ignored so
     * race conditions between the background tracker and UI interactions
     * don't cause crashes.
     */
    public void transition(DispatchState next) {
        DispatchState current = getState();

        if (!isLegal(current, next)) {
            System.err.printf("[DispatchStateManager] Blocked: %s → %s%n", current, next);
            return;
        }

        Runnable update = () -> {
            state.set(next);
            externalListeners.forEach(l -> l.accept(next));
            System.out.printf("[DispatchStateManager] %s → %s%n", current, next);
        };

        if (Platform.isFxApplicationThread()) update.run();
        else Platform.runLater(update);
    }

    /**
     * Resets to IDLE and stops any active tracker.
     * Call this when the user navigates Home or the dispatch is cancelled.
     */
    public void reset() {
        if (activeTracker != null) {
            activeTracker.stop();
            activeTracker = null;
        }
        Runnable update = () -> {
            state.set(DispatchState.IDLE);
            externalListeners.forEach(l -> l.accept(DispatchState.IDLE));
        };
        if (Platform.isFxApplicationThread()) update.run();
        else Platform.runLater(update);
    }

    // ── Convenience predicates ────────────────────────────────────────────────

    /** Returns true if hospital navigation should currently be enabled. */
    public boolean isHospitalUnlocked() {
        return getState().isHospitalUnlocked();
    }

    // ── Transition legality ───────────────────────────────────────────────────

    private boolean isLegal(DispatchState from, DispatchState to) {
        if (to == DispatchState.IDLE) return true; // always allow reset
        return switch (from) {
            case IDLE                -> to == DispatchState.DISPATCHED;
            case DISPATCHED          -> to == DispatchState.EN_ROUTE;
            case EN_ROUTE            -> to == DispatchState.ARRIVED_AT_PATIENT;
            case ARRIVED_AT_PATIENT  -> to == DispatchState.PATIENT_PICKED_UP;
            case PATIENT_PICKED_UP   -> to == DispatchState.EN_ROUTE_TO_HOSPITAL;
            case EN_ROUTE_TO_HOSPITAL-> to == DispatchState.COMPLETED;
            case COMPLETED           -> to == DispatchState.IDLE;
        };
    }
}
