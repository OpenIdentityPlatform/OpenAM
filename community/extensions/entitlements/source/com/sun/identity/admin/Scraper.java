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
 * $Id: Scraper.java,v 1.6 2009/06/04 11:49:11 veiming Exp $
 */

package com.sun.identity.admin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scraper {

    private static Map<URL, String> cache = new ExpiringHashMap<URL, String>(1000 * 60 * 15);
    private URL url;
    private int readTimeout = 10000;

    public Scraper(String u) throws MalformedURLException {
        this.url = new URL(u);
    }

    public Scraper(String u, int readTimeout) throws MalformedURLException {
        this(u);
        this.readTimeout = readTimeout;
    }

    public String scrape() throws IOException {
        String content = cache.get(url);
        if (content == null) {
            URLConnection uc = url.openConnection();
            uc.setReadTimeout(readTimeout);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                    uc.getInputStream()));

            String inputLine;
            StringBuffer b = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                b.append(inputLine);
            }

            in.close();

            String base = getBase();

            if (base != null) {
                content = setBase(b.toString(), base);
            } else {
                content = b.toString();
            }
            // disable cache
            //cache.put(url, content);
        }
        return content;
    }

    private String getBase() {
        StringBuffer b = new StringBuffer();
        b.append(url.getProtocol());
        b.append("://");
        b.append(url.getHost());
        if (url.getPort() != -1) {
            b.append(":");
            b.append(url.getPort());
        }
        b.append(url.getPath());

        return b.toString();
    }

    private static String setBase(String content, String base) {
        // remove base tag if it exists
        Pattern basePattern = Pattern.compile("<base.*?>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher baseMatcher = basePattern.matcher(content);
        if (baseMatcher.find()) {
            // base is already set
            return content;
        }

        // add new base tag
        Pattern headPattern = Pattern.compile("<head>(.*?)</head>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher headMatcher = headPattern.matcher(content);

        if (headMatcher.find()) {
            StringBuffer newHead = new StringBuffer();
            newHead.append("<head>\n");
            newHead.append("<base href=\"");
            newHead.append(base);
            newHead.append("\" target=\"_blank\"/>\n");
            newHead.append(headMatcher.group(1));
            newHead.append("\n");
            newHead.append("</head>\n");

            content = headMatcher.replaceFirst(newHead.toString());
        }

        return content;
    }
}
