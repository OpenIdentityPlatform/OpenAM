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
 * $Id: ProviderHeader.java,v 1.3 2008/06/25 05:47:23 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.soapbinding; 

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * The <code>ProviderHeader</code> class represents 'Provider' element defined
 * in SOAP binding schema.
 *
 * @supported.all.api
 */

public class ProviderHeader {

    private String providerID = null;
    private String affiliationID = null;
    private String id = null;
    private Boolean mustUnderstand = null;
    private String actor = null;

    /**
     * This constructor takes value of 'providerID' attribute which is
     * required.
     *
     * @param providerID value of 'providerID' attribute
     * @exception SOAPBindingException if the providerID is null
     */
    public ProviderHeader(String providerID) throws SOAPBindingException {
        if (providerID == null) {
            String msg = Utils.bundle.getString("missingProviderID");
            Utils.debug.error("ProviderHeader.setProviderID: " + msg);
            throw new SOAPBindingException(msg);
        }
        this.providerID = providerID;
        if(id == null) {
           id = SAMLUtils.generateID();
        }
        actor = SOAPBindingConstants.DEFAULT_SOAP_ACTOR;
        mustUnderstand = Boolean.TRUE;
    }

    /**
     * This constructor takes a <code>org.w3c.dom.Element</code>.
     *
     * @param providerElement a Provider element
     * @exception SOAPBindingException if an error occurs while parsing
     *                                 the Consent element
     */
    ProviderHeader(Element providerElement) throws SOAPBindingException {
        providerID = XMLUtils.getNodeAttributeValue(
                providerElement,
                SOAPBindingConstants.ATTR_PROVIDER_ID);
        if (providerID == null) {
            String msg = Utils.bundle.getString("missingProviderID");
            Utils.debug.error("ProviderHeader.setProviderID: " + msg);
            throw new SOAPBindingException(msg);
        }
        affiliationID = XMLUtils.getNodeAttributeValue(
                providerElement,
                SOAPBindingConstants.ATTR_AFFILIATION_ID);
        id = XMLUtils.getNodeAttributeValue(
                providerElement,
                SOAPBindingConstants.ATTR_id);
        String str = XMLUtils.getNodeAttributeValueNS(
                providerElement,
                SOAPBindingConstants.NS_SOAP,
                SOAPBindingConstants.ATTR_MUSTUNDERSTAND);
        if (str != null && str.length() > 0) {
            try {
                mustUnderstand = Utils.StringToBoolean(str);
            } catch (Exception pe) {
                String msg = Utils.bundle.getString("invalidMustUnderstand");
                Utils.debug.error("ProviderHeader: " + msg, pe);
                throw new SOAPBindingException(msg);
            }
        }
        
        actor = XMLUtils.getNodeAttributeValueNS(
                providerElement,
                SOAPBindingConstants.NS_SOAP,
                SOAPBindingConstants.ATTR_ACTOR);
    }

    /**
     * Returns value of <code>providerID</code> attribute.
     *
     * @return value of <code>providerID</code> attribute
     */
    public String getProviderID() {
        return providerID;
    }

    /**
     * Returns value of <code>affiliationID</code> attribute.
     *
     * @return value of <code>affiliationID</code> attribute.
     */
    public String getAffiliationID() {
        return affiliationID;
    }

    /**
     * Returns value of <code>id</code> attribute.
     *
     * @return value of <code>id</code> attribute
     */
    public String getId() {
        return id;
    }

    /**
     * Returns value of <code>mustUnderstand</code> attribute.
     *
     * @return value of <code>mustUnderstand</code> attribute
     */
    public Boolean getMustUnderstand() {
        return mustUnderstand;
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
     * Sets value of <code>providerID</code> attribute if the value is not null.
     *
     * @param providerID value of <code>providerID</code> attribute
     */
    public void setProviderID(String providerID) {
        if (providerID != null) {
            this.providerID = providerID;
        }
    }

    /**
     * Sets value of <code>affiliationID</code> attribute.
     *
     * @param affiliationID value of <code>affiliationID</code> attribute.
     */
    public void setAffiliationID(String affiliationID) {
        this.affiliationID = affiliationID;
    }

    /**
     * Sets value of <code>mustUnderstand</code> attribute.
     *
     * @param mustUnderstand value of <code>mustUnderstand</code> attribute
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
     * Sets the sign flag. The header will be signed if the
     * value is true.
     *
     * @param signFlag the boolean value of the sign flag.
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
        Element providerHeaderE = doc.createElementNS(
                SOAPBindingConstants.NS_SOAP_BINDING,
                SOAPBindingConstants.PTAG_PROVIDER);
        headerE.appendChild(providerHeaderE);
        
        providerHeaderE.setAttributeNS(null,
                SOAPBindingConstants.ATTR_PROVIDER_ID,
                providerID);
        if (affiliationID != null) {
            providerHeaderE.setAttributeNS(null,
                    SOAPBindingConstants.ATTR_AFFILIATION_ID,
                    affiliationID);
        }
        if (id != null) {
            providerHeaderE.setAttributeNS(null, SOAPBindingConstants.ATTR_id,
                    id);
        }
        if (mustUnderstand != null) {
            providerHeaderE.setAttributeNS(SOAPBindingConstants.NS_SOAP,
                    SOAPBindingConstants.PATTR_MUSTUNDERSTAND,
                    Utils.BooleanToString(mustUnderstand));
        }
        if (actor != null) {
            providerHeaderE.setAttributeNS(SOAPBindingConstants.NS_SOAP,
                    SOAPBindingConstants.PATTR_ACTOR,
                    actor);
        }
    }
}
