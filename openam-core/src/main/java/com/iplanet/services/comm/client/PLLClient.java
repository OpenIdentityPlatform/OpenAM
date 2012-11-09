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
 * $Id: PLLClient.java,v 1.8 2008/06/25 05:41:33 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.comm.client;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.comm.share.PLLBundle;
import com.iplanet.services.comm.share.RequestSet;
import com.iplanet.services.comm.share.ResponseSet;
import com.iplanet.services.naming.WebtopNaming.SiteMonitor;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.common.HttpURLConnectionManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.servlet.http.Cookie;

/**
 * The <code>PLLClient</code> class is used to send RequestSet XML documents
 * to the URL specified in the send() method. The high level services and
 * application can use Naming Service to find the service specific URL. This
 * class provides static methods to register notification handlers.
 * 
 * @see com.iplanet.services.comm.share.RequestSet
 * @see com.iplanet.services.comm.share.Response
 * @see com.iplanet.services.comm.client.SendRequestException
 * @see com.iplanet.services.comm.client.NotificationHandler
 */

public class PLLClient {

    /** notification handlers */
    private static Hashtable notificationHandlers = new Hashtable();

    private static Debug debug = Debug.getInstance("PLLClient");
    
    private static boolean useCache = Boolean.getBoolean(
      SystemProperties.get(Constants.URL_CONNECTION_USE_CACHE, "false"));
    
    

    /**
     * Translates the Java object to an XML RequestSet document and sends the
     * corresponding XML document to the specified URL.
     * 
     * @param url
     *            The destination URL for the RequestSet XML document.
     * @param set
     *            The RequestSet Java object to be translated to an XML
     *            RequestSet document.
     * @exception SendRequestException
     *                if there is an error in sending the XML document.
     */
    public static Vector send(URL url, RequestSet set)
            throws SendRequestException {
        return send(url, null, set, null);
    }

    /**
     * Translates the Java object to an XML RequestSet document and sends the
     * corresponding XML document to the specified URL.
     * 
     * @param url
     *            The destination URL for the RequestSet XML document.
     * @param cookies
     *            The value for Http Request Header 'Cookie'
     * @param set
     *            The RequestSet Java object to be translated to an XML
     *            RequestSet document.
     * @exception SendRequestException
     *                if there is an error in sending the XML document.
     */
    public static Vector send(URL url, String cookies, RequestSet set)
            throws SendRequestException {
        return send(url, cookies, set, null);
    }

    /**
     * Translates the Java object to an XML RequestSet document and sends the
     * corresponding XML document to the specified URL.
     * 
     * @param url
     *            The destination URL for the RequestSet XML document.
     * @param set
     *            The RequestSet Java object to be translated to an XML
     *            RequestSet document.
     * @param cookieTable
     *            The HashMap that constains cookies to be replayed and stores
     *            cookies retrieved from the response.
     * @exception SendRequestException
     *                if there is an error in sending the XML document.
     */
    public static Vector send(URL url, RequestSet set, HashMap cookieTable)
            throws SendRequestException {
        return send(url, null, set, cookieTable);
    }

