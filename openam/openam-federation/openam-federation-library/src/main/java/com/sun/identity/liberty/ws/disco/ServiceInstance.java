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
 * $Id: ServiceInstance.java,v 1.2 2008/06/25 05:47:11 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.disco;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.w3c.dom.*;

import com.sun.identity.liberty.ws.disco.common.DiscoConstants;
import com.sun.identity.liberty.ws.disco.common.DiscoUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * The class <code>ServiceInstance</code> describes a web service at a 
 * distinct protocol endpoint.
 * <p>The following schema fragment specifies the expected content 
 * within the <code>ServiceInstance</code> object.
 * <p>
 * <pre>
 * &lt;xs:element name="ServiceInstance" type="ServiceInstanceType"/>
 * &lt;complexType name="ServiceInstanceType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:liberty:disco:2003-08}ServiceType"/>
 *         &lt;element name="ProviderID" type="{urn:liberty:metadata:2003-08}entityIDType"/>
 *         &lt;element name="Description" type="{urn:liberty:disco:2003-08}DescriptionType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * @supported.all.api
 */
public class ServiceInstance {

    private String serviceType = null;
    private String providerID = null;
    private List descriptions = null;

    /**
     * Default Constructor.
     */
    public ServiceInstance (){}

    /**
     * Constructor
     * @param serviceType service type
     * @param providerID provider ID
     * @param descriptions List of Description objects
     */
    public ServiceInstance (String serviceType,
                            String providerID,
                            java.util.List descriptions)
    {
        this.serviceType = serviceType;
        this.providerID = providerID;
        this.descriptions = descriptions;
    }

    /**
     * Constructor.
     * @param elem <code>ServiceInstance</code> DOM element.
     * @exception DiscoveryException if error occurs.
     */
    public ServiceInstance(Element elem) throws DiscoveryException {
        if (elem == null) {
            DiscoUtils.debug.message("ServiceInstance(Element): null input.");
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("nullInput"));
        }
        String nodeName;
        String nameSpaceURI;
        if (((nodeName = elem.getLocalName()) == null) ||
            (!nodeName.equals("ServiceInstance")) ||
            ((nameSpaceURI = elem.getNamespaceURI()) == null) ||
            (!nameSpaceURI.equals(DiscoConstants.DISCO_NS)))
        {
            DiscoUtils.debug.message("ServiceInstance(Element): wrong input");
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("wrongInput"));
        }

        NodeList contentnl = elem.getChildNodes();
        Node child;
        for (int i = 0, length = contentnl.getLength(); i < length; i++) {
            child = contentnl.item(i);
            if ((nodeName = child.getLocalName()) != null) {
                nameSpaceURI = ((Element) child).getNamespaceURI();
                if ((nameSpaceURI == null) ||
                    (!nameSpaceURI.equals(DiscoConstants.DISCO_NS)))
                {
                    if (DiscoUtils.debug.messageEnabled()) {
                        DiscoUtils.debug.message("ServiceInstance(Element): "
                            + "invalid namespace for node " + nodeName);
                    }
                    throw new DiscoveryException(
                        DiscoUtils.bundle.getString("wrongInput"));
                }
                if (nodeName.equals("ServiceType")) {
                    if (serviceType != null) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("ServiceInstance(Element)"
                                + ": Included more than one ServiceType "
                                + "element.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("moreElement"));
                    }
                    serviceType = XMLUtils.getElementValue((Element) child);
                    if ((serviceType == null) || (serviceType.length() == 0)) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("ServiceInstance(Element)"
                                + ": missing ServiceType element value.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("emptyElement"));
                    }
                } else if (nodeName.equals("ProviderID")) {
                    if (providerID != null) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("ServiceInstance(Element)"
                                + ": Included more than one ProviderID.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("moreElement"));
                    }
                    providerID = XMLUtils.getElementValue((Element) child);
                    if ((providerID == null) || (providerID.length() == 0)) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("ServiceInstance(Element)"
                                + ": missing ProviderID element value.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("emptyElement"));
                    }
                } else if (nodeName.equals("Description")) {
                    if (descriptions == null) {
                        descriptions = new ArrayList();
                    }
                    descriptions.add(new Description((Element) child));
                } else {
                    if (DiscoUtils.debug.messageEnabled()) {
                        DiscoUtils.debug.message("ServiceInstance(Element): "
                            + "invalid node" + nodeName);
                    }
                    throw new DiscoveryException(
                        DiscoUtils.bundle.getString("wrongInput"));
                }
            }
        }

        if (serviceType == null) {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("ServiceInstance(Element): missing "
                    + "ServiceInstance element.");
            }
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("missingServiceInstance"));
        }

        if (providerID == null) {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("ServiceInstance(Element): missing "
                    + "ProviderID element.");
            }
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("missingProviderID"));
        }

        if ((descriptions == null) || (descriptions.size() < 1)) {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("ServiceInstance(Element): missing "
                    + "Description element.");
            }
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("missingDescription"));
        }
    }

    /**
     * Gets provider ID of the service instance.
     *
     * @return provider ID of the service instance.
     * @see #setProviderID(String)
     */
    public String getProviderID() {
        return providerID;
    }

    /**
     * Sets provider ID. 
     *
     * @param value provider ID.
     * @see #getProviderID()
     */
    public void setProviderID(String value) {
        providerID = value;
    }

    /**
     * Gets the service descriptions.
     * 
     * @return List of Description objects
     * @see #setDescription(List)
     */
    public List getDescription() {
        return descriptions;
    }

    /**
     * Sets the service descriptions.
     * 
     * @param desc List of Description objects.
     * @see #getDescription()
     */
    public void setDescription(List desc) {
        descriptions = desc;
    }

    /**
     * Gets service type.
     *
     * @return service type.
     * @see #setServiceType(String)
     */
    public String getServiceType() {
        return serviceType;
    }

    /**
     * Sets service type. 
     *
     * @param value service type.
     * @see #getServiceType()
     */
    public void setServiceType(String value) {
        serviceType = value;
    }

    /**
     * Returns string format of object <code>ServiceInstance</code>.
     *
     * @return formatted string.
     */ 
    public String toString() {
        StringBuffer sb = new StringBuffer(1000);
        sb.append("<ServiceInstance xmlns=\"").append(DiscoConstants.DISCO_NS).
                append("\"><ServiceType>").append(serviceType).
                append("</ServiceType><ProviderID>").
                append(providerID).append("</ProviderID>");
        if (descriptions != null) {
            Iterator iter = descriptions.iterator();
            while (iter.hasNext()) {
                sb.append(((Description) iter.next()).toString());
            }
        }
        sb.append("</ServiceInstance>");
        return sb.toString();
    }
}
