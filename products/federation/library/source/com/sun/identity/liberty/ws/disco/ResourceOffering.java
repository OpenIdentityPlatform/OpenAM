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
 * $Id: ResourceOffering.java,v 1.2 2008/06/25 05:47:11 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.disco;


import java.util.Iterator;
import java.util.List;

import org.w3c.dom.*;

import com.sun.identity.liberty.ws.disco.common.DiscoConstants;
import com.sun.identity.liberty.ws.disco.common.DiscoUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * The class <code>ResourceOffering</code> associates a resource with a service
 * instance that provides access to that resource.
 * <p>The following schema fragment specifies the expected content within the
 * <code>ResourceOffering</code> object.
 * <p>
 * <pre>
 * &lt;complexType name="ResourceOfferingType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;group ref="{urn:liberty:disco:2003-08}ResourceIDGroup"/>
 *         &lt;element name="ServiceInstance" type="{urn:liberty:disco:2003-08}ServiceInstanceType"/>
 *         &lt;element ref="{urn:liberty:disco:2003-08}Options" minOccurs="0"/>
 *         &lt;element name="Abstract" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="entryID" type="{urn:liberty:disco:2003-08}IDType" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * An example of the <code>ResourceOffering</code> is :
 * <pre>
 * &lt;ResourceOffering xmlns="urn:liberty:disco:2003-08">
 *     &lt;ResourceID>http://profile-provider.com/profiles/l4m0B82k15csaUxs&lt;/ResourceID>
 *     &lt;ServiceInstance xmlns="urn:liberty:disco:2003-08">
 *         &lt;ServiceType>urn:liberty:idpp:2003-08&lt;/ServiceType>
 *         &lt;ProviderID>http://profile-provider.com/&lt;/ProviderID>
 *         &lt;Description>
 *             &lt;SecurityMechID>urn:liberty:disco:2003-08:anonymous&lt;/SecurityMechID>
 *             &lt;SecurityMechID>urn:liberty:security:2003-08:x509&lt;/SecurityMechID>
 *             &lt;SecurityMechID>urn:liberty:security:2003-08:saml&lt;/SecurityMechID>
 *             &lt;Endpoint>https://soap.profile-provider.com/soap/&lt;/Endpoint>
 *         &lt;/Description>
 *         &lt;Description>
 *             &lt;SecurityMechID>urn:ietf:rfc:2246&lt;/SecurityMechID>
 *             &lt;Endpoint>https://soap-auth.profile-provider.com/soap/&lt;/Endpoint>
 *         &lt;/Description>
 *      &lt;/ServiceInstance>
 *      &lt;Options>
 *          &lt;Option>urn:liberty:idpp&lt;/Option>
 *          &lt;Option>urn:liberty:idpp:cn&lt;/Option>
 *          &lt;Option>urn:liberty:idpp:can&lt;/Option>
 *          &lt;Option>urn:liberty:idpp:can:cn&lt;/Option>
 *      &lt;/Options>
 *      &lt;Abstract>
 *          This is a personal profile containing common name information. 
 *      &lt;/Abstract>
 * &lt;/ResourceOffering>
 * </pre>
 * 
 * @supported.all.api
 */
public class ResourceOffering {

    private String entryID = null;
    private ResourceID resourceID = null;
    private EncryptedResourceID encryptResID = null;
    private ServiceInstance serviceInstance = null;
    private List options = null;
    private String abs = null;

    /**
     * Constructor.
     * @param resourceID ID for the resource.
     * @param serviceInstance service instance.
     */
    public ResourceOffering (ResourceID resourceID, 
                             ServiceInstance serviceInstance)
    {
        this.resourceID = resourceID;
        this.serviceInstance = serviceInstance;
    }

    /**
     * Constructor.
     *
     * @param resourceID Encrypted Resource ID.
     * @param serviceInstance service instance.
     */
    public ResourceOffering (EncryptedResourceID resourceID, 
                             ServiceInstance serviceInstance)
    {
        encryptResID = resourceID;
        this.serviceInstance = serviceInstance;
    }

    /**
     * Constructor.
     * @param elem <code>ResourceOffering</code> DOM element.
     * @exception DiscoveryException if error occurs.
     */
    public ResourceOffering(Element elem) throws DiscoveryException {
        if (elem == null) {
            DiscoUtils.debug.message("ResourceOffering(Element): null input.");
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("nullInput"));
        }
        String nodeName = null;
        String nameSpaceURI = null;
        if (((nodeName = elem.getLocalName()) == null) ||
            (!nodeName.equals("ResourceOffering")) ||
            ((nameSpaceURI = elem.getNamespaceURI()) == null) ||
            (!nameSpaceURI.equals(DiscoConstants.DISCO_NS)))
        {
            DiscoUtils.debug.message("ResourceOffering(Element): wrong input");
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("wrongInput"));
        }

        entryID = elem.getAttribute("entryID");

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
                        DiscoUtils.debug.message("ResourceOffering(Element): "
                            + "invalid namespace for node " + nodeName);
                    }
                    throw new DiscoveryException(
                        DiscoUtils.bundle.getString("wrongInput"));
                }
                if (nodeName.equals("ResourceID")) {
                    if ((resourceID != null) || (encryptResID != null)) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("ResourceOffering(Element)"
                                + ": Included more than one ResourceIDGroup "
                                + "element.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("moreResourceIDGroup"));
                    }
                    try {
                        resourceID = new ResourceID((Element) child);
                    } catch (DiscoveryException de) {
                        DiscoUtils.debug.error("ResourceOffering(Element):",de);
                    }
                } else if (nodeName.equals("EncryptedResourceID")) {
                    if ((resourceID != null) || (encryptResID != null)) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("ResourceOffering(Element)"
                                + ": Included more than one ResourceIDGroup "
                                + "element.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("moreResourceIDGroup"));
                    }
                    encryptResID = new EncryptedResourceID((Element) child);
                } else if (nodeName.equals("ServiceInstance")) {
                    if (serviceInstance != null) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("ResourceOffering(Element)"
                                + ": Included more than one ServiceInstance.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("moreElement"));
                    }
                    serviceInstance = new ServiceInstance((Element) child);
                } else if (nodeName.equals("Options")) {
                    if (options != null) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("ResourceOffering(Element)"
                                + ": Included more than one Options.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("moreElement"));
                    }
                    options = DiscoUtils.parseOptions((Element) child);
                } else if (nodeName.equals("Abstract")) {
                    if (abs != null) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("ResourceOffering(Element)"
                                + ": Included more than one Abstract.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("moreElement"));
                    }
                    abs = XMLUtils.getElementValue((Element) child);
                } else {
                    if (DiscoUtils.debug.messageEnabled()) {
                        DiscoUtils.debug.message("ResourceOffering(Element): "
                            + "invalid node" + nodeName);
                    }
                    throw new DiscoveryException(
                        DiscoUtils.bundle.getString("wrongInput"));
                }
            }
        }            

