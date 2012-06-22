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
 * $Id: SessionNotification.java,v 1.2 2008/06/25 05:41:31 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.dpro.session.share;

/**
 * This <code>SessionNotification</code> class represents a
 * <code>SessionNotification</code> XML document. The
 * <code>SessionNotification</code> DTD is defined as the following:
 * </p>
 * 
 * <pre>
 *     &lt;?xml version=&quot;1.0&quot;&gt;
 *     &lt; !DOCTYPE SessionNotification [
 *     &lt; !ELEMENT SessionNotification (Session, Type, Time)&gt;
 *     &lt; !ATTLIST SessionNotification
 *       vers   CDATA #REQUIRED
 *       notid  CDATA #REQUIRED&gt;
 *     &lt; !ELEMENT Session&gt;
 *     &lt; !ATTLIST Session
 *       sid        CDATA #REQUIRED
 *       stype      (user|application) &quot;user&quot;
 *       cid        CDATA #REQUIRED
 *       cdomain    CDATA #REQUIRED
 *       maxtime    CDATA #REQUIRED
 *       maxidle    CDATA #REQUIRED
 *       maxcaching CDATA #REQUIRED
 *       timeleft   CDATA #REQUIRED
 *       timeidle   CDATA #REQUIRED   
 *       state      (invalid|valid|inactive|destroyed) &quot;invalid&quot;&gt;
 *     &lt; !ELEMENT Type (creation|idle|max|logout|reactivation|destroy) 
 *                  &quot;creation&quot;&gt;
 *     &lt; !ELEMENT Time (#PCDATA)&gt;
 *     ]&gt;
 * </pre>
 * 
 * </p>
 */
public class SessionNotification {

    static final String QUOTE = "\"";

    static final String NL = "\n";

    private String notificationVersion = "1.0";

    private String notificationID = null;

    private SessionInfo sessionInfo = null;

    private int notificationType;

    private long notificationTime;

    private static int notificationCount = 0;

    /*
     * Constructors
     */

    /**
     * This constructor shall only be used at the server side to construct a
     * <code>SessionNotification</code> object.
     * 
     * @param info The session information.
     * @param type The session event type.
     * @param time The session event time.
     */
    public SessionNotification(SessionInfo info, int type, long time) {
        sessionInfo = info;
        notificationType = type;
        notificationTime = time;
        notificationID = (new Integer(notificationCount++)).toString();
    }

    /*
     * This constructor is used by <code>SessionNotificationParser</code> to
     * reconstruct a <code>SessionNotification</code> object.
     */
    SessionNotification() {
    }

    /**
     * This method is used primarily at the server side to reconstruct a
     * <code>SessionNotification</code> object based on the XML document
     * received from client. The DTD of this XML document is described above.
     * 
     * @param xml The <code>SessionNotification</code> XML document String.
     */
    public static SessionNotification parseXML(String xml) {
        SessionNotificationParser parser = new SessionNotificationParser(xml);
        return parser.parseXML();
    }

    /**
     * Sets the notification version.
     * 
     * @param version Notification version.
     */
    void setNotificationVersion(String version) {
        notificationVersion = version;
    }

    /**
     * Returns the notification version.
     * 
     * @return The notification version.
     */
    public String getNotificationVersion() {
        return notificationVersion;
    }

    /**
     * Sets the notification ID.
     * 
     * @param id Notification ID.
     */
    void setNotificationID(String id) {
        notificationID = id;
    }

    /**
     * Returns the notification ID.
     * 
     * @return The notification ID.
     */
    public String getNotificationID() {
        return notificationID;
    }

    /**
     * Sets the session information.
     * 
     * @param info The session information.
     */
    void setSessionInfo(SessionInfo info) {
        sessionInfo = info;
    }

    /**
     * Returns the session information.
     * 
     * @return The session information.
     */
    public SessionInfo getSessionInfo() {
        return sessionInfo;
    }

    /**
     * Sets the notification type.
     *
     * @param type Session type.
     */
    void setNotificationType(int type) {
        notificationType = type;
    }

    /**
     * Returns the notification type.
     *
     * @return Notification type.
     */
    public int getNotificationType() {
        return notificationType;
    }

    /**
     * Sets the notification time.
     *
     * @param time notification time.
     */
    void setNotificationTime(long time) {
        notificationTime = time;
    }

    /**
     * Returns the notification time.
     *
     * @return Session notification time.
     */
    public long getNotificationTime() {
        return notificationTime;
    }

    /**
     * Translates the notification to an XML document String based
     * on the <code>SessionNotification</code> DTD described above.
     * 
     * @return An XML String representing the notification.
     */
    public String toXMLString() {
        StringBuilder xml = new StringBuilder(300);
        xml.append("<SessionNotification vers=").append(QUOTE).append(
                notificationVersion).append(QUOTE).append(" notid=").append(
                QUOTE).append(notificationID).append(QUOTE).append(">").append(
                NL);

        xml.append(sessionInfo.toXMLString()).append(NL);
        xml.append("<Type>").append(Integer.toString(notificationType)).append(
                "</Type>").append(NL);
        xml.append("<Time>").append(Long.toString(notificationTime)).append(
                "</Time>").append(NL);
        xml.append("</SessionNotification>");
        return xml.toString();
    }
}
