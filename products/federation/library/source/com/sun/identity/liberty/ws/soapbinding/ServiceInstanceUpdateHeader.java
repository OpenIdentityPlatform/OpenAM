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
 * $Id: ServiceInstanceUpdateHeader.java,v 1.2 2008/06/25 05:47:23 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.soapbinding; 

import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.xml.XMLUtils;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The <code>ServiceInstanceUpdateHeader</code> class represents
 * <code>ServiceInstanceUpdate</code> element defined in SOAP binding schema.
 */
public class ServiceInstanceUpdateHeader {

    private List securityMechIDs = new ArrayList();
    private List credentials = new ArrayList();
    private String endpoint = null;
    private String id = null;
    private Boolean mustUnderstand = null;
    private String actor = null;

    /**
     * Default Constructor.
     */
    public ServiceInstanceUpdateHeader() {
        actor = SOAPBindingConstants.DEFAULT_SOAP_ACTOR;
        mustUnderstand = Boolean.TRUE;
    }

    /**
     * Constructor.
     *
     * @param siuElement a <code>ServiceInstanceUpdate</code> element.
     * @throws SOAPBindingException if an error occurs while parsing
     *            the <code>ServiceInstanceUpdate</code> element.
     */
    ServiceInstanceUpdateHeader(Element siuElement)
                                throws SOAPBindingException {
        NodeList nl = siuElement.getChildNodes();
        int length = nl.getLength();
        for(int i = 0; i < length; i++) {
            Node child = nl.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element)child;
                String localName = childElement.getLocalName();
                String namespaceURI = childElement.getNamespaceURI();
                
                if (!SOAPBindingConstants.NS_SOAP_BINDING_11
                        .equals(namespaceURI)) {
                    
                    Object[] args = { "{" + namespaceURI + "}:" + localName };
                    String msg = Utils.getString("invalidElementSIU", args);
                    Utils.debug.error("ServiceInstanceUpdateHeader: " + msg);
                    throw new SOAPBindingException(msg);
                }
                
                if (SOAPBindingConstants.TAG_SECURITY_MECH_ID.
                                                equals(localName)) {
                    if (!credentials.isEmpty() || endpoint != null) {
                        String msg = Utils.bundle.getString("invalidSeqSIU");
                        Utils.debug.error("ServiceInstanceUpdateHeader: "+msg);
                        throw new SOAPBindingException(msg);
                    }
                    String securityMechID = XMLUtils.getElementValue(
                        childElement);
                    if (securityMechID != null) {
                        securityMechIDs.add(securityMechID);
                    }
                } else if (SOAPBindingConstants.TAG_CREDENTIAL
                        .equals(localName)) {
                    if (endpoint != null) {
                        String msg = Utils.bundle.getString("invalidSeqSIU");
                        Utils.debug.error("ServiceInstanceUpdateHeader: "+msg);
                        throw new SOAPBindingException(msg);
                    }
                    
                    credentials.add(new Credential(childElement));
                } else if (SOAPBindingConstants.TAG_ENDPOINT
                        .equals(localName)) {
                    if (endpoint != null) {
                        String msg = Utils.bundle.getString("tooManyEndpoint");
                        Utils.debug.error("ServiceInstanceUpdateHeader: "+msg);
                        throw new SOAPBindingException(msg);
                    }
                    endpoint = XMLUtils.getElementValue(childElement);
                } else {
                    String msg = Utils.bundle.getString("invalidChildSIU");
                    Utils.debug.error("ServiceInstanceUpdateHeader: " + msg);
                    throw new SOAPBindingException(msg);
                }
            }
        }
        
        
        id = XMLUtils.getNodeAttributeValue(siuElement,
            SOAPBindingConstants.ATTR_id);
        String str = XMLUtils.getNodeAttributeValueNS(siuElement,
            SOAPBindingConstants.NS_SOAP,
            SOAPBindingConstants.ATTR_MUSTUNDERSTAND);
        if (str != null && str.length() > 0) {
            try {
                mustUnderstand = Utils.StringToBoolean(str);
            } catch (Exception pe) {
                String msg = Utils.bundle.getString("invalidMustUnderstand");
                Utils.debug.error("ServiceInstanceUpdateHeader: " + msg, pe);
                throw new SOAPBindingException(msg);
            }
        }
        
        actor = XMLUtils.getNodeAttributeValueNS(siuElement,
            SOAPBindingConstants.NS_SOAP, SOAPBindingConstants.ATTR_ACTOR);
    }

    /**
     * Returns a list of value of <code>SecurityMechID</code> element. Each
     * entry of the list will be a String object.
     *
     * @return a list of value of <code>SecurityMechID</code> element
     */
    public List getSecurityMechIDs() {
        return securityMechIDs;
    }

    /**
     * Returns a list of value of <code>Credential</code> element. Each entry of the list
     * will be a <code>ServiceInstanceUpdateHeader.Credential</code> object.
     *
     * @return a list of <code>Credential</code> elements.
     */
    public List getCredentials() {
        return credentials;
    }

    /**
     * Returns value of <code>Endpoint</code> element.
     *
     * @return value of <code>Endpoint</code> element
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Returns value of <code>id</code> attribute.
     *
     * @return value of <code>id</code> attribute.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns value of <code>mustUnderstand</code> attribute.
     *
     * @return value of <code>mustUnderstand</code> attribute.
     */
    public Boolean getMustUnderstand() {
        return mustUnderstand;
    }

    /**
     * Returns value of <code>actor</code> attribute.
     *
     * @return value of <code>actor</code> attribute.
     */
    public String getActor() {
        return actor;
    }

    /**
     * Sets a list of value of <code>SecurityMechID</code> element. Each entry
     * of the list will be a String object.
     *
     * @param securityMechIDs a list of value of <code>SecurityMechID</code>
     *        element.
     */
    public void setSecurityMechIDs(List securityMechIDs) {
        if (securityMechIDs != null) {
            this.securityMechIDs.clear();
            this.securityMechIDs.addAll(securityMechIDs);
        }
    }

    /**
     * Sets a list of value of <code>Credential</code> element. Each entry of the
     *      list will be a <code>ServiceInstanceUpdateHeader.Credential</code>
     *      object.
     *
     * @param credentials a list of value of <code>Credential</code> element
     */
    public void setCredentials(List credentials) {
        if (credentials != null) {
            this.credentials.clear();
            this.credentials.addAll(credentials);
        }
    }

    /**
     * Sets value of <code>Endpoint</code> element.
     *
     * @param endpoint value of <code>Endpoint</code> element.
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Sets value of <code>mustUnderstand</code> attribute.
     *
     * @param mustUnderstand value of <code>mustUnderstand</code> attribute.
     */
    public void setMustUnderstand(Boolean mustUnderstand) {
        this.mustUnderstand = mustUnderstand;
    }

    /**
     * Sets value of <code>actor</code> attribute.
     *
     * @param actor value of <code>actor</code> attribute.
     */
    public void setActor(String actor) {
        this.actor = actor;
    }

    /**
     * Sets the sign flag. The header is signed if the value
     * is true.
     *
     * @param signFlag the sign flag
     */
    public void setSignFlag(boolean signFlag) {
        if (signFlag) {
            id = SAMLUtils.generateID();
        } else {
            id = null;
        }
    }

    /**
     * Converts this header to <code>org.w3c.dom.Element</code> and add to
     * parent Header Element.
     *
     * @param headerE parent Header Element
     */
    void addToParent(Element headerE) {
        Document doc = headerE.getOwnerDocument();
        Element siuHeaderE = doc.createElementNS(
                SOAPBindingConstants.NS_SOAP_BINDING_11,
                SOAPBindingConstants.PTAG_SERVICE_INSTANCE_UPDATE);
        headerE.appendChild(siuHeaderE);
        
        if (!securityMechIDs.isEmpty()) {
            for(Iterator iter = securityMechIDs.iterator(); iter.hasNext();) {
                String securityMechID = (String)iter.next();
                Element secMechIDE = doc.createElementNS(
                        SOAPBindingConstants.NS_SOAP_BINDING_11,
                        SOAPBindingConstants.PTAG_SECURITY_MECH_ID);
                secMechIDE.appendChild(doc.createTextNode(securityMechID));
                siuHeaderE.appendChild(secMechIDE);
            }
        }
        
        if (!credentials.isEmpty()) {
            for(Iterator iter = credentials.iterator(); iter.hasNext();) {
                Credential credential = (Credential)iter.next();
                credential.addToParent(siuHeaderE);
            }
        }
        
        if (endpoint != null) {
            Element endpointE = doc.createElementNS(
                    SOAPBindingConstants.NS_SOAP_BINDING_11,
                    SOAPBindingConstants.PTAG_ENDPOINT);
            endpointE.appendChild(doc.createTextNode(endpoint));
            siuHeaderE.appendChild(endpointE);
        }
        
        if (id != null) {
            siuHeaderE.setAttributeNS(null, SOAPBindingConstants.ATTR_id, id);
        }
        if (mustUnderstand != null) {
            siuHeaderE.setAttributeNS(SOAPBindingConstants.NS_SOAP,
                    SOAPBindingConstants.PATTR_MUSTUNDERSTAND,
                    Utils.BooleanToString(mustUnderstand));
        }
        if (actor != null) {
            siuHeaderE.setAttributeNS(SOAPBindingConstants.NS_SOAP,
                    SOAPBindingConstants.PATTR_ACTOR,
                    actor);
        }
    }

    /**
     * The <code>ServiceInstanceUpdateHeader.Credential</code> class represents
     * <code>Credential</code> element in <code>ServiceInstanceUpdate</code> 
     * element defined in SOAP binding schema.
     */
    public static class Credential {
        private Element child;
        private Date notOnOrAfter = null;

	/**
         * Constructor.
	 *
	 * @param child the Child element
	 * @param notOnOrAfter a Date for <code>notOnOrAfter</code> attribute.
	 */ 
        public Credential(Element child, Date notOnOrAfter) {
            this.child = child;
            this.notOnOrAfter = notOnOrAfter;
        }

        /**
         * Constructor.
         *
         * @param credentialE the Credentail Element.
         * @throws SOAPBindingException if there is an error creating this 
         *         object.
         */
        Credential(Element credentialE) throws SOAPBindingException {
            NodeList nl = credentialE.getChildNodes();
            int length = nl.getLength();
            for(int i = 0; i < length; i++) {
                Node node = nl.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    
                    if (child != null) {
                        String msg=Utils.bundle.getString("tooManyChildrenCr");
                        Utils.debug.error("ServiceInstanceUpdateHeader: "+msg);
                        throw new SOAPBindingException(msg);
                    }
                    
                    child = (Element)node;
                }
            }
            
            String str = XMLUtils.getNodeAttributeValue(
                credentialE, SOAPBindingConstants.ATTR_NOT_ON_OR_AFTER);
            if (str != null && str.length() > 0) {
                try {
                    notOnOrAfter = DateUtils.stringToDate(str);
                } catch (ParseException pe) {
                    Object[] args = { str };
                    String msg = Utils.getString("invalidNotOnOrAfter", args);
                    Utils.debug.error("ServiceInstanceUpdateHeader: " + msg);
                    throw new SOAPBindingException(msg);
                }
            }
        }

        /**
         * Returns value of <code>notOnOrAfter</code> attribute.
         *
         * @return value of <code>notOnOrAfter</code> attribute.
         */
        public Date getNotOnOrAfter() {
            return notOnOrAfter;
        }

        /**
         * Returns the child element.
         *
         * @return the child element.
         */
        public Element getChild() {
            return child;
        }

        /**
         * Sets value of <code>notOnOrAfter</code> attribute.
         *
         * @param notOnOrAfter value of <code>notOnOrAfter</code> attribute.
         */
        public void setNotOnOrAfter(Date notOnOrAfter) {
            this.notOnOrAfter = notOnOrAfter;
        }

        /**
         * Sets the child element.
         *
         * @param child the child element.
         */
        public void setChild(Element child) {
            this.child = child;
        }

        /**
         * Appends the Credential element to the header element.
         */
        void addToParent(Element siuHeaderE) {
            Document doc = siuHeaderE.getOwnerDocument();
            Element credentialE = doc.createElementNS(
                    SOAPBindingConstants.NS_SOAP_BINDING_11,
                    SOAPBindingConstants.PTAG_CREDENTIAL);
            siuHeaderE.appendChild(credentialE);
            
            
            if (child != null) {
                credentialE.appendChild(doc.importNode(child, true));
            }
            
            if (notOnOrAfter != null) {
                credentialE.setAttributeNS(null,
                        SOAPBindingConstants.ATTR_NOT_ON_OR_AFTER,
                        DateUtils.toUTCDateFormat(notOnOrAfter));
                
            }
        }
    }
}
