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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.conn.DnsResolver;
import org.forgerock.http.Client;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownManager;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.google.inject.spi.Elements;
import com.google.inject.spi.ProviderKeyBinding;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Covers the DNS rebinding half of GHSA-7q42-6xr7-9rmw: the {@code UMA} client checks the
 * addresses it connects to, whatever the check made before the fetch saw. The scope host
 * {@code scopes.test} stands for a name whose DNS answer has changed to a loopback address by the
 * time the client connects.
 */
public class ScopeDescriptionClientProviderTest {

    private static final String HOST = "scopes.test";
    private static final InetAddress LOOPBACK = InetAddress.getLoopbackAddress();

    private final AtomicInteger scopeRequests = new AtomicInteger();
    private HttpServer server;
    private String baseUri;

    @BeforeMethod
    public void setUp() throws IOException {
        scopeRequests.set(0);
        server = HttpServer.create(new InetSocketAddress(LOOPBACK, 0), 0);
        server.createContext("/scope", exchange -> {
            scopeRequests.incrementAndGet();
            respond(exchange, 200, "{\"name\":\"Read photos\"}");
        });
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", baseUri + "/scope");
            respond(exchange, 302, "");
        });
        server.start();
        baseUri = "http://" + HOST + ":" + server.getAddress().getPort();
    }

    @AfterMethod
    public void tearDown() {
        server.stop(0);
    }

    @Test
    public void doesNotConnectWhenTheHostNowResolvesToABlockedAddress() throws Exception {
        Response response = send(client(false), baseUri + "/scope");

        assertThat(response.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR);
        assertThat(scopeRequests).hasValue(0);
    }

    @Test
    public void connectsToAnInternalAddressOnceInternalAddressesAreAllowed() throws Exception {
        Response response = send(client(true), baseUri + "/scope");

        assertThat(response.getStatus()).isEqualTo(Status.OK);
        assertThat(scopeRequests).hasValue(1);
    }

    /** The redirect would otherwise reach a URL that no check has seen. */
    @Test
    public void doesNotFollowRedirects() throws Exception {
        Response response = send(client(true), baseUri + "/redirect");

        assertThat(response.getStatus()).isEqualTo(Status.FOUND);
        assertThat(scopeRequests).hasValue(0);
    }

    /** IP literals keep the system resolver out of the test: they resolve to themselves. */
    @Test
    public void refusesAHostWithAnyBlockedAddress() {
        DnsResolver resolver = new ScopeDescriptionClientProvider.AddressCheckingDnsResolver(
                host -> new InetAddress[] {address("8.8.8.8"), address("169.254.169.254")}, () -> false);

        assertThatThrownBy(() -> resolver.resolve(HOST)).isInstanceOf(UnknownHostException.class);
    }

    @Test
    public void resolvesAHostWithOnlyPublicAddresses() throws Exception {
        DnsResolver resolver = new ScopeDescriptionClientProvider.AddressCheckingDnsResolver(
                host -> new InetAddress[] {address("8.8.8.8")}, () -> false);

        assertThat(resolver.resolve(HOST)).containsExactly(address("8.8.8.8"));
    }

    /** The client the server actually uses: default settings, system resolver. */
    @Test
    public void providedClientDoesNotConnectToLoopback() throws Exception {
        ShutdownManager shutdownManager = mock(ShutdownManager.class);
        Client client = new ScopeDescriptionClientProvider(shutdownManager).get();
        try {
            Response response = send(client, "http://localhost:" + server.getAddress().getPort() + "/scope");

            assertThat(response.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR);
            assertThat(scopeRequests).hasValue(0);
        } finally {
            ArgumentCaptor<ShutdownListener> listener = ArgumentCaptor.forClass(ShutdownListener.class);
            verify(shutdownManager).addShutdownListener(listener.capture());
            listener.getValue().shutdown();
        }
    }

    @Test
    public void umaClientIsBoundToThisProvider() {
        Key<Client> umaClient = Key.get(Client.class, Names.named("UMA"));

        assertThat(Elements.getElements(new UmaGuiceModule()))
                .filteredOn(element -> element instanceof ProviderKeyBinding
                        && umaClient.equals(((ProviderKeyBinding<?>) element).getKey()))
                .extracting(element -> (Object) ((ProviderKeyBinding<?>) element).getProviderKey())
                .containsExactly(Key.get(ScopeDescriptionClientProvider.class));
    }

    private static Client client(boolean anyAddressAllowed) {
        DnsResolver rebound = host -> new InetAddress[] {LOOPBACK};
        return new ScopeDescriptionClientProvider.ApacheClient(ScopeDescriptionClientProvider.newApacheClient(
                new ScopeDescriptionClientProvider.AddressCheckingDnsResolver(rebound, () -> anyAddressAllowed),
                false)).asClient();
    }

    private static Response send(Client client, String uri) throws Exception {
        return client.send(new Request().setMethod("GET").setUri(uri)).getOrThrowUninterruptibly();
    }

    private static InetAddress address(String literal) {
        try {
            return InetAddress.getByName(literal);
        } catch (UnknownHostException e) {
            throw new AssertionError(e);
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
