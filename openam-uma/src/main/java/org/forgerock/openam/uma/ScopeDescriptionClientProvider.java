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

package org.forgerock.openam.uma;

import jakarta.inject.Inject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ProxySelector;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import com.google.inject.Provider;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.common.SsrfUrlValidator;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.DnsResolver;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.forgerock.http.Client;
import org.forgerock.http.apache.AbstractHttpClient;
import org.forgerock.http.apache.NoAuthenticationStrategy;
import org.forgerock.http.io.IO;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Responses;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.thread.listener.ShutdownManager;

/**
 * Provides the {@code UMA} client that fetches scope descriptions (GHSA-7q42-6xr7-9rmw).
 *
 * <p>{@link PendingRequestEmailTemplate#isScopeDescriptionUriAllowed} checks the addresses a scope
 * host resolves to, but the HTTP client resolves the host again when it connects, so a host whose
 * DNS answer changes in between (DNS rebinding) would be fetched at an address that was never
 * checked. This client therefore checks the addresses in its own DNS resolver: the addresses it
 * checks are the addresses it connects to. Redirects are not followed.
 *
 * <p>When the system proxy is enabled ({@link Constants#SYSTEM_PROXY_ENABLED}), this client
 * resolves host names without the check, because the proxy's own address — usually an internal
 * one — must stay reachable. Only the check made before the fetch applies then, also to hosts the
 * {@link ProxySelector} sends direct rather than through the proxy (e.g. {@code http.nonProxyHosts}),
 * which this client resolves itself.
 */
class ScopeDescriptionClientProvider implements Provider<Client> {

    private static final Debug DEBUG = Debug.getInstance("UmaProvider");
    private static final int TIMEOUT_MILLIS = (int) TimeUnit.SECONDS.toMillis(10);

    private final ShutdownManager shutdownManager;

    @Inject
    ScopeDescriptionClientProvider(ShutdownManager shutdownManager) {
        this.shutdownManager = shutdownManager;
    }

    @Override
    public Client get() {
        final boolean systemProxy = SystemProperties.getAsBoolean(Constants.SYSTEM_PROXY_ENABLED, false);
        final ApacheClient client = new ApacheClient(newApacheClient(
                systemProxy ? SystemDefaultDnsResolver.INSTANCE
                        : new AddressCheckingDnsResolver(SystemDefaultDnsResolver.INSTANCE,
                                PendingRequestEmailTemplate::isAnyScopeDescriptionAddressAllowed),
                systemProxy));
        shutdownManager.addShutdownListener(() -> {
            try {
                client.close();
            } catch (IOException e) {
                DEBUG.message("Unable to close the scope description client", e);
            }
        });
        return client.asClient();
    }

    static CloseableHttpClient newApacheClient(DnsResolver dnsResolver, boolean systemProxy) {
        final HttpClientBuilder builder = HttpClientBuilder.create()
                .setDnsResolver(dnsResolver)
                .disableRedirectHandling()
                .disableCookieManagement()
                .setTargetAuthenticationStrategy(NoAuthenticationStrategy.INSTANCE)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(TIMEOUT_MILLIS)
                        // A connection the pool never gets back fails the fetch instead of
                        // blocking the request thread.
                        .setConnectionRequestTimeout(TIMEOUT_MILLIS)
                        .setSocketTimeout(TIMEOUT_MILLIS)
                        .build());
        if (systemProxy) {
            builder.setRoutePlanner(new SystemDefaultRoutePlanner(ProxySelector.getDefault()));
        }
        return builder.build();
    }

    /**
     * Refuses to resolve a host when any of its addresses is blocked by {@link SsrfUrlValidator},
     * unless {@link PendingRequestEmailTemplate#ALLOW_ANY_SCOPE_DESCRIPTION_ADDRESS} is set. The
     * connection then fails, and the scope is shown as it is.
     */
    static final class AddressCheckingDnsResolver implements DnsResolver {

        private final DnsResolver delegate;
        private final BooleanSupplier anyAddressAllowed;

        AddressCheckingDnsResolver(DnsResolver delegate, BooleanSupplier anyAddressAllowed) {
            this.delegate = delegate;
            this.anyAddressAllowed = anyAddressAllowed;
        }

        @Override
        public InetAddress[] resolve(String host) throws UnknownHostException {
            final InetAddress[] addresses = delegate.resolve(host);
            if (anyAddressAllowed.getAsBoolean()) {
                return addresses;
            }
            for (InetAddress address : addresses) {
                if (SsrfUrlValidator.isBlockedAddress(address)) {
                    DEBUG.warning("Not connecting to the scope description host " + host + ": it resolves to "
                            + address.getHostAddress() + ", a private, loopback or link-local address.");
                    throw new UnknownHostException(host + " resolves to a blocked address");
                }
            }
            return addresses;
        }
    }

    /** Adapts an Apache client to CHF, the way the CHF synchronous Apache client does. */
    static final class ApacheClient extends AbstractHttpClient {

        private final CloseableHttpClient httpClient;

        ApacheClient(CloseableHttpClient httpClient) {
            super(IO.newTemporaryStorage());
            this.httpClient = httpClient;
        }

        Client asClient() {
            return new Client((context, request) -> sendAsync(request));
        }

        @Override
        public Promise<Response, NeverThrowsException> sendAsync(Request request) {
            try {
                return Promises.newResultPromise(createResponse(httpClient.execute(createHttpUriRequest(request))));
            } catch (IOException e) {
                return Promises.newResultPromise(Responses.newInternalServerError(e));
            }
        }

        @Override
        public void close() throws IOException {
            httpClient.close();
        }
    }
}
