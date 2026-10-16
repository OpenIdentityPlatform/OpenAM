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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.forgerock.jaspi.modules.openid.helpers.SimpleHTTPClient;
import org.forgerock.openam.oauth2.validation.JwksUriValidator;

import com.sun.identity.common.HttpURLConnectionManager;

/**
 * Fetches a client's JWK Set the way {@link SimpleHTTPClient} does, but refuses to be used as an
 * SSRF gadget — GHSA-g7cv-hh35-cc7c.
 *
 * <p>{@code SimpleHTTPClient} calls {@link URL#openConnection()} on whatever URL it is handed and
 * lets {@code HttpURLConnection} chase redirects on its own. Both are a problem for a
 * {@code jwks_uri}, which an OAuth2 client chooses: the first accepts {@code file://} and other
 * non-HTTP schemes, and the second means a validated {@code https://public.example/jwks} can hand
 * the server a {@code 302} to an internal address after the check has already passed. This
 * subclass therefore validates every hop with {@link JwksUriValidator} and follows redirects
 * itself.
 *
 * <p>Redirects are followed rather than refused because JWK Sets legitimately move (an
 * {@code http} endpoint upgrading to {@code https}, a path settling on
 * {@code /.well-known/jwks.json}); refusing them outright would break working registrations for
 * no security gain, given that each hop is re-validated.
 */
public class SsrfSafeJwksHttpClient extends SimpleHTTPClient {

    /**
     * How many {@code 3xx} hops are followed before the fetch is abandoned. Enough for the
     * scheme upgrades and path moves seen in practice, low enough to bound a redirect loop.
     *
     * <p>{@code HttpURLConnection} would have followed up to 20 by default; this is deliberately
     * lower, and is documented in the administration guide as such.
     */
    private static final int MAX_REDIRECTS = 3;

    /**
     * Wall-clock budget for the whole fetch, redirects included.
     *
     * <p>The read timeout bounds a single {@code read()}, not the fetch: a hostile endpoint that
     * emits one byte just inside the read timeout keeps the calling thread — a request thread of
     * the unauthenticated {@code /oauth2/idtokeninfo} and token endpoints — busy for as long as it
     * likes, and {@link #MAX_RESPONSE_CHARS} only trips after half a million such reads. The
     * deadline is checked between reads and between hops, so it bounds a slow trickle rather than
     * a peer that has gone completely silent; the latter is what the read timeout is for.
     */
    private static final int DEFAULT_MAX_TOTAL_FETCH_MS = 10_000;

    /** {@code HttpURLConnection} has no constant for {@code 307 Temporary Redirect}. */
    private static final int HTTP_TEMPORARY_REDIRECT = 307;

    /** {@code HttpURLConnection} has no constant for {@code 308 Permanent Redirect}. */
    private static final int HTTP_PERMANENT_REDIRECT = 308;

    /**
     * Upper bound on the size of a JWK Set document. Real ones hold a handful of keys and run to
     * a few kilobytes; the cap stops a hostile endpoint from feeding the server an unbounded
     * response, which the superclass would buffer whole.
     */
    private static final int MAX_RESPONSE_CHARS = 512 * 1024;

    private final int readTimeout;
    private final int connTimeout;
    private final long maxTotalFetchMs;

    /**
     * Constructs a client with the given timeouts.
     *
     * @param readTimeout read timeout in milliseconds.
     * @param connTimeout connection timeout in milliseconds.
     */
    public SsrfSafeJwksHttpClient(int readTimeout, int connTimeout) {
        this(readTimeout, connTimeout, DEFAULT_MAX_TOTAL_FETCH_MS);
    }

