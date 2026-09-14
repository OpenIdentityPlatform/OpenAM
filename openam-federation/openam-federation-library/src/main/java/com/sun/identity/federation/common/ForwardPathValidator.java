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

import java.util.Locale;

/**
 * Sanity checks for request-derived values before they are handed to a
 * {@code RequestDispatcher}. A forward is served from inside the web application:
 * it can reach {@code /WEB-INF} and {@code /META-INF}, and it does not run the
 * filters declared in {@code web.xml}, so a path an end user controls must never
 * be allowed to climb out of the location the code intends to dispatch to.
 */
public final class ForwardPathValidator {

    private ForwardPathValidator() {
    }

    /**
     * Whether {@code path} may be forwarded to as-is: absolute, without {@code ..}
     * segments (path parameters stripped, as the container does), without
     * backslashes or control characters, and not under a reserved directory.
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
        if (containsTraversal(uri)) {
            return false;
        }
        String lower = uri.toLowerCase(Locale.ROOT);
        return !(lower.startsWith("/web-inf/") || lower.equals("/web-inf")
                || lower.startsWith("/meta-inf/") || lower.equals("/meta-inf"));
    }

    /**
     * Whether {@code metaAlias} can be appended to a fixed handler path without
     * changing which resource is dispatched to.
     *
     * @param metaAlias the provider meta alias taken from the request
     * @return {@code true} if the alias contains no traversal
     */
    public static boolean isSafeMetaAlias(String metaAlias) {
        return metaAlias != null && !metaAlias.isEmpty() && !containsTraversal(metaAlias);
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
}
