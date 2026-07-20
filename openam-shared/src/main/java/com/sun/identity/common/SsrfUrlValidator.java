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
package com.sun.identity.common;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * Validates that a (client-supplied) URL is safe for the server to fetch, to prevent
 * server-side request forgery (SSRF).
 *
 * <p>A URL is considered safe only when it uses the {@code http} or {@code https} scheme (which
 * rules out {@code file://}, {@code ftp://}, {@code gopher://} and similar variants) and none of
 * the host's resolved addresses point at a loopback, wildcard, link-local (incl. cloud metadata
 * {@code 169.254.0.0/16}), private/site-local, multicast or IPv6 unique-local address.
 *
 * <p>This class is intentionally free of any configuration/framework dependency so it can be
 * reused from any module; callers that need a runtime "allow any URL" escape hatch should read
 * their own system property and short-circuit before calling {@link #isSafeRemoteUrl(String)}.
 */
public final class SsrfUrlValidator {

    private SsrfUrlValidator() {
    }

    /**
     * @param url the URL to check.
     * @return {@code true} if the URL is safe for the server to fetch, {@code false} otherwise.
     */
    public static boolean isSafeRemoteUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        final URL parsed;
        try {
            parsed = new URL(url);
        } catch (MalformedURLException e) {
            return false;
        }
        final String protocol = parsed.getProtocol();
        if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
            return false;
        }
        final String host = parsed.getHost();
        if (host == null || host.isEmpty()) {
            return false;
        }
        try {
            final InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                return false;
            }
            for (InetAddress address : addresses) {
                if (isBlockedAddress(address)) {
                    return false;
                }
            }
        } catch (UnknownHostException e) {
            return false;
        }
        return true;
    }

    private static boolean isBlockedAddress(InetAddress address) {
        if (address.isLoopbackAddress()          // 127.0.0.0/8, ::1
                || address.isAnyLocalAddress()   // 0.0.0.0, ::
                || address.isLinkLocalAddress()  // 169.254.0.0/16, fe80::/10
                || address.isSiteLocalAddress()  // 10/8, 172.16/12, 192.168/16, fec0::/10
                || address.isMulticastAddress()) {
            return true;
        }
        // IPv6 Unique Local Addresses (fc00::/7) are not reported by isSiteLocalAddress().
        final byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
    }
}
