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

package com.sun.identity.common;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import com.sun.identity.protocol.AMURLStreamHandlerFactory;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;

/**
 * The <code>HttpURLConnectionManager</code> class is used to get
 * <code>HttpURLConnection</code> instances and set connection timeout
 * if it supported by the JDK
 */

public class HttpURLConnectionManager {
  
    private static Debug debug = Debug.getInstance("PLLClient");
    private static int READTIMEOUT = 30000;
    private static final String URL_READTIMEOUT = 
           "com.sun.identity.url.readTimeout";
    private static Method method, method_s;
    private static Object[] args;
    private static String prot_handler_string = null;
    private static AMURLStreamHandlerFactory stFactory = 
            new AMURLStreamHandlerFactory();
    
    static {
        String rto = SystemPropertiesManager.get(URL_READTIMEOUT);
        prot_handler_string = 
                SystemPropertiesManager.get(Constants.PROTOCOL_HANDLER, null);
        if (debug.messageEnabled()) {
            debug.message("Configured Protocol Handler : " + 
                    prot_handler_string);
        }
        
        if (rto != null && rto.length() > 0) {
            try {
                READTIMEOUT = Integer.valueOf(rto).intValue();
                if (debug.messageEnabled()) { 
                    debug.message("HttpURLConnectionManager.<init>: " + 
                        "Set READTIMEOUT to " + READTIMEOUT);
                }
            } catch (Exception e) {
                debug.error("HttpURLConnectionManager.<init>: Fail to read " +
                        URL_READTIMEOUT + " set READTIMEOUT to the default " +
                        READTIMEOUT, e);
            }

            try {
                URL url = new URL("http://opensso.dev.java.net");
                HttpURLConnection conn = 
                        (HttpURLConnection)url.openConnection();
                Class[] param = { Integer.TYPE };
                method = conn.getClass().getMethod("setReadTimeout", param);
                url = new URL("https://opensso.dev.java.net");
                conn = (HttpURLConnection)url.openConnection();
                method_s = conn.getClass().getMethod("setReadTimeout", param);
                args = new Object[] { new Integer(READTIMEOUT) };
            } catch (NoSuchMethodException e) {
                debug.warning("HttpURLConnectionManager.<init>: " +
                    "setReadTimeout is not supported by the JVM", e);
            } catch (Exception e) {
                debug.error("HttpURLConnectionManager.<init>: " + 
                    "Failed to find setReadTimeout method ", e);
            }
        }
    }
    
    /**
     * Get the <code>HttpURLConnection</code> and set read timeout when possible
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
        if (method != null) {
            try {
                if (prot.equalsIgnoreCase("http")) {
                    method.invoke(conn, args);
                } else {
                    method_s.invoke(conn, args);
                }
                if (debug.messageEnabled()) {
                    debug.message("HttpURLConnectionManager.getConnection: " + 
                            "set read timeout to " + READTIMEOUT);
                }
            } catch(IllegalAccessException e) {
                debug.error("HttpURLConnectionManager.getConnection: " + 
                        "Failed to set read timeout", e);
            } catch(IllegalArgumentException e) {
                debug.error("HttpURLConnectionManager.getConnection: " + 
                        "Failed to set read timeout", e);
            } catch(InvocationTargetException e) {
                debug.error("HttpURLConnectionManager.getConnection: " + 
                        "Failed to set read timeout", e);
            }
        }
        return conn;
    }
}
