package com.example.model;

import java.util.List;

/**
 * Encapsulates the full output of the triage decision engine.
 *
 * level: 1 = Low, 2 = Moderate, 3 = Critical
 */
public final class TriageResult {

    private final int          level;
    private final String       levelLabel;
    private final String       action;
    private final String       explanation;
    private final List<Hospital> suggestedHospitals;

    public TriageResult(int level,
                        String levelLabel,
                        String action,
                        String explanation,
                        List<Hospital> suggestedHospitals) {
        this.level              = level;
        this.levelLabel         = levelLabel;
        this.action             = action;
        this.explanation        = explanation;
        this.suggestedHospitals = suggestedHospitals;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public int          level()              { return level; }
    public String       levelLabel()         { return levelLabel; }
    public String       action()             { return action; }
    public String       explanation()        { return explanation; }
    public List<Hospital> suggestedHospitals() { return suggestedHospitals; }

    /**
     * True when the triage level is Critical (3).
     * Used by ResultController to decide whether to auto-dispatch the ambulance.
     */
    public boolean requiresImmediateDispatch() {
        return level == 3;
    }

    /** Returns the CSS hex colour matching the severity level. */
    public String levelColor() {
        return switch (level) {
            case 3  -> "#E53935"; // Red    — Critical
            case 2  -> "#FB8C00"; // Orange — Moderate
            default -> "#43A047"; // Green  — Low
        };
    }
}
