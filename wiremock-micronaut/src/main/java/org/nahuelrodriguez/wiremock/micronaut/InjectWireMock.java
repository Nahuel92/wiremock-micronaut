package org.nahuelrodriguez.wiremock.micronaut;

import java.lang.annotation.*;

/**
 * Injects WireMock instance previously configured on the class or field level with {@link ConfigureWireMock}.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InjectWireMock {
    /**
     * The name of WireMock instance to inject.
     *
     * @return the name of WireMock instance to inject.
     */
    String value();
}
