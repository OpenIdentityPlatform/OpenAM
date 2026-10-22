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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openam.services.baseurl;

/**
 * Tells apart the base URL providers that read the request from the ones that do not.
 */
public final class BaseURLProviders {

    /**
     * How many extension wrappers are unwrapped before the chain is given up on. A realm names one extension class,
     * so a chain of any length is already a deployment doing something odd; the limit is only here so that a cycle
     * cannot spin the request thread.
     */
    private static final int MAX_EXTENSION_DEPTH = 8;

    private BaseURLProviders() {
    }

    /**
     * Tells whether the base URL a provider yields is built out of the incoming request.
     *
     * <p>Anything a caller can put in a request - the <code>Forwarded</code> and <code>X-Forwarded-*</code> headers,
     * the host it was addressed to - is a value the caller chose, unless every route into the container rewrites it.
     * A base URL built out of one may be perfectly good for rendering links back to OpenAM, but it says nothing
     * about which origins this server actually answers on, so security decisions may not rest on it.
     *
     * <p>The extension provider is asked about its delegate rather than about itself: it is a wrapper around the
     * class named in <code>base-url-extension-class</code>, and that setting takes any {@link BaseURLProvider},
     * including the header based ones shipped here. What the wrapper is asked to wrap is what decides the answer. An
     * extension class of your own is taken at its word - a provider that reads request headers of its own accord
     * can not be told apart from one that does not - so an extension used on a realm whose goto URL list is empty
     * has to yield an origin the deployment fixed rather than one the caller supplied.
     *
     * @param provider The provider configured for a realm, may be <code>null</code>.
     * @return <code>true</code> if its base URL comes from the request, or if there is no provider to ask.
     */
    public static boolean isRequestDerived(final BaseURLProvider provider) {
        BaseURLProvider actual = provider;
        for (int depth = 0; actual instanceof ExtensionBaseURLProvider; depth++) {
            if (depth == MAX_EXTENSION_DEPTH) {
                // Extension providers wrapping one another round in a circle: there is no provider at the end of
                // the chain to judge by, and a security decision may not hang the request thread looking for one.
                return true;
            }
            final BaseURLProvider delegate = ((ExtensionBaseURLProvider) actual).getDelegate();
            if (delegate == actual) {
                // An extension class naming the wrapper itself: there is nothing behind it to trust.
                return true;
            }
            actual = delegate;
        }
        return actual == null
                || actual instanceof RequestValuesBaseURLProvider
                || actual instanceof ForwardedHeaderBaseURLProvider
                || actual instanceof XForwardedHeadersBaseURLProvider;
    }
}
