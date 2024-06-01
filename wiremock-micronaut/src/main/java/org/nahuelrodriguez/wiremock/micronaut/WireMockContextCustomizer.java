package org.nahuelrodriguez.wiremock.micronaut;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.context.ConfigurableApplicationContext;
import io.micronaut.context.DefaultApplicationContextBuilder;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.test.context.TestContext;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Attaches properties with urls pointing to {@link WireMockServer} instances to the Micronaut {@link Environment}.
 *
 * @author Nahuel Rodríguez
 */
public class WireMockContextCustomizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WireMockContextCustomizer.class);
    private final List<ConfigureWireMock> configuration;

    /**
     * Creates an instance of {@link WireMockContextCustomizer}.
     *
     * @param configurations the configurations
     */
    public WireMockContextCustomizer(final List<ConfigureWireMock> configurations) {
        this.configuration = configurations;
    }

    /**
     * Creates an instance of {@link WireMockContextCustomizer}.
     *
     * @param configurations the configurations
     */
    public WireMockContextCustomizer(final ConfigureWireMock[] configurations) {
        this(Arrays.asList(configurations));
    }

    public void customizeContext(ConfigurableApplicationContext context) {
        for (ConfigureWireMock configureWiremock : configuration) {
            resolveOrCreateWireMockServer(context, configureWiremock);
        }
    }

    private void resolveOrCreateWireMockServer(final ApplicationContext context, final ConfigureWireMock options) {
        WireMockServer wireMockServer = Store.INSTANCE.findWireMockInstance(context, options.name());

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
        Store.INSTANCE.store(context, options.name(), newServer);

        // add shutdown hook
        /*context.addApplicationListener(event -> {
            if (event instanceof ShutdownEvent) {
                LOGGER.info("Stopping WireMockServer with name '{}'", options.name());
                newServer.stop();
            }
        });*/

        // configure Micronaut environment property
        if (StringUtils.isNotBlank(options.property())) {
            String property = options.property() + "=" + newServer.baseUrl();
            LOGGER.debug("Adding property '{}' to Micronaut application context", property);

            context.getEnvironment().addPropertySource(PropertySource.of(property));
            context.getEnvironment().refresh();

            //TestPropertyValues.of(property).applyTo(context.getEnvironment());
        }
    }

    private static void applyCustomizers(final ConfigureWireMock options, final WireMockConfiguration serverOptions) {
        for (Class<? extends WireMockConfigurationCustomizer> customizer : options.configurationCustomizers()) {
            try {
                ReflectionUtils.newInstance(customizer).customize(serverOptions, options);
            } catch (Exception e) {
                if (e instanceof NoSuchMethodException) {
                    LOGGER.error("Customizer {} must have a no-arg constructor", customizer, e);
                }
                throw e;
            }
        }
    }

    private String resolveStubLocation(final ConfigureWireMock options) {
        return StringUtils.isBlank(options.stubLocation()) ? "wiremock/" + options.name() : options.stubLocation();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        final var that = (WireMockContextCustomizer) o;
        return Objects.equals(configuration, that.configuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configuration);
    }

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
