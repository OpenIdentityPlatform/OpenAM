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
 * $Id: SASLResponse.java,v 1.2 2008/06/25 05:47:08 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS.
 */


package com.sun.identity.liberty.ws.authnsvc.protocol;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.liberty.ws.authnsvc.AuthnSvcConstants;
import com.sun.identity.liberty.ws.authnsvc.AuthnSvcException;
import com.sun.identity.liberty.ws.authnsvc.AuthnSvcUtils;
import com.sun.identity.liberty.ws.disco.common.DiscoConstants;
import com.sun.identity.liberty.ws.disco.ResourceOffering;
import com.sun.identity.liberty.ws.soapbinding.Utils;

/**
 * The <code>SASLResponse</code> class represents <code>SASLResponse</code>
 * element defined in Authentication Service schema.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class SASLResponse {
    
    /**
     * Continue status where the server expects the client to send another
     * <code>SASLRequest</code>
     */ 
    public static final String CONTINUE = "continue";

    /**
     * Abort status where the server is aborting the authentication exchange.
     */
    public static final String ABORT = "abort";

    /**
     * OK status where the server considers the authentication exchange to have
     * successfully completed.
     */
    public static final String OK = "OK";

    private String statusCode = null;
    private PasswordTransforms passwordTransforms = null;
    private byte[] data = null;
    private ResourceOffering resourceOffering = null;
    private List credentials = null;
    private String serverMechanism = null;
    private String id = null;
    private String messageID = null;
    private String refToMessageID = null;

    /**
     * Constructs a <code>SASLResponse</code> instance.
     *
     * @param statusCode Status Code.
     */
    public SASLResponse(String statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Constructs a <code>SASLResponse</code> with a 
     * <code>org.w3c.dom.Element</code>.
     * @param element a <code>SASLResponse</code> element
     * @exception AuthnSvcException if an error occurs while parsing the
     *            <code>SASLResponse</code> element
     */
    public SASLResponse(Element element) throws AuthnSvcException {
        Element statusE = null;
        Element ptE = null;
        Element dataE = null;
        Element roE = null;
        Element credentialsE = null;

        NodeList nl = element.getChildNodes();
        int length = nl.getLength();

        int i;
        for(i = 0; i < length; i++) {
            Node child = nl.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element)child;
                String localName = childElement.getLocalName();
                String namespaceURI = childElement.getNamespaceURI();

                if (AuthnSvcConstants.NS_AUTHN_SVC.equals(namespaceURI) &&
                    AuthnSvcConstants.TAG_STATUS.equals(localName)){
                        statusE = childElement;
                    break;
                } else {
                    throw new AuthnSvcException("missingStatus");
                }
            }
        }

        String statusCodeStr = XMLUtils.getNodeAttributeValue(
                                    statusE,
                                    AuthnSvcConstants.ATTR_CODE);
        QName  statusCodeQN = Utils.convertStringToQName(statusCodeStr,
                                                         statusE);
        if (!AuthnSvcConstants.NS_AUTHN_SVC
                               .equals(statusCodeQN.getNamespaceURI())) {
            throw new AuthnSvcException("invalidStatusCodeNS");
        }

        statusCode = statusCodeQN.getLocalPart();

        for(i = i + 1; i < length; i++) {
            Node child = nl.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element)child;
                String localName = childElement.getLocalName();
                String namespaceURI = childElement.getNamespaceURI();
                if (AuthnSvcConstants.NS_AUTHN_SVC.equals(namespaceURI)) {
                    if (AuthnSvcConstants.TAG_STATUS.equals(localName)) {
                        throw new AuthnSvcException("tooManyStatus");
                    } else if(AuthnSvcConstants.TAG_PASSWORD_TRANSFORMS
                                        .equals(localName)){
                        if (ptE != null) {
                            throw new AuthnSvcException("tooManyPT");
                        } else if (dataE != null || roE != null ||
                                   credentialsE != null) {
                            throw new AuthnSvcException("invalidSeq");
                        }
                        ptE = childElement;
                    } else if(AuthnSvcConstants.TAG_DATA.equals(localName)){
                        if (dataE != null) {
                            throw new AuthnSvcException("tooManyData");
                        } else if (roE != null || credentialsE != null) {
                            throw new AuthnSvcException("invalidSeq");
                        }
                        dataE = childElement;
                    } else if(AuthnSvcConstants.TAG_CREDENTIALS
                                               .equals(localName)){
                        if (credentialsE != null) {
                            throw new AuthnSvcException("tooManyCr");
                        }
                        credentialsE = childElement;
                    } else {
                        throw new AuthnSvcException("invalidChild");
                    }
                } else if (DiscoConstants.DISCO_NS.equals(namespaceURI) &&
                           AuthnSvcConstants.TAG_RESOURCE_OFFERING
                                            .equals(localName)) {
                    if (roE != null) {
                        throw new AuthnSvcException("tooManyRO");
                    } else if (credentialsE != null) {
                        throw new AuthnSvcException("invalidSeq");
                    }
                    roE = childElement;
                } else {
                    throw new AuthnSvcException("invalidChild");
                }
            }
        }

        if (ptE != null) {
            passwordTransforms = new PasswordTransforms(ptE);
        }

        data = AuthnSvcUtils.decodeDataElement(dataE);

        if (roE != null) {
            try {
                resourceOffering = new ResourceOffering(roE);
            } catch (Exception ex) {
                throw new AuthnSvcException(ex);
            }
        }

        if (credentialsE != null) {
            credentials = new ArrayList();
            nl = credentialsE.getChildNodes();
            for(i = 0; i < nl.getLength(); i++) {
                Node child = nl.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    credentials.add(child);
                }
            }
        }

        serverMechanism = XMLUtils.getNodeAttributeValue(
                                element,
                                AuthnSvcConstants.ATTR_SERVER_MECHANISM);

        id = XMLUtils.getNodeAttributeValue(element,
                                            AuthnSvcConstants.ATTR_id);

    }

    /**
     * Returns value of attribute 'code' of Element 'Status'.
     * @return value of attribute 'code' of Element 'Status'
     * @see #setStatusCode(String)
     */
    public String getStatusCode()
    {
        return statusCode;
    }

    /**
     * Returns child Element 'PasswordTransforms'.
     * @return child Element 'PasswordTransforms'
     * @see #setPasswordTransforms(PasswordTransforms)
     */
    public PasswordTransforms getPasswordTransforms()
    {
        return passwordTransforms;
    }

    /**
     * Returns value of Element 'Data'.
     * @return value of Element 'Data'
     * @see #setData(byte[])
     */
    public byte[] getData()
    {
        return data;
    }

    /**
     * Returns Element <code>ResourceOffering</code>.
     * @return Element <code>ResourceOffering</code>.
     * @see #setResourceOffering(ResourceOffering)
     */
    public ResourceOffering getResourceOffering() {
        return resourceOffering;
    }

    /**
     * Returns a list of child Element of 'Credentials' Element.
     * @return a list of child Element of 'Credentials' Element
     * @see #setCredentials(List)
     */
    public List getCredentials() {
        return credentials;
    }

    /**
     * Returns value of <code>serverMechanism</code> attribute.
     * @return value of <code>serverMechanism</code> attribute
     * @see #setServerMechanism(String)
     */
    public String getServerMechanism() {
        return serverMechanism;
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
     * Sets value of attribute 'code' of Element 'Status'.
     * @param statusCode value of attribute 'code' of Element 'Status'
     * @see #getStatusCode()
     */
    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Sets child Element 'PasswordTransforms'
     * @param passwordTransforms Element 'PasswordTransforms'
     * @see #getPasswordTransforms()
     */
    public void setPasswordTransforms(PasswordTransforms passwordTransforms)
    {
        this.passwordTransforms = passwordTransforms;
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
     * Sets Element <code>ResourceOffering</code>.
     * @param resourceOffering Element <code>ResourceOffering</code>
     * @see #getResourceOffering()
     */
    public void setResourceOffering(ResourceOffering resourceOffering) {
        this.resourceOffering = resourceOffering;
    }

    /**
     * Sets a list of child Elements of 'Credentials' Element.
     * @param credentials a list of child Elements of 'Credentials' Element
     * @see #getCredentials()
     */
    public void setCredentials(List credentials) {
        this.credentials = credentials;
    }

    /**
     * Sets value of <code>mechanism</code> attribute.
     * @param serverMechanism value of <code>mechanism</code> attribute
     * @see #getServerMechanism()
     */
    public void setServerMechanism(String serverMechanism) {
        this.serverMechanism = serverMechanism;
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
     * Returns <code>SASLResponse</code> in <code>org.w3c.dom.Element</code>
     * format.
     *
     * @return <code>SASLResponse</code> in <code>org.w3c.dom.Element</code>
     *         format.
     * @exception AuthnSvcException if an error occurs while creating the
     *            <code>SASLResponse</code> element
     */
    public Element toElement() throws AuthnSvcException {
        Document doc = null;
        try {
            doc = XMLUtils.newDocument();
        } catch (Exception ex) {
            AuthnSvcUtils.debug.error("SASLResponse:toElement", ex);
            throw new AuthnSvcException(ex.getMessage());
        }

        Element saslRespE = doc.createElementNS(AuthnSvcConstants.NS_AUTHN_SVC,
                                         AuthnSvcConstants.PTAG_SASL_RESPONSE);
        saslRespE.setAttributeNS(AuthnSvcConstants.NS_XML,
                                 AuthnSvcConstants.XMLNS_AUTHN_SVC,
                                 AuthnSvcConstants.NS_AUTHN_SVC);
        saslRespE.setAttributeNS(AuthnSvcConstants.NS_XML,
                                 AuthnSvcConstants.XMLNS_DISCO,
                                 DiscoConstants.DISCO_NS);

        Element statusE = doc.createElementNS(AuthnSvcConstants.NS_AUTHN_SVC,
                                         AuthnSvcConstants.PTAG_STATUS);
        statusE.setAttributeNS(null, AuthnSvcConstants.ATTR_CODE,
                       AuthnSvcConstants.PREFIX_AUTHN_SVC + ":" + statusCode);
        saslRespE.appendChild(statusE);

        if (passwordTransforms != null) {
            passwordTransforms.addToParent(saslRespE);
        }

        if (data != null) {
            Element dataE = doc.createElementNS(AuthnSvcConstants.NS_AUTHN_SVC,
                                         AuthnSvcConstants.PTAG_DATA);
            dataE.appendChild(doc.createTextNode(Base64.encode(data)));
            saslRespE.appendChild(dataE);
        }

        if (resourceOffering != null) {
            Document roDoc =
                        XMLUtils.toDOMDocument(resourceOffering.toString(),
                                               AuthnSvcUtils.debug);
            if (roDoc == null) {
                throw new AuthnSvcException("invalidRO");
            }
            saslRespE.appendChild(doc.importNode(roDoc.getDocumentElement(),
                                                 true));
        }

        if (credentials != null && !credentials.isEmpty()) {
            Element credentialsE =
                     doc.createElementNS(AuthnSvcConstants.NS_AUTHN_SVC,
                                         AuthnSvcConstants.PTAG_CREDENTIALS);
            Iterator iter = credentials.iterator();
            while (iter.hasNext()) {
                credentialsE.appendChild(doc.importNode((Element)iter.next(),
                                                        true));
            }
            saslRespE.appendChild(credentialsE);
        }

        if (serverMechanism != null) {
            saslRespE.setAttributeNS(null,
                                     AuthnSvcConstants.ATTR_SERVER_MECHANISM,
                                     serverMechanism);
        }

        if (id != null) {
            saslRespE.setAttributeNS(null, AuthnSvcConstants.ATTR_id, id);
        }

        doc.appendChild(saslRespE);
        return doc.getDocumentElement();
    }
}