    // The private method that implements the above interfaces.
    // HashMap cookieTable passes in the cookies that will be replayed. It also
    // is the place holder to retrieve additional cookies if any from the
    // URL connection response.
    private static Vector send(URL url, String cookies, RequestSet set,
            HashMap cookieTable) throws SendRequestException {
        HttpURLConnection conn = null;
        OutputStream out = null;
        BufferedReader in = null;
        try {
            if ((SiteMonitor.keepMonitoring == true) &&
                !SiteMonitor.isAvailable(url)) {
                debug.error("Site " + url.toString() + " is down.");
                throw new SendRequestException("Site is down.");
            }
    	    
            conn = HttpURLConnectionManager.getConnection(url);
            conn.setDoOutput(true);
            conn.setUseCaches(useCache);
            conn.setRequestMethod("POST");

            // replay cookies
            StringBuffer cookieStr = null;
            if (cookies != null) {
                cookieStr = new StringBuffer();
                cookieStr.append(cookies);
            }
            if (cookieTable != null && !cookieTable.isEmpty()) {
                for (Iterator it = cookieTable.values().iterator(); it
                        .hasNext();) {
                    Cookie cookie = (Cookie) it.next();
                    if (cookieStr == null) {
                        cookieStr = new StringBuffer();
                    } else {
                        cookieStr.append(";");
                    }
                    cookieStr.append(cookie.getName()).append("=").append(
                            cookie.getValue());
                }
            }

            if (cookieStr != null) {
                cookies = cookieStr.toString();
                if (debug.messageEnabled()) {
                    debug.message("sending cookies: " + cookies);
                }
                conn.setRequestProperty("Cookie", cookies);
            }
            conn.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");

            // Output ...
            String xml = set.toXMLString();
            // compute and set length, just in case iWS set arbitrary length
            int requestLength = xml.getBytes("UTF-8").length;
            conn.setRequestProperty("Content-Length", Integer
                    .toString(requestLength));
            out = conn.getOutputStream();
            out.write(xml.getBytes("UTF-8"));
            out.flush();

            // Input ...
            in = new BufferedReader(new InputStreamReader(conn
                    .getInputStream(), "UTF-8"));
            StringBuilder in_buf = new StringBuilder();
            int len;
            char[] buf = new char[1024];
            while ((len = in.read(buf, 0, buf.length)) != -1) {
                in_buf.append(buf, 0, len);
            }
            String in_string = in_buf.toString();

            // retrieves cookies from the response
            Map headers = conn.getHeaderFields();
            if (cookieTable != null) {
                parseCookies(headers, cookieTable);
            }

            ResponseSet resset = ResponseSet.parseXML(in_string);
            return resset.getResponses();
        } catch (Exception e) {
            debug.message("PLLClient send exception: ", e);
            throw new SendRequestException(e.getMessage());
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    throw new SendRequestException(e.getMessage());
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw new SendRequestException(e.getMessage());
                }
            }    
        }
    }

    /**
     * Parses the cookies from the response header and stores them in
     * in cookieTable
     * 
     * @param headers
     *            The Map containig headers
     * @param cookieTable
     *            The HashMap that constains cookies to be replayed and stores
     *            cookies retrieved from the response.
     */
    public static void parseCookies(Map headers, HashMap cookieTable) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        if (debug.messageEnabled()) {
            debug.message("header in parseCookies(): " + headers);
        }
        for (Iterator hrs = headers.entrySet().iterator(); hrs.hasNext();) {
            Map.Entry me = (Map.Entry) hrs.next();
            String key = (String) me.getKey();
            if (key != null && (key.equalsIgnoreCase("Set-cookie") ||
                (key.equalsIgnoreCase("cookie")))) {
                List list = (List) me.getValue();
                if (list == null || list.isEmpty()) {
                    continue;
                }
                Cookie cookie = null;
                for (Iterator it = list.iterator(); it.hasNext();) {
                    String cookieStr = (String) it.next();
                    if (debug.messageEnabled()) {
                        debug.message("cookie: " + cookieStr);
                    }
                    StringTokenizer stz = new StringTokenizer(cookieStr, ";");
                    if (stz.hasMoreTokens()) {
                        String nameValue = stz.nextToken();
                        int index = nameValue.indexOf("=");
                        if (index == -1) {
                            continue;
                        }
                        String tmpName = nameValue.substring(0, index).trim();
                        String value = nameValue.substring(index + 1);
                        cookie = new Cookie(tmpName, value);
                        cookieTable.put(tmpName, cookie);
                    }
                }
            }
        }
    }

    /**
     * Adds a notification handler for a service. This handler is used by the
     * Platform Low Level servlet to pass notifications for processing.
     * 
     * @param service
     *            The name of the service such as session, profile
     * @param handler
     *            The handler for notification processing
     */
    public static void addNotificationHandler(String service,
            NotificationHandler handler) throws AlreadyRegisteredException {
        if (notificationHandlers.containsKey(service)) {
            throw new AlreadyRegisteredException(PLLBundle
                    .getString("alreadyRegistered")
                    + service);
        }
        notificationHandlers.put(service, handler);
    }

    /**
     * Removes a notification handler of a service.
     * 
     * @param service
     *            The name of the service whose handler needs to be removed
     */
    public static void removeNotificationHandler(String service) {
        notificationHandlers.remove(service);
    }

    /**
     * Gets a notification handler of a service.
     * 
     * @param service
     *            The name of the service whose handler needs to be returned
     */
    public static NotificationHandler getNotificationHandler(String service) {
        return (NotificationHandler) notificationHandlers.get(service);
    }
}
