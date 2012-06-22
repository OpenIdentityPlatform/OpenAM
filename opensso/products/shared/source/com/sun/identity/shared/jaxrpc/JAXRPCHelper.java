/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: JAXRPCHelper.java,v 1.6 2009/11/16 21:57:47 goodearth Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.shared.jaxrpc;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.StringTokenizer;

/**
 * The class <code>JAXRPCHelper</code> provides functions to get JAXRPC stubs to
 * a valid Identity Server. 
 */
public class JAXRPCHelper {

    // Static constants
    private final static String JAXRPC_URL = "com.sun.identity.jaxrpc.url";
    private final static String JAXRPC_SERVICE = "jaxrpc";
    public final static String SMS_SERVICE = "SMSObjectIF";
    
    // Static variables
    protected static String validRemoteURL;
    protected static boolean serverFailed = true;
    protected static Debug debug = SOAPClient.debug;
    
    /**
     * Returns a valid URL endpoint for the given service name.
     * 
     * @param serviceName Service Name.
     * @return a valid URL endpoint for the given service name.
     * @throws RemoteException if there are no valid servers.
     */
    public static String getValidURL(String serviceName) 
        throws RemoteException {
        // Validate service name
        if (serviceName == null) {
            throw (new RemoteException("invalid-service-name"));
        }

        // Check if there is a valid server
        if (serverFailed) {
            validRemoteURL = getValidServerURL();
        }
        if (validRemoteURL == null) {
            if (debug.warningEnabled()) {
                debug.warning("JAXRPCHelper: No vaild server found");
            }
            throw (new RemoteException("no-server-found"));
        }
        return (validRemoteURL + serviceName);
    }
                                                                                
    /**
     * Sets the service to be failed.
     */
    public static void serverFailed(String serviceName) {
        if (validRemoteURL == null) {
            if (debug.warningEnabled()) {
                debug.warning("JAXRPCHelper: No valid server found");
            }
            serverFailed = true;
        } else if (serviceName.startsWith(validRemoteURL)) {
            serverFailed = true;
        }
    }

    protected static String getValidServerURL() {
        // Check if the properties has been set
        String servers = SystemPropertiesManager.get(JAXRPC_URL);
        if (servers != null) {
            StringTokenizer st = new StringTokenizer(servers, ",");
            while (st.hasMoreTokens()) {
                String surl = st.nextToken();
                if (!surl.endsWith("/")) {
                    surl += "/";
                }
                if (isServerValid(surl)) {
                    return (surl);
                }
            }
        } else {
            Collection<URL> serverList = null;
            try {
                serverList = SystemPropertiesManager.getSystemProperties().
                    getServiceAllURLs(JAXRPC_SERVICE);
            } catch (Exception e) {
                // Unable to get platform server list
                if (debug.warningEnabled()) {
                    debug.warning("JAXRPCHelper:getValidServerURL: "
                            + "Unable to get platform server", e);
                }
                return (null);
            }

            if (serverList != null) {
                for (URL weburl : serverList) {
                    try {
                        String surl = weburl.toString();
                        if (!surl.endsWith("/")) {
                            surl += "/";
                        }
                        if (isServerValid(surl)) {
                            return (surl);
                        }
                    } catch (Exception e) {
                        debug.warning("JAXRPCHelper:getValidServerURL", e);
                    }
                }
            }
        }
        return (null);
    }

    protected static boolean isServerValid(String url) {
        try {
            if (!url.endsWith(SMS_SERVICE)) {
                url += SMS_SERVICE;
            }
            SOAPClient client = new SOAPClient();
            client.setURL(url);
            client.send(client.encodeMessage("checkForLocal", null),null,null);
        } catch (Exception e) {
            // Server is not valid
            if (debug.messageEnabled()) {
                debug.message("JAXRPCHelper: Connection to URL: " + url
                        + " failed", e);
            }
            return (false);
        }
        return (true);
    }
}
