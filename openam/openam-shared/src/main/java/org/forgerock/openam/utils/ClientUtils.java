/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 */

/*
 * Portions Copyrighted 2013 ForgeRock, Inc
 */

package org.forgerock.openam.utils;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;

import javax.servlet.http.HttpServletRequest;

/**
 * Shared utility class for HTTP Clients.
 */
public final class ClientUtils {

    private ClientUtils() {
        // Static utility methods so private constructor.
    }

    private static Debug utilDebug = Debug.getInstance("amComm");

    /**
     * Returns client IP address. The method checks the special HTTP header
     * first (handles Load Balancer case), then checks the remote address contained in the request.
     * Refactored from <code>AuthClientUtils.getClientIPAddress()</code> to enable classes outside of the core
     * (Federation for example) to access this utility method.
     *
     * @param request HttpServletRequest request to be used to obtain client's IP address
     * @return String representing the client's IP address
     */
    public static String getClientIPAddress(HttpServletRequest request) {

        String result = null;
        if (request != null) {
            String ipAddrHeader = SystemPropertiesManager.get(Constants.CLIENT_IP_ADDR_HEADER);

            if ((ipAddrHeader != null) && (ipAddrHeader.length() != 0)) {
                result = request.getHeader(ipAddrHeader);
                if (result != null) {
                    String[] ips = result.split(",");
                    result = ips[0].trim();
                }
            }
            if ((result == null) || (result.length() == 0)) {
                result = request.getRemoteAddr();
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("ClientUtils.getClientIPAddress : remoteAddr=[" + result + "]");
                }
            } else {
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("ClientUtils.getClientIPAddress : header=["
                            + ipAddrHeader + "], result=[" + result + "]");
                }
            }
        }

        return result;
    }
}
