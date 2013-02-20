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
 * $Id: DSTData.java,v 1.2 2008/06/25 05:47:13 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.dst;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import com.sun.identity.shared.xml.XMLUtils;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The <code>DSTData</code> class provides a wrapper for any data entry.
 * 
 * The following schema fragment specifies the expected content within 
 * the <code>DSTData</code> object.
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;any/>
 *       &lt;/sequence>
 *       &lt;attribute name="itemIDRef" 
 *       type="{urn:liberty:idpp:2003-08}IDReferenceType" />
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * @supported.all.api
 */

public class DSTData {

    private List dstData = new ArrayList();
    private String itemIDRef = null;
    private String id = null;
    private String nameSpaceURI = null;
    private String prefix = null;

    /**
     * Constructor
     * @param data List of data XML <code>DOM</code> Elements.
     * @param serviceNS service nameSpace
     */
    public DSTData (java.util.List data, String serviceNS) {
        if(data != null) {
           this.dstData.addAll(data);
        }
        nameSpaceURI = serviceNS;
    }

    /**
     * Constructor
     *
     * @param element <code>DOM</code> Element
     * @throws DSTException
     */
    public DSTData(org.w3c.dom.Element element) throws DSTException{
        if(element == null) {
           DSTUtils.debug.error("DSTData(element):null input");
           throw new DSTException(DSTUtils.bundle.getString("nullInputParams"));
        }
        String elementName = element.getLocalName();
        if(elementName == null || !elementName.equals("Data")) {
           DSTUtils.debug.error("DSTData(element):Invalid element name");
           throw new DSTException(DSTUtils.bundle.getString("invalidElement"));
        }
        nameSpaceURI = element.getNamespaceURI();
        if(nameSpaceURI == null) {
           DSTUtils.debug.error("DSTData(element): NameSpace is not defined");
           throw new DSTException(DSTUtils.bundle.getString("noNameSpace"));
        }
        prefix = element.getPrefix();
        id = element.getAttribute("id");
        itemIDRef = element.getAttribute("itemIDRef");
        NodeList list = element.getChildNodes(); 
        int size = list.getLength();
        for(int i=0; i < size; i++) {
           Node dataNode = list.item(i);
           if(dataNode.getNodeType() == Node.ELEMENT_NODE) {
              dstData.add((Element)dataNode);
           }
        }

    }

    /**
     * Gets id attribute.
     *
     * @return id attribute.
     */
    public java.lang.String getId() {
        return id;
    }

    /**
     * Sets id attribute
     *
     * @param id attribute 
     */
    public void setId(String id) {
        this.id = id;
    } 

    /**
     * Gets item reference.
     *
     * @return item reference.
     */
    public java.lang.String getItemIDRef() { 
        return itemIDRef;
    }

    /**
     * Sets item reference.
     *
     * @param ref reference item.
     */
    public void setItemIDRef(java.lang.String ref) {
        this.itemIDRef = ref;
    }

    /**
     * Gets the returned data objects.
     * 
     * @return List of any <code>java.lang.Object</code>.
     * 
     */
    public java.util.List getData() {
        return dstData;
    }

    /**
     * Gets the name space.
     * @return Name space.
     */
    public java.lang.String getNameSpaceURI() {
        return nameSpaceURI;
    }

    /**
     * Sets the name space.
     * @param nameSpace NameSpace URI
     */
    public void setNameSpaceURI(String nameSpace) {
        this.nameSpaceURI = nameSpace;
    }

    /**
     * Sets the name space prefix.
     * @param prefix NameSpace prefix.
     */
    public void setNameSpacePrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Gets the name space prefix.
     * @return String NameSpace prefix.
     */
    public java.lang.String getNameSpacePrefix() {
        return prefix;
    }

    /**
     * Creates a String representation of this object.
     * By default name space name is prepended to the element name
     * @return String A string containing the valid XML for this element
     */
    public java.lang.String toString() {
        return toString(true, false);
    }

    /**
     * Creates a String representation of this object.
     * @param includeNS if true prepends all elements by their Namespace prefix
     * @param declareNS if true includes the namespace within the
     *                  generated.
     * @return String A string containing the valid XML for this element
     */
    public java.lang.String toString(boolean includeNS, boolean declareNS) {

        String tempPrefix = "";
        if(includeNS) {
           if(prefix == null) {
              prefix = DSTConstants.DEFAULT_NS_PREFIX;
           }
           tempPrefix = prefix + ":";
        }
        if(declareNS)
        { if(nameSpaceURI == null) {
              DSTUtils.debug.error("DSTData.toString: Name Space is " +
              "not defined");
              return "";
           }
        }
        StringBuffer sb = new StringBuffer(300);
        sb.append("<").append(tempPrefix).append("Data");
        if(id != null && id.length() != 0) {
           sb.append(" id=\"").append(id).append("\"");
        }
        if(itemIDRef != null && itemIDRef.length() != 0) {
           sb.append(" itemIDRef=\"").append(itemIDRef).append("\"");
        }

        if(declareNS) {
           sb.append(" xmlns:").append(prefix).append("=\"")
             .append(nameSpaceURI).append("\"")
             .append(" xmlns=\"").append(nameSpaceURI).append("\"");
        }
        sb.append(">");

        Iterator iter = dstData.iterator();
        while(iter.hasNext()) {
           Node node = (Node)iter.next();
           sb.append(XMLUtils.print(node));
        }
        sb.append("</").append(tempPrefix).append("Data").append(">");
        return sb.toString(); 
    }

}
