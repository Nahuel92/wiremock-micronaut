package org.nahuelrodriguez.wiremock.micronaut;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
@EnableWireMock({
        @ConfigureWireMock(
                name = "user-service",
                property = "user-service.url",
                configurationCustomizers = WireMockConfigurationCustomizerTest.SampleConfigurationCustomizer.class
        ),
        @ConfigureWireMock(
                name = "todo-service",
                property = "todo-service.url",
                configurationCustomizers = WireMockConfigurationCustomizerTest.SampleConfigurationCustomizer.class
        ),
})
//@ExtendWith(OutputCaptureExtension.class)
class WireMockConfigurationCustomizerTest {
    private static final int USER_SERVICE_PORT = 1;//TestSocketUtils.findAvailableTcpPort();
    private static final int TODO_SERVICE_PORT = 1;//TestSocketUtils.findAvailableTcpPort();

    static class SampleConfigurationCustomizer implements WireMockConfigurationCustomizer {

        @Override
        public void customize(WireMockConfiguration configuration, ConfigureWireMock options) {
            if (options.name().equals("user-service")) {
                configuration.port(USER_SERVICE_PORT);
            } else {
                configuration.port(TODO_SERVICE_PORT);
            }
        }
    }

    static class AppConfiguration {

    }

    @InjectWireMock("user-service")
    private WireMockServer userService;

    @InjectWireMock("todo-service")
    private WireMockServer todoService;

    @Test
    void appliesConfigurationCustomizer() {
        assertThat(userService.port()).isEqualTo(USER_SERVICE_PORT);
        assertThat(todoService.port()).isEqualTo(TODO_SERVICE_PORT);
    }

    @Test
    void outputsWireMockLogs(
            //CapturedOutput capturedOutput
    ) throws IOException, InterruptedException {
        userService.stubFor(get(urlEqualTo("/test"))
                .willReturn(aResponse().withHeader("Content-Type", "text/plain").withBody("Hello World!")));

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder().GET().uri(URI.create("http://localhost:" + userService.port() + "/test")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.body()).isEqualTo("Hello World!");
        /*assertThat(capturedOutput.getAll())
                .as("Must contain debug logging for WireMock")
                .contains("Matched response definition:");*/
    }

}
