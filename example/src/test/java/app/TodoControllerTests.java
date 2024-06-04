package app;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.nahuelrodriguez.wiremock.micronaut.ConfigureWireMock;
import org.nahuelrodriguez.wiremock.micronaut.EnableWireMock;
import org.nahuelrodriguez.wiremock.micronaut.InjectWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
@EnableWireMock({
        @ConfigureWireMock(name = "user-client", property = "user-client.url"),
        @ConfigureWireMock(name = "todo-service", property = "todo-client.url")
})
class TodoControllerTests {
    @InjectWireMock("todo-service")
    private WireMockServer todoService;

    @InjectWireMock("user-client")
    private WireMockServer userService;

    @Inject
    private HttpClient httpClient;

    @Inject
    EmbeddedServer embeddedServer;

    @Test
    void returnsTodos() {
        todoService.stubFor(get("/").willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        [
                            { "id": 1, "userId": 1, "title": "my todo" },
                            { "id": 2, "userId": 2, "title": "my todo2" }
                        ]
                        """)));

        userService.stubFor(get("/1").willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        { "id": 1, "name": "Amy" }
                        """)));

        userService.stubFor(get("/2").willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        { "id": 2, "name": "John" }
                        """)));

        HttpResponse<TodoController.TodoDTO[]> response = httpClient.toBlocking()
                .exchange("http://localhost:" + embeddedServer.getPort() + "/", TodoController.TodoDTO[].class);

        org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.OK, response.getStatus());
        assertThat(response.getBody()).isPresent();
        assertThat(response.getBody().get())
                .hasSize(2)
                .satisfies(todos -> {
                    assertThat(todos[0].id()).isEqualTo(1);
                    assertThat(todos[0].title()).isEqualTo("my todo");
                    assertThat(todos[0].userName()).isEqualTo("Amy");

                    assertThat(todos[1].id()).isEqualTo(2);
                    assertThat(todos[1].title()).isEqualTo("my todo2");
                    assertThat(todos[1].userName()).isEqualTo("John");
                }
                );
    }
}
