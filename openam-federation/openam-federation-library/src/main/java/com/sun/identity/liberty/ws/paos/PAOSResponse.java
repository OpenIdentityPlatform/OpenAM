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
 * $Id: PAOSResponse.java,v 1.3 2008/06/25 05:47:20 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.paos;

import com.sun.identity.liberty.ws.soapbinding.Utils;
import com.sun.identity.shared.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The <code>PAOSResponse</code> class is used by a web application on
 * HTTP server side to receive and parse a <code>PAOS</code> response via an
 * HTTP request from the user agent side.
 *
 * From this class, the original <code>PAOSRequest</code> object could obtained
 * to correlate with this response.
 *
 * @supported.all.api
 */
public class PAOSResponse {
    
    private String refToMessageID = null;
    private Boolean mustUnderstand;
    private String actor;
    
    /**
     * Constructs the <code>PAOSResponse</code> Object.
     *
     * @param refToMessageID the value of the refToMessageID attribute
     * @param mustUnderstand the value of the mustUnderstand attribute
     * @param actor the value of the actor attribute
     * @throws PAOSException if <code>PAOSResponse</code> cannot be created.
     */
    public PAOSResponse(String refToMessageID, Boolean mustUnderstand,
        String actor) throws PAOSException {

        this.refToMessageID = refToMessageID;
        this.mustUnderstand = mustUnderstand;
        this.actor = actor;

        validateData();
    }

    /**
     * Constructs the <code>PAOSResponse</code> Object.
     *
     * @param element the Document Element of PAOS <code>Response</code> object.
     * @throws PAOSException if <code>PAOSResponse</code> cannot be created.
     */
    public PAOSResponse(Element element) throws PAOSException {
        parseElement(element);
    }

    /**
     * Constructs the <code>PAOSResponse</code> Object.
     *
     * @param xmlString the XML String representation of this object.
     * @throws PAOSException if <code>PAOSResponse</code> cannot be created.
     */
    public PAOSResponse(String xmlString) throws PAOSException {
        Document xmlDocument =
            XMLUtils.toDOMDocument(xmlString, PAOSUtils.debug);
        if (xmlDocument == null) {
            throw new PAOSException(
                PAOSUtils.bundle.getString("errorPAOSResponseElement"));
        }
        parseElement(xmlDocument.getDocumentElement());
    }

    /**
     * Returns the value of the refToMessageID attribute.
     *
     * @return the value of the refToMessageID attribute.
     * @see #setRefToMessageID(String)
     */
    public String getRefToMessageID() {
        return refToMessageID;
    }
    
