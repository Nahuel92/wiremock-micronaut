package app;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.MediaType;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nahuelrodriguez.wiremock.micronaut.ConfigureWireMock;
import org.nahuelrodriguez.wiremock.micronaut.EnableWireMock;
import org.nahuelrodriguez.wiremock.micronaut.InjectWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
@EnableWireMock(
        @ConfigureWireMock(name = "todo-client", property = "todo-client.url", stubLocation = "custom-location")
)
class TodoClientTests {
    @Inject
    private TodoClient todoClient;

    @InjectWireMock("todo-client")
    private WireMockServer wiremock;

    @Test
    @DisplayName("WireMock server should use Java stub when stubbing via the Java API")
    void successOnStubbingViaJavaAPI() {
        // given
        wiremock.stubFor(get("/").willReturn(
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
