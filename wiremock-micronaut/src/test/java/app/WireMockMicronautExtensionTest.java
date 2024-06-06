package app;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.micronaut.context.env.Environment;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
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
    void createsWiremockWithClassLevelConfigureWiremock(@InjectWireMock("user-service") final WireMockServer server) {
        assertWireMockServer(server, "user-service.url");
    }

    @Test
    void createsWiremockWithFieldLevelConfigureWiremock() {
        assertWireMockServer(todoWireMockServer, "todo-service.url");
    }

    @Test
    void doesNotSetPropertyWhenNotProvided(@InjectWireMock("noproperty-service") final WireMockServer server) {
        assertThat(server)
                .as("creates WireMock instance")
                .isNotNull();
    }

    private void assertWireMockServer(final WireMockServer wireMockServer, final String property) {
        assertThat(wireMockServer)
                .as("creates WireMock instance")
                .isNotNull();
        assertThat(wireMockServer.baseUrl())
                .as("WireMock baseUrl is set")
                .isNotNull();
        assertThat(wireMockServer.port())
                .as("sets random port")
                .isNotZero();
        assertThat(environment.getProperty(property, String.class))
                .isPresent()
                .as("sets Micronaut property")
                .contains(wireMockServer.baseUrl());
    }
}
