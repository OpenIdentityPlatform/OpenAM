/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: XMLUtils.java,v 1.15 2009/10/19 18:19:20 asyhuang Exp $
 *
 */
/**
 * Portions Copyrighted 2011-2013 ForgeRock Inc
 */
package com.sun.identity.shared.xml;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import com.sun.org.apache.xerces.internal.parsers.DOMParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.forgerock.util.xml.XMLHandler;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;



/**
 * This class contains utilities to parse XML documents
 */
public class XMLUtils {
    private static final Map EMPTY_MAP = Collections
            .unmodifiableMap(new HashMap());

    // property to see if XML document validating is needed. The validating
    // is turned on only if the value for com.iplanet.am.util.xml.validating
    // property is set to "on" and value for com.iplanet.services.debug.level
    // property is set to "warning" or "message".
    private static boolean validating = false;
    private static String ATTR_BASE64_ENCODED =
            "com_sun_identity_opensso_base64_encoded";
    private static int ATTR_BASE64_ENCODED_LENGTH =
            ATTR_BASE64_ENCODED.length();
    private static final String INVALID_XML_CHARACTERS = "[\u0000-\u0008\u000b-\u001f\ufffe\uffff]";   
    private static Pattern invalidXMLChars =
            Pattern.compile(INVALID_XML_CHARACTERS);

    static {
        try {
            String xmlVal = SystemPropertiesManager.get(
                Constants.XML_VALIDATING, "off");
            String debugLevel = SystemPropertiesManager.get(
                Constants.SERVICES_DEBUG_LEVEL, "error");
            
            if (xmlVal.trim().equalsIgnoreCase("on") && 
                (debugLevel.trim().equalsIgnoreCase("warning") ||
                    debugLevel.trim().equalsIgnoreCase("message"))
            ) {
                validating = true;
            }
        } catch (Exception e) {
            // ignore since there is not debug class here
        }
    }

    public String getATTR_VALUE_PAIR_NODE() {
        return ATTR_VALUE_PAIR_NODE;
    }

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
        if ((xmlString == null) || (xmlString.length() == 0)) {
            return null;
        }

