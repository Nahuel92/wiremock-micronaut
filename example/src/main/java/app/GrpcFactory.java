package app;

import io.grpc.ManagedChannel;
import io.micronaut.context.annotation.Factory;
import io.micronaut.grpc.annotation.GrpcChannel;
import jakarta.inject.Singleton;
import mypackage.Greeter2Grpc;

@Factory
public class GrpcFactory {
    @Singleton
    GreeterGrpc.GreeterBlockingStub greeter(@GrpcChannel("greeter") final ManagedChannel channel) {
        return GreeterGrpc.newBlockingStub(channel);
    }

    @Singleton
    Greeter2Grpc.Greeter2BlockingStub greeter2(@GrpcChannel("greeter2") final ManagedChannel channel) {
        return Greeter2Grpc.newBlockingStub(channel);
    }
}