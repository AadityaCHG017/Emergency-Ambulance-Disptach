package com.example.service;

import com.example.model.FirstAidTip;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Provides first-aid tips mapped to medical conditions / symptoms.
 * Used when ETA exceeds the threshold (default 8 minutes) so the user
 * has something actionable while waiting for the ambulance.
 */
public class FirstAidService {

    /** ETA threshold (minutes) above which first-aid tips are displayed. */
    public static final int ETA_THRESHOLD_MINUTES = 8;

    /** Condition → tips mapping. Keys match symptom names used in TriageService. */
    private static final Map<String, FirstAidTip> TIPS_MAP = Map.ofEntries(
        Map.entry("chest pain", new FirstAidTip("Chest Pain", List.of(
            "Keep patient calm and seated — no sudden movement",
            "Loosen any tight clothing around neck and chest",
            "If prescribed, assist patient with their nitro-glycerine",
            "Do NOT give food or water",
            "Be ready to perform CPR if patient becomes unresponsive"
        ))),
        Map.entry("breathing difficulty", new FirstAidTip("Breathing Difficulty", List.of(
            "Help patient sit upright or lean slightly forward",
            "Loosen collar, tie, or anything constricting the chest",
            "Open windows for fresh air",
            "If patient has an inhaler, assist them in using it",
            "Stay calm — anxiety worsens breathing difficulty"
        ))),
        Map.entry("unconscious", new FirstAidTip("Unconscious Patient", List.of(
            "Check for responsiveness — call their name and tap shoulder",
            "Check for normal breathing — look, listen, feel",
            "Place in recovery position (on their side) if breathing",
            "Do NOT give anything by mouth",
            "Begin CPR immediately if not breathing normally"
        ))),
        Map.entry("heavy bleeding", new FirstAidTip("Heavy Bleeding", List.of(
            "Apply firm, continuous pressure to the wound with a clean cloth",
            "Do NOT remove the cloth — add more on top if it soaks through",
            "Elevate the injured limb above heart level if possible",
            "Do NOT apply a tourniquet unless trained to do so",
            "Keep patient warm and still to reduce shock risk"
        ))),
        Map.entry("fracture", new FirstAidTip("Fracture / Broken Bone", List.of(
            "Immobilize the injured area — do NOT try to straighten it",
            "Apply ice wrapped in cloth to reduce swelling (not directly)",
            "Keep patient still and calm",
            "Support the limb with padding or a makeshift splint",
            "Monitor for signs of shock: pale skin, rapid breathing"
        ))),
        Map.entry("high fever", new FirstAidTip("High Fever", List.of(
            "Move patient to a cool, well-ventilated room",
            "Apply cool (not cold) wet cloth to forehead, neck, and armpits",
            "Encourage small sips of water to prevent dehydration",
            "Remove excess clothing or blankets",
            "Do NOT use cold water immersion — can cause shock"
        ))),
        Map.entry("headache", new FirstAidTip("Severe Headache", List.of(
            "Have patient rest in a quiet, dark room",
            "Apply a cold or warm compress to the forehead or neck",
            "Ensure adequate hydration",
            "Avoid screens and bright lights",
            "Watch for vomiting, confusion, or stiff neck — these are serious signs"
        ))),
        Map.entry("cold", new FirstAidTip("Cold / Flu Symptoms", List.of(
            "Ensure patient is warm and comfortable",
            "Encourage fluids — water, warm broths, herbal tea",
            "Rest is the most important treatment",
            "Use saline nasal drops to ease congestion",
            "Monitor temperature; seek care if fever exceeds 103°F (39.4°C)"
        )))
    );

    /**
     * Returns first-aid tips for the most severe matching symptom.
     * Returns the first match found in the priority order below.
     *
     * @param symptoms list of symptom strings (lowercase)
     * @return Optional FirstAidTip for the highest-priority match
     */
    public Optional<FirstAidTip> getTipsForSymptoms(List<String> symptoms) {
        // Priority order mirrors triage severity
        List<String> priority = List.of(
            "unconscious", "chest pain", "heavy bleeding",
            "breathing difficulty", "fracture", "high fever", "headache", "cold"
        );
        for (String key : priority) {
            if (symptoms.contains(key)) {
                return Optional.ofNullable(TIPS_MAP.get(key));
            }
        }
        return Optional.empty();
    }

    /**
     * Returns ALL matching tips across all symptoms.
     *
     * @param symptoms list of symptom strings (lowercase)
     * @return list of matching FirstAidTip objects (may be empty)
     */
    public List<FirstAidTip> getAllTipsForSymptoms(List<String> symptoms) {
        return symptoms.stream()
                .distinct()
                .map(TIPS_MAP::get)
                .filter(t -> t != null)
                .toList();
    }

    /**
     * Returns true if ETA is high enough that first-aid tips should be shown.
     *
     * @param etaMinutes estimated arrival time in minutes
     */
    public boolean shouldShowFirstAid(int etaMinutes) {
        return etaMinutes > ETA_THRESHOLD_MINUTES;
    }
}
