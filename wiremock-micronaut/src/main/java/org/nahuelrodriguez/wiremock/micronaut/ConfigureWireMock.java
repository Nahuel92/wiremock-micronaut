package org.nahuelrodriguez.wiremock.micronaut;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.Extension;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Configures WireMock instance.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigureWireMock {
    /**
     * Port on which WireMock server is going to listen. {@code 0} means WireMock will pick random port.
     *
     * @return WireMock server port
     */
    int port() default 0;

    /**
     * The name of WireMock server.
     *
     * @return the name of WireMock server.
     */
    String name();

    /**
     * The name of Micronaut property to inject the {@link WireMockServer#baseUrl()}
     *
     * @return the name of Micronaut property to inject the {@link WireMockServer#baseUrl()}
     */
    String property() default "";

    /**
     * The location of WireMock stub files. By default, stubs are resolved from classpath location
     * <code>wiremock-server-name/mappings/</code>.
     * <p>
     * If provided, stubs are resolved from <code>stub-location/mappings/</code>.
     *
     * @return the stub location
     */
    String stubLocation() default "";

    /**
     * WireMock extensions to register in {@link WireMockServer}.
     *
     * @return the extensions
     */
    Class<? extends Extension>[] extensions() default {};

    /**
     * Customizes {@link WireMockConfiguration} used by {@link WireMockServer} instance. Customizers are ordered by
     * their natural order in this array. Each customizer must have no-arg constructor.
     *
     * @return the configuration customizers classes
     */
    Class<? extends WireMockConfigurationCustomizer>[] configurationCustomizers() default {};
}
