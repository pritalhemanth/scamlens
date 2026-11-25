package com.scamlens.util;

import java.util.regex.Pattern;

/**
 * Makes a short, privacy-safe preview of a message for the history table.
 *
 *   "Share OTP 482913 at http://x.xyz now"  ->  "Share OTP ###### at [link] now"
 *
 * Links become [link], every digit becomes #, and only the first 40 characters are kept.
 * The full message is never stored.
 */
public final class PreviewMaker {

    public static final int PREVIEW_LENGTH = 40;

    private static final Pattern LINK = Pattern.compile(
            "(?i)\\b(?:https?://|www\\.)\\S+|\\b[a-z0-9-]+(?:\\.[a-z0-9-]+)+/\\S*");

    private PreviewMaker() { }

    public static String make(String message) {
        if (message == null) {
            return "";
        }
        String masked = LINK.matcher(message).replaceAll("[link]")
                .replaceAll("\\d", "#")
                .replaceAll("\\s+", " ")
                .strip();
        return masked.length() <= PREVIEW_LENGTH ? masked : masked.substring(0, PREVIEW_LENGTH) + "…";
    }
}
