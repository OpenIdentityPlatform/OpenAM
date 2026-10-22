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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.common.HttpURLConnectionManager;
import com.sun.identity.common.SsrfUrlValidator;

/**
 * Fetches the document behind an administrator-supplied URL for the federation workflow tasks —
 * SAML2 metadata and extended metadata, OpenID Connect discovery documents — without letting the
 * URL reach the OpenAM server's internal network (GHSA-rmw3-j353-w9f7).
 *
 * <p>Every hop, the original URL and each redirect target alike, must use {@code http} or
 * {@code https} and must not resolve to a loopback, private, link-local or otherwise
 * special-purpose address, as decided by {@link SsrfUrlValidator}. Redirects are followed here
 * rather than by {@code HttpURLConnection}, which would otherwise chase a {@code 302} from a
 * validated public host to an internal one without asking. A redirect may upgrade {@code http} to
 * {@code https} but never leave {@code https}: the document carries the partner's keys.
 *
 * <p>Federation partners on an internal network are common, so the address check — never the
 * scheme check — can be lifted at runtime with {@link #ALLOW_ANY_ADDRESS_PROPERTY}. Without it,
 * the metadata of such a partner can still be imported by pasting the document instead of its
 * URL.
 *
 * <p>The host is resolved here and resolved again when the connection is opened, so a hostile DNS
 * server can answer differently the second time (DNS rebinding), and a JVM-wide HTTP proxy
 * resolves the host itself; both limits are shared with the other SSRF guards in OpenAM.
 */
class WebContentFetcher {

    /**
     * When {@code true}, a URL fetched by the federation workflow tasks may resolve to an internal
     * or special-purpose address — any address {@link SsrfUrlValidator} blocks. The
     * {@code http}/{@code https} scheme allowlist still applies. Default {@code false}.
     */
    static final String ALLOW_ANY_ADDRESS_PROPERTY =
            "org.openidentityplatform.federation.workflow.allow-any-address";

    /**
     * How many {@code 3xx} hops are followed before the fetch is abandoned — enough for an
     * {@code http} to {@code https} upgrade or a moved metadata path, low enough to bound a loop.
     */
    private static final int MAX_REDIRECTS = 3;

    /** {@code HttpURLConnection} has no constant for {@code 307 Temporary Redirect}. */
    private static final int HTTP_TEMPORARY_REDIRECT = 307;

    /** {@code HttpURLConnection} has no constant for {@code 308 Permanent Redirect}. */
    private static final int HTTP_PERMANENT_REDIRECT = 308;

    /**
     * Thrown when a URL — or a URL it redirects to — is not one the server may fetch, so that the
     * caller can tell the administrator why instead of reporting an unreachable host.
     */
    static final class UnsafeUrlException extends IOException {

        private static final long serialVersionUID = 1L;

        UnsafeUrlException(URL url) {
            super("Refusing to fetch " + url + ": it does not use http or https, or it resolves to "
                    + "an internal or special-purpose address. Set " + ALLOW_ANY_ADDRESS_PROPERTY
                    + "=true if this host is intentionally on an internal address.");
        }
    }

    /**
     * Thrown when an {@code https} URL redirects to one that is not {@code https}, which would
     * fetch the partner's metadata — and the keys in it — in cleartext.
     */
    static final class InsecureRedirectException extends IOException {

        private static final long serialVersionUID = 1L;

        InsecureRedirectException(URL from, URL to) {
            super("Refusing to follow a redirect from " + from + " to " + to + ": it leaves https");
        }
    }

