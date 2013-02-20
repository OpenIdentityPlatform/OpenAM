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
 * $Id: Message.java,v 1.3 2008/06/25 05:47:22 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.soapbinding; 

import java.lang.Object;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.Iterator;

import java.security.cert.X509Certificate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.List;
import javax.xml.soap.SOAPMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import com.sun.identity.liberty.ws.common.wsse.BinarySecurityToken;
import com.sun.identity.liberty.ws.common.wsse.WSSEConstants;
import com.sun.identity.liberty.ws.security.SecurityAssertion;
import com.sun.identity.liberty.ws.security.SecurityUtils;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * The <code>Message</code> class is used by web service client and server to
 * construct request or response. It will be sent over the SOAP connection.
 * The <code>Message</code> contains SOAP headers and bodies. The SOAP binding
 * defines the following headers: <code>CorrelationHeader</code>,
 * <code>ProviderHeader</code>, <code>ConsentHeader</code>,
 * <code>UsageDirectiveHeader</code>, <code>ProcessingContextHeader</code>
 * and <code>ServiceInstanceUpdateHeader</code>.
 * The first 2 are required and the others are optional.
 * Signing is mandatory for <code>CorrelationHeader</code> and SOAP Body
 * element which is the parent of the bodies. Other headers are optional,
 * so each header needs to have a flag to specify whether it needs to be
 * signed or not. For each header that needs to be signed, it must have an
 * id attribute in the top element. The constuctor will take a SAML assertion
 * or cert alias in order to sign.
 * 
 * @supported.all.api
 */
public class Message {

    /**
     * anonymous profile is specified.
     */
    public static final int ANONYMOUS  = 0;

    /**
     * X509 Token profile is specified.
     */
    public static final int X509_TOKEN = 1;

    /**
     * SAML Token profile is specified.
     */
    public static final int SAML_TOKEN = 2;

    /**
     * Bearer Token profile is specified.
     */
    public static final int BEARER_TOKEN = 3;

    /**
     * Authentication mechanism "urn:liberty:security:2003-08:null:null"
     */
    public static final String NULL_NULL =
                       "urn:liberty:security:2003-08:null:null";

    /**
     * Authentication mechanism "urn:liberty:security:2003-08:null:X509"
     */
    public static final String NULL_X509 =
                       "urn:liberty:security:2003-08:null:X509";

    /**
     * Authentication mechanism "urn:liberty:security:2003-08:null:SAML"
     */
    public static final String NULL_SAML =
                       "urn:liberty:security:2003-08:null:SAML";

    /**
     * Authentication mechanism "urn:liberty:security:2004-04:null:Bearer"
     */
    public static final String NULL_BEARER =
                       "urn:liberty:security:2004-04:null:Bearer";

    /**
     * Authentication mechanism "urn:liberty:security:2003-08:TLS:null"
     */
    public static final String TLS_NULL =
                       "urn:liberty:security:2003-08:TLS:null";

    /**
     * Authentication mechanism "urn:liberty:security:2003-08:TLS:X509"
     */
    public static final String TLS_X509 =
                       "urn:liberty:security:2003-08:TLS:X509";

    /**
     * Authentication mechanism "urn:liberty:security:2003-08:TLS:SAML"
     */
    public static final String TLS_SAML =
                       "urn:liberty:security:2003-08:TLS:SAML";

    /**
     * Authentication mechanism "urn:liberty:security:2004-04:TLS:Bearer"
     */
    public static final String TLS_BEARER =
                       "urn:liberty:security:2004-04:TLS:Bearer";

    /**
     * Authentication mechanism "urn:liberty:security:2003-08:ClientTLS:null"
     */
    public static final String CLIENT_TLS_NULL =
                       "urn:liberty:security:2003-08:ClientTLS:null";

    /**
     * Authentication mechanism "urn:liberty:security:2003-08:ClientTLS:X509"
     */
    public static final String CLIENT_TLS_X509 =
                       "urn:liberty:security:2003-08:ClientTLS:X509";

    /**
     * Authentication mechanism "urn:liberty:security:2003-08:ClientTLS:SAML"
     */
    public static final String CLIENT_TLS_SAML =
                       "urn:liberty:security:2003-08:ClientTLS:SAML";

    /**
     * Authentication mechanism "urn:liberty:security:2004-04:ClientTLS:Bearer"
     */
    public static final String CLIENT_TLS_BEARER =
                       "urn:liberty:security:2004-04:ClientTLS:Bearer";

    /**
     * Authentication mechanism "urn:liberty:security:2005-02:null:X509"
     */
    public static final String NULL_X509_WSF11 =
                       "urn:liberty:security:2005-02:null:X509";

    /**
     * Authentication mechanism "urn:liberty:security:2005-02:TLS:X509"
     */
    public static final String TLS_X509_WSF11 =
                       "urn:liberty:security:2005-02:TLS:X509";

    /**
     * Authentication mechanism "urn:liberty:security:2005-02:ClientTLS:X509"
     */
    public static final String CLIENT_TLS_X509_WSF11 =
                       "urn:liberty:security:2005-02:ClientTLS:X509";

    /**
     * Authentication mechanism "urn:liberty:security:2005-02:null:SAML"
     */
    public static final String NULL_SAML_WSF11 =
                       "urn:liberty:security:2005-02:null:SAML";

    /**
     * Authentication mechanism "urn:liberty:security:2005-02:TLS:SAML"
     */
    public static final String TLS_SAML_WSF11 =
                       "urn:liberty:security:2005-02:TLS:SAML";

    /**
     * Authentication mechanism "urn:liberty:security:2005-02:ClientTLS:SAML"
     */
    public static final String CLIENT_TLS_SAML_WSF11 =
                       "urn:liberty:security:2005-02:ClientTLS:SAML";


