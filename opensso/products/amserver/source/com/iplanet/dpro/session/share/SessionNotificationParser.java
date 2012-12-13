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
 * $Id: SessionNotificationParser.java,v 1.3 2008/06/25 05:41:31 qcheng Exp $
 *
 */

package com.iplanet.dpro.session.share;

import com.sun.identity.shared.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * <code>SessionNotificationParser</code> parses the 
 * <code>SessionNotification</code> XML document and returns the 
 * <code>SessionNotification</code> object
 *
 */
class SessionNotificationParser {
    /**
     * <code>SessionNotification</code> object being returned after parsing
     * the XML document.
     */
    private SessionNotification sessionNotification = null;

    /**
     * Document to be parsed
     */
    private Document document;

   /**
    * Constructs <code>SessionNotificationParser</code>
    * @param xmlDoc Notification XML Document to be parsed.
    */
   public SessionNotificationParser(Document xmlDoc) {
        document = xmlDoc;
    }

   /**
    * 
    * Constructs <code>SessionNotificationParser</code>
    * @param xmlString Notification.
    *
    */
   public SessionNotificationParser(String xmlString) {
        document = XMLUtils
                .toDOMDocument(xmlString, SessionRequestParser.debug);
    }

    /**
     * Parses the session notification element. Please see file
     * <code>SessionNotification.dtd</code> for the corresponding DTD of the
     * <code>SessionNotification</code>.
     * 
     * @return a <code>SessionNotification</code> object.
     */
    public SessionNotification parseXML() {
        if (document == null) {
            return null;
        }

        // get document element
        Element elem = document.getDocumentElement();
        sessionNotification = new SessionNotification();
        // set notification attribute
        String temp = elem.getAttribute("vers");
        sessionNotification.setNotificationVersion(temp);
        // set notification id
        temp = elem.getAttribute("notid");
        sessionNotification.setNotificationID(temp);

        // process Session element
        NodeList nodelist = elem.getElementsByTagName("Session");
        if (nodelist != null && nodelist.getLength() != 0) {
            Element sess = (Element) nodelist.item(0);
            if (sess != null) {
                sessionNotification.setSessionInfo(SessionResponseParser
                        .parseSessionElement(sess));
            }
        }

        // process Type element
        nodelist = elem.getElementsByTagName("Type");
        if (nodelist != null && nodelist.getLength() != 0) {
            Element type = (Element) nodelist.item(0);
            if (type != null) {
                try {
                    int sType = (new Integer(SessionRequestParser
                            .parseCDATA(type))).intValue();
                    sessionNotification.setNotificationType(sType);
                } catch (Exception e) {
                    SessionRequestParser.debug.message("Session.Notif Type", e);
                }
            }
        }

        // process Time element
        nodelist = elem.getElementsByTagName("Time");
        if (nodelist != null && nodelist.getLength() != 0) {
            Element time = (Element) nodelist.item(0);
            if (time != null) {
                try {
                    long sTime = (new Long(SessionRequestParser
                            .parseCDATA(time))).longValue();
                    sessionNotification.setNotificationTime(sTime);
                } catch (Exception e) {
                    SessionRequestParser.debug.message("Session.Notif Time", e);
                }
            }
        }

        return sessionNotification;
    }

}
