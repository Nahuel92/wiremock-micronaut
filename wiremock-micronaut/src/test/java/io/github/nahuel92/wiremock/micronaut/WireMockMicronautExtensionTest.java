package io.github.nahuel92.wiremock.micronaut;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.micronaut.context.env.Environment;
import jakarta.inject.Inject;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WireMockMicronautExtensionTest {
    @MicronautWireMockTest({
            @ConfigureWireMock(name = "user-service", properties = "user-service.url", portProperty = "user-service.port"),
            @ConfigureWireMock(name = "todo-service", properties = "todo-service.url", portProperty = "todo-service.port"),
            @ConfigureWireMock(name = "noproperty-service"),
    })
    @Nested
    class SinglePropertyBindingTest {
        @InjectWireMock("todo-service")
        private WireMockServer todoWireMockServer;

        @Inject
        private Environment environment;

        @Test
        @DisplayName("WireMock should be available when injected as a method param")
        void createsWiremockWithClassLevelConfigureWiremock(@InjectWireMock("user-service") final WireMockServer server) {
            CommonAssertions.assertWireMockServerIsConfigured(
                    server,
                    environment,
                    "user-service.url",
                    "user-service.port"
            );
        }

        @Test
        @DisplayName("WireMock should be available when injected as a class field")
        void createsWiremockWithFieldLevelConfigureWiremock() {
            CommonAssertions.assertWireMockServerIsConfigured(
                    todoWireMockServer,
                    environment,
                    "todo-service.url",
                    "todo-service.port"
            );
        }

        @Test
        @DisplayName("WireMock should not set a property on the ApplicationContext if it's not provided")
        void failureOnSettingPropertyWhenNotProvided(@InjectWireMock("noproperty-service") final WireMockServer server) {
            assertThat(server)
                    .as("creates WireMock instance")
                    .isNotNull();
        }
    }

    @MicronautWireMockTest(
            @ConfigureWireMock(name = "user-service", properties = {"user-service.url", "todo-service.url"})
    )
    @Nested
    class MultiplePropertiesBindingTest {
        @InjectWireMock("user-service")
        private WireMockServer userServiceWireMockServer;

        @Inject
        private Environment environment;

        @Test
        void successOnBindingWireMockURLToMultipleProperties() {
            // expect
            try (final var softly = new AutoCloseableSoftAssertions()) {
                softly.assertThat(environment.getProperty("user-service.url", String.class))
                        .contains(userServiceWireMockServer.baseUrl());
                softly.assertThat(environment.getProperty("todo-service.url", String.class))
                        .contains(userServiceWireMockServer.baseUrl());
                assertThat(environment.getProperty("wiremock.server.port", Integer.class))
                        .as("Sets Micronaut port property");
            }
        }
    }
}
