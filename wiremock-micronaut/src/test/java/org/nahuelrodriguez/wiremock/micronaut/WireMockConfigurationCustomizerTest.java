package org.nahuelrodriguez.wiremock.micronaut;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
@EnableWireMock({
        @ConfigureWireMock(
                name = "user-service",
                properties = "user-service.url",
                configurationCustomizers = WireMockConfigurationCustomizerTest.SampleConfigurationCustomizer.class
        ),
        @ConfigureWireMock(
                name = "todo-service",
                properties = "todo-service.url",
                configurationCustomizers = WireMockConfigurationCustomizerTest.SampleConfigurationCustomizer.class
        ),
})
class WireMockConfigurationCustomizerTest {
    @Inject
    private HttpClient httpClient;

    @InjectWireMock("user-service")
    private WireMockServer userService;

    @InjectWireMock("todo-service")
    private WireMockServer todoService;

    @Test
    @DisplayName("WireMock should return the stubbed response when a request matches the stub conditions")
    void successOnGettingWireMockStubbedResponse() {
        // given
        userService.stubFor(get(urlEqualTo("/test")).willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
                        .withBody("Hello World!")
                )
        );

        // when
        final var response = httpClient.toBlocking()
                .exchange("http://localhost:" + userService.port() + "/test", String.class);

        //then
        assertThat(response.body()).isEqualTo("Hello World!");
    }

    @Test
    @DisplayName("WireMock should pick up the new behavior given by a Configuration Customizer if used")
    void successOnApplyingConfigurationCustomization() {
        assertThat(userService.port()).isEqualTo(SampleConfigurationCustomizer.USER_SERVICE_PORT);
        assertThat(todoService.port()).isEqualTo(SampleConfigurationCustomizer.TODO_SERVICE_PORT);
    }

    static class SampleConfigurationCustomizer implements WireMockConfigurationCustomizer {
        private static final int USER_SERVICE_PORT = findAvailablePort();
        private static final int TODO_SERVICE_PORT = findAvailablePort();

        private static int findAvailablePort() {
            try (final var serverSocket = new ServerSocket(0)) {
                return serverSocket.getLocalPort();
            } catch (final IOException e) {
                throw new UncheckedIOException("Failed to find available port", e);
            }
        }

        @Override
        public void customize(final WireMockConfiguration configuration, final ConfigureWireMock options) {
            if (options.name().equals("user-service")) {
                configuration.port(USER_SERVICE_PORT);
                return;
            }
            configuration.port(TODO_SERVICE_PORT);
        }
    }
}
