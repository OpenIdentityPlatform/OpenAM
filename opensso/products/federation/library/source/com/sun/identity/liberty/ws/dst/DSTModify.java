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
 * $Id: DSTModify.java,v 1.2 2008/06/25 05:47:13 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.dst;

import com.sun.identity.liberty.ws.disco.EncryptedResourceID;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * The <code>DSTModify</code> class represents a <code>DST</code> modify
 * request.
 * 
 * The following schema fragment specifies the expected content within
 * the <code>DSTModify</code> object.
 * 
 * <pre>
 * &lt;complexType name="ModifyType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;group ref="{urn:liberty:idpp:2003-08}ResourceIDGroup"/>
 *         &lt;element name="Modification" maxOccurs="unbounded">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}
 *                anyType">
 *                 &lt;sequence>
 *                   &lt;element name="Select"
 *                   type="{urn:liberty:idpp:2003-08}SelectType"/>
 *                   &lt;element name="NewData" minOccurs="0">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction 
 *                         base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;sequence>
 *                           &lt;any/>
 *                           &lt;/sequence>
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *                 &lt;attribute name="overrideAllowed"
 *                 type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *                 &lt;attribute name="id"
 *                 type="{http://www.w3.org/2001/XMLSchema}ID" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element ref="{urn:liberty:idpp:2003-08}Extension"
 *         maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *       &lt;attribute name="itemID" type="{urn:liberty:idpp:2003-08}IDType" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * @supported.all.api
 */
public class DSTModify {

    private String resourceID;
    private EncryptedResourceID encryptedResourceID;
    private List modification = new ArrayList();
    private String itemID;
    private String id;
    private List extension = new ArrayList();
    private String nameSpaceURI = null;
    private String prefix = null;

    /**
     * Constructor
     */
    public DSTModify() {}

    /**
     * Constructor
     * @param resourceID id for the resource to be modified. 
     * @param modifications list of <code>DSTModification</code> to be
     *        performed. 
     * @param serviceNS service name space.
     */
    public DSTModify(String resourceID, 
                     java.util.List modifications,
                     String serviceNS) {
        this.resourceID = resourceID;
        if(modifications != null) {
           modification.addAll(modifications);
           DSTModification dm = (DSTModification)modifications.get(0);
           if(serviceNS == null) {
              nameSpaceURI = dm.getNameSpaceURI();
           } else {
              nameSpaceURI = serviceNS;
           }
           prefix = dm.getNameSpacePrefix();
        }
    }

    /**
     * Constructor
     * @param encResourceID id for encrypted resource to be modified.
     * @param modifications list of <code>DSTModification</code> to be
     *        performed.
     * @param serviceNS service name space.
     */
    public DSTModify(
           com.sun.identity.liberty.ws.disco.EncryptedResourceID encResourceID, 
           java.util.List modifications,
           String serviceNS) {
        this.encryptedResourceID = encResourceID;
        if(modifications != null) {
           modification.addAll(modifications);
           DSTModification dm = (DSTModification)modifications.get(0);
           if(serviceNS == null) {
              nameSpaceURI = dm.getNameSpaceURI();
           } else {
              nameSpaceURI = serviceNS;
           }
           prefix = dm.getNameSpacePrefix();
        }
    }

    /**
     * Constructor
     * @param element <code>DOM</code> Element.
     * @exception DSTException
     */
    public DSTModify(org.w3c.dom.Element element) throws DSTException{
        if(element == null) {
           DSTUtils.debug.error("DSTModify(element):null input");
           throw new DSTException(DSTUtils.bundle.getString("nullInputParams"));
        }
        String elementName = element.getLocalName();
        if(elementName == null || !elementName.equals("Modify")) {
           DSTUtils.debug.error("DSTModify(element):Invalid element name");
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
           DSTUtils.debug.error("DSTModify(element): ResourceIDNode is null" +
           " or more than one resource id is found.");
           throw new DSTException(
           DSTUtils.bundle.getString("invalidResourceID"));
        }
        resourceID = XMLUtils.getElementValue((Element)list.item(0));
        if(resourceID == null) {
           DSTUtils.debug.error("DSTModify(element): ResourceID is null" );
           throw new DSTException(
           DSTUtils.bundle.getString("invalidResourceID"));
        }

        NodeList modificationNodes = element.getElementsByTagNameNS(
                 nameSpaceURI, "Modification"); 
        if(modificationNodes == null || modificationNodes.getLength() == 0) {
           DSTUtils.debug.error("DSTModify(element): Modifications are null" );
           throw new DSTException(
           DSTUtils.bundle.getString("nullModifications"));
        }
        int size = modificationNodes.getLength();
        for(int i=0; i < size; i++) {
            Node node = modificationNodes.item(0);
            DSTModification dstModification = 
                     new DSTModification((Element)node);
            modification.add(dstModification);
        }
    }


