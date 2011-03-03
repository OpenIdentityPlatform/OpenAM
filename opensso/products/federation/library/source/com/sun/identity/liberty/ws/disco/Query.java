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
 * $Id: Query.java,v 1.2 2008/06/25 05:47:10 qcheng Exp $
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
 * The class <code>Query</code> represents a discovery Query object.
 * The following schema fragment specifies the expected content within the
 * <code>Query</code> object.
 * <pre>
 * &lt;xs:element name="Query" type="Query"/> 
 * &lt;complexType name="Query">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;group ref="{urn:liberty:disco:2003-08}ResourceIDGroup"/>
 *         &lt;element name="RequestedServiceType" maxOccurs="unbounded" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element ref="{urn:liberty:disco:2003-08}ServiceType"/>
 *                   &lt;element ref="{urn:liberty:disco:2003-08}Options" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * @supported.all.api
 */
public class Query {

    private String id = null;
    private ResourceID resourceID = null;
    private EncryptedResourceID encryptResID = null;
    private List requestedService = null;

    /**
     * Constructor.
     * @param resourceID resource ID of the discovery resource to be queried.
     * @param RequestedService List of <code>RequestService</code> object.
     */
    public Query (ResourceID resourceID, java.util.List RequestedService) {
        this.resourceID = resourceID;
        requestedService = RequestedService;
    }

    /**
     * Constructor.
     * @param resourceID encrypted resource ID of the discovery resource 
     *                to be queried.
     * @param RequestedService List of <code>RequestService</code> object.
     */
    public Query (EncryptedResourceID resourceID, 
                  java.util.List RequestedService) 
    {
        encryptResID = resourceID;
        requestedService = RequestedService;
    }

    /**
     * Constructor.
     * @param root Query in DOM Element
     * @exception DiscoveryException if error occurs
     */
    public Query (Element root) throws DiscoveryException {
        if (root == null) {
            DiscoUtils.debug.message("Query(Element): null input.");
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("nullInput"));
        }
        String nodeName;
        String nameSpaceURI;
        if (((nodeName = root.getLocalName()) == null) ||
            (!nodeName.equals("Query")) ||
            ((nameSpaceURI = root.getNamespaceURI()) == null) ||
            (!nameSpaceURI.equals(DiscoConstants.DISCO_NS)))
        {
            DiscoUtils.debug.message("Query(Element): wrong input");
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("wrongInput"));
        }

        // attribute id
        id = root.getAttribute("id");

        // loop to get ResourceID or EncryptedResourceID
        // 0 or more RequestedService
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
                        DiscoUtils.debug.message("Query(Element): "
                            + "invalid namespace for node " + nodeName);
                    }
                    throw new DiscoveryException(
                        DiscoUtils.bundle.getString("wrongInput"));
                }
                if (nodeName.equals("ResourceID")) {
                    if ((resourceID != null) || (encryptResID != null)) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("Query(Element): Included"
                                + " more than one ResourceIDGroup element.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("moreResourceIDGroup"));
                    }
                    resourceID = new ResourceID((Element) child);
                } else if (nodeName.equals("EncryptedResourceID")) {
                    if ((resourceID != null) || (encryptResID != null)) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("Query(Element): Included"
                                + " more than one ResourceIDGroup element.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("moreResourceIDGroup"));
                    }
                    encryptResID = new EncryptedResourceID((Element) child);
                } else if (nodeName.equals("RequestedServiceType")) {
                    if (requestedService == null) {
                        requestedService = new ArrayList();
                    }
                    requestedService.add(new RequestedService((Element) child));
                } else {
                    if (DiscoUtils.debug.messageEnabled()) {
                        DiscoUtils.debug.message("Query(Element): invalid"
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
                DiscoUtils.debug.message("Query(Element): missing ResourceID "
                    + "or EncryptedResourceID element.");
            }
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("missingResourceIDGroup"));
        }
    }

    /**
     * Gets id attribute.
     *
     * @return id attribute.
     * @see #setId(String)
     */
    public java.lang.String getId() {
        return id;
    }

    /**
     * Sets id attribute.
     *
     * @param id id attribute.
     * @see #getId()
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the encrypted resource ID of the discovery resource to be queried.
     *
     * @return the encrypted resource ID of the discovery resource to be
     *         queried.
     * @see #setEncryptedResourceID(EncryptedResourceID)
     */
    public EncryptedResourceID getEncryptedResourceID() {
        return encryptResID;
    }

    /**
     * Sets the encrypted resource ID of the discovery resource to be queried.
     *
     * @param value the encrypted resource ID.
     * @see #getEncryptedResourceID()
     */
    public void setEncryptedResourceID(EncryptedResourceID value) {
        encryptResID = value;
    }

    /**
     * Gets the resource ID of the discovery resource to be queried.
     *
     * @return the resource ID of the discovery resource to be queried.
     * @see #setResourceID(ResourceID)
     */
    public ResourceID getResourceID() {
        return resourceID;
    }

    /**
     * Sets the resource ID of the discovery resource to be queried
     *
     * @param resourceID the resource ID of the discovery resource to be
     *        queried.
     * @see #getResourceID()
     */
    public void setResourceID(ResourceID resourceID) {
        this.resourceID = resourceID;
    }

    /**
     * Gets the list of the requested service types.
     *
     * @return the list of the requested service types.
     * @see #setRequestedServiceType(List)
     */
    public java.util.List getRequestedServiceType() {
        return requestedService;
    }

    /**
     * Sets the list of the requested service types.
     *
     * @param requestedService the list of the requested service types to be
     *        set.
     * @see #getRequestedServiceType()
     */
    public void setRequestedServiceType(List requestedService) {
        this.requestedService = requestedService;
    }

    /**
     * Returns formatted string of the <code>Query</code> object.
     *
     * @return formatted string of the <code>Query</code> object.
     */ 
    public java.lang.String toString() {
        StringBuffer sb = new StringBuffer(1000);
        sb.append("<Query xmlns=\"").append(DiscoConstants.DISCO_NS).
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
        if ((requestedService != null) && !requestedService.isEmpty()) {
            Iterator iter = requestedService.iterator();
            while (iter.hasNext()) {
                sb.append(((RequestedService) iter.next()).toString());
            }
        }
        sb.append("</Query>");
        return sb.toString();
    }
}
