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
 * $Id: Notification.java,v 1.2 2008/06/25 05:41:35 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */

package com.iplanet.services.comm.share;

import com.sun.identity.shared.xml.XMLUtils;

/**
 * This <code>Notification</code> class represents a notification. The most
 * important information in this Notification object is the content of this
 * notification. The content in this Notification object can be an arbitrary
 * String. This makes it possible that high level services and applications can
 * define their own notification XML DTDs and then embed the corresponding XML
 * document into this Notification object as its content.
 * 
 * @see com.iplanet.services.comm.share.NotificationSet
 */

public class Notification {

    private String dtdID = null;

    private String notificationContent = "";

    /*
     * Constructors
     */

    /**
     * Constructs an instance of Notification class with the content of the
     * Notification. The DTD ID needs to be set explicitly using the
     * corresponding setter as it is optional for the notification.
     * 
     * @param content
     *            The content of this Notification.
     */
    public Notification(String content) {
    	notificationContent =  XMLUtils.removeInvalidXMLChars(content);
    }

    /**
     * This constructor is used by NotificationSetParser to reconstruct a
     * Notification object.
     */
    Notification() {
    }

    /**
     * Gets the ID of the DTD for the content of the Notification
     * 
     * @return The ID of the DTD for the content of the Notification.
     */
    public String getDtdID() {
        return dtdID;
    }

    /**
     * Gets the content of the Notification.
     * 
     * @return The content of the Notification.
     */
    public String getContent() {
        return notificationContent;
    }

    /**
     * Sets the ID of the DTD for the content of the Notification
     * 
     * @param id
     *            The ID of the DTD for the content of the Notification.
     */
    public void setDtdID(String id) {
        dtdID = id;
    }

    /**
     * Sets the content of the Notification.
     * 
     * @param content
     *            The content of the Notification in String format.
     */
    public void setContent(String content) {
        notificationContent = content;
    }
}
