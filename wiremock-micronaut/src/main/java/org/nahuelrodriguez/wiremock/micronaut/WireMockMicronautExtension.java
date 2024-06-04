package org.nahuelrodriguez.wiremock.micronaut;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.MapPropertySource;
import io.micronaut.test.annotation.AnnotationUtils;
import io.micronaut.test.extensions.junit5.MicronautJunit5Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class WireMockMicronautExtension extends MicronautJunit5Extension {
    private static final Logger LOGGER = LoggerFactory.getLogger(WireMockMicronautExtension.class);

    private static void applyCustomizers(final ConfigureWireMock options, final WireMockConfiguration serverOptions) {
        for (final Class<? extends WireMockConfigurationCustomizer> customizer : options.configurationCustomizers()) {
            try {
                ReflectionUtils.newInstance(customizer).customize(serverOptions, options);
            } catch (final RuntimeException e) {
                if (e.getCause() instanceof NoSuchMethodException) {
                    LOGGER.error("Customizer {} must have a no-arg constructor", customizer, e);
                }
                throw e;
            }
        }
    }

    @Override
    public void beforeAll(final ExtensionContext extensionContext) throws Exception {
        super.beforeAll(extensionContext);
        Arrays.stream(extensionContext.getRequiredTestClass()
                        .getAnnotationsByType(EnableWireMock.class)
                )
                .map(EnableWireMock::value)
                .flatMap(Arrays::stream)
                .forEach(e -> resolveOrCreateWireMockServer(extensionContext, applicationContext, e));
    }

    @Override
    public void beforeEach(final ExtensionContext extensionContext) throws Exception {
        super.beforeEach(extensionContext);
        final var a = getStore(extensionContext).get(applicationContext, Map.class);
        Collection<WireMockServer> values = a.values();
        values.forEach(WireMockServer::resetAll);
        injectWireMockInstances(extensionContext);
    }

    private void injectWireMockInstances(final ExtensionContext extensionContext) throws IllegalAccessException {
        // getRequiredTestInstances() return multiple instances for nested tests
        for (final Object testInstance : extensionContext.getRequiredTestInstances().getAllInstances()) {
            final List<Field> annotatedFields = AnnotationSupport.findAnnotatedFields(testInstance.getClass(), InjectWireMock.class);
            for (final Field annotatedField : annotatedFields) {
                final var annotationValue = annotatedField.getAnnotation(InjectWireMock.class);
                annotatedField.setAccessible(true);
                final var testApplicationContext = Optional.ofNullable(getStore(extensionContext)
                                .get(applicationContext))
                        .filter(e -> e instanceof Map<?, ?>)
                        .map(e -> (Map<String, WireMockServer>) e)
                        .orElseThrow();

                final var wiremock = testApplicationContext.get(annotationValue.value());
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

    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType().equals(WireMockServer.class) &&
                parameterContext.isAnnotated(InjectWireMock.class);
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) {
        return parameterContext.findAnnotation(InjectWireMock.class)
                .map(InjectWireMock::value)
                .map(e -> getStore(extensionContext).get(e, WireMockServer.class))
                .orElseThrow();
    }

    private void resolveOrCreateWireMockServer(final ExtensionContext extensionContext, final ApplicationContext context,
                                               final ConfigureWireMock options) {
        final var wireMockServer = getStore(extensionContext).get(options.name(), WireMockServer.class);
        if (wireMockServer != null && wireMockServer.isRunning()) {
            LOGGER.info("WireMockServer with name '{}' is already configured", options.name());
            return;
        }

        // create & start wiremock server
        final WireMockConfiguration serverOptions = options()
                .usingFilesUnderClasspath(resolveStubLocation(options))
                .port(options.port())
                .notifier(new Slf4jNotifier(true));

        if (options.extensions().length > 0) {
            serverOptions.extensions(options.extensions());
        }

        applyCustomizers(options, serverOptions);

        //LOGGER.info("Configuring WireMockServer with name '{}' on port: {}", options.name(), serverOptions.portNumber());
        LOGGER.info("Configuring WireMockServer with {}", options.stubLocation());
        final WireMockServer newServer = new WireMockServer(serverOptions);
        newServer.start();
        LOGGER.info("Started WireMockServer with name '{}':{}", options.name(), newServer.baseUrl());

        // save server to store
        final var isPresent = getStore(extensionContext).get(applicationContext) != null;
        if (!isPresent) {
            getStore(extensionContext).put(applicationContext, new ConcurrentHashMap<>());
        }
        final var map = getStore(extensionContext).get(applicationContext, Map.class);
        map.put(options.name(), newServer);

        // add shutdown hook
        context.registerSingleton(
                ShutdownEventForWiremock.class,
                new ShutdownEventForWiremock(newServer, options)
        );

        // configure Micronaut environment property
        if (StringUtils.isNotBlank(options.property())) {
            final var property = Map.<String, Object>of(options.property(), newServer.baseUrl());
            LOGGER.debug("Adding property '{}' to Micronaut application context", property);
            context.getEnvironment().addPropertySource(MapPropertySource.of("customSource", property));
        }
    }

    private String resolveStubLocation(final ConfigureWireMock options) {
        return StringUtils.isBlank(options.stubLocation()) ? "wiremock/" + options.name() : options.stubLocation();
    }

    // TODO see if this is needed
    public void createContextCustomizer(final Class<?> testClass, List<?> configAttributes) {
        // scan class and all enclosing classes if the test class is @Nested
        final var holder = new ConfigureWiremockHolder();
        holder.parse(testClass);
        //if (TestContextAnnotationUtils.searchEnclosingClass(testClass)) {
        //parseDefinitions(testClass.getEnclosingClass(), parser);
        //}

        if (holder.isEmpty()) {
        }
        //holder.asArray()
    }

    private static class ConfigureWiremockHolder {
        private final List<ConfigureWireMock> annotations = new ArrayList<>();

        void add(final ConfigureWireMock[] annotations) {
            this.annotations.addAll(Arrays.asList(annotations));
        }

        void parse(final Class<?> clazz) {
            AnnotationUtils.findRepeatableAnnotations(clazz, EnableWireMock.class)
                    .stream()
                    .map(EnableWireMock::value)
                    .forEach(this::add);
        }

        boolean isEmpty() {
            return annotations.isEmpty();
        }

        ConfigureWireMock[] asArray() {
            return annotations.toArray(new ConfigureWireMock[]{});
        }
    }

    //

    static class Slf4jNotifier implements Notifier {
        private static final Logger log = LoggerFactory.getLogger("WireMock");
        private final boolean verbose;

        Slf4jNotifier(boolean verbose) {
            this.verbose = verbose;
        }

        @Override
        public void info(final String message) {
            if (verbose) {
                log.info(message);
            }
        }

        @Override
        public void error(final String message) {
            log.error(message);
        }

        @Override
        public void error(final String message, final Throwable t) {
            log.error(message, t);
        }
    }
}
