/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: WSDLParser.java,v 1.1 2009/08/19 05:40:34 veiming Exp $
 */
/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.entitlement;

import com.sun.identity.shared.xml.XMLUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class WSDLParser {
    private static final String ELT_NAME_BINDING = "binding";
    private static final String ELT_NAME_SERVICE = "service";
    private static final String ELT_NAME_OPERATION = "operation";
    private static final String ELT_NAME_PORT = "port";
    private static final String ELT_NAME_SOAP_ADDRESS = "soap:address";
    private static final String ATTR_NAME_OPERATION = "name";
    private static final String ATTR_NAME_LOCATION = "location";

    private Set<String> operationNames = new HashSet<String>();
    private Set<String> resources = new HashSet<String>();

    private void clear() {
        operationNames.clear();
        resources.clear();
    }

    /**
     * Returns operation names.
     *
     * @return operation names.
     */
    public Set<String> getOperationNames() {
        return operationNames;
    }

    /**
     * Returns resources.
     *
     * @return resources
     */
    public Set<String> getResources() {
        return resources;
    }

    public void parse(String uri) throws EntitlementException {
        clear();
        Document root = parseXML(uri);
        parse(root);
    }

    public void parse(File file) throws EntitlementException {
        clear();
        Document root = parseXML(file);
        parse(root);
    }

    public void parse(InputStream is) throws EntitlementException {
        clear();
        Document root = parseXML(is);
        parse(root);
    }

    private void parse(Document root) {
        Element elm = root.getDocumentElement();
        NodeList childElt = elm.getChildNodes();
        int numChildElements = childElt.getLength();

        for (int i = 0; i < numChildElements; i++) {
            Node node = childElt.item(i);
            if ((node != null) &&  (node.getNodeType() == Node.ELEMENT_NODE)) {
                String elementName = node.getNodeName();
                if (elementName.equals(ELT_NAME_BINDING)) {
                    processBindingElement(node);
                } else if (elementName.equals(ELT_NAME_SERVICE)) {
                    processServiceElement(node);
                }
            }
        }
    }

    private DocumentBuilder getDocumentBuilder()
        throws EntitlementException {
        try {
            DocumentBuilder builder = XMLUtils.getSafeDocumentBuilder(false);
            builder.setErrorHandler(new ValidationErrorHandler());
            return builder;
        } catch (ParserConfigurationException ex) {
            throw new EntitlementException(7, ex);
        }
    }

    private Document parseXML(File file) throws EntitlementException {
        DocumentBuilder builder = getDocumentBuilder();
        try {
            return builder.parse(file);
        } catch (SAXException ex) {
            throw new EntitlementException(7, ex);
        } catch (IOException ex) {
            throw new EntitlementException(7, ex);
        }
    }

    private Document parseXML(String uri) throws EntitlementException {
        DocumentBuilder builder = getDocumentBuilder();
        try {
            return builder.parse(uri);
        } catch (SAXException ex) {
            throw new EntitlementException(7, ex);
        } catch (IOException ex) {
            throw new EntitlementException(7, ex);
        }
    }

    private Document parseXML(InputStream is) throws EntitlementException {
        DocumentBuilder builder = getDocumentBuilder();
        try {
            return builder.parse(is);
        } catch (SAXException ex) {
            throw new EntitlementException(7, ex);
        } catch (IOException ex) {
            throw new EntitlementException(7, ex);
        }
    }

    private void processBindingElement(Node parentNode) {
        NodeList childElt = parentNode.getChildNodes();
        int numChildElements = childElt.getLength();

        for (int i = 0; i < numChildElements; i++) {
            Node node = childElt.item(i);
            if ((node != null) &&  (node.getNodeType() == Node.ELEMENT_NODE)) {
                String elementName = node.getNodeName();
                if (elementName.equals(ELT_NAME_OPERATION)) {
                    String op = ((Element)node).getAttribute(
                        ATTR_NAME_OPERATION);
                    if ((op != null) && (op.length() > 0)) {
                        operationNames.add(op);
                    }
                }
            }
        }
    }

    private void processServiceElement(Node parentNode) {
        NodeList childElt = parentNode.getChildNodes();
        int numChildElements = childElt.getLength();

        for (int i = 0; i < numChildElements; i++) {
            Node node = childElt.item(i);
            if ((node != null) &&  (node.getNodeType() == Node.ELEMENT_NODE)) {
                String elementName = node.getNodeName();
                if (elementName.equals(ELT_NAME_PORT)) {
                    processServicePortElement(node);
                }
            }
        }
    }

    private void processServicePortElement(Node parentNode) {
        NodeList childElt = parentNode.getChildNodes();
        int numChildElements = childElt.getLength();

        for (int i = 0; i < numChildElements; i++) {
            Node node = childElt.item(i);
            if ((node != null) &&  (node.getNodeType() == Node.ELEMENT_NODE)) {
                String elementName = node.getNodeName();
                if (elementName.equals(ELT_NAME_SOAP_ADDRESS)) {
                    String url = ((Element)node).getAttribute(
                        ATTR_NAME_LOCATION);
                    if ((url != null) && (url.length() > 0)) {
                        resources.add(url);
                    }
                }
            }
        }
    }

    /**
     *Inner class to take care of XML validation errors
     */
    class ValidationErrorHandler implements ErrorHandler {
        // ignore fatal errors(an exception is guaranteed)
        public void fatalError(SAXParseException spe)
            throws SAXParseException
        {
            PrivilegeManager.debug.error("WSDLParser.fatalError", spe);
        }

        //treat validation errors also as fatal error
        public void error(SAXParseException spe)
            throws SAXParseException
        {
            PrivilegeManager.debug.error("WSDLParser.error", spe);
            throw spe;
        }

        // dump warnings too
        public void warning(SAXParseException err)
            throws SAXParseException
        {
            PrivilegeManager.debug.warning("WSDLParser.warning", err);
        }
    }
}
