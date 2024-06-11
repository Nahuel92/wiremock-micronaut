package app;

import app.protobuf.Greeter;
import com.google.protobuf.RpcChannel;
import io.grpc.ManagedChannel;
import io.micronaut.context.annotation.Factory;
import io.micronaut.grpc.annotation.GrpcChannel;
import jakarta.inject.Singleton;

@Factory
public class GrpcFactory {
    @Singleton
    Greeter.Stub reactiveStub(
            @GrpcChannel("https://${my.server}:${my.port}") final ManagedChannel channel) {
        return Greeter.Stub.newStub((RpcChannel) channel);
    }
}
