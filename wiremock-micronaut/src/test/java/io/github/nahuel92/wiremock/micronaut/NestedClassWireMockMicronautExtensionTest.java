package io.github.nahuel92.wiremock.micronaut;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.micronaut.context.env.Environment;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautWireMockTest({
        @ConfigureWireMock(name = "user-service", properties = "user-service.url", portProperty = "user-service.port"),
        @ConfigureWireMock(name = "todo-service", properties = "todo-service.url", portProperty = "todo-service.port"),
        @ConfigureWireMock(name = "noproperty-service")
})
public class NestedClassWireMockMicronautExtensionTest {
    @Inject
    private Environment environment;

    @InjectWireMock("todo-service")
    private WireMockServer topLevelClassTodoService;

    @Nested
    class NestedTest {
        @InjectWireMock("todo-service")
        private WireMockServer nestedClassTodoService;

        @Test
        @DisplayName("WireMock should be available when injected as a method param")
        void successOnInjectingWireMockServerAsMethodParameter(@InjectWireMock("user-service") final WireMockServer server) {
            // expect
            CommonAssertions.assertWireMockServerIsConfigured(
                    server,
                    environment,
                    "user-service.url",
                    "user-service.port"
            );
        }

        @Test
        @DisplayName("WireMock should be available when injected as a nested class field")
        void successOnInjectingWireMockServerAsNestedClassField() {
            // expect
            CommonAssertions.assertWireMockServerIsConfigured(
                    nestedClassTodoService,
                    environment,
                    "todo-service.url",
                    "todo-service.port"
            );
        }

        @Test
        @DisplayName("WireMock should be available when injected as a top level class field")
        void successOnInjectingWireMockServerAsTopLevelClassField() {
            // expect
            CommonAssertions.assertWireMockServerIsConfigured(
                    topLevelClassTodoService,
                    environment,
                    "todo-service.url",
                    "todo-service.port"
            );
        }

        @Test
        @DisplayName("WireMock should not set a property on the ApplicationContext if it's not provided")
        void failureOnSettingPropertyWhenNotProvided(@InjectWireMock("noproperty-service") final WireMockServer server) {
            // expect
            assertThat(server)
                    .as("creates WireMock instance")
                    .isNotNull();
        }
    }
}
