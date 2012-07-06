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
 * $Id: UsageDirectiveHeader.java,v 1.2 2008/06/25 05:47:23 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.soapbinding; 

import java.lang.Object;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * The <code>UsageDirectiveHeader</code> class represents 'UsageDirective'
 * element defined in SOAP binding schema.
 *
 * @supported.all.api
 */
public class UsageDirectiveHeader {   

    private String ref = null;
    private String id = null;
    private Boolean mustUnderstand = null;
    private String actor = null;
    private List elements = null;

    /**
     * Constructor.
     *
     * @param ref the value of <code>ref</code> attribute.
     * @throws SOAPBindingException if the value of <code>ref</code> attribute
     *                              is null.
     */
    public UsageDirectiveHeader(String ref) throws SOAPBindingException {
        if (ref == null) {
            String msg = Utils.bundle.getString("refAttributeNull");
            Utils.debug.error("UsageDirectiveHeader: " + msg);
            throw new SOAPBindingException(msg);
        }
        this.ref = ref;
    }

    /**
     * This constructor takes a <code>org.w3c.dom.Element</code>.
     *
     * @param usageDirectiveElement a UsageDirective element.
     * @throws SOAPBindingException if an error occurs while parsing
     *                                 the UsageDirective element.
     */
    UsageDirectiveHeader(Element usageDirectiveElement) 
                         throws SOAPBindingException {
        ref = XMLUtils.getNodeAttributeValue(
            usageDirectiveElement, SOAPBindingConstants.ATTR_REF);
        id = XMLUtils.getNodeAttributeValue(
            usageDirectiveElement, SOAPBindingConstants.ATTR_id);
        String str = XMLUtils.getNodeAttributeValueNS(
            usageDirectiveElement, SOAPBindingConstants.NS_SOAP,
            SOAPBindingConstants.ATTR_MUSTUNDERSTAND);
        if (str != null && str.length() > 0) {
            try {
                mustUnderstand = Utils.StringToBoolean(str);
            } catch (Exception pe) {
                String msg = Utils.bundle.getString("invalidMustUnderstand");
                Utils.debug.error("UsageDirectiveHeader: " + msg, pe);
                throw new SOAPBindingException(msg);
            }
        }
        actor = XMLUtils.getNodeAttributeValueNS(usageDirectiveElement,
            SOAPBindingConstants.NS_SOAP, SOAPBindingConstants.ATTR_ACTOR);
        NodeList nl = usageDirectiveElement.getChildNodes();
        int length = nl.getLength();
        for(int i = 0; i < length; i++) {
            Node child = nl.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                if (elements == null) {
                    elements = new ArrayList();
                }
                elements.add(child);
            }
        }
    }

    /**
     * Returns value of <code>ref</code> attribute.
     *
     * @return value of <code>ref</code> attribute.
     */
    public String getRef() {
        return ref;
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
     * Returns a list of child elements. 
     * Each entry will be a <code>org.w3c.dom.Element</code>.
     *
     * @return a list of child elements
     */
    public List getElements() {
        return elements;
    }

    /**
     * Sets value of <code>ref</code> attribute.
     *
     * @param ref value of <code>ref</code> attribute
     */
    public void setRef(String ref) {
        if (ref != null) {
            this.ref = ref;
        }
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
     * Sets a list of child elements.
     * Each entry will be a <code>org.w3c.dom.Element</code>.
     *
     * @param elements a list of child elements
     */
    public void setElements(List elements) {
        this.elements = elements;
    }

    /**
     * Sets the sign flag. The header will be signed if 
     * the value is true.
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
        Element usageDirectiveHeaderE = doc.createElementNS(
                SOAPBindingConstants.NS_SOAP_BINDING,
                SOAPBindingConstants.PTAG_USAGE_DIRECTIVE);
        headerE.appendChild(usageDirectiveHeaderE);
        
        usageDirectiveHeaderE.setAttributeNS(null,
                SOAPBindingConstants.ATTR_REF,
                ref);
        if (id != null) {
            usageDirectiveHeaderE.setAttributeNS(null,
                    SOAPBindingConstants.ATTR_id,
                    id);
        }
        if (mustUnderstand != null) {
            usageDirectiveHeaderE.setAttributeNS(SOAPBindingConstants.NS_SOAP,
                    SOAPBindingConstants.PATTR_MUSTUNDERSTAND,
                    Utils.BooleanToString(mustUnderstand));
        }
        if (actor != null) {
            usageDirectiveHeaderE.setAttributeNS(SOAPBindingConstants.NS_SOAP,
                    SOAPBindingConstants.PATTR_ACTOR, actor);
        }
        if (elements != null && !elements.isEmpty()) {
            Iterator iter = elements.iterator();
            while(iter.hasNext()) {
                Element childE = (Element)iter.next();
                usageDirectiveHeaderE.appendChild(doc.importNode(childE,true));
            }
        }
    }
}
