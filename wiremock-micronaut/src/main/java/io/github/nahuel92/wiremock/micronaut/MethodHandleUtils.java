package io.github.nahuel92.wiremock.micronaut;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.ExtensionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class MethodHandleUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(WireMockMicronautExtension.class);
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public static void applyCustomizer(final ConfigureWireMock options, final WireMockConfiguration serverOptions,
                                       Class<? extends WireMockConfigurationCustomizer> customizer) {
        try {
            getInstance(customizer, WireMockConfigurationCustomizer.class).customize(serverOptions, options);
        } catch (final Throwable e) {
            if (e instanceof NoSuchMethodException) {
                LOGGER.error("Customizer '{}' must have a no-arg constructor", customizer, e);
            }
            throw new IllegalStateException(e);
        }
    }

    public static ExtensionFactory getInstance(final Class<? extends ExtensionFactory> extensionFactory) {
        try {
            return getInstance(extensionFactory, ExtensionFactory.class);
        } catch (final Throwable ex) {
            throw new IllegalStateException(
                    "Couldn't create instance of ExtensionFactory: '" + extensionFactory.getName() + "'", ex
            );
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getInstance(final Class<?> rClass, final Class<? extends T> tClass) throws Throwable {
        return (T) LOOKUP.findConstructor(rClass, MethodType.methodType(void.class))
                .asType(MethodType.methodType(tClass))
                .invoke();
    }
}
