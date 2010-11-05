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
 * $Id: Functions.java,v 1.15 2009/07/24 23:05:57 farble1670 Exp $
 */
package com.sun.identity.admin;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class Functions {

    public static String indexOf(List l, Object o) {
        return Integer.toString(l.indexOf(o));
    }

    public static boolean contains(Collection c, Object o) {
        if (c == null) {
            return false;
        }
        return c.contains(o);
    }

    public static String truncate(String s, int length) {
        if (length >= s.length()) {
            return s;
        }
        return s.substring(0, length - 1) + "...";
    }

    public static int size(Collection c) {
        if (c == null) {
            return 0;
        }
        return c.size();
    }

    public static String scrape(String url) {
        try {
            Scraper s = new Scraper(url);
            String result = s.scrape();
            return result;
        } catch (IOException ioe) {
            Resources r = new Resources();
            return r.getString(Functions.class, "scrapeError", url, ioe);
        }
    }

    public static String concat(String s1, String s2) {
        return s1 + s2;
    }
}
