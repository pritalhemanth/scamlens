package com.scamlens.analyzer;

import com.scamlens.model.Category;
import com.scamlens.model.DetectionResult;
import com.scamlens.model.RiskLevel;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turns detections into a score from 0 to 100.
 *
 * Rules:
 *   1. Each category counts ONCE, even if many of its phrases were found.
 *   2. Add up the points of the categories found.
 *   3. Credential request + (suspicious link OR money request) is the classic
 *      phishing pattern, so the score is raised to at least 65 (HIGH).
 *   4. The score never goes above 100.
 *
 * The score is a heuristic warning level, not a probability or proof of fraud.
 */
public class RiskScorer {

    public static final int MAX_SCORE = 100;
    public static final int PHISHING_COMBO_MINIMUM = 65;

    public int calculateScore(List<DetectionResult> detections) {
        // A Set removes duplicates, so each category is counted once
        Set<Category> found = detections.stream()
                .map(DetectionResult::category)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Category.class)));

        int score = found.stream()
                .mapToInt(Category::points)
                .sum();

        boolean phishingCombo = found.contains(Category.CREDENTIAL)
                && (found.contains(Category.URL) || found.contains(Category.FINANCIAL));
        if (phishingCombo) {
            score = Math.max(score, PHISHING_COMBO_MINIMUM);
        }

        return Math.min(score, MAX_SCORE);
    }

    public RiskLevel classify(int score) {
        return RiskLevel.fromScore(score);
    }
}
