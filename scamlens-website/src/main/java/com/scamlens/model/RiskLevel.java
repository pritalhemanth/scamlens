package com.scamlens.model;

/**
 * The three risk levels from PRD section 10.2.
 *
 *    0 – 30  -> LOW
 *   31 – 60  -> SUSPICIOUS
 *   61 – 100 -> HIGH
 */
public enum RiskLevel {

    LOW("Low risk",
            "No strong scam pattern was found. Still be careful with unexpected messages."),
    SUSPICIOUS("Suspicious",
            "Some warning signs were found. Check with the sender through an official channel before you act."),
    HIGH("High risk",
            "Several strong scam signs were found. Do not reply, click links, share details or pay.");

    private final String label;
    private final String summary;

    RiskLevel(String label, String summary) {
        this.label = label;
        this.summary = summary;
    }

    public String label()   { return label; }
    public String summary() { return summary; }

    /** CSS class used by the GUI to colour this level (green / amber / red). */
    public String cssClass() {
        return "risk-" + name().toLowerCase();
    }

    /** Converts a score (0–100) into a risk level. */
    public static RiskLevel fromScore(int score) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("Score must be between 0 and 100, but was " + score);
        }
        if (score >= 61) return HIGH;
        if (score >= 31) return SUSPICIOUS;
        return LOW;
    }
}
