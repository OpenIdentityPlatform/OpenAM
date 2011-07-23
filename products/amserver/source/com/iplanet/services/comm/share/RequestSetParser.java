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
 * $Id: RequestSetParser.java,v 1.3 2008/06/25 05:41:35 qcheng Exp $
 *
 */

package com.iplanet.services.comm.share;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class RequestSetParser {
    /**
     * Document
     */
    private Document document;

    /**
     * Debug instance
     */
    protected static Debug debug = Debug.getInstance("amComm");

    public RequestSetParser(Document xmlDoc) {
        document = xmlDoc;
    }

    public RequestSetParser(String xmlString) {
        document = XMLUtils.toDOMDocument(xmlString, debug);
    }

    /**
     * Parses the tree from root element. Please see RequestSet.java for the
     * corresponding DTD of the RequestSet.
     * 
     * @return a RequestSet object.
     */
    public RequestSet parseXML() {
        if (document == null) {
            return null;
        }
        // get request set element
        Element reqSetElem = document.getDocumentElement();
        RequestSet reqSet = new RequestSet();

        // set request set attributes
        setRequestSetAttributes(reqSetElem, reqSet);

        // get all requests
        NodeList requests = reqSetElem.getElementsByTagName("Request");
        if (requests == null) {
            return reqSet;
        }

        // go through each request, and add them to the request set
        int nodeLen = requests.getLength();
        for (int i = 0; i < nodeLen; i++) {
            reqSet.addRequest(parseRequestElement((Element) requests.item(i)));
        }

        return reqSet;
    }

    /**
     * This method is an internal method used by parseXML method.
     */
    private void setRequestSetAttributes(Element elem, RequestSet requestSet) {
        // get vers attribute
        String temp = elem.getAttribute("vers");
        if (temp != null) {
            requestSet.setRequestSetVersion(temp);
        }

        // get service id
        temp = elem.getAttribute("svcid");
        if (temp != null) {
            requestSet.setServiceID(temp);
        }

        // get request id
        temp = elem.getAttribute("reqid");
        if (temp != null) {
            requestSet.setRequestSetID(temp);
        }
    }

    /**
     * function to parse a single request element. Request contain a text
     * element and several attributes
     */
    private Request parseRequestElement(Element elem) {
        Request req = new Request();
        // process request attributes
        String temp = elem.getAttribute("dtdid");
        if (temp != null) {
            req.setDtdID(temp);
        }
        temp = elem.getAttribute("sid");
        if (temp != null) {
            req.setSessionID(temp);
        }

        // process TEXT child element
        Node text = elem.getFirstChild();
        if (text != null) {
            req.setContent(text.getNodeValue());
        }
        return req;
    }
}
