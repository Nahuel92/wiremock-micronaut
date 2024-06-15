package io.github.nahuel92.wiremock.micronaut;

class StringUtils {
    public static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}