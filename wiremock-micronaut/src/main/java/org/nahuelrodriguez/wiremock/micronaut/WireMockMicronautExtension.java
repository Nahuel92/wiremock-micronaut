package org.nahuelrodriguez.wiremock.micronaut;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.micronaut.context.env.MapPropertySource;
import io.micronaut.test.extensions.junit5.MicronautJunit5Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * JUnit 5 extension that sets {@link WireMockServer} instances previously registered with {@link ConfigureWireMock}
 * on test class fields.
 */
public class WireMockMicronautExtension extends MicronautJunit5Extension {
    private static final Logger LOGGER = LoggerFactory.getLogger(WireMockMicronautExtension.class);

    @Override
    public void beforeAll(final ExtensionContext extensionContext) throws Exception {
        super.beforeAll(extensionContext);
        configureWireMockServers(extensionContext);
    }

    @Override
    public void beforeEach(final ExtensionContext extensionContext) throws Exception {
        super.beforeEach(extensionContext);
        getWireMockServerMap(extensionContext).values().forEach(WireMockServer::resetAll);
        injectWireMockInstances(extensionContext);
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) {
        return parameterContext.findAnnotation(InjectWireMock.class)
                .map(InjectWireMock::value)
                .map(e -> getWireMockServerMap(extensionContext).get(e))
                .orElseThrow();
    }

    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) {
        return WireMockServer.class.equals(parameterContext.getParameter().getType()) &&
                parameterContext.isAnnotated(InjectWireMock.class);
    }

    private void configureWireMockServers(final ExtensionContext extensionContext) {
        final var enableWireMocks = extensionContext.getRequiredTestClass()
                .getAnnotationsByType(EnableWireMock.class);
        for (final var enableWireMock : enableWireMocks) {
            final var configureWireMocks = enableWireMock.value();
            for (final var options : configureWireMocks) {
                resolveOrCreateWireMockServer(extensionContext, options);
            }
        }
    }

    private void resolveOrCreateWireMockServer(final ExtensionContext extensionContext, final ConfigureWireMock options) {
        final var wireMockServer = getWireMockServerMap(extensionContext).get(options.name());
        if (wireMockServer != null && wireMockServer.isRunning()) {
            LOGGER.info("WireMockServer with name '{}' is already configured", options.name());
            return;
        }
        final var newServer = getStartedWireMockServer(options);
        saveWireMockServerToStore(extensionContext, options.name(), newServer);
        setShutdownHookForWireMockServer(options, newServer);
        injectPropertyIntoMicronautEnvironment(options.property(), newServer);
    }

    @SuppressWarnings("unchecked")  // "get" doesn't support generics usage
    private Map<String, WireMockServer> getWireMockServerMap(final ExtensionContext extensionContext) {
        return getStore(extensionContext)
                .getOrComputeIfAbsent(
                        applicationContext,
                        (applicationContext) -> new ConcurrentHashMap<String, WireMockServer>(),
                        Map.class
                );
    }

    private WireMockServer getStartedWireMockServer(final ConfigureWireMock options) {
        final var serverOptions = getWireMockConfiguration(options);
        LOGGER.info("Configuring WireMockServer with name '{}' on port: '{}'", options.name(), serverOptions.portNumber());
        final var newServer = new WireMockServer(serverOptions);
        newServer.start();
        LOGGER.info("Started WireMockServer with name '{}': '{}'", options.name(), newServer.baseUrl());
        return newServer;
    }

    private WireMockConfiguration getWireMockConfiguration(final ConfigureWireMock options) {
        final var stubLocation = StringUtils.isBlank(options.stubLocation()) ? "wiremock/" + options.name() :
                options.stubLocation();
        final var serverOptions = options()
                .usingFilesUnderClasspath(stubLocation)
                .port(options.port())
                .notifier(new Slf4jNotifier(true));
        if (options.extensions().length > 0) {
            serverOptions.extensions(options.extensions());
        }
        applyCustomizers(options, serverOptions);
        return serverOptions;
    }

    private void applyCustomizers(final ConfigureWireMock options, final WireMockConfiguration serverOptions) {
        for (final var customizer : options.configurationCustomizers()) {
            try {
                ReflectionUtils.newInstance(customizer).customize(serverOptions, options);
            } catch (final RuntimeException e) {
                if (e.getCause() instanceof NoSuchMethodException) {
                    LOGGER.error("Customizer '{}' must have a no-arg constructor", customizer, e);
                }
                throw e;
            }
        }
    }

    private void saveWireMockServerToStore(final ExtensionContext extensionContext, final String serverName,
                                           final WireMockServer server) {
        getWireMockServerMap(extensionContext).put(serverName, server);
    }

    private void setShutdownHookForWireMockServer(final ConfigureWireMock options, final WireMockServer server) {
        applicationContext.registerSingleton(ShutdownEventForWiremock.class, new ShutdownEventForWiremock(server, options));
    }

    @SuppressWarnings("resource")  // "addPropertySource" returns an autocloseable which shouldn't be closed here.
    private void injectPropertyIntoMicronautEnvironment(final String propertyName, final WireMockServer server) {
        if (StringUtils.isBlank(propertyName)) {
            return;
        }
        final var property = Map.<String, Object>of(propertyName, server.baseUrl());
        LOGGER.debug("Adding property '{}' to Micronaut application context", property);
        applicationContext.getEnvironment().addPropertySource(MapPropertySource.of("customSource", property));
    }

    private void injectWireMockInstances(final ExtensionContext extensionContext) throws IllegalAccessException {
        for (final var testInstance : extensionContext.getRequiredTestInstances().getAllInstances()) {
            final var annotatedFields = AnnotationSupport.findAnnotatedFields(testInstance.getClass(), InjectWireMock.class);
            for (final var annotatedField : annotatedFields) {
                annotatedField.setAccessible(true);
                final var annotationValue = annotatedField.getAnnotation(InjectWireMock.class);
                final var wireMockServerMap = getWireMockServerMap(extensionContext);
                final var wiremock = wireMockServerMap.get(annotationValue.value());
                if (wiremock == null) {
                    throw new IllegalStateException(
                            "WireMockServer with name '" + annotationValue.value() + "' not registered. " +
                                    "Perhaps you forgot to configure it first with @ConfigureWireMock?"
                    );
                }
                annotatedField.set(testInstance, wiremock);
            }
        }
    }
}
