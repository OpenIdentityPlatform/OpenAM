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

package com.sun.identity.security.cert;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * A loopback HTTP server standing in for a CRL Distribution Point, counting the requests it
 * receives so a test can assert that no fetch was attempted at all.
 */
final class TestHttpServer implements Closeable {

    private final HttpServer server;
    private final AtomicInteger requests = new AtomicInteger();

    TestHttpServer(HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", (HttpExchange exchange) -> {
            requests.incrementAndGet();
            handler.handle(exchange);
        });
        server.start();
    }

    static HttpHandler respond(int status, byte[] body) {
        return exchange -> {
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        };
    }

    /**
     * Responds without a {@code Content-Length}: passing 0 as the response length makes
     * {@link HttpExchange} chunk the response, so the fetcher has no header to pre-check the
     * size against and only its own read accounting can bound it.
     */
    static HttpHandler respondChunked(int status, byte[] body) {
        return exchange -> {
            exchange.sendResponseHeaders(status, 0);
            exchange.getResponseBody().write(body);
            exchange.close();
        };
    }

    /**
     * Sends the response headers and one byte of the body, then stops — a distribution point
     * that stalls or drip-feeds. Gives up after {@code maxMillis} so a failing test cannot hang
     * the build indefinitely.
     */
    static HttpHandler stallAfterHeaders(long maxMillis) {
        return exchange -> {
            exchange.sendResponseHeaders(200, 4096);
            exchange.getResponseBody().write(new byte[]{'x'});
            exchange.getResponseBody().flush();
            try {
                Thread.sleep(maxMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        };
    }

    static HttpHandler redirectTo(String location) {
        return redirectTo(302, location);
    }

    static HttpHandler redirectTo(int status, String location) {
        return exchange -> {
            exchange.getResponseHeaders().set("Location", location);
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        };
    }

    /**
     * Adds each request to {@code seen} as its method and body — {@code "POST secret=value"}, or
     * {@code "GET "} — before {@code handler} answers it, so a test can assert on what reached
     * the server rather than on what the fetcher meant to send.
     */
    static HttpHandler recording(List<String> seen, HttpHandler handler) {
        return exchange -> {
            byte[] body = exchange.getRequestBody().readAllBytes();
            seen.add(exchange.getRequestMethod() + " " + new String(body, StandardCharsets.UTF_8));
            handler.handle(exchange);
        };
    }

    /**
     * Waits {@code millis} before redirecting — a distribution point that answers, but slowly.
     * Stalling ahead of the status line is what makes a hop cost the connection's read timeout
     * rather than nothing, so the redirect chain is where a per-hop budget shows up as a
     * multiplied wait.
     *
     * <p>The location is read when the request arrives rather than taken up front, so a handler
     * can point at the server it is installed on, whose port is only known afterwards.
     */
    static HttpHandler redirectAfter(long millis, Supplier<String> location) {
        return exchange -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.getResponseHeaders().set("Location", location.get());
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        };
    }

    String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/crl";
    }

    int requestCount() {
        return requests.get();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
