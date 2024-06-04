package org.nahuelrodriguez.wiremock.micronaut;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables creating WireMock servers through {@link WireMockConfigurationCustomizer}.
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(WireMockMicronautExtension.class)
public @interface EnableWireMock {
    /**
     * A list of {@link WireMockServer} configurations. For each configuration a separate instance
     * of {@link WireMockServer} is created.
     *
     * @return an array of configurations
     */
    ConfigureWireMock[] value() default {};
}
