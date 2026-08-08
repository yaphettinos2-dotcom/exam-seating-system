package examsystem.util;

import java.util.Locale;

/** Small text helpers shared by the model, service and UI layers. */
public final class Strings {
    private Strings() {
    }

    public static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public static String upper(String value) {
        return clean(value).toUpperCase(Locale.ROOT);
    }

    public static String lower(String value) {
        return clean(value).toLowerCase(Locale.ROOT);
    }

    public static boolean isBlank(String value) {
        return clean(value).isEmpty();
    }

    public static boolean same(String left, String right) {
        return left != null && left.equalsIgnoreCase(clean(right));
    }

    public static boolean contains(String haystack, String needle) {
        return lower(haystack).contains(lower(needle));
    }
}
