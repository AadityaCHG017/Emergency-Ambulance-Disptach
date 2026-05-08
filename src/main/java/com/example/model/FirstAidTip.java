package com.example.model;

import java.util.List;

/**
 * Maps a condition keyword to a list of first-aid tips.
 * Uses Java 21 record for immutability.
 */
public record FirstAidTip(String condition, List<String> tips) {

    /** Returns formatted bullet-point tips. */
    public String tipsDisplay() {
        StringBuilder sb = new StringBuilder();
        for (String tip : tips) {
            sb.append("• ").append(tip).append("\n");
        }
        return sb.toString().trim();
    }
}
