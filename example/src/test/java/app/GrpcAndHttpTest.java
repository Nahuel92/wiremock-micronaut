package app;

import io.github.nahuel92.wiremock.micronaut.ConfigureWireMock;
import io.github.nahuel92.wiremock.micronaut.EnableWireMock;
import io.github.nahuel92.wiremock.micronaut.InjectWireMock;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import mypackage.Greeter2Grpc;
import mypackage.HelloReply2;
import mypackage.HelloRequest2;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wiremock.grpc.GrpcExtensionFactory;
import org.wiremock.grpc.dsl.WireMockGrpcService;

import static org.wiremock.grpc.dsl.WireMockGrpc.equalToMessage;
import static org.wiremock.grpc.dsl.WireMockGrpc.message;
import static org.wiremock.grpc.dsl.WireMockGrpc.method;

@MicronautTest
@EnableWireMock({
        @ConfigureWireMock(
                name = GreeterGrpc.SERVICE_NAME,
                portProperty = "my.port",
                properties = "my.server",
                extensionFactories = GrpcExtensionFactory.class,
                stubLocation = "src/test/resources/wiremock"
        ),
        @ConfigureWireMock(
                name = Greeter2Grpc.SERVICE_NAME,
                portProperty = "my.port2",
                properties = "my.server2",
                extensionFactories = GrpcExtensionFactory.class,
                stubLocation = "src/test/resources/wiremock2"
        ),
        @ConfigureWireMock(name = "user-client", properties = "user-client.url")
})
class GrpcAndHttpTest {
    @Inject
    private GreeterGrpc.GreeterBlockingStub greeter;

    @Inject
    private Greeter2Grpc.Greeter2BlockingStub greeter2;

    @InjectWireMock(GreeterGrpc.SERVICE_NAME)
    private WireMockGrpcService greeterGrpcService;

    @InjectWireMock(Greeter2Grpc.SERVICE_NAME)
    private WireMockGrpcService greeter2GrpcService;

    @Inject
    private UserClient userClient;

    @Test
    @DisplayName("WireMock should allow configuring multiple gRPC and HTTP services per test")
    void successOnTestingWithGrpc() {
        // given
        greeterGrpcService.stubFor(method("sayHello")
                .withRequestMessage(equalToMessage(HelloRequest.newBuilder().setName("Tom")))
                .willReturn(message(HelloReply.newBuilder().setMessage("Hello Tom!")))
        );
        greeter2GrpcService.stubFor(method("sayHello2")
                .withRequestMessage(equalToMessage(HelloRequest2.newBuilder().setName("Nahuel")))
                .willReturn(message(HelloReply2.newBuilder().setMessage("Hello Nahuel!")))
        );
        final var result = userClient.findOne(1L);

        // when
        final var message = greeter.sayHello(HelloRequest.newBuilder().setName("Tom").build());
        final var message2 = greeter2.sayHello2(HelloRequest2.newBuilder().setName("Nahuel").build());

        // then
        try (final var softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(message.getMessage()).isEqualTo("Hello Tom!");
            softly.assertThat(message2.getMessage()).isEqualTo("Hello Nahuel!");
            softly.assertAlso(CommonAssertions.assertThatUserHasIdAndName(result, 1L, "Jenna"));
        }
    }
}
