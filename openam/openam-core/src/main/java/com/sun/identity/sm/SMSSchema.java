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
 * $Id: SMSSchema.java,v 1.11 2009/06/05 19:25:27 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2012 ForgeRock Inc
 */
package com.sun.identity.sm;

import com.iplanet.ums.IUMSConstants;
import com.sun.identity.shared.xml.XMLUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/**
 * This class provides methods to obtain service schema and plugin schema from
 * XML documents.
 */
public class SMSSchema {
    private static final String TEXT_NODE = "#text";

    private static final String SLASH_ESC_SEQ = "&#47;";

    static final String XML_ENC = 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

    static final String XML_ENC_UTF8 = "encoding=\"utf-8\"";

    private static final String SCHEMA_PREFIX_1 = XML_ENC +
        "\n<ServicesConfiguration><Service name=\"";

    private static final String SCHEMA_PREFIX_2 = "\" version=\"";

    private static final String SCHEMA_PREFIX_3 = "\">";

    private static final String SCHEMA_SUFFIX = 
        "</Service></ServicesConfiguration>";

    private static final String PLUGIN_PREFIX_1 = XML_ENC +
         "\n<ServicesConfiguration><Service name=\"";

    private static final String PLUGIN_PREFIX_2 = "\" version=\"";

    private static final String PLUGIN_PREFIX_3 = "\">";

    private static final String PLUGIN_SUFFIX =
        "</Service></ServicesConfiguration>";

    // Pointer to the complete document
    private Document document;

    private String serviceName;

    private String version;


    /**
     * Constructor to instantiate SMSSchema with a DOM's Document. Using this
     * constructor would use the first <code>Service
     * </code> element.
     * 
     * @param document
     *            service schema in XML DOM
     * @throws SMSException
     */
    SMSSchema(Document document) throws SMSException {
        this.document = document;
        serviceName = getServiceName();
        version = getServiceVersion();
    }

    /**
     * Constructor to instantiate SMSSchema with Document
     * 
     * @param serviceName
     *            name of the service
     * @param document
     *            service schema in XML DOM
     * @throws SMSException
     */
    SMSSchema(String serviceName, Document document) throws SMSException {
        this(document);
        this.serviceName = serviceName;
    }

    SMSSchema(String serviceName, String version, Document document)
            throws SMSException {
        this(serviceName, document);
        this.version = version;
    }

    /**
     * Constructor to instantiate SMSSchema with XML input stream.
     * 
     * @param serviceName
     *            name of the service
     * @param in
     *            service schema xml
     * @throws SMSException
     */
    SMSSchema(String serviceName, InputStream in) throws SMSException {
        this(in);
        this.serviceName = serviceName;
    }

    /**
     * Constructor to instantiate SMSSchema with XML input stream. Using this
     * constructor would use the first <code>Service
     * </code> element.
     * 
     * @param in
     *            service schema xml
     * @throws SMSException
     */
    SMSSchema(InputStream in) throws SMSException {
        this(getXMLDocument(in));
    }

    protected String getServiceName() throws SMSException {
        if (serviceName == null) {
            serviceName = getServiceAttribute(SMSUtils.NAME);
        }
        return (serviceName);
    }

    protected String getServiceVersion() throws SMSException {
        if (version == null) {
            version = getServiceAttribute(SMSUtils.VERSION);
        }
        return (version);
    }

