package io.github.nahuel92.wiremock.micronaut;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.micronaut.context.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonAssertions {
    public static void assertWireMockServerIsConfigured(final WireMockServer wireMockServer,
                                                        final Environment environment, final String property,
                                                        final String portProperty) {
        assertThat(wireMockServer)
                .as("Creates WireMock instance")
                .isNotNull();
        assertThat(wireMockServer.baseUrl())
                .as("WireMock baseUrl is set")
                .isNotNull();
        assertThat(wireMockServer.port())
                .as("Sets random port")
                .isNotZero();
        assertThat(environment.getProperty(property, String.class))
                .as("Sets Micronaut property")
                .contains(wireMockServer.baseUrl());
        assertThat(environment.getProperty(portProperty, Integer.class))
                .as("Sets Micronaut port property")
                .contains(wireMockServer.port());
    }
}
