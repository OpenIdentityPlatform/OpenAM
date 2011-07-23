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
 * $Id: FSBrowserArtifactConsumerHandler.java,v 1.8 2008/12/19 06:50:46 exu Exp $
 *
 */
package com.sun.identity.federation.services.fednsso;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.LogUtil;
import com.sun.identity.federation.message.FSAssertion;
import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.message.FSRequest;
import com.sun.identity.federation.message.FSResponse;
import com.sun.identity.federation.message.FSSubject;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.plugins.FederationSPAdapter;
import com.sun.identity.federation.services.FSSOAPService;
import com.sun.identity.federation.services.FSSessionManager;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.liberty.ws.meta.jaxb.IDPDescriptorType;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLResponderException;
import com.sun.identity.saml.protocol.Response;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.shared.xml.XMLUtils;
import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * <code>SP</code> side assertion consumer handler handes artifact profile.
 */
public class FSBrowserArtifactConsumerHandler extends FSAssertionArtifactHandler
{
    private FSRequest samlRequest = null;
    
    protected FSBrowserArtifactConsumerHandler() {
    }
    
    /**
     * Constructs a <code>FSBrowserArtifactHandler</code> object.
     * @param request <code>HttpServletRequest</code> object.
     * @param response <code>HttpServletResponse</code> object
     * @param idpDescriptor <code>IDP</code> provider descriptor
     * @param idpEntityId <code>IDP</code> entity id
     * @param doFederate a flag indicating if it is a federation request
     * @param nameIDPolicy <code>nameIDPolicy</code> used
     * @param relayState <code>RelayState</code> url
     */
    public FSBrowserArtifactConsumerHandler(
        HttpServletRequest request, 
        HttpServletResponse response, 
        IDPDescriptorType idpDescriptor, 
        String idpEntityId,
        boolean doFederate, 
        String nameIDPolicy,
        String relayState
    ) 
    {
        super(
            request,
            response,
            idpDescriptor,
            idpEntityId,
            doFederate,
            nameIDPolicy,
            relayState);
    }
    
