package com.maciejwalkowiak.wiremock.spring;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.TestSocketUtils;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = WireMockConfigurationCustomizerTest.AppConfiguration.class)
@EnableWireMock({
        @ConfigureWireMock(
                name = "user-service",
                property = "user-service.url",
                configurationCustomizers = WireMockConfigurationCustomizerTest.SampleConfigurationCustomizer.class
        ),
        @ConfigureWireMock(
                name = "todo-service",
                property = "todo-service.url",
                configurationCustomizers = WireMockConfigurationCustomizerTest.SampleConfigurationCustomizer.class
        ),
})
class WireMockConfigurationCustomizerTest {
    private static final int USER_SERVICE_PORT = TestSocketUtils.findAvailableTcpPort();
    private static final int TODO_SERVICE_PORT = TestSocketUtils.findAvailableTcpPort();

    static class SampleConfigurationCustomizer implements WireMockConfigurationCustomizer {

        @Override
        public void customize(WireMockConfiguration configuration, ConfigureWireMock options) {
            if (options.name().equals("user-service")) {
                configuration.port(USER_SERVICE_PORT);
            } else {
                configuration.port(TODO_SERVICE_PORT);
            }
        }
    }

    @SpringBootApplication
    static class AppConfiguration {

    }

    @InjectWireMock("user-service")
    private WireMockServer userService;

    @InjectWireMock("todo-service")
    private WireMockServer todoService;

    @Test
    void appliesConfigurationCustomizer() {
        assertThat(userService.port()).isEqualTo(USER_SERVICE_PORT);
        assertThat(todoService.port()).isEqualTo(TODO_SERVICE_PORT);
    }

}
