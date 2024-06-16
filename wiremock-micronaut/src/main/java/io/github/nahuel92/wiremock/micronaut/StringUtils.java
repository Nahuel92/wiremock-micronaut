package io.github.nahuel92.wiremock.micronaut;

class StringUtils {
    public static String defaultIfBlank(final String value, final String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    public static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    public static boolean isNotBlank(final String value) {
        return !isBlank(value);
    }
}