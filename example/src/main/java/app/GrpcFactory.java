package app;

import io.grpc.ManagedChannel;
import io.micronaut.context.annotation.Factory;
import io.micronaut.grpc.annotation.GrpcChannel;
import jakarta.inject.Singleton;

@Factory
public class GrpcFactory {
    @Singleton
    GreeterGrpc.GreeterBlockingStub greeter(@GrpcChannel("greeter") final ManagedChannel channel) {
        return GreeterGrpc.newBlockingStub(channel);
    }
}