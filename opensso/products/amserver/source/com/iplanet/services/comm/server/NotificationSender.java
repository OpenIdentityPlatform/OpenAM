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
 * $Id: NotificationSender.java,v 1.4 2008/06/25 05:41:34 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.comm.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.comm.share.NotificationSet;
import com.iplanet.services.comm.share.PLLBundle;
import com.sun.identity.common.HttpURLConnectionManager;
import com.sun.identity.shared.Constants;

public class NotificationSender {

    private URL url;

    private NotificationSet set;

    private static boolean useCache = Boolean.getBoolean(
        SystemProperties.get(Constants.URL_CONNECTION_USE_CACHE, "false"));
    
    NotificationSender(URL u, NotificationSet s) {
        url = u;
        set = s;
    }

    public void run() throws SendNotificationException {
        HttpURLConnection conn = null;
        OutputStream httpOut = null;
        try {
            conn = HttpURLConnectionManager.getConnection(url);
            conn.setDoOutput(true);
            conn.setUseCaches(useCache);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
            String xml = set.toXMLString();
            // compute length in case iWS set arbitrary length
            int requestLength = xml.getBytes("UTF-8").length;
            conn.setRequestProperty("Content-Length", Integer
                    .toString(requestLength));
            if (PLLServer.pllDebug.messageEnabled()) {
                PLLServer.pllDebug.message("Sent NotificationSet XML :" + xml);
            }
            httpOut = conn.getOutputStream();
            httpOut.write(xml.getBytes("UTF-8"));
            httpOut.flush();

            // Input ...
            // We need to check the response so that the high level services
            // or applications can clean things up such as removing the
            // notification
            // URLs of the apps in case the apps died.
            // Read input stream fully
            StringBuilder in_buf = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn
                    .getInputStream()));
            String in_string;
            while ((in_string = in.readLine()) != null) {
                in_buf.append(in_string);
            }
            in_string = in_buf.toString();
            if (!in_string.equals("OK")) {
                throw new SendNotificationException(PLLBundle
                        .getString("sendNotificationFailed"));
            }
        } catch (Exception e) {
            PLLServer.pllDebug.error("Cannot send notification to " + url, e);
            // FIXME: Currently we ignore the exception received here, because
            // not all the agent answers with 'OK' if the notification was
            // received, see OPENAM-498 (and linked RFE) for more details.
        } finally {
            try {
                if (conn != null) {
                    conn.disconnect();
                }
                if (httpOut != null) {
                    httpOut.close();
                }
            } catch (IOException e) {
            }
        }
    }
}