    /**
     * Gets the modifications to be performed.
     * 
     * @return List of <code>DSTModification</code> object
     * 
     */
    public java.util.List getModification() {
        return modification;
    }

    /**
     * Gets id attribute 
     * @return 
     * {@link java.lang.String}
     */
    public java.lang.String getId() {
        return id;
    }

    /**
     * Sets id attribute 
     * @param id id attribute value to be set 
     */
    public void setId(java.lang.String id) {
        this.id = id;
    }

    /**
     * Gets the encrypted resource ID.
     * @return encrypted resource ID.
     */
    public com.sun.identity.liberty.ws.disco.EncryptedResourceID 
    getEncryptedResourceID() {
        return encryptedResourceID;
    }

    /**
     * Sets encrypted resource ID 
     * @param resourceID encrypted resource ID to be set
     */
    public void setEncryptedResourceID(
     com.sun.identity.liberty.ws.disco.EncryptedResourceID resourceID) {
        this.encryptedResourceID = resourceID;
    }

    /**
     * Gets resource ID 
     * @return
     * {@link java.lang.String}
     */
    public java.lang.String getResourceID() {
        return resourceID;
    }

    /**
     * Sets the resource ID 
     * @param resourceID  resource ID to be set
     */
    public void setResourceID(String resourceID) {
        this.resourceID = resourceID;
    }

    /**
     * Gets item id attribute 
     * @return 
     * {@link java.lang.String}
     */
    public java.lang.String getItemID() {
        return itemID;
    }

    /**
     * Sets item id attribute 
     * @param itemID item ID to be set
     */
    public void setItemID(java.lang.String itemID) {
        this.itemID = itemID;
    }

    /**
     * Gets the extension property 
     * 
     * @return List of Object
     * 
     */
    public java.util.List getExtension() {
        return extension;
    }

    /**
     * Sets the extension property 
     *
     * @param extensions List of Object to be set
     *
     */
    public void setExtension(java.util.List extensions) {
        if(extensions != null) {
           extension.addAll(extensions);
        }
    }

    /**
     * Gets the name space.
     *
     * @return name space.
     */
    public java.lang.String getNameSpaceURI() {
        return nameSpaceURI;
    }

    /**
     * Sets the name space.
     * @param nameSpace name space URI.
     */
    public void setNameSpaceURI(String nameSpace) {
        this.nameSpaceURI = nameSpace;
    }

    /**
     * Sets the name space prefix.
     * @param prefix name space prefix.
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

        String tempPrefix = "";
        if(includeNS) {
           if(prefix == null) {
              prefix = DSTConstants.DEFAULT_NS_PREFIX;
           }
           tempPrefix = prefix + ":";
        }
        if(declareNS) {
           if(nameSpaceURI == null) {
              DSTUtils.debug.error("DSTModify.toString: Name Space is " +
              "not defined");
              return "";
           }
        }
        StringBuffer sb = new StringBuffer(300);
        sb.append("<").append(tempPrefix).append("Modify");
        if(id != null && id.length() != 0) {
           sb.append(" id=\"").append(id).append("\"");
        }
        if(itemID != null && itemID.length() != 0) {
           sb.append(" itemID=\"").append(itemID).append("\"");
        }
        if(declareNS) {
           sb.append(" xmlns:").append(prefix).append("=\"")
             .append(nameSpaceURI).append("\"")
             .append(" xmlns=\"").append(nameSpaceURI).append("\"");
        }
        sb.append(">");
        if(encryptedResourceID == null) {
           sb.append("<").append(tempPrefix).append("ResourceID").append(">")
             .append(resourceID).append("</").append(tempPrefix)
             .append("ResourceID").append(">");
        } else {
           sb.append(encryptedResourceID.toString(nameSpaceURI));
        }

        Iterator iter = modification.iterator();
        while(iter.hasNext()) {
            DSTModification modification = (DSTModification)iter.next();
            sb.append(modification.toString());
        }
        sb.append("</").append(tempPrefix).append("Modify").append(">");
        if(DSTUtils.debug.messageEnabled()) {
           DSTUtils.debug.message("DSTModify.toString: " + sb.toString());
        }
        return sb.toString(); 
    }

}
