package app;

import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.nahuelrodriguez.wiremock.micronaut.ConfigureWireMock;
import org.nahuelrodriguez.wiremock.micronaut.EnableWireMock;
import org.nahuelrodriguez.wiremock.micronaut.InjectWireMock;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
@EnableWireMock({
        @ConfigureWireMock(name = "todo-client", property = "todo-client.url", stubLocation = "custom-location")
})
class TodoClientTests {
    @Inject
    private TodoClient todoClient;

    @InjectWireMock("todo-client")
    private WireMockServer wiremock;

    @Test
    void usesJavaStubbing() {
        wiremock.stubFor(get("/").willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        [
                            { "id": 1, "userId": 1, "title": "my todo" },
                            { "id": 2, "userId": 1, "title": "my todo2" }
                        ]
                        """)));
        assertThat(todoClient.findAll()).isNotNull().hasSize(2);
    }

    @Test
    void usesStubFilesFromCustomLocation() {
        List<Todo> todos = todoClient.findAll();
        assertThat(todos).isNotNull().hasSize(2);
        assertThat(todos.get(0)).satisfies(todo -> {
            assertThat(todo.id()).isEqualTo(1);
            assertThat(todo.title()).isEqualTo("custom location todo 1");
        });
        assertThat(todos.get(1)).satisfies(todo -> {
            assertThat(todo.id()).isEqualTo(2);
            assertThat(todo.title()).isEqualTo("custom location todo 2");
        });
    }

}