/*
        if ((resourceID == null) && (encryptResID == null)) {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("ResourceOffering(Element): missing "
                    + "ResourceID or EncryptedResourceID element.");
            }
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("missingResourceIDGroup"));
        }

        if (serviceInstance == null) {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("ResourceOffering(Element): missing "
                    + "ServiceInstance element.");
            }
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("missingServiceInstance"));
        }
*/

    }

    /**
     * Gets options of the resource offering, which expresses the options 
     * available for the resource offering, that is provides hints to a
     * potential requester whether certain data or operations may be available
     * with a particular resource offering.
     *
     * @return List of options as String
     * @see #setOptions(List)
     */
    public List getOptions() {
        return options;
    }

    /**
     * Sets options.
     * @param options List of options as String 
     * @see #getOptions()
     */
    public void setOptions(List options) {
        this.options = options;
    }

    /**
     * Gets encrypted resource ID.
     *
     * @return encrypted resource ID.
     * @see #setEncryptedResourceID(EncryptedResourceID)
     */
    public EncryptedResourceID getEncryptedResourceID() {
        return encryptResID;
    }

    /**
     * Sets encrypted resource ID.
     *
     * @param resourceID <code>EncryptedResourceID</code> to be set
     * @see #getEncryptedResourceID()
     */
    public void setEncryptedResourceID(EncryptedResourceID resourceID) {
        encryptResID = resourceID;
    }

    /**
     * Gets resource ID.
     * @return resource ID.
     * @see #setResourceID(ResourceID)
     */
    public ResourceID getResourceID() {
        return resourceID;
    }

    /**
     * Sets resource ID.
     *
     * @param resourceID resource ID. 
     * @see #getResourceID()
     */
    public void setResourceID(ResourceID resourceID) {
        this.resourceID = resourceID;
    }

    /**
     * Gets entry ID.
     *
     * @return entry ID.
     * @see #setEntryID(String)
     */
    public String getEntryID() {
        return entryID;
    }

    /**
     * Sets entry ID. 
     * @param value of the id
     * @see #getEntryID()
     */
    public void setEntryID(String value) {
        entryID = value;
    }

    /**
     * Gets service instance.
     *
     * @return service instance.
     * @see #setServiceInstance(ServiceInstance)
     */
    public ServiceInstance getServiceInstance() {
        return serviceInstance;
    }

    /**
     * Sets service instance.
     *
     * @param value service instance.
     * @see #getServiceInstance()
     */
    public void setServiceInstance(ServiceInstance value) {
        serviceInstance = value;
    }

    /**
     * Gets abstract of the resource offering 
     *
     * @return abstract of the resource offering.
     * @see #setAbstract(String)
     */
    public String getAbstract() {
        return abs;
    }

    /**
     * Sets abstract.
     *
     * @param value abstract of the resource offering.
     * @see #getAbstract()
     */
    public void setAbstract(String value) {
        abs = value;
    }

    /**
     * Returns string representation of object <code>ResourceOffering</code>.
     *
     * @return string representation
     */
    public String toString() {
        // entryID, resIDgroup, service instance, options, Abstract
        StringBuffer sb = new StringBuffer(1000);
        sb.append("<ResourceOffering xmlns=\"").append(DiscoConstants.DISCO_NS).
                append("\"");
        if ((entryID != null) && entryID.length() != 0) {
            sb.append(" entryID=\"").append(entryID).append("\"");
        }
        sb.append(">");
        if (resourceID != null) {
            sb.append(resourceID.toString());
        } else if (encryptResID != null) {
            sb.append(encryptResID.toString());
        }
        if (serviceInstance != null) {
            sb.append(serviceInstance.toString());
        }
        if (options != null) {
            sb.append("<Options>");
            if (!options.isEmpty()) {
                Iterator iter = options.iterator();
                String option = null;
                while (iter.hasNext()) {
                    option = (String) iter.next();
                    if ((option != null) && option.length() != 0) {
                        sb.append("<Option>").append(option).
                                append("</Option>");
                    }
                }
            }
            sb.append("</Options>");
        }
        if ((abs != null) && abs.length() != 0) {
            sb.append("<Abstract>").append(abs).append("</Abstract>");
        }
        sb.append("</ResourceOffering>");
        return sb.toString();
    }
}
