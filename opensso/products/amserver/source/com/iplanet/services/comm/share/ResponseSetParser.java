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
 * $Id: ResponseSetParser.java,v 1.3 2008/06/25 05:41:35 qcheng Exp $
 *
 */

package com.iplanet.services.comm.share;

import com.sun.identity.shared.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class ResponseSetParser {

    /**
     * Document being parsed
     */
    Document document;

    public ResponseSetParser(Document xmlDoc) {
        document = xmlDoc;
    }

    public ResponseSetParser(String xmlString) {
        document = XMLUtils.toDOMDocument(xmlString, RequestSetParser.debug);
    }

    /**
     * Parses the tree from root element. Please see ResponseSet.java for the
     * corresponding DTD of the ResponseSet.
     * 
     * @return a ResponseSet object.
     */
    public ResponseSet parseXML() {
        if (document == null) {
            return null;
        }

        // get ResponseSet element
        Element responseSetElem = document.getDocumentElement();
        ResponseSet responseSet = new ResponseSet();
        // set response set attributes
        setResponseSetAttributes(responseSetElem, responseSet);

        // get Responses
        NodeList responses = responseSetElem.getElementsByTagName("Response");
        if (responses == null) {
            return responseSet;
        }

        // go through each response, and add them to the response set
        int nodeLen = responses.getLength();
        for (int i = 0; i < nodeLen; i++) {
            responseSet.addResponse(parseResponseElement((Element) responses
                    .item(i)));
        }

        return responseSet;
    }

    /**
     * This method is an internal method used by parseXML method.
     *
     * @param elem XML element object.
     * @param responseSet Response Set.
     */
    public void setResponseSetAttributes(Element elem, ResponseSet responseSet) 
    {
        // get vers attribute
        String temp = elem.getAttribute("vers");
        if (temp != null) {
            responseSet.setResponseSetVersion(temp);
        }

        // get service id
        temp = elem.getAttribute("svcid");
        if (temp != null) {
            responseSet.setServiceID(temp);
        }

        // get request id
        temp = elem.getAttribute("reqid");
        if (temp != null) {
            responseSet.setRequestSetID(temp);
        }
    }

    /**
     * function to parse a single response element. Response contain a text
     * element and dtdid attribute
     */
    private Response parseResponseElement(Element elem) {
        Response response = new Response();
        // process request attributes
        String temp = elem.getAttribute("dtdid");
        if (temp != null) {
            response.setDtdID(temp);
        }

        // process TEXT child element
        Node text = elem.getFirstChild();
        if (text != null) {
            response.setContent(text.getNodeValue());
        }
        return response;
    }
}
