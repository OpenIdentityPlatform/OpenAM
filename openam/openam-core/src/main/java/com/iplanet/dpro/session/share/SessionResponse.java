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
 * $Id: SessionResponse.java,v 1.3 2008/06/25 05:41:31 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.iplanet.dpro.session.share;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.iplanet.dpro.session.SessionException;
import com.sun.identity.common.SearchResults;
import java.util.ArrayList;
import java.util.List;

/**
 * This <code>SessionResponse</code> class represents a
 * <code>SessionResponse</code> XML document. The <code>SessionResponse</code>
 * DTD is defined as the following:
 * </p>
 * 
 * <pre>
 *     &lt;?xml version=&quot;1.0&quot;&gt;
 *     &lt; !DOCTYPE SessionResponse [
 *     &lt; !ELEMENT SessionResponse(GetSession |
 *                                    GetValidSessions |
 *                                    DestroySession |
 *                                    Logout |
 *                                    AddSessionListener |
 *                                    AddSessionListenerOnAllSessions |
 *                                    SetProperty |
 *                                    GetSessionCount)&gt;
 *     &lt; !ATTLIST SessionResponse
 *         vers   CDATA #REQUIRED
 *         reqid  CDATA #REQUIRED&gt;
 *     &lt; !ELEMENT GetSession (Session|Exception)&gt;
 *     &lt; !ELEMENT GetValidSessions ((SessionList Status)|Exception)&gt;
 *     &lt; !ELEMENT DestroySession (OK|Exception)&gt;
 *     &lt; !ELEMENT Logout (OK|Exception)&gt;
 *     &lt; !ELEMENT AddSessionListener (OK|Exception)&gt;
 *     &lt; !ELEMENT AddSessionListenerOnAllSessions (OK|Exception)&gt;
 *     &lt; !ELEMENT SetProperty (OK|Exception)&gt;
 *     &lt; !ELEMENT GetSessionCount (AllSessionsGivenUUID|Exception)&gt;
 *     &lt; !ELEMENT SessionExpirationTimeInfo&gt;
 *     &lt; !ATTLIST SessionExpirationTimeInfo
 *         sid        CDATA #REQUIRED
 *         expTime    CDATA #REQUIRED&gt;
 *     &lt; !ELEMENT AllSessionsGivenUUID (SessionExpirationTimeInfo)*&gt;
 *     &lt; !ELEMENT Session (Property)*&gt;
 *     &lt; !ATTLIST Session
 *         sid        CDATA #REQUIRED
 *         stype      (user|application) &quot;user&quot;
 *         cid        CDATA #REQUIRED
 *         cdomain    CDATA #REQUIRED
 *         maxtime    CDATA #REQUIRED
 *         maxidle    CDATA #REQUIRED
 *         maxcaching CDATA #REQUIRED
 *         timeleft   CDATA #REQUIRED
 *         timeidle   CDATA #REQUIRED   
 *         state   (invalid|valid|inactive|destroyed) &quot;invalid&quot;&gt;
 *     &lt; !ELEMENT Property&gt;
 *     &lt; !ATTLIST Property
 *         name   CDATA #REQUIRED
 *         value  CDATA #REQUIRED&gt;
 *     &lt; !ELEMENT SessionList (Session)*&gt;
 *     &lt; !ELEMENT OK (#PCDATA)&gt;
 *     &lt; !ELEMENT Exception (#PCDATA)&gt;
 *     &lt; !ELEMENT Status (#PCDATA)&gt;
 *     ]&gt;
 * </pre>
 * 
 * </p>
 */

public class SessionResponse {

    static final char QUOTE = '\"';

    static final char NL = '\n';

    private String responseVersion = "1.0";

    private String requestID = null;

    private int methodID;

    // For GetSessionCount
    private Map allSessionsforGivenUUID = new HashMap();

    // For GetSession and GetValidSessions
    private List<SessionInfo> sessionInfoSet = new ArrayList<SessionInfo>();

