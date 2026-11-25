package com.scamlens.web;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Reads the values a browser sends from a form or a link.
 *
 * A browser sends form fields like this:   message=Act+now%21&savePreview=on
 * parse() turns that into a map:           {message="Act now!", savePreview="on"}
 */
public final class FormData {

    private final Map<String, String> values;

    private FormData(Map<String, String> values) {
        this.values = Map.copyOf(values);
    }

    public static FormData parse(String encoded) {
        Map<String, String> values = new HashMap<>();
        if (encoded != null && !encoded.isEmpty()) {
            Arrays.stream(encoded.split("&"))
                    .filter(pair -> !pair.isEmpty())
                    .forEach(pair -> {
                        int equals = pair.indexOf('=');
                        String key = decode(equals < 0 ? pair : pair.substring(0, equals));
                        String value = equals < 0 ? "" : decode(pair.substring(equals + 1));
                        values.putIfAbsent(key, value);
                    });
        }
        return new FormData(values);
    }

    /** The value for a field, or empty if it was not sent. */
    public Optional<String> get(String key) {
        return Optional.ofNullable(values.get(key));
    }

    /** True if a checkbox was ticked (browsers only send ticked checkboxes). */
    public boolean isChecked(String key) {
        return values.containsKey(key);
    }

    private static String decode(String text) {
        try {
            return URLDecoder.decode(text, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException badEncoding) {
            return text;   // a broken %-code: keep the raw text instead of crashing
        }
    }
}
