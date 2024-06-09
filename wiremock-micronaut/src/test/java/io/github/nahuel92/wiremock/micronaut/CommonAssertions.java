package io.github.nahuel92.wiremock.micronaut;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.micronaut.context.env.Environment;
import org.assertj.core.api.AutoCloseableSoftAssertions;

public class CommonAssertions {
    public static void assertWireMockServerIsConfigured(final WireMockServer wireMockServer,
                                                        final Environment environment, final String property,
                                                        final String portProperty) {
        try (final var softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(wireMockServer)
                    .as("Creates WireMock instance")
                    .isNotNull();
            softly.assertThat(wireMockServer.baseUrl())
                    .as("WireMock baseUrl is set")
                    .isNotNull();
            softly.assertThat(wireMockServer.port())
                    .as("Sets random port")
                    .isNotZero();
            softly.assertThat(environment.getProperty(property, String.class))
                    .as("Sets Micronaut property")
                    .contains(wireMockServer.baseUrl());
            softly.assertThat(environment.getProperty(portProperty, Integer.class))
                    .as("Sets Micronaut port property")
                    .contains(wireMockServer.port());
        }
    }
}
