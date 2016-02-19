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
 * $Id: CorrelationHeader.java,v 1.3 2008/06/25 05:47:22 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.soapbinding; 


import java.text.ParseException;

import java.util.Date;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml.common.SAMLUtils;

/**
 * The <code>CorrelationHeader</code> class represents <code>Correlation</code>
 * element defined in SOAP binding schema. The <code>messageID</code> is a 
 * required attribute and will be generated automatically when a constructor
 * is called.
 *
 * @supported.all.api
 */

public class CorrelationHeader {   
    private String messageID = null;
    private String id = null;
    private String refToMessageID = null;
    private Date timestamp = null;
    private Boolean mustUnderstand = null;
    private String actor = null;

    /**
     * Default Construtor
     */
    public CorrelationHeader() {
        messageID = SAMLUtils.generateID();
        id = messageID;
        timestamp = new Date();
        actor = SOAPBindingConstants.DEFAULT_SOAP_ACTOR;
        mustUnderstand = Boolean.TRUE;
    }

    /**
     * This constructor takes a <code>org.w3c.dom.Element</code>.
     *
     * @param correlationElement a Correlation element
     * @throws SOAPBindingException if an error occurs while parsing
     *                              the Correlation element
     */
    CorrelationHeader(Element correlationElement) throws SOAPBindingException {
        messageID = XMLUtils.getNodeAttributeValue(
                                    correlationElement,
                                    SOAPBindingConstants.ATTR_MESSAGE_ID);
        if (messageID == null) {
            String msg = Utils.bundle.getString("missingMessageID");
            Utils.debug.error("CorrelationHeader: " + msg);
            throw new SOAPBindingException(msg);
        }

        id = XMLUtils.getNodeAttributeValue(
                                     correlationElement,
                                     SOAPBindingConstants.ATTR_id);
        refToMessageID = XMLUtils.getNodeAttributeValue(
                                correlationElement,
                                SOAPBindingConstants.ATTR_REF_TO_MESSAGE_ID);
        String str = XMLUtils.getNodeAttributeValueNS(
                                     correlationElement,
                                     SOAPBindingConstants.NS_SOAP,
                                     SOAPBindingConstants.ATTR_MUSTUNDERSTAND);
        if (str != null && str.length() > 0) {
            try {
                mustUnderstand = Utils.StringToBoolean(str);
            } catch (Exception pe) {
                String msg = Utils.bundle.getString("invalidMustUnderstand");
                Utils.debug.error("CorrelationHeader: " + msg, pe);
                throw new SOAPBindingException(msg);
            }
        }

        str = XMLUtils.getNodeAttributeValue(
                                     correlationElement,
                                     SOAPBindingConstants.ATTR_TIMESTAMP);
        if (str == null || str.length() == 0) {
            String msg = Utils.bundle.getString("missingCorrelationTimestamp");
            Utils.debug.error("CorrelationHeader: " + msg);
            throw new SOAPBindingException(msg);
        }
        try {
            timestamp = DateUtils.stringToDate(str);
        } catch (ParseException pe) {
            String msg = Utils.bundle.getString("invalidTimestamp");
            Utils.debug.error("CorrelationHeader: " + msg, pe);
            throw new SOAPBindingException(msg);
        }
        actor = XMLUtils.getNodeAttributeValueNS(
                                       correlationElement,
                                       SOAPBindingConstants.NS_SOAP,
                                       SOAPBindingConstants.ATTR_ACTOR);
    }

    /**
     * Returns value of <code>messageID</code> attribute.
     *
     * @return value of <code>messageID</code> attribute
     */
    public String getMessageID() {
        return messageID;
    }

    /**
     * Returns value of <code>refToMessageID</code> attribute.
     *
     * @return value of <code>refToMessageID</code> attribute
     */
    public String getRefToMessageID() {
        return refToMessageID;
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
     * @return value of <code>id</code> attribute
     */
    public String getId() {
        return messageID;
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
     * @param actor value of <code>actor</code> attribute
     */
    public void setActor(String actor) {
        this.actor = actor;
    }

    /**
     * Sets value of <code>refToMessageID</code> attribute.
     *
     * @param refToMessageID value of <code>refToMessageID</code> attribute
     *
     */
    public void setRefToMessageID(String refToMessageID) {
        this.refToMessageID = refToMessageID;
    }

    /**
     * Converts this header to <code>org.w3c.dom.Element</code> and add to
     * parent Header Element.
     *
     * @param headerE parent Header Element
     */
    public void addToParent(Element headerE) {
        Document doc = headerE.getOwnerDocument();
        Element correlationHeaderE = doc.createElementNS(
                                        SOAPBindingConstants.NS_SOAP_BINDING,
                                        SOAPBindingConstants.PTAG_CORRELATION);
        headerE.appendChild(correlationHeaderE);
        correlationHeaderE.setAttributeNS(null,
                                          SOAPBindingConstants.ATTR_MESSAGE_ID,
                                          messageID);
        if (refToMessageID != null) {
            correlationHeaderE.setAttributeNS(null,
                                  SOAPBindingConstants.ATTR_REF_TO_MESSAGE_ID,
                                  refToMessageID);
        }

        correlationHeaderE.setAttributeNS(null,
	    SOAPBindingConstants.ATTR_TIMESTAMP,
	    DateUtils.toUTCDateFormat(timestamp));

        correlationHeaderE.setAttributeNS(null, SOAPBindingConstants.ATTR_id,
                                          messageID);
        if (mustUnderstand != null) {
            correlationHeaderE.setAttributeNS(SOAPBindingConstants.NS_SOAP,
                                    SOAPBindingConstants.PATTR_MUSTUNDERSTAND,
                                    Utils.BooleanToString(mustUnderstand));
        }
        if (actor != null) {
            correlationHeaderE.setAttributeNS(SOAPBindingConstants.NS_SOAP,
                                              SOAPBindingConstants.PATTR_ACTOR,
                                              actor);
        }
    }
}
