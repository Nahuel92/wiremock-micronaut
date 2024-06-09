package io.github.nahuel92.wiremock.micronaut;

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
     * Names of Micronaut properties to inject the {@link WireMockServer#baseUrl()}.
     *
     * @return names of Micronaut properties to inject the {@link WireMockServer#baseUrl()}.
     */
    String[] properties() default {};

    /**
     * The name of Micronaut property to inject the {@link WireMockServer#port()}
     *
     * @return the name of Micronaut property to inject the {@link WireMockServer#port()}
     */
    String portProperty() default "wiremock.server.port";

    /**
     * The location of WireMock stub files.
     * By default, classpath location is used to read stubs. This can be changed by setting
     * {@link #stubLocationOnClasspath} to false, which makes WireMock to use a directory location to get stub files
     * instead.
     * <p>
     * When {@link #stubLocationOnClasspath} is true, classpath location is used to get stubs
     * <code>wiremock/server-name/mappings/</code>, and, if provided, stubs are resolved from
     * <code>stub-location/mappings/</code>.
     *
     * @return the stub location
     */
    String stubLocation() default "";

    /**
     * Allows user to specify if the mappings should be loaded from classpath or a directory. The location is specified
     * with {@link #stubLocation()}.
     *
     * @return true if {@link #stubLocation} should point to a classpath directory and false if it should point to a
     * directory on the file system.
     */
    boolean stubLocationOnClasspath() default true;

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
