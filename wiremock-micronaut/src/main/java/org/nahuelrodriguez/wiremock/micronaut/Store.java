package org.nahuelrodriguez.wiremock.micronaut;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.ApplicationContextProvider;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores references to the instances of {@link WireMockServer} associated with Micronaut application context.
 *
 * @author Nahuel Rodríguez
 */
enum Store {
    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(Store.class);
    private final Map<ApplicationContext, Map<String, WireMockServer>> store = new ConcurrentHashMap<>();

    WireMockServer findWireMockInstance(final ApplicationContext applicationContext, final String name) {
        return resolve(applicationContext).get(name);
    }

    WireMockServer findRequiredWireMockInstance(final ExtensionContext extensionContext, final String name) {
        final var wiremock = resolve(extensionContext).get(name);
        if (wiremock == null) {
            throw new IllegalStateException("WireMockServer with name '" + name + "' not registered. Perhaps you forgot to configure it first with @ConfigureWireMock?");
        }
        return wiremock;
    }

    void store(final ApplicationContext applicationContext, final String name, final WireMockServer wireMockServer) {
        resolve(applicationContext).put(name, wireMockServer);
    }

    Collection<WireMockServer> findAllInstances(final ExtensionContext extensionContext) {
        return resolve(extensionContext).values();
    }

    private Map<String, WireMockServer> resolve(final ExtensionContext extensionContext) {
        return null;//resolve(store.entrySet(.getApplicationContext(extensionContext));
    }

    private Map<String, WireMockServer> resolve(final ApplicationContext applicationContext) {
        LOGGER.info("Resolving store from context: {}", applicationContext);
        return store.computeIfAbsent(applicationContext, ctx -> new ConcurrentHashMap<>());
    }
}