    /**
     * Visible for testing: lets a test shorten the overall fetch budget so that the slow-trickle
     * case can be exercised in milliseconds rather than in the hours it takes in production.
     *
     * @param readTimeout read timeout in milliseconds.
     * @param connTimeout connection timeout in milliseconds.
     * @param maxTotalFetchMs wall-clock budget for the whole fetch, redirects included.
     */
    SsrfSafeJwksHttpClient(int readTimeout, int connTimeout, long maxTotalFetchMs) {
        super(readTimeout, connTimeout);
        this.readTimeout = readTimeout;
        this.connTimeout = connTimeout;
        this.maxTotalFetchMs = maxTotalFetchMs;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException if the URL — or any URL it redirects to — is not safe for the server to
     *         fetch, if the redirect budget or the overall time budget is exhausted, or if the
     *         fetch itself fails.
     */
    @Override
    public String get(final URL url) throws IOException {
        final long deadline = System.currentTimeMillis() + maxTotalFetchMs;
        URL target = url;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            checkDeadline(deadline);
            validate(target);
            final HttpURLConnection connection = open(target);
            try {
                if (isRedirect(connection.getResponseCode())) {
                    target = redirectTarget(connection, target);
                    continue;
                }
                return readBody(connection, deadline);
            } finally {
                connection.disconnect();
            }
        }
        throw new IOException("Gave up fetching the JWK Set after " + MAX_REDIRECTS + " redirects");
    }

    /**
     * Whether the server may fetch {@code url}, as decided by {@link JwksUriValidator}.
     *
     * <p>Visible for testing, and overridable only from this package: the address blocklist
     * rejects the loopback address that a test HTTP server necessarily listens on, so a test that
     * needs the first hop to be reachable and every later hop to face the real blocklist has no
     * other seam.
     *
     * @param url the URL about to be fetched — the original {@code jwks_uri} or a redirect target.
     * @return {@code true} if the URL is safe for the server to fetch.
     */
    boolean isSafe(URL url) {
        return JwksUriValidator.isSafe(url.toExternalForm());
    }

    private void validate(URL url) throws IOException {
        if (!isSafe(url)) {
            // The URL is left out of the message on purpose: this travels out as the cause of a
            // FailedToLoadJWKException, and the caller already logs the URL against the client id.
            throw new IOException("Refusing to fetch a JWK Set from a URL that does not use http "
                    + "or https, or that resolves to a private, loopback or link-local address");
        }
    }

    private void checkDeadline(long deadline) throws IOException {
        if (System.currentTimeMillis() > deadline) {
            throw new IOException("Gave up fetching the JWK Set after " + maxTotalFetchMs
                    + " milliseconds");
        }
    }

    private HttpURLConnection open(URL url) throws IOException {
        final HttpURLConnection connection = HttpURLConnectionManager.getConnection(url);
        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(false);
        connection.setReadTimeout(readTimeout);
        connection.setConnectTimeout(connTimeout);
        return connection;
    }

    private static boolean isRedirect(int status) {
        return status == HttpURLConnection.HTTP_MOVED_PERM
                || status == HttpURLConnection.HTTP_MOVED_TEMP
                || status == HttpURLConnection.HTTP_SEE_OTHER
                || status == HTTP_TEMPORARY_REDIRECT
                || status == HTTP_PERMANENT_REDIRECT;
    }

    private static URL redirectTarget(HttpURLConnection connection, URL current) throws IOException {
        final String location = connection.getHeaderField("Location");
        if (location == null || location.trim().isEmpty()) {
            throw new IOException("JWK Set endpoint returned a redirect without a Location header");
        }
        try {
            // Resolve against the current URL so that a relative Location is handled the way the
            // automatic redirect follower would have handled it.
            return new URL(current, location);
        } catch (MalformedURLException e) {
            throw new IOException("JWK Set endpoint returned an unusable redirect Location", e);
        }
    }

    private String readBody(HttpURLConnection connection, long deadline) throws IOException {
        final StringBuilder body = new StringBuilder();
        try (InputStream in = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            final char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                checkDeadline(deadline);
                if (body.length() + read > MAX_RESPONSE_CHARS) {
                    throw new IOException("JWK Set document is larger than the supported maximum of "
                            + MAX_RESPONSE_CHARS + " characters");
                }
                body.append(buffer, 0, read);
            }
        }
        return body.toString();
    }
}
