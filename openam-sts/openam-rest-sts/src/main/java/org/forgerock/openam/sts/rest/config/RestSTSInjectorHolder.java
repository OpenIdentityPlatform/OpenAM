package org.forgerock.openam.sts.rest.config;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;

/**
 * Holds the Injector instance common to all Rest STS instances. Should only be called from contexts
 * where injection cannot be done - i.e. where Guice is not controlling instance lifecycle. Reference to
 * this Enum will trigger the creation of of the Injector with the common bindings. Modeled after the InjectorHolder
 * class, and taking advantage of the fact that enums are singletons.
 *
 * The RestSTSInjectorHolder is called only from the RestSTSServiceConnectionFactoryProvider, the class which Crest
 * invokes to obtain a ConnectionFactory. This call triggers the invocation of the no-arg constructor below, which triggers
 * the bindings common to all rest-sts instances - i.e. those related to the publication of rest-sts instances, and the
 * ConnectionFactory which allows Crest to invoke every published instance.
 */
public enum RestSTSInjectorHolder {
    /**
     * The Singleton instance of the InjectorHolder.
     */
    INSTANCE;

    private final Injector injector;

    private RestSTSInjectorHolder() {
        try {
            injector = Guice.createInjector(new RestSTSModule());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the appropriate instance for the given injection key.
     * Avoid using this method, in favor of having Guice inject your dependencies ahead of time.
     * Is only called by the RestSTSServiceConnectionFactoryProvider, and by the RestSTSInstanceModule. The
     * RestSTSInstanceModule uses it to obtain the global url elements which allow each Rest STS instance
     * to integrate with OpenAM -e.g. /authenticate, /sessions?_action=logout, /users/?_action=idFromSession, etc).
     *
     * @param key The key that defines the class to get.
     * @param <T> The type of class defined by the key.
     * @return An instance of the class defined by the key.
     */
    public static <T> T getInstance(Key<T> key) {
        return INSTANCE.injector.getInstance(key);
    }
}
