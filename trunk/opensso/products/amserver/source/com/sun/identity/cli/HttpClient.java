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
 * $Id: HttpClient.java,v 1.3 2008/06/25 05:42:08 qcheng Exp $
 *
 */
package com.sun.identity.cli;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;

/**
 * A client to make connection to the Web CLI servlet to make CLI request.
 *
 * @see WebCLI
 */
public class HttpClient {
    /**
     * properties file where protocol, host and port of amserver is found.
     */
    private static final String PROPERTIES = "CLIClient";
    private static final String PROP_PROTOCOL = "protocol";
    private static final String PROP_HOST = "host";
    private static final String PROP_PORT = "port";
    private static final String PROP_URI = "deployment-uri";
    private static boolean useCache = Boolean.getBoolean(
        SystemProperties.get(Constants.URL_CONNECTION_USE_CACHE, "false"));

    /**
     * Creates a URL connection to the Web CLI servlet, create and
     * submit the request; and display the response accordingly.
     *
     * @param args Commandline options.
     */
    public static void main(String[] args) {
        try {
            URL url = new URL(getServerURL());
            HttpURLConnection connection = (HttpURLConnection)
                url.openConnection();
            connection.setDoOutput(true);
            connection.setUseCaches(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty(
                "Content-type", "application/x-www-form-urlencoded");
            
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            for (int i = 0; i < args.length; i++) {
                if (i == 0) {
                    // out.print("?");
                } else {
                    out.print("&");
                }
                out.print("arg" + i + "=");
                out.print(URLEncoder.encode(args[i], "UTF-8"));
            }
            out.close();
            
            BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
            String inputLine;
            
            while ((inputLine = in.readLine()) != null) {
                System.out.println(inputLine);
            }
            
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CLIException e) {
            System.err.println(e.getMessage());
        }
    }
    
    private static String getServerURL()
        throws CLIException 
    {
        ResourceBundle bdl = null;
        
        try {
            bdl = ResourceBundle.getBundle(PROPERTIES);
        } catch (MissingResourceException e) {
            throw new CLIException("CLIClient.properties file not found.",
                ExitCodes.MISSING_RESOURCE_BUNDLE);
        }
        
        try {
            return bdl.getString(PROP_PROTOCOL) + "://" +
                bdl.getString(PROP_HOST) + ":" + bdl.getString(PROP_PORT) +
                "/" + bdl.getString(PROP_URI) + "/webcli";
        } catch (MissingResourceException e) {
            throw new CLIException(
                "Missing properties in CLIClient.properties.",
                ExitCodes.MISSING_RESOURCE_BUNDLE);
        }
    }
}
