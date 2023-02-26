package app;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.maciejwalkowiak.wiremock.spring.ConfigureWiremock;
import com.maciejwalkowiak.wiremock.spring.EnableWiremock;
import com.maciejwalkowiak.wiremock.spring.Wiremock;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnableWiremock({
        @ConfigureWiremock(name = "user-client", property = "user-client.url")
})
class UserClientTests {

    @Autowired
    private UserClient userClient;

    @Wiremock("user-client")
    private WireMockServer wiremock;

    @Test
    void findUserById() {
        wiremock.stubFor(get("/1").willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        { "id": 1, "name": "Amy" }
                        """)));
        User user = userClient.findOne(1L);
        assertThat(user).isNotNull();
        assertThat(user.id()).isEqualTo(1L);
        assertThat(user.name()).isEqualTo("Amy");
    }

}
