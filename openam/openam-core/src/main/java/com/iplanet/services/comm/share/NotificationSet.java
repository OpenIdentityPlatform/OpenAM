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
 * $Id: NotificationSet.java,v 1.2 2008/06/25 05:41:35 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.comm.share;

import java.util.Vector;

/**
 * This <code>NotificationSet</code> class represents a NotificationSet XML
 * document. The NotificationSet DTD is defined as the following:
 * </p>
 * 
 * <pre>
 *     &lt;?xml version=&quot;1.0&quot;&gt;
 *     &lt; !-- This DTD is used by PLL --&gt;
 *     &lt; !DOCTYPE NotificationSet [
 *     &lt; !ELEMENT NotificationSet(Notification)+&gt;
 *     &lt; !ATTLIST NotificationSet
 *       vers  CDATA #REQUIRED
 *       svcid CDATA #REQUIRED
 *       notid CDATA #REQUIRED&gt;
 *     &lt; !ELEMENT Notification(#PCDATA)*&gt;
 *     &lt; !ATTLIST Notification
 *       dtdid CDATA #IMPLIED&gt;
 *     ]&gt;
 * </pre>
 * 
 * </p>
 * Each NotificationSet object contains a version, service ID, notification set
 * ID, and a collection of Notification objects. The NotificationSet views each
 * Notification object as a String. This makes it possible that the content of
 * the Notification object can be another XML document. The PLL provides a
 * reference Notification DTD. Please see class Notification for details on the
 * Notification DTD. This class also provides a method to aggregate each
 * Notification object and returns a NotificationSet XML document based on the
 * NotificationSet DTD mentioned above.
 * 
 * @see com.iplanet.services.comm.share.Notification
 */

public class NotificationSet {

    static final char QUOTE = '\"';

    static final char NL = '\n';

    static final String BEGIN_CDATA = "<![CDATA[";

    static final String END_CDATA = "]]>";

    private String notificationSetVersion;

    private String serviceID;

    private String notificationSetID;

    private Vector notificationVector = new Vector();

    private static int notificationCount = 0;

    /**
     * This constructor is used primarily at the server side to construct a
     * NotificationSet object for a given service. Individual notification shall
     * be added to this object by calling addNotification method.
     * 
     * @param service
     *            The name of the service.
     */
    public NotificationSet(String service) {
        serviceID = service;
        notificationSetVersion = "1.0";
        notificationSetID = Integer.toString(notificationCount++);
    }

    /**
     * This constructor is used by NotificationSetParser to reconstruct a
     * NotificationSet object.
     */
    NotificationSet() {
    }

    /**
     * This method is used primarily at the client side to reconstruct a
     * NotificationSet object based on the XML document received from server.
     * The DTD of this XML document is described above.
     * 
     * @param xml
     *            The NotificationSet XML document String.
     */
    public static NotificationSet parseXML(String xml) {
        // Parse the XML document and extract the XML objects out of the
        // XML document
        NotificationSetParser parser = new NotificationSetParser(xml);
        return parser.parseXML();
    }

    /**
     * Gets the service ID of the NotificationSet request.
     * 
     * @return The service ID of the NotificationSet request.
     */
    public String getServiceID() {
        return serviceID;
    }

    /**
     * Gets the Notification objects contained in this object.
     * 
     * @return A Vector of Notification objects.
     */
    public Vector getNotifications() {
        return notificationVector;
    }

    /**
     * Adds a Notification object to this object.
     * 
     * @param notification
     *            A reference to a Notification object.
     */
    public void addNotification(Notification notification) {
        notificationVector.addElement(notification);
    }

    /**
     * Returns an XML NotificationSet document in String format. The returned
     * String is formatted based on the NotificationSet DTD by aggregating each
     * Notification object in this object.
     * 
     * @return An XML NotificationSet document in String format.
     */
    public String toXMLString() {
        StringBuilder xml = new StringBuilder(300);
        xml.append("<?xml version=").append(QUOTE).append("1.0").append(QUOTE)
                .append(" encoding=").append(QUOTE).append("UTF-8").append(
                        QUOTE).append(" standalone=").append(QUOTE).append(
                        "yes").append(QUOTE).append("?>").append(NL);

        xml.append("<NotificationSet vers=").append(QUOTE).append(
                notificationSetVersion).append(QUOTE).append(" svcid=").append(
                QUOTE).append(serviceID).append(QUOTE).append(" notid=")
                .append(QUOTE).append(notificationSetID).append(QUOTE).append(
                        '>').append(NL);

        int numNotifications = notificationVector.size();
        for (int i = 0; i < numNotifications; i++) {
            Notification notif = (Notification) notificationVector.elementAt(i);
            xml.append("<Notification");
            if (notif.getDtdID() != null) {
                xml.append(" dtdid=").append(QUOTE).append(notif.getDtdID())
                        .append(QUOTE);
            }
            xml.append('>');
            xml.append(BEGIN_CDATA).append(notif.getContent())
                    .append(END_CDATA);
            xml.append("</Notification>").append(NL);
        }
        xml.append("</NotificationSet>");
        return (xml.toString());
    }

    /*
     * The following methods are used by NotificationSetParser to reconstruct a
     * NotificationSet object.
     */
    void setNotificationSetVersion(String ver) {
        notificationSetVersion = ver;
    }

    void setServiceID(String id) {
        serviceID = id;
    }

    void setNotificationSetID(String id) {
        notificationSetID = id;
    }
}
