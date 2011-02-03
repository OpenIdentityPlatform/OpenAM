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
 * $Id: SessionRequestParser.java,v 1.3 2008/06/25 05:41:31 qcheng Exp $
 *
 */

package com.iplanet.dpro.session.share;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * <code>SessionRequestParser</code> parses the <code>SessionRequest</code>
 * XML document and returns the <code>SessionRequest</code> object
 */
class SessionRequestParser {
    /**
     * <code>SessionRequest</code> object being returned after parsing the XML
     * document.
     */
    private SessionRequest sessionRequest = null;

    /**
     * Document to be parsed
     */
    Document document;

    static Debug debug = Debug.getInstance("amSession");

   /**
    * Constructs <code>SessionRequestParser</code>.
    *
    * @param xmlDoc  XML document to be parsed
    */
   public SessionRequestParser(Document xmlDoc) {
        document = xmlDoc;
    }

   /**
    * Constructs <code>SessionRequestParser</code>.
    *
    * @param xmlString An XML String representing the request.
    */
   public SessionRequestParser(String xmlString) {
        document = XMLUtils.toDOMDocument(xmlString, debug);
    }

    /**
     * Parses the session request document. Please see file
     * <code>SessionRequest.dtd</code> for the corresponding DTD of the
     * <code>SessionRequest</code>.
     * 
     * @return a <code>SessionRequest</code> object.
     */
    public SessionRequest parseXML() {
        if (document == null) {
            return null;
        }

        sessionRequest = new SessionRequest();
        // get document element
        Element elem = document.getDocumentElement();
        // set Session Request attributes
        String temp = elem.getAttribute("vers");
        if (temp != null) {
            sessionRequest.setRequestVersion(temp);
        }
        temp = elem.getAttribute("reqid");
        if (temp != null) {
            sessionRequest.setRequestID(temp);
        }

        // set "requester" attribute
        String data = elem.getAttribute("requester");
        if (data != null && data.length() > 0) {
            try {
                sessionRequest.setRequester(new String(Base64.decode(data),
                        "UTF-8"));
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Illegal requester attribute value=" + data);
            }
        }

        // check GetSession
        NodeList nodelist = elem.getElementsByTagName("GetSession");
        if (nodelist != null && nodelist.getLength() != 0) {
            sessionRequest.setMethodID(SessionRequest.GetSession);
            parseGetSessionAttributes((Element) nodelist.item(0));
        }

        // check GetValidSessions
        nodelist = elem.getElementsByTagName("GetValidSessions");
        if (nodelist != null && nodelist.getLength() != 0) {
            sessionRequest.setMethodID(SessionRequest.GetValidSessions);
        }

        // check DestroySession
        nodelist = elem.getElementsByTagName("DestroySession");
        if (nodelist != null && nodelist.getLength() != 0) {
            sessionRequest.setMethodID(SessionRequest.DestroySession);
        }

        // check Logout
        nodelist = elem.getElementsByTagName("Logout");
        if (nodelist != null && nodelist.getLength() != 0) {
            sessionRequest.setMethodID(SessionRequest.Logout);
        }

        // check AddSessionListener
        nodelist = elem.getElementsByTagName("AddSessionListener");
        if (nodelist != null && nodelist.getLength() != 0) {
            sessionRequest.setMethodID(SessionRequest.AddSessionListener);
        }

        // check AddSessionListenerOnAllSessions
        nodelist = elem.getElementsByTagName("AddSessionListenerOnAllSessions");
        if (nodelist != null && nodelist.getLength() != 0) {
            sessionRequest.setMethodID(
                    SessionRequest.AddSessionListenerOnAllSessions);
        }

        // check SetProperty
        nodelist = elem.getElementsByTagName("SetProperty");
        if (nodelist != null && nodelist.getLength() != 0) {
            sessionRequest.setMethodID(SessionRequest.SetProperty);
        }

        // check SessionID
        nodelist = elem.getElementsByTagName("SessionID");
        if (nodelist != null && nodelist.getLength() != 0) {
            sessionRequest.setSessionID(parseCDATA((Element) nodelist.item(0)));
        }

        // check DestroySessionID
        nodelist = elem.getElementsByTagName("DestroySessionID");
        if (nodelist != null && nodelist.getLength() != 0) {
            sessionRequest.setDestroySessionID(parseCDATA((Element) nodelist
                    .item(0)));
        }

        // check URL
        nodelist = elem.getElementsByTagName("URL");
        if (nodelist != null && nodelist.getLength() != 0) {
            sessionRequest.setNotificationURL(parseCDATA((Element) nodelist
                    .item(0)));
        }

        // check Property
        nodelist = elem.getElementsByTagName("Property");
        if (nodelist != null && nodelist.getLength() != 0) {
            parsePropertyAttributes((Element) nodelist.item(0));
        }

        // check Pattern
        nodelist = elem.getElementsByTagName("Pattern");
        if (nodelist != null && nodelist.getLength() != 0) {
            sessionRequest.setPattern(parseCDATA((Element) nodelist.item(0)));
        }

        // check SessionCount
        nodelist = elem.getElementsByTagName("GetSessionCount");
        if (nodelist != null && nodelist.getLength() != 0) {
            sessionRequest.setMethodID(SessionRequest.GetSessionCount);
        }

        // check UUID
        nodelist = elem.getElementsByTagName("UUID");
        if (nodelist != null && nodelist.getLength() != 0) {
            sessionRequest.setUUID(parseCDATA((Element) nodelist.item(0)));
        }

        return sessionRequest;
    }

    /**
     * parse property name & value
     */
    private void parsePropertyAttributes(Element property) {
        if (property == null) {
            return;
        }
        // set property name & value
        String temp = property.getAttribute("name");
        if (temp != null) {
            sessionRequest.setPropertyName(temp);
        }
        temp = property.getAttribute("value");
        if (temp != null) {
            sessionRequest.setPropertyValue(temp);
        }
    }

    /**
     * Parse element, and return CDATA as String
     */
    static String parseCDATA(Element elem) {
        if (elem == null) {
            return null;
        }
        Node text = elem.getFirstChild();
        if (text != null) {
            return text.getNodeValue();
        }
        return null;
    }

    /**
     * Parse GetSession attribute
     */
    private void parseGetSessionAttributes(Element elem) {
        if (elem == null) {
            return;
        }
        // set "reset" attribute
        String temp = elem.getAttribute("reset");
        if (temp != null) {
            if (temp.equals("true")) {
                sessionRequest.setResetFlag(true);
            } else {
                sessionRequest.setResetFlag(false);
            }
        }
    }
}
