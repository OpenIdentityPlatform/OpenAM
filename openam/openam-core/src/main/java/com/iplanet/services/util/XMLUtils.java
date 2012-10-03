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
 * $Id: XMLUtils.java,v 1.3 2008/06/25 05:41:41 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.util;

import com.iplanet.ums.IUMSConstants;
import com.sun.identity.shared.debug.Debug;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * This class contains utilities to parse XML documents
 * 
 * @deprecated use <code>com.iplanet.am.util.XMLUtils</code>
 */
public class XMLUtils {

    protected static I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);

    private static DocumentBuilderFactory dbFactory = null;

    /**
     * Converts the XML document from a String format to DOM Document format.
     * 
     * @param xmlString
     *            is the XML document in a String format
     * @param debug
     *            is the debug object used for logging debug info
     * @return Document is the DOM object obtained by converting the String XML
     *         Returns null if xmlString is null. Returns null if there are any
     *         parser errores.
     */
    public static Document toDOMDocument(String xmlString, Debug debug) {
        try {
            if (dbFactory == null) {
                // first time to initialize the doc builder
                synchronized (XMLUtils.class) {
                    if (dbFactory == null) {
                        try {
                            dbFactory = DocumentBuilderFactory.newInstance();
                            dbFactory.setValidating(false);
                            dbFactory.setNamespaceAware(true);
                        } catch (Exception e) {
                            if (debug != null) {
                                debug.error(
                                        "XMLUtils.DocumentBuilder init failed",
                                        e);
                            }
                        }
                    }
                }
            }

            if (xmlString == null) {
                return null;
            }

            DocumentBuilder documentBuilder = null;

            // new a document builder from the factory
            synchronized (dbFactory) {
                documentBuilder = dbFactory.newDocumentBuilder();
            }

            if (documentBuilder == null) {
                if (debug != null) {
                    debug.error("XMLUtils.toDOM : null builder instance");
                }
                return null;
            }

            ByteArrayInputStream is = new ByteArrayInputStream(xmlString
                    .getBytes("UTF-8"));
            return documentBuilder.parse(is);
        } catch (Exception e) {
            // Since there may potentially be several invalid XML documents
            // that are mostly client-side errors, only a warning is logged for
            // efficiency reasons.
            if (debug != null && debug.warningEnabled()) {
                debug.warning("Can't parse the XML document:\n" + xmlString, e);
            }
            return null;
        }
    }

    public static Document getXMLDocument(String inputStr) throws XMLException,
            UnsupportedEncodingException {
        int idx = inputStr.indexOf("encoding=");
        String encoding = "UTF-8";
        if (idx != -1) {
            int idx1 = inputStr.indexOf('"', idx);
            int idx2 = (idx1 == -1) ? idx1 : inputStr.indexOf('"', idx1 + 1);
            if (idx1 != -1 && idx2 != -1) {
                encoding = inputStr.substring(idx1 + 1, idx2);
            }
        }
        byte[] barr;
        barr = inputStr.getBytes(encoding);
        return getXMLDocument(new ByteArrayInputStream(barr));
    }

    public static Document getXMLDocument(InputStream in) throws XMLException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory
                    .newInstance();
            factory.setValidating(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(in);
            return (doc);
        } catch (SAXParseException pe) {
            String msg = "\n" + pe.getMessage() + "\n";
            Object params[] = { new Integer(pe.getLineNumber()) };
            throw (new XMLException(msg
                    + i18n.getString("XMLUtils.parser_error", params)));
        } catch (SAXException sax) {
            Object params[] = { sax.getMessage()};
            throw (new XMLException(i18n.getString(
                    "XMLUtils.exception_message", params)));
        } catch (ParserConfigurationException pc) {
            Object params[] = { pc.getMessage()};
            throw (new XMLException(i18n.getString(
                    "XMLUtils.invalid_xml_document", params)));
        } catch (IOException ioe) {
            Object params[] = { ioe.getMessage()};
            throw (new XMLException(i18n.getString(
                    "XMLUtils.invalid_input_stream", params)));
        }
    }

    public static Node getRootNode(Document doc, String nodeName) {
        NodeList nodes = doc.getElementsByTagName(nodeName);
        if (nodes == null || nodes.getLength() == 0)
            return (null);
        return (nodes.item(0));
    }

    public static Node getChildNode(Node parentNode, String childName) {
        NodeList children = parentNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeName().equalsIgnoreCase(childName))
                return (node);
        }
        return (null);
    }

    /**
     * Returns a child node that has the given node name and give attribute name
     * and value.
     */
    public static Node getNamedChildNode(Node parentNode, String childNodeName,
            String attrName, String attrValue) {
        NodeList children = parentNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (childNodeName.equalsIgnoreCase(node.getNodeName())) {
                if (getNodeAttributeValue(node, attrName).equalsIgnoreCase(
                        attrValue))
                    return (node);
            }
        }
        return (null);
    }

    public static Set getChildNodes(Node parentNode, String childName) {
        Set retVal = new HashSet();
        NodeList children = parentNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeName().equalsIgnoreCase(childName)) {
                retVal.add(node);
            }
        }
        return (retVal);
    }

    public static String getNodeAttributeValue(Node node, String attrName) {
        NamedNodeMap attrs = node.getAttributes();
        if (attrs == null)
            return (null);
        Node value = attrs.getNamedItem(attrName);
        if (value == null)
            return (null);
        return (value.getNodeValue());
    }

    /**
     * Method to get Values within AttributeValuePair as a java.util.Set
     */
    public static Set getAttributeValuePair(Node node) {
        if (!node.getNodeName().equals(ATTR_VALUE_PAIR_NODE))
            return (null);

        Set retVal = new HashSet();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeName().equalsIgnoreCase(VALUE_NODE)) {
                retVal.add(getValueOfValueNode(n));
            }
        }
        return (retVal);
    }

    /**
     * Method to get the value of "Value" node
     */
    public static String getValueOfValueNode(Node n) {
        NodeList textNodes = n.getChildNodes();
        Node textNode;
        StringBuilder value = new StringBuilder();
        for (int j = 0; j < textNodes.getLength(); j++) {
            textNode = textNodes.item(j);
            value.append(textNode.getNodeValue());
        }
        return (value.toString().trim());
    }

    /**
     * Replaces XML special character '&', '<', '>', '"', ''' with
     * corresponding entity references
     * 
     * @return String with the special characters replaced with entity
     *         references
     */
    public static String escapeSpecialCharacters(String text) {

        String escapedText = text;
        StringBuffer sb = null;
        int len = 0;
        if (text != null) {
            len = text.length();
        }

        int i = 0;
        boolean specialCharacterFound = false;
        for (; i < len; i++) {
            char c = text.charAt(i);
            if (c == '&' || c == '<' || c == '>' || c == '\'' || c == '\"'
                    || c == '\n') {
                specialCharacterFound = true;
                break;
            }
        }
        if (specialCharacterFound) {
            sb = new StringBuffer();
            sb.append(text.substring(0, i));
            for (; i < len; i++) {
                char c = text.charAt(i);
                switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '\"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&apos;");
                    break;
                case '\n':
                    sb.append("&#xD;");
                    break;
                default:
                    sb.append(c);
                }
            }
            escapedText = sb.toString();
        }

        return escapedText;
    }

    private static String ATTR_VALUE_PAIR_NODE = "AttributeValuePair";

    private static String VALUE_NODE = "Value";
}
