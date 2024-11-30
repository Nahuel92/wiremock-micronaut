package io.github.nahuel92.wiremock.micronaut;

import io.micronaut.context.annotation.Property;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@MicronautWireMockTest(@ConfigureWireMock(name = "wiremock", properties = "my.property"))
class MyTest {
    @Property(name = "my.property")
    String myProperty;

    @Property(name = "wiremock.server.port")
    String wiremockServerPort;

    @Test
    void myPropertyShouldNotBeEmtpyNorNull() {
        Assertions.assertTrue(myProperty != null && !myProperty.isEmpty());
    }
}