    /**
     * Authentication mechanism "urn:liberty:security:2005-02:null:Bearer"
     */
    public static final String NULL_BEARER_WSF11 =
                       "urn:liberty:security:2005-02:null:Bearer";

    /**
     * Authentication mechanism "urn:liberty:security:2005-02:TLS:Bearer"
     */
    public static final String TLS_BEARER_WSF11 =
                       "urn:liberty:security:2005-02:TLS:Bearer";

    /**
     * Authentication mechanism "urn:liberty:security:2005-02:ClientTLS:Bearer"
     */
    public static final String CLIENT_TLS_BEARER_WSF11 =
                       "urn:liberty:security:2005-02:ClientTLS:Bearer";

    private int securityProfileType = ANONYMOUS;
    private CorrelationHeader correlationHeader = null;
    private ConsentHeader consentHeader = null;
    private List usageDirectiveHeaders = null;
    private ProviderHeader providerHeader = null;
    private ProcessingContextHeader processingContextHeader = null;
    private ServiceInstanceUpdateHeader serviceInstanceUpdateHeader = null;
    private List soapHeaders = null;
    private List soapBodies = null;
    private List securityHeaders = null;
    private List signingIds = null;
    private SOAPFault soapFault = null;
    private String ipAddress = null;
    private String protocol = "http";
    private SecurityAssertion assertion = null;
    private BinarySecurityToken binarySecurityToken = null;
    private X509Certificate certificate = null;
    private X509Certificate messageCertificate = null;
    private Object token = null;
    private String bodyId = null;
    private boolean clientAuthentication = false;
    private String authenticationMechanism = null;
    private Document doc = null;
    private String wsfVersion = SOAPBindingConstants.WSF_11_VERSION;

    /**
     * Default Constructor.
     */
    public Message() {
        correlationHeader = new CorrelationHeader();
        securityProfileType = ANONYMOUS;
    }

    /**
     * The default constructor uses default cert alias defined in AMConfig for
     * signing.
     *
     * @param providerHeader <code>ProviderHeader</code>.
     * @throws SOAPBindingException if provider header is null.
     */
    public Message(ProviderHeader providerHeader) throws SOAPBindingException {
        correlationHeader = new CorrelationHeader();
        this.providerHeader = providerHeader;
        securityProfileType = ANONYMOUS;
    }

    /**
     * This constructor takes a SAML assertion for signing.
     *
     * @param providerHeader <code>ProviderHeader</code>
     * @param assertion a SAML assertion
     * @throws SOAPBindingException if an error occurs while processing
     *                                 the SAML assertion or the provider
     *                                 header is null
     */
    public Message(ProviderHeader providerHeader,SecurityAssertion assertion)
                   throws SOAPBindingException {
        if (assertion == null) {
            throw new SOAPBindingException(
                    Utils.bundle.getString("SAMLAssertionNull"));
        }
        
        this.assertion = assertion;
        
        if (assertion.isBearer()) {
            securityProfileType = BEARER_TOKEN;
        } else {
            securityProfileType = SAML_TOKEN;
            messageCertificate =
                    (X509Certificate)SecurityUtils.getCertificate(assertion);
        }
        
        correlationHeader = new CorrelationHeader();
        this.providerHeader = providerHeader;
    }

    /**
     * This constructor takes a binary security token for signing.
     *
     * @param providerHeader <code>ProviderHeader</code>
     * @param token a binary security token
     * @throws SOAPBindingException if an error occurs while processing
     *                                 the token or the provider header is null
     */
    public Message(ProviderHeader providerHeader, BinarySecurityToken token)
                   throws SOAPBindingException {
        if (token == null) {
            throw new SOAPBindingException(
                    Utils.bundle.getString("binarySecurityTokenNull"));
        }
        binarySecurityToken = token;
        wsfVersion = binarySecurityToken.getWSFVersion();
        messageCertificate =
            (X509Certificate)SecurityUtils.getCertificate(binarySecurityToken);
        correlationHeader = new CorrelationHeader();
        this.providerHeader = providerHeader;
        securityProfileType = X509_TOKEN;
    }

    /**
     * This constructor is to create a SOAP fault message.
     *
     * @param soapFault <code>SOAPFault</code>
     */
    public Message( SOAPFault soapFault) {
        this.soapFault = soapFault;
        correlationHeader = new CorrelationHeader();
    }

    /**
     * This constructor takes an InputStream.
     *
     * @param inputStream an InputStream
     * @throws SOAPBindingException if an error occurs while parsing the input.
     */
    public Message(InputStream inputStream) throws SOAPBindingException {
        try {
            doc = XMLUtils.toDOMDocument(inputStream, Utils.debug);
            parseDocument(doc);
        } catch (Exception ex) {
            Utils.debug.error("Message:Message", ex);
            throw new SOAPBindingException(ex.getMessage());
        }
    }

    /**
     * This constructor takes a SOAP message which is received from a SOAP
     * connection.
     *
     * @param  soapMessage a SOAP message
     * @throws SOAPBindingException if an error occurs while parsing the
     *         SOAP message
     */
    public Message(SOAPMessage soapMessage)
           throws SOAPBindingException,SOAPFaultException {
        try {
            ByteArrayOutputStream bop = new ByteArrayOutputStream();
            soapMessage.writeTo(bop);
            ByteArrayInputStream bin =
                    new ByteArrayInputStream(bop.toByteArray());
            doc = XMLUtils.toDOMDocument(bin, Utils.debug);
            parseDocument(doc);
        } catch (Exception ex) {
            Utils.debug.error("Message:Message", ex);
            throw new SOAPBindingException(ex.getMessage());
        }
    }

    /**
     * Gets security profile type. Possible values are ANONYMOUS, X509_TOKEN
     * and SAML_TOKEN.
     *
     * @return the Security Profile type
     */
    public int getSecurityProfileType() {
        return securityProfileType;
    }

