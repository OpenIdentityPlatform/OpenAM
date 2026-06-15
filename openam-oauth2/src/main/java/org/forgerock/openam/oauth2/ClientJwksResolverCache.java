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
 * Copyright 2026 3A Systems LLC.
 */
package org.forgerock.openam.oauth2;

import java.security.AccessController;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;

import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver;

/**
 * Process-wide cache of {@link OpenIdResolver} instances used by
 * {@link OpenAMClientRegistration#byJWKsURI(org.forgerock.oauth2.core.OAuth2Jwt)} to validate
 * {@code private_key_jwt} client assertions against a registration's {@code jwks_uri}.
 *
 * <p>This cache replaces the shared {@code OpenIdResolverServiceImpl} singleton for the
 * {@code byJWKsURI} path. The shared service uses its single string argument as both the
 * map key and the resolver's bound issuer; binding the key to {@code clientId|jwks_uri}
 * (as required by GHSA-f2cx-463q-7m2c, fix&nbsp;#2) would therefore overwrite the JWT
 * issuer check inside {@code BaseOpenIdResolver.verifyIssuer} and break every legitimate
 * assertion. Keeping our own map decouples the cache key from the resolver's bound
 * issuer.
 *
 * <p>The cache is keyed by {@code clientId|jwks_uri}: each registration owns its own
 * resolver and two registrations with the same JWT {@code iss} cannot collide.
 *
 * <p>An SMS {@link ServiceListener} is lazily registered (once per JVM) on the services
 * that own OAuth2 client registrations ({@code AgentService}) and the OAuth2 provider
 * configuration ({@code OAuth2Provider}). On any configuration change the cache is
 * cleared so that the next request re-fetches the current {@code jwks_uri}.
 *
 * <p>Visible for testing.
 */
final class ClientJwksResolverCache {

    private static final Debug LOGGER = Debug.getInstance("OAuth2Provider");

    private static final ConcurrentMap<String, OpenIdResolver> CACHE = new ConcurrentHashMap<>();
    private static final AtomicBoolean LISTENER_REGISTERED = new AtomicBoolean(false);

    private ClientJwksResolverCache() {
    }

    /** Returns the resolver for the given cache key or {@code null}. */
    static OpenIdResolver get(String cacheKey) {
        return CACHE.get(cacheKey);
    }

    /**
     * Install {@code resolver} under {@code cacheKey} if no entry exists yet, otherwise
     * return the existing entry. Also ensures the SMS invalidation listener is registered.
     *
     * <p>Note: this is a {@link java.util.concurrent.ConcurrentMap#putIfAbsent}-style API
     * — the caller has already constructed the resolver, so naming it after the lazy
     * {@code computeIfAbsent} would be misleading.
     *
     * @return the resolver now stored under {@code cacheKey} (never {@code null}).
     */
    static OpenIdResolver putIfAbsent(String cacheKey, OpenIdResolver resolver) {
        ensureListenerRegistered();
        OpenIdResolver existing = CACHE.putIfAbsent(cacheKey, resolver);
        return existing != null ? existing : resolver;
    }

    /** Drop everything. Called by the SMS listener on configuration changes. */
    static void invalidateAll() {
        CACHE.clear();
    }

    /** Visible for testing. */
    static int size() {
        return CACHE.size();
    }

    /** Visible for testing. */
    static boolean contains(String cacheKey) {
        return CACHE.containsKey(cacheKey);
    }

    /** Visible for testing. */
    static void resetForTest() {
        CACHE.clear();
        LISTENER_REGISTERED.set(false);
    }

    private static void ensureListenerRegistered() {
        if (!LISTENER_REGISTERED.compareAndSet(false, true)) {
            return;
        }
        try {
            final SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
            registerListener(token, "AgentService", "1.0");
            registerListener(token, "OAuth2Provider", "1.0");
        } catch (Exception e) {
            // Unit-test or partially initialised environment: tolerate and retry on the next
            // cache-miss. Losing dynamic invalidation does not affect the security boundary
            // because the cache key is already bound to (clientId | jwks_uri).
            LISTENER_REGISTERED.set(false);
            if (LOGGER.warningEnabled()) {
                LOGGER.warning("ClientJwksResolverCache: could not register SMS listener: " + e);
            }
        }
    }

    private static void registerListener(SSOToken token, String serviceName, String version) {
        try {
            ServiceConfigManager scm = new ServiceConfigManager(token, serviceName, version);
            if (scm.addListener(new InvalidateOnChange()) == null) {
                LOGGER.warning("ClientJwksResolverCache: addListener returned null for " + serviceName);
            }
        } catch (Exception e) {
            if (LOGGER.warningEnabled()) {
                LOGGER.warning("ClientJwksResolverCache: failed to add listener for " + serviceName
                        + ": " + e);
            }
        }
    }

    private static final class InvalidateOnChange implements ServiceListener {
        @Override
        public void schemaChanged(String serviceName, String version) {
            invalidateAll();
        }

        @Override
        public void globalConfigChanged(String serviceName, String version, String groupName,
                String serviceComponent, int type) {
            invalidateAll();
        }

        @Override
        public void organizationConfigChanged(String serviceName, String version, String orgName,
                String groupName, String serviceComponent, int type) {
            invalidateAll();
        }
    }
}


