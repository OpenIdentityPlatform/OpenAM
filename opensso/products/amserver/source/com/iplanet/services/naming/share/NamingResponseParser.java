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
 * $Id: NamingResponseParser.java,v 1.3 2008/06/25 05:41:40 qcheng Exp $
 *
 */

package com.iplanet.services.naming.share;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.shared.xml.XMLUtils;

class NamingResponseParser {

    /**
     * NamingResponse object being returned after parsing the XML document
     */
    private NamingResponse namingResponse = null;

    /**
     * Document to be parsed
     */
    Document document;

    public NamingResponseParser(Document xmlDoc) {
        document = xmlDoc;
    }

    public NamingResponseParser(String xmlString) {
        document = XMLUtils.toDOMDocument(xmlString, NamingRequestParser.debug);
    }

    /**
     * Parses the response document. Please see file NamingResponse.dtd for the
     * corresponding DTD of the NamingResponse.
     * 
     * @return a NamingResponse object.
     */
    public NamingResponse parseXML() {
        if (document == null) {
            return null;
        }

        // get NamingResponse element
        Element elem = document.getDocumentElement();
        namingResponse = new NamingResponse();

        // set naming response attributes
        String temp = elem.getAttribute("vers");
        if (temp != null) {
            namingResponse.setResponseVersion(temp);
        }
        temp = elem.getAttribute("reqid");
        if (temp != null) {
            namingResponse.setRequestID(temp);
        }

        // get attribute element
        NodeList attribs = elem.getElementsByTagName("Attribute");
        if (attribs != null && attribs.getLength() != 0) {
            parseAttributeTag(attribs);
        }

        // get exception element
        NodeList exception = elem.getElementsByTagName("Exception");
        if (exception != null && exception.getLength() != 0) {
            Node node = exception.item(0);
            if (node != null) {
                namingResponse.setException(node.getNodeValue());
            }
        }

        return namingResponse;
    }

    /**
     * This method is an internal method used by parseXML method to parse
     * Attribute.
     *
     * @param attributes XML Node for attributes.
     */
    public void parseAttributeTag(NodeList attributes) {
        int len = attributes.getLength();
        for (int i = 0; i < len; i++) {
            Element tempElem = (Element) attributes.item(i);
            // get node name & value
            String name = tempElem.getAttribute("name");
            if (name != null) {
                String value = tempElem.getAttribute("value");
                namingResponse.setAttribute(name, value);
            }
        }
    }
}
