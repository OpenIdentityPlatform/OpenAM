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
 * $Id: SessionResponseParser.java,v 1.3 2008/06/25 05:41:31 qcheng Exp $
 *
 */

package com.iplanet.dpro.session.share;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sun.identity.shared.xml.XMLUtils;
import com.iplanet.dpro.session.SessionException;

/**
 * <code>SessionResponseParser</code> parses the <code>SessionResponse</code>
 * XML document and returns the <code>SessionResponse</code> object
 *
 */
class SessionResponseParser {

    /**
     * <code>SessionResponse</code> object being returned after parsing the
     * XML document
     */
    private SessionResponse sessionResponse = null;

    /**
     * Document to be parsed
     */
    Document document;

    /**
     * Constructs new SessionResponseParser
     * @param XML response document to be parsed
     */
    public SessionResponseParser(Document xmlDoc) {
        document = xmlDoc;
    }

    /**
     * Constructs new SessionResponseParser
     * @param XML string representing the response
     * @exception when the SessionReponse object cannot be parsed
     */
    public SessionResponseParser(String xmlString) {
        document = XMLUtils
                .toDOMDocument(xmlString, SessionRequestParser.debug);
    }

    /**
     * Parses the session reponse element. Please see file
     * <code>SessionResponse.dtd</code> for the corresponding DTD of the
     * SessionResponse.
     * 
     * @return a <code>SessionResponse</code> object.
     */
    public SessionResponse parseXML() throws SessionException {
        if (document == null) {
            return null;
        }
        // get document element
        Element elem = document.getDocumentElement();
        sessionResponse = new SessionResponse();
        // set session response attribute
        String temp = elem.getAttribute("vers");
        sessionResponse.setResponseVersion(temp);
        // set session reqid
        temp = elem.getAttribute("reqid");
        sessionResponse.setRequestID(temp);

        // check GetSession element
        NodeList nodelist = elem.getElementsByTagName("GetSession");
        if (nodelist != null && nodelist.getLength() != 0) {
            sessionResponse.setMethodID(SessionRequest.GetSession);
        }

        // check GetActiveSessions element
        nodelist = elem.getElementsByTagName("GetActiveSessions");
        if (nodelist != null && nodelist.getLength() != 0) {
            sessionResponse.setMethodID(SessionRequest.GetValidSessions);
        }

        // check DestroySession element
        nodelist = elem.getElementsByTagName("DestroySession");
        if (nodelist != null && nodelist.getLength() != 0) {
            sessionResponse.setMethodID(SessionRequest.DestroySession);
        }

        // check Logout element
        nodelist = elem.getElementsByTagName("Logout");
        if (nodelist != null && nodelist.getLength() != 0) {
            sessionResponse.setMethodID(SessionRequest.Logout);
        }

        // check AddSessionListener element
        nodelist = elem.getElementsByTagName("AddSessionListener");
        if (nodelist != null && nodelist.getLength() != 0) {
            sessionResponse.setMethodID(SessionRequest.AddSessionListener);
        }

        // check AddSessionListenerOnAllSessions element
        nodelist = elem.getElementsByTagName("AddSessionListenerOnAllSessions");
        if (nodelist != null && nodelist.getLength() != 0) {
            sessionResponse.setMethodID(
                    SessionRequest.AddSessionListenerOnAllSessions);
        }

        // check SetProperty element
        nodelist = elem.getElementsByTagName("SetProperty");
        if (nodelist != null && nodelist.getLength() != 0) {
            sessionResponse.setMethodID(SessionRequest.SetProperty);
        }

        // check GetSessionCount element
        nodelist = elem.getElementsByTagName("GetSessionCount");
        if (nodelist != null && nodelist.getLength() != 0) {
            sessionResponse.setMethodID(SessionRequest.GetSessionCount);
        }

        // check COUNT element
        nodelist = elem.getElementsByTagName("SessionExpirationTimeInfo");
        if (nodelist != null && nodelist.getLength() != 0) {
            parseAllSessionsGivenUUIDElements(nodelist);
        }

        // check Session element
        nodelist = elem.getElementsByTagName("Session");
        if (nodelist != null && nodelist.getLength() != 0) {
            parseSessionElements(nodelist);
        }

        // check OK element
        nodelist = elem.getElementsByTagName("OK");
        if (nodelist != null && nodelist.getLength() != 0) {
            sessionResponse.setBooleanFlag(true);
        }

        // check Exception element
        nodelist = elem.getElementsByTagName("Exception");
        if (nodelist != null && nodelist.getLength() != 0) {
            Element exception = (Element) nodelist.item(0);
            sessionResponse.setException(SessionRequestParser
                    .parseCDATA(exception));
        }

        // check Status element
        nodelist = elem.getElementsByTagName("Status");
        if (nodelist != null && nodelist.getLength() != 0) {
            String status = SessionRequestParser.parseCDATA((Element) nodelist
                    .item(0));
            try {
                sessionResponse.setStatus(Integer.parseInt(status));
            } catch (NumberFormatException e) {
                SessionRequestParser.debug.error("SessionResponseParse : ", e);
                throw new SessionException(e.getMessage());
            }
        }

        // return session reponse
        return sessionResponse;
    }

