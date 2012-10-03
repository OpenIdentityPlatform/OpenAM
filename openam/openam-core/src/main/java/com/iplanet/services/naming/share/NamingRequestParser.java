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
 * $Id: NamingRequestParser.java,v 1.4 2008/06/25 05:41:40 qcheng Exp $
 *
 */

package com.iplanet.services.naming.share;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;

class NamingRequestParser {

    /**
     * Document to be parsed
     */
    private Document document;

    static Debug debug = Debug.getInstance("amNaming");

    /**
     * Class constructor with <code>NamingRequest</code>
     * in <code>Document</code> format.
     * @param xmlDoc A DOM representing <code>NamingRequest</code> class.
     */
    public NamingRequestParser(Document xmlDoc) {
        document = xmlDoc;
    }

    /**
     * Class constructor with <code>NamingRequest</code>
     * in <code>String</code> format.
     * @param xmlString A string representing <code>NamingRequest</code> class.
     */
    public NamingRequestParser(String xmlString) {
        document = XMLUtils.toDOMDocument(xmlString, debug);
    }

    /**
     * Parses the naming request xml document. Please see file NamingRequest.dtd
     * for the corresponding DTD of the request.
     * 
     * @return a NamingRequest object.
     */
    public NamingRequest parseXML() {
        if (document == null) {
            return null;
        }

        // get naming request element
        Element elem = document.getDocumentElement();
        NamingRequest namingRequest = new NamingRequest();

        // set naming request attributes
        String temp = elem.getAttribute("vers");
        if (temp != null) {
            namingRequest.setRequestVersion(temp);
        }
        temp = elem.getAttribute("reqid");
        if (temp != null) {
            namingRequest.setRequestID(temp);
        }
        temp = elem.getAttribute("sessid");
        if ((temp != null) && ((temp.trim()).length() != 0)) {
            namingRequest.setSessionId(temp);
        } else {
            namingRequest.setSessionId(null);
        }

        temp = elem.getAttribute("preferredNamingURL");
        if ((temp != null) &&  ((temp.trim()).length() != 0)) {
            namingRequest.setPreferredNamingURL(temp);
        } else {
            namingRequest.setPreferredNamingURL(null);
        }

        return namingRequest;
    }
}
