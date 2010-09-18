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
 * $Id: RequestedService.java,v 1.2 2008/06/25 05:47:11 qcheng Exp $
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
 * The class <code>RequestedService</code> enables the requester to specify 
 * that all the resource offerings returned must be offered via a service
 * instance complying with one of the specified service type.
 * <p>The following schema fragment specifies the expected content 
 * within the <code>RequestedService</code> object.
 * <p>
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:liberty:disco:2003-08}ServiceType"/>
 *         &lt;element ref="{urn:liberty:disco:2003-08}Options" minOccurs="0"/>
 *   *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * <p>
 * <pre>
 * In this implementation, the value of Options has the following meanings:
 * When the List of options is null, no Options element will be created;
 * When the List of options is an empty List, or is Collection.EMPTY_LIST,
 *        empty Options element &lt;Options>&lt;/Options> will be created;
 * When the List of options is not empty,
 *        Options element with child Option element(s) will be created.
 * </pre>
 * @supported.all.api
 */
public class RequestedService {

    private List options = null;
    private String serviceType = null;

    /**
     * Constructor.
     * @param options List of String, each is a URI specifying an option the
     *     returned resource offering should support.  
     * @param serviceType URI specifying the type of service to be returned 
     */
    public RequestedService (java.util.List options, 
                             java.lang.String serviceType)
    {
        this.options = options;
        this.serviceType = serviceType;
    }

    /**
     * Constructor.
     * @param elem <code>RequestedService</code> DOM element
     * @exception DiscoveryException if error occurs
     */
    public RequestedService(Element elem) throws DiscoveryException {
        if (elem == null) {
            DiscoUtils.debug.message("RequestedService(Element): null input.");
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("nullInput"));
        }
        String nodeName;
        String nameSpaceURI;
        if (((nodeName = elem.getLocalName()) == null) ||
            (!nodeName.equals("RequestedServiceType")) ||
            ((nameSpaceURI = elem.getNamespaceURI()) == null) ||
            (!nameSpaceURI.equals(DiscoConstants.DISCO_NS)))
        {
            DiscoUtils.debug.message("RequestedService(Element): wrong input");
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
                        DiscoUtils.debug.message("RequestedService(Element): "
                            + "invalid namespace for node " + nodeName);
                    }
                    throw new DiscoveryException(
                        DiscoUtils.bundle.getString("wrongInput"));
                }
                if (nodeName.equals("ServiceType")) {
                    if (serviceType != null) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("RequestedService(Element)"
                                + ": Included more than one ServiceType "
                                + "element.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("moreElement"));
                    }
                    serviceType = XMLUtils.getElementValue((Element)child);
                    if ((serviceType == null) || (serviceType.length() == 0)) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("RequestedService(Element)"
                                + ": missing ServiceType element value.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("emptyElement"));
                    }
                } else if (nodeName.equals("Options")) {
                    if (options != null) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("RequestedService(Element)"
                                + ": Included more than one Options element.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("moreElement"));
                    }
                    options = DiscoUtils.parseOptions((Element) child);
                } else {
                    if (DiscoUtils.debug.messageEnabled()) {
                        DiscoUtils.debug.message("RequestedService(Element): "
                            + "invalid node" + nodeName);
                    }
                    throw new DiscoveryException(
                        DiscoUtils.bundle.getString("wrongInput"));
                }
            }
        }

        if (serviceType == null) {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("RequestedService(Element): missing "
                    + "ServiceType element.");
            }
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("missingServiceType"));
        }
    }

    /**
     * Gets list of options.
     * @return List of options
     * @see #setOptions(List)
     */
    public List getOptions() {
        return options;
    }

    /**
     * Sets options.
     * @param options List of option to be set
     * @see #getOptions()
     */
    public void setOptions(List options) {
        this.options = options;
    }

    /**
     * Gets service type.
     * @return service type String 
     * @see #setServiceType(String)
     */
    public String getServiceType() {
        return serviceType;
    }

    /**
     *  Sets service type.
     * @param serviceType String 
     * @see #getServiceType()
     */
    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    /**
     * Returns string format of object <code>RequestedServiceType</code>.
     *
     * @return formatted string.
     */ 
    public java.lang.String toString() {
        StringBuffer sb = new StringBuffer(1000);
        sb.append("<RequestedServiceType xmlns=\"").
            append(DiscoConstants.DISCO_NS).append("\"><ServiceType>");
        if (serviceType != null) {
            sb.append(serviceType);
        }
        sb.append("</ServiceType>");
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
        sb.append("</RequestedServiceType>");
        return sb.toString();
    }
}
