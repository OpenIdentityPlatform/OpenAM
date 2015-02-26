/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ECPRequestImpl.java,v 1.2 2008/06/25 05:47:47 qcheng Exp $
 *
 */

package com.sun.identity.saml2.ecp.impl;

import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.ecp.ECPRequest;
import com.sun.identity.saml2.protocol.IDPList;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.shared.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** 
 * This class implements <code>ECPRequest</code> element.
 * It provides all the methods required by <code>ECPRequest</code>
 */
public class ECPRequestImpl implements ECPRequest {

    private static final String REQUEST = "Request";
    private Issuer issuer;
    private IDPList idpList;
    private Boolean mustUnderstand;
    private String actor;
    private String providerName;
    private Boolean isPassive;
    private boolean isMutable = false;

    /**
     * Constructs the <code>ECPRequest</code> Object.
     *
     */
    public ECPRequestImpl() {
        isMutable=true;
    }

    /**
     * Constructs the <code>ECPRequest</code> Object.
     *
     * @param element the Document Element of ECP <code>Request</code> object.
     * @throws SAML2Exception if <code>ECPRequest</code> cannot be created.
     */
    public ECPRequestImpl(Element element) throws SAML2Exception {
        parseElement(element);
    }

    /**
     * Constructs the <code>ECPRequest</code> Object.
     *
     * @param xmlString the XML String representation of this object.
     * @throws SAML2Exception if <code>ECPRequest</code> cannot be created.
     */
    public ECPRequestImpl(String xmlString) throws SAML2Exception {
        Document xmlDocument =
            XMLUtils.toDOMDocument(xmlString, SAML2SDKUtils.debug);
        if (xmlDocument == null) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(xmlDocument.getDocumentElement());
    }

    /**
     * Returns the value of the issuer attribute.
     *
     * @return the value of the issuer attribute.
     * @see #setIssuer(Issuer)
     */
    public Issuer getIssuer() {
        return issuer;
    }

    /**
     * Sets the value of the issuer attribute.
     *
     * @param issuer the value of the issuer attribute
     * @throws SAML2Exception if the object is immutable
     * @see #getIssuer
     */
    public void setIssuer(Issuer issuer) throws SAML2Exception {
        if (isMutable) {
            this.issuer = issuer;
        } else {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }

    /** 
     * Returns the <code>IDPList</code> Object.
     *
     * @return the <code>IDPList</code> object.
     * @see #setIDPList(IDPList)
     */
    public IDPList getIDPList() {
        return idpList;
    }
    
    /** 
     * Sets the <code>IDPList</code> Object.
     *
     * @param idpList the new <code>IDPList</code> object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getIDPList
     */
    public void setIDPList(IDPList idpList) throws SAML2Exception {
        if (isMutable) {
            this.idpList = idpList;
        } else {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }

    /** 
     * Returns value of <code>mustUnderstand</code> attribute.
     *
     * @return value of <code>mustUnderstand</code> attribute.
     */
    public Boolean isMustUnderstand() {
        return mustUnderstand;
    }
    
    /** 
     * Sets the value of the <code>mustUnderstand</code> attribute.
     *
     * @param mustUnderstand the value of <code>mustUnderstand</code>
     *     attribute.
     * @throws SAML2Exception if the object is immutable.
     */
    public void setMustUnderstand(Boolean mustUnderstand)
        throws SAML2Exception {

        if (!isMutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        this.mustUnderstand = mustUnderstand;
    }

    /**
     * Returns value of <code>actor</code> attribute.
     *
     * @return value of <code>actor</code> attribute
     */
    public String getActor() {
        return actor;
    }

    /**
     * Sets the value of <code>actor</code> attribute.
     *
     * @param actor the value of <code>actor</code> attribute
     * @throws SAML2Exception if the object is immutable.
     */
    public void setActor(String actor) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        this.actor = actor;
    }

    /** 
     * Returns the <code>ProviderName</code> attribute value.
     *
     * @return value of the <code>ProviderName</code> attribute value.
     * @see #setProviderName(String)
     */
    public String getProviderName() {
        return providerName;
    }

    /** 
     * Sets the <code>ProviderName</code> attribute value.
     *
     * @param providerName value of the <code>ProviderName</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getProviderName
     */
    public void setProviderName(String providerName) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        this.providerName = providerName;
    }

    /** 
     * Returns the value of the <code>isPassive</code> attribute.
     *
     * @return value of <code>isPassive</code> attribute.
     */
    public Boolean isPassive() {
        return isPassive;
    }

    /** 
     * Sets the value of the <code>IsPassive</code> attribute.
     *
     * @param isPassive value of <code>IsPassive</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     */
    public void setIsPassive(Boolean isPassive) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        this.isPassive = isPassive;
    }

