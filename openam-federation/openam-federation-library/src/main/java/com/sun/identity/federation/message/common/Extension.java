/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Extension.java,v 1.2 2008/06/25 05:46:46 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.federation.message.common;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.xml.XMLUtils;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The class <code>Extension</code> is used to create , parse
 * <code>Extension</code> object.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class Extension {
    private List children = null;
    private Map avpairs = null;
    private int minorVersion = 0;

    /**
     * Constructor to create <code>Extension</code> object.
     *
     * @param children a list of XML <code>String</code> object.
     * @throws FSMsgException on error.
     */
    public Extension(List children) throws FSMsgException {
        validateChildren(children);
        this.children = children;
    }

    /**
     * Constructor to create <code>Extension</code> object.
     *
     * @param element the <code>Extension</code> Element object.
     * @throws FSMsgException on error.
     */
    public Extension(Element element) throws FSMsgException {
        if (element == null) {
            FSUtils.debug.error("Extension.Extension: null input.");
            throw new FSMsgException("nullInput", null);
        }
        String nodeName = element.getLocalName();
        if (!IFSConstants.EXTENSION.equals(nodeName)) {
            FSUtils.debug.error("Extension.Extension: wrong input");
            throw new FSMsgException("wrongInput", null);
        }
             
        NodeList childNodes = element.getChildNodes();
        int length = childNodes.getLength();
        for(int i = 0; i < length; i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                if (children == null) {
                    children = new ArrayList();
                }
                children.add(XMLUtils.print(child));
                addToAvpairs((Element)child);
            }
        }
    }

    /**
     * Constructor to create <code>Extension</code> object. Each attribute
     * value pair will be converted to a XML string. The converted XML string
     * has only one element. The local name of the element will be the key of
     * the map entry and the value of the element will be the value of the map
     * entry. Both key and value of the map entry should be a
     * <code>String</code> object. 
     *
     * @param avpairs attribute value pairs.
     * @throws FSMsgException on error.
     */
    public Extension(Map avpairs) throws FSMsgException {
        setAttributeMap(avpairs);
    }

    /**
     * Returns a list of XML <code>String</code> objects.
     *
     * @return a list of XML <code>String</code> objects.
     * @see #setChildren(List)
     */
    public List getChildren() {
        return children;
    }

    /**
     * Sets a list of XML <code>String</code> object.
     *
     * @param children a list of XML <code>String</code> object.
     * @see #getChildren()
     */
    public void setChildren(List children) throws FSMsgException {
        validateChildren(children);
        this.children = children;
    }

    /*
     * Gets attribute value pairs. Each attribute value pair is converted from
     * a XML string. The XML string can have only one element. The element
     * element can't have namespace and must have a simple content. The local
     * name of the element will be the key of the map entry and the value of
     * the element will be the value of the map entry. Both key and value of
     * the map entry will be a <code>String</code> object. If a XML string
     * can't be converted, it will not be added to the map.
     * 
     * @return an attribute value pairs.
     */
    public Map getAttributeMap() {
        if ((children == null) || (children.isEmpty())) {
            return null;
        }

        if (avpairs != null) {
            return avpairs;
        }
        for(Iterator iter = children.iterator(); iter.hasNext(); ) {
            String xml = (String)iter.next();

            Document doc = XMLUtils.toDOMDocument(xml, FSUtils.debug);
            if (doc == null) {
                continue;
            }
            Element element = doc.getDocumentElement();
            addToAvpairs(element);
        }

        return avpairs;
    }

    /**
     * Converts attribute value pairs to a list of XML <code>String</code>
     * objects. Each attribute value pair will be converted to a XML string.
     * The converted XML string has only one element. The local name of the
     * element will be the key of the map entry and the value of the element
     * will be the value of the map entry. Both key and value of the map entry
     * should be a <code>String</code> object. 
     *
     * @param avpairs attribute value pairs.
     * @throws FSMsgException on error.
     */
    public void setAttributeMap(Map avpairs) throws FSMsgException {
        this.avpairs = avpairs;
        if ((avpairs != null) && (!avpairs.isEmpty())) {
            for(Iterator iter = avpairs.keySet().iterator(); iter.hasNext();) {
                String key = (String)iter.next();
                String value = (String)avpairs.get(key);
                String xml = IFSConstants.LEFT_ANGLE + key +
                             IFSConstants.RIGHT_ANGLE +
                             XMLUtils.escapeSpecialCharacters(value) +
                             IFSConstants.START_END_ELEMENT + key +
                             IFSConstants.RIGHT_ANGLE;
                if (children == null) {
                    children = new ArrayList();
                }
                children.add(xml);
            }
        }
    }

    /**
     * Returns the <code>MinorVersion</code>.
     *
     * @return the <code>MinorVersion</code>.
     * @see #setMinorVersion(int)
     */
    public int getMinorVersion() {
       return minorVersion;
    }

    /**
     * Sets the <code>MinorVersion</code>.
     *
     * @param minorVersion the <code>MinorVersion</code>.
     * @see #getMinorVersion()
     */
    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }

    /**
     * Returns a String representation of the <Code>Extension</Code> element.
     *
     * @return a string containing the valid XML for this element
     * @throws FSMsgException if there is an error converting this object to
     *     a string.
     */
    public String toXMLString() throws FSMsgException {
        return this.toXMLString(true, false);
    }

    /**
     * Creates a String representation of the <Code>Extension</Code> element.
     *
     * @param includeNS : Determines whether or not the namespace qualifier
     *     is prepended to the Element when converted
     * @param declareNS : Determines whether or not the namespace is declared
     *     within the Element.
     * @return string containing the valid XML for this element.
     * @throws FSMsgException if there is an error.
     */
    public String toXMLString(boolean includeNS, boolean declareNS)
        throws FSMsgException {

        String prefix = "";
        String uri = "";

        StringBuffer xml = new StringBuffer();
        if (includeNS) {
            prefix = IFSConstants.LIB_PREFIX;
        }
        if (declareNS) {
            if(minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
                uri = IFSConstants.LIB_12_NAMESPACE_STRING;
            } else {
                uri = IFSConstants.LIB_NAMESPACE_STRING;
            }
        }
        xml.append(IFSConstants.LEFT_ANGLE)
           .append(prefix)
           .append(IFSConstants.EXTENSION)
           .append(uri)
           .append(IFSConstants.RIGHT_ANGLE);
        if ((children != null) && (!children.isEmpty())) {
            for(Iterator iter = children.iterator(); iter.hasNext();) {
                String child = (String)iter.next();
                xml.append(child);
            }
        }
        xml.append(IFSConstants.START_END_ELEMENT)
           .append(prefix)
           .append(IFSConstants.EXTENSION)
           .append(IFSConstants.RIGHT_ANGLE);

        return xml.toString();
    }

    /**
     * Returns <code>Extension</code> object. The object is creating by
     * parsing the <code>HttpServletRequest</code> object.
     *
     * @param request the <code>HttpServletRequest</code> object.
     * @param prefix the string that is prepended to the key of query
     *     string.
     * @param minorVersion the <code>MinorVersion</code>.
     * @return <code><Extension/code> object.
     * @throws FSMsgException if there is an error creating
     *     <code>Extension</code> object.
     */
    public static Extension parseURLEncodedRequest(HttpServletRequest request,
        String prefix, int minorVersion) throws FSMsgException {

        Map attrMap = null;
        for(Enumeration e=request.getParameterNames(); e.hasMoreElements();) {
            String paraName = (String)e.nextElement();
            if (paraName.startsWith(prefix)) {
                String key = paraName.substring(prefix.length());
                String value = request.getParameter(paraName);
                if (attrMap == null) {
                    attrMap = new HashMap();
                }
                attrMap.put(key, value);
            }
        }

        if (attrMap == null) {
            return null;
        }

        Extension extension = new Extension(attrMap);
        extension.setMinorVersion(minorVersion);
        return extension;        
    }
      
    /**
     * Returns an URL Encoded String.
     *
     * @param prefix the string that will be prepended to the key of query
     *     string.
     * @return a url encoded query string.
     * @throws FSMsgException if there is an error.
     */
    public String toURLEncodedQueryString(String prefix)
        throws FSMsgException {

        Map attrMap = getAttributeMap();
        if ((attrMap == null) || (attrMap.isEmpty())) {
            return "";
        }

        StringBuffer queryString = new StringBuffer();
        for(Iterator iter = attrMap.keySet().iterator();iter.hasNext();) {
            String key = (String)iter.next();
            String value = URLEncDec.encode((String)attrMap.get(key));
            key = URLEncDec.encode(prefix + key);
            if (queryString.length() > 0) {
                queryString.append(IFSConstants.AMPERSAND);
            }
            queryString.append(key).append(IFSConstants.EQUAL_TO)
                       .append(value);
        }

        return queryString.toString();
    }

    private void addToAvpairs(Element element) {
        String ns = element.getNamespaceURI();
        if ((ns == null) && (!XMLUtils.hasElementChild(element))) {
            String key = element.getLocalName();
            String value = XMLUtils.getElementValue(element);
            if (avpairs == null) {
                avpairs = new HashMap();
            }
            avpairs.put(key, value);
       }
    }

    private static void validateChildren(List children) throws FSMsgException {
        if ((children != null) && (!children.isEmpty())) {
            for(Iterator iter = children.iterator(); iter.hasNext(); ) {
                String xml = (String)iter.next();
                Document doc = XMLUtils.toDOMDocument(xml, FSUtils.debug);
                if (doc == null) {
                    FSUtils.debug.error("Extension.validateChildren: Error "
                        + "while parsing input xml string");
                    throw new FSMsgException("parseError", null);
                }
            }
        }
    }
}