    /**
     * Sets security profile type. 
     *
     * @param profileType Profile Type. Possible values are ANONYMOUS,
     *               X509_TOKEN , SAML_TOKEN and BEARER_TOKEN
     */
    public void setSecurityProfileType(int profileType) {
        securityProfileType = profileType;
    }

    /**
     * Sets a binary security token for this message.
     *
     * @param binaryToken a binary security token
     */
    public void setBinarySecurityToken(BinarySecurityToken binaryToken) {
        binarySecurityToken = binaryToken;
        messageCertificate = 
            (X509Certificate)SecurityUtils.getCertificate(binarySecurityToken);
    }

    /**
     * Gets authentication mechanism. 
     * Possible values are NULL_NULL,NULL_X509, NULL_SAML, TLS_NULL,
     * TLS_X509, TLS_SAML, CLIENT_TLS_NULL,CLIENT_TLS_X509, CLIENT_TLS_SAML, 
     * NULL_BEAER, TLS_BEARER, and CLIENT_TLS_BEARER.
     *
     * @return an authentication mechanism
     */
    public String getAuthenticationMechanism() {
        if (authenticationMechanism != null) {
            return authenticationMechanism;
        }

        if (protocol.equalsIgnoreCase("https")) {
            if (certificate == null) {
                switch (securityProfileType) {
                    case X509_TOKEN:
                        if(SOAPBindingConstants.WSF_11_VERSION.equals(
                                                wsfVersion)) {
                           authenticationMechanism = TLS_X509_WSF11;
                        } else {
                           authenticationMechanism = TLS_X509;
                        }
                        return authenticationMechanism;
                    case SAML_TOKEN:
                        if(SOAPBindingConstants.WSF_11_VERSION.equals(
                                                wsfVersion)) {
                           authenticationMechanism = TLS_SAML_WSF11;
                        } else {
                           authenticationMechanism = TLS_SAML;
                        }
                        return authenticationMechanism;
                    case BEARER_TOKEN:
                        if(SOAPBindingConstants.WSF_11_VERSION.equals(
                                                wsfVersion)) {
                           authenticationMechanism = TLS_BEARER_WSF11;
                        } else {
                           authenticationMechanism = TLS_BEARER;
                        }
                        return authenticationMechanism;
                    default:
                        authenticationMechanism = TLS_NULL;
                        return authenticationMechanism;
                }
            } else {
                switch (securityProfileType) {
                    case X509_TOKEN:
                        if(SOAPBindingConstants.WSF_11_VERSION.equals(
                                                wsfVersion)) {
                           authenticationMechanism = CLIENT_TLS_X509_WSF11;
                        } else {
                           authenticationMechanism = CLIENT_TLS_X509;
                        }
                        return authenticationMechanism;
                    case SAML_TOKEN:
                        if(SOAPBindingConstants.WSF_11_VERSION.equals(
                                                wsfVersion)) {
                           authenticationMechanism = CLIENT_TLS_SAML_WSF11;
                        } else {
                           authenticationMechanism = CLIENT_TLS_SAML;
                        }
                        return authenticationMechanism;
                    case BEARER_TOKEN:
                        if(SOAPBindingConstants.WSF_11_VERSION.equals(
                                                wsfVersion)) {
                           authenticationMechanism = CLIENT_TLS_BEARER_WSF11;
                        } else {
                           authenticationMechanism = CLIENT_TLS_BEARER;
                        }
                        return authenticationMechanism;
                    default:
                        authenticationMechanism = CLIENT_TLS_NULL;
                        return authenticationMechanism;
                }
            }
        } else {
            switch (securityProfileType) {
                case X509_TOKEN:
                    if(SOAPBindingConstants.WSF_11_VERSION.equals(
                                            wsfVersion)) {
                       authenticationMechanism = NULL_X509_WSF11;
                    } else {
                       authenticationMechanism = NULL_X509;
                    }
                    return authenticationMechanism;
                case SAML_TOKEN:
                    if(SOAPBindingConstants.WSF_11_VERSION.equals(
                                            wsfVersion)) {
                       authenticationMechanism = NULL_SAML_WSF11;
                    } else {
                       authenticationMechanism = NULL_SAML;
                    }
                    return authenticationMechanism;
                case BEARER_TOKEN:
                    if(SOAPBindingConstants.WSF_11_VERSION.equals(
                                            wsfVersion)) {
                       authenticationMechanism = NULL_BEARER_WSF11;
                    } else {
                       authenticationMechanism = NULL_BEARER;
                    }
                    return authenticationMechanism;
                default:
                    authenticationMechanism = NULL_NULL;
                    return authenticationMechanism;
            }
        }
    }

    /**
     * Returns a boolean flag to determine if this Message will be sent to
     *         a server that requires client authentication.
     *
     * @return true if this Message will be sent to a server that
     *         requires client authentication
     */
    public boolean isClientAuthentication() {
        return clientAuthentication;
    }

    /**
     * Returns the <code>CorrelationHeader</code>.
     *
     * @return the <code>CorrelationHeader</code>.
     */
    public CorrelationHeader getCorrelationHeader() {
        return correlationHeader;
    }

    /**
     * Returns the <code>ConsentHeader</code>.
     *
     * @return the <code>ConsentHeader</code>.
     */
    public ConsentHeader getConsentHeader() {
        return consentHeader;
    }

    /**
     * Returns a list of <code>UsageDirectiveHeader</code>.
     *
     * @return a list of <code>UsageDirectiveHeader</code>.
     */
    public List getUsageDirectiveHeaders() {
        return usageDirectiveHeaders;
    }

    /**
     * Returns the <code>ProviderHeader</code>.
     *
     * @return the <code>ProviderHeader</code>.
     */
    public ProviderHeader getProviderHeader() {
        return providerHeader;
    }

    /**
     * Returns the <code>ProcessingContextHeader</code>.
     *
     * @return the <code>ProcessingContextHeader</code>.
     */
    public ProcessingContextHeader getProcessingContextHeader() {
        return processingContextHeader;
    }

