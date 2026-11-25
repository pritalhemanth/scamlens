package com.scamlens.detector;

import com.scamlens.model.Category;
import com.scamlens.model.DetectionResult;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Parent class for the five phrase-based detectors (INHERITANCE).
 *
 * A child class only passes in its category and its list of phrases
 * (written as regular expressions). This class does the actual work:
 *   1. finds every phrase in the message (case-insensitive, whole words only)
 *   2. ignores a phrase if it is negated, e.g. "Do NOT share your OTP"
 *   3. returns the matched words as evidence
 */
public abstract class KeywordDetector implements Detector {

    /** Words that cancel a warning sign when they come just before it. */
    private static final Pattern NEGATION =
            Pattern.compile("\\b(not|never|don['’]?t|won['’]?t|no need to)\\b", Pattern.CASE_INSENSITIVE);

    /** How many characters before a match are checked for a negation word. */
    private static final int NEGATION_WINDOW = 15;

    private final Category category;          // ENCAPSULATION: private, set once
    private final List<Pattern> patterns;

    protected KeywordDetector(Category category, List<String> phrases) {
        this.category = category;
        // \b = word boundary, so "pin" does not match inside "shopping"
        this.patterns = phrases.stream()
                .map(phrase -> Pattern.compile("\\b(?:" + phrase + ")\\b",
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE))
                .toList();
    }

    @Override
    public Category category() {
        return category;
    }

    @Override
    public Optional<DetectionResult> detect(String message) {
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }

        // FUNCTIONAL style: a stream pipeline that turns patterns into evidence
        List<String> evidence = patterns.stream()
                .flatMap(pattern -> pattern.matcher(message).results())   // every match of every pattern
                .filter(match -> !isNegated(message, match.start()))      // drop "do not share your OTP"
                .map(match -> match.group().toLowerCase())
                .distinct()                                                // same phrase counted once
                .toList();

        if (evidence.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new DetectionResult(category, evidence, category.explanation()));
    }

    /**
     * True if a negation word appears just before the match, in the same clause.
     * "Never share your OTP"                       -> negated   (safe advice)
     * "If you do not pay, your account is blocked" -> NOT negated (the comma ends the clause)
     */
    protected boolean isNegated(String message, int matchStart) {
        String before = message.substring(Math.max(0, matchStart - NEGATION_WINDOW), matchStart);
        String[] clauses = before.split("[.,;:!?\\n]", -1);
        String sameClause = clauses[clauses.length - 1];
        return NEGATION.matcher(sameClause).find();
    }
}