   /*
    * Parses all the Sessions for the given user.
    *
    * @param nodelist.
    */
   private void parseAllSessionsGivenUUIDElements(NodeList nodelist) {
        // parse SessionExpirationTimeInfo one by one
        int len = nodelist.getLength();
        for (int i = 0; i < len; i++) {
            // get one SessionExpirationTimeInfo element
            Element sess = (Element) nodelist.item(i);
            // parse one SessionExpirationTimeInfo element
            parseSessionExpirationTimeInfo(sess);
        }
    }

    
   /**
    * Parses all the Sessions Expiration for the given user.
    *
    * @param sess session element.
    */
   private void parseSessionExpirationTimeInfo(Element sess) {
        String sid = null;
        Long expTime = null;

        // parse the attributes
        String temp = sess.getAttribute("sid");
        if (temp != null) {
            sid = temp;
        }
        temp = sess.getAttribute("expTime");
        if (temp != null) {
            expTime = new Long(temp);
        }

        // add to sessionResponse
        sessionResponse.addSessionForGivenUUID(sid, expTime);
    }

    /**
     * Parse Session Elements.
     * 
     * @param nodelist NodeList of Session element.
     */
    private void parseSessionElements(NodeList nodelist) {
        // parse session one by one
        int len = nodelist.getLength();
        for (int i = 0; i < len; i++) {
            // get one Session element
            Element sess = (Element) nodelist.item(i);
            // parse one Session element
            SessionInfo sessionInfo = parseSessionElement(sess);
            // add to sessionResponse
            sessionResponse.addSessionInfo(sessionInfo);
            SessionRequestParser.debug.message("In parse session "
                    + sessionInfo.toString());
        }
    }

    /**
     * Parse one Session Element, it contains two attributes and properties.
     * 
     * @param sess Session Element.
     * @return SessionInfo
     */
    static SessionInfo parseSessionElement(Element sess) {
        SessionInfo sessionInfo = new SessionInfo();
        // parse Session attributes
        String temp = sess.getAttribute("sid");
        if (temp != null) {
            sessionInfo.sid = temp;
        }
        temp = sess.getAttribute("stype");
        if (temp != null) {
            sessionInfo.stype = temp;
        }
        temp = sess.getAttribute("cid");
        if (temp != null) {
            sessionInfo.cid = temp;
        }
        temp = sess.getAttribute("cdomain");
        if (temp != null) {
            sessionInfo.cdomain = temp;
        }
        temp = sess.getAttribute("maxtime");
        if (temp != null) {
            sessionInfo.maxtime = temp;
        }
        temp = sess.getAttribute("maxidle");
        if (temp != null) {
            sessionInfo.maxidle = temp;
        }
        temp = sess.getAttribute("maxcaching");
        if (temp != null) {
            sessionInfo.maxcaching = temp;
        }
        temp = sess.getAttribute("timeleft");
        if (temp != null) {
            sessionInfo.timeleft = temp;
        }
        temp = sess.getAttribute("timeidle");
        if (temp != null) {
            sessionInfo.timeidle = temp;
        }
        temp = sess.getAttribute("state");
        if (temp != null) {
            sessionInfo.state = temp;
        }

        // parse session properties
        NodeList properties = sess.getElementsByTagName("Property");
        if (properties != null) {
            // parse all properties
            int p = properties.getLength();
            for (int j = 0; j < p; j++) {
                // get Property element
                Element property = (Element) properties.item(j);
                // get property attributes
                String name = property.getAttribute("name");
                if (name != null) {
                    sessionInfo.properties.put(name, property
                            .getAttribute("value"));
                }
            }
        }

        return sessionInfo;
    }
}
