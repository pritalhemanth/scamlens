package com.scamlens.web;

import com.scamlens.model.RiskLevel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Small helpers for building HTML safely.
 *
 * SECURITY: every piece of text that came from a user goes through escape()
 * before it is put into a page. "<script>" becomes "&lt;script&gt;", so the
 * browser shows it as text and never runs it. (PRD requirement: treat input
 * as untrusted and never execute embedded scripts.)
 */
public final class Html {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    private Html() { }

    /** Makes text safe to put inside HTML. */
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder safe = new StringBuilder(text.length() + 16);
        for (char c : text.toCharArray()) {
            switch (c) {
                case '&'  -> safe.append("&amp;");
                case '<'  -> safe.append("&lt;");
                case '>'  -> safe.append("&gt;");
                case '"'  -> safe.append("&quot;");
                case '\'' -> safe.append("&#39;");
                default   -> safe.append(c);
            }
        }
        return safe.toString();
    }

    /** The risk level as a coloured dot and its name, e.g. "High risk" in red. */
    public static String badge(RiskLevel level) {
        return "<span class=\"level " + level.cssClass() + "\">" + escape(level.label()) + "</span>";
    }

    /** "24 Nov 2026, 10:40 PM" */
    public static String date(LocalDateTime time) {
        return escape(time.format(DATE_FORMAT));
    }
}
