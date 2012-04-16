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
 * $Id: FSSOAPReceiver.java,v 1.7 2008/06/25 05:46:56 qcheng Exp $
 *
 */


package com.sun.identity.federation.services;

import com.sun.identity.common.SystemConfigurationException;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.LogUtil;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.key.KeyUtil;
import com.sun.identity.federation.message.FSAssertion;
import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.message.FSFederationTerminationNotification;
import com.sun.identity.federation.message.FSLogoutNotification;
import com.sun.identity.federation.message.FSLogoutResponse;
import com.sun.identity.federation.message.FSNameIdentifierMappingRequest;
import com.sun.identity.federation.message.FSNameIdentifierMappingResponse;
import com.sun.identity.federation.message.FSNameRegistrationRequest;
import com.sun.identity.federation.message.FSNameRegistrationResponse;
import com.sun.identity.federation.message.FSResponse;
import com.sun.identity.federation.message.FSSAMLRequest;
import com.sun.identity.federation.message.common.EncryptedNameIdentifier;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.plugins.FederationSPAdapter;
import com.sun.identity.federation.services.fednsso.FSSSOBrowserArtifactProfileHandler;
import com.sun.identity.federation.services.fednsso.FSSSOLECPProfileHandler;
import com.sun.identity.federation.services.logout.FSLogoutStatus;
import com.sun.identity.federation.services.logout.FSLogoutUtil;
import com.sun.identity.federation.services.logout.FSPreLogoutHandler;
import com.sun.identity.federation.services.namemapping.FSNameMappingHandler;
import com.sun.identity.federation.services.registration.FSNameRegistrationHandler;
import com.sun.identity.federation.services.termination.FSFedTerminationHandler;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.liberty.ws.meta.jaxb.ProviderDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.IDPDescriptorType;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.*;
import com.sun.identity.saml.protocol.*;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.shared.xml.XMLUtils;
import java.io.ByteArrayOutputStream;
import java.net.*;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * <code>SOAP</code> endpoint that handles federation <code>SOAP</code>
 * request.
 */
public class FSSOAPReceiver extends HttpServlet {
    private static MessageFactory msgFactory = null;
    private static FSSOAPService soapService;
    private static final String MESSAGE = "message";
    private static final String USERID = "userID";

    /**
     * Initializes the servlet.
     * @param config <code>ServletConfig</code> object
     * @exception ServletException if error occurrs
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        // initializing the msgFactory field with a default
        // MessageFactory object.
        soapService = FSSOAPService.getInstance();
        try {
            // Initialize it to the default.
            msgFactory = MessageFactory.newInstance();
        } catch (SOAPException ex) {
            FSUtils.debug.error("FSSOAPReceiver:Unable to get message factory"
                , ex);
            throw new ServletException(ex.getMessage());
        }
    }

    /**
     * Default constructor.
     */
    public FSSOAPReceiver() {
    }
 