        try {
            ByteArrayInputStream is = new ByteArrayInputStream(xmlString
                    .getBytes("UTF-8"));
            return toDOMDocument(is, debug);
        } catch (UnsupportedEncodingException uee) {
            if (debug != null && debug.warningEnabled()) {
                debug.warning("Can't parse the XML document:\n" + xmlString,
                        uee);
            }
            return null;
        }
    }

    /**
     * Converts the XML document from an input stream to DOM Document format.
     * 
     * @param is
     *            is the InputStream that contains XML document
     * @return Document is the DOM object obtained by parsing the input stream.
     *         Returns null if there are any parser errores.
     */
    public static Document toDOMDocument(InputStream is, Debug debug) {
        DocumentBuilder documentBuilder = null;
        try {
            // Assign new debug object
            documentBuilder = getSafeDocumentBuilder(validating);
        } catch (ParserConfigurationException pe) {
            if (debug != null) {
                debug.error("XMLUtils.DocumentBuilder init failed", pe);
            }
        }

        try {
            if (documentBuilder == null) {
                if (debug != null) {
                    debug.error("XMLUtils.toDOM : null builder instance");
                }
                return null;
            }
            if (debug != null && debug.warningEnabled()) {
                documentBuilder.setErrorHandler(new ValidationErrorHandler(
                        debug));
            }

            return documentBuilder.parse(is);
        } catch (Exception e) {
            // Since there may potentially be several invalid XML documents
            // that are mostly client-side errors, only a warning is logged for
            // efficiency reasons.
            if (debug != null && debug.warningEnabled()) {
                debug.warning("Can't parse the XML document", e);
            }

            return null;
        }
    }

    /**
     * This method parse an Attributes tag, DTD for Attribute is as follows.
     * 
     * <pre>
     *  &lt; !-- This DTD defines the DPro Attribute tag.
     *    Unique Declaration name for DOCTYPE tag:
     *    &quot;Directory Pro 5.0 Attributes DTD&quot;
     *  --&gt;
     *  &lt; !ELEMENT Attributes (Attribute)+&gt;
     *  &lt; !ELEMENT Attribute EMPTY&gt;
     *  &lt; !ATTLIST Attribute
     *       name    NMTOKEN         #REQUIRED
     *  &gt;
     * </pre>
     * 
     * @param n
     *            Node
     * @return Set Set of the attribute names
     */
    public static Set parseAttributesTag(Node n) {
        // get Attribute node list
        NodeList attributes = n.getChildNodes();
        final int numAttributes = attributes.getLength();

        if (numAttributes == 0) {
            return null;
        }

        Set set = new HashSet();
        for (int l = 0; l < numAttributes; l++) {
            // get next attribute
            Node attr = attributes.item(l);
            if ((attr.getNodeType() != Node.ELEMENT_NODE)
                    && !attr.getNodeName().equals("Attribute")) {
                // need error handling
                continue;
            }

            set.add(((Element) attr).getAttribute("name"));
        }
        return set;
    }

    /**
     * @param parentNode
     *            is the element tag that contains all the AttirbuteValuePair
     *            tags as children
     * @return Map Returns the AV pairs in a Map where each entry in the Map is
     *         an AV pair. The key is the attribute name and the value is a Set
     *         of String objects.
     */
    public static Map parseAttributeValuePairTags(Node parentNode) {

        NodeList avList = parentNode.getChildNodes();
        Map map = null;
        int numAVPairs = avList.getLength();
        
        if (numAVPairs <= 0) {
            return EMPTY_MAP;
        }
        
        for (int l = 0; l < numAVPairs; l++) {
            Node avPair = avList.item(l);
            if ((avPair.getNodeType() != Node.ELEMENT_NODE) ||
                !avPair.getNodeName().equals("AttributeValuePair")
            ) {
                continue;
            }
            NodeList leafNodeList = avPair.getChildNodes();
            long numLeafNodes = leafNodeList.getLength();
            if (numLeafNodes < 2) {
                // TODO: More error handling required later for missing
                // 'Attribute' or
                // 'Value' tags.
                continue;
            }
            String key = null;
            Set values = null;
            // Since Attribute tag is always the first leaf node as per the
            // DTD,and
            // values can one or more, Attribute tag can be parsed first and
            // then
            // iterate over the values, if any.
            Node attributeNode = null;
            for (int i = 0; i < numLeafNodes; i++) {
                attributeNode = leafNodeList.item(i);
                if ((attributeNode.getNodeType() == Node.ELEMENT_NODE)
                        && (attributeNode.getNodeName().equals("Attribute"))) {
                    i = (int) numLeafNodes;
                } else {
                    continue;
                }
            }
            key = ((Element) attributeNode).getAttribute("name");
            // Now parse the Value tags. If there are not 'Value' tags, ignore
            // this key
            // TODO: More error handling required later for zero 'Value' tags.
            for (int m = 0; m < numLeafNodes; m++) {
                Node valueNode = leafNodeList.item(m);
                if ((valueNode.getNodeType() != Node.ELEMENT_NODE)
                        || !valueNode.getNodeName().equals("Value")) {
                    // TODO: Error handling required here
                    continue;
                }
                if (values == null) {
                    values = new HashSet();
                }
                Node fchild = (Text) valueNode.getFirstChild();
                if (fchild != null) {
                    String value = fchild.getNodeValue();
                    if (value != null) {
                        values.add(value.trim());
                    }
                }
            }
            if (values == null) {
                // No 'Value' tags found. So ignore this key.
                // TODO: More error handling required later for zero
                // 'Value'tags.
                continue;
            }
            if (map == null) {
                map = new HashMap();
            }
            Set oldValues = (Set)map.get(key);
            if (oldValues != null) {
                values.addAll(oldValues);
            }
            map.put(key, values);

            // now reset values to prepare for the next AV pair.
            values = null;
        }
        
        return (map == null) ? EMPTY_MAP : map;
    }

    /**
     * Obtains a new instance of a DOM Document object
     * 
     * @return a new instance of a DOM Document object
     * @exception Exception
     *                if an error occurs while constructing a new document
     */
    public static Document newDocument() throws ParserConfigurationException {
        return getSafeDocumentBuilder(validating).newDocument();
    }

    public static Document getXMLDocument(InputStream in) throws Exception {
        try {
            DocumentBuilder builder = getSafeDocumentBuilder(validating);
            Document doc = builder.parse(in);
            return doc;
        } catch (SAXParseException pe) {
            String msg = "\n" + pe.getMessage() + "\n";
            Object params[] = { new Integer(pe.getLineNumber()) };
            throw new Exception(msg + "XMLUtils.parser_error" + params);
        } catch (SAXException sax) {
            Object params[] = { sax.getMessage()};
            throw new Exception("XMLUtils.exception_message" + params);
        } catch (ParserConfigurationException pc) {
            Object params[] = { pc.getMessage()};
            throw new Exception("XMLUtils.invalid_xml_document" + params);
        } catch (IOException ioe) {
            Object params[] = { ioe.getMessage()};
            throw new Exception("XMLUtils.invalid_input_stream" + params);
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
     * Checks if a node has a child of ELEMENT type.
     * 
     * @param node
     *            a node
     * @return true if the node has a child of ELEMENT type
     */
    public static boolean hasElementChild(Node node) {
        NodeList nl = node.getChildNodes();
        Node child = null;
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            child = nl.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                return true;
            }
        }

        return false;
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

    /**
     * Gets the value of an element. This method returns a concatenated String
     * from all its TEXT children.
     * 
     * @param element
     *            a DOM tree element.
     * @return A String that contained in its TEXT children; or null if an error
     *         occurred.
     */
    public static String getElementValue(Element element) {
        if (element == null) {
            return null;
        }
        StringBuffer sb = new StringBuffer(1000);
        NodeList nl = element.getChildNodes();
        Node child = null;
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            child = nl.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                sb.append(child.getNodeValue());
            }
        }

        return sb.toString().trim();
    }

    /**
     * Gets the children value of an element. This method returns a
     * concatenated String from all its children.
     * @param element a DOM tree element.
     * @return A String that contained in its TEXT children; 
     *		or null if an error occurred.
     */
    public static String getChildrenValue(Element element) {
        if (element == null) {
            return null;
        }
        StringBuffer sb = new StringBuffer(1000);
        NodeList nl = element.getChildNodes();
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            sb.append(XMLUtils.print(nl.item(i)));
        }

	return sb.toString().trim();
    }

    /**
     * Gets the value of an element. This method returns a concatenated String
     * from all its TEXT children.
     * 
     * @param element
     *            a DOM tree element.
     * @return A String that contained in its TEXT children; or null if an error
     *         occurred or the input contain non Node.TEXT_NODE node.
     */
    public static String getElementString(Element element) {
        if (element == null) {
            return null;
        }
        StringBuffer sb = new StringBuffer(1000);
        NodeList nl = element.getChildNodes();
        Node child = null;
        for (int i = 0, length = nl.getLength(); i < length; i++) {
            child = nl.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                sb.append(child.getNodeValue());
            } else {
                return null;
            }
        }

        return sb.toString().trim();
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
     * Gets attribute value of a node.
     * 
     * @param node
     *            a node
     * @param namespaceURI
     *            attribute namespace URI
     * @param attrName
     *            attribute name
     * @return attribute value
     */
    public static String getNodeAttributeValueNS(Node node,
            String namespaceURI, String attrName) {
        NamedNodeMap attrs = node.getAttributes();
        if (attrs == null) {
            return null;
        }
        Node value = attrs.getNamedItemNS(namespaceURI, attrName);
        if (value == null) {
            return null;
        }
        return value.getNodeValue();
    }
    
    /**
     * Method to get Values within AttributeValuePair as a java.util.Set
     */
    public static Set getAttributeValuePair(Node node) {
        return (getAttributeValuePair(node, true));
    }

    /**
     * Method to get Values within AttributeValuePair as a java.util.Set
     * If <class>unescape<class> is set to false, xml escaped chars will not
     * be unescaped.
     */
    public static Set getAttributeValuePair(Node node, boolean unescape) {
        if (!node.getNodeName().equals(ATTR_VALUE_PAIR_NODE)) {
            return (null);
        }

        Set retVal = new OrderedSet();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeName().equalsIgnoreCase(VALUE_NODE)) {
                retVal.add(getValueOfValueNode(n, unescape));
            }
        }
        return (retVal);
    }
    
    /**
     * Method to get the value of "Value" node
     */
    public static String getValueOfValueNode(Node n) {
        return (getValueOfValueNode(n, true));
    }

    /**
     * Method to get the value of "Value" node
     * If <class>unescape<class> is set to false, xml escaped chars will not
     * be unescaped.
     */
    public static String getValueOfValueNode(Node n, boolean unescape) {
        return getValueOfValueNodeNoTrim(n, unescape).trim();
    }
    
    /**
     * Method to get the value of "Value" node
     */
    public static String getValueOfValueNodeNoTrim(Node n) {
        return (getValueOfValueNodeNoTrim(n, true));
    }

    /**
     * Method to get the value of "Value" node
     * If <class>unescape<class> is set to false, xml escaped chars will not
     * be unescaped.
     */
    public static String getValueOfValueNodeNoTrim(Node n, boolean unescape) {
        NodeList textNodes = n.getChildNodes();
        Node textNode;
        StringBuffer value = new StringBuffer("");
        for (int j = 0; j < textNodes.getLength(); j++) {
            textNode = textNodes.item(j);
            String text = null;
            if (textNode.getNodeType() == Node.TEXT_NODE) {
                text = textNode.getNodeValue();
            } else if (textNode.getNodeType() == Node.ELEMENT_NODE) {
                text = print(textNode);
            }
            if (text != null && unescape) {
                value.append(unescapeSpecialCharacters(text));
            } else {
                value.append(text);
            }
        }
        return value.toString();
    }

    /**
     * This method searches children of Element element for element with tagName
     * and namespaceURI nsName. It searchs one level down only.
     * 
     * @param element
     *            The root element
     * @param nsName
     *            NamespaceURI
     * @param tagName
     *            A String representing the name of the tag to be searched for.
     * @return A List of elements that meet the criterial.
     */
    public static List getElementsByTagNameNS1(
        Element element,
        String nsName,
        String tagName
    ) {
        List list = new ArrayList();

        if (element != null) {
            NodeList nl = element.getChildNodes();
            int length = nl.getLength();
            for (int i = 0; i < length; i++) {
                Node child = nl.item(i);
                String childName = child.getLocalName();
                String childNS = child.getNamespaceURI();
                if ((childName != null) && (childName.equals(tagName))
                        && (childNS != null) && (childNS.equals(nsName))) {
                    list.add(child);
                }
            }
        }
        return list;
    }

    /**
     * Print SAML Attribute Element and replace its prefix with the input
     * prefix.
     * 
     * @param node
     *            A DOM tree Node
     * @param prefix
     *            A String representing the new prefix
     * @return An xml String representation of the DOM tree.
     */
    public static String printAttributeValue(Element node, String prefix) {
        if (node == null) {
            return null;
        }

        StringBuffer xml = new StringBuffer(100);
        xml.append('<');
        xml.append(prefix).append(node.getLocalName());
        NamedNodeMap attrs = node.getAttributes();
        int length = attrs.getLength();
        for (int i = 0; i < length; i++) {
            Attr attr = (Attr) attrs.item(i);
            xml.append(' ');
            xml.append(attr.getNodeName());
            xml.append("=\"");
            // xml.append(normalize(attr.getNodeValue()));
            xml.append(attr.getNodeValue());
            xml.append('"');
        }
        xml.append('>');
        NodeList children = node.getChildNodes();
        if (children != null) {
            int len = children.getLength();
            for (int i = 0; i < len; i++) {
                xml.append(print(children.item(i)));
            }
        }
        xml.append("</");
        xml.append(prefix).append(node.getLocalName());
        xml.append('>');

        return xml.toString();

    }

    /**
     * Print a Node tree recursively using UTF-8 encoding.
     * 
     * @param node
     *            A DOM tree Node
     * @return An xml String representation of the DOM tree.
     */
    public static String print(Node node) {
        return print(node, "UTF-8");
    }
    
    /**
     * Prints a Node tree recursively.
     * @param node A DOM tree Node
     * @param encoding character encoding
     * @return An xml String representation of the DOM tree.
     */
    public static String print(Node node, String encoding) {
	if (node == null) {
	    return null;
	}
        
        try {
            TransformerFactory tFactory =
                TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            transformer.setOutputProperty("omit-xml-declaration", "yes");
            transformer.setOutputProperty("encoding", encoding);
            DOMSource source = new DOMSource(node);
            ByteArrayOutputStream os = new ByteArrayOutputStream(2000);
            StreamResult result = new StreamResult(os);
            transformer.transform(source, result);
            return os.toString(encoding);
        } catch (Exception e) {
            return null; 
        }
    }

    /**
     * Returns unescape special character text.
     *
     * @param text String to be unescaped.
     * @return unescape special character text.
     */
    public static String unescapeSpecialCharacters(String text) {
        text = text.replaceAll("&amp;", "&");
        text = text.replaceAll("&lt;", "<");
        text = text.replaceAll("&gt;", ">");
        text = text.replaceAll("&quot;", "\"");
        text = text.replaceAll("&apos;", "'");
        text = text.replaceAll("&#xA;", "\n");
        text = text.replaceAll("&#xD;", "\r");
        return text;
    }

    /**
     * Replaces XML special character <code>&</code>, <code>&lt;</code>,
     * <code>&gt;</code>, <code>"</code>, <code>'</code> with
     * corresponding entity references.
     * 
     * @return String with the special characters replaced with entity
     *         references.
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
                    || c == '\n' || c == '\r') {
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
                    sb.append("&#xA;");
                    break;
                case '\r':
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

    private static boolean invalidXMLCharExists(String st) {
        Matcher matcher = invalidXMLChars.matcher(st);
        return matcher.find();
    }
       
    /**
     * Remove invalid XML characters from a string.
     * @param text the text to cleanse.
     * @return cleansed text or the original string if it is null or empty
     */
    public static String removeInvalidXMLChars(String text){
    	if (text == null || text.length() == 0) {
    		return text;
    	}
    	String modifiedText = text.replaceAll(INVALID_XML_CHARACTERS, "");    	
    	return modifiedText;
    }

    public static Set encodeAttributeSet(Set set, Debug debug) {
        if (set == null) {
            return set;
        }

        Set newSet = new HashSet();
        Iterator iter = set.iterator();
        while(iter.hasNext()) {
            Object obj = iter.next();
            String st = (String)obj;
            if (invalidXMLCharExists(st)) {
                st = Base64.encode(st.getBytes());
                if ((debug != null) && (debug.warningEnabled())) {
                    debug.warning("XMLUtils.encodeAttributeSet invalid XML characters get Base64 encoded to be : " + st);
                }
                st = ATTR_BASE64_ENCODED + st;
            }
            newSet.add(st);
        }
        return newSet;
    }

    public static Set decodeAttributeSet(Set set) {
        if (set == null) {
            return set;
        }

        Set newSet = new HashSet();
        Iterator iter = set.iterator();
        while(iter.hasNext()) {
            Object obj = iter.next();
            String st = (String)obj;
            if (st.startsWith(ATTR_BASE64_ENCODED)) {
                st = new String(Base64.decode(
                        st.substring(ATTR_BASE64_ENCODED_LENGTH)));
            }
            newSet.add(st);
        }
        return newSet;
    }

    public static String removeNullCharAtEnd(String st) {
        int index = st.length() - 1;
        char c = st.charAt(index);
        if (c == '\u0000') {
            return st.substring(0, index);
        }
        return st;
    }

    /**
     * Provides a secure DocumentBuilder implementation, which is protected against
     * different types of entity expansion attacks and makes sure that only locally
     * available DTDs can be referenced within the XML document.
     * @param validating Whether the returned DocumentBuilder should validate input.
     * @return A secure DocumentBuilder instance.
     * @throws ParserConfigurationException In case xerces does not support one
     * of the required features.
     */
    public static DocumentBuilder getSafeDocumentBuilder(boolean validating) throws ParserConfigurationException {
        return org.forgerock.util.xml.XMLUtils.getSafeDocumentBuilder(validating);
    }

    /**
     * Provides a secure SAXParser instance, which is protected against different
     * types of entity expension, DoS attacks and makes sure that only locally
     * available DTDs can be referenced within the XML document.
     * @param validating Whether the returned DocumentBuilder should validate input.
     * @return A secure SAXParser instance.
     * @throws ParserConfigurationException In case Xerces does not support one of
     * the required features.
     * @throws SAXException In case Xerces does not support one of the required
     * features.
     */
    public static SAXParser getSafeSAXParser(boolean validating) throws ParserConfigurationException, SAXException {
        return org.forgerock.util.xml.XMLUtils.getSafeSAXParser(validating);
    }

    /**
     * Creates a SAXSource instance based on the incoming InputSource, which
     * later on can be safely used by JAXB unmarshalling. The SAXSource will be
     * protected against different types of entity expansion, DoS attacks and
     * makes sure that only locally available DTDs can be referenced within the
     * XML document.
     * @param source The InputSource to be unmarshalled by JAXB
     * @return A safe SAXSource instance
     * @throws JAXBException In case an error occurs while creating the SAXSource
     */
    public static SAXSource createSAXSource(InputSource source) throws JAXBException{
        try {
            SAXParser saxParser = getSafeSAXParser(false);
            return new SAXSource(saxParser.getXMLReader(), source);
        } catch (Exception ex) {
            //Let's convert the exception to a JAXBException, so the unmarshalling
            //codes can handle the failure.
            throw new JAXBException("Unable to create SAXSource", ex);
        }
    }

    private static String ATTR_VALUE_PAIR_NODE = "AttributeValuePair";

    private static String VALUE_NODE = "Value";

}

class ValidationErrorHandler implements ErrorHandler {
    private Debug debug;

    ValidationErrorHandler(Debug tmpDebug) {
        debug = tmpDebug;

    }

    public void fatalError(SAXParseException spe) throws SAXParseException {
        if (debug != null) {
            debug.error("XMLUtils.fatalError", spe);
        }
    }

    public void error(SAXParseException spe) throws SAXParseException {
        if (debug != null) {
            debug.warning("XMLUtils.error", spe);
        }
    }

    public void warning(SAXParseException spe) throws SAXParseException {
        if ((debug != null) && (debug.warningEnabled())) {
            debug.warning("XMLUtils.warning", spe);
        }
    }
}
