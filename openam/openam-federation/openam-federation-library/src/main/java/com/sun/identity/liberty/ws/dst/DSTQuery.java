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
 * $Id: DSTQuery.java,v 1.3 2008/06/25 05:47:13 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.dst;

import com.sun.identity.liberty.ws.disco.EncryptedResourceID;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * The <code>DSTQuery</code> class represents a <code>DST</code> query request.
 * <p>The following schema fragment specifies the expected content within the
 * <code>DSTQuery</code>  object.
 * <p>
 * <pre>
 * &lt;complexType name="QueryType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;group ref="{urn:liberty:idpp:2003-08}ResourceIDGroup"/>
 *         &lt;element name="QueryItem" maxOccurs="unbounded">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}
 *               anyType">
 *                 &lt;sequence>
 *                   &lt;element name="Select" 
 *                   type="{urn:liberty:idpp:2003-08}SelectType"/>
 *                 &lt;/sequence>
 *                 &lt;attribute name="id"
 *                 type="{http://www.w3.org/2001/XMLSchema}ID" />
 *                 &lt;attribute name="changedSince"
 *                 type="{http://www.w3.org/2001/XMLSchema}dateTime" />
 *                 &lt;attribute name="itemID"
 *                 type="{urn:liberty:idpp:2003-08}IDType" />
 *                 &lt;attribute name="includeCommonAttributes"
 *                 type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element ref="{urn:liberty:idpp:2003-08}Extension"
 *         maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="itemID" type="{urn:liberty:idpp:2003-08}IDType" />
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * @supported.all.api
 */

public class DSTQuery {

    private String resourceID = null;
    private EncryptedResourceID encryptedResourceID = null;
    private String id = null;
    private String itemID = null;
    private List dstQueryItems = new ArrayList();
    private List extensions = new ArrayList();
    private String nameSpaceURI = null;
    private String prefix = null;

    /**
     * Constructor
     */
    public DSTQuery() {}

    /**
     * Constructor 
     * @param resourceID id for the resource to be queried.
     * @param items List of <code>DSTQueryItem</code> objects.
     * @param serviceNS service name space.
     */
    public DSTQuery(String resourceID, java.util.List items, String serviceNS) {
        this.resourceID = resourceID;
        if(items != null && items.size() != 0) {
           dstQueryItems.addAll(items); 
           DSTQueryItem item = (DSTQueryItem)items.get(0);
           if(serviceNS == null) {
              nameSpaceURI = item.getNameSpaceURI();
           } else {
              nameSpaceURI = serviceNS;
           }
           prefix = item.getNameSpacePrefix();
        }
    }

    /**
     * Constructor 
     * @param encResourceID id for the encrypted resource to be queried.
     * @param items list of <code>DSTQueryItem</code> objects.
     * @param serviceNS service name space.
     */
    public DSTQuery(com.sun.identity.liberty.ws.disco.EncryptedResourceID 
                    encResourceID, java.util.List items, String serviceNS) {
        this.encryptedResourceID = encResourceID;
        if(items != null && items.size() != 0) {
           dstQueryItems.addAll(items); 
           DSTQueryItem item = (DSTQueryItem)items.get(0);
           if(serviceNS == null) {
              nameSpaceURI = item.getNameSpaceURI();
           } else {
              nameSpaceURI = serviceNS;
           }
           prefix = item.getNameSpacePrefix();
        }
    }

    /**
     * Constructor
     *
     * @param element <code>DOM</code> Element.
     * @throws DSTException
     */
    public DSTQuery(org.w3c.dom.Element element) throws DSTException{
        if(element == null) {
           DSTUtils.debug.error("DSTQuery(element):null input");
           throw new DSTException(DSTUtils.bundle.getString("nullInputParams"));
        }
        String elementName = element.getLocalName();
        if(elementName == null || !elementName.equals("Query")) {
           DSTUtils.debug.error("DSTQuery(element):Invalid element name");
           throw new DSTException(DSTUtils.bundle.getString("invalidElement"));
        }
        nameSpaceURI = element.getNamespaceURI();
        if(nameSpaceURI == null) {
           DSTUtils.debug.error("DSTModify(element): NameSpace is not defined");
           throw new DSTException(DSTUtils.bundle.getString("noNameSpace"));
        }
        prefix = element.getPrefix();
        id = element.getAttribute("id");
        itemID = element.getAttribute("itemID");
        NodeList list = element.getElementsByTagNameNS(
                        nameSpaceURI, "ResourceID");

        if((list.getLength() == 0) || (list.getLength() > 1)) {
           DSTUtils.debug.error("DSTQuery(element): ResourceIDNode is null" +
           " or more than one resource id is found.");
           throw new DSTException(
           DSTUtils.bundle.getString("invalidResourceID"));
        }
        resourceID = XMLUtils.getElementValue((Element)list.item(0));
        if(resourceID == null) {
           DSTUtils.debug.error("DSTQuery(element): ResourceID is null" );
           throw new DSTException(
           DSTUtils.bundle.getString("invalidResourceID"));
        }

        NodeList queryItemNodes = element.getElementsByTagNameNS(
                 nameSpaceURI, "QueryItem");
        if(queryItemNodes == null || queryItemNodes.getLength() == 0) {
           DSTUtils.debug.error("DSTQuery(element): QueryItems are null" );
           throw new DSTException(
           DSTUtils.bundle.getString("nullQueryItems"));
        }
        int size = queryItemNodes.getLength();
        for(int i=0; i < size; i++) {
            Node node = queryItemNodes.item(0);
            DSTQueryItem dstQueryItem =
                     new DSTQueryItem((Element)node);
            dstQueryItems.add(dstQueryItem);
        }
    }

