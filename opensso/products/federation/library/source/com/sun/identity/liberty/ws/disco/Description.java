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
 * $Id: Description.java,v 1.2 2008/06/25 05:47:10 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.disco;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import javax.xml.namespace.QName;

import org.w3c.dom.*;

import com.sun.identity.liberty.ws.disco.common.DiscoConstants;
import com.sun.identity.liberty.ws.disco.common.DiscoUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * The class <code>Description</code> represents a 
 * Description Type of a service instance.
 * <p>The following schema fragment specifies the expected content within the
 * <code>Description</code> object.
 * <p>
 * <pre>
 * &lt;complexType name="DescriptionType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="SecurityMechID" type="{http://www.w3.org/2001/XMLSchema}anyURI" maxOccurs="unbounded"/>
 *         &lt;element name="CredentialRef" type="{http://www.w3.org/2001/XMLSchema}IDREF" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;choice>
 *           &lt;group ref="{urn:liberty:disco:2003-08}WsdlRef"/>
 *           &lt;group ref="{urn:liberty:disco:2003-08}BriefSoapHttpDescription"/>
 *         &lt;/choice>
 *       &lt;/sequence>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * @supported.all.api
 */
public class Description {

    private String id = null;
    private List mechID = null;
    private List credentialRef = new ArrayList();
    private String soapEndpoint = null;
    private String soapAction = null;
    private QName serviceNameRef = null;
    private String wsdlURI = null;

    /**
     * Default constructor.
     */
     public Description() {}

    /**
     * Constructor.
     * @param securityMechID List of supported security mechanism ID as String
     * @param credentialRef List of credential references
     * @param endPoint SOAP endpoint URI
     */
    public Description (java.util.List securityMechID,
                        java.util.List credentialRef,
                        String endPoint)
    {
        mechID = securityMechID;
        this.credentialRef = credentialRef;
        soapEndpoint = endPoint;
    }

    /**
     * Constructs a Description object from DOM element.
     * @param elem DOM Element of Description.
     * @exception DiscoveryException if error occurs.
     */
    public Description(Element elem) throws DiscoveryException {
        if (elem == null) {
            DiscoUtils.debug.message("Description(Element): null input.");
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("nullInput"));
        }
        String nodeName;
        String nameSpaceURI;
        if (((nodeName = elem.getLocalName()) == null) ||
            (!nodeName.equals("Description")) ||
            ((nameSpaceURI = elem.getNamespaceURI()) == null) ||
            (!nameSpaceURI.equals(DiscoConstants.DISCO_NS)))
        {
            DiscoUtils.debug.message("Description(Element): wrong input");
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("wrongInput"));
        }

        id = elem.getAttribute("id");

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
                        DiscoUtils.debug.message("Description(Element): "
                            + "invalid namespace for node " + nodeName);
                    }
                    throw new DiscoveryException(
                        DiscoUtils.bundle.getString("wrongInput"));
                }
                if (nodeName.equals("SecurityMechID")) {
                    String mID = XMLUtils.getElementValue((Element) child);
                    if ((mID == null) || (mID.length() == 0)) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("Description(Element): "
                                + "missing SecurityMechID value.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("emptyElement"));
                    }
                    if (mechID == null) {
                        mechID = new ArrayList();
                    }
                    mechID.add(mID);
                } else if (nodeName.equals("CredentialRef")) {
                    String ref = XMLUtils.getElementValue((Element) child);
                    if ((ref == null) || (ref.length() == 0)) {
                        if (DiscoUtils.debug.messageEnabled()) {
                            DiscoUtils.debug.message("Description(Element): "
                                + "missing CredentialRef value.");
                        }
                        throw new DiscoveryException(
                            DiscoUtils.bundle.getString("emptyElement"));
                    }
                    if (credentialRef == null) {
                        credentialRef = new ArrayList();
                    }
                    credentialRef.add(ref);
                } else if (nodeName.equals("Endpoint")) {
                    parseEndpoint((Element) child);
                } else if (nodeName.equals("SoapAction")) {
                    parseSoapAction((Element) child);
                } else if (nodeName.equals("WsdlURI")) {
                    parseWsdlURI((Element) child);
                } else if (nodeName.equals("ServiceNameRef")) {
                    parseServiceNameRef((Element) child);
                } else {
                    if (DiscoUtils.debug.messageEnabled()) {
                        DiscoUtils.debug.message("Description(Element): "
                            + "invalid node" + nodeName);
                    }
                    throw new DiscoveryException(
                        DiscoUtils.bundle.getString("wrongInput"));
                }
            }
        }
        if ((mechID == null) || (mechID.size() == 0)) {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("ServiceInstance(Element): missing "
                    + "SecurityMechID element.");
            }
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("missingSecurityMechID"));
        }

        if ((soapEndpoint == null) && (wsdlURI == null)) {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("ServiceInstance(Element): missing "
                    + "WsdlRef or BriefSoapHttpDescription.");
            }
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("missingWsdlOrBrief"));
        }
    }

    private void parseEndpoint(Element child) throws DiscoveryException {
        if ((soapEndpoint != null) || (wsdlURI != null) ||
            (serviceNameRef != null))
        {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("Description(Element): "
                    + "included more Endpoint.");
            }
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("moreElement"));
        }
        soapEndpoint = XMLUtils.getElementValue((Element) child);
        if ((soapEndpoint == null) || (soapEndpoint.length() == 0)) {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("Description(Element): "
                        + "missing Endpoint value.");
            }
            throw new DiscoveryException(
                    DiscoUtils.bundle.getString("emptyElement"));
        }
    }

    private void parseSoapAction(Element child) throws DiscoveryException {
        if ((soapAction != null) || (wsdlURI != null) ||
            (serviceNameRef != null))
        {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("Description(Element): "
                    + "included more SoapAction.");
            }
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("moreElement"));
        }
        soapAction = XMLUtils.getElementValue((Element) child);
        if ((soapAction == null) || (soapAction.length() == 0)) {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("Description(Element): "
                        + "missing SoapAction value.");
            }
            throw new DiscoveryException(
                    DiscoUtils.bundle.getString("emptyElement"));
        }
    }

    private void parseWsdlURI(Element child) throws DiscoveryException {
        if ((soapEndpoint != null) || (wsdlURI != null) ||
            (soapAction != null))
        {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("Description(Element): "
                    + "included more WsdlURI.");
            }
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("moreElement"));
        }
        wsdlURI = XMLUtils.getElementValue((Element) child);
        if ((wsdlURI == null) || (wsdlURI.length() == 0)) {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("Description(Element): "
                        + "missing WsdlURI value.");
            }
            throw new DiscoveryException(
                    DiscoUtils.bundle.getString("emptyElement"));
        }
    }

    private void parseServiceNameRef(Element child) throws DiscoveryException {
        if ((soapEndpoint != null) || (serviceNameRef != null) ||
            (soapAction != null))
        {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("Description(Element): "
                    + "included more WsdlURI.");
            }
            throw new DiscoveryException(
                DiscoUtils.bundle.getString("moreElement"));
        }
        String eleValue = XMLUtils.getElementValue(child);
        if ((eleValue == null) || (eleValue.length() == 0)) {
            if (DiscoUtils.debug.messageEnabled()) {
                DiscoUtils.debug.message("Description(Element): "
                        + "missing ServiceNameRef value.");
            }
            throw new DiscoveryException(
                    DiscoUtils.bundle.getString("emptyElement"));
        }
        String localPart = eleValue;
        String prefix = null;
        String attrName = "xmlns";
        if (eleValue.indexOf(":") != -1) {
            StringTokenizer st = new StringTokenizer(localPart, ":");
            if (st.countTokens() != 2) {
                if (DiscoUtils.debug.messageEnabled()) {
                    DiscoUtils.debug.message("Description(Element): "
                        + "wrong ServiceNameRef value.");
                }
                throw new DiscoveryException(
                    DiscoUtils.bundle.getString("wrongInput"));
            }
            prefix = st.nextToken();
            attrName = attrName + ":" + prefix;
            localPart = st.nextToken();
        }
        String namespaceURI = child.getAttribute(attrName);
        if ((namespaceURI != null) && (namespaceURI.length() != 0)) {
            if ((prefix != null) && (prefix.length() != 0)) {
                serviceNameRef = new QName(namespaceURI, localPart, prefix);
            } else {
                serviceNameRef = new QName(namespaceURI, localPart);
            }
        } else {
            serviceNameRef = new QName(localPart);
        }
    }

    /**
     * Gets id attribute.
     *
     * @return id attribute.
     * @see #setId(String)
     */
    public String getId() {
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
     * Gets SOAP action.
     *
     * @return SOAP action.
     * @see #setSoapAction(String)
     */
    public String getSoapAction() {
        return soapAction;
    }

    /**
     * Sets SOAP action.
     * @param value SOAP action to be set 
     * @see #getSoapAction()
     */
    public void setSoapAction(String value) {
        soapAction = value;
    }

    /**
     * Gets supported Security Mechanism IDs.
     * 
     * @return List of IDs as String for security mechanism 
     * 
     */
    public List getSecurityMechID() {
        return mechID;
    }

    /**
     * Sets supported Security Mechanism IDs.
     * 
     * @param mechIDs List of IDs as String for security mechanism 
     * 
     */
    public void getSecurityMechID(List mechIDs) {
        mechID = mechIDs;
    }

    /**
     * Gets WSDL service name reference.
     *
     * @return WSDL service name reference.
     * @see #setServiceNameRef(QName)
     */
    public QName getServiceNameRef() {
        return serviceNameRef;
    }

    /**
     * Sets WSDL service name reference.
     *
     * @param nameRef service name reference. 
     * @see #getServiceNameRef()
     */
    public void setServiceNameRef(QName nameRef) {
        serviceNameRef = nameRef;
    }

    /**
     * Gets URI to WSDL resource containing the service description.
     *
     * @return URI to WSDL resource containing the service description.
     * @see #setWsdlURI(String)
     */
    public String getWsdlURI() {
        return wsdlURI;
    }

    /**
     * Sets URI to WSDL resource containing the service description.
     *
     * @param uri URI to the WSDL resource 
     * @see #getWsdlURI()
     */
    public void setWsdlURI(String uri) {
        wsdlURI = uri;
    }

    /**
     * Gets the value of the <code>CredentialRef</code> property.
     * 
     * @return value of the <code>CredentialRef</code> property.
     * @see #setCredentialRef(List)
     */
    public List getCredentialRef() {
        return credentialRef;
    }

    /**
     * Sets the value of the <code>CredentialRef</code> property.
     * 
     * @param refs List of String value of the <code>CredentialRef</code>
     *                property.
     * @see #getCredentialRef()
     * 
     */
    public void setCredentialRef(List refs) {
        credentialRef = refs;
    }

    /**
     * Gets SOAP end point URI. 
     * @return SOAP end point URI 
     * @see #setEndpoint(String)
     */
    public String getEndpoint() {
        return soapEndpoint;
    }

    /**
     * Sets SOAP end point URI. 
     * @param uri end point URI to be set 
     * @see #getEndpoint()
     */
    public void setEndpoint(String uri) {
        soapEndpoint = uri;
    }

    /**
     * Returns formatted string of the service description.
     *
     * @return formatted string of the service description.
     */ 
    public String toString() {
        StringBuffer sb = new StringBuffer(500);
        sb.append("<Description xmlns=\"").append(DiscoConstants.DISCO_NS).
                append("\"");
        if ((id != null) && id.length() != 0) {
            sb.append(" id=\"").append(id).append("\"");
        }
        sb.append(">");
        if (mechID != null) {
            Iterator iter = mechID.iterator();
            while (iter.hasNext()) {
                sb.append("<SecurityMechID>").append((String) iter.next()).
                    append("</SecurityMechID>");
            }
        }
        if (credentialRef != null) {
            Iterator iter2 = credentialRef.iterator();
            while (iter2.hasNext()) {
                sb.append("<CredentialRef>").append((String) iter2.next()).
                    append("</CredentialRef>");
            }
        }
        if (soapEndpoint != null) {
            sb.append("<Endpoint>").append(soapEndpoint).append("</Endpoint>");
            if (soapAction != null) {
                sb.append("<SoapAction>").append(soapAction).
                        append("</SoapAction>");
            }
        } else {
            sb.append("<WsdlURI>").append(wsdlURI).append("</WsdlURI>");
            sb.append("<ServiceNameRef");
            String prefix = null;
            String namespace = serviceNameRef.getNamespaceURI();
            if ((namespace != null) && namespace.length() != 0) {
                sb.append(" xmlns:");
                prefix = serviceNameRef.getPrefix();
                if ((prefix == null) || prefix.length() == 0) {
                    prefix = "ns1"; // our default
                }
                sb.append(prefix).append("=\"").append(namespace).append("\"");
            }
            sb.append(">");
            if (prefix != null) {
                sb.append(prefix).append(":");
            }
            sb.append(serviceNameRef.getLocalPart()).
                append("</ServiceNameRef>");
        }
        sb.append("</Description>");
        return sb.toString();
    }
}