    /**
     * Returns the <code>ServiceInstanceUpdateHeader</code>.
     *
     * @return the <code>ServiceInstanceUpdateHeader</code>.
     */
    public ServiceInstanceUpdateHeader getServiceInstanceUpdateHeader() {
        return serviceInstanceUpdateHeader;
    }

    /**
     * Returns a list of SOAP headers except  <code>CorrelationHeader</code>,
     * <code>ConsentHeader</code>, <code>UsageDirectiveHeader</code> and
     * <code>Security</code> header. Each entry will be a 
     * <code>org.w3c.dom.Element</code>.
     *
     * @return a list of SOAP headers
     */
    public List getOtherSOAPHeaders() {
        return soapHeaders;
    }

    /**
     * Returns the <code>SOAPFault</code>.
     *
     * @return the <code>SOAPFault</code>.
     */
    public SOAPFault getSOAPFault() {
        return soapFault;
    }

    /**
     * Returns a list of SOAP bodies.
     * Each entry will be a <code>org.w3c.dom.Element</code>.
     *
     * @return a list of SOAP bodies
     */
    public List getBodies() {
        return soapBodies;
    }

    /**
     * Returns a list of SOAP bodies.
     * Each entry will be a <code>org.w3c.dom.Element</code> with specified
     * namespace URI and local name.
     *
     * @param namespaceURI namspace URI
     * @param localName local name
     * @return a list of SOAP bodies
     */
    public List getBodies( String namespaceURI, String localName) {
        ArrayList bodies = new ArrayList();
        if (soapBodies != null && !soapBodies.isEmpty()) {
            Iterator iter = soapBodies.iterator();
            while(iter.hasNext()) {
                Element bodyE = (Element)iter.next();
                String ln = bodyE.getLocalName();
                String ns = bodyE.getNamespaceURI();
                if (((ns == null && namespaceURI == null) ||
                        (ns != null && ns.equals(namespaceURI))) &&
                        ln.equals(localName)) {
                    bodies.add(bodyE);
                }
            }
        }
        return soapBodies;
    }

    /**
     * Returns a list of security header except the SAML assertion used in
     * SAML token profile or the binary security token used in X509 token
     * profile. Each entry will be a <code>org.w3c.dom.Element</code>.
     *
     * @return a list of security headers
     */
    public List getOtherSecurityHeaders() {
        return securityHeaders;
    }

    /**
     * Returns the SAML assertion used for signing.
     *
     * @return the SAML assertion.
     */
    public SecurityAssertion getAssertion() {
        return assertion;
    }

    /**
     * Returns a binary security token used for signing.
     *
     * @return a binary security token.
     */
    public BinarySecurityToken getBinarySecurityToken() {
        return binarySecurityToken;
    }

    /**
     * Returns the X509 certificate used in client authentication.
     *
     * @return a X509 certificate
     */
    public X509Certificate getPeerCertificate() {
        return certificate;
    }

    /**
     * Returns the X509 certificate used in message level authentication.
     *
     * @return a X509 certificate.
     */
    public X509Certificate getMessageCertificate() {
        return messageCertificate;
    }

    /**
     * Returns a token for the sender of this Message.
     *
     * @return a token Object.
     */
    public Object getToken() {
        return token;
    }

    /**
     * Returns the IP address of remote site of the SOAP connection.
     *
     * @return a IP address
     */
    public String getIPAddress() {
        return ipAddress;
    }

