package com.scamlens.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * One row of scan history, as loaded from the history file.
 *
 * @param preview a short masked preview, or null if the user chose not to save one
 * @param owner   who made the scan: a website visitor's random id, or "desktop"
 */
public record ScanRecord(long id,
                         LocalDateTime scannedAt,
                         int score,
                         RiskLevel riskLevel,
                         List<Category> categories,
                         String preview,
                         String owner) {

    public ScanRecord {
        categories = List.copyOf(categories);
    }

    /** Readable list of categories, e.g. "Urgency, Suspicious link". */
    public String categoryLabels() {
        if (categories.isEmpty()) {
            return "None";
        }
        return categories.stream()
                .map(Category::label)
                .collect(Collectors.joining(", "));
    }
}
