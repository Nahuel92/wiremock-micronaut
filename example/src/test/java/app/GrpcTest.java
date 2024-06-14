package app;

import io.github.nahuel92.wiremock.micronaut.ConfigureWireMock;
import io.github.nahuel92.wiremock.micronaut.EnableWireMock;
import io.github.nahuel92.wiremock.micronaut.InjectWireMock;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.wiremock.grpc.GrpcExtensionFactory;
import org.wiremock.grpc.dsl.WireMockGrpcService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.wiremock.grpc.dsl.WireMockGrpc.equalToMessage;
import static org.wiremock.grpc.dsl.WireMockGrpc.message;
import static org.wiremock.grpc.dsl.WireMockGrpc.method;

@MicronautTest
@EnableWireMock(
        @ConfigureWireMock(
                name = GreeterGrpc.SERVICE_NAME,
                portProperty = "my.port",
                properties = "my.server",
                extensionFactories = GrpcExtensionFactory.class,
                stubLocation = "src/test/resources/wiremock"
        )
)
public class GrpcTest {
    @Inject
    private GreeterGrpc.GreeterBlockingStub greeter;

    @InjectWireMock(GreeterGrpc.SERVICE_NAME)
    private WireMockGrpcService wireMockServer;

    @Test
    void successOnTestingWithGrpc() {
        // given
        wireMockServer.stubFor(method("sayHello")
                .withRequestMessage(equalToMessage(HelloRequest.newBuilder().setName("Tom")))
                .willReturn(message(HelloReply.newBuilder().setMessage("Hello Tom!")))
        );

        // when
        final var message = greeter.sayHello(HelloRequest.newBuilder().setName("Tom").build());

        // then
        assertThat(message.getMessage()).isEqualTo("Hello Tom!");
    }
}