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
 * $Id: RelaxedURL.java,v 1.2 2009/10/20 18:46:16 veiming Exp $
 *
 * Portions copyright 2013 ForgeRock, Inc.
 */
package com.sun.identity.entitlement.util;

import java.net.MalformedURLException;

public class RelaxedURL {

    private static final String PROTOCOL_HTTPS = "https";
    private static final String PROTOCOL_HTTP = "http";

    private String protocol;
    private String hostname;
    private String port;
    private String path;
    private String query;

    public RelaxedURL(String url)
        throws MalformedURLException {
        int idx = getProtocol(url);
        parseURL(url, idx);
    }

    private int getProtocol(String url) throws MalformedURLException {
        int idx = url.indexOf("://");
        if (idx == -1) {
            throw new MalformedURLException(url);
        }

        protocol = url.substring(0, idx);
        return idx+3;
    }

    private void parseURL(String url, int begins) {
        if (protocol.equals(PROTOCOL_HTTP) || protocol.equals(PROTOCOL_HTTPS)) {
            int colon = url.indexOf(":", begins);
            if (colon == -1) {
                if (protocol.equals("http")) {
                    port = "80";
                } else if (protocol.equals("https")) {
                    port = "443";
                }

                int slash = url.indexOf('/', begins);
                if (slash == -1) {
                    hostname = url.substring(begins);
                    path = "/";
                } else {
                    hostname = url.substring(begins, slash);
                    path = url.substring(slash);
                }
            } else {
                hostname = url.substring(begins, colon);

                int slash = url.indexOf('/', colon);
                if (slash == -1) {
                    port = url.substring(colon + 1);
                    path = "/";
                } else {
                    port = url.substring(colon + 1, slash);
                    path = url.substring(slash);
                }
            }
        } else {
            int slash = url.indexOf('/', begins);
            if (slash == -1) {
                hostname = url.substring(begins);
                path = "/";
            } else {
                hostname = url.substring(begins, slash);
                path = url.substring(slash);
            }
        }
        
        int idx = path.indexOf('?');
        if (idx == -1) {
            query = "";
        } else {
            query = path.substring(idx +1);
            path = path.substring(0, idx);
        }
    }

    public String getHostname() {
        return hostname;
    }

    public String getPath() {
        return path;
    }

    public String getPort() {
        return port;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getQuery() {
        return query;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(protocol);
        builder.append("://");
        builder.append(hostname);
        builder.append(':');
        builder.append(port);
        builder.append(path);

        if (!query.isEmpty()) {
            builder.append('?');
            builder.append(query);
        }

        return builder.toString();
    }

}

