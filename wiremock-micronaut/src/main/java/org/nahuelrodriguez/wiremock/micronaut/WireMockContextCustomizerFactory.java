package org.nahuelrodriguez.wiremock.micronaut;

import io.micronaut.test.annotation.AnnotationUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.micronaut.test.annotation.AnnotationUtils.findRepeatableAnnotations;

/**
 * Creates {@link WireMockContextCustomizer} for test classes annotated with {@link EnableWireMock}.
 *
 * @author Nahuel Rodríguez
 */
public class WireMockContextCustomizerFactory {
    public WireMockContextCustomizer createContextCustomizer(final Class<?> testClass, List<?> configAttributes) {
        // scan class and all enclosing classes if the test class is @Nested
        final var holder = new ConfigureWiremockHolder();
        parseDefinitions(testClass, holder);

        if (holder.isEmpty()) {
            return null;
        }
        return new WireMockContextCustomizer(holder.asArray());
    }

    private void parseDefinitions(final Class<?> testClass, final ConfigureWiremockHolder parser) {
        parser.parse(testClass);
        //if (TestContextAnnotationUtils.searchEnclosingClass(testClass)) {
        //parseDefinitions(testClass.getEnclosingClass(), parser);
        //}
    }

    private static class ConfigureWiremockHolder {
        private final List<ConfigureWireMock> annotations = new ArrayList<>();

        void add(final ConfigureWireMock[] annotations) {
            this.annotations.addAll(Arrays.asList(annotations));
        }

        void parse(final Class<?> clazz) {
            findRepeatableAnnotations(clazz, EnableWireMock.class)
                    .stream()
                    .map(EnableWireMock::value)
                    .forEach(this::add);
        }

        boolean isEmpty() {
            return annotations.isEmpty();
        }

        ConfigureWireMock[] asArray() {
            return annotations.toArray(new ConfigureWireMock[]{});
        }
    }
}