    /**
     * Handles post request.
     * @param request http request object
     * @param response http response object
     * @exception ServletException, IOException if error occurrs.
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws javax.servlet.ServletException, java.io.IOException 
    {
        FSUtils.debug.message("FSSOAPReceiver.doPost: Called");

        FSUtils.checkHTTPRequestLength(request);

        try {
            MimeHeaders mimeHeaders = SAMLUtils.getMimeHeaders(request);
            ServletInputStream sInputStream = request.getInputStream();
            SOAPMessage soapMessage =
                msgFactory.createMessage(mimeHeaders, sInputStream);

            this.onMessage(request, response, soapMessage);
            return;
        } catch (SOAPException se) {
            throw new ServletException(se);
        }
    }
    
    /**
     * Process the request.
     * @param request http request object
     * @param response http response object
     * @param message received soap message
     */
    public void onMessage(HttpServletRequest request,
                        HttpServletResponse response,
                        SOAPMessage message) 
    {
        FSUtils.debug.message("FSSOAPReceiver.onMessage: Called");
        try {
            Element elt = soapService.parseSOAPMessage(message);
            if (elt == null){
                FSUtils.debug.error("FSSOAPReceiver.onMessage: "
                    + "Error in processing saml:Request. Invalid SOAPMessage");
                response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
                returnSOAPMessage(
                    soapService.formSOAPError(
                        "Server", "cannotProcessRequest", null),
                    response);
                return;
            }
            String eltTagName = (elt.getTagName().trim());
            String ns = elt.getNamespaceURI().trim();
            String nodeName = elt.getLocalName().trim();
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSOAPReceiver.onMessage: "
                    + "tagName: " + eltTagName + " namespaceUri: " + ns
                    + " localName: " + nodeName);
            }
            //check for saml:Request
            if (nodeName.equalsIgnoreCase("Request") &&
                ns.equalsIgnoreCase(IFSConstants.PROTOCOL_NAMESPACE_URI))
            {
                SOAPMessage retMessage = null;
                try {
                    FSSAMLRequest samlRequest = new FSSAMLRequest(elt);
                    IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
                    if (metaManager == null) {
                        FSUtils.debug.error("FSSOAPReceiver.onMessage: "
                                    + "could not create meta instance");
                        response.setStatus(
                            response.SC_INTERNAL_SERVER_ERROR);
                        returnSOAPMessage(
                            soapService.formSOAPError(
                                "Server", "cannotProcessRequest", null),
                            response);
                        return;
                    }
                    String metaAlias = FSServiceUtils.getMetaAlias(request);
                    String realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
                    String hostedEntityId = 
                        metaManager.getEntityIDByMetaAlias(metaAlias);
                    IDPDescriptorType hostedDesc = 
                        metaManager.getIDPDescriptor(realm, hostedEntityId);
                    BaseConfigType hostedConfig = 
                        metaManager.getIDPDescriptorConfig(
                            realm, hostedEntityId);
                    FSServiceManager sm = FSServiceManager.getInstance();
                    FSSSOBrowserArtifactProfileHandler handler =
                        (FSSSOBrowserArtifactProfileHandler)sm
                            .getBrowserArtifactSSOAndFedHandler(
                                request, response, samlRequest);
                    handler.setSOAPMessage(message);
                    handler.setSAMLRequestElement(elt);
                    handler.setHostedEntityId(hostedEntityId);
                    handler.setHostedDescriptor(hostedDesc);
                    handler.setHostedDescriptorConfig(hostedConfig);
                    handler.setMetaAlias(metaAlias);
                    handler.setRealm(realm);
                    FSResponse samlResponse = 
                        handler.processSAMLRequest(samlRequest);

                    if (samlResponse != null) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSSOAPReceiver.onMessage: "
                                + "SAML Response created: "
                                + samlResponse.toXMLString());
                        }
                    } else {
                        FSUtils.debug.error("FSSOAPReceiver.onMessage: "
                            + "SAML Response is null");
                        response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
                        returnSOAPMessage(
                            soapService.formSOAPError(
                                "Server", "cannotProcessRequest", null),
                            response);
                        return;
                    }
                    // introduce id attribute for Assertion bind in 
                    // SOAPEnvelope and sign
                    retMessage = soapService.bind(((FSResponse)samlResponse).
                        toXMLString(true, true));
                    if (FSServiceUtils.isSigningOn()) {
                        List assList = samlResponse.getAssertion();
                        Iterator iter = assList.iterator();
                        while (iter.hasNext()) {
                            FSAssertion assertion = (FSAssertion)iter.next();
                            String id = assertion.getID();
                            Document doc = (Document)
                                FSServiceUtils.createSOAPDOM(retMessage);
                            String certAlias = 
                                IDFFMetaUtils.getFirstAttributeValueFromConfig(
                                    hostedConfig,
                                    IFSConstants.SIGNING_CERT_ALIAS);
                            if (certAlias == null) {
                                if (FSUtils.debug.messageEnabled()) {
                                    FSUtils.debug.message(
                                        "SOAPReceiver.onMessage: couldn't " +
                                        "obtain this site's cert alias.");
                                }
                                throw new SAMLResponderException(
                                    FSUtils.bundle.getString(
                                        "cannotFindCertAlias"));
                            }
                            XMLSignatureManager manager = 
                                XMLSignatureManager.getInstance();
                            int minorVersion = assertion.getMinorVersion();  
                            if (minorVersion == 
                                IFSConstants.FF_11_ASSERTION_MINOR_VERSION) 
                            {
                                manager.signXML(
                                    doc,
                                    certAlias, 
                                    SystemConfigurationUtil.getProperty(
                                        SAMLConstants.XMLSIG_ALGORITHM),
                                    IFSConstants.ID,
                                    id,
                                    false);
                            } else if (minorVersion == 
                                IFSConstants.FF_12_POST_ASSERTION_MINOR_VERSION 
                                       ||
                                minorVersion == 
                                IFSConstants.FF_12_ART_ASSERTION_MINOR_VERSION)
                            {
                                manager.signXML(
                                    doc,
                                    certAlias, 
                                    SystemConfigurationUtil.getProperty(
                                        SAMLConstants.XMLSIG_ALGORITHM),
                                    IFSConstants.ASSERTION_ID, 
                                    assertion.getAssertionID(),
                                    false);
                            } else { 
                                FSUtils.debug.error("invalid minor version.");
                            }
                          
                            retMessage = FSServiceUtils.convertDOMToSOAP(doc);
                        }
                    }
                    if (retMessage == null) {
                        FSUtils.debug.error("FSSOAPReceiver.onMessage: "
                            + "Error in processing saml:Request");
                        response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
                        returnSOAPMessage(
                            soapService.formSOAPError(
                                "Server", "cannotProcessRequest", null),
                            response);
                        return;
                    }
                } catch(SAMLException se){
                    FSUtils.debug.error("FSSOAPReceiver.onMessage: "
                        + "Error in processing saml:Request:" , se);
                    response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
                    returnSOAPMessage(
                        soapService.formSOAPError(
                            "Server", "cannotProcessRequest", null),
                        response);
                    return;
                } catch (IDFFMetaException me) {
                    FSUtils.debug.error("FSSOAPReceiver.onMessage: "
                        + "Error in processing saml:Request:" , me);
                    response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
                    returnSOAPMessage(
                        soapService.formSOAPError(
                            "Server", "cannotProcessRequest", null),
                        response);
                    return;
                }
                returnSOAPMessage(retMessage, response);
                return;
            } if (nodeName.equalsIgnoreCase("AuthnRequest") &&
                (ns.equalsIgnoreCase(IFSConstants.libertyMessageNamespaceURI) ||
                ns.equalsIgnoreCase(IFSConstants.FF_12_XML_NS)))
            {
                SOAPMessage retMessage = null;
                try {
                    FSAuthnRequest authnRequest = new FSAuthnRequest(elt);
                    handleLECPRequest(request, response, authnRequest);
                    retMessage = null;
                } catch(FSException e){
                    FSUtils.debug.error("FSSOAPReceiver.onMessage: "
                        + "Error in processing lecp AuthnRequest:", e);
                    response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
                    returnSOAPMessage(
                        soapService.formSOAPError(
                            "Server", "cannotProcessRequest", null),
                        response);
                    return;
                }
                returnSOAPMessage(retMessage, response);
                return;
            } else if(
                nodeName.equalsIgnoreCase("RegisterNameIdentifierRequest") &&
                (ns.equalsIgnoreCase(IFSConstants.libertyMessageNamespaceURI) ||
                ns.equalsIgnoreCase(IFSConstants.FF_12_XML_NS)))
            {
                SOAPMessage retMessage = null;
                boolean isError = false;
                String providerAlias = null;
                ProviderDescriptorType hostedProviderDesc = null;
                BaseConfigType hostedConfig = null;
                String realm = null;
                String hostedEntityId = null;
                String hostedRole = null;
                try {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSSOAPReceiver.onMessage: "
                            + "Handling NameRegistrationRequest");
                    }
                    
                    IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
                    if (metaManager == null) {
                        FSUtils.debug.message("Unable to get meta manager");
                        isError = true;
                    } else {
                        providerAlias = 
                            FSServiceUtils.getMetaAlias(request);
                        if (providerAlias == null || providerAlias.length() < 1)
                        {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message("Unable to retrieve alias"
                                + "Hosted Provider. Cannot process request");
                            }
                            isError = true;
                        }
                        realm = IDFFMetaUtils.getRealmByMetaAlias(
                            providerAlias);
                        try {
                            hostedRole = 
                                metaManager.getProviderRoleByMetaAlias(
                                    providerAlias);
                            hostedEntityId = metaManager.getEntityIDByMetaAlias(
                                providerAlias);
                            if (hostedRole != null && 
                                hostedRole.equals(IFSConstants.IDP))
                            {
                                hostedProviderDesc = metaManager.
                                    getIDPDescriptor(realm, hostedEntityId);
                                hostedConfig = metaManager.
                                    getIDPDescriptorConfig(
                                        realm, hostedEntityId);
                            } else if (hostedRole != null &&
                                hostedRole.equals(IFSConstants.SP))
                            {
                                hostedProviderDesc = metaManager.
                                    getSPDescriptor(realm, hostedEntityId);
                                hostedConfig = metaManager.
                                    getSPDescriptorConfig(realm,hostedEntityId);
                            }
                                
                            if (hostedProviderDesc == null) {
                                throw new IDFFMetaException((String) null);
                            }
                        } catch(IDFFMetaException eam) {
                            FSUtils.debug.error(
                                "Unable to find Hosted Provider. "
                                + "Cannot process request");
                            isError = true;
                        }
                    }
                    if (isError || hostedProviderDesc == null) {
                        returnSOAPMessage(retMessage, response);
                        return;
                    } else {
                        FSNameRegistrationResponse regisResponse =
                            handleRegistrationRequest(
                                elt, message, hostedProviderDesc, hostedConfig,
                                hostedRole, realm,hostedEntityId, providerAlias,
                                request, response);
                        if (regisResponse == null) {
                            FSUtils.debug.error(
                                "Error in creating NameRegistration Response");
                            response.setStatus(
                                response.SC_INTERNAL_SERVER_ERROR);
                            retMessage =
                                soapService.formSOAPError(
                                    "Server", "cannotProcessRequest", null);
                        } else {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message(
                                    "FSSOAPReceiver.onMessage: "
                                    + "Completed creating response");
                            }
                            retMessage = soapService.bind(
                                regisResponse.toXMLString(true, true));
                            FSUtils.debug.message(
                                "Completed bind message");
                            if (retMessage == null) {
                                FSUtils.debug.error(
                                    "Error in processing NameRegistration " +
                                    "Response");
                                response.setStatus(
                                    response.SC_INTERNAL_SERVER_ERROR);
                                retMessage = soapService.formSOAPError(
                                    "Server", "cannotProcessRequest", null);
                            } else {
                                if (FSServiceUtils.isSigningOn()) {
                                    try {
                                        int minorVersion = 
                                            regisResponse.getMinorVersion(); 
                                        if (minorVersion == IFSConstants.
                                            FF_11_PROTOCOL_MINOR_VERSION) 
                                        {
                                            retMessage = 
                                                signResponse(
                                                    retMessage,
                                                    IFSConstants.ID, 
                                                    regisResponse.getID(),
                                                    hostedConfig); 
                                        } else if(minorVersion == IFSConstants.
                                            FF_12_PROTOCOL_MINOR_VERSION) 
                                        {
                                            retMessage = 
                                                signResponse(
                                                    retMessage, 
                                                    IFSConstants.RESPONSE_ID, 
                                                    regisResponse.
                                                        getResponseID(),
                                                    hostedConfig); 
                                        } else { 
                                            if (FSUtils.debug.messageEnabled()){
                                                FSUtils.debug.message(
                                                    "invalid minor version.");
                                            }
                                        }    
                                        
                                    } catch(SAMLException e) {
                                        FSUtils.debug.error (
                                            "FSNameRegistrationHandler:" +
                                            "sign soap Response failed", 
                                            e);
                                        returnSOAPMessage(
                                            soapService.formSOAPError(
                                                "Server",
                                                "cannotProcessRequest",
                                                null),
                                            response);
                                        return;
                                    } catch(FSMsgException e){
                                        FSUtils.debug.error(
                                            "FSNameRegistrationHandler::" +
                                            "signRegistrationResponse failed",
                                            e);
                                        returnSOAPMessage(
                                            soapService.formSOAPError(
                                                "Server",
                                                "cannotProcessRequest",
                                                null),
                                            response);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                    if (FSUtils.debug.messageEnabled()) {
                        ByteArrayOutputStream bop = null;
                        String xmlString = null;
                        bop = new ByteArrayOutputStream();
                        retMessage.writeTo(bop);
                        xmlString = bop.toString(IFSConstants.DEFAULT_ENCODING);
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("return SOAP message:" +
                                xmlString);
                        }
                    }
                    returnSOAPMessage(retMessage, response);
                    return;
                } catch(Exception se){
                    FSUtils.debug.error(
                        "Error in processing Name Registration request"
                        + se.getMessage());
                    response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
                    retMessage = soapService.formSOAPError(
                        "Server", "cannotProcessRequest", null);
                    returnSOAPMessage(retMessage, response);
                }
            } else if(
                nodeName.equalsIgnoreCase("NameIdentifierMappingRequest") &&
                (ns.equalsIgnoreCase(IFSConstants.libertyMessageNamespaceURI) ||
                ns.equalsIgnoreCase(IFSConstants.FF_12_XML_NS))) 
            {
                FSUtils.debug.message(
                    "FSSOAPReceiver:handling Name Identifier Mapping Request");
                IDFFMetaManager metaManager = 
                    FSUtils.getIDFFMetaManager();
                String metaAlias = FSServiceUtils.getMetaAlias(request);
                String realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
                String hostedEntityId = metaManager.getEntityIDByMetaAlias(
                    metaAlias);
                ProviderDescriptorType hostedDesc = 
                    metaManager.getIDPDescriptor(realm, hostedEntityId);
                BaseConfigType hostedConfig = 
                    metaManager.getIDPDescriptorConfig(realm, hostedEntityId);
                FSNameIdentifierMappingRequest mappingRequest =
                    new FSNameIdentifierMappingRequest(elt);
                if (FSServiceUtils.isSigningOn()) {
                    String remoteEntityId = mappingRequest.getProviderID();
                    ProviderDescriptorType remoteDesc =
                        getRemoteProviderDescriptor(
                            //hostedProviderDesc.getProviderRole(),
                            IFSConstants.IDP, // it has to be idp
                            remoteEntityId,
                            realm);
                    if (remoteDesc == null) {
                        return;
                    }
                    if (verifyRequestSignature(
                        elt, message, 
                        KeyUtil.getVerificationCert(
                            remoteDesc, remoteEntityId, true)))
                    {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSSOAPReceiver: Success in verifying "
                                + "Name Identifier Mapping Request");
                        }
                    } else {
                        FSUtils.debug.error(
                            "Failed verifying Name Identifier Mapping Request");
                        returnSOAPMessage(
                            soapService.formSOAPError(
                                "Server", "cannotProcessRequest", null),
                            response);
                        return;
                    }
                }
                
                String targetNamespace = mappingRequest.getTargetNamespace();
                String inResponseTo = mappingRequest.getRequestID();
                Status status = new Status(new StatusCode("samlp:Success"));
                FSNameMappingHandler idpHandler = new
                    FSNameMappingHandler(hostedEntityId, hostedDesc,
                        hostedConfig, metaAlias);

                NameIdentifier nameIdentifier = idpHandler.getNameIdentifier(
                    mappingRequest,
                    targetNamespace,
                    false);

                String enableEncryption = 
                    IDFFMetaUtils.getFirstAttributeValueFromConfig(
                        hostedConfig, IFSConstants.ENABLE_NAMEID_ENCRYPTION);
                if (enableEncryption != null &&
                    enableEncryption.equalsIgnoreCase("true")) 
                {
                    nameIdentifier = EncryptedNameIdentifier.
                        getEncryptedNameIdentifier(
                            nameIdentifier,realm, targetNamespace);
                }
                FSNameIdentifierMappingResponse mappingResponse = new
                    FSNameIdentifierMappingResponse(hostedEntityId,
                                                    inResponseTo, 
                                                    status,
                                                    nameIdentifier);
                if (FSServiceUtils.isSigningOn()) {
                    String certAlias = 
                        IDFFMetaUtils.getFirstAttributeValueFromConfig(
                            hostedConfig, IFSConstants.SIGNING_CERT_ALIAS);
                    mappingResponse.signXML(certAlias);
                }
                SOAPMessage retMessage =
                    soapService.bind(mappingResponse.toXMLString(true, true));
                
                returnSOAPMessage(retMessage, response);
                return;
            } else if(nodeName.equalsIgnoreCase(
                "FederationTerminationNotification") &&
                (ns.equalsIgnoreCase(IFSConstants.libertyMessageNamespaceURI) ||
                ns.equalsIgnoreCase(IFSConstants.FF_12_XML_NS))) 
            {
                try {
                    FSUtils.debug.message(
                        "calling FSSOAPReceiver::handleTerminationRequest");
                    boolean bHandleStatus = handleTerminationRequest(
                        elt, message, request, response);
                    if (bHandleStatus) {
                        FSUtils.debug.message(
                            "Completed processing terminationRequest");
                        returnTerminationStatus(response);
                        return;
                    } else {
                        FSUtils.debug.message(
                            "Failed processing terminationRequest");
                        returnSOAPMessage(
                            soapService.formSOAPError(
                                "Server", "cannotProcessRequest", null),
                            response);
                        return;
                    }
                } catch(Exception se){
                    FSUtils.debug.error(
                        "Error in processing Federation Termination Request",
                        se);
                    String[] data =
                        { IFSConstants.TERMINATION_REQUEST_PROCESSING_FAILED };
                    LogUtil.error(Level.INFO, 
                            LogUtil.TERMINATION_REQUEST_PROCESSING_FAILED,
                            data);
                    returnSOAPMessage(
                        soapService.formSOAPError(
                            "Server", "cannotProcessRequest", null),
                        response);
                    return;
                }
            } else if (nodeName.equalsIgnoreCase("LogoutRequest") &&
               (ns.equalsIgnoreCase(IFSConstants.libertyMessageNamespaceURI) ||
               ns.equalsIgnoreCase(IFSConstants.FF_12_XML_NS))) 
            {
                try {
                    FSUtils.debug.message(
                        "calling FSSOAPReceiver::handleLogoutRequest");
                    ProviderDescriptorType hostedProviderDesc = null;
                    BaseConfigType hostedConfig = null;
                    String providerAlias = null;
                    String realm = null;
                    String hostedEntityId = null;
                    String hostedRole = null;
                    try {
                        providerAlias = FSServiceUtils.getMetaAlias(request);
                        realm = IDFFMetaUtils.getRealmByMetaAlias(
                            providerAlias);
                        IDFFMetaManager metaManager =
                                FSUtils.getIDFFMetaManager();
                        hostedRole = metaManager.getProviderRoleByMetaAlias(
                            providerAlias);
                        hostedEntityId = metaManager.getEntityIDByMetaAlias(
                            providerAlias);
                        if (hostedRole != null) {
                            if (hostedRole.equalsIgnoreCase(IFSConstants.IDP)) {
                                hostedProviderDesc = 
                                    metaManager.getIDPDescriptor(
                                        realm, hostedEntityId);
                                hostedConfig = 
                                    metaManager.getIDPDescriptorConfig(
                                        realm, hostedEntityId);
                            } else if (hostedRole.equalsIgnoreCase(
                                IFSConstants.SP))
                            {
                                hostedProviderDesc = 
                                    metaManager.getSPDescriptor(
                                        realm, hostedEntityId);
                                hostedConfig = 
                                    metaManager.getSPDescriptorConfig(
                                        realm, hostedEntityId);
                            }
                        }
                    } catch (Exception e){
                       FSUtils.debug.error("FSSOAPReceiver, provider", e);
                    }

                    FSLogoutNotification logoutRequest = 
                        new FSLogoutNotification(elt);

                    Map map = handleLogoutRequest(
                        elt, logoutRequest, message, request, response,
                        hostedProviderDesc,
                        hostedConfig, providerAlias, realm, hostedEntityId,
                        hostedRole);
                    
                    String responseID = SAMLUtils.generateID();
                    String inResponseTo = logoutRequest.getRequestID();
                    String relayState = logoutRequest.getRelayState();
                    FSLogoutResponse resp = null;
                    boolean statusSuccess = false;
                    SOAPMessage retSoapMessage = null;
                    if (map == null) {
                        StatusCode statusCode =
                            new StatusCode(IFSConstants.SAML_RESPONDER);
                        Status status = new Status(statusCode);
                        resp = new FSLogoutResponse(responseID,
                                                    inResponseTo,
                                                    status,
                                                    hostedEntityId,
                                                    relayState);

                    } else {
                        retSoapMessage = (SOAPMessage) map.get(MESSAGE);
                        SOAPPart sp = retSoapMessage.getSOAPPart();
                        SOAPEnvelope se = sp.getEnvelope();
                        SOAPBody sb = se.getBody();
                        if (sb.hasFault()) {
                            StatusCode secondLevelstatusCode = 
                                new StatusCode(IFSConstants.SAML_UNSUPPORTED);
                            StatusCode statusCode = 
                                new StatusCode(IFSConstants.SAML_RESPONDER,
                                                secondLevelstatusCode);
                            Status status = new Status(statusCode);
                            resp = new FSLogoutResponse(responseID,
                                                        inResponseTo,
                                                        status,
                                                        hostedEntityId, 
                                                        relayState);
                        } else {
                            StatusCode statusCode = 
                                new StatusCode(IFSConstants.SAML_SUCCESS);
                            Status status = new Status(statusCode);
                            resp = new FSLogoutResponse(responseID,
                                                        inResponseTo,
                                                        status,
                                                        hostedEntityId, 
                                                        relayState);
                            statusSuccess = true;
                        }
                    }
                    resp.setID(IFSConstants.LOGOUTID);
                    resp.setMinorVersion(logoutRequest.getMinorVersion());
                    retSoapMessage = soapService.bind(
                        resp.toXMLString(true, true));

                    // Call SP Adapter postSingleLogoutSuccess for IDP/SOAP
                    if (hostedRole != null &&
                        hostedRole.equalsIgnoreCase(IFSConstants.SP) && 
                        statusSuccess) 
                    {
                        FederationSPAdapter spAdapter =
                            FSServiceUtils.getSPAdapter(
                                hostedEntityId, hostedConfig);
                        if (spAdapter != null) {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message("FSSOAPReceiver, "
                                    + "call postSingleLogoutSuccess, IDP/SOAP");                            }
                            try {
                                spAdapter.postSingleLogoutSuccess(
                                    hostedEntityId,
                                    request, response, (String) map.get(USERID),                                    logoutRequest, resp,
                                    IFSConstants.LOGOUT_IDP_SOAP_PROFILE);
                            } catch (Exception e) {
                                // ignore adapter exception
                                FSUtils.debug.error("postSingleLogoutSuccess."
                                    + "IDP/SOAP", e);
                            }
                        }
                    }

                    if (FSServiceUtils.isSigningOn()){
                        try{
                            int minorVersion = resp.getMinorVersion(); 
                            if (minorVersion == 
                                IFSConstants.FF_11_PROTOCOL_MINOR_VERSION) 
                            {
                                retSoapMessage = signResponse(
                                    retSoapMessage, 
                                    IFSConstants.ID, 
                                    resp.getID(), 
                                    hostedConfig); 
                            } else if (minorVersion == 
                                IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) 
                            {
                                retSoapMessage = signResponse(
                                    retSoapMessage, 
                                    IFSConstants.RESPONSE_ID, 
                                    resp.getResponseID(), 
                                    hostedConfig); 
                            } else { 
                                FSUtils.debug.error("invalid minor version.");
                            }
                        } catch(SAMLException e){
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message("LogoutResponse failed",
                                    e);
                            }
                            returnSOAPMessage(
                                soapService.formSOAPError(
                                    "Server", "cannotProcessRequest", null),
                                response);
                            return;
                        } catch(FSMsgException e){
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message("LogoutResponse failed",
                                    e);
                            }
                            returnSOAPMessage(
                                soapService.formSOAPError(
                                    "Server", "cannotProcessRequest", null),
                                response);
                            return;
                        } catch (Exception e) {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message("Logout exception:", e);
                            }
                        }
                    }
                    returnSOAPMessage(retSoapMessage, response);
                    return;
                } catch(Exception se){
                    FSUtils.debug.error(
                        "Error in processing logout Request",se);
                    String[] data =
                        { FSUtils.bundle.getString(
                            IFSConstants.LOGOUT_REQUEST_PROCESSING_FAILED) };
                    LogUtil.error(
                        Level.INFO,
                        LogUtil.LOGOUT_REQUEST_PROCESSING_FAILED,
                        data);
                    returnSOAPMessage( 
                        soapService.formSOAPError(
                            "Server", "cannotProcessRequest", null),
                        response);
                    return;
                }
            }
            //check for other Liberty msgs should go here
        } catch(Exception e) {
            FSUtils.debug.error("FSSOAPReceiver.onMessage: "
                + "Error in processing Request: Exception occured: ", e);
            response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
            java.io.ByteArrayOutputStream strm =
                new java.io.ByteArrayOutputStream();
            e.printStackTrace(new java.io.PrintStream(strm));
            FSUtils.debug.error(strm.toString());
            returnSOAPMessage(
                soapService.formSOAPError(
                    "Server", "cannotProcessRequest", null), 
                response);
            return;
        }
        returnSOAPMessage(
            soapService.formSOAPError("Server", "cannotProcessRequest", null),
            response);
        return;
    }
    
    private ProviderDescriptorType getRemoteProviderDescriptor(
        String hostedProviderRole, String remoteEntityId, String realm)
    {
        try {
            IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
            if (metaManager == null) {
                FSUtils.debug.message("Unable to get meta manager");
                return null;
            }
            ProviderDescriptorType remoteDesc = null;
            if (hostedProviderRole.equalsIgnoreCase(IFSConstants.SP)) {
                remoteDesc = metaManager.getIDPDescriptor(realm,remoteEntityId);
            } else {
                remoteDesc = metaManager.getSPDescriptor(realm, remoteEntityId);
            }
            return remoteDesc;
        } catch (IDFFMetaException eam) {
            FSUtils.debug.error(
                "Unable to find Hosted Provider.Cannot process request:", eam);
            return null;
        }
    }

    private FSNameRegistrationResponse handleRegistrationRequest(
        Element elt,
        SOAPMessage msg,
        ProviderDescriptorType hostedProviderDesc,
        BaseConfigType hostedConfig,
        String hostedRole,
        String realm,
        String hostedEntityId,
        String providerAlias,
        HttpServletRequest request,
        HttpServletResponse response)
    {
        try {
            FSNameRegistrationRequest regisRequest =
                new FSNameRegistrationRequest(elt);
            String remoteEntityId = regisRequest.getProviderId();
            boolean isIDP = false;
            if (hostedRole.equalsIgnoreCase(IFSConstants.SP)) {
                isIDP = true;
            }
            ProviderDescriptorType remoteDesc = 
                getRemoteProviderDescriptor(
                    hostedRole,
                    remoteEntityId,
                    realm);
            if (remoteDesc == null) {
                return null;
            }

            X509Certificate cert = KeyUtil.getVerificationCert(
                remoteDesc, remoteEntityId, isIDP);

            if (!FSServiceUtils.isSigningOn() ||
                verifyRequestSignature(elt, msg, cert)) 
            {
                FSUtils.debug.message(
                    "Registration Signature successfully passed");

                IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
                if (metaManager.isTrustedProvider(
                    realm, hostedEntityId, remoteEntityId)) 
                {
                    FSServiceManager instService =
                        FSServiceManager.getInstance();
                    if (instService != null) {
                        FSNameRegistrationHandler regisHandler =
                            new FSNameRegistrationHandler();
                        regisHandler.setHostedDescriptor(
                            hostedProviderDesc);
                        regisHandler.setHostedDescriptorConfig(hostedConfig);
                        regisHandler.setHostedEntityId(hostedEntityId);
                        regisHandler.setHostedProviderRole(hostedRole);
                        regisHandler.setMetaAlias(providerAlias);
                        regisHandler.setRealm(realm);
                        regisHandler.setRemoteDescriptor(remoteDesc);
                        regisHandler.setRemoteEntityId(remoteEntityId);
                        FSNameRegistrationResponse regisResponse =
                            regisHandler.processSOAPRegistrationRequest(
                                request, response, regisRequest);
                        return regisResponse;
                    } else {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSServiceManager instance is null. "
                                + "Cannot process registration request");
                        }
                        return null;
                    }
                }
                FSUtils.debug.error("Remote provider not in trusted list");
                return null;
            } else {
                FSUtils.debug.message(
                    "Registration Signature failed verification");
                return null;
            }
        } catch(Exception se) {
            FSUtils.debug.error(
                "FSNameRegistrationHandler.doPost.doGet:Exception occured ", 
                se );
            return null;
        }
    }
    
    private boolean handleTerminationRequest(
        Element elt,
        SOAPMessage terminationMsg,
        HttpServletRequest request,
        HttpServletResponse response) 
    {
        try {
            String providerAlias = FSServiceUtils.getMetaAlias(request);
            if (providerAlias == null || providerAlias.length() < 1) {
                FSUtils.debug.error("Unable to retrieve alias, Hosted Provider."
                    + "Cannot process  the termination request");
                return false;
            }

            IDFFMetaManager metaManager =
                FSUtils.getIDFFMetaManager();

            if (metaManager == null) {
                FSUtils.debug.error("Unable to get meta manager");
                return false;
            }

            String realm = IDFFMetaUtils.getRealmByMetaAlias(providerAlias);
            FSFederationTerminationNotification terminationRequest =
                new FSFederationTerminationNotification(elt);

            ProviderDescriptorType hostedProviderDesc = null;
            String remoteEntityId = terminationRequest.getProviderId();
            ProviderDescriptorType remoteDesc = null;
            String hostedRole = null;
            String hostedEntityId = null;
            BaseConfigType hostedConfig = null;
            try {
                hostedRole = metaManager.getProviderRoleByMetaAlias(
                    providerAlias);
                hostedEntityId = metaManager.getEntityIDByMetaAlias(
                    providerAlias);
                if (hostedRole == null) {
                    return false;
                } else if (hostedRole.equalsIgnoreCase(IFSConstants.IDP)) {
                    hostedProviderDesc = metaManager.getIDPDescriptor(
                        realm, hostedEntityId);
                    hostedConfig = metaManager.getIDPDescriptorConfig(
                        realm, hostedEntityId);
                    remoteDesc = metaManager.getSPDescriptor(
                        realm, remoteEntityId);
                } else if (hostedRole.equalsIgnoreCase(IFSConstants.SP)) {
                    hostedProviderDesc = metaManager.getSPDescriptor(
                        realm, hostedEntityId);
                    hostedConfig = metaManager.getSPDescriptorConfig(
                        realm, hostedEntityId);
                    remoteDesc = metaManager.getIDPDescriptor(
                        realm, remoteEntityId);
                }
                    
                if (hostedProviderDesc == null) {
                    return false;
                }
                if (remoteDesc == null) {
                    return false;
                }   
            } catch(IDFFMetaException eam) {
                FSUtils.debug.error(
                    "Unable to find Hosted Provider. Cannot process request:",
                    eam);
                return false;
            }

            X509Certificate cert = KeyUtil.getVerificationCert(
                remoteDesc, remoteEntityId, true);
            
            if (!FSServiceUtils.isSigningOn() ||
                verifyRequestSignature(elt, terminationMsg, cert)) 
            {
                FSUtils.debug.message(
                    "Termination Signature successfully verified");
                
                if (metaManager.isTrustedProvider(
                    realm, hostedEntityId, remoteEntityId)) 
                {
                    FSServiceManager instService =
                        FSServiceManager.getInstance();
                    if (instService != null) {
                        FSFedTerminationHandler terminationHandler =
                            instService.getFedTerminationHandler(
                                terminationRequest, hostedConfig, 
                                realm, hostedEntityId, hostedRole, 
                                providerAlias, remoteEntityId);
                        if (terminationHandler != null) {
                            terminationHandler.setHostedDescriptor(
                                hostedProviderDesc);
                            terminationHandler.setHostedDescriptorConfig(
                                hostedConfig);
                            terminationHandler.setRealm(realm);
                            terminationHandler.setHostedEntityId(
                                hostedEntityId);
                            terminationHandler.setMetaAlias(providerAlias);
                            terminationHandler.setHostedProviderRole(
                                hostedRole);
                            terminationHandler.setRemoteEntityId(
                                remoteEntityId);
                            terminationHandler.setRemoteDescriptor(
                                remoteDesc);
                            boolean bProcessStatus = terminationHandler.
                                processSOAPTerminationRequest(
                                    request, response, terminationRequest);
                            return bProcessStatus;
                        } else {
                            FSUtils.debug.error(
                                "Unable to get Termination Handler");
                            return false;
                        }
                    } else {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSServiceManager instance is null. "
                                + "Cannot process termination request");
                        }
                        return false;
                    }
                }
                FSUtils.debug.message("Remote provider not in trusted list");
                return false;
            } else {
                FSUtils.debug.message(
                    "Termination Signature failed verification");
                return false;
            }
        } catch(Exception se) {
            FSUtils.debug.error(
                "FSSOAPService::handleTerminationRequest failed ", se);
            return false;
        }
    }
    
    /**
     * Initiates the processing of the logout request received from a remote
     * trusted provider.
     * @param elt containing the logout request in the XML message
     * @param logoutRequest logout notification
     * @param msgLogout logout message
     * @param request http request object
     * @param response http response object
     * @param hostedProviderDesc hosted provider meta descriptor
     * @param hostedConfig hosted provider's extended meta
     * @param providerAlias hosted provider's meta alias
     * @param realm The realm under which the entity resides.
     * @param hostedEntityId hosted provider's entity ID
     * @param hostedRole hosted provider's role
     * @return null if error in processing, or Map containing two
     * keys, MESSAGE for SOAPMessage object and USERID for userID string
     */
    private Map handleLogoutRequest(
        Element elt, 
        FSLogoutNotification logoutRequest,       
        SOAPMessage msgLogout,
        HttpServletRequest request,
        HttpServletResponse response,
        ProviderDescriptorType hostedProviderDesc,
        BaseConfigType hostedConfig,
        String providerAlias,
        String realm,
        String hostedEntityId,
        String hostedRole) 
    {
        try {
            String remoteEntityId = logoutRequest.getProviderId();
            ProviderDescriptorType remoteDesc = 
                getRemoteProviderDescriptor(
                    hostedRole, remoteEntityId, realm);
            if (remoteDesc == null) {
                return null;
            }
            boolean isIDP = false;
            if (hostedRole.equalsIgnoreCase(IFSConstants.SP)) {
                isIDP = true;
            }
            X509Certificate remoteCert = KeyUtil.getVerificationCert(
                remoteDesc, remoteEntityId, isIDP);

            if (!FSServiceUtils.isSigningOn() || 
                verifyRequestSignature(elt, msgLogout, remoteCert)) 
            {
                FSUtils.debug.message("Logout Signature successfully verified");
                if (providerAlias == null || providerAlias.length() < 1) {
                    FSUtils.debug.message("Unable to retrieve alias, " +
                        "Hosted Provider Cannot process logout request");
                    return null;
                }

                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSSOAPReceiver:handleLogoutRequest: "
                        + "Completed forming request FSLogoutNotification");
                }

                IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
                if (metaManager.isTrustedProvider(
                    realm, hostedEntityId, remoteEntityId)) 
                {
                    String userID = FSLogoutUtil.getUserFromRequest(
                        logoutRequest, realm, hostedEntityId, hostedRole,
                        hostedConfig, providerAlias);
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "FSSOAPReceiver:handleLogoutRequest"
                            + " found user Id = " + userID);
                    }
                    // Call SP Adapter preSingleLogoutProcess for IDP/SOAP
                    if (hostedRole != null &&
                        hostedRole.equalsIgnoreCase(IFSConstants.SP)) 
                    {
                        FederationSPAdapter spAdapter =
                            FSServiceUtils.getSPAdapter(
                                hostedEntityId, hostedConfig);
                        if (spAdapter != null) {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message("FSSOAPReceiver, " +
                                "call preSingleLogoutProcess, IDP/SOAP");
                            }
                            try {
                                spAdapter.preSingleLogoutProcess(
                                    hostedEntityId,
                                    request, response, userID,
                                    logoutRequest, null,
                                    IFSConstants.LOGOUT_IDP_SOAP_PROFILE);
                            } catch (Exception e){
                                // ignore adapter process error
                                FSUtils.debug.error("preSingleLogoutProcess." +
                                    "IDP/SOAP", e);
                            }
                        }
                    }

                    // TODO : change to use FSLogoutUtil.liveConnectionsExist
                    if (!isUserExists(userID, providerAlias)) {
                        // Need to get the list of servers from the 
                        // platform list and make a call to each of them 
                        //to do the cleanup
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSSOAPReceiver:handleLogoutRequest: User "+
                                "does not exist locally. Finding remotely");
                        }
                        List platformList = null;
                        try {
                            platformList =
                                SystemConfigurationUtil.getServerList();
                        } catch (SystemConfigurationException se) {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message(
                                    "FSSOAPReceiver:handleLogoutRequest: " +
                                    "Couldn't find remote server:", se);
                            }
                        }
                        if (platformList == null) {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message(
                                    "FSSOAPReceiver:handleLogoutRequest"
                                    + "platformList is null");
                            }
                            return null;
                        }
                        Iterator iter = platformList.iterator();
                        while(iter.hasNext()) {
                            String remoteServerURL = (String)iter.next();
                            StringBuffer tmpremoteURL = 
                                new StringBuffer(remoteServerURL);
                            tmpremoteURL.append(
                                SystemConfigurationUtil.getProperty(
                                    "com.iplanet.am.services." +
                                    "deploymentDescriptor"));
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message(
                                    "FSSOAPReceiver:handleLogoutRequest"
                                    + "remoteServerURL = " 
                                    + remoteServerURL
                                    + " and self serverUrl ="
                                    + FSServiceUtils.getBaseURL());
                            }
                              
                            if ((FSServiceUtils.getBaseURL()).
                                equalsIgnoreCase(
                                    remoteServerURL.toString()))
                            {
                                continue;
                            }
                            FSAssertionManagerClient amc =
                                new FSAssertionManagerClient(
                                    providerAlias,
                                    getFullServiceURL(remoteServerURL));
                            if (amc.isUserExists(userID)) {
                                if (FSUtils.debug.messageEnabled()) {
                                    FSUtils.debug.message(
                                        "FSSOAPReceiver:handleLogoutRequest"
                                        + "user found here =" 
                                        + remoteServerURL);
                                }

                                StringBuffer remoteURL = new StringBuffer();
                                remoteURL.append(remoteServerURL.toString())
                                    .append(
                                        SystemConfigurationUtil.getProperty(
                                            "com.iplanet.am.services." +
                                            "deploymentDescriptor"))
                                    .append(
                                        IFSConstants.SOAP_END_POINT_VALUE)
                                    .append("/")
                                    .append(IFSConstants.META_ALIAS)
                                    .append(providerAlias);
                                FSSOAPService instSOAP = 
                                    FSSOAPService.getInstance();
                                SOAPMessage retSOAPMessage = null;
                                if (instSOAP != null){
                                    try {
                                        if (FSUtils.debug.messageEnabled()){
                                            FSUtils.debug.message(
                                               "Forward logout request to "
                                               + remoteURL.toString());
                                        }
                                        retSOAPMessage = 
                                            instSOAP.sendMessage(
                                                msgLogout,
                                                remoteURL.toString());
                                        if (retSOAPMessage != null) {
                                            Map map = new HashMap();
                                            map.put(MESSAGE, retSOAPMessage);
                                            if (userID != null) {
                                                map.put(USERID, userID);
                                            }
                                            return map;
                                        } else {
                                            return null;
                                        }
                                    } catch(SOAPException e){
                                        FSUtils.debug.error(
                                        "FSSOAPException in doSOAPProfile"
                                        + " Cannot send request", e );
                                        return null;
                                    }
                                } else {
                                    return null;
                                }
                            }
                        }
                    }
                    FSServiceManager instService =
                        FSServiceManager.getInstance();
                    if (instService != null) {
                        FSPreLogoutHandler logoutHandler =
                            instService.getPreLogoutHandler();
                        if (logoutHandler != null) {
                            logoutHandler.setHostedDescriptor(
                                hostedProviderDesc);
                            logoutHandler.setHostedDescriptorConfig(
                                hostedConfig);
                            logoutHandler.setHostedEntityId(hostedEntityId);
                            logoutHandler.setHostedProviderRole(hostedRole);
                            logoutHandler.setMetaAlias(providerAlias);
                            logoutHandler.setRealm(realm);
                            logoutHandler.setRemoteDescriptor(remoteDesc);
                            logoutHandler.setRemoteEntityId(remoteEntityId);
                            logoutHandler.setLogoutRequest(logoutRequest);
                            FSLogoutStatus bProcessStatus =
                                logoutHandler.processSingleLogoutRequest(
                                    logoutRequest);
                            if (bProcessStatus.getStatus().
                                equalsIgnoreCase(IFSConstants.SAML_SUCCESS))
                            {
                                MessageFactory factory = 
                                    MessageFactory.newInstance();        
                                SOAPMessage successSOAP =
                                    factory.createMessage();
                                if (successSOAP != null) {
                                    Map map = new HashMap();
                                    map.put(MESSAGE, successSOAP);
                                    if (userID != null) {
                                        map.put(USERID, userID);
                                    }
                                    return map;
                                } else {
                                    return null;
                                }
                            } else  if (bProcessStatus.getStatus().
                                equalsIgnoreCase(
                                    IFSConstants.SAML_UNSUPPORTED)) 
                            {
                                SOAPMessage retSOAPMessage =
                                    soapService.formSOAPError(
                                    "Server", "cannotProcessRequest", null);
                                if (retSOAPMessage != null) {
                                    Map map = new HashMap();
                                    map.put(MESSAGE, retSOAPMessage);
                                    if (userID != null) {
                                        map.put(USERID, userID);
                                    }
                                    return map;
                                } else {
                                    return null;
                                }
                            } else {
                                return null;
                            }
                        } else {
                            FSUtils.debug.error(
                                "Unable to get PreLogoutHandler");
                            FSUtils.debug.error("Cannot process request");
                            return null;
                        }
                    } else {
                        FSUtils.debug.message("FSServiceManager instance is"
                            + "null. Cannot process logout request");
                        return null;
                    }
                }
                FSUtils.debug.message("Remote provider not in trusted list");
                return null;
            } else {
                FSUtils.debug.error("Logout Signature failed verification");
                return null;
            }
        } catch (Exception se) {
            FSUtils.debug.error("FSSOAPService::handleLogoutRequest failed",se);
            return null;
        }
    }
    
    private void handleLECPRequest(
        HttpServletRequest request,
        HttpServletResponse response,
        FSAuthnRequest authnRequest
    ) {
        FSUtils.debug.message("FSSOAPReceiver.handleLECPRequest: Called");
        try {
            String metaAlias = FSServiceUtils.getMetaAlias(request);
            String realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
            IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
            String hostedEntityId = metaManager.getEntityIDByMetaAlias(
                metaAlias);
            FSSessionManager sessionService = FSSessionManager.
                getInstance(metaAlias);
            sessionService.setAuthnRequest(
                authnRequest.getRequestID(), authnRequest);
            // handle sso
            FSServiceManager sm = FSServiceManager.getInstance();
            FSSSOLECPProfileHandler handler = sm.getLECPProfileHandler(
                request, response, authnRequest, realm);
            handler.setMetaAlias(metaAlias);
            handler.setHostedEntityId(hostedEntityId);
            handler.setHostedDescriptor(
                metaManager.getIDPDescriptor(realm, hostedEntityId));
            handler.setHostedDescriptorConfig(
                metaManager.getIDPDescriptorConfig(realm, hostedEntityId));
            handler.setRealm(realm);
            handler.processLECPAuthnRequest(authnRequest);
        } catch(Exception se) {
            FSUtils.debug.error(
                "FSSSOAndFedService.onMessage: Exception occured ", se);
            return;
        }
    }
    
    /**
     * Verifies the signature on the request received from a remote trusted 
     * provider.
     * @param elt containing the logout request in the XML message
     * @param msg request soap message
     * @param cert Certificate to be used in verifying the signature.
     * @return boolean <code>true</code> if signature verfication successful;
     *  otherwise return <code>false.
     */
    protected boolean verifyRequestSignature(
        Element elt, 
        SOAPMessage msg, 
        X509Certificate cert)
    {
        FSUtils.debug.message("FSSOAPReceiver::verifyRequestSignature: Called");
        try {
            if (cert == null) {
                FSUtils.debug.error("FSSOAPReceiver.verifyRequestSignature" +
                    ": couldn't obtain this site's cert.");
                throw new SAMLResponderException(
                    FSUtils.bundle.getString(IFSConstants.NO_CERT));
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSOAPReceiver::verifyRequestSignature: "
                + "Provider's cert is found. "
                + "\nxmlString to be verified: " + XMLUtils.print(elt));
            }
            XMLSignatureManager manager = XMLSignatureManager.getInstance();
            Document doc = (Document)FSServiceUtils.createSOAPDOM(msg);
            return manager.verifyXMLSignature(doc, cert);
        } catch(Exception e){
            FSUtils.debug.error("FSSOPAReceiver::verifyRequestSignature " +
                " Exception occured while verifying signature:", e);
            return false;
        }
    }
    
    /**
     * Signs SOAP Response before sending it to the provider.
     * @param msg the response message to be sent to provider
     * @param idAttrName name of the id attribute to be signed
     * @param id the value of the id attributer to be signed
     * @param hostedConfig hosted provider's extended meta
     * @return SOAPMessage the signed response message
     * @exception SAMLException, FSMsgException if error occurrs
     */
    protected SOAPMessage signResponse (
        SOAPMessage msg,
        String idAttrName,
        String id,
        BaseConfigType hostedConfig)
        throws SAMLException, FSMsgException 
    {
        FSUtils.debug.message("FSSOAPReceiver::Entered signResponse::");
        String certAlias = IDFFMetaUtils.getFirstAttributeValueFromConfig(
            hostedConfig, IFSConstants.SIGNING_CERT_ALIAS);
        if (certAlias == null || certAlias.length() == 0) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSOAPReceiver.signResponse:"
                + " couldn't obtain this site's cert alias.");
            }
            throw new SAMLResponderException(
                FSUtils.bundle.getString(IFSConstants.NO_CERT_ALIAS));
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "signResponse::Provider's certAlias is found: " +
                certAlias);
        }
        XMLSignatureManager manager = XMLSignatureManager.getInstance();
        Document doc = (Document)FSServiceUtils.createSOAPDOM(msg);
        String xpath = "//*[local-name()=\'ProviderID\']";
        manager.signXML(
            doc,
            certAlias,
            SystemConfigurationUtil.getProperty(
                SAMLConstants.XMLSIG_ALGORITHM),
            idAttrName,
            id,
            false,
            xpath);
        return FSServiceUtils.convertDOMToSOAP(doc);
    }
    
    private void returnSOAPMessage(
        SOAPMessage msg,
        HttpServletResponse response) 
    {
        try {
            if (msg != null) {
                SAMLUtils.setMimeHeaders(msg.getMimeHeaders(), response);
                ServletOutputStream servletoutputstream =
                    response.getOutputStream();
                msg.writeTo(servletoutputstream);
                servletoutputstream.flush();
                return;
            } else {
                response.flushBuffer();
                return;
            }
        } catch(Exception e) {
            FSUtils.debug.error(
                "FSSOAPReceiver.returnSOAPMessage: Exception::", e);
            return;
        }
    }

    /**
     * Federation termination must send 204 status when it succeeds. 
     */
    private void returnTerminationStatus(HttpServletResponse response) {
        try {
            response.setStatus(204);
            response.flushBuffer();
            return;
        } catch(Exception e) {
            FSUtils.debug.error(
                "FSSOAPReceiver.returnFedTerminationStatus: Exception::", e);
        }
    }

    private boolean isUserExists(String userDN, String providerAlias) {
       try {
            FSSessionManager sessionMgr = FSSessionManager.getInstance(
                providerAlias);
            synchronized(sessionMgr) {
                FSUtils.debug.message("About to call getSessionList");
                List sessionList = sessionMgr.getSessionList(userDN);
                if (sessionList == null) {
                    FSUtils.debug.message(
                        "SOAPReceiver:isUserExists:List is empty");
                    return false;
                } else {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "SOAPReceiver:isUserExists: List is not empty. "
                            + "User found: " + userDN);
                    }
                    return true;
                }
            }
       } catch(Exception e) {
           if (FSUtils.debug.messageEnabled()) {
               FSUtils.debug.message("SOAPReceiver.isUserExists:", e);
           }
           return false;
        }
    }

    private String getFullServiceURL(String shortUrl) {
        String result = null;
        String SERVICE_NAMING = "fsassertionmanager";
        try {
            URL u = new URL(shortUrl);
            URL weburl = SystemConfigurationUtil.getServiceURL(
                SERVICE_NAMING, u.getProtocol(), u.getHost(), u.getPort(),
                u.getPath());
            result = weburl.toString();
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "AssertionManager.getFullServiceURL:full remote URL is: " +
                    result);
            }
        } catch (Exception e) {
            if (FSUtils.debug.warningEnabled()) {
                FSUtils.debug.warning(
                    "AssertionManager.getFullServiceURL:Exception:", e);
            }
        }
        return result;
    }
}
