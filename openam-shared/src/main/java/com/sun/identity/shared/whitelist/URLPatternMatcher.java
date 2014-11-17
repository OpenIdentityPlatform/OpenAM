/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: URLPatternMatcher.java,v 1.1 2009/11/24 21:42:35 madan_ranganath Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS.
 */
package com.sun.identity.shared.whitelist;

import com.sun.identity.shared.debug.Debug;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;

/**
 * The class provides pattern matching for notenforced URIs/URLs.
 */
public class URLPatternMatcher {

    private static final Debug DEBUG = Debug.getInstance("patternMatching");
    private HttpURLResourceName resourceName = null;

    /**
     * Constructor
     */
    public URLPatternMatcher() {
        resourceName = new HttpURLResourceName();
        resourceName.initialize(new HashMap());
    }

    /**
     * Matches the URL against the provided URL patterns.
     *
     * @param requestedURL The URL to be matched.
     * @param patterns The patterns to match the URL against.
     * @param wildcard Flag for wildcard comparison.
     * @return <code>true</code> if matched, <code>false</code> otherwise.
     * @throws MalformedURLException If the URL or one of the patterns is invalid.
     */
    public boolean match(String requestedURL, Collection<String> patterns, boolean wildcard)
            throws MalformedURLException {
        boolean result = false;
        String patternLower;

        for (String pattern : patterns) {
            patternLower = pattern.toLowerCase();

            requestedURL = resourceName.canonicalize(requestedURL);

            // convert URI to URL if any.
            if (pattern.startsWith("/")) {
                pattern = convertToURL(pattern, requestedURL);
            } else if (!patternLower.startsWith("http")) {
                pattern = convertToURL("/" + pattern, requestedURL);
            }

            pattern = resourceName.canonicalize(pattern);
            if (DEBUG.messageEnabled()) {
                DEBUG.message("URLPatternMatcher.match(" + requestedURL + "): matching by pattern: " + pattern);
            }

            ResourceMatch res = resourceName.compare(requestedURL, pattern, wildcard);

            if (res == ResourceMatch.WILDCARD_MATCH || res == ResourceMatch.EXACT_MATCH) {
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("URLPatternMatcher.match(" + requestedURL + "): matched by pattern: " + pattern
                            + " result = " + res);
                }
                result = true;
                break;
            }
        }
        return result;
    }

    /*
     * convert pattern's URI to URL format.
     */
    private String convertToURL(String pattern, String requestedURL) {
        StringBuilder builder = new StringBuilder();

        try {
            URL url = new URL(requestedURL);
            builder.append(url.getProtocol()).append("://");
            builder.append(url.getHost()).append(":");
            builder.append(url.getPort());
            builder.append(pattern);
            pattern = builder.toString();
        } catch (MalformedURLException ex) {
            if (DEBUG.warningEnabled()) {
                DEBUG.warning("URLPatternMatcher.convertToURL()- pattern:" + pattern + " requestedURL:" + requestedURL
                        + " Exception:", ex);
            }
        }
        return pattern;
    }
}
