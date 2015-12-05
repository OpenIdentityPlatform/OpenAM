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
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted 2013-2015 ForgeRock AS.
 */

package org.forgerock.openam.utils;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.http.protocol.Request;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.services.context.ClientContext;
import org.forgerock.services.context.Context;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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

    /**
     * Gets the client IP address from the provided {@code Context} and {@code Request}.
     *
     * @param context the Commons Http-Framework context which will be examined to obtain the client's ip address.
     * @param request the Commons Http-Framework request which will be examined to obtain the client's ip address
     * @return the client ip address, specified in either a custom header value, or pulled from the request, via
     * the ClientContext class.
     */
    public static String getClientIPAddress(Context context, Request request) {
        String result = null;
        if (request != null) {
            String ipAddrHeader = SystemPropertiesManager.get(Constants.CLIENT_IP_ADDR_HEADER);

            if (!StringUtils.isBlank(ipAddrHeader)) {
                result = request.getHeaders().getFirst(ipAddrHeader);
                if (result != null) {
                    String[] ips = result.split(",");
                    result = ips[0].trim();
                }
            }
            if (StringUtils.isBlank(result)) {
                result = context.asContext(ClientContext.class).getRemoteAddress();
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

    /**
     * Gets the client IP address from the provided {@code Context}.
     *
     * @param context the Commons Http-Framework context which will be examined to obtain the client's ip address.
     * @return the client ip address, specified in either a custom header value, or pulled from the request, via the
     * ClientContext class. Differs from the method above as this method relies upon the presence of crest context
     * objects, which are not present in the CHF context.
     */
    public static String getClientIPAddress(Context context) {
        String result = null;
        String ipAddrHeader = SystemPropertiesManager.get(Constants.CLIENT_IP_ADDR_HEADER);

        if (!StringUtils.isBlank(ipAddrHeader)) {
            List<String> clientIPHeaderContent = context.asContext(HttpContext.class).getHeaders().get(ipAddrHeader);
            if (!CollectionUtils.isEmpty(clientIPHeaderContent)) {
                result = clientIPHeaderContent.get(0);
                if (result != null) {
                    String[] ips = result.split(",");
                    result = ips[0].trim();
                }
            }
        }
        if (StringUtils.isBlank(result)) {
            result = context.asContext(ClientContext.class).getRemoteAddress();
            if (utilDebug.messageEnabled()) {
                utilDebug.message("ClientUtils.getClientIPAddress : remoteAddr=[" + result + "]");
            }
        } else {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("ClientUtils.getClientIPAddress : header=["
                        + ipAddrHeader + "], result=[" + result + "]");
            }
        }

        return result;
    }
}
