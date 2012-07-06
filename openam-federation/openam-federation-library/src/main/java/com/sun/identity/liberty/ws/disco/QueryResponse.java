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
 * $Id: QueryResponse.java,v 1.2 2008/06/25 05:47:10 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.disco;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.w3c.dom.*;

import com.sun.identity.liberty.ws.common.Status;
import com.sun.identity.liberty.ws.disco.common.DiscoConstants;
import com.sun.identity.liberty.ws.disco.common.DiscoUtils;
import com.sun.identity.liberty.ws.security.SecurityAssertion;
import com.sun.identity.saml.common.SAMLException;

/**
 * The class <code>QueryResponse</code> represents a response for a discovery
 * query request.
 * The following schema fragment specifies the expected content within the
 * <code>QueryResponse</code> object.
 * <pre>
 * &lt;complexType name="QueryResponseType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:liberty:disco:2003-08}Status"/>
 *         &lt;element ref="{urn:liberty:disco:2003-08}ResourceOffering" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="Credentials" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;any/>
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
public class QueryResponse {

    private String id = null;
    private Status status = null;
    private List offerings = null;
    private List creds = null;

    /**
     * Constructor.
     * @param root <code>QueryResponse</code> DOM element.
     * @exception DiscoveryException if error occurs.
     */
    public QueryResponse(Element root) throws DiscoveryException {
        if (root == null) {
            DiscoUtils.debug.message("QueryResponse(Element): null input.");
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("nullInput"));
        }
        String nodeName;
        String nameSpaceURI;
        if (((nodeName = root.getLocalName()) == null) ||
            (!nodeName.equals("QueryResponse")) ||
            ((nameSpaceURI = root.getNamespaceURI()) == null) ||
            (!nameSpaceURI.equals(DiscoConstants.DISCO_NS)))
        {
            DiscoUtils.debug.message("QueryResponse(Element): wrong input");
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("wrongInput"));
        }

        id = root.getAttribute("id");

        NodeList contentnl = root.getChildNodes();
        Node child;
        boolean foundCreds = false;
        for (int i = 0, length = contentnl.getLength(); i < length; i++) {
            child = contentnl.item(i);
            if ((nodeName = child.getLocalName()) != null) {
                nameSpaceURI = ((Element) child).getNamespaceURI();
                if ((nameSpaceURI == null) ||
                    (!nameSpaceURI.equals(DiscoConstants.DISCO_NS)))
                {
                    if (DiscoUtils.debug.messageEnabled()) {
                        DiscoUtils.debug.message("QueryResponse(Element): "
                            + "invalid namespace for node " + nodeName);
                    }
                    throw new DiscoveryException(
                        DiscoUtils.bundle.getString("wrongInput"));
                }
                if (nodeName.equals("Status")) {
                    if (status != null) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("QueryResponse(Element): "
                                + "included more than one Status.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("moreElement"));
                    }
                    status = DiscoUtils.parseStatus((Element) child);
                } else if (nodeName.equals("ResourceOffering")) {
                    if (offerings == null) {
                        offerings = new ArrayList();
                    }
                    offerings.add(new ResourceOffering((Element) child));
                } else if (nodeName.equals("Credentials")) {
                    if (foundCreds) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("QueryResponse(Element): "
                                + "included more than one Credentials.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("moreElement"));
                    }
                    foundCreds = true;
                    parseCreds((Element) child);
                } else {
                    if (DiscoUtils.debug.messageEnabled()) {
                        DiscoUtils.debug.message("QueryResponse(Element): "
                            + "invalid node" + nodeName);
                    }
                    throw new DiscoveryException(
                        DiscoUtils.bundle.getString("wrongInput"));
                }
            }
        }

        if (status == null) {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("QueryResponse(Element): missing "
                    + "Status.");
            }
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("missingStatus"));
        }
    }

    private void parseCreds(Element elem) throws DiscoveryException {
        NodeList contentnl = elem.getChildNodes();
        Node child;
        String nodeName;
        SecurityAssertion assertion;
        for (int i = 0, length = contentnl.getLength(); i < length; i++) {
            child = contentnl.item(i);
            if ((nodeName = child.getLocalName()) != null) {
                try {
                    assertion = new SecurityAssertion((Element) child);
                } catch (SAMLException se) {
                    if (DiscoUtils.debug.messageEnabled()) {
                        DiscoUtils.debug.message("QueryResponse(Element): "
                            + "Exception thrown when parsing Credentials:", se);
                    }
                    throw new DiscoveryException(
                        DiscoUtils.bundle.getString("wrongCredential"));
                }
                if (creds == null) {
                    creds = new ArrayList();
                }
                creds.add(assertion);
            }
        }
    }

    /**
     * Default constructor.
     */
    public QueryResponse () {}

    /**
     * Constructor.
     *
     * @param status Status of the response.
     */
    public QueryResponse(Status status) {
        this.status = status;
    }

    /**
     * Gets status of the query response.
     *
     * @return status of the query response.
     * @see #setStatus(com.sun.identity.liberty.ws.common.Status)
     */
    public com.sun.identity.liberty.ws.common.Status getStatus() {
        return status;
    }

    /**
     * Sets the Status of the query response.
     *
     * @param status the Status of the query response.
     * @see #getStatus()
     */
    public void setStatus(com.sun.identity.liberty.ws.common.Status status) {
        this.status = status;
    }

    /**
     * Gets the returned <code>ResourceOffering</code>. 
     * 
     * @return List of <code>ResourceOffering</code> objects
     * @see #setResourceOffering(List)
     */
    public java.util.List getResourceOffering() {
        return offerings;
    }

    /**
     * Sets <code>ResourceOffering</code> to return.
     * 
     * @param offerings List of <code>ResourceOffering</code> objects
     * @see #getResourceOffering()
     */
    public void setResourceOffering(List offerings) {
        this.offerings = offerings;
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
     * Gets credentials. 
     * @return List of
     *  <code>com.sun.identity.liberty.ws.security.SecurityAssertion</code>
     *  objects.
     * @see #setCredentials(List)
     */
    public List getCredentials() {
        return creds;
    }

    /**
     * Sets credentials. 
     * @param credentials List of
     *  <code>com.sun.identity.liberty.ws.security.SecurityAssertion</code>
     *  objects.
     * @see #getCredentials()
     */
    public void setCredentials(List credentials) {
        creds = credentials;
    }


    /**
     * Returns formatted string of the <code>QueryResponse</code>.
     *
     * @return formatted string of the <code>QueryResponse</code>.
     */ 
    public java.lang.String toString() {
        StringBuffer sb = new StringBuffer(2000);
        sb.append("<QueryResponse xmlns=\"").append(DiscoConstants.DISCO_NS).
            append("\"");
        if ((id != null) && id.length() != 0) {
            sb.append(" id=\"").append(id).append("\"");
        }
        sb.append(">");
        if (status != null) {
            sb.append(status.toString());
        }
        if (offerings != null) {
            Iterator iter = offerings.iterator();
            while (iter.hasNext()) {
                sb.append(((ResourceOffering) iter.next()).toString());
            }
        }
        if (creds != null) {
            sb.append("<Credentials xmlns=\"").append(DiscoConstants.DISCO_NS).
                append("\">");
            Iterator iter2 = creds.iterator();
            while (iter2.hasNext()) {
                sb.append(iter2.next().toString());
            }
            sb.append("</Credentials>");
        }
        sb.append("</QueryResponse>");
        return sb.toString();
    }
}
