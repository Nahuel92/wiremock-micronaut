package app;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.nahuel92.wiremock.micronaut.EnableWireMock;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wiremock.grpc.GrpcExtensionFactory;
import org.wiremock.grpc.dsl.WireMockGrpcService;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.wiremock.grpc.dsl.WireMockGrpc.equalToMessage;
import static org.wiremock.grpc.dsl.WireMockGrpc.message;
import static org.wiremock.grpc.dsl.WireMockGrpc.method;

@MicronautTest
@EnableWireMock
public class GrpcTest {
    private final WireMockServer wm = new WireMockServer(wireMockConfig()
            .port(65000)
            //.dynamicPort()
            .withRootDirectory("src/test/resources/wiremock")
            .extensions(new GrpcExtensionFactory())
    );

    @Inject
    private GreeterGrpc.GreeterBlockingStub greeter;

    @BeforeEach
    void setUp() {
        wm.start();
    }

    @AfterEach
    void tearDown() {
        wm.stop();
    }

    @Test
    void name() {
        // given
        final var mockGreetingService = new WireMockGrpcService(
                new WireMock(wm.port()),
                GreeterGrpc.SERVICE_NAME
        );
        mockGreetingService.stubFor(method("sayHello")
                .withRequestMessage(equalToMessage(HelloRequest.newBuilder().setName("Tom")))
                .willReturn(message(HelloReply.newBuilder().setMessage("Hello Tom!")))
        );

        // when
        final var message = greeter.sayHello(HelloRequest.newBuilder().setName("Tom").build());

        // then
        assertThat(message.getMessage()).isEqualTo("Hello Tom!");
    }
}