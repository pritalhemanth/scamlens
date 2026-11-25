package com.scamlens.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The final result of analysing one message.
 *
 * The original message is kept here only so the GUI can highlight it.
 * It is never written to the database.
 */
public record AnalysisResult(String message,
                             int score,
                             RiskLevel riskLevel,
                             List<DetectionResult> detections,
                             List<String> recommendations,
                             LocalDateTime analysedAt) {

    public AnalysisResult {
        detections = List.copyOf(detections);
        recommendations = List.copyOf(recommendations);
    }

    /** The categories that were detected, e.g. [URGENCY, URL]. */
    public List<Category> categories() {
        return detections.stream()
                .map(DetectionResult::category)
                .toList();
    }

    /** Every piece of evidence from every detector, used to highlight the message. */
    public List<String> allEvidence() {
        return detections.stream()
                .flatMap(d -> d.evidence().stream())
                .distinct()
                .toList();
    }

    public boolean hasIndicators() {
        return !detections.isEmpty();
    }
}