    private String getServiceAttribute(String attrName) throws SMSException {
        // Get the first service element
        NodeList nodes = document.getElementsByTagName(SMSUtils.SERVICE);
        if (nodes == null || nodes.getLength() == 0) {
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    IUMSConstants.SMS_SMSSchema_no_service_element, null));
        }

        // Get the requested attribute value
        String value = XMLUtils.getNodeAttributeValue(nodes.item(0), attrName);
        if (value == null) {
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    IUMSConstants.SMS_SMSSchema_no_service_element, null));
        }
        return (value);
    }

    protected Node getServiceNode() throws SMSException {
        NodeList nodes = document.getElementsByTagName(SMSUtils.SERVICE);
        if (nodes == null || nodes.getLength() == 0) {
            SMSEntry.debug.error("SMSSchema::getSchema: "
                    + "No Service node not found in XML input.");
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    IUMSConstants.SMS_SMSSchema_no_service_element, null));
        }

        // Get the required service node
        Node parent = null;
        boolean foundNode = false;
        int nodeLen = nodes.getLength();
        for (int i = 0; i < nodeLen; i++) {
            parent = nodes.item(i);
            if (XMLUtils.getNodeAttributeValue(parent, SMSUtils.NAME)
                    .equalsIgnoreCase(serviceName)
                    && XMLUtils.getNodeAttributeValue(parent, SMSUtils.VERSION)
                            .equalsIgnoreCase(version)) {
                foundNode = true;
                break;
            }
        }

        if (!foundNode) {
            SMSEntry.debug.error("SMSSchema::getSchema: "
                    + "Service not found :" + serviceName);
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    IUMSConstants.SMS_SMSSchema_no_service_element, null));
        }
        return (parent);
    }

    /**
     * Method returns the XML schema for the given service name in a String
     * format that can stored in a directory.
     * 
     * @param serviceName
     *            name of the service
     * @return service schema in serialized String format
     * @throws SMSException
     */
    protected String getSchema(String serviceName) throws SMSException {
        String oldServiceName = this.serviceName;
        this.serviceName = serviceName;
        String schema = getSchema();
        this.serviceName = oldServiceName;
        return (schema);
    }

    /**
     * Method returns the XML schema for the service in a String format that can
     * stored in a directory.
     * 
     * @return service schema in serialized String format
     * @throws SMSException
     */
    public String getSchema() throws SMSException {
        Node node = null;
        // Get Service Schema node
        if ((node = XMLUtils.getChildNode(getServiceNode(), SMSUtils.SCHEMA)) 
                == null) 
        {
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    "IUMSConstants.SMS_SMSSchema_no_service_element", null));
        }
        return (schemaToXML(node, getServiceName(), getServiceVersion()));
    }

    /**
     * Method returns the XML plugin schema for the given service name and
     * plugin name in a String format that can stored in a directory.
     * 
     * @param serviceName
     *            name of the service
     * @param pluginName
     *            name of the plugin schema name
     * @return service plugin schema in serialized String format
     * @throws SMSException
     */
    public String getPluginSchema(String serviceName, String pluginName)
            throws SMSException {
        String oldServiceName = this.serviceName;
        this.serviceName = serviceName;
        String schema = getPluginSchema(pluginName);
        this.serviceName = oldServiceName;
        return (schema);
    }

    /**
     * Method returns the XML plugin schema for the given plugin name in a
     * String format that can stored in a directory.
     * 
     * @param pluginName
     *            name of the plugin schema name
     * @return service plugin schema in serialized String format
     * @throws SMSException
     */
    public String getPluginSchema(String pluginName) throws SMSException {
        Node parent = getServiceNode();
        for (Iterator pNodes = XMLUtils.getChildNodes(parent,
                SMSUtils.PLUGIN_SCHEMA).iterator(); pNodes.hasNext();) {
            Node node = (Node) pNodes.next();
            if (XMLUtils.getNodeAttributeValue(node, SMSUtils.NAME)
                    .equalsIgnoreCase(pluginName)) {
                return (PLUGIN_PREFIX_1 + getServiceName() + PLUGIN_PREFIX_2
                        + getServiceVersion() + PLUGIN_PREFIX_3
                        + nodeToString(node) + PLUGIN_SUFFIX);
            }
        }
        // Since the plugin schema node is not found, throw an exception
        throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                IUMSConstants.SMS_SMSSchema_no_service_element, null));
    }

    /**
     * Method returns the XML plugin schema for the given plugin name in a
     * String format that can stored in a directory.
     *
     * @param pluginName plugin schema name
     * @return service plugin schema in serialized String format
     * @throws SMSException
     */
    public String getPluginSchema(Node pluginName) throws SMSException {
        return (PLUGIN_PREFIX_1 + getServiceName() + PLUGIN_PREFIX_2
                                + getServiceVersion() + PLUGIN_PREFIX_3
                                + nodeToString(pluginName) + PLUGIN_SUFFIX);            
    }

    // ---------- static methods ----------------------
    static String schemaToXML(Node schemaNode, String svcName, String version) {
        return (SCHEMA_PREFIX_1 + svcName + SCHEMA_PREFIX_2 + version
                + SCHEMA_PREFIX_3 + nodeToString(schemaNode) + SCHEMA_SUFFIX);
    }

    public static String getDummyXML(String serviceName, String version) {
        return (SCHEMA_PREFIX_1 + serviceName + SCHEMA_PREFIX_2 + version 
                + "\"></Service></ServicesConfiguration>");
    }

    public static String nodeToString(Node a) {
        String nodeName = a.getNodeName();
        if (nodeName.equalsIgnoreCase("#comment")) {
            return "";
        }
        String answer = "<" + nodeName + getNodeAttributes(a) + ">";
        answer += getNodeValue(a);
        NodeList nodes = a.getChildNodes();
        Node node;
        int nodeLen = nodes.getLength();
        for (int i = 0; i < nodeLen; i++) {
            node = nodes.item(i);
            if (node.getNodeName().equalsIgnoreCase(TEXT_NODE))
                answer += escapeSpecialCharacters(node.getNodeValue());
            else
                answer += nodeToString(node);
        }
        answer += "</" + nodeName + ">";
        return (answer);
    }

    private static String getNodeAttributes(Node a) {
        String answer = "";
        int attrLen = 0;
        NamedNodeMap attrs = a.getAttributes();
        if ((attrs != null) && ((attrLen = attrs.getLength()) != 0)) {
            Node node;
            for (int i = 0; i < attrLen; i++) {
                node = attrs.item(i);
                answer += " " + node.getNodeName() + "=\"" + getNodeValue(node)
                        + "\" ";
            }
        }
        return (answer);
    }

    private static String getNodeValue(Node a) {
        String value = a.getNodeValue();
        if (value != null && value.length() != 0)
            return (value.trim());
        return ("");
    }

    public static Document getXMLDocument(String xml, boolean validation)
            throws SchemaException, SMSException {
        return (getXMLDocument(getServiceSchemaInputStream(xml), validation));
    }

    public static Document getXMLDocument(InputStream in)
            throws SchemaException, SMSException {
        return (getXMLDocument(in, true));
    }

    public static Document getXMLDocument(InputStream in, boolean validation)
            throws SchemaException, SMSException {

        try {
            DocumentBuilder builder = XMLUtils.getSafeDocumentBuilder(validation);
            builder.setErrorHandler(new SMSErrorHandler());
            Document doc = builder.parse(in);
            return (doc);
        } catch (SAXParseException pe) {
            SMSEntry.debug.error("SMSSchema: SAXPasrseException", pe);
            Object params[] = { new Integer(pe.getLineNumber()) };
            throw (new SchemaException(IUMSConstants.UMS_BUNDLE_NAME,
                    IUMSConstants.SMS_SMSSchema_parser_error, params));
        } catch (SAXException sax) {
            Object params[] = { sax.toString() };
            throw (new SchemaException(IUMSConstants.UMS_BUNDLE_NAME,
                    IUMSConstants.SMS_SMSSchema_exception_message, params));
        } catch (ParserConfigurationException pc) {
            Object params[] = { pc.toString() };
            throw (new SchemaException(IUMSConstants.UMS_BUNDLE_NAME,
                    IUMSConstants.SMS_SMSSchema_invalid_xml_document, params));
        } catch (IOException ioe) {
            Object params[] = { ioe.toString() };
            throw (new SchemaException(IUMSConstants.UMS_BUNDLE_NAME,
                    IUMSConstants.SMS_SMSSchema_invalid_input_stream, params));
        }
    }

    static InputStream getServiceSchemaInputStream(String xmlSchema)
            throws SMSException {
        if (xmlSchema == null || xmlSchema.length() == 0) {
            throw new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    IUMSConstants.SMSSCHEMA_SERVICE_NOTFOUND, null);
        }
        InputStream in;
        String xmlEncoding = "ISO-8859-1";
        if ((xmlSchema !=null) && 
                (xmlSchema.toLowerCase().indexOf(XML_ENC_UTF8) != -1) ) {
            xmlEncoding = "UTF-8";
        }
        try {
            in = new ByteArrayInputStream(xmlSchema.getBytes(xmlEncoding));
        } catch (Exception ex) {
            SMSEntry.debug.error("SMSSchema: Unsupported encoding ", ex);
            in = new ByteArrayInputStream(xmlSchema.getBytes());
        }
        return in;
    }

    Document getDocument() {
        return document;
    }

    /**
     * The method escapes '&', '<', '>', '"', '''
     */
    public static String escapeSpecialCharacters(String txt) {
        if (txt == null) {
            return txt;
        }

        int len = txt.length();
        if (len == 0) {
            return txt;
        }

        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < len; i++) {
            char c = txt.charAt(i);
            switch (c) {
            case '&':
                buf.append("&amp;");
                break;
            case '<':
                buf.append("&lt;");
                break;
            case '>':
                buf.append("&gt;");
                break;
            case '\"':
                buf.append("&quot;");
                break;
            case '\'':
                buf.append("&apos;");
                break;
            case '/':
                buf.append("&#47;");
                break;
            case '\u00A0':
                buf.append("&#x00A0;");
                break;
            default:
                buf.append(c);
            }
        }

        return buf.toString();
    }

    /**
     * The method reverses the escape sequence "&#47;" to the character "/".
     */
    public static String unescapeName(String txtName) {
        if (txtName == null) {
            return txtName;
        }

        int len = txtName.length();
        if (len == 0) {
            return txtName;
        }

        int indx;
        for (int i = 0; i < txtName.length(); i++) {
            indx = txtName.indexOf(SLASH_ESC_SEQ);
            if (indx >= 0) {
                String prefixID = txtName.substring(0, indx);
                String postfixID = txtName.substring(indx + 5);
                txtName = prefixID + "/" + postfixID;
            }
        }
        return (txtName);
    }
}