    /**
     * Fetches {@code url} and returns its body, one {@code \n}-terminated line per line read.
     *
     * @param url the URL to fetch.
     * @return the body of the response.
     * @throws UnsafeUrlException if the URL, or any URL it redirects to, is not safe to fetch.
     * @throws InsecureRedirectException if an {@code https} URL redirects to a non-{@code https} one.
     * @throws MalformedURLException if the URL or a redirect {@code Location} is not a valid URL.
     * @throws IOException if the redirect budget is exhausted or the fetch itself fails.
     */
    String fetch(String url) throws IOException {
        URL target = new URL(url);
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            if (!isSafe(target)) {
                if (isHttp(target)) {
                    // A host that does not resolve is unreachable, not unsafe: the validator refuses
                    // it all the same, so resolve it here and let UnknownHostException, an
                    // IOException, reach Task as unable.to.reach.url.
                    InetAddress.getAllByName(target.getHost());
                }
                throw new UnsafeUrlException(target);
            }
            final HttpURLConnection connection = open(target);
            try {
                connection.setRequestMethod("GET");
                connection.setInstanceFollowRedirects(false);
                if (isRedirect(connection.getResponseCode())) {
                    final URL next = redirectTarget(connection, target);
                    if (!isAllowedHop(target, next)) {
                        throw new InsecureRedirectException(target, next);
                    }
                    target = next;
                    continue;
                }
                return readBody(connection);
            } finally {
                connection.disconnect();
            }
        }
        throw new IOException("Gave up fetching " + url + " after " + MAX_REDIRECTS + " redirects");
    }

    /**
     * Whether the server may fetch {@code url}.
     *
     * <p>Visible for testing: the address blocklist rejects the loopback address that a test HTTP
     * server necessarily listens on, so a test that needs the first hop to be reachable and every
     * later hop to face the real blocklist has no other seam.
     *
     * @param url the URL about to be fetched — the original one or a redirect target.
     * @return {@code true} if the URL is safe for the server to fetch.
     */
    boolean isSafe(URL url) {
        if (!isHttp(url)) {
            return false;
        }
        return SystemProperties.getAsBoolean(ALLOW_ANY_ADDRESS_PROPERTY, false)
                || SsrfUrlValidator.isSafeRemoteUrl(url.toExternalForm());
    }

    /**
     * Opens the connection for one hop.
     *
     * <p>Visible for testing: the test HTTP server speaks only plain {@code http}, so a test that
     * needs an {@code https} hop to answer with a redirect has no other seam.
     *
     * @param target the URL of the hop, already checked by {@link #isSafe(URL)}.
     * @return the connection, not yet connected.
     * @throws IOException if the connection cannot be opened.
     */
    HttpURLConnection open(URL target) throws IOException {
        return HttpURLConnectionManager.getConnection(target);
    }

    /**
     * Whether a redirect from {@code from} to {@code to} may be followed, as far as the scheme goes:
     * {@code http} to {@code https} is an upgrade and is allowed, but a redirect never leaves
     * {@code https}. The address of {@code to} is checked separately by {@link #isSafe(URL)}.
     *
     * @param from the URL that answered with the redirect.
     * @param to the redirect target.
     * @return {@code false} if the redirect leaves {@code https}.
     */
    static boolean isAllowedHop(URL from, URL to) {
        return !"https".equalsIgnoreCase(from.getProtocol()) || "https".equalsIgnoreCase(to.getProtocol());
    }

    private static boolean isHttp(URL url) {
        final String protocol = url.getProtocol();
        return "http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol);
    }

    /**
     * The statuses the JDK follower this replaces followed, plus {@code 308}. A {@code 300} without
     * a {@code Location} fails in {@link #redirectTarget} instead of handing its list of choices
     * back as the document; {@code 305 Use Proxy} names a proxy, not the document, and is not
     * followed.
     */
    private static boolean isRedirect(int status) {
        return status == HttpURLConnection.HTTP_MULT_CHOICE
                || status == HttpURLConnection.HTTP_MOVED_PERM
                || status == HttpURLConnection.HTTP_MOVED_TEMP
                || status == HttpURLConnection.HTTP_SEE_OTHER
                || status == HTTP_TEMPORARY_REDIRECT
                || status == HTTP_PERMANENT_REDIRECT;
    }

    private static URL redirectTarget(HttpURLConnection connection, URL current) throws IOException {
        final String location = connection.getHeaderField("Location");
        if (location == null || location.trim().isEmpty()) {
            throw new IOException(current + " returned a redirect without a Location header");
        }
        // Resolve against the current URL so that a relative Location is handled the way the
        // automatic redirect follower would have handled it.
        return new URL(current, location);
    }

    private static String readBody(HttpURLConnection connection) throws IOException {
        final StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
        }
        return content.toString();
    }
}
