package io.github.nahuel92.wiremock.micronaut;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
