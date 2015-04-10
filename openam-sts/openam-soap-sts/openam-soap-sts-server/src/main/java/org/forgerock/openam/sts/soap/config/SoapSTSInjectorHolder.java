/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.soap.config;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;

/**
 * Holds the Injector instance common to all Rest STS instances. Should only be called from contexts
 * where injection cannot be done - i.e. where Guice is not controlling instance lifecycle. Reference to
 * this Enum will trigger the creation of of the Injector with the common bindings. Modeled after the InjectorHolder
 * class, and taking advantage of the fact that enums are singletons.
 *
 * This class will be first referenced by the STSBroker class, the CXFNonSpringServlet subclass that is the entry point
 * for all invocations against the soap sts.
 */
public enum SoapSTSInjectorHolder {
    /**
     * The Singleton instance of the InjectorHolder.
     */
    INSTANCE;

    private final Injector injector;

    private SoapSTSInjectorHolder() {
        try {
            injector = Guice.createInjector(new SoapSTSModule());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the appropriate instance for the given injection key.
     * Avoid using this method, in favor of having Guice inject your dependencies ahead of time. This method is called
     * by the STSBroker, to obtain the SoapSTSLifecycle implementation which bootstraps the soap sts context. It is also
     * used by the SoapSTSContextListener, to shut-down the soap sts context, and by the SoapSTSInstancePublisherImpl, to
     * get this common injector, which is the parent of all Injectors corresponding to published soap-sts instances.
     * @param key The key that defines the class to get.
     * @param <T> The type of class defined by the key.
     * @return An instance of the class defined by the key.
     */
    public static <T> T getInstance(Key<T> key) {
        return INSTANCE.injector.getInstance(key);
    }
}
