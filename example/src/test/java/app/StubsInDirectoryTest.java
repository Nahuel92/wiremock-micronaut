package app;

import io.github.nahuel92.wiremock.micronaut.ConfigureWireMock;
import io.github.nahuel92.wiremock.micronaut.EnableWireMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@EnableWireMock(
        @ConfigureWireMock(
                name = "user-client",
                properties = "user-client.url",
                stubLocation = "src/test/wiremock-mappings/user-client",
                stubLocationOnClasspath = false
        )
)
public class StubsInDirectoryTest {
    @Inject
    private UserClient userClient;

    @Test
    void usesStubFiles() {
        // when
        final var user = userClient.findOne(1L);

        // then
        assertThat(user.id()).isEqualTo(1);
        assertThat(user.name()).isEqualTo("Jenny");
    }
}
