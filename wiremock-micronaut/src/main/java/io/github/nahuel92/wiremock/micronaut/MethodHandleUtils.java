package io.github.nahuel92.wiremock.micronaut;

import com.github.tomakehurst.wiremock.extension.ExtensionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Utility class that uses Method Handles to create instances.
 */
public class MethodHandleUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(WireMockMicronautExtension.class);
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public static WireMockConfigurationCustomizer getCustomizer(final Class<? extends WireMockConfigurationCustomizer> customizer) {
        try {
            return getInstance(customizer, WireMockConfigurationCustomizer.class);
        } catch (final Throwable e) {
            if (e instanceof NoSuchMethodException) {
                LOGGER.error("Customizer '{}' must have a no-arg constructor", customizer, e);
            }
            throw new IllegalStateException(e);
        }
    }

    public static ExtensionFactory getExtensionFactory(final Class<? extends ExtensionFactory> extensionFactory) {
        try {
            return getInstance(extensionFactory, ExtensionFactory.class);
        } catch (final Throwable e) {
            throw new IllegalStateException(
                    "Couldn't create instance of ExtensionFactory: '" + extensionFactory.getName() + "'", e
            );
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getInstance(final Class<?> returnClass, final Class<? extends T> typeClass) throws Throwable {
        return (T) LOOKUP.findConstructor(returnClass, MethodType.methodType(void.class))
                .asType(MethodType.methodType(typeClass))
                .invoke();
    }
}