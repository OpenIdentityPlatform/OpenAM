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
 * $Id: FSAssertionConsumerService.java,v 1.3 2008/06/25 05:46:57 qcheng Exp $
 *
 */

package com.sun.identity.federation.services.fednsso;

import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.LogUtil;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.key.KeyUtil;
import com.sun.identity.federation.message.FSAssertionArtifact;
import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.message.FSAuthnResponse;
import com.sun.identity.federation.message.FSRequest;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.services.FSServiceManager;
import com.sun.identity.federation.services.FSSessionManager;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.liberty.ws.meta.jaxb.IDPDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.xml.XMLUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.w3c.dom.Document;

/**
 * <code>SP</code> <code>AssertionConsumerService</code>.
 */
public class FSAssertionConsumerService extends HttpServlet {
    private IDFFMetaManager metaManager = null;
    /**
     * Initializes the servlet.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        metaManager = FSUtils.getIDFFMetaManager();
    }

    /**
     * Default constructor.
     */
    public FSAssertionConsumerService() {
    }
    
    /**
     * Handles artifact profile.
     * @param request <code>HttpServletRequest</code> object
     * @param response <code>HttpServletResponse</code> object
     * @exception ServletException, IOException if error occurrs.
     */
    public void doGet(
        HttpServletRequest request, 
        HttpServletResponse response
    ) throws ServletException, IOException 
    {
        if ((request == null) || (response == null)) {
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                FSUtils.bundle.getString("nullInputParameter"));
            return;
        }

