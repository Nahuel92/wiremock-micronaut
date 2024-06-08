package app;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.MediaType;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nahuelrodriguez.wiremock.micronaut.ConfigureWireMock;
import org.nahuelrodriguez.wiremock.micronaut.EnableWireMock;
import org.nahuelrodriguez.wiremock.micronaut.InjectWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;

@MicronautTest
@EnableWireMock(@ConfigureWireMock(name = "user-client", properties = "user-client.url"))
class UserClientTests {
    @Inject
    private UserClient userClient;

    @InjectWireMock("user-client")
    private WireMockServer wiremock;

    private static void assertThatUserHasIdAndName(final User result, long id, final String name) {
        try (final var softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result.id()).isEqualTo(id);
            softly.assertThat(result.name()).isEqualTo(name);
        }
    }

    @Test
    @DisplayName("WireMock server should use Java stub when stubbing via the Java API")
    void successOnUsingMocksFromJavaAPIStubbing() {
        // given
        wiremock.stubFor(get("/2").willReturn(
                        aResponse()
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                                .withBody("""
                                        { "id": 2, "name": "Amy" }"""
                                )
                )
        );

        // when
        final var result = userClient.findOne(2L);

        // then
        assertThatUserHasIdAndName(result, 2L, "Amy");
    }

    @Test
    @DisplayName("WireMock server should use files stub when stubbing via files on the file system")
    void successOnUsingMocksFromStubFiles() {
        // given
        final var result = userClient.findOne(1L);

        // then
        assertThatUserHasIdAndName(result, 1L, "Jenna");
    }
}
