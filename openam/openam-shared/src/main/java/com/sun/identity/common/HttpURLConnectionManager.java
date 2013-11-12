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
 * $Id: HttpURLConnectionManager.java,v 1.3 2008/10/04 05:28:48 beomsuk Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.sun.identity.common;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import com.sun.identity.protocol.AMURLStreamHandlerFactory;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;

/**
 * The <code>HttpURLConnectionManager</code> class is used to get
 * <code>HttpURLConnection</code> instances and set read as well as connect timeout
 *
 * @author Bernhard Thalmayr
 */

public class HttpURLConnectionManager {
  
    private static Debug debug = Debug.getInstance("PLLClient");
    private static int READ_TIMEOUT = 30000;
    private static int CONNECT_TIMEOUT = 10000;
    private static final String URL_READ_TIMEOUT = "com.sun.identity.url.readTimeout";
    private static final String URL_CONNECT_TIMEOUT = "org.forgerock.openam.url.connectTimeout";
    private static String prot_handler_string = null;
    private static AMURLStreamHandlerFactory stFactory = new AMURLStreamHandlerFactory();
    
    static {
        initialize();
    }

    private static void initialize() {
        prot_handler_string = SystemPropertiesManager.get(Constants.PROTOCOL_HANDLER, null);
        if (debug.messageEnabled()) {
            debug.message("Configured Protocol Handler : " +
                    prot_handler_string);
        }

        String rto = SystemPropertiesManager.get(URL_READ_TIMEOUT);
        if (rto != null && rto.length() > 0) {
            try {
                READ_TIMEOUT = Integer.valueOf(rto);
                if (debug.messageEnabled()) {
                    debug.message("HttpURLConnectionManager.initialize(): " +
                        "Set READTIMEOUT to " + READ_TIMEOUT);
                }
            } catch (Exception e) {
                debug.error("HttpURLConnectionManager.initialize(): Fail to read " +
                        URL_READ_TIMEOUT + " using default READTIMEOUT " +
                        READ_TIMEOUT, e);
            }
        }
        String cto = SystemPropertiesManager.get(URL_CONNECT_TIMEOUT);
        if ((cto != null) && (cto.length() > 0)) {
            try {
                CONNECT_TIMEOUT = Integer.valueOf(cto);
                if (debug.messageEnabled()) {
                    debug.message("HttpURLConnectionManager.initialize(): " +
                        "Set CONNECT_TIMEOUT to " + CONNECT_TIMEOUT);
                }
            } catch (Exception ex) {
                debug.error("HttpURLConnectionManager.initialize(): Fail to read " +
                        URL_CONNECT_TIMEOUT + " using default CONNECT_TIMEOUT " +
                        CONNECT_TIMEOUT, ex);
            }
        }
    }
    
    /**
     * Get the <code>HttpURLConnection</code> and set read and connect timeout
     * @param url The <code>URL</code> to open connection with
     * @exception IOException when calling <code>URL.openConnection</code> fails
     * @return A <code>HttpURLConnection</code>.
     */
    public static HttpURLConnection getConnection(URL url) throws IOException {
        String prot = url.getProtocol();
        
        if ((prot_handler_string != null) && prot.equalsIgnoreCase("https")) {
            url = new URL(url, url.toExternalForm(), 
                    stFactory.createURLStreamHandler("https"));
        }
        
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        return conn;
    }
}
