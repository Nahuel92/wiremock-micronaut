package app;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.assertj.core.api.AutoCloseableSoftAssertions;
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
    void successOnStubbingViaJavaAPI() {
        // given
        wiremock.stubFor(
                get("/").willReturn(
                        aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("""
                                        [
                                            { "id": 1, "userId": 1, "title": "my todo" },
                                            { "id": 2, "userId": 1, "title": "my todo2" }
                                        ]
                                        """
                                )
                )
        );

        // when
        final var results = todoClient.findAll();

        // then
        assertThat(results).hasSize(2);
    }

    @Test
    void successOnStubbingViaFilesFromCustomLocation() {
        // when
        final var results = todoClient.findAll();

        // then
        try (final var softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(results).hasSize(2);
            softly.assertThat(results)
                    .first()
                    .satisfies(todo -> {
                        assertThat(todo.id()).isEqualTo(1);
                        assertThat(todo.title()).isEqualTo("custom location todo 1");
                    });
            softly.assertThat(results.get(1))
                    .satisfies(todo -> {
                        assertThat(todo.id()).isEqualTo(2);
                        assertThat(todo.title()).isEqualTo("custom location todo 2");
                    });
        }
    }
}
