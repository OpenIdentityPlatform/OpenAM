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
 * $Id: PAOSResponse.java,v 1.1 2008/03/17 03:11:05 hengming Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */

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
     * @throws Exception if <code>PAOSResponse</code> cannot be created.
     */
    public PAOSResponse(String refToMessageID, Boolean mustUnderstand,
        String actor) throws Exception {

        this.refToMessageID = refToMessageID;
        this.mustUnderstand = mustUnderstand;
        this.actor = actor;

        validateData();
    }

    /**
     * Constructs the <code>PAOSResponse</code> Object.
     *
     * @param element the Document Element of PAOS <code>Response</code> object.
     * @throws Exception if <code>PAOSResponse</code> cannot be created.
     */
    public PAOSResponse(Element element) throws Exception {
        parseElement(element);
    }

    /**
     * Constructs the <code>PAOSResponse</code> Object.
     *
     * @param xmlString the XML String representation of this object.
     * @throws Exception if <code>PAOSResponse</code> cannot be created.
     */
    public PAOSResponse(String xmlString) throws Exception {
        Document xmlDocument =
            XMLUtils.toDOMDocument(xmlString, null);
        if (xmlDocument == null) {
            throw new Exception("Unable to parse PAOSResponse element");
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

    private void parseElement(Element element) throws Exception {
        if (element == null) {
            throw new Exception("PAOSResponse.parseElement: input is null.");
        }
        String localName = element.getLocalName();
        if (!PAOSConstants.RESPONSE.equals(localName)) {
            throw new Exception("PAOSResponse.parseElement: " +
                "Element local name should be " + PAOSConstants.RESPONSE);
        }
        String namespaceURI = element.getNamespaceURI();
        if (!PAOSConstants.PAOS_NAMESPACE.equals(namespaceURI)) {
            throw new Exception("PAOSResponse.parseElement:" +
                " element namespace should be " +PAOSConstants.PAOS_NAMESPACE);
        }

        refToMessageID = XMLUtils.getNodeAttributeValue(element,
            PAOSConstants.REF_TO_MESSAGE_ID);

        String str = XMLUtils.getNodeAttributeValueNS(element,
            PAOSConstants.SOAP_ENV_NAMESPACE, PAOSConstants.MUST_UNDERSTAND);
        try {
            mustUnderstand = StringToBoolean(str);
        } catch (Exception ex) {
            throw new Exception("PAOSResponse.parseElement:" +
                " invalid MustUnderstand");
        }
        actor = XMLUtils.getNodeAttributeValueNS(element,
            PAOSConstants.SOAP_ENV_NAMESPACE, PAOSConstants.ACTOR);

        validateData();
    }

    private void validateData() throws Exception {
        if (mustUnderstand == null) {
            throw new Exception("PAOSResponse.validateData: " +
                "mustUnderstand is missing in the paos:Response");
        }

        if (actor == null) {
            throw new Exception("PAOSResponse.validateData: " +
                "actor is missing in the paos:Response");
        }
    }

    /**
     * Converts a value of XML boolean type to Boolean object.
     *
     * @param str a value of XML boolean type
     * @return a Boolean object
     * @throws Exception if there is a syntax error
     * @supported.api
     */
    public static Boolean StringToBoolean(String str) throws Exception {
        if (str == null) {
            return null;
        }
        
        if (str.equals("true") || str.equals("1")) {
            return Boolean.TRUE;
        }
        
        if (str.equals("false") || str.equals("0")) {
            return Boolean.FALSE;
        }
        
        throw new Exception();
    }
}
