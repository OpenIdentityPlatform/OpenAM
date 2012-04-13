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
 * $Id: ECPRelayStateImpl.java,v 1.2 2008/06/25 05:47:46 qcheng Exp $
 *
 */

package com.sun.identity.saml2.ecp.impl;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.ecp.ECPRelayState;
import com.sun.identity.shared.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** 
 * This class implements <code>ECPRelayState</code> element.
 * It provides all the methods required by <code>ECPRelayState</code>
 */
public class ECPRelayStateImpl implements ECPRelayState {

    private String value;
    private Boolean mustUnderstand;
    private String actor;
    private boolean isMutable = false;

    /**
     * Constructs the <code>ECPRelayState</code> Object.
     *
     */
    public ECPRelayStateImpl() {
        isMutable=true;
    }

    /**
     * Constructs the <code>ECPRelayState</code> Object.
     *
     * @param element the Document Element of ECP <code>RelayState</code>
     *     object.
     * @throws SAML2Exception if <code>ECPRelayState</code> cannot be created.
     */
    
    public ECPRelayStateImpl(Element element) throws SAML2Exception {
        parseElement(element);
    }

    /**
     * Constructs the <code>ECPRelayState</code> Object.
     *
     * @param xmlString the XML String representation of this object.
     * @throws SAML2Exception if <code>ECPRelayState</code> cannot be created.
     */
    public ECPRelayStateImpl(String xmlString) throws SAML2Exception {
        Document xmlDocument =
            XMLUtils.toDOMDocument(xmlString, SAML2SDKUtils.debug);
        if (xmlDocument == null) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(xmlDocument.getDocumentElement());
    }

    /** 
     * Returns the value of the <code>RelayState</code>.
     *
     * @return value of the <code>RelayState</code>.
     * @see #setValue(String)
     */
    public String getValue() {
        return value;
    }
    
    /** 
     * Sets the value of the <code>RelayState</code>.
     *
     * @param value new value of the <code>RelayState</code>.
     * @throws SAML2Exception if the object is immutable.
     * @see #getValue
     */
    public void setValue(String value) throws SAML2Exception {
        if (isMutable) {
            this.value = value;
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
     * Returns a String representation of this Object.
     * @return a  String representation of this Object.
     * @exception SAML2Exception if it could not create String object
     */
    public java.lang.String toXMLString() throws SAML2Exception {
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
        xml.append(SAML2Constants.RELAY_STATE);

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
           .append(SAML2Constants.QUOTE)
           .append(SAML2Constants.END_TAG)
           .append(value)
           .append(SAML2Constants.ECP_END_TAG)
           .append(SAML2Constants.RELAY_STATE)
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
                SAML2SDKUtils.debug.message("ECPRelayStateImpl.parseElement:" +
                     " Input is null.");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("nullInput"));
        }
        String localName = element.getLocalName();
        if (!SAML2Constants.RELAY_STATE.equals(localName)) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ECPRelayStateImpl.parseElement:" +
                    " element local name should be " +
                    SAML2Constants.RELAY_STATE);
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("invalidECPRelayState"));
        }
        String namespaceURI = element.getNamespaceURI();
        if (!SAML2Constants.ECP_NAMESPACE.equals(namespaceURI)) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ECPRelayStateImpl.parseElement:" +
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

        value = XMLUtils.getElementValue(element);

        validateData();
    }

    protected void validateData() throws SAML2Exception {
        if (value == null) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ECPRelayStateImpl.validateData: " +
                    "value is missing in the ecp:RelayState");
            }
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("missingECPRelayState"));
        }

        if (mustUnderstand == null) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ECPRelayStateImpl.validateData: " +
                    "mustUnderstand is missing in the ecp:RelayState");
            }
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missingMustUnderstandECPRelayState"));
        }

        if (actor == null) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("ECPRelayStateImpl.validateData: " +
                    "actor is missing in the ecp:RelayState");
            }
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missingActorECPRelayState"));
        }
    }
}