    /**
     * Constructs a <code>FSBrowserArtifactConsumerHandler</code> object.
     * @param request <code>HttpServletRequest</code> object.
     * @param response <code>HttpServletResponse</code> object
     * @param idpDescriptor <code>IDP</code> provider descriptor
     * @param idpEntityId <code>IDP</code> entity id
     * @param relayState <code>RelayState</code> url
     * @param samlReq <code>FSRequest</code> with artifact
     */
    public FSBrowserArtifactConsumerHandler(
        HttpServletRequest request, 
        HttpServletResponse response, 
        IDPDescriptorType idpDescriptor, 
        String idpEntityId,
        String relayState, 
        FSRequest samlReq
    ) 
    {
        super(request, response, idpDescriptor, idpEntityId,
            false, null, relayState);
        this.samlRequest = samlReq;
        if (FSServiceUtils.getMinorVersion(
                idpDescriptor.getProtocolSupportEnumeration()) ==
            IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) 
        {
           samlRequest.setMinorVersion(
               IFSConstants.FF_12_SAML_PROTOCOL_MINOR_VERSION);
        } else {
           samlRequest.setMinorVersion(
               IFSConstants.FF_11_SAML_PROTOCOL_MINOR_VERSION);
        }
    }
    
    
    /**
     * Builds <code>SAML</code> request (with artifact),
     * sends <code>SAML</code> request to <code>IDP</code> through 
     * <code>SOAP</code>, receives <code>SAML</code> response, then
     * processes the response.
     */
    public void processSAMLRequest() {
        FSUtils.debug.message(
            "FSBrowserArtifactConsumerHandler.processSAMLRequest: Called"); 
        String baseURL = FSServiceUtils.getBaseURL(request);
        String framedPageURL = FSServiceUtils.getCommonLoginPageURL(
            hostMetaAlias, relayState, null, request,baseURL);
        try {
            FSSOAPService soapHelper = FSSOAPService.getInstance();
            samlRequest.setID(samlRequest.getRequestID());
            SOAPMessage msg = soapHelper.bind(
                samlRequest.toXMLString(true, true));
            //sign here
            if (FSServiceUtils.isSigningOn())
            {
                Document doc = (Document)FSServiceUtils.createSOAPDOM(msg);
                IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
                if (metaManager == null) {
                    FSUtils.debug.error("FSBrowserArtifactConsumerHandler." +
                        "processSAMLRequest: could not create meta " +
                        "instance");
                    FSUtils.forwardRequest(request, response, framedPageURL);
                    return;
                }
                String certAlias = 
                    IDFFMetaUtils.getFirstAttributeValueFromConfig(
                        hostConfig, IFSConstants.SIGNING_CERT_ALIAS);
                if (certAlias == null) {
                    FSUtils.debug.error("FSBrowserArtifactConsumerHandler."
                        + "processSAMLRequest: couldn't obtain this site's cert"
                        + " alias.");
                    FSUtils.forwardRequest(request, response, framedPageURL);
                    return;
                }
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSBrowserArtifactConsumerHandler."
                        + "processSAMLRequest: certAlias: " 
                        + certAlias);
                }
                XMLSignatureManager manager = XMLSignatureManager.getInstance();
                int minorVersion = samlRequest.getMinorVersion();  
                if (minorVersion == 
                    IFSConstants.FF_11_SAML_PROTOCOL_MINOR_VERSION) {
                        manager.signXML(
                            doc,
                            certAlias,
                            SystemConfigurationUtil.getProperty(
                                SAMLConstants.XMLSIG_ALGORITHM), 
                            IFSConstants.ID,
                            samlRequest.getID(),
                            false);
                } else if(minorVersion == 
                    IFSConstants.FF_12_SAML_PROTOCOL_MINOR_VERSION) {
                        manager.signXML(
                            doc,
                            certAlias,
                            SystemConfigurationUtil.getProperty(
                                SAMLConstants.XMLSIG_ALGORITHM), 
                            IFSConstants.REQUEST_ID,
                            samlRequest.getRequestID(), 
                            false,
                            IFSConstants.ARTIFACT_XPATH);
                } else { 
                    FSUtils.debug.message("invalid minor version.");
                }
               
                msg = FSServiceUtils.convertDOMToSOAP(doc);
            }                
            //call with saml request
            SOAPMessage retMsg = 
                soapHelper.doSyncCall(response, msg, idpDescriptor, false);
            if (retMsg == null) {
                FSUtils.debug.error("FSBrowserArtifactConsumerHandler."
                    + "processSAMLRequest: " 
                    + FSUtils.bundle.getString("invalidSOAPResponse") 
                    + " Response SOAPMessage is null");
                FSUtils.forwardRequest(request, response, framedPageURL);
                return;
            }
            //getback response
            samlResponseElt = soapHelper.parseSOAPMessage(retMsg);
            if ((samlResponseElt != null) && 
                (samlResponseElt.getLocalName().trim()).equals(
                    "Fault"))
            {
                FSUtils.debug.error("FSBrowserArtifactConsumerHandler."
                    + "processSAMLRequest: " 
                    + FSUtils.bundle.getString("invalidSOAPResponse") 
                    + " SOAPFault occured");
                String[] data = 
                    { FSUtils.bundle.getString("invalidSOAPResponse") };
                LogUtil.error(Level.INFO,LogUtil.INVALID_SOAP_RESPONSE,data);
                FSUtils.forwardRequest(request, response, framedPageURL);
                return;
            } else if ((samlResponseElt != null) && 
                (samlResponseElt.getLocalName().trim()).equals("Response"))
            {
                samlResponse = new FSResponse(samlResponseElt);
                if (samlResponse == null) {
                    FSUtils.debug.error("FSBrowserArtifactConsumerHandler."
                        + "processSAMLRequest: " 
                        + FSUtils.bundle.getString("invalidSOAPResponse") 
                        + " Could not create SAML Response");
                    String[] data = 
                        { FSUtils.bundle.getString("invalidSOAPResponse") };
                    LogUtil.error(
                        Level.INFO,LogUtil.INVALID_SOAP_RESPONSE, data);
                    FSUtils.forwardRequest(request, response, framedPageURL);
                    return;
                }
            } else {
                FSUtils.debug.error("FSBrowserArtifactConsumerHandler."
                    + "processSAMLRequest: " 
                    + FSUtils.bundle.getString("invalidSOAPResponse")
                    + " SOAP response does not contain samlp:Response");
                String[] data = 
                    { FSUtils.bundle.getString("invalidSOAPResponse") };
                LogUtil.error(Level.INFO,LogUtil.INVALID_SOAP_RESPONSE,data);
                FSUtils.forwardRequest(request, response, framedPageURL);
                return;
            }
            //process saml response
            processSAMLResponse((FSResponse)samlResponse);
            return;
        } catch(Exception e){
            StringWriter baos = new StringWriter();
            e.printStackTrace(new PrintWriter(baos));
            FSUtils.debug.error("FSBrowserArtifactConsumerHandler."
                + "processSAMLRequest: Exception occured: " 
                + e.getMessage()+ "\n" + baos.getBuffer().toString());
            try {
                FSUtils.forwardRequest(request, response, framedPageURL);
            } catch(Exception ex){
                FSUtils.debug.error("FSBrowserArtifactConsumerHandler."
                    + "processSAMLRequest: IOException occured: ", e);
            }
            return;
        }
    }
    
    private void processSAMLResponse(FSResponse samlResponse) {
        FSUtils.debug.message(
            "FSBrowserArtifactConsumerHandler.processSAMLResponse: Called");
        String baseURL = FSServiceUtils.getBaseURL(request);
        String framedPageURL = FSServiceUtils.getCommonLoginPageURL(
            hostMetaAlias, relayState, null, request,baseURL);        

        try {
            if (samlResponse == null) {
                FSUtils.debug.error("FSBrowserArtifactConsumerHandler."
                    + "processSAMLResponse: null input "
                    + FSUtils.bundle.getString("missingResponse"));
                String[] data = { FSUtils.bundle.getString("missingResponse") };
                LogUtil.error(Level.INFO,LogUtil.MISSING_RESPONSE,data);
                FSUtils.forwardRequest(request, response, framedPageURL);
                return;
            }
            FederationSPAdapter spAdapter = FSServiceUtils.getSPAdapter(
                hostEntityId, hostConfig);
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSBrowserArtifactConsumerHandler."
                    + "processSAMLResponse: Received "
                    + samlResponse.toXMLString());
            }
            
            boolean valid = verifyResponseStatus(samlResponse);
            if (!valid) {
                FSUtils.debug.error("FSBrowserArtifactConsumerHandler."
                    + "processSAMLResponse: verify Status failed "
                    + FSUtils.bundle.getString("invalidResponse"));
                String[] data = { samlResponse.toXMLString() };
                LogUtil.error(Level.INFO,LogUtil.INVALID_RESPONSE,data);
                if (spAdapter == null ||
                    !spAdapter.postSSOFederationFailure(hostEntityId,
                        request, response, authnRequest, null, samlResponse,
                        FederationSPAdapter.INVALID_RESPONSE))
                {
                    FSUtils.forwardRequest(request, response, framedPageURL);
                }
                return;
            }
            
            // check Assertion
            List assertions = samlResponse.getAssertion();
            if ((assertions == null) || !(assertions.size() > 0)) {
                FSUtils.debug.error("FSBrowserArtifactConsumerHandler."
                    + "processSAMLResponse"
                    + FSUtils.bundle.getString("invalidResponse")
                    + ": No assertion found inside the AuthnResponse");
                String[] data = { samlResponse.toXMLString() };
                LogUtil.error(Level.INFO,LogUtil.INVALID_RESPONSE,data);
                FSUtils.forwardRequest(request, response, framedPageURL);
                return;
            }
            Iterator iter = assertions.iterator();
            FSAssertion assertion = (FSAssertion)iter.next();
            FSAuthnRequest authnRequestRef = 
                getInResponseToRequest(assertion.getInResponseTo());
            if (authnRequestRef == null) {
                FSUtils.debug.error("FSBrowserArtifactConsumerHandler."
                    + "processSAMLResponse: "
                    + FSUtils.bundle.getString("invalidResponse")
                    + ": Assertion does not correspond to any AuthnRequest");
                String[] data = { samlResponse.toXMLString() };
                LogUtil.error(Level.INFO,LogUtil.INVALID_RESPONSE,data);
                FSUtils.forwardRequest(request, response, framedPageURL);
                return;
            }
            
            this.authnRequest = authnRequestRef;
            this.relayState = authnRequest.getRelayState();
            if ((this.relayState == null) || 
                (this.relayState.trim().length() == 0))
            {
                this.relayState = 
                    IDFFMetaUtils.getFirstAttributeValueFromConfig(
                        hostConfig, IFSConstants.PROVIDER_HOME_PAGE_URL);
                if ((this.relayState == null) || 
                    (this.relayState.trim().length() == 0))
                {
                    this.relayState =
                        baseURL + IFSConstants.SP_DEFAULT_RELAY_STATE;
                }
            }
            this.doFederate = authnRequest.getFederate();
            this.nameIDPolicy = authnRequest.getNameIDPolicy();
            
            // Call SP preSSOFederationProcess for Artifact case
            if (spAdapter != null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSBrowserArtifactConsumerHandler, " +
                        "Artifact, Invoke spAdapter.preSSOFederationProcess");
                }
                try {
                    spAdapter.preSSOFederationProcess(
                        hostEntityId, request, response, authnRequest,
                        null, (FSResponse) samlResponse);
                } catch (Exception e) {
                    // log run time exception in Adapter
                    // implementation, continue
                    FSUtils.debug.error("FSAssertionArtifactHandler"
                        + " SPAdapter.preSSOFederationSuccess", e);
                }
            }

            framedPageURL = FSServiceUtils.getCommonLoginPageURL(
                hostMetaAlias, 
                authnRequest.getRelayState(), 
                null,
                request,
                baseURL);
            
            String idpEntityIdRef = getProvider(assertion.getInResponseTo());
            
            if ((idpEntityIdRef == null) ||
                !(idpEntityIdRef.equals(idpEntityId)))
            {
                FSUtils.debug.error("FSBrowserArtifactConsumerHandler."
                    + "processSAMLResponse: " 
                    +  FSUtils.bundle.getString("invalidAssertion")
                    + ": Assertion does not correspond to any IDP");
                String[] data = { FSUtils.bundle.getString("invalidAssertion")};
                LogUtil.error(Level.INFO,LogUtil.INVALID_ASSERTION,data);
                FSUtils.forwardRequest(request, response, framedPageURL);
                return;
            }
            
            FSSubject validSubject =(FSSubject)validateAssertions(assertions);
            if (validSubject == null) {
                FSUtils.debug.error("FSBrowserArtifactConsumerHandler."
                    + "processSAMLResponse: validateAssertions failed: "
                    + FSUtils.bundle.getString("invalidAssertion"));
                String[] data = { FSUtils.bundle.getString("invalidAssertion")};
                LogUtil.error(Level.INFO,LogUtil.INVALID_ASSERTION,data); 
                if (spAdapter == null ||
                    !spAdapter.postSSOFederationFailure(hostEntityId,
                        request, response, authnRequest, null, samlResponse,
                        FederationSPAdapter.INVALID_RESPONSE))
                {
                    FSUtils.forwardRequest(request, response, framedPageURL);
                }
                return;
            }

            if (doFederate) {
                NameIdentifier ni = validSubject.getIDPProvidedNameIdentifier();
                if (ni == null) {
                   ni = validSubject.getNameIdentifier();
                }
                if (ni != null) {
                    int returnCode = doAccountFederation(ni);
                    if (returnCode == FederationSPAdapter.SUCCESS) {
                        // remove it from session manager table
                        FSSessionManager sessionManager =
                            FSSessionManager.getInstance(hostMetaAlias);
                        sessionManager.removeAuthnRequest(
                           assertion.getInResponseTo());
                        return;
                    } else {
                        FSUtils.debug.error("FSBrowserArtifactConsumerHandler."
                            + "processSAMLResponse: "
                            + FSUtils.bundle.getString(
                                "AccountFederationFailed"));
                        String[] data = { FSUtils.bundle.getString(
                                "AccountFederationFailed") };
                        LogUtil.error(
                            Level.INFO, LogUtil.ACCOUNT_FEDERATION_FAILED,data);
                        if (spAdapter == null ||
                            !spAdapter.postSSOFederationFailure(hostEntityId,
                                request, response, authnRequest,
                                authnResponse, samlResponse, returnCode))
                        {
                            FSUtils.forwardRequest(
                                request, response, framedPageURL);
                        }
                    } 
                } else {
                    FSUtils.debug.error("FSBrowserArtifactConsumerHandler."
                        + "processSAMLResponse: Single Sign-On failed. "
                        + "NameIdentifier of the subject is null: ");
                    String[] data = 
                         { FSUtils.bundle.getString("SingleSignOnFailed") };
                    LogUtil.error(Level.INFO,LogUtil.SINGLE_SIGNON_FAILED,data);
                    throw new FSException("missingNIofSubject", null);
                }
            } else {
                // remove it from session manager table
                FSSessionManager sessionManager =
                    FSSessionManager.getInstance(hostMetaAlias);
                sessionManager.removeAuthnRequest(
                    assertion.getInResponseTo());
                NameIdentifier niIdp = 
                    validSubject.getIDPProvidedNameIdentifier();
                NameIdentifier ni = validSubject.getNameIdentifier();
                if (niIdp == null) {
                    niIdp = ni;
                }
                if ((niIdp == null) || (ni == null)) {
                    String[] data = 
                          { FSUtils.bundle.getString("invalidResponse") };
                    LogUtil.error(Level.INFO,LogUtil.INVALID_RESPONSE,data);
                    FSUtils.forwardRequest(request, response, framedPageURL);
                    return;
                }
                
                String idpHandle = niIdp.getName();
                String spHandle = ni.getName();
                int handleType;
                if ((idpHandle == null) || (spHandle == null)) {
                    String[] data =
                        { FSUtils.bundle.getString("invalidResponse") };
                    LogUtil.error(Level.INFO,LogUtil.INVALID_RESPONSE,data);
                    FSUtils.forwardRequest(request, response, framedPageURL);
                    return;
                }
                if (idpHandle.equals(spHandle)) {
                    ni = niIdp;
                    handleType = IFSConstants.REMOTE_OPAQUE_HANDLE;
                } else {
                    handleType = IFSConstants.LOCAL_OPAQUE_HANDLE;
                }
                if (ni != null) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "FSBrowserArtifactConsumerHandler."
                            + "processSAMLResponse: NameIdentifier=" 
                            + ni.getName() 
                            + " securityDomain=" 
                            + ni.getNameQualifier());
                    }

                    Map env = new HashMap();
                    env.put(IFSConstants.FS_USER_PROVIDER_ENV_FSRESPONSE_KEY,
                                samlResponse);
                    int returnCode = doSingleSignOn(
                        ni, handleType, niIdp, env);
                    if (returnCode == FederationSPAdapter.SUCCESS) {
                        String requestID = assertion.getInResponseTo();
                        if (isIDPProxyEnabled(requestID)) {
                            sendProxyResponse(requestID);
                            return;
                        }
                        String[] data = { this.relayState };
                        LogUtil.access(Level.INFO,
                                    LogUtil.ACCESS_GRANTED_REDIRECT_TO,
                                    data,
                                    ssoToken);
                        // Call SP Adapter
                        if (spAdapter != null) {
                            FSUtils.debug.message("Invoke spAdapter");
                            try {
                                if (spAdapter.postSSOFederationSuccess(
                                    hostEntityId, request, response, ssoToken,
                                    authnRequest, null, samlResponse))
                                {
                                    return;
                                }
                            } catch (Exception e) {
                                // log run time exception in Adapter
                                // implementation, continue
                                FSUtils.debug.error("FSAssertionArtifactHandler"
                                    + " SPAdapter.postSSOFederationSuccess:",e);
                            }
                        }

                        redirectToResource(this.relayState);
                        return;
                    } else {
                        FSUtils.debug.error("FSBrowserArtifactConsumerHandler."
                            + "processSAMLResponse: SingleSignOnFailed, ni="
                            + ni.getName() + "[" + ni.getNameQualifier() + "]");
                        String[] data = { ni.getName() };
                        LogUtil.error(
                            Level.INFO,LogUtil.SINGLE_SIGNON_FAILED ,data);
                        if(spAdapter == null ||
                            !spAdapter.postSSOFederationFailure(hostEntityId,
                                request, response, authnRequest,
                                null, samlResponse, returnCode))
                        {
                            FSUtils.forwardRequest(
                                request, response, framedPageURL);
                        }
                        return;
                    }
                } else {
                    FSUtils.debug.error("FSBrowserArtifactConsumerHandler."
                        + "processSAMLResponse: SingleSignOnFailed (null)");
                    String[] data = 
                        { FSUtils.bundle.getString("SingleSignOnFailed") };
                    LogUtil.error(Level.INFO,LogUtil.SINGLE_SIGNON_FAILED,data);
                    throw new FSException("missingNIofSubject", null);
                }
            }
        } catch(Exception e){
            FSUtils.debug.error("FSBrowserArtifactConsumerHandler."
                + "processSAMLResponse: Exception occured: ", e);
            return;
        }
    }
    
    protected void redirectToResource( String resourceURL)
        throws FSException 
    {
        String baseURL = FSServiceUtils.getBaseURL(request);
        String framedPageURL = FSServiceUtils.getCommonLoginPageURL(
                hostMetaAlias, 
                authnRequest.getRelayState(), 
                null,
                request,
                baseURL);
        
        try {
            FSUtils.debug.message(
                "FSBrowserArtifactConsumerHandler.redirectToResource: Called");
            if (resourceURL == null){
                FSUtils.debug.error("FSBrowserArtifactConsumerHandler."
                    + "redirectToResource: Resource URL is null");
                FSUtils.forwardRequest(request, response, framedPageURL);
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSBrowserArtifactConsumerHandler."
                    + "redirectToResource: User's Authentication"
                    + " Assertion verified redirecting to Resource:" 
                    + resourceURL);
            }
            
            response.setContentType("text/html");
            response.sendRedirect(resourceURL);
        } catch(IOException e){
            throw new FSException(e.getMessage());
        }
    }
    
    protected FSAuthnRequest getInResponseToRequest(String requestID) {
        FSUtils.debug.message(
            "FSBrowserArtifactConsumerHandler.getInResponseToRequest: Called");
        FSSessionManager sessionManager = 
            FSSessionManager.getInstance(hostMetaAlias);
        authnRequest = sessionManager.getAuthnRequest(requestID);
        return authnRequest;
    }
    
    protected FSRequest signSAMLRequest(
        FSRequest samlRequest
    ) throws SAMLException 
    {
        FSUtils.debug.message(
             "FSBrowserArtifactConsumerHandler.signSAMLRequest: Called");
        if (samlRequest.isSigned()) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSBrowserArtifactConsumerHandler."
                    + "signSAMLRequest: the request is already signed.");
            }
            throw new SAMLException(
                FSUtils.bundle.getString("alreadySigned"));
        }
        String certAlias = IDFFMetaUtils.getFirstAttributeValueFromConfig(
            hostConfig, IFSConstants.SIGNING_CERT_ALIAS);
        if (certAlias == null || certAlias.length() == 0) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSBrowserArtifactConsumerHandler." +
                    "signSAMLRequest: couldn't obtain this site's cert alias.");
            }
            throw new SAMLResponderException(
                FSUtils.bundle.getString("cannotFindCertAlias"));
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSBrowserArtifactConsumerHandler."
                + "signSAMLRequest: Provider's certAlias is found: " 
                + certAlias);
        }
        XMLSignatureManager manager = XMLSignatureManager.getInstance();
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSBrowserArtifactConsumerHandler."
                + "signSAMLRequest: XMLString to be signed: " 
                + samlRequest.toString(true, true));
        }
        
        String signatureString = 
                manager.signXML(samlRequest.toString(true, true), certAlias);
        
        Element signature = 
                XMLUtils.toDOMDocument(signatureString, FSUtils.debug)
                        .getDocumentElement();
        samlRequest.setSignature(signature);
        return samlRequest;
    } 
}
