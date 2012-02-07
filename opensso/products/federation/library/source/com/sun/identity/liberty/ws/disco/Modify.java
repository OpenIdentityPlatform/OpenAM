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
 * $Id: Modify.java,v 1.2 2008/06/25 05:47:10 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.disco;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import org.w3c.dom.*;
import com.sun.identity.liberty.ws.disco.common.DiscoConstants;
import com.sun.identity.liberty.ws.disco.common.DiscoUtils;

/**
 * This class represents a discovery modify request.
 * The following schema fragment specifies the expected content within
 * the <code>Modify</code> object.
 * <pre>
 * &lt;xs:element name="Modify" type="ModifyType"/>
 * &lt;complexType name="ModifyType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;group ref="{urn:liberty:disco:2003-08}ResourceIDGroup"/>
 *         &lt;element name="InsertEntry" type="{urn:liberty:disco:2003-08}InsertEntryType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="RemoveEntry" type="{urn:liberty:disco:2003-08}RemoveEntryType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * @supported.all.api
 */
public class Modify {

    private String id = null;
    private ResourceID resourceID = null;
    private EncryptedResourceID encryptResID = null;
    private List inserts = null;
    private List removes = null;

    /**
     * Constructor.
     * @param resourceID ID for the discovery resource to be modified 
     * @param insertEntry List of insert entries
     * @param removeEntry List of remove entries
     */
    public Modify(ResourceID resourceID,
                  List insertEntry,
                  List removeEntry)
    {
        this.resourceID = resourceID;
        inserts = insertEntry;
        removes = removeEntry;
    } 

    /**
     * Constructor.
     * @param resourceID Encrypted Discovery Resource ID to be modified
     * @param insertEntry List of insert entries
     * @param removeEntry List of remove entries
     */
    public Modify(EncryptedResourceID resourceID,
                   List insertEntry,
                   List removeEntry)
    {
        encryptResID = resourceID;
        inserts = insertEntry;
        removes = removeEntry;
    }

    /**
     * Default constructor.
     */
    public Modify() {} 

