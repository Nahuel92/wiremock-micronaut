package io.github.nahuel92.wiremock.micronaut;

import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.ExtensionFactory;

import java.util.ArrayList;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Helper class to map a {@link ConfigureWireMock} into a {@link WireMockConfiguration}.
 */
class WireMockConfigurationHelper {
    public static WireMockConfiguration getConfiguration(final ConfigureWireMock options) {
        final var serverOptions = options()
                .port(options.port())
                .notifier(new Slf4jNotifier(true));
        if (options.extensions().length > 0) {
            serverOptions.extensions(options.extensions());
        }
        resolveStubLocation(options, serverOptions);
        applyCustomizers(options, serverOptions);
        serverOptions.extensions(getExtensionFactories(options));
        return serverOptions;
    }

    private static void resolveStubLocation(final ConfigureWireMock options, final WireMockConfiguration serverOptions) {
        if (options.stubLocationOnClasspath()) {
            final var stubLocation = StringUtils.defaultIfBlank(options.stubLocation(), "wiremock/" + options.name());
            serverOptions.usingFilesUnderClasspath(stubLocation);
            return;
        }
        serverOptions.usingFilesUnderDirectory(options.stubLocation());
    }

    private static void applyCustomizers(final ConfigureWireMock options, final WireMockConfiguration serverOptions) {
        for (final var customizer : options.configurationCustomizers()) {
            MethodHandleUtils.getCustomizer(customizer).customize(serverOptions, options);
        }
    }

    private static ExtensionFactory[] getExtensionFactories(final ConfigureWireMock options) {
        final var extensionFactories = new ArrayList<ExtensionFactory>();
        for (final var extensionFactory : options.extensionFactories()) {
            extensionFactories.add(MethodHandleUtils.getExtensionFactory(extensionFactory));
        }
        return extensionFactories.toArray(ExtensionFactory[]::new);
    }
}