    /**
     * Gets the value of the <code>QueryItem</code> property.
     * 
     * @return List of <code>DSTQueryItem</code> objects
     */
    public java.util.List getQueryItems() {
        return dstQueryItems;
    }

    /**
     * Sets the value of the <code>QueryItem</code> property.
     *
     * @param items List of <code>DSTQueryItem</code> objects
     */
    public void setQueryItem(java.util.List items) {
        if(items != null && items.size() != 0) {
           dstQueryItems.addAll(items);
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
     * @param id value of id to be set 
     */
     public void setId(java.lang.String id) {
         this.id = id;
     }

    /**
     * Gets encrypted resource ID.
     * @return Encrypted resource ID.
     */
    public com.sun.identity.liberty.ws.disco.EncryptedResourceID
        getEncryptedResourceID() {
        return encryptedResourceID;
    }

    /**
     * Sets encrypted resource ID for the <code>DST</code> query.
     *
     * @param encResourceID encrypted resource ID.
     */
    public void setEncryptedResourceID(
        com.sun.identity.liberty.ws.disco.EncryptedResourceID encResourceID) {
        this.encryptedResourceID = encResourceID;
    }

    /**
     * Gets resource ID.
     *
     * @return resource ID.
     */
    public java.lang.String getResourceID() {
        return resourceID;
    }

    /**
     * Sets resource ID for the <code>DST</code> query.
     * @param resourceID resource ID to be set 
     */
    public void setResourceID(java.lang.String resourceID) {
        this.resourceID = resourceID;
    }

    /**
     * Gets item ID attribute
     * @return String
     */
    public java.lang.String getItemID() {
        return itemID;
    }

    /**
     * Sets item ID attribute
     * @param value item ID to be set 
     */
    public void setItemID(java.lang.String value) {
        this.itemID = value;
    }

    /**
     * Gets the extension property.
     * 
     * @return List of any Object
     * 
     */
    public java.util.List getExtension() {
        return extensions;
    }

    /**
     * Gets the name space.
     * @return name space.
     */
    public java.lang.String getNameSpaceURI() {
        return nameSpaceURI;
    }

    /**
     * Sets the name space.
     *
     * @param nameSpace Name space URI.
     */
    public void setNameSpaceURI(String nameSpace) {
        this.nameSpaceURI = nameSpace;
    }

    /**
     * Sets the name space prefix.
     * @param prefix Name space prefix.
     */
    public void setNameSpacePrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Gets the name space prefix.
     * @return Name space prefix.
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
     * @param includeNS if true prepends all elements by their name space prefix
     * @param declareNS if true includes the name space within the 
     *                  generated.
     * @return String A string containing the valid XML for this element
     */
    public java.lang.String toString(boolean includeNS, boolean declareNS) {

        if((encryptedResourceID == null && resourceID == null) ||
           dstQueryItems.isEmpty()) {
           DSTUtils.debug.error("DSTQuery.toString: ResourceID or QueryItems" +
           " are null.");
           return "";
        }

        String tempPrefix = "";
        if(includeNS) {
           if(prefix == null) {
              prefix = DSTConstants.DEFAULT_NS_PREFIX;
           }
           tempPrefix = prefix + ":";
        } 

        if (declareNS && nameSpaceURI == null) {
            DSTUtils.debug.error("DSTQuery.toString: Name Space is " +
              "not defined");
            return "";
        }

        StringBuffer sb = new StringBuffer(3000);
        sb.append("<").append(tempPrefix).append("Query");
        if(id != null && id.length() != 0) {
           sb.append(" id=\"").append(id).append("\"");
        }
        if(itemID != null && itemID.length() != 0) {
           sb.append(" itemID=\"").append(itemID).append("\"");
        }
        if(declareNS) {
           sb.append(" xmlns:").append(prefix).append("=\"")
             .append(nameSpaceURI).append("\"");
        }
        sb.append(">");
        if(encryptedResourceID == null) {
            sb.append("<").append(tempPrefix).append("ResourceID").append(">")
                .append(resourceID).append("</").append(tempPrefix)
                .append("ResourceID").append(">");
        } else {
            sb.append(encryptedResourceID.toString(nameSpaceURI));
        }

        Iterator iter = dstQueryItems.iterator();
        while(iter.hasNext()) {
           DSTQueryItem item = (DSTQueryItem)iter.next();
           sb.append(item.toString(true, false));
        }
        sb.append("</").append(tempPrefix).append("Query").append(">");

        if(DSTUtils.debug.messageEnabled()) {
           DSTUtils.debug.message("DSTQuery.toString: Query: " + sb.toString());
        }

        return sb.toString();
    }

}
