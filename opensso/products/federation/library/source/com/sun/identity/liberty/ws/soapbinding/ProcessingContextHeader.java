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
 * $Id: ProcessingContextHeader.java,v 1.2 2008/06/25 05:47:22 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.soapbinding; 

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * The <code>ProcessingContextHeader</code> class represents
 * <code>ProcessingContext</code> element defined in SOAP binding schema.
 * 
 * @supported.all.api
 */
public class ProcessingContextHeader {   

    private String elementValue = null;
    private String id = null;
    private Boolean mustUnderstand = null;
    private String actor = null;

    /**
     * Constructor.
     *
     * @param elementValue the processing context header string.
     * @throws SOAPBindingException if the elementValue is null
     */
    public ProcessingContextHeader(String elementValue) 
                                   throws SOAPBindingException {
        if (elementValue == null) {
            String msg = Utils.bundle.getString("missingPCvalue");
            Utils.debug.error("ProcessingContextHeader: " + msg);
            throw new SOAPBindingException(msg);
        }
        this.elementValue = elementValue;
    }

    /**
     * Constructor.
     *
     * @param processingContextElement a ProcessingContext element
     * @throws SOAPBindingException if an error occurs while parsing
     *                                 the ProcessingContext element
     */
    ProcessingContextHeader(Element processingContextElement)
                            throws SOAPBindingException {
        elementValue = XMLUtils.getElementValue(processingContextElement);
        if (elementValue == null) {
            String msg = Utils.bundle.getString("missingPCvalue");
            Utils.debug.error("ProcessingContextHeader: " + msg);
            throw new SOAPBindingException(msg);
        }
        if (elementValue == null) {
            String msg = Utils.bundle.getString("missingPCvalue");
            Utils.debug.error("ProcessingContextHeader: " + msg);
            throw new SOAPBindingException(msg);
        }
        id = XMLUtils.getNodeAttributeValue(processingContextElement,
                SOAPBindingConstants.ATTR_id);
        String str = XMLUtils.getNodeAttributeValueNS(
                processingContextElement,
                SOAPBindingConstants.NS_SOAP,
                SOAPBindingConstants.ATTR_MUSTUNDERSTAND);
        if (str != null && str.length() > 0) {
            try {
                mustUnderstand = Utils.StringToBoolean(str);
            } catch (Exception pe) {
                String msg = Utils.bundle.getString("invalidMustUnderstand");
                Utils.debug.error("ProcessingContextHeader: " + msg, pe);
                throw new SOAPBindingException(msg);
            }
        }
        
        actor = XMLUtils.getNodeAttributeValueNS(
                processingContextElement,
                SOAPBindingConstants.NS_SOAP,
                SOAPBindingConstants.ATTR_ACTOR);
    }

    /**
     * Returns value of <code>ProcessingContext</code> element.
     *
     * @return value of <code>ProcessingContext</code> element.
     */
    public String getElementValue() {
        return elementValue;
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
     * Returns value of 'mustUnderstand' attribute.
     *
     * @return value of 'mustUnderstand' attribute
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
     * Sets value of <code>ProcessingContex</code> element if it is not null.
     *
     * @param elementValue value of <code>ProcessingContext</code> element
     */
    public void setElementValue(String elementValue) {
        if (elementValue != null) {
            this.elementValue = elementValue;
        }
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
     * Sets the sign flag. If the value is set to true then the header
     * will be signed.
     *
     * @param signFlag the value of the sign flag.
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
        Element processingContextHeaderE = doc.createElementNS(
                SOAPBindingConstants.NS_SOAP_BINDING,
                SOAPBindingConstants.PTAG_PROCESSING_CONTEXT);
        headerE.appendChild(processingContextHeaderE);
        
        processingContextHeaderE.appendChild(doc.createTextNode(elementValue));
        if (id != null) {
            processingContextHeaderE.setAttributeNS(null,
                    SOAPBindingConstants.ATTR_id, id);
        }
        if (mustUnderstand != null) {
            processingContextHeaderE.setAttributeNS(
                    SOAPBindingConstants.NS_SOAP,
                    SOAPBindingConstants.PATTR_MUSTUNDERSTAND,
                    Utils.BooleanToString(mustUnderstand));
        }
        if (actor != null) {
            processingContextHeaderE.setAttributeNS(
                    SOAPBindingConstants.NS_SOAP,
                    SOAPBindingConstants.PATTR_ACTOR,
                    actor);
        }
    }
}
