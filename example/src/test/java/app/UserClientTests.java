package app;

import com.github.tomakehurst.wiremock.WireMockServer;
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
@EnableWireMock(@ConfigureWireMock(name = "user-client", property = "user-client.url"))
class UserClientTests {
    @Inject
    private UserClient userClient;

    @InjectWireMock("user-client")
    private WireMockServer wiremock;

    @Test
    void usesJavaStubbing() {
        wiremock.stubFor(get("/2").willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        { "id": 2, "name": "Amy" }
                        """)));
        User user = userClient.findOne(2L);
        assertThat(user).isNotNull();
        assertThat(user.id()).isEqualTo(2L);
        assertThat(user.name()).isEqualTo("Amy");
    }

    @Test
    void usesStubFiles() {
        User user = userClient.findOne(1L);
        assertThat(user).isNotNull();
        assertThat(user.id()).isEqualTo(1L);
        assertThat(user.name()).isEqualTo("Jenna");
    }
}
