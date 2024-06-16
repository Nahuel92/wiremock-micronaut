package io.github.nahuel92.wiremock.micronaut;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.ShutdownEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event to stop WireMock instances when the {@link ApplicationContext} is shut down.
 */
record ShutdownEventForServer(WireMockServer wireMockServer, ConfigureWireMock options)
        implements ApplicationEventListener<ShutdownEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownEventForServer.class);

    @Override
    public void onApplicationEvent(final ShutdownEvent event) {
        LOGGER.info("Stopping WireMockServer with name '{}'", options.name());
        wireMockServer.stop();
    }
}