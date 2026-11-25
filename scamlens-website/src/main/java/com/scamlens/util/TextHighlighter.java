package com.scamlens.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Splits a message into normal and highlighted pieces.
 *
 *   split("Act now and share your OTP", ["act now", "share your otp"])
 *   -> [ "Act now"(highlighted), " and "(normal), "share your OTP"(highlighted) ]
 *
 * This class has no JavaFX code. The GUI just turns each Segment into a styled node.
 */
public final class TextHighlighter {

    public record Segment(String text, boolean highlighted) { }

    private TextHighlighter() { }

    public static List<Segment> split(String message, List<String> phrases) {
        List<Segment> segments = new ArrayList<>();
        if (message == null || message.isEmpty()) {
            return segments;
        }

        // Longest phrases first, so "share your otp" wins over "otp"
        String alternatives = phrases.stream()
                .filter(p -> p != null && !p.isBlank())
                .sorted(Comparator.comparingInt(String::length).reversed())
                .map(Pattern::quote)                       // treat phrases as plain text
                .collect(Collectors.joining("|"));

        if (alternatives.isEmpty()) {
            segments.add(new Segment(message, false));
            return segments;
        }

        Matcher matcher = Pattern.compile(alternatives, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                .matcher(message);
        int last = 0;
        while (matcher.find()) {
            if (matcher.start() > last) {
                segments.add(new Segment(message.substring(last, matcher.start()), false));
            }
            segments.add(new Segment(matcher.group(), true));
            last = matcher.end();
        }
        if (last < message.length()) {
            segments.add(new Segment(message.substring(last), false));
        }
        return segments;
    }
}
