package com.scamlens.util;

import java.util.Optional;

/**
 * Checks the user's input before analysis (FR2).
 * Returns an error message if the input is invalid, or empty if it is fine.
 */
public final class InputValidator {

    public static final int MAX_LENGTH = 5000;

    private InputValidator() { }   // utility class: no objects needed

    public static Optional<String> validate(String message) {
        if (message == null || message.isBlank()) {
            return Optional.of("Please enter or paste a message to analyse.");
        }
        if (message.length() > MAX_LENGTH) {
            return Optional.of("The message is too long (" + message.length()
                    + " characters). Please keep it under " + MAX_LENGTH + " characters.");
        }
        if (message.chars().noneMatch(Character::isLetterOrDigit)) {
            return Optional.of("The message must contain some letters or numbers.");
        }
        return Optional.empty();
    }
}
