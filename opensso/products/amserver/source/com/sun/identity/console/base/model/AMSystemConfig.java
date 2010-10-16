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
 * $Id: AMSystemConfig.java,v 1.7 2009/01/28 05:34:56 ww203982 Exp $
 *
 */

package com.sun.identity.console.base.model;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.common.HttpURLConnectionManager;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.SMSEntry;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.ldap.util.RDN;

/* - NEED NOT LOG - */

/**
 * <code>AMSystemConfig</code> is contains system configuration information
 */
public class AMSystemConfig
    implements AMAdminConstants 
{
    /** 
     * Server deployment URI 
     */
    public static String serverDeploymentURI =
        SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);

    /**   
     * Console deployment URI 
     */
    public static String consoleDeploymentURI =
        SystemProperties.get(Constants.AM_CONSOLE_DEPLOYMENT_DESCRIPTOR);

    /** 
     * Server protocol
     */
    public static String serverProtocol = SystemProperties.get(
        Constants.AM_SERVER_PROTOCOL);

    /** 
     * Server host name
     */
    public static String serverHost = SystemProperties.get(
        Constants.AM_SERVER_HOST);

    /** 
     * Server port name 
     */
    public static String serverPort = SystemProperties.get(
        Constants.AM_SERVER_PORT);

    /** 
     * Server URL
     */
    public static String serverURL = serverProtocol + "://" + serverHost + ":"
        + serverPort;

    /** 
     * Determines if console is remote 
     */
    public static boolean isConsoleRemote = Boolean.valueOf(
        SystemProperties.get(Constants.AM_CONSOLE_REMOTE)).booleanValue();

    /** 
     * default organization 
     */
    public static String defaultOrg = SMSEntry.getRootSuffix();

    /** 
     * version 
     */
    public static String version = "";
    
    static {
        if (!isConsoleRemote) {
            version = SystemProperties.get(Constants.AM_VERSION);
        } else {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(serverProtocol + "://" + serverHost + ":" +
                    serverPort + serverDeploymentURI +
                    "/SMSServlet?method=version");
                connection = HttpURLConnectionManager.getConnection(url);
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    version = getContent(connection);
                }
            } catch (MalformedURLException e) {
                AMModelBase.debug.error(
                    "AMSystemConfig.<init>, get version from server", e);
            } catch (IOException e) {
                AMModelBase.debug.error(
                    "AMSystemConfig.<init>, get version from server", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }

    private static String getContent(HttpURLConnection connection) {
        InputStream in_buf = null;
        String result = null;
        try {
            in_buf = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                in_buf, "UTF-8"));
            result = reader.readLine();
        } catch (IOException e) {
            AMModelBase.debug.error("AMSystemConfig.getContent", e);
        } finally {
            if (in_buf != null) {
                try {
                    in_buf.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
        return (result == null) ? "" : result;
    }

    public static String normalizeString(String tmp) {
        if (tmp != null) {
            // parse string to remove any extraneous space
            String[] dns = LDAPDN.explodeDN(tmp, false);
            if ((dns != null) && (dns.length > 0)) {
                int len = dns.length -1;
                DN root = new DN(dns[len]);

                for (int i = len -1; i >= 0; --i) {
                    root.addRDN(new RDN(dns[i]));
                }

                tmp = root.toString();
            }
        } else {
            tmp = "";
        }
        return tmp;
    }

    /** this value is set to true if directory is iPlanet DIT compliance */
    public static boolean iPlanetCompliantDIT;

    static {
        String s = SystemProperties.get("com.iplanet.am.compliance");
        if ((s != null) && s.equalsIgnoreCase("true")) {
            iPlanetCompliantDIT = true;
        }
    }
}

