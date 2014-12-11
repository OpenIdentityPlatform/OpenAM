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
 * $Id: SASLRequest.java,v 1.2 2008/06/25 05:47:08 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS.
 */


package com.sun.identity.liberty.ws.authnsvc.protocol;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.liberty.ws.authnsvc.AuthnSvcConstants;
import com.sun.identity.liberty.ws.authnsvc.AuthnSvcException;
import com.sun.identity.liberty.ws.authnsvc.AuthnSvcUtils;

/**
 * The <code>SASLRequest</code> class represents <code>SASLRequest</code>
 * element defined in Authentication Service schema.
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class SASLRequest {
    private byte[]  data = null;
    private Element requestAuthnContext = null;
    private String mechanism = null;
    private String authzID = null;
    private String advisoryAuthnID = null;
    private String id = null;
    private String messageID = null;
    private String refToMessageID = null;

    /**
     * Constructs a <code>SASLRequest</code> instance.
     *
     * @param mechanism Mechanism attribute value.
     */
    public SASLRequest(String mechanism) {
        this.mechanism = mechanism;
    }

    /**
     * Constructs a <code>SAMLRequest</code> with a 
     * <code>org.w3c.dom.Element</code>.
     * @param element a <code>SASLRequest</code> element
     * @exception AuthnSvcException if an error occurs while parsing the
     *            <code>SASLRequest</code> element
     */
    public SASLRequest(Element element) throws AuthnSvcException {
        Element dataE = null;

        NodeList nl = element.getChildNodes();
        int length = nl.getLength();

        for(int i = 0; i < length; i++) {
            Node child = nl.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element)child;
                String localName = childElement.getLocalName();
                String namespaceURI = childElement.getNamespaceURI();

                if (AuthnSvcConstants.NS_AUTHN_SVC.equals(namespaceURI) &&
                    AuthnSvcConstants.TAG_DATA.equals(localName)) {
 
                   if (dataE != null) {
                        throw new AuthnSvcException("tooManyDataInReq");
                    } else if (requestAuthnContext != null) {
                        throw new AuthnSvcException("invalidSeqInReq");
                    }
                    dataE = childElement;
                } else if (AuthnSvcConstants.NS_PROTOCOLS_SCHEMA
                                            .equals(namespaceURI) &&
                           AuthnSvcConstants.TAG_REQUEST_AUTHN_CONTEXT
                                            .equals(localName)) {
                    if (requestAuthnContext != null) {
                        throw new AuthnSvcException("tooManyReqAuthnCon");
                    }
                    requestAuthnContext = childElement;
                } else {
                    throw new AuthnSvcException("invalidChildReq");
                }
            }
        }

        data = AuthnSvcUtils.decodeDataElement(dataE);

        mechanism = XMLUtils.getNodeAttributeValue(element,
                                    AuthnSvcConstants.ATTR_MECHANISM);
        if (mechanism == null) {
            String msg = AuthnSvcUtils.getString("missingMechanism");
            AuthnSvcUtils.debug.error("SASLRequest: " + msg);
            throw new AuthnSvcException(msg);
        }

        id = XMLUtils.getNodeAttributeValue(element,
                                            AuthnSvcConstants.ATTR_id);

        authzID = XMLUtils.getNodeAttributeValue(element,
                                           AuthnSvcConstants.ATTR_AUTHZ_ID);

        advisoryAuthnID = XMLUtils.getNodeAttributeValue(element,
                                AuthnSvcConstants.ATTR_ADVISORY_AUTHN_ID);

    }

    /**
     * Returns value of Element 'Data'.
     * @return value of Element 'Data'
     * @see #setData(byte[])
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Returns Element <code>RequestAuthnContext</code>.
     * @return Element <code>RequestAuthnContext</code>
     * @see #setRequestAuthnContext(Element)
     */
    public Element getRequestAuthnContext() {
        return requestAuthnContext;
    }

    /**
     * Returns value of <code>mechanism</code> attribute.
     * @return value of <code>mechanism</code> attribute
     * @see #setMechanism(String)
     */
    public String getMechanism() {
        return mechanism;
    }

    /**
     * Returns value of <code>authzID</code> attribute.
     * @return value of <code>authzID</code> attribute
     * @see #setAuthzID(String)
     */
    public String getAuthzID() {
        return authzID;
    }

    /**
     * Returns value of <code>advisoryAuthnID</code> attribute.
     * @return value of <code>advisoryAuthnID</code> attribute
     * @see #setAdvisoryAuthnID(String)
     */
    public String getAdvisoryAuthnID() {
        return advisoryAuthnID;
    }

    /**
     * Returns value of <code>id</code> attribute.
     * @return value of <code>id</code> attribute
     * @see #setId(String)
     */
    public String getId() {
        return id;
    }

    /**
     * Returns value of <code>messageID</code> attribute of
     * <code>CorrelationHeader</code>.
     * @return value of <code>messageID</code> attribute
     * @see #setMessageID(String)
     */
    public String getMessageID() {
        return messageID;
    }

    /**
     * Returns value of <code>refToMessageID</code> attribute of
     * <code>CorrelationHeader</code>.
     * @return value of <code>refToMessageID</code> attribute
     * @see #setRefToMessageID(String)
     */
    public String getRefToMessageID() {
        return refToMessageID;
    }

    /**
     * Sets value of Element 'Data'.
     * @param data value of Element 'Data'
     * @see #getData()
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * Sets Element <code>RequestAuthnContext</code>.
     * @param requestAuthnContext Element <code>RequestAuthnContext</code>
     * @see #getRequestAuthnContext()
     */
    public void setRequestAuthnContext(Element requestAuthnContext) {
        this.requestAuthnContext = requestAuthnContext;
    }

    /**
     * Sets value of <code>mechanism</code> attribute
     * @param mechanism value of <code>mechanism</code> attribute
     * @see #getMechanism()
     */
    public void setMechanism(String mechanism) {
        this.mechanism = mechanism;
    }

    /**
     * Sets value of <code>authzID</code> attribute.
     * @param authzID value of <code>authzID</code> attribute
     * @see #getAuthzID()
     */
    public void setAuthzID(String authzID) {
        this.authzID = authzID;
    }

    /**
     * Sets value of <code>advisoryAuthnID</code> attribute.
     * @param advisoryAuthnID value of <code>advisoryAuthnID</code> attribute
     * @see #getAdvisoryAuthnID()
     */
    public void setAdvisoryAuthnID(String advisoryAuthnID) {
        this.advisoryAuthnID = advisoryAuthnID;
    }

    /**
     * Sets value of <code>id</code> attribute.
     * @param id value of <code>id</code> attribute
     * @see #getId()
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets value of <code>messageID</code> attribute of
     * <code>CorrelationHeader</code>.
     * @param messageID value of <code>messageID</code> attribute
     * @see #getMessageID()
     */
    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }

    /**
     * Sets value of <code>refToMessageID</code> attribute of
     * <code>CorrelationHeader</code>.
     * @param refToMessageID value of <code>refToMessageID</code> attribute
     * @see #getRefToMessageID()
     */
    public void setRefToMessageID(String refToMessageID) {
        this.refToMessageID = refToMessageID;
    }

    /**
     * Returns <code>SASLRequest</code> in <code>org.w3c.dom.Element</code>
     * format.
     *
     * @return <code>SASLRequest</code> in <code>org.w3c.dom.Element</code>
     *         format.
     * @exception AuthnSvcException if an error occurs while creating the
     *            <code>SASLRequest</code> element
     */
    public Element toElement() throws AuthnSvcException {
        Document doc = null;
        try {
            doc = XMLUtils.newDocument();
        } catch (Exception ex) {
            AuthnSvcUtils.debug.error("SASLRequest:toElement", ex);
            throw new AuthnSvcException(ex.getMessage());
        }

        Element saslReqE = doc.createElementNS(AuthnSvcConstants.NS_AUTHN_SVC,
                                         AuthnSvcConstants.PTAG_SASL_REQUEST);
        saslReqE.setAttributeNS(AuthnSvcConstants.NS_XML,
                                AuthnSvcConstants.XMLNS_AUTHN_SVC,
                                AuthnSvcConstants.NS_AUTHN_SVC);
        saslReqE.setAttributeNS(AuthnSvcConstants.NS_XML,
                                AuthnSvcConstants.XMLNS_PROTOCOLS_SCHEMA,
                                AuthnSvcConstants.NS_PROTOCOLS_SCHEMA);

        saslReqE.setAttributeNS(null,
                                AuthnSvcConstants.ATTR_MECHANISM,
                                mechanism);

        if (authzID != null) {
            saslReqE.setAttributeNS(null,
                                    AuthnSvcConstants.ATTR_AUTHZ_ID,
                                    authzID);
        }

        if (advisoryAuthnID != null) {
            saslReqE.setAttributeNS(null,
                                    AuthnSvcConstants.ATTR_ADVISORY_AUTHN_ID,
                                    advisoryAuthnID);
        }

        if (id != null) {
            saslReqE.setAttributeNS(null, AuthnSvcConstants.ATTR_id, id);
        }

        if (data != null) {
            Element dataE = doc.createElementNS(AuthnSvcConstants.NS_AUTHN_SVC,
                                         AuthnSvcConstants.PTAG_DATA);
            dataE.appendChild(doc.createTextNode(Base64.encode(data)));
            saslReqE.appendChild(dataE);
        }

        doc.appendChild(saslReqE);
        return doc.getDocumentElement();
    }
}
