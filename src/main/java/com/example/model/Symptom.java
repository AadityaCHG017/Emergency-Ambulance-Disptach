package com.example.model;

/**
 * Represents a single symptom in the triage system.
 * Uses Java 21 record for immutability and conciseness.
 */
public record Symptom(String name, String category) {

    // Convenience constructor — category defaults to "general"
    public Symptom(String name) {
        this(name, "general");
    }
}
