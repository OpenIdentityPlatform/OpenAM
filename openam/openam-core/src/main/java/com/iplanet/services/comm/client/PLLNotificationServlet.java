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
 * $Id: PLLNotificationServlet.java,v 1.6 2008/08/19 19:08:43 veiming Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.comm.client;

import com.iplanet.services.comm.share.NotificationSet;
import com.iplanet.services.comm.share.PLLBundle;
import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.common.RequestUtils;
import com.sun.identity.shared.locale.L10NMessageImpl;
import java.io.InputStream;
import java.util.Vector;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 * The <code>PLLNotificationServlet</code> class is used to receive
 * notifications from servers and forward those notifications to the high level
 * services and applications for processing.
 * </p>
 * The doPost() method gets the XML NotificationSet document from the
 * HttpRequest object, then parses the XML documnent and reconstructs a
 * NotificationSet object.
 * 
 * @see com.iplanet.services.comm.share.Notification
 * @see com.iplanet.services.comm.share.NotificationSet
 */

public class PLLNotificationServlet extends HttpServlet {

    public void init() throws ServletException {
    }

    /*
     * Accepts POST requests, reads Inpt Stream, forwards the NotificationSet
     * XML Flushes the ResponseSet XML to OutputStream @param
     * HttpServletNotification Reference to HttpServletNotification object
     * @param HttpServletResponse Reference to HttpServletResponse object
     * 
     * @see javax.servlet.http.HttpServlet
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, java.io.IOException {

        // Check content length
        try {
            RequestUtils.checkContentLength(request);
        } catch (L10NMessageImpl e) {
            ISLocaleContext localeContext = new ISLocaleContext();
            localeContext.setLocale(request);
            java.util.Locale locale = localeContext.getLocale();
            throw new ServletException(e.getL10NMessage(locale));
        }

        int length = request.getContentLength();
        if (length == -1) {
            throw new ServletException(PLLBundle.getString("unknownLength"));
        }

        byte[] reqData = new byte[length];
        InputStream in = request.getInputStream();
        int rlength = 0;
        int offset = 0;
        while (rlength != length) {
            int r = in.read(reqData, offset, length - offset);
            if (r == -1) {
                throw new ServletException(PLLBundle
                        .getString("readRequestError"));
            }
            rlength += r;
            offset += r;
        }
        String xml = new String(reqData, 0, length, "UTF-8");

        ServletOutputStream out = response.getOutputStream();
        try {
            try {
                handleNotification(xml);
                out.print("OK");
            } catch (ServletException e) {
                out.print("NOT OK");
            }
            out.flush();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, java.io.IOException {
        ServletOutputStream out = response.getOutputStream();
        out.print("OpenSSO");
        out.flush();
    }

    /*
     * This methid is used by doPost method. It gets the corresponding
     * notification handler and passes the Notification objects to it for
     * processing. @param notificationXML The XML String for the NotificationSet
     * object.
     */
    private void handleNotification(String notificationXML)
            throws ServletException {
        NotificationSet set = NotificationSet.parseXML(notificationXML);
        Vector nots = set.getNotifications();
        if (!nots.isEmpty()) {
            // Each notification in this set shall have the same service id
            String serviceid = set.getServiceID();

            // Get the notification handler ...
            NotificationHandler ns = PLLClient
                    .getNotificationHandler(serviceid);
            if (ns == null) {
                throw new ServletException(PLLBundle
                        .getString("noNotificationHandler")
                        + serviceid);
            }
            ns.process(nots);
        }
    }
}
