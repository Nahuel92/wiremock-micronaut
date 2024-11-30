package app;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.nahuel92.wiremock.micronaut.ConfigureWireMock;
import io.github.nahuel92.wiremock.micronaut.EnableWireMock;
import io.github.nahuel92.wiremock.micronaut.InjectWireMock;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.MediaType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;

@EnableWireMock(@ConfigureWireMock(name = "user-client", properties = "user-client.url"))
class UserClientTest {
    @Inject
    private UserClient userClient;

    @InjectWireMock("user-client")
    private WireMockServer wiremock;

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
        CommonAssertions.assertThatUserHasIdAndName(result, 2L, "Amy").close();
    }

    @Test
    @DisplayName("WireMock server should use files stub when stubbing via files on the file system")
    void successOnUsingMocksFromStubFiles() {
        // given
        final var result = userClient.findOne(1L);

        // then
        CommonAssertions.assertThatUserHasIdAndName(result, 1L, "Jenna").close();
    }
}
