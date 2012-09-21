/* The contents of this file are subject to the terms
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
 * $Id: PAOSRequest.java,v 1.1 2008/03/17 03:11:05 hengming Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */

import com.sun.identity.shared.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The <code>PAOSRequest</code> class is used by a web application on
 * HTTP server side to construct a <code>PAOS</code> request message and send
 * it via an HTTP response to the user agent side.
 *
 */
public class PAOSRequest {

    private String responseConsumerURL;
    private String service;
    private String messageID;
    private Boolean mustUnderstand;
    private String actor;
    
    /**
     * Constructs the <code>PAOSRequest</code> Object.
     *
     * @param responseConsumerURL the value of the responseConsumerURL
     *     attribute
     * @param service the value of the service attribute
     * @param messageID the value of the messageID attribute
     * @param mustUnderstand the value of the mustUnderstand attribute
     * @param actor the value of the actor attribute
     * @throws Exception if <code>PAOSRequest</code> cannot be created.
     */
    public PAOSRequest(String responseConsumerURL, String service,
        String messageID, Boolean mustUnderstand, String actor)
        throws Exception {

        this.responseConsumerURL = responseConsumerURL;
        this.service = service;
        this.messageID = messageID;
        this.mustUnderstand = mustUnderstand;
        this.actor = actor;

        validateData();
    }

    /**
     * Constructs the <code>PAOSRequest</code> Object.
     *
     * @param element the Document Element of PAOS <code>Request</code> object.
     * @throws Exception if <code>PAOSRequest</code> cannot be created.
     */
    public PAOSRequest(Element element) throws Exception {
        parseElement(element);
    }

    /**
     * Constructs the <code>PAOSRequest</code> Object.
     *
     * @param xmlString the XML String representation of this object.
     * @throws Exception if <code>PAOSRequest</code> cannot be created.
     */
    public PAOSRequest(String xmlString) throws Exception {
        Document xmlDocument =
            XMLUtils.toDOMDocument(xmlString, null);
        if (xmlDocument == null) {
            throw new Exception("error parsing PAOSRequest element.");
        }
        parseElement(xmlDocument.getDocumentElement());
    }

    /**
     * Returns the value of the responseConsumerURL attribute.
     *
     * @return the value of the responseConsumerURL attribute.
     * @see #setResponseConsumerURL(String)
     */
    public String getResponseConsumerURL() {
        return responseConsumerURL;
    }

    /**
     * Sets the value of the responseConsumerURL attribute.
     *
     * @param responseConsumerURL the value of the responseConsumerURL
     *     attribute
     * @see #getResponseConsumerURL
     */
    public void setResponseConsumerURL(String responseConsumerURL) {
        this.responseConsumerURL = responseConsumerURL;
    }

    /**
     * Returns the value of the service attribute.
     *
     * @return the value of the service attribute.
     * @see #setService(String)
     */
    public String getService() {
        return service;
    }

    /**
     * Sets the value of the service attribute.
     *
     * @param service the value of the service attribute
     * @see #getService
     */
    public void setService(String service) {
        this.service = service;
    }

    
    /**
     * Returns the value of the messageID attribute.
     *
     * @return the value of the messageID attribute.
     * @see #setMessageID(String)
     */
    public String getMessageID() {
        return messageID;
    }
    
    /**
     * Sets the value of the messageID attribute.
     *
     * @param messageID the value of the messageID attribute
     * @see #getMessageID
     */
    public void setMessageID(String messageID) {
        this.messageID = messageID;
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
     * @exception Exception if it could not create String object
     */
    public String toXMLString() throws Exception {
        return toXMLString(true, false);
    }

    /**
     *  Returns a String representation
     *  @param includeNSPrefix determines whether or not the namespace
     *      qualifier is prepended to the Element when converted
     *  @param declareNS determines whether or not the namespace is declared
     *      within the Element.
     *  @return a String representation of this Object.
     *  @exception Exception ,if it could not create String object.
     */
    public String toXMLString(boolean includeNSPrefix,boolean declareNS)
        throws Exception {

        validateData();

        StringBuffer xml = new StringBuffer(300);

        xml.append("<");
        if (includeNSPrefix) {
            xml.append(PAOSConstants.PAOS_PREFIX).append(":");
        }
        xml.append(PAOSConstants.REQUEST);

        if (declareNS) {
            xml.append(" xmlns:").append(PAOSConstants.PAOS_PREFIX)
               .append("=\"").append(PAOSConstants.PAOS_NAMESPACE)
               .append("\" xmlns:").append(PAOSConstants.SOAP_ENV_PREFIX)
               .append("=\"").append(PAOSConstants.SOAP_ENV_NAMESPACE)
               .append("\"");
        }
        xml.append(" ").append(PAOSConstants.RESPONSE_CONSUMER_URL)
           .append("=\"").append(responseConsumerURL).append("\"")
           .append(" ").append(PAOSConstants.SERVICE).append("=\"")
           .append(service).append("\"");
        if (messageID != null) {
            xml.append(" ").append(PAOSConstants.MESSAGE_ID).append("=\"")
               .append(messageID).append("\"");
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
        xml.append(PAOSConstants.REQUEST).append(">");

        return xml.toString();
    }

    private void parseElement(Element element) throws Exception {
        if (element == null) {
            throw new Exception("PAOSRequest.parseElement:" +
                " Input is null.");
        }
        String localName = element.getLocalName();
        if (!PAOSConstants.REQUEST.equals(localName)) {
            throw new Exception("PAOSRequest.parseElement:" +
                    " element local name should be " + PAOSConstants.REQUEST);
        }
        String namespaceURI = element.getNamespaceURI();
        if (!PAOSConstants.PAOS_NAMESPACE.equals(namespaceURI)) {
            throw new Exception("PAOSRequest.parseElement:" +
                " element namespace should be " +
                PAOSConstants.PAOS_NAMESPACE);
        }

        responseConsumerURL = XMLUtils.getNodeAttributeValue(element,
            PAOSConstants.RESPONSE_CONSUMER_URL);

        service = XMLUtils.getNodeAttributeValue(element,
            PAOSConstants.SERVICE);

        messageID = XMLUtils.getNodeAttributeValue(element,
            PAOSConstants.MESSAGE_ID);

        String str = XMLUtils.getNodeAttributeValueNS(element,
            PAOSConstants.SOAP_ENV_NAMESPACE, PAOSConstants.MUST_UNDERSTAND);
        try {
            mustUnderstand = PAOSResponse.StringToBoolean(str);
        } catch (Exception ex) {
            throw new Exception("PAOSRequest.parseElement:" +
                "invalid MustUnderstand");
        }
        actor = XMLUtils.getNodeAttributeValueNS(element,
            PAOSConstants.SOAP_ENV_NAMESPACE, PAOSConstants.ACTOR);

        validateData();
    }

    private void validateData() throws Exception {
        if (responseConsumerURL == null) {
            throw new Exception("PAOSRequest.validateData: " +
                "responseConsumerURL is missing in the paos:Request");
        }

        if (service == null) {
            throw new Exception("PAOSRequest.validateData: " +
                "service is missing in the paos:Request");
        }

        if (mustUnderstand == null) {
            throw new Exception("PAOSRequest.validateData: " +
                    "mustUnderstand is missing in the paos:Request");
        }

        if (actor == null) {
            throw new Exception("PAOSRequest.validateData: " +
                    "actor is missing in the paos:Request");
        }
    }
}
