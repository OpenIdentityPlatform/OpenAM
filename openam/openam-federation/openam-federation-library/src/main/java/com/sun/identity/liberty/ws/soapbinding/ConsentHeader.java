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
 * $Id: ConsentHeader.java,v 1.2 2008/06/25 05:47:22 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.soapbinding; 

import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml.common.SAMLUtils;

import java.text.ParseException;

import java.util.Date;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The <code>ConsentHeader</code> class represents <code>Consent</code> element
 * defined in SOAP binding schema.
 *
 * @supported.all.api
 */
public class ConsentHeader {   

    private String uri = null;
    private Date timestamp = null;
    private String id = null;
    private Boolean mustUnderstand = null;
    private String actor = null;

    /**
     * Constructor
     *
     * @param uri the Consent URI .
     */
    public ConsentHeader(String uri) {
        this.uri = uri;
    }

    /**
     * Constructor
     *
     * @param consentElement the Document Element.
     * @throws SOAPBindingException if an error occurs while parsing
     *                                 the Consent element
     */
    ConsentHeader(Element consentElement) throws SOAPBindingException {
        uri = XMLUtils.getNodeAttributeValue(
            consentElement, SOAPBindingConstants.ATTR_URI);
        id = XMLUtils.getNodeAttributeValue(
            consentElement, SOAPBindingConstants.ATTR_id);
        String str = XMLUtils.getNodeAttributeValueNS(
            consentElement,SOAPBindingConstants.NS_SOAP,
            SOAPBindingConstants.ATTR_MUSTUNDERSTAND);
        if (str != null && str.length() > 0) {
            try {
                mustUnderstand = Utils.StringToBoolean(str);
            } catch (Exception pe) {
                String msg = Utils.bundle.getString("invalidMustUnderstand");
                Utils.debug.error("ConsentHeader: " + msg, pe);
                throw new SOAPBindingException(msg);
            }
        }

        str = XMLUtils.getNodeAttributeValue(
                                     consentElement,
                                     SOAPBindingConstants.ATTR_TIMESTAMP);
        if (str != null && str.length() > 0) {
            try {
                timestamp = DateUtils.stringToDate(str);
            } catch (ParseException pe) {
                String msg = Utils.bundle.getString("invalidTimestamp");
                Utils.debug.error("ConsentHeader: " + msg, pe);
                throw new SOAPBindingException(msg);
            }
        }
        actor = XMLUtils.getNodeAttributeValueNS(
                                              consentElement,
                                              SOAPBindingConstants.NS_SOAP,
                                              SOAPBindingConstants.ATTR_ACTOR);
    }

    /**
     * Returns value of <code>uri</code> attribute.
     *
     * @return value of <code>uri</code> attribute.
     */
    public String getUri() {
        return uri;
    }

    /**
     * Returns value of <code>timestamp</code> attribute.
     *
     * @return value of <code>timestamp</code> attribute
     */
    public Date getTimestamp() {
        return timestamp;
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
     * Returns value of </code>mustUnderstand</code> attribute.
     * @return value of </code>mustUnderstand</code> attribute
     */
    public Boolean getMustUnderstand() {
        return mustUnderstand;
    }

    /**
     * Returns value of <code>actor</code> attribute.
     * @return value of <code>actor</code> attribute
     */
    public String getActor() {
        return actor;
    }

    /**
     * Sets value of <code>uri</code> attribute.
     *
     * @param uri value of <code>uri</code> attribute
     */
    public void setUri(String uri) {
        this.uri = uri;
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
     * @param actor value of <code>actor</code> attribute
     */
    public void setActor(String actor) {
        this.actor = actor;
    }

    /**
     * Sets the value of the signFlag. The default value is
     * false.
     *
     * @param signFlag the boolean value.
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
        Element consentHeaderE = doc.createElementNS(
                SOAPBindingConstants.NS_SOAP_BINDING,
                SOAPBindingConstants.PTAG_CONSENT);
        headerE.appendChild(consentHeaderE);
        
        consentHeaderE.setAttributeNS(null, SOAPBindingConstants.ATTR_URI,uri);
        
        if (timestamp != null) {
            consentHeaderE.setAttributeNS(null,
                    SOAPBindingConstants.ATTR_TIMESTAMP,
                    DateUtils.toUTCDateFormat(timestamp));
        }
        if (id != null) {
            consentHeaderE.setAttributeNS(null, SOAPBindingConstants.ATTR_id,
                    id);
        }
        if (mustUnderstand != null) {
            consentHeaderE.setAttributeNS(SOAPBindingConstants.NS_SOAP,
                    SOAPBindingConstants.PATTR_MUSTUNDERSTAND,
                    Utils.BooleanToString(mustUnderstand));
        }
        if (actor != null) {
            consentHeaderE.setAttributeNS(SOAPBindingConstants.NS_SOAP,
                    SOAPBindingConstants.PATTR_ACTOR,
                    actor);
        }
    }
}