    // For DestroySession, Logout, AddSessionListener and
    // AddSessionListenerOnAllSessions
    private boolean booleanFlag = false;

    private String exception = null;

    private int status = SearchResults.UNDEFINED_RESULT_COUNT;

    /**
     * This constructor shall only be used at the server side to construct a
     * <code>SessionResponse</code> object.
     * 
     * @param reqid The original <code>SessionRequest</code> ID.
     * @param method The method ID of the original Session Request.
     */
    public SessionResponse(String reqid, int method) {
        requestID = reqid;
        methodID = method;
    }

    /*
     * This constructor is used by <code>SessionResponseParser</code> to
     * reconstruct a <code>SessionResponse</code> object.
     */
    SessionResponse() {
    }

    /**
     * This method is used primarily at the client side to reconstruct a
     * <code>SessionResponse</code> object based on the XML document received
     * from server. The DTD of this XML document is described above.
     * 
     * @param xml The <code>SessionResponse</code> XML document.
     */
    public static SessionResponse parseXML(String xml) throws SessionException {
        SessionResponseParser parser = new SessionResponseParser(xml);
        return parser.parseXML();
    }

    /**
     * Sets the response version.
     * 
     * @param version Response version.
     */
    void setResponseVersion(String version) {
        responseVersion = version;
    }

    /**
     * Returns the response version.
     * 
     * @return The response version.
     */
    public String getResponseVersion() {
        return responseVersion;
    }

    /**
     * Sets the request ID.
     * 
     * @param id
     *            A string representing the original request ID.
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
     * @param id
     *            A integer representing the method ID.
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
     * Adds a <code>SessionInfo</code> object.
     */
    public void addSessionInfo(SessionInfo info) {
        sessionInfoSet.add(info);
    }

    /**
     * Returns the <code>SessionInfo</code>.
     *
     * @return Set containing the session information
     */
    public List<SessionInfo> getSessionInfo() {
        return sessionInfoSet;
    }

    /**
     * Sets the <code>SessionInfo</code>.
     * @param infos set containing the session information.
     */
    public void setSessionInfo(List<SessionInfo> infos) {
        sessionInfoSet = infos;
    }

    /**
     * Sets the flag.
     *
     * @param flag 
     */
    public void setBooleanFlag(boolean flag) {
        booleanFlag = flag;
    }

    /**
     * Returns the flag.
     * @return flag <code>true</code> if the flag is set,<code> false</code>
     * otherwise
     */
    public boolean getBooleanFlag() {
        return booleanFlag;
    }

    /**
     * Adds the Session Information for a User.
     *
     * @param sid Session ID.
     * @param expTime time when the session would expire.
     */
    public void addSessionForGivenUUID(String sid, Long expTime) {
        allSessionsforGivenUUID.put(sid, expTime);
    }

    /**
     * Sets the Sessions.
     *
     * @param sessions number for sessions for the user.
     */
    public void setSessionsForGivenUUID(Map sessions) {
        allSessionsforGivenUUID = sessions;
    }

    /**
    * Returns the Session Information for a User.
    *
    * @return list sessions for the user
    */
    public Map getSessionsForGivenUUID() {
        return allSessionsforGivenUUID;
    }

    /**
     * Returns the exception.
     * 
     * @return The exception.
     */
    public String getException() {
        return exception;
    }

    /**
     * Sets the exception.
     * 
     * @param ex Exception.
     */
    public void setException(String ex) {
        exception = ex;
    }

    /**
     * Returns the status.
     *
     * @return The status.
     */
    public int getStatus() {
        return status;
    }

    /**
     * Sets the status.
     * 
     * @param value Status.
     */
    public void setStatus(int value) {
        status = value;
    }