    /**
     * Sets the value of the refToMessageID attribute.
     *
     * @param refToMessageID the value of the refToMessageID attribute
     * @see #getRefToMessageID
     */
    public void setRefToMessageID(String refToMessageID) {
        this.refToMessageID = refToMessageID;
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
     */
    public void setMustUnderstand(Boolean mustUnderstand) {
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
     */
    public void setActor(String actor) {
        this.actor = actor;
    }

    /**
     * Returns a String representation of this Object.
     * @return a  String representation of this Object.
     * @exception PAOSException if it could not create String object
     */
    public String toXMLString() throws PAOSException {
        return toXMLString(true, false);
    }

    /**
     *  Returns a String representation
     *  @param includeNSPrefix determines whether or not the namespace
     *      qualifier is prepended to the Element when converted
     *  @param declareNS determines whether or not the namespace is declared
     *      within the Element.
     *  @return a String representation of this Object.
     *  @exception PAOSException ,if it could not create String object.
     */
    public String toXMLString(boolean includeNSPrefix,boolean declareNS)
        throws PAOSException {

        validateData();

        StringBuffer xml = new StringBuffer(300);

        xml.append("<");
        if (includeNSPrefix) {
            xml.append(PAOSConstants.PAOS_PREFIX).append(":");
        }
        xml.append(PAOSConstants.RESPONSE);

        if (declareNS) {
            xml.append(" xmlns:").append(PAOSConstants.PAOS_PREFIX)
               .append("=\"").append(PAOSConstants.PAOS_NAMESPACE)
               .append("\" xmlns:").append(PAOSConstants.SOAP_ENV_PREFIX)
               .append("=\"").append(PAOSConstants.SOAP_ENV_NAMESPACE)
               .append("\"");
        }
        if (refToMessageID != null) {
            xml.append(" ").append(PAOSConstants.REF_TO_MESSAGE_ID)
               .append("=\"").append(refToMessageID).append("\"");
        }
        xml.append(" ").append(PAOSConstants.SOAP_ENV_PREFIX).append(":")
           .append(PAOSConstants.MUST_UNDERSTAND).append("=\"")
           .append(mustUnderstand.toString()).append("\"")
           .append(" ").append(PAOSConstants.SOAP_ENV_PREFIX).append(":")
           .append(PAOSConstants.ACTOR).append("=\"")
           .append(actor).append("\"></");
        if (includeNSPrefix) {
            xml.append(PAOSConstants.PAOS_PREFIX).append(":");
        }
        xml.append(PAOSConstants.RESPONSE).append(">");

        return xml.toString();
    }

    private void parseElement(Element element) throws PAOSException {
        if (element == null) {
            if (PAOSUtils.debug.messageEnabled()) {
                PAOSUtils.debug.message("PAOSResponse.parseElement:" +
                    " Input is null.");
            }
            throw new PAOSException(
                PAOSUtils.bundle.getString("nullInput"));
        }
        String localName = element.getLocalName();
        if (!PAOSConstants.RESPONSE.equals(localName)) {
            if (PAOSUtils.debug.messageEnabled()) {
                PAOSUtils.debug.message("PAOSResponse.parseElement:" +
                    " element local name should be " + PAOSConstants.RESPONSE);
            }
            throw new PAOSException(
                PAOSUtils.bundle.getString("invalidPAOSResponse"));
        }
        String namespaceURI = element.getNamespaceURI();
        if (!PAOSConstants.PAOS_NAMESPACE.equals(namespaceURI)) {
            if (PAOSUtils.debug.messageEnabled()) {
                PAOSUtils.debug.message("PAOSResponse.parseElement:" +
                    " element namespace should be " +
                    PAOSConstants.PAOS_NAMESPACE);
            }
            throw new PAOSException(
                PAOSUtils.bundle.getString("invalidPAOSNamesapce"));
        }

        refToMessageID = XMLUtils.getNodeAttributeValue(element,
            PAOSConstants.REF_TO_MESSAGE_ID);

        String str = XMLUtils.getNodeAttributeValueNS(element,
            PAOSConstants.SOAP_ENV_NAMESPACE, PAOSConstants.MUST_UNDERSTAND);
        try {
            mustUnderstand = Utils.StringToBoolean(str);
        } catch (Exception ex) {
            throw new PAOSException(PAOSUtils.bundle.getString(
                "invalidValueMustUnderstand"));
        }
        actor = XMLUtils.getNodeAttributeValueNS(element,
            PAOSConstants.SOAP_ENV_NAMESPACE, PAOSConstants.ACTOR);

        validateData();
    }

    private void validateData() throws PAOSException {
        if (mustUnderstand == null) {
            if (PAOSUtils.debug.messageEnabled()) {
                PAOSUtils.debug.message("PAOSResponse.validateData: " +
                    "mustUnderstand is missing in the paos:Response");
            }
            throw new PAOSException(PAOSUtils.bundle.getString(
                "missingMustUnderstandPAOSResponse"));
        }

        if (actor == null) {
            if (PAOSUtils.debug.messageEnabled()) {
                PAOSUtils.debug.message("PAOSResponse.validateData: " +
                    "actor is missing in the paos:Response");
            }
            throw new PAOSException(PAOSUtils.bundle.getString(
                "missingActorPAOSResponse"));
        }
    }
}
