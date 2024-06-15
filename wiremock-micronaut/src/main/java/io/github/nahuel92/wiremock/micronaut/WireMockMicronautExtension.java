package io.github.nahuel92.wiremock.micronaut;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.ExtensionFactory;
import io.micronaut.context.env.MapPropertySource;
import io.micronaut.test.extensions.junit5.MicronautJunit5Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wiremock.grpc.GrpcExtensionFactory;
import org.wiremock.grpc.dsl.WireMockGrpcService;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * JUnit 5 extension that sets {@link WireMockServer} instances previously registered with {@link ConfigureWireMock}
 * on test class fields.
 */
class WireMockMicronautExtension extends MicronautJunit5Extension {
    private static final Logger LOGGER = LoggerFactory.getLogger(WireMockMicronautExtension.class);

    WireMockMicronautExtension() {
    }

    private static void registerExtensionFactories(ConfigureWireMock options, WireMockConfiguration serverOptions) {
        // TODO: improve this
        final var extensionFactories = Arrays.stream(options.extensionFactories())
                .map(e -> {
                    try {
                        return e.getDeclaredConstructor().newInstance();
                    } catch (final InvocationTargetException | InstantiationException | NoSuchMethodException |
                                   IllegalAccessException ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .toArray(ExtensionFactory[]::new);
        serverOptions.extensions(extensionFactories);
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) {
        return parameterContext.findAnnotation(InjectWireMock.class)
                .map(InjectWireMock::value)
                .map(e -> getWireMockServerMap(extensionContext).get(e))
                .orElseThrow();
    }

    private static Object getServerInstance(final Field annotatedField, final Map<String, Object> wireMockServerMap) {
        final var serverName = annotatedField.getAnnotation(InjectWireMock.class).value();
        final Object wiremock;
        if (annotatedField.getType().equals(WireMockGrpcService.class)) {
            wiremock = wireMockServerMap.get("grpc/" + serverName);
        } else if (annotatedField.getType().equals(WireMockServer.class)) {
            wiremock = wireMockServerMap.get(serverName);
        } else {
            throw new IllegalStateException("@InjectWireMock must be used on [WireMockServer|WireMockGrpcService] type fields!");
        }
        if (wiremock == null) {
            throw new IllegalStateException(
                    "WireMock server/gRPC service with name '" + serverName + "' not registered. " +
                            "Perhaps you forgot to configure it first with @ConfigureWireMock?"
            );
        }
        return wiremock;
    }

    @Override
    public void beforeEach(final ExtensionContext extensionContext) throws Exception {
        super.beforeEach(extensionContext);
        for (final var wiremockServer : getWireMockServerMap(extensionContext).values()) {
            if (wiremockServer instanceof WireMockServer w) {
                w.resetAll();
            }
        }
        injectWireMockInstances(extensionContext);
    }

    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) {
        return (WireMockServer.class.equals(parameterContext.getParameter().getType()) ||
                WireMockGrpcService.class.equals(parameterContext.getParameter().getType())) &&
                parameterContext.isAnnotated(InjectWireMock.class);
    }

    @Override
    public void beforeAll(final ExtensionContext extensionContext) throws Exception {
        super.beforeAll(extensionContext);
        configureWireMockServers(extensionContext);
    }

    private void configureWireMockServers(final ExtensionContext extensionContext) {
        final var enableWireMocks = extensionContext.getRequiredTestClass().getAnnotationsByType(EnableWireMock.class);
        for (final var enableWireMock : enableWireMocks) {
            final var configureWireMocks = enableWireMock.value();
            for (final var options : configureWireMocks) {
                final var wireMockServer = getOrCreateServer(extensionContext, options);
                if (configureWireMocks.length == 1) {
                    WireMock.configureFor(wireMockServer.port());
                }
            }
        }
    }

    private WireMockServer getOrCreateServer(final ExtensionContext extensionContext, final ConfigureWireMock options) {
        final var wireMockServer = getWireMockServerMap(extensionContext).get(options.name());
        if (wireMockServer instanceof WireMockServer w && w.isRunning()) {
            LOGGER.info("WireMockServer with name '{}' is already configured", options.name());
            return w;
        }
        final var newServer = getStartedServer(options);
        if (isGrpcTest(options)) {
            final var newGrpcService = new WireMockGrpcService(new WireMock(newServer.port()), options.name());
            saveToStore(extensionContext, "grpc/" + options.name(), newGrpcService);
        }
        saveToStore(extensionContext, options.name(), newServer);
        registerShutdownHookForServer(newServer, options);
        injectPropertyIntoMicronautEnvironment(newServer, options);
        return newServer;
    }

    @SuppressWarnings("unchecked")  // "get" doesn't support generics usage
    private Map<String, Object> getWireMockServerMap(final ExtensionContext extensionContext) {
        return getStore(extensionContext)
                .getOrComputeIfAbsent(
                        applicationContext,
                        (applicationContext) -> new ConcurrentHashMap<String, Object>(),
                        Map.class
                );
    }

    private boolean isGrpcTest(final ConfigureWireMock options) {
        for (final var extensionFactory : options.extensionFactories()) {
            if (extensionFactory.equals(GrpcExtensionFactory.class)) {
                return true;
            }
        }
        return false;
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

    private WireMockServer getStartedServer(final ConfigureWireMock options) {
        final var serverOptions = getConfiguration(options);
        LOGGER.info("Configuring WireMockServer with name '{}' on port: '{}'", options.name(), serverOptions.portNumber());
        final var newServer = new WireMockServer(serverOptions);
        newServer.start();
        LOGGER.info("Started WireMockServer with name '{}': '{}'", options.name(), newServer.baseUrl());
        return newServer;
    }

    private void resolveStubLocation(final ConfigureWireMock options, final WireMockConfiguration serverOptions) {
        if (options.stubLocationOnClasspath()) {
            final var stubLocation = StringUtils.isBlank(options.stubLocation()) ?
                    "wiremock/" + options.name() : options.stubLocation();
            serverOptions.usingFilesUnderClasspath(stubLocation);
            return;
        }
        serverOptions.usingFilesUnderDirectory(options.stubLocation());
    }

    private WireMockConfiguration getConfiguration(final ConfigureWireMock options) {
        final var serverOptions = options()
                .port(options.port())
                .notifier(new Slf4jNotifier(true));
        if (options.extensions().length > 0) {
            serverOptions.extensions(options.extensions());
        }
        resolveStubLocation(options, serverOptions);
        applyCustomizers(options, serverOptions);
        registerExtensionFactories(options, serverOptions);
        return serverOptions;
    }

    private void registerShutdownHookForServer(final WireMockServer server, final ConfigureWireMock options) {
        applicationContext.registerSingleton(ShutdownEventForServer.class, new ShutdownEventForServer(server, options));
    }

    private void saveToStore(final ExtensionContext extensionContext, final String serverName, final Object server) {
        getWireMockServerMap(extensionContext).put(serverName, server);
    }

    @SuppressWarnings("resource")  // "addPropertySource" returns an autocloseable which shouldn't be closed here.
    private void injectPropertyIntoMicronautEnvironment(final WireMockServer server, final ConfigureWireMock options) {
        final var propertiesToAdd = new HashMap<String, Object>();
        propertiesToAdd.put(options.portProperty(), server.port());
        for (final var propertyName : options.properties()) {
            if (StringUtils.isBlank(propertyName)) {
                continue;
            }
            final var isGrpc = isGrpcTest(options);
            if (isGrpc) {
                final var serverUrl = server.baseUrl().substring(0, server.baseUrl().lastIndexOf(":"));
                propertiesToAdd.put(propertyName, serverUrl);
                continue;
            }
            propertiesToAdd.put(propertyName, server.baseUrl());
        }
        LOGGER.debug("Adding properties '{}' to Micronaut application context", propertiesToAdd);
        final var customSource = MapPropertySource.of("wiremockExtensionSource", propertiesToAdd);
        applicationContext.getEnvironment().addPropertySource(customSource);
    }

    private void injectWireMockInstances(final ExtensionContext extensionContext) throws IllegalAccessException {
        final var wireMockServerMap = getWireMockServerMap(extensionContext);
        for (final var testInstance : extensionContext.getRequiredTestInstances().getAllInstances()) {
            final var annotatedFields = AnnotationSupport.findAnnotatedFields(testInstance.getClass(), InjectWireMock.class);
            for (final var annotatedField : annotatedFields) {
                annotatedField.setAccessible(true);
                annotatedField.set(testInstance, getServerInstance(annotatedField, wireMockServerMap));
            }
        }
    }
}
