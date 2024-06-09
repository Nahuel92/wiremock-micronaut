package app;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.nahuel92.wiremock.micronaut.ConfigureWireMock;
import io.github.nahuel92.wiremock.micronaut.EnableWireMock;
import io.github.nahuel92.wiremock.micronaut.InjectWireMock;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;

@MicronautTest
@EnableWireMock({
        @ConfigureWireMock(name = "user-client", properties = "user-client.url"),
        @ConfigureWireMock(name = "todo-service", properties = "todo-client.url")
})
class TodoControllerTest {
    @InjectWireMock("todo-service")
    private WireMockServer todoService;

    @InjectWireMock("user-client")
    private WireMockServer userService;

    @Inject
    private HttpClient httpClient;

    @Inject
    private EmbeddedServer embeddedServer;

    private static void stubResponse(final WireMockServer server, final String url, final String body) {
        server.stubFor(get(url).willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .withBody(body)));
    }

    @Test
    @DisplayName("WireMock should handle multiple stubbed services involved in a single request")
    void successOnGettingTodos() {
        // given
        stubResponse(todoService, "/", """
                [
                    { "id": 1, "userId": 1, "title": "my todo" },
                    { "id": 2, "userId": 2, "title": "my todo2" }
                ]
                """);
        // and
        stubResponse(userService, "/1", """
                { "id": 1, "name": "Amy" }
                """);
        // and
        stubResponse(userService, "/2", """
                { "id": 2, "name": "John" }
                """);

        // when
        final var response = httpClient.toBlocking()
                .exchange(embeddedServer.getURI().toString(), TodoController.TodoDTO[].class);

        // then
        try (final var softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat((Object) response.getStatus()).isEqualTo(HttpStatus.OK);
            softly.assertThat(response.getBody())
                    .isPresent()
                    .get(InstanceOfAssertFactories.array(TodoController.TodoDTO[].class))
                    .hasSize(2)
                    .containsExactly(
                            new TodoController.TodoDTO(1L, "my todo", "Amy"),
                            new TodoController.TodoDTO(2L, "my todo2", "John")
                    );
        }
    }
}
