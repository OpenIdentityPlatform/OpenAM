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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.dashboard;

import com.iplanet.am.util.SystemProperties;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.servlet.HttpContext;

import java.util.List;

/**
 * This class contains method that help with getting information out of ServerContexts objects.
 */
public final class ServerContextHelper {

    /**
     * Gets the iPlanetDirectoryPro cookie from the ServerContext.
     *
     * @param context The ServerContext instance.
     * @return The cookie value or null.
     */
    public static String getCookieFromServerContext(ServerContext context) {

        List<String> cookies = null;
        String cookieName = null;
        HttpContext header = null;
        try {
            cookieName = SystemProperties.get("com.iplanet.am.cookie.name");
            if (cookieName == null || cookieName.isEmpty()) {
                return null;
            }
            header = context.asContext(HttpContext.class);
            if (header == null) {
                return null;
            }
            //get the cookie from header directly   as the name of com.iplanet.am.cookie.am
            cookies = header.getHeaders().get(cookieName.toLowerCase());
            if (cookies != null && !cookies.isEmpty()) {
                for (String s : cookies) {
                    if (s == null || s.isEmpty()) {
                        return null;
                    } else {
                        return s;
                    }
                }
            } else {  //get cookie from header parameter called cookie
                cookies = header.getHeaders().get("cookie");
                if (cookies != null && !cookies.isEmpty()) {
                    for (String cookie : cookies) {
                        String cookieNames[] = cookie.split(";"); //Split parameter up
                        for (String c : cookieNames) {
                            if (c.contains(cookieName)) { //if com.iplanet.am.cookie.name exists in cookie param
                                String amCookie = c.replace(cookieName + "=", "").trim();
                                return amCookie; //return com.iplanet.am.cookie.name value
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return null;
    }
}
