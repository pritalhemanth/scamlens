package com.scamlens.detector;

import com.scamlens.model.Category;

import java.util.List;

/** Finds pressure to act quickly: "urgent", "act now", "within 24 hours"... */
public class UrgencyDetector extends KeywordDetector {

    public UrgencyDetector() {
        super(Category.URGENCY, List.of(
                "urgent(?:ly)?",
                "immediately",
                "act (?:now|fast|quickly)",
                "right away",
                "as soon as possible",
                "asap",
                "hurry",
                "within \\d+ ?(?:hours?|hrs?|minutes?|mins?|days?)",
                "last (?:chance|warning|reminder|date)",
                "final (?:notice|warning|reminder)",
                "expires? (?:today|tonight|soon)",
                "today only",
                "limited time"
        ));
    }
}
