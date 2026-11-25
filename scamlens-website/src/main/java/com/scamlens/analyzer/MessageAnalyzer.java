package com.scamlens.analyzer;

import com.scamlens.detector.CredentialDetector;
import com.scamlens.detector.Detector;
import com.scamlens.detector.FinancialDetector;
import com.scamlens.detector.RewardDetector;
import com.scamlens.detector.ThreatDetector;
import com.scamlens.detector.URLAnalyzer;
import com.scamlens.detector.UrgencyDetector;
import com.scamlens.model.AnalysisResult;
import com.scamlens.model.DetectionResult;
import com.scamlens.model.RiskLevel;
import com.scamlens.util.InputValidator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The main engine. It does not detect anything itself; it COORDINATES:
 *
 *   1. validate the input
 *   2. run every detector
 *   3. ask RiskScorer for the score and level
 *   4. build the safety recommendations
 *   5. return one AnalysisResult
 *
 * COMPOSITION: a MessageAnalyzer is built from detectors and a RiskScorer.
 * POLYMORPHISM: it calls detect() on each Detector without knowing its type.
 *
 * No JavaFX and no database code here, so it can be tested on its own.
 */
public class MessageAnalyzer {

    private final List<Detector> detectors;
    private final RiskScorer scorer;

    /** Default setup with all six detectors. */
    public MessageAnalyzer() {
        this(List.of(
                new UrgencyDetector(),
                new ThreatDetector(),
                new RewardDetector(),
                new CredentialDetector(),
                new FinancialDetector(),
                new URLAnalyzer()
        ), new RiskScorer());
    }

    /** Lets tests (or future versions) supply their own detectors. */
    public MessageAnalyzer(List<Detector> detectors, RiskScorer scorer) {
        this.detectors = List.copyOf(detectors);
        this.scorer = scorer;
    }

    /**
     * Analyses one message.
     * @throws IllegalArgumentException if the input is empty, too long or invalid
     */
    public AnalysisResult analyze(String message) {
        // 1. Validate
        InputValidator.validate(message).ifPresent(error -> {
            throw new IllegalArgumentException(error);
        });

        // 2. Run every detector (stream + method reference)
        List<DetectionResult> detections = detectors.stream()
                .map(detector -> detector.detect(message))
                .flatMap(Optional::stream)          // keep only detectors that found something
                .toList();

        // 3. Score and classify
        int score = scorer.calculateScore(detections);
        RiskLevel level = scorer.classify(score);

        // 4. Recommendations: one per detected category, plus the general advice for the level
        List<String> recommendations = Stream.concat(
                        detections.stream().map(d -> d.category().advice()),
                        Stream.of(level.summary()))
                .distinct()
                .toList();

        // 5. Package everything into an immutable result
        return new AnalysisResult(message, score, level, detections, recommendations, LocalDateTime.now());
    }
}
