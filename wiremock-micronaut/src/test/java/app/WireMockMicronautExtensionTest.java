package app;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.micronaut.context.env.Environment;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nahuelrodriguez.wiremock.micronaut.ConfigureWireMock;
import org.nahuelrodriguez.wiremock.micronaut.EnableWireMock;
import org.nahuelrodriguez.wiremock.micronaut.InjectWireMock;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
@EnableWireMock({
        @ConfigureWireMock(name = "user-service", property = "user-service.url"),
        @ConfigureWireMock(name = "todo-service", property = "todo-service.url"),
        @ConfigureWireMock(name = "noproperty-service")
})
public class WireMockMicronautExtensionTest {
    @InjectWireMock("todo-service")
    private WireMockServer todoWireMockServer;

    @Inject
    private Environment environment;

    @Test
    @DisplayName("WireMock should be available when injected as a method param")
    void createsWiremockWithClassLevelConfigureWiremock(@InjectWireMock("user-service") final WireMockServer server) {
        CommonAssertions.assertWireMockServerIsConfigured(server, environment, "user-service.url");
    }

    @Test
    @DisplayName("WireMock should be available when injected as a class field")
    void createsWiremockWithFieldLevelConfigureWiremock() {
        CommonAssertions.assertWireMockServerIsConfigured(todoWireMockServer, environment, "todo-service.url");
    }

    @Test
    @DisplayName("WireMock should not set a property on the ApplicationContext if it's not provided")
    void failureOnSettingPropertyWhenNotProvided(@InjectWireMock("noproperty-service") final WireMockServer server) {
        assertThat(server)
                .as("creates WireMock instance")
                .isNotNull();
    }
}
