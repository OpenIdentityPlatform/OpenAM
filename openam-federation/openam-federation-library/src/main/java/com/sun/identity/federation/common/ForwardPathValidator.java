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
package com.sun.identity.federation.common;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Sanity checks for request-derived values before they are handed to a
 * {@code RequestDispatcher}. A forward is served from inside the web application:
 * it can reach {@code /WEB-INF} and {@code /META-INF}, and it does not run the
 * filters declared in {@code web.xml}, so a path an end user controls must never
 * be allowed to climb out of the location the code intends to dispatch to.
 * <p>
 * The container percent-decodes a dispatcher path once more and then collapses
 * it ({@code //}, {@code /./}, {@code ;params}) before it maps it (Tomcat:
 * {@code dispatchersUseEncodedPaths}, on by default), so every check runs on the
 * decoded form as well as on the raw one, and the reserved-directory check reads
 * the collapsed path. An escape that decodes to a URL delimiter ({@code %25},
 * {@code %3F}, {@code %23}) or to something that is not UTF-8 is refused
 * outright: no in-app path of the product carries one.
 */
public final class ForwardPathValidator {

    private ForwardPathValidator() {
    }

    /**
     * Whether {@code path} may be forwarded to as-is: absolute, without {@code ..}
     * segments in its raw or decoded form (path parameters stripped, as the
     * container does), without backslashes, control characters, malformed or
     * delimiter escapes, and not under a reserved directory once collapsed.
     *
     * @param path a context-relative path, optionally with a query string
     * @return {@code true} if the path is safe to pass to a request dispatcher
     */
    public static boolean isSafeForwardPath(String path) {
        if (path == null || !path.startsWith("/")) {
            return false;
        }
        int query = path.indexOf('?');
        String uri = query == -1 ? path : path.substring(0, query);
        String resolved = resolve(uri);
        if (resolved == null || isReserved(resolved)) {
            return false;
        }
        // HttpServletRequest.getRequestDispatcher() drops a fragment before it
        // maps the path, so the reserved-directory check has to see that form
        // too; a ServletContext dispatcher keeps it, which the traversal checks
        // above already cover.
        int fragment = resolved.indexOf('#');
        return fragment == -1 || !isReserved(collapse(resolved.substring(0, fragment)));
    }

    private static boolean isReserved(String collapsedPath) {
        String lower = collapsedPath.toLowerCase(Locale.ROOT);
        return lower.startsWith("/web-inf/") || lower.equals("/web-inf")
                || lower.startsWith("/meta-inf/") || lower.equals("/meta-inf");
    }

    /**
     * Whether {@code metaAlias} can be appended to a fixed handler path without
     * changing which resource is dispatched to.
     *
     * @param metaAlias the provider meta alias taken from the request
     * @return {@code true} if the alias contains no traversal
     */
    public static boolean isSafeMetaAlias(String metaAlias) {
        return metaAlias != null && !metaAlias.isEmpty() && resolve(metaAlias) != null;
    }

    /**
     * The path as the container will map it - decoded once and collapsed - or
     * {@code null} when it must not be dispatched to: traversal, a backslash or a
     * control character in either the raw or the decoded form, or an escape that
     * is malformed, not UTF-8, or decodes to a URL delimiter.
     */
    private static String resolve(String value) {
        if (containsTraversal(value)) {
            return null;
        }
        String decoded = decodeOnce(value);
        if (decoded == null || containsTraversal(decoded)) {
            return null;
        }
        return collapse(decoded);
    }

    /**
     * Collapses a path the way the container does before mapping it: {@code ;params}
     * stripped from each segment, empty and {@code .} segments dropped. {@code ..}
     * never reaches here.
     */
    private static String collapse(String path) {
        StringBuilder collapsed = new StringBuilder(path.length());
        for (String segment : path.split("/", -1)) {
            int semicolon = segment.indexOf(';');
            if (semicolon != -1) {
                segment = segment.substring(0, semicolon);
            }
            if (!segment.isEmpty() && !segment.equals(".")) {
                collapsed.append('/').append(segment);
            }
        }
        return collapsed.length() == 0 ? "/" : collapsed.toString();
    }

    private static boolean containsTraversal(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' || c < ' ' || c == 0x7f) {
                return true;
            }
        }
        for (String segment : value.split("/", -1)) {
            // The container strips ";param" from each segment before normalising.
            int semicolon = segment.indexOf(';');
            if (semicolon != -1) {
                segment = segment.substring(0, semicolon);
            }
            if (segment.equals("..")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Percent-decodes {@code value} exactly once, the way the container does for
     * a path: {@code %XX} with two ASCII hex digits only, {@code +} left alone.
     *
     * @return the decoded value, or {@code null} if an escape is malformed, is
     *  not valid UTF-8, or decodes to {@code %}, {@code ?} or {@code #}
     */
    private static String decodeOnce(String value) {
        if (value.indexOf('%') == -1) {
            return value;
        }
        StringBuilder decoded = new StringBuilder(value.length());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int i = 0;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (c != '%') {
                decoded.append(c);
                i++;
                continue;
            }
            // A run of escapes is one byte sequence: a multi-byte character has
            // to be decoded as a whole.
            bytes.reset();
            while (i < value.length() && value.charAt(i) == '%') {
                if (i + 2 >= value.length()) {
                    return null;
                }
                int hi = hexDigit(value.charAt(i + 1));
                int lo = hexDigit(value.charAt(i + 2));
                if (hi < 0 || lo < 0) {
                    return null;
                }
                int b = (hi << 4) | lo;
                if (b == '%' || b == '?' || b == '#') {
                    // A second encoding layer, or a delimiter the container would
                    // map literally: never a plain in-app path.
                    return null;
                }
                bytes.write(b);
                i += 3;
            }
            try {
                decoded.append(StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes.toByteArray())));
            } catch (CharacterCodingException e) {
                // Overlong or truncated sequences: the container would turn them
                // into replacement characters, never into a path this code means.
                return null;
            }
        }
        return decoded.toString();
    }

    private static int hexDigit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }
}
