package com.scamlens.detector;

import com.scamlens.model.Category;
import com.scamlens.model.DetectionResult;

import java.util.Optional;

/**
 * The contract every detector follows (ABSTRACTION).
 *
 * MessageAnalyzer only knows this interface, not the concrete classes,
 * so a new detector can be added without changing the analyzer.
 */
public interface Detector {

    /** Which category this detector is responsible for. */
    Category category();

    /**
     * Checks a message.
     * @return a DetectionResult if warning signs were found, otherwise Optional.empty()
     */
    Optional<DetectionResult> detect(String message);
}
