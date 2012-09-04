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
 * $Id: SessionRequest.java,v 1.3 2008/06/25 05:41:31 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.dpro.session.share;

import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This <code>SessionRequest</code> class represents a
 * <code>SessionRequest</code> XML document. The <code>SessionRequest</code>
 * DTD is defined as the following:
 * </p>
 * 
 * <pre>
 *     &lt;?xml version=&quot;1.0&quot;&gt;
 *     &lt; !DOCTYPE SessionRequest [
 *     &lt; !ELEMENT SessionRequest (GetSession |
 *                                   GetValidSessions |
 *                                   DestroySession |
 *                                   Logout |
 *                                   AddSessionListener |
 *                                   AddSessionListenerOnAllSessions |
 *                                   SetProperty |
 *                                   GetSessionCount)&gt;
 *     &lt; !ATTLIST SessionRequest
 *         vers   CDATA #REQUIRED
 *         reqid  CDATA #REQUIRED&gt;
 *     &lt; !-- This attribute carries the requester identity info --&gt;
 *          requester  CDATA #IMPLIED&gt;
 *     &lt; !ELEMENT GetSession (SessionID)&gt;
 *     &lt; !-- This attribute indicates whether resets 
 *     the latest access time --&gt;
 *         reset  CDATA #REQUIRED&gt;
 *     &lt; !ELEMENT GetValidSessions (SessionID Pattern?)&gt;
 *     &lt; !ELEMENT DestroySession (SessionID, DestroySessionID)&gt;
 *     &lt; !ELEMENT Logout (SessionID)&gt;
 *     &lt; !ELEMENT AddSessionListener (SessionID, URL)&gt;
 *     &lt; !ELEMENT AddSessionListenerOnAllSessions (SessionID, URL)&gt;
 *     &lt; !ELEMENT SetProperty (SessionID, Property)&gt;
 *     &lt; !ATTLIST Property
 *         name   CDATA #REQUIRED
 *         value  CDATA #REQUIRED&gt;
 *     &lt; !ELEMENT SessionID (#PCDATA)&gt;
 *     &lt; !ELEMENT DestroySessionID (#PCDATA)&gt;
 *     &lt; !ELEMENT URL (#PCDATA)&gt;
 *     &lt; !ELEMENT GetSessionCount (SessionID, UUID)&gt;
 *     &lt; !ELEMENT UUID (#PCDATA)&gt;
 *     &lt; !ELEMENT Pattern (#PCDATA)&gt;
 *     ]&gt;
 * </pre>
 * 
 * </p>
 */

public class SessionRequest {

    public static final int GetSession = 0;

    public static final int GetValidSessions = 1;

    public static final int DestroySession = 2;

    public static final int Logout = 3;

    public static final int AddSessionListener = 4;

    public static final int AddSessionListenerOnAllSessions = 5;

    public static final int SetProperty = 6;

    public static final int GetSessionCount = 7;

    static final char QUOTE = '\"';

    static final char NL = '\n';

    static final String AMPERSAND = "&amp;";

    static final String LESSTHAN = "&lt;";

    static final String GREATERTHAN = "&gt;";

    static final String APOSTROPHE = "&apos;";

    static final String QUOTATION = "&quot;";

    private String requestVersion = "1.0";

    private String requestID = null;

    private boolean resetFlag;

    private int methodID;

    private String sessionID = null;

    private String requester = null;

    private String destroySessionID = null;

    private String notificationURL = null;

    private String propertyName = null;

    private String propertyValue = null;

    private String pattern = null;

    private String uuid = null;

    private static int requestCount = 0;

    /*
     * Constructors
     */

    /**
     * This constructor shall only be used at the client side to construct a
     * <code>SessionRequest</code> object.
     * 
     * @param method The method ID of the <code>SessionRequest</code>.
     * @param sid The session ID required by the <code>SessionRequest</code>.
     * @param reset The flag to indicate whether this request needs to update
     *        the latest session access time.
     */
    public SessionRequest(int method, String sid, boolean reset) {
        methodID = method;
        sessionID = sid;
        resetFlag = reset;
        requestID = (new Integer(requestCount++)).toString();
    }

    /*
     * This constructor is used by <code>SessionRequestParser</code>
     * to reconstruct a <code>SessionRequest</code> object.
     *
     */
    SessionRequest() {
    }

    /**
     * This method is used primarily at the server side to reconstruct a
     * <code>SessionRequest</code> object based on the XML document received
     * from client. The DTD of this XML document is described above.
     * 
     * @param xml The <code>SessionRequest</code> XML document String.
     * @return <code>SessionRequest</code> object.
     */
    public static SessionRequest parseXML(String xml) {
        SessionRequestParser parser = new SessionRequestParser(xml);
        return parser.parseXML();
    }

    /**
     * Sets the request version.
     * 
     * @param version Request version.
     */
    void setRequestVersion(String version) {
        requestVersion = version;
    }

    /**
     * Returns the request version.
     * 
     * @return The request version.
     */
    public String getRequestVersion() {
        return requestVersion;
    }

    /**
     * Sets the request ID.
     * 
     * @param id Request ID.
     */
    void setRequestID(String id) {
        requestID = id;
    }

    /**
     * Returns the request ID.
     * 
     * @return The request ID.
     */
    public String getRequestID() {
        return requestID;
    }

    /**
     * Sets the method ID.
     * 
     * @param id Method ID.
     */
    void setMethodID(int id) {
        methodID = id;
    }

    /**
     * Returns the method ID.
     * 
     * @return The method ID.
     */
    public int getMethodID() {
        return methodID;
    }

    /**
     * Sets the session ID.
     * 
     * @param id Session ID.
     */
    void setSessionID(String id) {
        sessionID = id;
    }

    /**
     * Returns the session ID.
     * 
     * @return Session ID.
     */
    public String getSessionID() {
        return sessionID;
    }

   /**
    * Sets the requester.
    *
    * @param requester Session requester.
    */
    public void setRequester(String requester) {
        this.requester = requester;
    }

   /**
    * Returns the requester
    *
    * @return id Session requester.
    */
   public String getRequester() {
        return requester;
    }

    /**
     * Sets the reset flag.
     * 
     * @param reset <code>true</code> to update the latest session access time.
     */
    void setResetFlag(boolean reset) {
        resetFlag = reset;
    }

    /**
     * Returns the reset flag.
     * 
     * @return The reset flag.
     */
    public boolean getResetFlag() {
        return resetFlag;
    }

    /**
     * Sets the ID of the session to be destroyed.
     * 
     * @param id The ID of the session to be destroyed.
     */
    public void setDestroySessionID(String id) {
        destroySessionID = id;
    }

    /**
     * Returns the ID of the session to be destroyed.
     * 
     * @return The ID of the session to be destroyed.
     */
    public String getDestroySessionID() {
        return destroySessionID;
    }

    /**
     * Sets the notification URL.
     * 
     * @param url The notification URL.
     */
    public void setNotificationURL(String url) {
        notificationURL = url;
    }

    /**
     * Returns the notification URL.
     * 
     * @return The notification URL.
     */
    public String getNotificationURL() {
        return notificationURL;
    }

    /**
     * Sets the property name.
     * 
     * @param name The property name.
     */
    public void setPropertyName(String name) {
        propertyName = name;
    }

    /**
     * Returns the property name.
     * 
     * @return The property name.
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Sets the property value.
     * 
     * @param value The property value.
     */
    public void setPropertyValue(String value) {
        propertyValue = value;
    }

    /**
     * Returns the property value.
     * 
     * @return The property value.
     */
    public String getPropertyValue() {
        return propertyValue;
    }

    /**
     * Sets the pattern value. Process escape chars in pattern with
     * <code>CDATA</code>.
     * 
     * @param value The pattern value.
     */
    public void setPattern(String value) {
        String data = value;

        if (value == null) {
            pattern = null;
            return;
        }

        data = replaceIllegalChar(data, '&', AMPERSAND);
        data = replaceIllegalChar(data, '\'', APOSTROPHE);
        data = replaceIllegalChar(data, '\"', QUOTATION);
        data = replaceIllegalChar(data, '<', LESSTHAN);
        data = replaceIllegalChar(data, '>', GREATERTHAN);

        pattern = data;
    }

    /**
     * Returns the pattern value.
     * 
     * @return The pattern value.
     */
    public String getPattern() {
        String data = pattern;

        if (data == null) {
            return null;
        }

        data = replaceEntityRef(data, AMPERSAND, '&');
        data = replaceEntityRef(data, APOSTROPHE, '\'');
        data = replaceEntityRef(data, QUOTATION, '\"');
        data = replaceEntityRef(data, LESSTHAN, '<');
        data = replaceEntityRef(data, GREATERTHAN, '>');

        return data;
    }

    /**
     * Sets the universal unique identifier.
     * 
     * @param id The universal unique identifier.
     */
    public void setUUID(String id) {
        uuid = id;
    }

    /**
     * Returns the universal unique identifier
     * 
     * @return The universal unique identifier
     */
    public String getUUID() {
        return uuid;
    }

    /**
     * Replacing illegal XML char with entity ref
     */
    private String replaceIllegalChar(String data, char ch, String replacement)
    {
        int idx = 0;

        StringBuilder buffer = new StringBuilder(data.length() * 4);
        while ((data != null) && (idx = data.indexOf(ch)) != -1) {
            buffer.append(data.substring(0, idx));
            buffer.append(replacement);
            data = data.substring(idx + 1);
        }
        if ((data != null) && (data.length() > 0)) {
            buffer.append(data);
        }
        return buffer.toString();
    }

    /**
     * Replacing entity ref with original char
     */
    private String replaceEntityRef(String data, String ref, char ch) {
        int idx = 0;

        StringBuilder buffer = new StringBuilder(data.length());
        while ((idx = data.indexOf(ref)) != -1) {
            buffer.append(data.substring(0, idx));
            buffer.append(ch);
            data = data.substring(idx + ref.length());
        }
        if ((data != null) && (data.length() > 0)) {
            buffer.append(data);
        }

        return buffer.toString();
    }

    /**
     * This method translates the request to an XML document String based on the
     * <code>SessionRequest</code> DTD described above. The ID of the session
     * to be destroyed has to be set for method <code>DestroySession</code>.
     * The notification URL has to be set for both methods 
     * <code>AddSessionListener</code> and
     * <code>AddSessionListenerOnAllSessions</code>. otherwise, the returns
     * <code>null</code>.
     * 
     * @return An XML String representing the request.
     */
    public String toXMLString() {

        StringBuilder xml = new StringBuilder();
        xml.append("<SessionRequest vers=").append(QUOTE).append(requestVersion).
                append(QUOTE).append(" reqid=").append(QUOTE).append(requestID).append(QUOTE);
        if (requester != null) {
            try {
                String data = Base64.encode(requester.getBytes("UTF8"));
                xml.append(" requester=").append(QUOTE).append(data).append(QUOTE);
            } catch (java.io.UnsupportedEncodingException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
        xml.append('>').append(NL);
        switch (methodID) {
        case GetSession:
            xml.append("<GetSession reset=");
            if (resetFlag)
                xml.append(QUOTE).append("true").append(QUOTE).append('>').append(NL);
            else
                xml.append(QUOTE).append("false").append(QUOTE).append('>').append(NL);
            xml.append("<SessionID>").append(sessionID).append("</SessionID>").append(NL);
            xml.append("</GetSession>").append(NL);
            break;
        case GetValidSessions:
            xml.append("<GetValidSessions>").append(NL);
            xml.append("<SessionID>").append(sessionID).append("</SessionID>").append(NL);
            if (pattern != null) {
                xml.append("<Pattern>").append(pattern).append("</Pattern>").append(NL);
            }
            xml.append("</GetValidSessions>").append(NL);
            break;
        case DestroySession:
            if (destroySessionID == null) {
                return null;
            }
            xml.append("<DestroySession>").append(NL);
            xml.append("<SessionID>").append(sessionID).append("</SessionID>").append(NL);
            xml.append("<DestroySessionID>").append(destroySessionID).append("</DestroySessionID>").append(NL);
            xml.append("</DestroySession>").append(NL);
            break;
        case Logout:
            xml.append("<Logout>").append(NL);
            xml.append("<SessionID>").append(sessionID).append("</SessionID>").append(NL);
            xml.append("</Logout>").append(NL);
            break;
        case AddSessionListener:
            if (notificationURL == null) {
                return null;
            }
            xml.append("<AddSessionListener>").append(NL);
            xml.append("<SessionID>").append(sessionID).append("</SessionID>").append(NL);
            xml.append("<URL>").append(notificationURL).append("</URL>").append(NL);
            xml.append("</AddSessionListener>").append(NL);
            break;
        case AddSessionListenerOnAllSessions:
            if (notificationURL == null) {
                return null;
            }
            xml.append("<AddSessionListenerOnAllSessions>").append(NL);
            xml.append("<SessionID>").append(sessionID).append("</SessionID>").append(NL);
            xml.append("<URL>").append(notificationURL).append("</URL>").append(NL);
            xml.append("</AddSessionListenerOnAllSessions>").append(NL);
            break;
        case SetProperty:
            if (propertyName == null || propertyValue == null) {
                return null;
            }
            xml.append("<SetProperty>").append(NL);
            xml.append("<SessionID>").append(sessionID).append("</SessionID>").append(NL);
            xml.append("<Property name=").append(QUOTE).append(
                    XMLUtils.escapeSpecialCharacters(propertyName)).append(QUOTE).
                    append(" value=").append(QUOTE).append(XMLUtils.escapeSpecialCharacters(propertyValue)).
                    append(QUOTE).append('>').append("</Property>").append(NL);
            xml.append("</SetProperty>").append(NL);
            break;
        case GetSessionCount:
            xml.append("<GetSessionCount>").append(NL);
            xml.append("<SessionID>").append(sessionID).append("</SessionID>")
                    .append(NL);
            xml.append("<UUID>").append(uuid).append("</UUID>").append(NL);
            xml.append("</GetSessionCount>").append(NL);
            break;
        default:
            return null;
        }
        xml.append("</SessionRequest>");
        return xml.toString();
    }
}
