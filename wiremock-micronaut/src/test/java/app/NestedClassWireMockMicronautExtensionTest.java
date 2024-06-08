package app;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.micronaut.context.env.Environment;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nahuelrodriguez.wiremock.micronaut.ConfigureWireMock;
import org.nahuelrodriguez.wiremock.micronaut.EnableWireMock;
import org.nahuelrodriguez.wiremock.micronaut.InjectWireMock;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
@EnableWireMock({
        @ConfigureWireMock(name = "user-service", properties = "user-service.url"),
        @ConfigureWireMock(name = "todo-service", properties = "todo-service.url"),
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
            CommonAssertions.assertWireMockServerIsConfigured(server, environment, "user-service.url");
        }

        @Test
        @DisplayName("WireMock should be available when injected as a nested class field")
        void successOnInjectingWireMockServerAsNestedClassField() {
            CommonAssertions.assertWireMockServerIsConfigured(nestedClassTodoService, environment, "todo-service.url");
        }

        @Test
        @DisplayName("WireMock should be available when injected as a top level class field")
        void successOnInjectingWireMockServerAsTopLevelClassField() {
            CommonAssertions.assertWireMockServerIsConfigured(topLevelClassTodoService, environment, "todo-service.url");
        }

        @Test
        @DisplayName("WireMock should not set a property on the ApplicationContext if it's not provided")
        void failureOnSettingPropertyWhenNotProvided(@InjectWireMock("noproperty-service") final WireMockServer server) {
            assertThat(server)
                    .as("creates WireMock instance")
                    .isNotNull();
        }
    }
}