        FSUtils.debug.message("FSAssertionConsumerService.doGet(): called");
        String relayState = request.getParameter(IFSConstants.LRURL);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSAssertionConsumerService.doGet():Resource URL: "
                + relayState);
        }
        String metaAlias = FSServiceUtils.getMetaAlias(request);
        String realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
        String baseURL = FSServiceUtils.getBaseURL(request);
        String framedPageURL = FSServiceUtils.getCommonLoginPageURL(
            metaAlias, relayState, null,request,baseURL);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSAssertionConsumerService: CommonLoginPage: " 
                + framedPageURL);
        }
        
        SPDescriptorType hostDesc = null;
        BaseConfigType hostConfig = null;
        String hostEntityId = null;
        try {
            hostEntityId = metaManager.getEntityIDByMetaAlias(metaAlias);
            hostDesc = metaManager.getSPDescriptor(realm, hostEntityId);
            hostConfig = metaManager.getSPDescriptorConfig(
                realm, hostEntityId);
        } catch (Exception e) {
            FSUtils.debug.error("FSAssertionConsumerService.doGet: ", e);
            FSUtils.forwardRequest(request, response, framedPageURL);
            return;
        }

        FSRequest samlRequest = null;
        String firstSourceID = null;
        String artifactName = IFSConstants.ARTIFACT_NAME_DEFAULT;        
        String[] arti =(String[])request.getParameterValues(artifactName);   
        if ((arti == null) ||(arti.length < 0) ||(arti[0] == null)) {
            FSUtils.debug.error("FSAssertionConsumerService.doGet: "
                + "AuthnRequest Processing Failed at the IDP "
                + "Redirecting to the Framed Login Page");
            FSUtils.forwardRequest(request, response, framedPageURL);
        }
        
        List al = new ArrayList();
        try {
            FSAssertionArtifact firstArtifact =
                new FSAssertionArtifact(arti[0]);
            firstSourceID = firstArtifact.getSourceID();
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertionConsumerService.doGet: "
                    + "SourceID within the Artifact is " 
                    + firstSourceID);
            }
            al.add(firstArtifact);
            for (int k = 1; k < arti.length; k++) {
                // check all artifacts coming from the same source id
                FSAssertionArtifact assertArtifact = 
                    new FSAssertionArtifact(arti[k]);
                String dest = assertArtifact.getSourceID();
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAssertionConsumerService.doGet: "
                        + "SourceID within the Artifact is " 
                        + dest);
                }
                if (!dest.equals(firstSourceID)) {
                    FSUtils.debug.error("FSAssertionConsumerService.doGet: " +
                        "Received multiple artifacts have different source id");
                    FSUtils.forwardRequest(request, response, framedPageURL);
                    return;
                }
                al.add(assertArtifact);
            }
            samlRequest = new FSRequest(null, al);
        } catch(SAMLException se) {
            FSUtils.debug.error("FSAssertionConsumerService.doGet: ", se);
            FSUtils.forwardRequest(request, response, framedPageURL);
            return;
        } catch(FSMsgException se) {
            FSUtils.debug.error("FSAssertionConsumerService.doGet: ", se);
            FSUtils.forwardRequest(request, response, framedPageURL);
            return;
        }
        try {
            // handle sso
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertionConsumerService.doGet: "
                    + "Trying to get BrowserArtifactHandler");
            }
            FSServiceManager sm = FSServiceManager.getInstance();
            FSAssertionArtifactHandler handler = sm.getBrowserArtifactHandler(
                request, 
                response,
                realm,
                firstSourceID, 
                samlRequest,
                relayState);
            if (handler == null){
                FSUtils.debug.error("FSAssertionConsumerService.doGet: " 
                    + FSUtils.bundle.getString("internalError"));
                FSUtils.forwardRequest(request, response, framedPageURL);
                return;
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertionConsumerService.doGet: "
                    + "BrowserArtifactHandler created");
            }
            handler.setRealm(realm);
            handler.setHostEntityId(hostEntityId);
            handler.setMetaAlias(metaAlias);
            handler.setHostDescriptor(hostDesc);
            handler.setHostDescriptorConfig(hostConfig);
            handler.processSAMLRequest();
            return;
        } catch(Exception e) {
            FSUtils.debug.error("FSAssertionConsumerService.doGet: "
                + "Exception occurred :", e);
            FSUtils.forwardRequest(request, response, framedPageURL);
            return;
        }
    }
    
    /**
     * Handles post profile.
     * @param request <code>HttpServletRequest</code> object
     * @param response <code>HttpServletResponse</code> object
     * @exception ServletException, IOException if error occurs.
     */
    public void doPost(
        HttpServletRequest request, 
        HttpServletResponse response
    ) throws ServletException, IOException 
    {
        FSUtils.debug.message("FSAssertionConsumerService.doPost : called");
        Document doc = null;
        if ((request == null) ||(response == null)) {
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                FSUtils.bundle.getString("nullInputParameter"));
            return;
        }
        
        String metaAlias = FSServiceUtils.getMetaAlias(request);
        String realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
        String baseURL = FSServiceUtils.getBaseURL(request);
        String framedPageURL = FSServiceUtils.getCommonLoginPageURL(
            metaAlias, null, null, request, baseURL);
        String hostEntityId = null;
        SPDescriptorType hostDesc = null;
        BaseConfigType hostConfig = null;
        try {
            hostEntityId = metaManager.getEntityIDByMetaAlias(metaAlias);
            hostDesc = metaManager.getSPDescriptor(realm, hostEntityId);
            hostConfig = metaManager.getSPDescriptorConfig(realm, hostEntityId);
        } catch (Exception e) {
            FSUtils.debug.error("FSAssertionConsumerService.doPost: " +
                "Exception when obtain host meta data:", e);
            FSUtils.forwardRequest(request, response, framedPageURL);
            return;
        }
        // obtain AuthnResponse message
        String encodedAuthnResponse = 
            request.getParameter(IFSConstants.POST_AUTHN_RESPONSE_PARAM);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSAssertionConsumerService.doPost: "
                + "Base64 encoded AuthnResponse: " + encodedAuthnResponse);
        }
        if (encodedAuthnResponse == null) {
            String[] data =
                { FSUtils.bundle.getString("missingAuthnResponse") };
            LogUtil.error(Level.INFO,LogUtil.MISSING_AUTHN_RESPONSE,data);
            FSUtils.debug.error("FSAssertionConsumerService.doPost: " 
                + FSUtils.bundle.getString("missingAuthnResponse")
                + " AuthnRequest Processing Failed at the IDP"
                + " Redirecting to the Framed Login Page"); 
            FSUtils.forwardRequest(request, response, framedPageURL);
            return;
        }
        
        encodedAuthnResponse = encodedAuthnResponse.replace (' ', '\n');
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSAssertionConsumerService.doPost: "
                + "Base64 encoded AuthnResponse2: " + encodedAuthnResponse);
        }
        
        FSAuthnResponse authnResponse = null;
        try {
            String decodedAuthnResponse = 
                new String(Base64.decode(encodedAuthnResponse));
            FSUtils.debug.message("Decoded authnResponse" +
                decodedAuthnResponse);
            doc = XMLUtils.toDOMDocument(decodedAuthnResponse, FSUtils.debug);
            
            if (doc == null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSAssertionConsumerService.doPost:Error "
                        + "while parsing input xml string");
                }
                throw new FSMsgException("parseError", null);
            }
            authnResponse = new FSAuthnResponse(doc.getDocumentElement());
            if (authnResponse == null){
                FSUtils.debug.error("FSAssertionConsumerService.doPost: "
                    + "Invalid AuthnResponse. "
                    + "Can't parse Base64 encoded AuthnResponse");
                String[] data =
                    { FSUtils.bundle.getString("invalidAuthnResponse") };
                LogUtil.error(Level.INFO,LogUtil.INVALID_AUTHN_RESPONSE,data);
                FSUtils.debug.error("FSAssertionConsumerService.doPost: " 
                    + FSUtils.bundle.getString("invalidAuthnResponse")
                    + " AuthnRequest Processing Failed at the IDP"
                    + " Redirecting to the Framed Login Page");
                FSUtils.forwardRequest(request, response, framedPageURL);
                return;
            }                       
        } catch(FSException e){
            FSUtils.debug.error("FSAssertionConsumerService.doPost: "
                + "Invalid AuthnResponse. FSException"
                + " occured while parsing Base64 encoded AuthnResponse: ", e);
            String[] data =
                { FSUtils.bundle.getString("invalidAuthnResponse") };
            LogUtil.error(Level.INFO,LogUtil.INVALID_AUTHN_RESPONSE,data);
            FSUtils.debug.error("FSAssertionConsumerService.doPost: " 
                + FSUtils.bundle.getString("invalidAuthnResponse")
                + " AuthnRequest Processing Failed at the IDP"
                + " Redirecting to the Framed Login Page");
            FSUtils.forwardRequest(request, response, framedPageURL);
            return;
        } catch(SAMLException e) {
            FSUtils.debug.error("FSAssertionConsumerService.doPost: "
                + "Invalid AuthnResponse. SAMLException"
                + " occurred while parsing Base64 encoded AuthnResponse: ", e);
            String[] data =
                    { FSUtils.bundle.getString("invalidAuthnResponse") };
            LogUtil.error(Level.INFO,LogUtil.INVALID_AUTHN_RESPONSE,data);
            FSUtils.debug.error("FSAssertionConsumerService.doPost: " 
                + FSUtils.bundle.getString("invalidAuthnResponse")
                + " AuthnRequest Processing Failed at the IDP"
                + " Redirecting to the Framed Login Page");
            FSUtils.forwardRequest(request, response, framedPageURL);
            return;
        }
        
        try {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertionConsumerService.doPost: " +
                    "AuthnResponse received is valid: " +
                    authnResponse.toXMLString());
            }
        } catch(FSException e){
            FSUtils.debug.error("FSAssertionConsumerService.doPost: "
                + "Invalid AuthnResponse. FSException"
                + " occurred while calling AuthnResponse.toXMLString(): ", e);
            String[] data =
                { FSUtils.bundle.getString("invalidAuthnResponse") };
            LogUtil.error(Level.INFO,LogUtil.INVALID_AUTHN_RESPONSE,data);
            FSUtils.debug.error("FSAssertionConsumerService.doPost: " 
                + FSUtils.bundle.getString("invalidAuthnResponse")
                + " AuthnRequest Processing Failed at the IDP"
                + " Redirecting to the Framed Login Page");
            FSUtils.forwardRequest(request, response, framedPageURL);
            return;
        }
        
        FSAuthnRequest authnRequest = null;
        String requestID = authnResponse.getInResponseTo();
        if (requestID == null){
            FSUtils.debug.error("FSAssertionConsumerService.doPost: "
                + "Invalid AuthnResponse. AuthnResponse "
                + "received does not have inResponseTo attribute");
            String[] data =
                    { FSUtils.bundle.getString("invalidAuthnResponse") };
            LogUtil.error(Level.INFO,LogUtil.INVALID_AUTHN_RESPONSE,data);
            FSUtils.debug.error("FSAssertionConsumerService.doPost: " 
                + FSUtils.bundle.getString("invalidAuthnResponse")
                + " AuthnRequest Processing Failed at the IDP"
                + " Redirecting to the Framed Login Page");  
            FSUtils.forwardRequest(request, response, framedPageURL);
            return;
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSAssertionConsumerService.doPost: " +
                "AuthnResponse received is against requestID: " + requestID);
        }
        
        authnRequest = getInResponseToRequest(requestID, metaAlias);
        if (authnRequest == null){
            FSUtils.debug.error("FSAssertionConsumerService.doPost: "
                + "Invalid AuthnResponse. AuthnResponse"
                + " received does not have an associated AuthnRequest");
            String[] data =
                { FSUtils.bundle.getString("invalidAuthnResponse") };
            LogUtil.error(Level.INFO,LogUtil.INVALID_AUTHN_RESPONSE,data);
            FSUtils.debug.error("FSAssertionConsumerService.doPost: " 
                + FSUtils.bundle.getString("invalidAuthnResponse")
                + " AuthnRequest Processing Failed at the IDP"
                + " Redirecting to the Framed Login Page");    
            FSUtils.forwardRequest(request, response, framedPageURL);
            return;
        }
        String framedLoginPageURL = FSServiceUtils.getCommonLoginPageURL(
            metaAlias, authnRequest.getRelayState(), null,request,baseURL);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSAssertionConsumerService.doPost: "
                + "inResponseTo validation is successful");
        }        
        
        try {
            String idpEntityId = null;
            IDPDescriptorType idpDescriptor = null;

            if (!authnRequest.getProtocolProfile().equals(
                IFSConstants.SSO_PROF_LECP))
            {
                idpEntityId = getProvider(
                    authnResponse.getInResponseTo(), metaAlias);
                idpDescriptor = metaManager.getIDPDescriptor(
                    realm, idpEntityId);
                if (idpEntityId == null || idpDescriptor == null) {
                    FSUtils.debug.error("FSAssertionConsumerService.doPost: "
                        + "Invalid AuthnResponse. Sender information "
                        + "not found for the received AuthnResponse");
                    String[] data =
                        { FSUtils.bundle.getString("invalidAuthnResponse") };
                    LogUtil.error(
                        Level.INFO,LogUtil.INVALID_AUTHN_RESPONSE,data);
                    FSUtils.debug.error("FSAssertionConsumerService.doPost: " 
                        + FSUtils.bundle.getString("invalidAuthnResponse")
                        + " AuthnRequest Processing Failed at the IDP"
                        + " Redirecting to the Framed Login Page");
                    FSUtils.forwardRequest(
                        request, response, framedLoginPageURL);
                    return;
                } 
                
                if ((FSServiceUtils.isSigningOn () ||
                    (FSServiceUtils.isSigningOptional() && 
                        authnRequest.getProtocolProfile().equals(
                            IFSConstants.SSO_PROF_BROWSER_POST)))
                    && !verifyAuthnResponseSignature(
                        doc, idpDescriptor, idpEntityId))
                {
                    FSUtils.debug.error(
                        "FSAssertionConsumerService.doPost: Signature " +
                        "verification failed");
                    FSUtils.forwardRequest(
                        request, response, framedLoginPageURL);
                    return;
                } 
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAssertionConsumerService.doPost: "
                        + "LECP Profile identified. IDP info is unknown so far"
                        + "Get providerId from the response");
                }
                idpEntityId = authnResponse.getProviderId();
                idpDescriptor = metaManager.getIDPDescriptor(
                    realm, idpEntityId);
            }     
        
       
            // handle sso
            FSServiceManager sm = FSServiceManager.getInstance();
            FSAssertionArtifactHandler handler = sm.getAssertionArtifactHandler(
                    request, 
                    response, 
                    authnRequest, 
                    authnResponse, 
                    idpDescriptor,
                    idpEntityId);
            if (handler == null) {
                FSUtils.debug.error("FSAssertionConsumerService.doPost: "
                    + "could not create AssertionArtifactHandler");
                String[] data = 
                    { FSUtils.bundle.getString("requestProcessingFailed") };
                LogUtil.error(
                    Level.INFO, LogUtil.AUTHN_REQUEST_PROCESSING_FAILED, data);
                FSUtils.debug.error("FSAssertionConsumerService.doPost: "
                    + FSUtils.bundle.getString("requestProcessingFailed") 
                    + " AuthnRequest Processing Failed at the IDP" 
                    + " Redirecting to the Framed Login Page");
                FSUtils.forwardRequest(request, response, framedLoginPageURL);
                return;
            }
            handler.setHostEntityId(hostEntityId);
            handler.setHostDescriptor(hostDesc);
            handler.setHostDescriptorConfig(hostConfig);
            handler.setMetaAlias(metaAlias);
            handler.setRealm(realm);
            handler.processAuthnResponse(authnResponse);
            return;
        } catch (Exception se) {
            FSUtils.debug.error("FSAssertionConsumerService.doPost: "
                + "Exception: ", se);
            FSUtils.debug.error("FSAssertionConsumerService.doPost: " 
                + FSUtils.bundle.getString("requestProcessingFailed")
                + " AuthnRequest Processing Failed at the IDP"
                + " Redirecting to the Framed Login Page");
            FSUtils.forwardRequest(request, response, framedLoginPageURL);
            return;
        }
    }
    
    
    private FSAuthnRequest getInResponseToRequest(
        String requestID, 
        String metaAlias
    ) 
    {
        FSUtils.debug.message(
            "FSAssertionConsumerService::getInResponseToRequest: Called");
        FSSessionManager sessionManager = FSSessionManager.getInstance(
            metaAlias);
        return sessionManager.getAuthnRequest(requestID);
    }
    
    private String getProvider( String requestID, String metaAlias) {
        FSUtils.debug.message("FSAssertionConsumerService.getProvider: Called");
        FSSessionManager sessionManager = FSSessionManager.getInstance(
            metaAlias);
        return sessionManager.getIDPEntityID(requestID);
    }
    
    private boolean verifyAuthnResponseSignature(
        Document doc, IDPDescriptorType idpDescriptor, String idpEntityId) 
    {
        FSUtils.debug.message(
            "FSAssertionConsumerService.verifyAuthnResponseSignature: Called");
        try {
            X509Certificate cert = KeyUtil.getVerificationCert(
                idpDescriptor, idpEntityId, true);
               
            if (cert == null) {
                FSUtils.debug.error("FSAssertionConsumerService."
                    + "verifyAuthnResponseSignature: couldn't obtain "
                    + "this site's cert.");
                return false;
            }
            
            XMLSignatureManager manager = XMLSignatureManager.getInstance();
            return manager.verifyXMLSignature( doc, cert); 
        } catch(Exception e){
            FSUtils.debug.error("FSAssertionConsumerService."
                + "verifyAuthnResponseSignature: Exception occurred while "
                + "verifying signature: "
                , e);
            return false;
        }
    }
}