    /**
     * Returns a list of id's for signing.
     *
     * @return a list of id's for signing.
     */
    public List getSigningIds() {
        List ids = new  ArrayList();
        ids.add(correlationHeader.getId());
        if (consentHeader != null) {
            String id = consentHeader.getId();
            if (id != null) {
                ids.add(id);
            }
        }
        if (usageDirectiveHeaders != null &&
            !usageDirectiveHeaders.isEmpty()) {
            Iterator iter = usageDirectiveHeaders.iterator();
            while(iter.hasNext()) {
                String id = ((UsageDirectiveHeader)iter.next()).getId();
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        if (providerHeader != null) {
            String id = providerHeader.getId();
            if (id != null) {
                ids.add(id);
            }
        }
        if (processingContextHeader != null) {
            String id = processingContextHeader.getId();
            if (id != null) {
                ids.add(id);
            }
        }
        if (serviceInstanceUpdateHeader != null) {
            String id = serviceInstanceUpdateHeader.getId();
            if (id != null) {
                ids.add(id);
            }
        }
        if (signingIds != null && !signingIds.isEmpty()) {
            ids.addAll(signingIds);
        }
        if (bodyId == null) {
            bodyId = SAMLUtils.generateID();
        }
        ids.add(bodyId);
        return ids;
    }

    /**
     * Sets the <code>CorrelationHeader</code>.
     *
     * @param correlationHeader <code>CorrelationHeader</code>
     */
    public void setCorrelationHeader(CorrelationHeader correlationHeader) {
        if (correlationHeader != null) {
            this.correlationHeader = correlationHeader;
        }
    }

    /**
     * Sets <code>ConsentHeader</code>.
     *
     * @param consentHeader the <code>ConsentHeader</code>.
     */
    public void setConsentHeader(ConsentHeader consentHeader) {
        this.consentHeader = consentHeader;
    }

    /**
     * Sets a list of <code>UsageDirectiveHeader</code>.
     *
     * @param usageDirectiveHeaders a list of <code>UsageDirectiveHeader</code>.
     */
    public void setUsageDirectiveHeaders(List usageDirectiveHeaders) {
        this.usageDirectiveHeaders = usageDirectiveHeaders;
    }

    /**
     * Sets <code>ProviderHeader</code> if it is not null.
     *
     * @param providerHeader the <code>ProviderHeader</code>.
     */
    public void setProviderHeader(ProviderHeader providerHeader) {
        this.providerHeader = providerHeader;
    }

    /**
     * Sets the <code>ProcessingContextHeader</code>.
     *
     * @param processingContextHeader <code>ProcessingContextHeader</code>
     */
    public void setProcessingContextHeader(
                  ProcessingContextHeader processingContextHeader) {
        this.processingContextHeader = processingContextHeader;
    }

    /**
     * Sets the <code>ServiceInstanceUpdateHeader</code>.
     *
     * @param serviceInstanceUpdateHeader
     *        the <code>ServiceInstanceUpdateHeader</code>
     */
    public void setServiceInstanceUpdateHeader(
               ServiceInstanceUpdateHeader serviceInstanceUpdateHeader) {
        this.serviceInstanceUpdateHeader = serviceInstanceUpdateHeader;
    }

    /**
     * Sets a list of SOAP headers except  <code>CorrelationHeader</code>,
     * <code>ConsentHeader</code>, <code>UsageDirectiveHeader</code> and
     * 'Security' header. Each entry will be a <code>org.w3c.dom.Element</code>.
     *
     * @param headers a list of SOAP headers.
     * @param signingIds a list of values of <code>id</code> attribute for 
     *        signing
     */
    public void setOtherSOAPHeaders(List headers, List signingIds) {
        soapHeaders = headers;
        this.signingIds = signingIds;
    }

    /**
     * Sets a SOAP header except  <code>CorrelationHeader</code>,
     * <code>ConsentHeader</code> and <code>UsageDirectiveHeader</code>.
     *
     * @param header a <code>org.w3c.dom.Element</code>
     * @param signingId the value of <code>id</code> attribute for signing.
     *        A value null value for this attribute is assumed as no signing.
     */
    public void setOtherSOAPHeader(Element header, String signingId) {
        soapHeaders = new ArrayList(1);
        soapHeaders.add(header);
        if (signingId != null) {
            signingIds = new ArrayList(1);
            signingIds.add(signingId);
        }
    }

    /**
     * Sets a list of security headers.  Each entry will be a
     * <code>org.w3c.dom.Element</code>.
     *
     * @param headers a list of security headers.
     */
    public void setOtherSecurityHeaders(List headers) {
        securityHeaders = headers;
    }

    /**
     * Sets a security header.
     *
     * @param header the security header element.
     */
    public void setOtherSecurityHeader(Element header) {
        securityHeaders = new ArrayList(1);
        securityHeaders.add(header);
    }

    /**
     * Sets the <code>SOAPFault</code>.
     *
     * @param soapFault the <code>SOAPFault</code>.
     */
    public void setSOAPFault(SOAPFault soapFault) {
        this.soapFault = soapFault;
    }

    /**
     * Sets a list of SOAP bodies. Each entry will be a
     * <code>org.w3c.dom.Element</code>. To send a SOAP Fault, please use
     * method <code>setSOAPFault</code>.
     *
     * @param bodies a list of SOAP bodies.
     */
    public void setSOAPBodies(List bodies) {
        soapBodies = bodies;
    }

    /**
     * Sets a SOAP body. To send a SOAP Fault, please use method
     * <code>setSOAPFault</code>.
     *
     * @param body a <code>org.w3c.dom.Element</code>
     */
    public void setSOAPBody(Element body) {
        soapBodies = new ArrayList(1);
        soapBodies.add(body);
    }

    /**
     * Sets the IP address of remote site of the SOAP connection.
     *
     * @param ipAddress a IP address
     */
    void setIPAddress( String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Sets the protocol value . The expected
     * value is either http or https.
     *
     * @param protocol the protocol value.
     */
    void setProtocol( String protocol) {
        if (protocol == null) {
            this.protocol = "http";
        } else {
            this.protocol = protocol;
        }
    }

    /**
     * Sets the X509 certificate used in client authentication.
     *
     * @param cert a X509 certificate
     */
    void setPeerCertificate(X509Certificate cert) {
        certificate = cert;
        clientAuthentication = (certificate != null);
    }
    
    /**
     * Sets a boolean flag. If the flag is true, this Message will be sent to
     * a server that requires client authentication.
     *
     * @param clientAuthentication a boolean flag
     */
    public void setClientAuthentication( boolean clientAuthentication) {
        this.clientAuthentication = clientAuthentication;
    }

    /**
     * Sets a token for the sender of this Message. The accual type
     * will be the same as the type of the Object retured from
     * <code>WebServiceAuthenticator.authenticate</code>.
     *
     * @param Object a token Object
     */
    void setToken( Object token) {
        this.token = token;
    }

    /**
     * Returns the SOAP message in String format.
     *
     * @return the SOAP message in String format.
     */
    public String toString() {
        try {
            return XMLUtils.print(toDocument(true).getDocumentElement());
        } catch (Exception ex) {
            Utils.debug.error("Message.toString", ex);
            return "";
        }
    }

    /**
     * Returns the SOAP message in <code>org.w3c.dom.Document</code> format.
     *
     * @return the SOAP message in <code>org.w3c.dom.Document</code> format.
     * @throws SOAPBindingException if an error occurs while constructing
     *                                 a document.
     */
    public Document toDocument() throws SOAPBindingException {
        return toDocument(false);
    }

    /**
     * Returns the SOAP message in <code>org.w3c.dom.Document</code> format.
     *
     * @param refresh true to reconstruct a document, false to reuse a
     *                previous document. If previous document doesn't exist,
     *                it will construct a new document.
     * @return the SOAP message in <code>org.w3c.dom.Document</code> format.
     * @throws SOAPBindingException if an error occurs while constructing
     *                                 the <code>org.w3c.dom.Document</code>.
     */
    public Document toDocument( boolean refresh) throws SOAPBindingException {
        if (!refresh && doc != null) {
            return doc;
        }

        try {
            doc = XMLUtils.newDocument();
        } catch (Exception ex) {
            Utils.debug.error("Message:toDocument", ex);
            throw new SOAPBindingException(ex.getMessage());
        }

        String wsseNS = WSSEConstants.NS_WSSE_WSF11;
        String wsuNS = WSSEConstants.NS_WSU_WSF11;
        if(SOAPBindingConstants.WSF_10_VERSION.equals(wsfVersion)) {
           wsseNS = WSSEConstants.NS_WSSE;
           wsuNS = WSSEConstants.NS_WSU;
        }

        Element envelopeE = doc.createElementNS(SOAPBindingConstants.NS_SOAP,
                                           SOAPBindingConstants.PTAG_ENVELOPE);
        envelopeE.setAttributeNS(SOAPBindingConstants.NS_XML,
                                 SOAPBindingConstants.XMLNS_SOAP,
                                 SOAPBindingConstants.NS_SOAP);
        envelopeE.setAttributeNS(SOAPBindingConstants.NS_XML,
                                 SOAPBindingConstants.XMLNS_SOAP_BINDING,
                                 SOAPBindingConstants.NS_SOAP_BINDING);
        envelopeE.setAttributeNS(SOAPBindingConstants.NS_XML,
                                 SOAPBindingConstants.XMLNS_SOAP_BINDING_11,
                                 SOAPBindingConstants.NS_SOAP_BINDING_11);
        envelopeE.setAttributeNS(SOAPBindingConstants.NS_XML,
                                 WSSEConstants.TAG_XML_WSU,
                                 wsuNS);
        doc.appendChild(envelopeE);
        Element headerE = doc.createElementNS(SOAPBindingConstants.NS_SOAP,
                                             SOAPBindingConstants.PTAG_HEADER);
        envelopeE.appendChild(headerE);
        if (correlationHeader != null) {
            correlationHeader.addToParent(headerE);
        }
        if (consentHeader != null) {
            consentHeader.addToParent(headerE);
        }
        if (usageDirectiveHeaders != null &&
            !usageDirectiveHeaders.isEmpty()) {
            Iterator iter = usageDirectiveHeaders.iterator();
            while(iter.hasNext()) {
                ((UsageDirectiveHeader)iter.next()).addToParent(headerE);
            }
        }

        if (providerHeader != null) {
            providerHeader.addToParent(headerE);
        }

        if (processingContextHeader != null) {
            processingContextHeader.addToParent(headerE);
        }

        if (serviceInstanceUpdateHeader != null) {
            serviceInstanceUpdateHeader.addToParent(headerE);
        }

        if (soapHeaders != null && !soapHeaders.isEmpty()) {
            if (Utils.debug.messageEnabled()) {
                Utils.debug.message("Message.toDocument: adding headers ");
            }
            Iterator iter = soapHeaders.iterator();
            while(iter.hasNext()) {
                Element soapHeaderE = (Element)iter.next();
                headerE.appendChild(doc.importNode(soapHeaderE, true));
            }
        }

        boolean hasSecurityHeaders = 
                (securityHeaders != null && !securityHeaders.isEmpty());
        if (securityProfileType != ANONYMOUS || hasSecurityHeaders) {
            if (Utils.debug.messageEnabled()) {
                Utils.debug.message(
                    "Message.toDocument: adding security headers ");
            }

            Element securityE = doc.createElementNS(wsseNS,
                WSSEConstants.TAG_WSSE + ":" + WSSEConstants.TAG_SECURITYT);
            securityE.setAttributeNS(SOAPBindingConstants.NS_XML,
                WSSEConstants.TAG_XML_WSSE, wsseNS);
            headerE.appendChild(securityE);

            if (assertion != null) {
                Document assertionDoc =
                        XMLUtils.toDOMDocument(assertion.toString(true, true),
                                               Utils.debug);
                if (assertionDoc == null) {
                    String msg =
                        Utils.bundle.getString("cannotProcessSAMLAssertion");
                    Utils.debug.error("Message.Message: " + msg);
                    throw new SOAPBindingException(msg);
                }
                Element assertionE = assertionDoc.getDocumentElement();
                securityE.appendChild(doc.importNode(assertionE, true));
            } else if (binarySecurityToken != null) {
                Document bstDoc =
                        XMLUtils.toDOMDocument(binarySecurityToken.toString(),
                                               Utils.debug);
                if (bstDoc == null) {
                    String msg = Utils.bundle.getString(
                                     "cannotProcessBinarySecurityToken");
                    Utils.debug.error("Message.Message: " + msg);
                    throw new SOAPBindingException(msg);
                }
                Element binarySecurityTokenE = bstDoc.getDocumentElement();
                securityE.appendChild(doc.importNode(binarySecurityTokenE,
                                                     true));
            }

            if (hasSecurityHeaders) {
                Iterator iter = securityHeaders.iterator();
                while(iter.hasNext()) {
                    securityE.appendChild(doc.importNode((Node)iter.next(),
                                                         true));
                }
            }
        }

        Element bodyE = null;
        if (soapFault != null) {
            if (Utils.debug.messageEnabled()) {
                Utils.debug.message("Message.toDocument: adding soapFault ");
            }

            bodyE = doc.createElementNS(SOAPBindingConstants.NS_SOAP,
                                        SOAPBindingConstants.PTAG_BODY);
            envelopeE.appendChild(bodyE);
            soapFault.addToParent(bodyE);
        }

        if (soapBodies != null && !soapBodies.isEmpty()){

            if (Utils.debug.messageEnabled()) {
                Utils.debug.message("Message.toDocument: adding bodies ");
            }

            if (bodyE == null) {
                bodyE = doc.createElementNS(SOAPBindingConstants.NS_SOAP,
                                            SOAPBindingConstants.PTAG_BODY);
                bodyE.setAttributeNS(SOAPBindingConstants.NS_XML,
                    SOAPBindingConstants.XMLNS_SOAP,
                    SOAPBindingConstants.NS_SOAP);
                envelopeE.appendChild(bodyE);
            }

            Iterator iter = soapBodies.iterator();
            while(iter.hasNext()) {
                Element soapBodyE = (Element)iter.next();
                bodyE.appendChild(doc.importNode(soapBodyE, true));
            }

            if (bodyId == null) {
                bodyId = SAMLUtils.generateID();
            }
            if (SOAPBindingConstants.WSF_10_VERSION.equals(wsfVersion)) {
                bodyE.setAttributeNS(null, SOAPBindingConstants.ATTR_id,
                    bodyId);
            } else {
                bodyE.setAttributeNS(wsuNS, WSSEConstants.WSU_ID, bodyId);
            }
        }

        return doc;
    }

    /**
     * Returns the SOAP message in SOAPMessage format.
     *
     * @return the SOAP message in SOAPMessage format.
     * @throws SOAPBindingException if an error occurs while converting
     *                              this object to a SOAP message.
     */
    SOAPMessage toSOAPMessage() throws SOAPBindingException {
        return Utils.DocumentToSOAPMessage(toDocument(true));
    }

    /**
     * Parses a <code>org.w3c.dom.Document</code> to construct this object.
     *
     * @param doc a <code>org.w3c.dom.Document</code>.
     * @throws SOAPBindingException if an error occurs while parsing
     *                              the document
     */
    private void parseDocument( Document doc) throws SOAPBindingException {
        Element envelopeE = doc.getDocumentElement();

        if (Utils.debug.messageEnabled()) {
            Utils.debug.message("Message.parseDocument: doc = " +
                                XMLUtils.print(envelopeE));
        }

        NodeList nl = envelopeE.getChildNodes();
        int length = nl.getLength();

        if (length == 0) {
            String msg = Utils.bundle.getString("soapEnvelopeMissingChildren");
            Utils.debug.error("Message.parseDocument: " + msg);
            throw new SOAPBindingException(msg);
        }

        Element headerE = null;
        Element bodyE = null;
        for(int i = 0; i < length; i++) {
            Node child = nl.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element)child;
                String localName = element.getLocalName();
                String namespaceURI = element.getNamespaceURI();

                if (SOAPBindingConstants.NS_SOAP.equals(namespaceURI)) {
                    if (SOAPBindingConstants.TAG_HEADER.equals(localName)) {
                        headerE = element;
                    } else if(SOAPBindingConstants.TAG_BODY.equals(localName)){
                        bodyE = element;
                    }
                }
            }
        }

        Element securityE = null;
        soapHeaders = new ArrayList();
        // parsing Header element
        if (headerE != null) {
            nl = headerE.getChildNodes();
            length = nl.getLength();
            for (int i = 0; i < length; i++) {
                Node child = nl.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element)child;
		    String localName = element.getLocalName();
		    String ns = element.getNamespaceURI();

                    if (SOAPBindingConstants.NS_SOAP_BINDING.equals(ns)) {
                        if (SOAPBindingConstants.TAG_CORRELATION
                                                .equals(localName)) {
                            correlationHeader = new CorrelationHeader(element);
                        } else if (SOAPBindingConstants.TAG_CONSENT
                                                       .equals(localName)) {
                            consentHeader = new ConsentHeader(element);
                        } else if(SOAPBindingConstants.TAG_USAGE_DIRECTIVE
                                                      .equals(localName)){
                            if (usageDirectiveHeaders == null) {
                                usageDirectiveHeaders = new ArrayList();
                            }
                            usageDirectiveHeaders.add(
                                    new UsageDirectiveHeader(element));
                        } else if (SOAPBindingConstants.TAG_PROVIDER
                                                       .equals(localName)) {
                            providerHeader = new ProviderHeader(element);
                        } else if (SOAPBindingConstants.TAG_PROCESSING_CONTEXT
                                                       .equals(localName)) {
                            processingContextHeader =
                                new ProcessingContextHeader(element);
                        } else {
                            soapHeaders.add(element);
                        }
                    } else if (SOAPBindingConstants.NS_SOAP_BINDING_11
                                                   .equals(ns) &&
                               SOAPBindingConstants
                                                 .TAG_SERVICE_INSTANCE_UPDATE
                                                 .equals(localName)) {

                        serviceInstanceUpdateHeader =
                                new ServiceInstanceUpdateHeader(element);
                    } else if (WSSEConstants.NS_WSSE.equals(ns) ||
                        WSSEConstants.NS_WSSE_WSF11.equals(ns)) {
                        if (WSSEConstants.TAG_SECURITYT.equals(localName)) {
                            securityE = element;
                        } else {
                            soapHeaders.add(element);
                        }
                    } else {
                        soapHeaders.add(element);
                    }
                }
            }
            parseSecurityElement(securityE);
        }

