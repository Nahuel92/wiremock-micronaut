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
import org.junit.platform.commons.util.StringUtils;
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

    @Override
    public void beforeAll(final ExtensionContext extensionContext) throws Exception {
        super.beforeAll(extensionContext);
        configureWireMockServers(extensionContext);
    }

    private static Object getWireMockServer(final Field annotatedField, final Map<String, Object> wireMockServerMap,
                                            final String name) {
        if (annotatedField.getType().equals(WireMockGrpcService.class)) {
            return wireMockServerMap.get("grpc/" + name);
        }
        if (annotatedField.getType().equals(WireMockServer.class)) {
            return wireMockServerMap.get(name);
        }
        throw new IllegalStateException("@InjectWireMock must be used on [WireMockServer|WireMockGrpcService] type fields!");
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) {
        return parameterContext.findAnnotation(InjectWireMock.class)
                .map(InjectWireMock::value)
                .map(e -> getWireMockServerMap(extensionContext).get(e))
                .orElseThrow();
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

    private void configureWireMockServers(final ExtensionContext extensionContext) {
        final var enableWireMocks = extensionContext.getRequiredTestClass().getAnnotationsByType(EnableWireMock.class);
        for (final var enableWireMock : enableWireMocks) {
            final var configureWireMocks = enableWireMock.value();
            for (final var options : configureWireMocks) {
                final var wireMockServer = resolveOrCreateWireMockServer(extensionContext, options);
                if (configureWireMocks.length == 1) {
                    WireMock.configureFor(wireMockServer.port());
                }
            }
        }
    }

    private WireMockServer resolveOrCreateWireMockServer(final ExtensionContext extensionContext,
                                                         final ConfigureWireMock options) {
        final var wireMockServer = getWireMockServerMap(extensionContext).get(options.name());
        if (wireMockServer instanceof WireMockServer w && w.isRunning()) {
            LOGGER.info("WireMockServer with name '{}' is already configured", options.name());
            return w;
        }
        final var newServer = getStartedWireMockServer(options);
        final var isGrpc = Arrays.asList(options.extensionFactories()).contains(GrpcExtensionFactory.class);
        if (isGrpc) {
            final var newServer2 = new WireMockGrpcService(new WireMock(newServer.port()), options.name());
            saveWireMockServerToStore(extensionContext, newServer2, "grpc/" + options.name());
        }

        saveWireMockServerToStore(extensionContext, newServer, options.name());
        setShutdownHookForWireMockServer(newServer, options);
        injectPropertyIntoMicronautEnvironment(newServer, options);
        return newServer;
    }

    private WireMockServer getStartedWireMockServer(final ConfigureWireMock options) {
        final var serverOptions = getWireMockConfiguration(options);
        LOGGER.info("Configuring WireMockServer with name '{}' on port: '{}'", options.name(), serverOptions.portNumber());
        final var newServer = new WireMockServer(serverOptions);
        newServer.start();
        LOGGER.info("Started WireMockServer with name '{}': '{}'", options.name(), newServer.baseUrl());
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

    private void resolveStubLocation(final ConfigureWireMock options, final WireMockConfiguration serverOptions) {
        if (options.stubLocationOnClasspath()) {
            final var stubLocation = StringUtils.isBlank(options.stubLocation()) ? "wiremock/" + options.name() :
                    options.stubLocation();
            serverOptions.usingFilesUnderClasspath(stubLocation);
            return;
        }
        serverOptions.usingFilesUnderDirectory(options.stubLocation());
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

    private WireMockConfiguration getWireMockConfiguration(final ConfigureWireMock options) {
        final var serverOptions = options()
                .port(options.port())
                .notifier(new Slf4jNotifier(true));
        if (options.extensions().length > 0) {
            serverOptions.extensions(options.extensions());
        }
        resolveStubLocation(options, serverOptions);
        applyCustomizers(options, serverOptions);

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
        return serverOptions;
    }

    private void setShutdownHookForWireMockServer(final WireMockServer server, final ConfigureWireMock options) {
        applicationContext.registerSingleton(ShutdownEventForWiremock.class, new ShutdownEventForWiremock(server, options));
    }

    private void saveWireMockServerToStore(final ExtensionContext extensionContext, final Object server,
                                           final String serverName) {
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
            final var isGrpc = Arrays.asList(options.extensionFactories()).contains(GrpcExtensionFactory.class);
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
                final var annotationValue = annotatedField.getAnnotation(InjectWireMock.class);
                final var wiremock = getWireMockServer(annotatedField, wireMockServerMap, annotationValue.value());
                if (wiremock == null) {
                    throw new IllegalStateException(
                            "WireMock server/gRPC service with name '" + annotationValue.value() + "' not registered. " +
                                    "Perhaps you forgot to configure it first with @ConfigureWireMock?"
                    );
                }
                annotatedField.set(testInstance, wiremock);
            }
        }
    }
}
