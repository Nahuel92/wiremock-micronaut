package io.github.nahuel92.wiremock.micronaut;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.micronaut.context.env.MapPropertySource;
import io.micronaut.test.extensions.junit5.MicronautJunit5Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wiremock.grpc.GrpcExtensionFactory;
import org.wiremock.grpc.dsl.WireMockGrpcService;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JUnit 5 extension that sets {@link WireMockServer} instances previously registered with {@link ConfigureWireMock}
 * on test class fields.
 */
class WireMockMicronautExtension extends MicronautJunit5Extension {
    private static final Logger LOGGER = LoggerFactory.getLogger(WireMockMicronautExtension.class);
    private static final List<Class<?>> SUPPORTED_TYPES = List.of(WireMockServer.class, WireMockGrpcService.class);
    private final InternalStore internalStore = new InternalStore();

    WireMockMicronautExtension() {
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) {
        final var annotation = parameterContext.findAnnotation(InjectWireMock.class);
        if (annotation.isPresent()) {
            return internalStore.getServerMap(extensionContext).get(annotation.get().value());
        }
        throw new IllegalStateException();
    }

    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) {
        return SUPPORTED_TYPES.contains(parameterContext.getParameter().getType()) &&
                parameterContext.isAnnotated(InjectWireMock.class);
    }

    @Override
    public void beforeAll(final ExtensionContext extensionContext) throws Exception {
        super.beforeAll(extensionContext);
        configureWireMockServers(extensionContext);
    }

    @Override
    public void beforeEach(final ExtensionContext extensionContext) throws Exception {
        super.beforeEach(extensionContext);
        for (final var wireMockServer : internalStore.getServerMap(extensionContext).values()) {
            wireMockServer.resetAll();
        }
        injectWireMockInstances(extensionContext);
    }

    private void configureWireMockServers(final ExtensionContext extensionContext) {
        for (final var enableWireMock : extensionContext.getRequiredTestClass().getAnnotationsByType(EnableWireMock.class)) {
            if (enableWireMock.value().length == 1) {
                final var wireMockServer = getOrCreateServer(extensionContext, enableWireMock.value()[0]);
                WireMock.configureFor(wireMockServer.port());
                continue;
            }
            for (final var options : enableWireMock.value()) {
                getOrCreateServer(extensionContext, options);
            }
        }
    }

    private WireMockServer getOrCreateServer(final ExtensionContext extensionContext, final ConfigureWireMock options) {
        final var wireMockServer = internalStore.getServerMap(extensionContext).get(options.name());
        if (wireMockServer != null && wireMockServer.isRunning()) {
            LOGGER.info("WireMockServer with name '{}' is already configured", options.name());
            return wireMockServer;
        }
        final var newServer = getStartedServer(options);
        internalStore.put(extensionContext, options.name(), newServer);
        if (isGrpcTest(options)) {
            final var newGrpcService = new WireMockGrpcService(new WireMock(newServer.port()), options.name());
            internalStore.put(extensionContext, options.name(), newGrpcService);
        }
        applicationContext.registerSingleton(ShutdownServerEvent.class, new ShutdownServerEvent(newServer, options));
        addPropertiesToMicronautContext(newServer, options);
        return newServer;
    }

    private WireMockServer getStartedServer(final ConfigureWireMock options) {
        LOGGER.info("Configuring WireMockServer with name '{}' on port: '{}'", options.name(), options.port());
        final var newServer = new WireMockServer(WireMockConfigurationMapper.from(options));
        newServer.start();
        LOGGER.info("Started WireMockServer with name '{}' with base URL: '{}'", options.name(), newServer.baseUrl());
        return newServer;
    }

    private boolean isGrpcTest(final ConfigureWireMock options) {
        for (final var extensionFactory : options.extensionFactories()) {
            if (extensionFactory.equals(GrpcExtensionFactory.class)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("resource")  // "addPropertySource" returns an autocloseable which shouldn't be closed here.
    private void addPropertiesToMicronautContext(final WireMockServer server, final ConfigureWireMock options) {
        final var properties = new HashMap<String, Object>();
        properties.put(options.portProperty(), server.port());
        final var serverUrl = getServerUrl(server.baseUrl(), options);
        for (final var name : options.properties()) {
            if (StringUtils.isNotBlank(name)) {
                properties.put(name, serverUrl);
            }
        }
        LOGGER.debug("Adding properties '{}' to Micronaut application context", properties);
        final var customSource = MapPropertySource.of("wireMockExtensionSource", properties);
        applicationContext.getEnvironment().addPropertySource(customSource);
    }

    private String getServerUrl(final String baseUrl, final ConfigureWireMock options) {
        if (isGrpcTest(options)) {
            return baseUrl.substring(0, baseUrl.lastIndexOf(":"));  // Port is not needed for gRPC.
        }
        return baseUrl;
    }

    private void injectWireMockInstances(final ExtensionContext extensionContext) throws IllegalAccessException {
        final var serverMap = internalStore.getServerMap(extensionContext);
        final var grpcServicesMap = internalStore.getGrpcServicesMap(extensionContext);
        for (final var testInstance : extensionContext.getRequiredTestInstances().getAllInstances()) {
            final var annotatedFields = AnnotationSupport.findAnnotatedFields(testInstance.getClass(), InjectWireMock.class);
            for (final var annotatedField : annotatedFields) {
                annotatedField.setAccessible(true);
                annotatedField.set(testInstance, getInstance(annotatedField, serverMap, grpcServicesMap));
            }
        }
    }

    private Object getInstance(final Field annotatedField, final Map<String, WireMockServer> mockServerMap,
                               final Map<String, WireMockGrpcService> grpcServiceMap) {
        final var serverName = annotatedField.getAnnotation(InjectWireMock.class).value();
        final var wireMock = getInstance(mockServerMap, grpcServiceMap, serverName, annotatedField.getType());
        if (wireMock == null) {
            throw new IllegalStateException(
                    "WireMock server/gRPC service with name '" + serverName + "' not registered. " +
                            "Perhaps you forgot to configure it first with @ConfigureWireMock?"
            );
        }
        return wireMock;
    }

    private Object getInstance(final Map<String, WireMockServer> mockServerMap,
                               final Map<String, WireMockGrpcService> grpcServiceMap,
                               final String serverName, final Class<?> type) {
        if (WireMockGrpcService.class.isAssignableFrom(type)) {
            return grpcServiceMap.get(serverName);
        }
        if (WireMockServer.class.isAssignableFrom(type)) {
            return mockServerMap.get(serverName);
        }
        throw new IllegalStateException("@InjectWireMock must be used on [WireMockServer|WireMockGrpcService] type fields!");
    }

    private class InternalStore {
        private void put(final ExtensionContext extensionContext, final String key, final WireMockServer server) {
            getServerMap(extensionContext).put(key, server);
        }

        private void put(final ExtensionContext extensionContext, final String key, final WireMockGrpcService service) {
            getGrpcServicesMap(extensionContext).put(key, service);
        }

        private Map<String, WireMockServer> getServerMap(final ExtensionContext extensionContext) {
            return getMap(extensionContext, "wiremock-server-");
        }

        private Map<String, WireMockGrpcService> getGrpcServicesMap(final ExtensionContext extensionContext) {
            return getMap(extensionContext, "wiremock-grpc-service-");
        }

        @SuppressWarnings("unchecked")  // "get" doesn't support generics usage
        private <K, V> Map<K, V> getMap(final ExtensionContext extensionContext, final K key) {
            return getStore(extensionContext)
                    .getOrComputeIfAbsent(
                            key.toString() + applicationContext,
                            (applicationContext) -> new ConcurrentHashMap<K, V>(),
                            Map.class
                    );
        }
    }
}