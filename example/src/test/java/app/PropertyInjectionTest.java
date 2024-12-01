package app;

import io.github.nahuel92.wiremock.micronaut.ConfigureWireMock;
import io.github.nahuel92.wiremock.micronaut.MicronautWireMockTest;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Value;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.Test;

@Property(name = "myValue1", value = "any1")
@Property(name = "myValue2", value = "any2")
@MicronautWireMockTest(@ConfigureWireMock(name = "wiremock", properties = "my.property"))
class PropertyInjectionTest {
    @Property(name = "my.property")
    private String myProperty;

    @Property(name = "wiremock.server.port")
    private String wiremockServerPort;

    @Property(name = "myValue1")
    private String myValue1;

    @Value("${myValue2}")
    private String myValue2;

    @Test
    @Property(name = "myValue3", value = "any3")
    @Property(name = "myValue4", value = "any4")
    void successOnInjectingProperties(@Property(name = "myValue3") final String myValue3,
                                      /* I wouldn't recommend using @Value as parameter
                                      since it doesn't resolve placeholders (e.g., ${myValue4}).
                                      It might be a Micronaut Test bug.
                                       */
                                      @Value("myValue4") final String myValue4) {
        try (final var softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(myProperty).isNotBlank();
            softly.assertThat(wiremockServerPort).isNotBlank();
            softly.assertThat(myValue1).isEqualTo("any1");
            softly.assertThat(myValue2).isEqualTo("any2");
            softly.assertThat(myValue3).isEqualTo("any3");
            softly.assertThat(myValue4).isEqualTo("any4");
        }
    }
}