    /**
     * Translates the response to an XML document String based on
     * the <code>SessionResponse</code> DTD described above.
     * 
     * @return An XML String representing the response.
     */
    public String toXMLString() {
        StringBuilder xml = new StringBuilder();
        xml.append("<SessionResponse vers=").append(QUOTE).append(responseVersion).append(QUOTE).
                append(" reqid=").append(QUOTE).append(requestID).append(QUOTE).append('>').append(NL);
        switch (methodID) {
        case SessionRequest.GetSession:
            xml.append("<GetSession>").append(NL);
            if (exception != null) {
                xml.append("<Exception>").append(exception).append("</Exception>").append(NL);
            } else {
                if (sessionInfoSet.size() != 1) {
                    return null;
                }
                for (SessionInfo info : sessionInfoSet) {
                    xml.append(info.toXMLString());
                }
            }
            xml.append("</GetSession>").append(NL);
            break;
        case SessionRequest.GetValidSessions:
            xml.append("<GetValidSessions>").append(NL);
            if (exception != null) {
                xml.append("<Exception>").append(exception).append("</Exception>").append(NL);
            } else {
                xml.append("<SessionList>").append(NL);
                for (SessionInfo info : sessionInfoSet) {
                    xml.append(info.toXMLString());
                }
                xml.append("</SessionList>").append(NL);
                xml.append("<Status>").append(Integer.toString(status)).append("</Status>").append(NL);
            }
            xml.append("</GetValidSessions>").append(NL);
            break;
        case SessionRequest.DestroySession:
            xml.append("<DestroySession>").append(NL);
            if (exception != null) {
                xml.append("<Exception>").append(exception).append("</Exception>").append(NL);
            } else {
                xml.append("<OK></OK>").append(NL);
            }
            xml.append("</DestroySession>").append(NL);
            break;
        case SessionRequest.Logout:
            xml.append("<Logout>").append(NL);
            if (exception != null) {
                xml.append("<Exception>").append(exception).append("</Exception>").append(NL);
            } else {
                xml.append("<OK></OK>").append(NL);
            }
            xml.append("</Logout>").append(NL);
            break;
        case SessionRequest.AddSessionListener:
            xml.append("<AddSessionListener>").append(NL);
            if (exception != null) {
                xml.append("<Exception>").append(exception).append("</Exception>").append(NL);
            } else {
                xml.append("<OK></OK>").append(NL);
            }
            xml.append("</AddSessionListener>").append(NL);
            break;
        case SessionRequest.AddSessionListenerOnAllSessions:
            xml.append("<AddSessionListenerOnAllSessions>").append(NL);
            if (exception != null) {
                xml.append("<Exception>").append(exception).append("</Exception>").append(NL);
            } else {
                xml.append("<OK></OK>").append(NL);
            }
            xml.append("</AddSessionListenerOnAllSessions>").append(NL);
            break;
        case SessionRequest.SetProperty:
            xml.append("<SetProperty>").append(NL);
            if (exception != null) {
                xml.append("<Exception>").append(exception).append("</Exception>").append(NL);
            } else {
                xml.append("<OK></OK>").append(NL);
            }
            xml.append("</SetProperty>").append(NL);
            break;
        case SessionRequest.GetSessionCount:
            xml.append("<GetSessionCount>").append(NL);
            if (exception != null) {
                xml.append("<Exception>").append(exception).append(
                        "</Exception>").append(NL);
            } else {
                xml.append("<AllSessionsGivenUUID>").append(NL);
                Set sids = allSessionsforGivenUUID.keySet();
                for (Iterator m = sids.iterator(); m.hasNext();) {

                    String sid = (String) m.next();
                    xml.append("<SessionExpirationTimeInfo sid=").append(QUOTE)
                            .append(sid).append(QUOTE).append(" expTime=")
                            .append(QUOTE).append(
                                    ((Long) allSessionsforGivenUUID.get(sid))
                                            .toString()).append(QUOTE).append(
                                    '>').append("</SessionExpirationTimeInfo>");
                }
                xml.append("</AllSessionsGivenUUID>").append(NL);
            }
            xml.append("</GetSessionCount>").append(NL);
            break;
        default:
            return null;
        }
        xml.append("</SessionResponse>");
        return xml.toString();
    }
}