    /**
     * Returns a String representation of this Object.
     * @return a  String representation of this Object.
     * @exception SAML2Exception if it could not create String object
     */
    public String toXMLString() throws SAML2Exception {
        return toXMLString(true,false);
    }

    /**
     *  Returns a String representation
     *  @param includeNSPrefix determines whether or not the namespace
     *      qualifier is prepended to the Element when converted
     *  @param declareNS determines whether or not the namespace is declared
     *      within the Element.
     *  @return a String representation of this Object.
     *  @exception SAML2Exception ,if it could not create String object.
     */
    public String toXMLString(boolean includeNSPrefix,boolean declareNS)
        throws SAML2Exception {

        validateData();

        StringBuffer xml = new StringBuffer(300);

        xml.append(SAML2Constants.START_TAG);
        if (includeNSPrefix) {
            xml.append(SAML2Constants.ECP_PREFIX);
        }
        xml.append(REQUEST);
        if (declareNS) {
            xml.append(SAML2Constants.SPACE)
               .append(SAML2Constants.ECP_DECLARE_STR)
               .append(SAML2Constants.SPACE)
               .append(SAML2Constants.SOAP_ENV_DECLARE_STR);
        }

        xml.append(SAML2Constants.SPACE)
           .append(SAML2Constants.SOAP_ENV_PREFIX)
           .append(SAML2Constants.MUST_UNDERSTAND)
           .append(SAML2Constants.EQUAL)
           .append(SAML2Constants.QUOTE)
           .append(mustUnderstand.toString())
           .append(SAML2Constants.QUOTE)
           .append(SAML2Constants.SPACE)
           .append(SAML2Constants.SOAP_ENV_PREFIX)
           .append(SAML2Constants.ACTOR)
           .append(SAML2Constants.EQUAL)
           .append(SAML2Constants.QUOTE)
           .append(actor)
           .append(SAML2Constants.QUOTE);

        if (providerName != null) {
            xml.append(SAML2Constants.SPACE)
               .append(SAML2Constants.PROVIDER_NAME)
               .append(SAML2Constants.EQUAL)
               .append(SAML2Constants.QUOTE)
               .append(providerName)
               .append(SAML2Constants.QUOTE);
        }

        if (isPassive != null) {
            xml.append(SAML2Constants.SPACE)
               .append(SAML2Constants.ISPASSIVE)
               .append(SAML2Constants.EQUAL)
               .append(SAML2Constants.QUOTE)
               .append(isPassive.toString())
               .append(SAML2Constants.QUOTE);
        }

        xml.append(SAML2Constants.END_TAG)
           .append(SAML2Constants.NEWLINE)
           .append(issuer.toXMLString(includeNSPrefix,declareNS));

        if (idpList != null) {
            xml.append(SAML2Constants.NEWLINE)
               .append(idpList.toXMLString(includeNSPrefix,declareNS));
        }

        xml.append(SAML2Constants.NEWLINE)
           .append(SAML2Constants.ECP_END_TAG)
           .append(REQUEST)
           .append(SAML2Constants.END_TAG);

        return xml.toString();
    }

