package app;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.nahuel92.wiremock.micronaut.ConfigureWireMock;
import io.github.nahuel92.wiremock.micronaut.EnableWireMock;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.MediaType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.assertj.core.api.Assertions.assertThat;

class TodoClientTest {
    @Inject
    private TodoClient todoClient;

    @EnableWireMock(
            @ConfigureWireMock(name = "todo-client", properties = "todo-client.url", stubLocation = "custom-location")
    )
    @Nested
    @DisplayName("WireMock server instances must be accessed via injected fields")
    class InjectWireMockServerAccessTest {
        @Test
        @DisplayName("WireMock server should use Java stub when stubbing via the Java API")
        void successOnStubbingViaJavaAPI() {
            // given
            WireMock.stubFor(get("/").willReturn(
                            aResponse()
                                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                                    .withBody("""
                                            [
                                                { "id": 1, "userId": 1, "title": "my todo" },
                                                { "id": 2, "userId": 1, "title": "my todo2" }
                                            ]"""
                                    )
                    )
            );

            // when
            final var results = todoClient.findAll();

            // then
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("WireMock server should use files stub when stubbing via files on the file system")
        void successOnStubbingViaFilesFromCustomLocation() {
            // when
            final var results = todoClient.findAll();

            // then
            assertThat(results)
                    .hasSize(2)
                    .containsExactly(
                            new Todo(1L, 1L, "custom location todo 1"),
                            new Todo(2L, 1L, "custom location todo 2")
                    );
        }
    }

    @EnableWireMock(
            @ConfigureWireMock(name = "todo-client", properties = "todo-client.url", stubLocation = "custom-location")
    )
    @Nested
    @DisplayName("When exactly one WireMock server instance is configured, it can be accessed statically via the 'WireMock' client")
    class StaticWireMockServerAccessTest {
        @Test
        @DisplayName("WireMock server should use Java stub when stubbing via the Java API")
        void successOnStubbingViaJavaAPI() {
            // given
            WireMock.stubFor(get("/").willReturn(
                            aResponse()
                                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                                    .withBody("""
                                            [
                                                { "id": 1, "userId": 1, "title": "my todo" },
                                                { "id": 2, "userId": 1, "title": "my todo2" }
                                            ]"""
                                    )
                    )
            );

            // when
            final var results = todoClient.findAll();

            // then
            assertThat(results).hasSize(2);
        }
    }
}
