package org.nahuelrodriguez.wiremock.micronaut;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.ShutdownEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShutdownEventForWiremock implements ApplicationEventListener<ShutdownEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownEventForWiremock.class);
    private final WireMockServer wireMockServer;
    private final ConfigureWireMock options;

    public ShutdownEventForWiremock(final WireMockServer wireMockServer, final ConfigureWireMock options) {
        this.wireMockServer = wireMockServer;
        this.options = options;
    }

    @Override
    public void onApplicationEvent(final ShutdownEvent event) {
        LOGGER.info("Stopping WireMockServer with name '{}'", options.name());
        wireMockServer.stop();
    }
}