    /** 
     * Makes this object immutable. 
     */
    public void makeImmutable() {
        if (isMutable) {
            isMutable=false;
        }
    }
    
    /** 
     * Returns true if object is mutable.
     *
     * @return true if object is mutable.
     */
    public boolean isMutable(){
        return isMutable;
    }

    /* Parses the NameIDPolicy Element */
    private void parseElement(Element element) throws SAML2Exception {
        if (element == null) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ECPRequestImpl.parseElement:" +
                     " Input is null.");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("nullInput"));
        }
        String localName = element.getLocalName();
        if (!REQUEST.equals(localName)) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ECPRequestImpl.parseElement:" +
                    " element local name should be " + REQUEST);
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("invalidECPRequest"));
        }
        String namespaceURI = element.getNamespaceURI();
        if (!SAML2Constants.ECP_NAMESPACE.equals(namespaceURI)) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ECPRequestImpl.parseElement:" +
                    " element namespace should be " +
                    SAML2Constants.ECP_NAMESPACE);
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("invalidECPNamesapce"));
        }

        String str = XMLUtils.getNodeAttributeValueNS(element,
            SAML2Constants.SOAP_ENV_NAMESPACE, SAML2Constants.MUST_UNDERSTAND);
        mustUnderstand = SAML2SDKUtils.StringToBoolean(str);

        actor = XMLUtils.getNodeAttributeValueNS(element,
            SAML2Constants.SOAP_ENV_NAMESPACE, SAML2Constants.ACTOR);

        providerName = XMLUtils.getNodeAttributeValue(element,
            SAML2Constants.PROVIDER_NAME);

        str = XMLUtils.getNodeAttributeValue(element, SAML2Constants.ISPASSIVE);
        isPassive = SAML2SDKUtils.StringToBoolean(str);

        NodeList nList = element.getChildNodes();
        if ((nList !=null) && (nList.getLength() >0)) {
            for(int i=0; i < nList.getLength(); i++) {
                Node childNode = nList.item(i);
                if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                String cName = childNode.getLocalName() ;
                if (cName.equals(SAML2Constants.ISSUER)) {
                    validateIssuer(); 
                    issuer = AssertionFactory.getInstance().createIssuer(
                        (Element)childNode);
                } else if (cName.equals(SAML2Constants.IDPLIST)) {
                    validateIDPList();
                    idpList = ProtocolFactory.getInstance().createIDPList(
                        (Element) childNode);                   
                } else {
                     if (SAML2SDKUtils.debug.messageEnabled()) {
                         SAML2SDKUtils.debug.message(
                             "ECPRequestImpl.parseElement: " +
                             "ECP Request has invalid child element");
                     }
                     throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                         "invalidElementECPReq"));
                }
            }

        }
        validateData();
    }

    private void validateIssuer() throws SAML2Exception {
        if (issuer != null) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ECPRequestImpl.validateIssuer: " +
                    "ECP Request has too many Issuer Element");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("ecpReqTooManyIssuer"));
        }
        if (idpList != null) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ECPRequestImpl.validateIssuer: " +
                    "Issuer should be first child element in ECP Request");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("ecpReqIssuerNotFirst"));
        }
    }

    private void validateIDPList() throws SAML2Exception {
        if (idpList != null) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ECPRequestImpl.validateIssuer: " +
                    "ECP Request has too many IDPList Element");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("ecpReqTooManyIDPList"));
        }
    }

    protected void validateData() throws SAML2Exception {
        if (mustUnderstand == null) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ECPRequestImpl.validateData: " +
                    "mustUnderstand is missing in the ecp:Request");
            }
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missingMustUnderstandECPRequest"));
        }

        if (actor == null) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ECPRequestImpl.validateData: " +
                    "actor is missing in the ecp:Request");
            }
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missingActorECPRequest"));
        }

        if (issuer == null) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ECPRequestImpl.validateData: " +
                    "Issuer is missing in the ecp:Request");
            }
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missingIssuerECPRequest"));
        }
    }
}
