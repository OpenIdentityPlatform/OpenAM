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
 * $Id: NotificationSetParser.java,v 1.3 2008/06/25 05:41:35 qcheng Exp $
 *
 */

package com.iplanet.services.comm.share;

import com.sun.identity.shared.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class NotificationSetParser {

    /**
     * Document to be parsed
     */
    Document document;

    public NotificationSetParser(Document xmlDoc) {
        document = xmlDoc;
    }

    public NotificationSetParser(String xmlString) {
        document = XMLUtils.toDOMDocument(xmlString, RequestSetParser.debug);
    }

    /**
     * Parses the tree from root element. Please see NotificationSet.java for
     * the corresponding DTD of the NotificationSet.
     * 
     * @return a NotificationSet object.
     */
    public NotificationSet parseXML() {
        if (document == null) {
            return null;
        }

        // get NotificationSet element
        Element notifSetElem = document.getDocumentElement();
        NotificationSet notificationSet = new NotificationSet();
        // set notification set attributes
        setNotificationSetAttributes(notifSetElem, notificationSet);

        // get Nofifications
        NodeList notifs = notifSetElem.getElementsByTagName("Notification");
        if (notifs == null) {
            return notificationSet;
        }

        // go through each notifications, and add them to the notif set
        int nodeLen = notifs.getLength();
        for (int i = 0; i < nodeLen; i++) {
            notificationSet
                    .addNotification(parseNotificationElement((Element) notifs
                            .item(i)));
        }

        return notificationSet;
    }

    /**
     * This method is an internal method used by parseXML method.
     */
    public void setNotificationSetAttributes(Element elem,
            NotificationSet notifSet) {
        // get vers attribute
        String temp = elem.getAttribute("vers");
        if (temp != null) {
            notifSet.setNotificationSetVersion(temp);
        }

        // get service id
        temp = elem.getAttribute("svcid");
        if (temp != null) {
            notifSet.setServiceID(temp);
        }

        // get notification id
        temp = elem.getAttribute("notid");
        if (temp != null) {
            notifSet.setNotificationSetID(temp);
        }
    }

    /**
     * function to parse a single notification element. Notification contain a
     * text element and dtdid attribute
     */
    private Notification parseNotificationElement(Element elem) {
        Notification notif = new Notification();
        // process request attributes
        String temp = elem.getAttribute("dtdid");
        if (temp != null) {
            notif.setDtdID(temp);
        }

        // process TEXT child element
        Node text = null;
        NodeList nlist = elem.getChildNodes();
        int nodeLen = nlist.getLength();
        for (int i = 0; i < nodeLen; i++) {
            Node n = nlist.item(i);
            if (n.getNodeType() == Node.CDATA_SECTION_NODE) {
                text = n;
                break;
            }
        }

        if (text != null) {
            notif.setContent(text.getNodeValue());
        }
        return notif;
    }
}
