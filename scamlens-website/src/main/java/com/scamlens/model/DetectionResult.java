package com.scamlens.model;

import java.util.List;

/**
 * What ONE detector found in a message.
 *
 * @param category    which kind of warning sign (e.g. URGENCY)
 * @param evidence    the exact words or links that triggered it (used for highlighting)
 * @param explanation a plain-English reason shown to the user
 *
 * A record is immutable: once created it cannot change.
 */
public record DetectionResult(Category category, List<String> evidence, String explanation) {

    public DetectionResult {
        evidence = List.copyOf(evidence);   // defensive, unmodifiable copy
    }
}
