package org.nahuelrodriguez.wiremock.micronaut;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.MapPropertySource;
import io.micronaut.test.extensions.junit5.MicronautJunit5Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.micronaut.test.annotation.AnnotationUtils.findRepeatableAnnotations;

/**
 * JUnit 5 extension that sets {@link WireMockServer} instances previously registered with {@link ConfigureWireMock}
 * on test class fields.
 *
 * @author Nahuel Rodríguez
 */
public class WireMockMicronautExtension extends MicronautJunit5Extension {
    private static final Logger LOGGER = LoggerFactory.getLogger(WireMockMicronautExtension.class);

    private static void applyCustomizers(final ConfigureWireMock options, final WireMockConfiguration serverOptions) {
        for (Class<? extends WireMockConfigurationCustomizer> customizer : options.configurationCustomizers()) {
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
                        .getAnnotationsByType(EnableWireMock.class))
                .map(EnableWireMock::value)
                .flatMap(Arrays::stream)
                .forEach(e -> resolveOrCreateWireMockServer(extensionContext, applicationContext, e));
    }

    @Override
    public void beforeEach(final ExtensionContext extensionContext) throws Exception {
        super.beforeEach(extensionContext);

        // reset all wiremock servers associated with application context
        applicationContext.getBeansOfType(WireMockServer.class).forEach(WireMockServer::resetAll);
        //Store.INSTANCE.findAllInstances(extensionContext).forEach(WireMockServer::resetAll);

        // inject properties into test class fields
        injectWireMockInstances(extensionContext, InjectWireMock.class, InjectWireMock::value);
    }

    private <T extends Annotation> void injectWireMockInstances(ExtensionContext extensionContext, Class<T> annotation, Function<T, String> fn) throws IllegalAccessException {
        // getRequiredTestInstances() return multiple instances for nested tests
        for (Object testInstance : extensionContext.getRequiredTestInstances().getAllInstances()) {
            List<Field> annotatedFields = AnnotationSupport.findAnnotatedFields(testInstance.getClass(), annotation);
            for (Field annotatedField : annotatedFields) {
                T annotationValue = annotatedField.getAnnotation(annotation);
                annotatedField.setAccessible(true);

                WireMockServer wiremock = getStore(extensionContext).get(annotationValue, WireMockServer.class);
                //Store.INSTANCE.findRequiredWireMockInstance(extensionContext, fn.apply(annotationValue));
                annotatedField.set(testInstance, wiremock);
            }
        }
    }

    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == WireMockServer.class && (parameterContext.isAnnotated(InjectWireMock.class));
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        final String wireMockServerName = parameterContext.findAnnotation(InjectWireMock.class)
                .get()
                .value();
        return getStore(extensionContext).get(wireMockServerName, WireMockServer.class);
    }

    private void resolveOrCreateWireMockServer(ExtensionContext extensionContext, final ApplicationContext context, final ConfigureWireMock options) {
        final WireMockServer wireMockServer = getStore(extensionContext).get(options.name(), WireMockServer.class);
        if (wireMockServer != null) {
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

        LOGGER.info("Configuring WireMockServer with name '{}' on port: {}", options.name(), serverOptions.portNumber());
        final WireMockServer newServer = new WireMockServer(serverOptions);
        newServer.start();
        LOGGER.info("Started WireMockServer with name '{}':{}", options.name(), newServer.baseUrl());

        // save server to store
        getStore(extensionContext).put(options.name(), newServer);

        // add shutdown hook
        context.registerSingleton(
                ShutdownEventForWiremock.class,
                new ShutdownEventForWiremock(newServer, options)
        );

        // configure Micronaut environment property
        if (StringUtils.isNotBlank(options.property())) {
            Map<String, Object> property = Map.of(options.property(), newServer.baseUrl());
            LOGGER.debug("Adding property '{}' to Micronaut application context", property);
            context.getEnvironment().addPropertySource(MapPropertySource.of("customSource", property));
        }
    }

    private String resolveStubLocation(final ConfigureWireMock options) {
        return StringUtils.isBlank(options.stubLocation()) ? "wiremock/" + options.name() : options.stubLocation();
    }

    // TODO
    public void createContextCustomizer(final Class<?> testClass, List<?> configAttributes) {
        // scan class and all enclosing classes if the test class is @Nested
        final var holder = new ConfigureWiremockHolder();
        holder.parse(testClass);
        //if (TestContextAnnotationUtils.searchEnclosingClass(testClass)) {
        //parseDefinitions(testClass.getEnclosingClass(), parser);
        //}

        if (holder.isEmpty()) {
            return;
        }
        //holder.asArray()
    }

    private static class ConfigureWiremockHolder {
        private final List<ConfigureWireMock> annotations = new ArrayList<>();

        void add(final ConfigureWireMock[] annotations) {
            this.annotations.addAll(Arrays.asList(annotations));
        }

        void parse(final Class<?> clazz) {
            findRepeatableAnnotations(clazz, EnableWireMock.class)
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
