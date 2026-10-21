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

package com.sun.identity.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.iplanet.am.util.SystemProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Covers the SSRF hardening of the federation workflow URL fetch (GHSA-rmw3-j353-w9f7): the
 * address blocklist on the URL an administrator supplies for metadata import and OpenID Connect
 * discovery, and its re-application to every redirect target.
 */
public class WebContentFetcherTest {

    private static final String METADATA = "<EntityDescriptor entityID=\"idp\"/>";

    private HttpServer server;
    private String baseUri;
    private String previousAllowAnyAddress;
    private final AtomicInteger metadataRequests = new AtomicInteger();
    private boolean stockKeyAbsent;
    private boolean stockAllowsLoopback;

    /**
     * Captures the stock behaviour before {@link #setUp()} writes the property: SystemProperties has
     * no removal API, so the absent state cannot be restored once a test has set it.
     */
    @BeforeClass
    public void captureStockDefault() throws Exception {
        stockKeyAbsent = SystemProperties.get(WebContentFetcher.ALLOW_ANY_ADDRESS_PROPERTY) == null;
        stockAllowsLoopback = new WebContentFetcher().isSafe(new URL("http://127.0.0.1/"));
    }

    @BeforeMethod
    public void setUp() throws Exception {
        previousAllowAnyAddress = SystemProperties.get(WebContentFetcher.ALLOW_ANY_ADDRESS_PROPERTY);
        metadataRequests.set(0);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/metadata", exchange -> {
            metadataRequests.incrementAndGet();
            respond(exchange, METADATA);
        });
        server.createContext("/once", exchange -> redirect(exchange, "/metadata"));
        server.createContext("/loop", exchange -> redirect(exchange, "/loop"));
        server.createContext("/to-file", exchange -> redirect(exchange, "file:///etc/passwd"));
        // /hops/<left>/<status>: <left> more redirects with <status>, then one to /metadata.
        server.createContext("/hops", exchange -> {
            final String[] parts = exchange.getRequestURI().getPath().split("/");
            final int left = Integer.parseInt(parts[2]);
            final int status = Integer.parseInt(parts[3]);
            redirect(exchange, left == 0 ? "/metadata" : "/hops/" + (left - 1) + "/" + status, status);
        });
        server.start();
        baseUri = "http://127.0.0.1:" + server.getAddress().getPort();
        allowInternalAddresses("false");
    }

    @AfterMethod
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        // SystemProperties has no removal API; an absent property is restored as "false", the
        // documented default and so indistinguishable from absent to every reader of it.
        allowInternalAddresses(previousAllowAnyAddress == null ? "false" : previousAllowAnyAddress);
    }

    /** The advisory itself: an internal URL must not be fetched, and the host never contacted. */
    @Test
    public void refusesInternalAddressByDefault() {
        assertThatThrownBy(() -> Task.getContent(baseUri + "/metadata", Locale.ROOT))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("is not fetched")
                .hasMessageContaining("internal or special-purpose");

        assertThat(metadataRequests.get()).isZero();
    }

    /** The advisory's fail-closed default: with the property unset, the address check is on. */
    @Test
    public void addressCheckIsOnWhenPropertyUnset() {
        assertThat(stockKeyAbsent).isTrue();
        assertThat(stockAllowsLoopback).isFalse();
    }

    /** The cloud metadata endpoint is refused before any connection is attempted. */
    @Test
    public void refusesLinkLocalMetadataEndpoint() {
        assertThatThrownBy(() -> new WebContentFetcher().fetch("http://169.254.169.254/latest/meta-data/"))
                .isInstanceOf(WebContentFetcher.UnsafeUrlException.class);
    }

    @Test
    public void fetchesOnceInternalAddressesAreAllowed() throws Exception {
        allowInternalAddresses("true");

        assertThat(Task.getContent(baseUri + "/metadata", Locale.ROOT)).isEqualTo(METADATA + "\n");
    }

    @Test
    public void followsRedirectsWithinBudget() throws Exception {
        allowInternalAddresses("true");

        assertThat(new WebContentFetcher().fetch(baseUri + "/once")).isEqualTo(METADATA + "\n");
    }

    @DataProvider
    public Object[][] redirectStatuses() {
        return new Object[][] {{300}, {301}, {302}, {303}, {307}, {308}};
    }

    /** A dropped status would hand the empty body of the 3xx response back as the document. */
    @Test(dataProvider = "redirectStatuses")
    public void followsEachRedirectStatus(int status) throws Exception {
        allowInternalAddresses("true");

        assertThat(new WebContentFetcher().fetch(baseUri + "/hops/0/" + status)).isEqualTo(METADATA + "\n");
    }

    /** {@code /hops/2} is three redirects and {@code /hops/3} is four. */
    @Test
    public void followsExactlyThreeRedirects() throws Exception {
        allowInternalAddresses("true");

        assertThat(new WebContentFetcher().fetch(baseUri + "/hops/2/302")).isEqualTo(METADATA + "\n");
        assertThatThrownBy(() -> new WebContentFetcher().fetch(baseUri + "/hops/3/302"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("redirects");
    }

    @Test
    public void givesUpOnRedirectLoop() {
        allowInternalAddresses("true");

        assertThatThrownBy(() -> new WebContentFetcher().fetch(baseUri + "/loop"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("redirects");
    }

    /** Lifting the address check never re-enables non-HTTP schemes reached through a redirect. */
    @Test
    public void refusesRedirectToNonHttpScheme() {
        allowInternalAddresses("true");

        assertThatThrownBy(() -> new WebContentFetcher().fetch(baseUri + "/to-file"))
                .isInstanceOf(WebContentFetcher.UnsafeUrlException.class);
    }

    @DataProvider
    public Object[][] hops() {
        return new Object[][] {
            {"https://a/md", "http://a/md", false},
            {"HTTPS://a/md", "http://a/md", false},
            {"http://a/md", "https://a/md", true},
            {"https://a/md", "https://b/md", true},
            {"http://a/md", "http://b/md", true},
        };
    }

    /** A redirect may upgrade to {@code https} but never leave it. */
    @Test(dataProvider = "hops")
    public void redirectNeverLeavesHttps(String from, String to, boolean allowed) throws Exception {
        assertThat(WebContentFetcher.isAllowedHop(new URL(from), new URL(to))).isEqualTo(allowed);
    }

    @Test
    public void reportsRedirectLeavingHttps() {
        final WebContentFetcher downgrading = new WebContentFetcher() {
            @Override
            String fetch(String url) throws IOException {
                throw new InsecureRedirectException(new URL(url), new URL("http://a/md"));
            }
        };

        assertThatThrownBy(() -> Task.getWebContent(downgrading, "https://a/md", Locale.ROOT))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("redirects from https");
    }

    /**
     * The redirect follower applies {@link WebContentFetcher#isAllowedHop} itself. The test server
     * speaks only plain {@code http}, so the {@code https} partner is a connection that answers
     * {@code 302} to an {@code http} URL; the downgrade must be refused before it is opened.
     */
    @Test
    public void fetchRefusesRedirectLeavingHttps() {
        final List<String> opened = new ArrayList<>();
        final WebContentFetcher httpsPartner = new WebContentFetcher() {
            @Override
            boolean isSafe(URL url) {
                return true;
            }

            @Override
            HttpURLConnection open(URL target) {
                opened.add(target.toExternalForm());
                return new HttpURLConnection(target) {
                    @Override
                    public int getResponseCode() {
                        return HTTP_MOVED_TEMP;
                    }

                    @Override
                    public String getHeaderField(String name) {
                        return "Location".equalsIgnoreCase(name) ? "http://a/md" : null;
                    }

                    @Override
                    public void connect() {
                    }

                    @Override
                    public void disconnect() {
                    }

                    @Override
                    public boolean usingProxy() {
                        return false;
                    }
                };
            }
        };

        assertThatThrownBy(() -> httpsPartner.fetch("https://a/md"))
                .isInstanceOf(WebContentFetcher.InsecureRedirectException.class);
        assertThat(opened).containsExactly("https://a/md");
    }

    /**
     * A host that does not resolve is reported as unreachable, as before the fix, not as resolving
     * to an internal address. {@code .invalid} never resolves (RFC 6761).
     */
    @Test
    public void reportsUnresolvableHostAsUnreachable() {
        assertThatThrownBy(() -> Task.getContent("http://metadata.invalid/md", Locale.ROOT))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("Unable to reach");
    }

    /** The scheme is refused before the host is resolved, so an unresolvable host does not mask it. */
    @Test
    public void refusesNonHttpSchemeEvenWhenHostDoesNotResolve() {
        assertThatThrownBy(() -> new WebContentFetcher().fetch("ftp://metadata.invalid/md"))
                .isInstanceOf(WebContentFetcher.UnsafeUrlException.class);
    }

    /**
     * A URL on a public host passes the check and then answers {@code 302} to an internal
     * address. {@code HttpURLConnection} would follow that on its own; here the target is
     * re-validated and refused.
     *
     * <p>A test server can only listen on loopback, which the blocklist rejects, so the entry
     * point is waved through by overriding {@link WebContentFetcher#isSafe(URL)} — standing in
     * for the public host — while the redirect target faces the unmodified check.
     */
    @Test
    public void refusesRedirectToInternalAddress() throws Exception {
        final URL entryPoint = new URL(baseUri + "/once");
        final WebContentFetcher publicEntryPoint = new WebContentFetcher() {
            @Override
            boolean isSafe(URL url) {
                return url.toExternalForm().equals(entryPoint.toExternalForm()) || super.isSafe(url);
            }
        };

        assertThatThrownBy(() -> Task.getWebContent(publicEntryPoint, entryPoint.toExternalForm(), Locale.ROOT))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("is not fetched")
                .hasMessageContaining("internal or special-purpose");

        assertThat(metadataRequests.get())
                .as("the redirect target is on a blocked address and must never be contacted")
                .isZero();
    }

    private static void allowInternalAddresses(String value) {
        SystemProperties.initializeProperties(WebContentFetcher.ALLOW_ANY_ADDRESS_PROPERTY, value);
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/xml");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void redirect(HttpExchange exchange, String location) throws IOException {
        redirect(exchange, location, 302);
    }

    private static void redirect(HttpExchange exchange, String location, int status) throws IOException {
        exchange.getResponseHeaders().add("Location", location);
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }
}