    /**
     * Constructor.
     * @param root Modify DOM element
     * @exception DiscoveryException if error occurs
     */
    public Modify(Element root) throws DiscoveryException {
        if (root == null) {
            DiscoUtils.debug.message("Modify(Element): null input.");
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("nullInput"));
        }
        String nodeName;
        String nameSpaceURI;
        if (((nodeName = root.getLocalName()) == null) ||
            (!nodeName.equals("Modify")) ||
            ((nameSpaceURI = root.getNamespaceURI()) == null) ||
            (!nameSpaceURI.equals(DiscoConstants.DISCO_NS)))
        {
            DiscoUtils.debug.message("Modify(Element): wrong input");
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("wrongInput"));
        }

        id = root.getAttribute("id");

        NodeList contentnl = root.getChildNodes();
        Node child;
        for (int i = 0, length = contentnl.getLength(); i < length; i++) {
            child = contentnl.item(i);
            if ((nodeName = child.getLocalName()) != null) {
                nameSpaceURI = ((Element) child).getNamespaceURI();
                if ((nameSpaceURI == null) ||
                    (!nameSpaceURI.equals(DiscoConstants.DISCO_NS)))
                {
                    if (DiscoUtils.debug.messageEnabled()) {
                        DiscoUtils.debug.message("Modify(Element): "
                            + "invalid namespace for node " + nodeName);
                    }
                    throw new DiscoveryException(
                        DiscoUtils.bundle.getString("wrongInput"));
                }
                if (nodeName.equals("ResourceID")) {
                    if ((resourceID != null) || (encryptResID != null)) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("Modify(Element): Included"
                                + " more than one ResourceIDGroup element.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("moreResourceIDGroup"));
                    }
                    resourceID = new ResourceID((Element) child);
                } else if (nodeName.equals("EncryptedResourceID")) {
                    if ((resourceID != null) || (encryptResID != null)) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("Modify(Element): Included"
                                + " more than one ResourceIDGroup element.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("moreResourceIDGroup"));
                    }
                    encryptResID = new EncryptedResourceID((Element) child);
                } else if (nodeName.equals("InsertEntry")) {
                    if (inserts == null) {
                        inserts = new ArrayList();
                    }
                    inserts.add(new InsertEntry((Element) child));
                } else if (nodeName.equals("RemoveEntry")) {
                    if (removes == null) {
                        removes = new ArrayList();
                    }
                    removes.add(new RemoveEntry(
                        ((Element) child).getAttribute("entryID")));
                } else {
                    if (DiscoUtils.debug.messageEnabled()) {
                        DiscoUtils.debug.message("Modify(Element): invalid"
                            + " node" + nodeName);
                    }
                    throw new DiscoveryException(
                        DiscoUtils.bundle.getString("wrongInput"));
                }
            } // if nodeName != null
        } // done for the nl loop

        // make sure there is a ResourceID or EncryptedResourceID
        if ((resourceID == null) && (encryptResID == null)) {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("Modify(Element): missing ResourceID "
                    + "or EncryptedResourceID element.");
            }
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("missingResourceIDGroup"));
        }
    }

    /**
     * Gets the encrypted resource ID for the discovery resource to be modified.
     *
     * @return the encrypted resource ID.
     * @see #setEncryptedResourceID(EncryptedResourceID)
     */
    public EncryptedResourceID getEncryptedResourceID() {
        return encryptResID;
    }

    /**
     * Sets the encrypted resource ID for the discovery resource to be modified.
     *
     * @param value the encrypted resource ID.
     * @see #getEncryptedResourceID()
     */
    public void setEncryptedResourceID(EncryptedResourceID value) {
        encryptResID = value;
    }

    /**
     * Gets the resource ID for the discovery resource to be modified.
     *
     * @return resource ID for the discovery resource to be modified.
     * @see #setResourceID(ResourceID)
     */
    public ResourceID getResourceID() {
        return resourceID;
    }

    /**
     * Sets resource ID for the discovery resource to be modified.
     * @param resourceID resource ID for the discovery resource to be modified. 
     * @see #getResourceID()
     */
    public void setResourceID(ResourceID resourceID) {
        this.resourceID = resourceID;
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
     * Gets the value of the <code>RemoveEntry</code> property.
     *
     * @return List of <code>RemoveEntry</code> objects
     * @see #setRemoveEntry(List)
     */
    public List getRemoveEntry() {
        return removes;
    }

    /**
     * Sets the value of the <code>RemoveEntry</code> property.
     *
     * @param removes List of <code>RemoveEntry</code> object.
     * @see #getRemoveEntry()
     */
    public void setRemoveEntry(List removes) {
        this.removes = removes;
    }

    /**
     * Gets the value of the <code>InsertEntry</code> property.
     *
     * @return List of <code>InsertEntry</code> object
     * @see #setInsertEntry(List)
     */
    public java.util.List getInsertEntry() {
        return inserts;
    }

    /**
     * Sets the value of the <code>InsertEntry</code> property.
     *
     * @param inserts List of <code>InsertEntry</code> object.
     * @see #getInsertEntry()
     */
    public void setInsertEntry(List inserts) {
        this.inserts = inserts;
    }
     
    /**
     * Gets string format.
     *
     * @return formatted String.
     */ 
    public java.lang.String toString() {
        StringBuffer sb = new StringBuffer(1200);
        sb.append("<Modify xmlns=\"").append(DiscoConstants.DISCO_NS).
                append("\"");
        if ((id != null) && id.length() != 0) {
            sb.append(" id=\"").append(id).append("\"");
        }
        sb.append(">");
        if (resourceID != null) {
            sb.append(resourceID.toString());
        } else if (encryptResID != null) {
            sb.append(encryptResID.toString());
        }
        if (inserts != null) {
            Iterator iter = inserts.iterator();
            while (iter.hasNext()) {
                sb.append(((InsertEntry) iter.next()).toString());
            }
        }
        if (removes != null) {
            Iterator iter1 = removes.iterator();
            while (iter1.hasNext()) {
                sb.append(((RemoveEntry) iter1.next()).toString());
            }
        }
        sb.append("</Modify>");
        return sb.toString();
    }
}
