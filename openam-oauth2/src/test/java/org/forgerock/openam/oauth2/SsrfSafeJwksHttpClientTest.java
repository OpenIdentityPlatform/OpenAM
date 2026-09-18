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
package org.forgerock.openam.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.testng.Assert.assertThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.forgerock.openam.oauth2.validation.JwksUriValidator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.am.util.SystemProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Covers the SSRF hardening of the {@code jwks_uri} fetch (GHSA-g7cv-hh35-cc7c): the non-HTTP
 * schemes that {@code URLConnection} would otherwise open, the address blocklist, and the
 * per-hop revalidation of redirects that the stock {@code SimpleHTTPClient} skips entirely.
 */
public class SsrfSafeJwksHttpClientTest {

    private static final String JWKS_BODY = "{\"keys\":[]}";

    private HttpServer server;
    private String baseUri;
    private String previousAllowAnyAddress;
    private final AtomicInteger jwksRequests = new AtomicInteger();
    private final SsrfSafeJwksHttpClient client = new SsrfSafeJwksHttpClient(3000, 3000);

    @BeforeMethod
    public void setUp() throws Exception {
        previousAllowAnyAddress = SystemProperties.get(JwksUriValidator.ALLOW_ANY_ADDRESS_PROPERTY);
        jwksRequests.set(0);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/jwks", exchange -> {
            jwksRequests.incrementAndGet();
            respond(exchange, 200, JWKS_BODY);
        });
        server.createContext("/slow", SsrfSafeJwksHttpClientTest::trickle);
        server.createContext("/huge", exchange -> respond(exchange, 200, oversizedBody()));
        server.createContext("/once", exchange -> redirect(exchange, 302, "/jwks"));
        server.createContext("/twice", exchange -> redirect(exchange, 301, "/once"));
        server.createContext("/loop", exchange -> redirect(exchange, 302, "/loop"));
        server.createContext("/to-file", exchange -> redirect(exchange, 302, "file:///etc/passwd"));
        server.createContext("/no-location", exchange -> {
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.start();
        baseUri = "http://127.0.0.1:" + server.getAddress().getPort();
        allowInternalAddresses(false);
    }

    @AfterMethod
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        // SystemProperties is a process-wide holder: hand back whatever was there rather than
        // asserting that the rest of the JVM wanted the default. There is no removal API, so an
        // absent property is restored as "false" — the documented default, and so indistinguishable
        // from absent to every reader of it.
        allowInternalAddresses(previousAllowAnyAddress == null
                ? "false"
                : previousAllowAnyAddress);
    }

    /**
     * The core of the advisory: {@code new URL("file:///etc/passwd")} is well-formed, and the
     * stock client would hand it straight to {@code URLConnection} and read the file.
     */
    @Test
    public void refusesNonHttpScheme() {
        assertThrows(IOException.class, () -> client.get(new URL("file:///etc/passwd")));
    }

    @Test
    public void refusesInternalAddressByDefault() {
        assertThrows(IOException.class, () -> client.get(new URL(baseUri + "/jwks")));
    }

    @Test
    public void fetchesJwksOnceInternalAddressesAreAllowed() throws Exception {
        allowInternalAddresses(true);

        assertThat(client.get(new URL(baseUri + "/jwks"))).isEqualTo(JWKS_BODY);
    }

    @Test
    public void followsRedirectsWithinBudget() throws Exception {
        allowInternalAddresses(true);

        assertThat(client.get(new URL(baseUri + "/twice")))
                .as("a JWK Set that has moved must still be fetched")
                .isEqualTo(JWKS_BODY);
    }

    /**
     * The redirect is where a URL that passed validation can still reach somewhere it must not:
     * every hop is revalidated, so the {@code file://} target is refused even though the address
     * check has been lifted.
     */
    @Test
    public void refusesRedirectToNonHttpScheme() {
        allowInternalAddresses(true);

        assertThrows(IOException.class, () -> client.get(new URL(baseUri + "/to-file")));
    }

    /**
     * The threat this class was written for: a {@code jwks_uri} on a genuinely public host passes
     * validation at registration and at the first hop, and then answers {@code 302} pointing at an
     * internal address.
     *
     * <p>A test HTTP server can only listen on loopback, which the blocklist rejects, so the entry
     * point is waved through by overriding {@link SsrfSafeJwksHttpClient#isSafe(URL)} — standing in
     * for the public host of the real attack — while every later hop faces the unmodified check.
     */
    @Test
    public void refusesRedirectToBlockedAddress() throws Exception {
        final URL entryPoint = new URL(baseUri + "/once");
        final SsrfSafeJwksHttpClient publicEntryPoint = new SsrfSafeJwksHttpClient(3000, 3000) {
            @Override
            boolean isSafe(URL url) {
                return url.toExternalForm().equals(entryPoint.toExternalForm()) || super.isSafe(url);
            }
        };

        assertThatThrownBy(() -> publicEntryPoint.get(entryPoint))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Refusing to fetch");

        assertThat(jwksRequests.get())
                .as("the redirect target is on a blocked address and must never be contacted")
                .isZero();
    }

    /**
     * The size ceiling bounds bytes, not time: an endpoint that answers one character at a time,
     * each well inside the read timeout, would otherwise hold a request thread for hundreds of
     * hours before the ceiling tripped.
     */
    @Test
    public void refusesResponseThatOutlivesTheFetchBudget() {
        allowInternalAddresses(true);
        final SsrfSafeJwksHttpClient impatient = new SsrfSafeJwksHttpClient(3000, 3000, 300);

        final long start = System.currentTimeMillis();
        assertThatThrownBy(() -> impatient.get(new URL(baseUri + "/slow")))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("milliseconds");

        assertThat(System.currentTimeMillis() - start)
                .as("the fetch must end on its own budget: after it, and well before the "
                        + "10-second response would have finished")
                .isBetween(300L, 5000L);
    }

    @Test
    public void refusesEndlessRedirects() {
        allowInternalAddresses(true);

        assertThrows(IOException.class, () -> client.get(new URL(baseUri + "/loop")));
    }

    @Test
    public void refusesRedirectWithoutLocation() {
        allowInternalAddresses(true);

        assertThrows(IOException.class, () -> client.get(new URL(baseUri + "/no-location")));
    }

    @Test
    public void refusesOversizedDocument() {
        allowInternalAddresses(true);

        assertThrows(IOException.class, () -> client.get(new URL(baseUri + "/huge")));
    }

    // --- helpers ---------------------------------------------------------------------------

    private static void allowInternalAddresses(boolean allow) {
        allowInternalAddresses(Boolean.toString(allow));
    }

    private static void allowInternalAddresses(String value) {
        SystemProperties.initializeProperties(JwksUriValidator.ALLOW_ANY_ADDRESS_PROPERTY, value);
    }

    /**
     * Answers forever, one character at a time, well inside the read timeout — the shape of
     * response that a per-read timeout and a size ceiling both fail to bound.
     */
    private static void trickle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, 0);
        try (OutputStream os = exchange.getResponseBody()) {
            for (int i = 0; i < 400; i++) {
                os.write('a');
                os.flush();
                Thread.sleep(25);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // The client gave up and closed the connection, which is what the test is asserting.
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void redirect(HttpExchange exchange, int status, String location) throws IOException {
        exchange.getResponseHeaders().add("Location", location);
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }

    /** Comfortably past the client's 512K character ceiling. */
    private static String oversizedBody() {
        final char[] filler = new char[768 * 1024];
        java.util.Arrays.fill(filler, 'a');
        return "{\"keys\":\"" + new String(filler) + "\"}";
    }
}
