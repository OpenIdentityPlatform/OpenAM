/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMConsoleConfig.java,v 1.2 2008/06/25 05:42:47 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.base;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import com.sun.identity.common.RequestUtils;
import com.sun.identity.console.base.model.AMAdminConstants;
import javax.servlet.http.HttpServletRequest;

public final class AMConsoleConfig {
    static private AMConsoleConfig instance = new AMConsoleConfig();

    public static String TAB_VIEW_BEAN_PREFIX = "tabViewBean";

    public static String CONSOLE_DEPLOYMENT_URI = SystemProperties.get(
        Constants.AM_CONSOLE_DEPLOYMENT_DESCRIPTOR);
    public static String SERVER_DEPLOYMENT_URI = SystemProperties.get(
        Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
    public static String SERVER_PROTOCOL = SystemProperties.get(
        Constants.AM_SERVER_PROTOCOL);
    public static String SERVER_HOST = SystemProperties.get(
        Constants.AM_SERVER_HOST);
    public static String SERVER_PORT = SystemProperties.get(
        Constants.AM_SERVER_PORT);
    public static String SERVER_URL = SERVER_PROTOCOL + "://" + SERVER_HOST +
        ":" + SERVER_PORT;

    public static String SERVER_NAME = null;
    public static boolean IS_CONSOLE_REMOTE = Boolean.valueOf(
        SystemProperties.get(Constants.AM_CONSOLE_REMOTE)).booleanValue();

    static {
        SERVER_NAME = SystemProperties.get(Constants.AM_CONSOLE_HOST);
        int idx = SERVER_NAME.indexOf('.');
        if (idx != -1) {
            SERVER_NAME = SERVER_NAME.substring(0, idx);
        }
    }

    private AMConsoleConfig() {
    }

    public static AMConsoleConfig getInstance() {
        return instance;
    }

    public String getLogoutURL(HttpServletRequest req) {
        String url = SERVER_URL + SERVER_DEPLOYMENT_URI +
            AMAdminConstants.URL_LOGOUT;

        if (!IS_CONSOLE_REMOTE) {
            String host = req.getHeader("Host");
            if (host != null) {
                StringBuilder sb = new StringBuilder(200);
                                                                                
                String protocol = RequestUtils.getRedirectProtocol(
                    req.getScheme(), host);
                sb.append(protocol);
                sb.append("://");
                sb.append(host);
                sb.append(SERVER_DEPLOYMENT_URI);
                sb.append(AMAdminConstants.URL_LOGOUT);
                url = sb.toString();
            }
        }

        return url;
    }
}