        if (soapHeaders.isEmpty()) {
            soapHeaders = null;
        }

        // parsing Body element

        if (bodyE != null) {
            nl = bodyE.getChildNodes();
            length = nl.getLength();
            for(int i = 0; i < length; i++) {
                Node child = nl.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    Element childE = (Element)child;
                    String localName = childE.getLocalName();
                    String ns = childE.getNamespaceURI();
                    if (soapFault == null &&
                        SOAPBindingConstants.NS_SOAP.equals(ns) &&
                        SOAPBindingConstants.TAG_FAULT.equals(localName)) {
                        soapFault = new SOAPFault(childE);
                    } else {
                        if (soapBodies == null) {
                            soapBodies = new ArrayList();
                        }
                        soapBodies.add(child);
                    }
                }
            }
        }

    }

    /**
     * Sets security profile type by parsing a security element.
     *
     * @param se a security element
     * @throws SOAPBindingException if an error occurs while parsing
     *                              the security element
     */
    private void parseSecurityElement(Element securityE)
    throws SOAPBindingException {
        if (securityE == null) {
            securityProfileType = ANONYMOUS;
            return;
        }

        String wsseNS = securityE.getNamespaceURI();
        if (wsseNS == null) {
            securityProfileType = ANONYMOUS;
            return;
        }
        String wsuNS = null;
        if (wsseNS.equals(WSSEConstants.NS_WSSE_WSF11)) {
            wsfVersion = SOAPBindingConstants.WSF_11_VERSION;
            wsuNS = WSSEConstants.NS_WSU_WSF11;

        } else if(wsseNS.equals(WSSEConstants.NS_WSSE)) {
            wsfVersion = SOAPBindingConstants.WSF_10_VERSION;
            wsuNS = WSSEConstants.NS_WSU;

        } else {
            securityProfileType = ANONYMOUS;
            return;
        }

        NodeList nl = securityE.getElementsByTagNameNS(wsseNS,
            SAMLConstants.TAG_SECURITYTOKENREFERENCE);

        Element securityTokenRefE = null;
        String uri = null;
        if (nl != null && nl.getLength() > 0) {
            securityTokenRefE = (Element)nl.item(0);
            List list = XMLUtils.getElementsByTagNameNS1(securityTokenRefE,
                wsseNS, SAMLConstants.TAG_REFERENCE);
            if (!list.isEmpty()) {
                Element referenceE = (Element)list.get(0);
                uri = XMLUtils.getNodeAttributeValue(referenceE,
                        SAMLConstants.TAG_URI);
                if (uri != null && uri.length() > 1 && uri.startsWith("#")) {
                    uri = uri.substring(1);
                } else {
                    String msg = Utils.bundle.getString("invalidReferenceURI");
                    Utils.debug.error("Message.parseSecurityElement: " + msg);
                    throw new SOAPBindingException(msg);
                }
                if (Utils.debug.messageEnabled()) {
                    Utils.debug.message("Message.parseSecurityElement: " +
                            "SecurityTokenReference Reference URI = " + uri);
                }
            }
        }
        
        securityProfileType = ANONYMOUS;
        securityHeaders = new ArrayList();
        nl = securityE.getChildNodes();
        int length = nl.getLength();
        for(int i = 0; i < length; i++) {
            Node child = nl.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String localName = child.getLocalName();
                String ns = child.getNamespaceURI();
                
                if (securityProfileType != ANONYMOUS) {
                    securityHeaders.add(child);
                    continue;
                }
                
                if (SAMLConstants.BINARYSECURITYTOKEN.equals(localName) &&
                    wsseNS.equals(ns)) {
                    
                    Element binarySecurityTokenE = (Element)child;
                    String valuetype = XMLUtils.getNodeAttributeValue(
                            binarySecurityTokenE,
                            "ValueType");
                    Utils.debug.message("ValueType: "+valuetype);
                    if ((valuetype != null) &&
                            valuetype.endsWith("ServiceSessionContext")) {
                        securityHeaders.add(child);
                        continue;
                    }
                    if (uri != null) {
                        String id = XMLUtils.getNodeAttributeValueNS(
                            binarySecurityTokenE, wsuNS, SAMLConstants.TAG_ID);
                        if (!uri.equals(id)) {
                            securityHeaders.add(child);
                            continue;
                        }
                    }
                    
                    try {
                        binarySecurityToken =
                                new BinarySecurityToken(binarySecurityTokenE);
                        messageCertificate =
                                (X509Certificate)SecurityUtils.getCertificate(
                                binarySecurityToken);
                    } catch (Exception ex) {
                        String msg = Utils.bundle.getString(
                                "cannotProcessBinarySecurityToken");
                        Utils.debug.error("Message.parseSecurityElement: "+
                                msg);
                        throw new SOAPBindingException(msg);
                    }
                    if (Utils.debug.messageEnabled()) {
                        Utils.debug.message("Message.parseSecurityElement:" +
                                " found binary security token");
                    }
                    securityProfileType = X509_TOKEN;
                } else if (SAMLConstants.TAG_ASSERTION.equals(localName) &&
                        SAMLConstants.assertionSAMLNameSpaceURI.equals(ns)){
                    
                    Element assertionE = (Element)child;
                    
                    if (uri != null) {
                        String assertionID = XMLUtils.getNodeAttributeValue(
                                assertionE,
                                SAMLConstants.TAG_ASSERTION_ID);
                        if (!uri.equals(assertionID)) {
                            securityHeaders.add(child);
                            continue;
                        }
                    }
                    
                    try {
                        assertion = new SecurityAssertion(assertionE);
                    } catch (SAMLException ex) {
                        String msg = Utils.bundle.getString(
                                "cannotProcessSAMLAssertion");
                        Utils.debug.error("Message.parseSecurityElement: " +
                                msg);
                        throw new SOAPBindingException(msg);
                    }
                    if (Utils.debug.messageEnabled()) {
                        Utils.debug.message("Message.parseSecurityElement:" +
                                " found security assertion, " +
                                "isBearer = " +
                                assertion.isBearer());
                    }
                    
                    if (assertion.isBearer()) {
                        securityProfileType = BEARER_TOKEN;
                    } else {
                        securityProfileType = SAML_TOKEN;
                        messageCertificate =
                                (X509Certificate)SecurityUtils.getCertificate(
                                assertion);
                    }
                } else {
                    securityHeaders.add(child);
                }
            }
        }
        if (securityHeaders.isEmpty()) {
            securityHeaders = null;
        }
    }

    /**
     * Returns the web services version of the message.
     *
     * @return the web services version.
     */
    public String
    getWSFVersion()
    {
        return wsfVersion;
    }

    /**
     * Sets the web services version to the message.
     *
     * @param version the web services framework version.
     */
    public void
    setWSFVersion(String version)
    {
       this.wsfVersion = version;
    }
}
