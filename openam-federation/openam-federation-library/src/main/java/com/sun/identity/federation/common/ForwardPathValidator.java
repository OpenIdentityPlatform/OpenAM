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
import java.net.URLDecoder;
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
 * The container processes a dispatcher path in a fixed order before it maps it
 * (Tomcat {@code ApplicationContext.getRequestDispatcher}, with
 * {@code dispatchersUseEncodedPaths} on by default): the query string is cut,
 * {@code ;params} are stripped from the raw string (each {@code ;} up to the
 * next raw {@code /}), the rest is percent-decoded once, and {@code //} and
 * {@code /./} are collapsed. The checks here follow that order: traversal is
 * refused in the raw form and again in the decoded form, and the
 * reserved-directory check reads the stripped, decoded, collapsed path. An
 * escape that decodes to a URL delimiter ({@code %25}, {@code %3F},
 * {@code %23}, {@code %3B}) or to something that is not UTF-8 is refused
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
        // maps the path, so that form has to pass every check as well (a
        // trailing "..#" segment is ".." once the fragment is gone); a
        // ServletContext dispatcher keeps the fragment, which the checks
        // above cover.
        int fragment = uri.indexOf('#');
        if (fragment != -1) {
            String withoutFragment = resolve(uri.substring(0, fragment));
            return withoutFragment != null && !isReserved(withoutFragment);
        }
        return true;
    }

    /**
     * The path to hand to a {@code RequestDispatcher} for {@code path}, or {@code null} when
     * {@code path} must not be dispatched to. The result is the form the container maps -
     * path parameters stripped and percent-escapes decoded once, {@code +} kept literal - with
     * the query string as it was given.
     * <p>
     * Beyond {@link #isSafeForwardPath} the dispatcher form is refused when it carries a
     * {@code WEB-INF} segment anywhere, not only at the root: no in-app path of the product
     * does. The decoded form is derived and re-checked here in the shape static analysis
     * recognises as a sanitised forward target, so the checks read as a repetition of what
     * {@code isSafeForwardPath} established.
     *
     * @param path a context-relative path, optionally with a query string
     * @return the path to dispatch to, or {@code null} if it is not a plain in-app path
     */
    public static String forwardTarget(String path) {
        if (!isSafeForwardPath(path)) {
            return null;
        }
        int query = path.indexOf('?');
        String uri = query == -1 ? path : path.substring(0, query);
        // isSafeForwardPath refused every escape that decodes to a delimiter, %25 included,
        // so one round decodes everything; '+' is protected from URLDecoder's form decoding.
        String decoded = stripPathParams(uri).replace("+", "%2B");
        try {
            while (decoded.contains("%")) {
                decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8);
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        // Only a segment that is exactly ".." is traversal; every other segment becomes "x".
        String dotSegments = decoded.replaceAll("(?<=^|/)(?!\\.\\.(?=/|$))[^/]+", "x");
        if ((decoded + "/").toUpperCase(Locale.ROOT).contains("/WEB-INF/")
                || dotSegments.contains("..")) {
            return null;
        }
        return query == -1 ? decoded : decoded + "?" + path.substring(query + 1);
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
        // The container cuts the dispatcher path at the first '?', so a '?'
        // in the alias would end the path early and turn a trailing ".." that
        // precedes it into a whole segment; an alias never carries one.
        return metaAlias != null && !metaAlias.isEmpty() && metaAlias.indexOf('?') == -1
                && resolve(metaAlias) != null;
    }

    /**
     * The path as the container will map it - path parameters stripped, decoded
     * once, collapsed - or {@code null} when it must not be dispatched to:
     * traversal, a backslash or a control character in either the raw or the
     * decoded form, or an escape that is malformed, not UTF-8, or decodes to a
     * URL delimiter.
     */
    private static String resolve(String value) {
        if (containsTraversal(value)) {
            return null;
        }
        // The container strips ";param" on the raw string, up to the next raw
        // '/', before it decodes: an encoded slash inside a parameter goes with
        // the parameter and never becomes a separator.
        String decoded = decodeOnce(stripPathParams(value));
        if (decoded == null || containsTraversal(decoded)) {
            return null;
        }
        return collapse(decoded);
    }

    /**
     * Drops every {@code ;param} the way the container does on the raw path:
     * from each {@code ;} up to the next {@code /}.
     */
    private static String stripPathParams(String path) {
        if (path.indexOf(';') == -1) {
            return path;
        }
        StringBuilder stripped = new StringBuilder(path.length());
        int pos = 0;
        while (pos < path.length()) {
            int semicolon = path.indexOf(';', pos);
            if (semicolon == -1) {
                stripped.append(path, pos, path.length());
                break;
            }
            stripped.append(path, pos, semicolon);
            int slash = path.indexOf('/', semicolon);
            pos = slash == -1 ? path.length() : slash;
        }
        return stripped.toString();
    }

    /**
     * Collapses a decoded, parameter-free path the way the container normalises
     * it before mapping: empty and {@code .} segments dropped. {@code ..} never
     * reaches here.
     */
    private static String collapse(String path) {
        StringBuilder collapsed = new StringBuilder(path.length());
        for (String segment : path.split("/", -1)) {
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
     *  not valid UTF-8, or decodes to {@code %}, {@code ?}, {@code #} or {@code ;}
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
                if (b == '%' || b == '?' || b == '#' || b == ';') {
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
