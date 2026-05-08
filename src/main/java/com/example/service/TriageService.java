package com.example.service;

import com.example.model.TriageResult;
import com.example.model.Hospital;

import java.util.List;

/**
 * Core rule-based triage engine.
 * Evaluates a list of symptoms and produces a TriageResult.
 *
 * Severity Levels:
 *   Level 3 → Critical
 *   Level 2 → Moderate
 *   Level 1 → Low
 */
public class TriageService {


    /**
     * Analyzes the given symptoms and returns a complete TriageResult.
     *
     * @param symptoms   list of symptom name strings (lowercase)
     * @param hospitals  pre-loaded hospital list passed in from HospitalService
     * @return           TriageResult with level, action, explanation, hospitals
     */
    public TriageResult analyze(List<String> symptoms, List<Hospital> hospitals) {
        int level          = computeLevel(symptoms);
        String label       = levelLabel(level);
        String action      = action(level);
        String explanation = buildExplanation(symptoms, level);

        // Pick relevant hospitals based on symptoms
        List<Hospital> suggested = HospitalService.suggestHospitals(symptoms, hospitals);

        return new TriageResult(level, label, action, explanation, suggested);
    }

    // ─── Level computation ────────────────────────────────────────────────────

    /**
     * Determines the highest severity level triggered by the symptoms.
     * Combination rules are checked first; then individual base rules.
     */
    private int computeLevel(List<String> symptoms) {
        // Combination rules (highest priority)
        if (hasBoth(symptoms, "chest pain", "sweating"))        return 3;
        if (hasBoth(symptoms, "accident", "bleeding"))          return 3;
        if (hasBoth(symptoms, "fever", "headache"))             return 2;

        // Base rules — scan for highest individual severity
        int highest = 0;
        for (String symptom : symptoms) {
            int s = baseSeverity(symptom);
            if (s > highest) highest = s;
        }
        return highest == 0 ? 1 : highest; // default to Low if nothing matched
    }

    /**
     * Maps a single symptom to its base severity level.
     * Uses Java 21 enhanced switch expression.
     */
    private int baseSeverity(String symptom) {
        return switch (symptom.toLowerCase().trim()) {
            case "chest pain",
                 "breathing difficulty",
                 "unconscious",
                 "heavy bleeding"   -> 3; // Critical
            case "fracture",
                 "high fever"       -> 2; // Moderate
            case "headache",
                 "cold"             -> 1; // Low
            default                 -> 0; // Unknown
        };
    }

    /** Checks if the symptom list contains both of the specified symptoms. */
    private boolean hasBoth(List<String> symptoms, String a, String b) {
        return symptoms.contains(a) && symptoms.contains(b);
    }

    // ─── Label, Action ────────────────────────────────────────────────────────

    private String levelLabel(int level) {
        return switch (level) {
            case 3  -> "CRITICAL";
            case 2  -> "MODERATE";
            default -> "LOW";
        };
    }

    private String action(int level) {
        return switch (level) {
            case 3  -> "Call ambulance immediately";
            case 2  -> "Visit hospital urgently";
            default -> "Monitor and consult doctor";
        };
    }

    // ─── Explanation builder ──────────────────────────────────────────────────

    /**
     * Generates a human-readable explanation based on triggered rules.
     */
    private String buildExplanation(List<String> symptoms, int level) {
        StringBuilder sb = new StringBuilder();

        // Combination rule explanations
        if (hasBoth(symptoms, "chest pain", "sweating")) {
            sb.append("Chest pain combined with sweating indicates a possible cardiac emergency. ");
        }
        if (hasBoth(symptoms, "accident", "bleeding")) {
            sb.append("Accident with bleeding indicates a possible trauma emergency. ");
        }
        if (hasBoth(symptoms, "fever", "headache")) {
            sb.append("Fever combined with headache may indicate a systemic infection. ");
        }

        // Individual symptom explanations
        if (symptoms.contains("chest pain") && !hasBoth(symptoms, "chest pain", "sweating")) {
            sb.append("Chest pain is a high-priority symptom that may indicate cardiac issues. ");
        }
        if (symptoms.contains("breathing difficulty")) {
            sb.append("Difficulty breathing requires immediate assessment. ");
        }
        if (symptoms.contains("unconscious")) {
            sb.append("Loss of consciousness is a life-threatening emergency. ");
        }
        if (symptoms.contains("heavy bleeding")) {
            sb.append("Heavy bleeding must be controlled immediately to prevent shock. ");
        }
        if (symptoms.contains("fracture")) {
            sb.append("A fracture requires urgent medical imaging and immobilization. ");
        }
        if (symptoms.contains("high fever")) {
            sb.append("High fever may indicate a serious infection. ");
        }
        if (symptoms.contains("headache") && !hasBoth(symptoms, "fever", "headache")) {
            sb.append("Headache is noted; monitor for worsening symptoms. ");
        }
        if (symptoms.contains("cold")) {
            sb.append("Cold symptoms are generally mild; rest and fluids are recommended. ");
        }

        // Fallback
        if (sb.isEmpty()) {
            sb.append(switch (level) {
                case 3  -> "Critical emergency detected. Immediate medical attention required.";
                case 2  -> "Moderate emergency detected. Prompt medical care is advised.";
                default -> "Symptoms appear mild. Rest and monitor your condition.";
            });
        }

        return sb.toString().trim();
    }
}
