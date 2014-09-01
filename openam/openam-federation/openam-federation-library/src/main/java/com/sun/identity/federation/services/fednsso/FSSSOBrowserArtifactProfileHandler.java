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
 * $Id: FSSSOBrowserArtifactProfileHandler.java,v 1.6 2008/12/19 06:50:46 exu Exp $
 *
 */

/*
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */

package com.sun.identity.federation.services.fednsso;

import com.sun.identity.federation.services.FSAssertionManager;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.federation.message.FSResponse;
import com.sun.identity.federation.message.FSSAMLRequest;
import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.message.FSAssertionArtifact;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.LogUtil;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.key.KeyUtil;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.assertion.Conditions;
import com.sun.identity.saml.assertion.AudienceRestrictionCondition;
import com.sun.identity.saml.protocol.StatusCode;
import com.sun.identity.saml.protocol.Request;
import com.sun.identity.saml.protocol.Status;
import com.sun.identity.saml.protocol.AssertionArtifact;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLResponderException;
import com.sun.identity.saml.common.SAMLUtils;

import com.sun.identity.shared.encode.URLEncDec;
import org.forgerock.openam.utils.ClientUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPMessage;

import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;

import org.w3c.dom.Element;
import org.w3c.dom.Document;

/**
 * <code>IDP</code> single sign on service handler handles browser artifact
 * profile.
 */
public class FSSSOBrowserArtifactProfileHandler extends FSSSOAndFedHandler {
    
    private Element samlRequestElement = null;
    private SOAPMessage soapMsg = null;

    /**
     * Sets <code>SOAP</code> message.
     * @param msg <code>SOAPMessage</code> object
     */
    public void setSOAPMessage(SOAPMessage msg) {
        soapMsg = msg;
    }

    /**
     * Sets <code>SAML</code> request element.
     * @param root <code>SAML</code> request element
     */
    public void setSAMLRequestElement(Element root) {
        FSUtils.debug.message(
            "FSBrowserArtifactConsumerHandler.setSAMLRequestElement: Called");
        samlRequestElement = root;
    }
    
    protected FSSSOBrowserArtifactProfileHandler() {
    }
    
    /**
     * Constructor.
     * @param request <code>HttpServletRequest</code> object
     * @param response <code>HttpServletResponse</code> object
     * @param authnRequest authentication request
     * @param spDescriptor <code>SP</code>'s provider descriptor
     * @param spConfig <code>SP</code>'s extended meta config
     * @param spEntityId <code>SP</code>'s entity id
     * @param relayState where to go after single sign on is done
     */
    public FSSSOBrowserArtifactProfileHandler(
        HttpServletRequest request,
        HttpServletResponse response, 
        FSAuthnRequest authnRequest, 
        SPDescriptorType spDescriptor, 
        BaseConfigType spConfig,
        String spEntityId,
        String relayState) 
    {
        super(request, response, authnRequest, spDescriptor, 
            spConfig, spEntityId, relayState);
    }
    
    /**
     * Constructor.
     * @param request <code>HttpServletRequest</code> object
     * @param response <code>HttpServletResponse</code> object
     * @param samlRequest <code>Request</code> object that contains artifact
     */
    public FSSSOBrowserArtifactProfileHandler(
        HttpServletRequest request, 
        HttpServletResponse response, 
        Request samlRequest
    ) 
    {
        this.request = request;
        this.response = response;
        //this.samlRequest = samlRequest;
    }
    
    /**
     * Processes authentication request.
     * @param authnRequest authentication request
     * @param bPostAuthn <code>true</code> indicates it's post authentication;
     *  <code>false</code> indicates it's pre authentication.
     */
    @Override
    public void processAuthnRequest(
        FSAuthnRequest authnRequest, 
        boolean bPostAuthn)
    {
        FSUtils.debug.message(
            "FSSSOBrowserArtifactProfileHandler.processAuthnRequest: Called");
        try {
            if (bPostAuthn){
                if (processPostAuthnSSO(authnRequest)){
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "FSSSOBrowserArtifactProfileHandler."
                            + "processAuthnRequest: AuthnRequest Processing"
                            + "successful");
                    }
                } else {
                    if (FSUtils.debug.warningEnabled()) {
                        FSUtils.debug.warning(
                            "FSSSOBrowserArtifactProfileHandler."
                            + "processAuthnRequest: AuthnRequest Processing "
                            + "failed");
                    }
                    String[] data = { 
                        FSUtils.bundle.getString("AuthnRequestProcessingFailed")
                        };
                    LogUtil.error(Level.INFO,
                        LogUtil.AUTHN_REQUEST_PROCESSING_FAILED,
                        data,
                        ssoToken);
                    sendSAMLArtifacts(null);
                }
            } else {
                boolean authnRequestSigned = 
                    spDescriptor.isAuthnRequestsSigned();
                
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSSSOBrowserArtifactProfileHandler."
                        + "processAuthnRequest: ProviderID : " 
                        + spEntityId
                        + " AuthnRequestSigned : " 
                        + authnRequestSigned);
                }
                if (FSServiceUtils.isSigningOn()){
                    if (authnRequestSigned){
                        if (!verifyRequestSignature(authnRequest)){
                            FSUtils.debug.error(
                                "FSSSOBrowserArtifactProfileHandler."
                                + "processAuthnRequest: "
                                + "AuthnRequest Signature Verification Failed");
                            String[] data = 
                                { FSUtils.bundle.getString(
                                            "signatureVerificationFailed") };
                            LogUtil.error(Level.INFO,
                                        LogUtil.SIGNATURE_VERIFICATION_FAILED,
                                        data,
                                        ssoToken);
                            sendSAMLArtifacts(null);
                            return;
                        } else {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message(
                                    "FSSSOBrowserArtifactProfileHandler."
                                    + "processAuthnRequest: "
                                    + "AuthnRequest Signature Verified");
                            }
                        }
                    }
                }
                if (processPreAuthnSSO(authnRequest)){
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "FSSSOBrowserArtifactProfileHandler."
                            + "processAuthnRequest: AuthnRequest Processing "
                            + " successful");
                    }
                } else {
                    if (FSUtils.debug.warningEnabled()) {
                        FSUtils.debug.warning(
                            "FSSSOBrowserArtifactProfileHandler."
                            + "processAuthnRequest: AuthnRequest Processing "
                            + "failed");
                    }
                    String[] data = { 
                        FSUtils.bundle.getString("AuthnRequestProcessingFailed")
                        };
                    LogUtil.error(Level.INFO, 
                        LogUtil.AUTHN_REQUEST_PROCESSING_FAILED,
                        data,
                        ssoToken);
                    sendSAMLArtifacts(null);
                }
            }
        } catch(Exception e){
            FSUtils.debug.error("FSSSOBrowserArtifactProfileHandler."
                + "processAuthnRequest: Exception Occured: ", e);
            sendSAMLArtifacts(null);
        }
    }
    
    /**
     * Processes request with artifacts.
     * @param samlRequest <code>FSSAMLRequest</code> object
     * @return <code>FSResponse</code> object
     */
    @Override
    public FSResponse processSAMLRequest(FSSAMLRequest samlRequest) {
        FSUtils.debug.message(
            "FSSSOBrowserArtifactProfileHandler.processSAMLRequest: Called");
        try {
            return createSAMLResponse(samlRequest);
        } catch(Exception e){
            FSUtils.debug.error("FSSSOBrowserArtifactProfileHandler."
                + "processSAMLRequest: Fatal error, "
                + "cannot create status or response: ", e);
            return null;
        }
    }
    
    private FSResponse createSAMLResponse(FSSAMLRequest samlRequest)
        throws FSException 
    {
        FSUtils.debug.message(
            "FSSSOBrowserArtifactProfileHandler.createSAMLResponse: Called");
        FSResponse retResponse = null;
        String respID= FSUtils.generateID();
        String inResponseTo= samlRequest.getRequestID();
        List contents = new ArrayList();
        String message = null;
        int length;
        Status status;
        
        String remoteAddr = ClientUtils.getClientIPAddress(request);
        String respPrefix = 
            FSUtils.bundle.getString("responseLogMessage") + " " + remoteAddr;
        
        int reqType = samlRequest.getContentType();
        if (reqType == Request.NOT_SUPPORTED) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSSOBrowserArtifactProfileHandler."
                    + "createSAMLResponse: "
                    + "Found element in the request which are not supported");
            }
            message = FSUtils.bundle.getString("unsupportedElement");
            try {
                status = new Status(
                    new StatusCode("samlp:Responder"),message, null);
                retResponse =  new FSResponse(
                    respID, inResponseTo, status, contents);
                retResponse.setMinorVersion(samlRequest.getMinorVersion());
            } catch( SAMLException se ) {
                FSUtils.debug.error("FSSSOBrowserArtifactProfileHandler."
                    + "createSAMLResponse: "
                    + "Fatal error, cannot create status or response: ", se);
            }
            if (LogUtil.isAccessLoggable(Level.FINER)) {
                String[] data = { respPrefix , retResponse.toString() };
                LogUtil.access(Level.FINER,LogUtil.CREATE_SAML_RESPONSE,data);
            } else {
                String[] data = { respPrefix,
                        FSUtils.bundle.getString("responseID") + "=" +
                        retResponse.getResponseID() + "," + 
                        FSUtils.bundle.getString("inResponseTo") + "=" +
                        retResponse.getInResponseTo()};
                LogUtil.access(Level.INFO,LogUtil.CREATE_SAML_RESPONSE,data);
            }
            return retResponse;
        }
        FSAssertionManager am = null;
        try {
            am = FSAssertionManager.getInstance(metaAlias);
        } catch(FSException se ) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSSOBrowserArtifactProfileHandler."
                    + "createSAMLResponse: Cannot instantiate "
                    + "FSAssertionManager");
            }
            message = se.getMessage();
            try {
                status = new Status(
                    new StatusCode("samlp:Responder"), message, null);
                retResponse =  new FSResponse(
                    respID,inResponseTo, status, contents);
                retResponse.setMinorVersion(samlRequest.getMinorVersion());
            } catch( SAMLException sse ) {
                FSUtils.debug.error("FSSSOBrowserArtifactProfileHandler."
                    + "createSAMLResponse: "
                    + "Fatal error, cannot create status or response: ", sse);
            }
            if (LogUtil.isAccessLoggable(Level.FINER)) {
                String[] data = { respPrefix , retResponse.toString() };
                LogUtil.access(Level.FINER,LogUtil.CREATE_SAML_RESPONSE,data);
            } else {
                String[] data = { respPrefix,
                    FSUtils.bundle.getString("responseID") + "=" +
                    retResponse.getResponseID() + "," +
                    FSUtils.bundle.getString("inResponseTo") + "=" +
                    retResponse.getInResponseTo()};
                LogUtil.access(Level.INFO,LogUtil.CREATE_SAML_RESPONSE,data);
            }
            return retResponse;
        }
        List artifacts = null;
        List assertions = new ArrayList();
        if (reqType == Request.ASSERTION_ARTIFACT) {
            artifacts = samlRequest.getAssertionArtifact();
            length = artifacts.size();
            // ensure that all the artifacts have the same sourceID
            String sourceID = null;
            String providerID = null;
            AssertionArtifact art = null;
            for (int j = 0; j < length; j++) {
                art =(AssertionArtifact)artifacts.get(j);
                if (sourceID != null) {
                    if (!sourceID.equals(art.getSourceID())) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSSSOBrowserArtifactProfileHandler."
                                + "createSAMLResponse: Artifacts not from "
                                + "the same source");
                        }
                        message = FSUtils.bundle.getString("mismatchSourceID");
                        try {
                           /**
                            * Need a second level status for the federation
                            * does not exist. 
                            */
                            status = new Status( 
                                new StatusCode("samlp:Requester", 
                                  new StatusCode(
                                    IFSConstants.FEDERATION_NOT_EXISTS_STATUS,
                                    null)),
                                message,
                                null);
                            retResponse =  
                                new FSResponse(respID,
                                               inResponseTo, 
                                               status, 
                                               contents);
                            retResponse.setMinorVersion(
                                samlRequest.getMinorVersion());
                        } catch( SAMLException ex ) {
                            FSUtils.debug.error(
                                "FSSSOBrowserArtifactProfileHandler."
                                + "createSAMLResponse: Fatal error, "
                                + "cannot create status or response: ", ex);
                        }
                        if (LogUtil.isAccessLoggable(Level.FINER)) {
                            String[] data = { respPrefix ,
                                        retResponse.toString() };
                            LogUtil.access(Level.FINER,
                                            LogUtil.CREATE_SAML_RESPONSE,
                                            data);
                        } else {
                            String[] data = { respPrefix,
                                FSUtils.bundle.getString("responseID") + "=" +
                                retResponse.getResponseID() + "," +
                                FSUtils.bundle.getString("inResponseTo") + "=" +
                                retResponse.getInResponseTo()};
                            LogUtil.access(
                                Level.INFO, LogUtil.CREATE_SAML_RESPONSE,data);
                        }
                        return retResponse;
                    } else { //sourceids are equal
                        continue;
                    }
                } else {// sourceID == null
                    sourceID = art.getSourceID();
                }
            } // while loop to go through artifacts to check for sourceID
            if (art != null){
                try {
                    providerID = am.getDestIdForArtifact(art);
                } catch(FSException ex){
                    FSUtils.debug.error("FSSSOBrowserArtifactProfileHandler."
                        + "createSAMLResponse: FSException Occured while "
                        + "retrieving sp's providerID for the artifact: ", ex);
                    providerID = null;
                }
                if (providerID == null){
                    FSUtils.debug.error("FSSSOBrowserArtifactProfileHandler."
                        + "createSAMLResponse: "
                        + "artifact received does not correspond to any SP");
                    message = FSUtils.bundle.getString("invalidSource");
                    try {
                        /**
                         * Need a second level status for the federation
                         * does not exist. 
                         */
                        /**
                         * First, let's check we haven't recorded a status
                         * beforehand (by another call) related to this
                         * artifact. If so, use it.
                         */
                        Status sorig = am.getErrorStatus( art );
                        if ( sorig != null ) {
                            status = sorig;
                        } else {
                            status = new Status( 
                                new StatusCode("samlp:Requester", 
                                  new StatusCode(
                                    IFSConstants.FEDERATION_NOT_EXISTS_STATUS,
                                    null)),
                                message,
                                null);
                        }
                        retResponse = new FSResponse(
                            respID,inResponseTo, status, contents);
                        retResponse.setMinorVersion(
                            samlRequest.getMinorVersion());
                        return retResponse;
                    } catch( SAMLException sse ) {
                        FSUtils.debug.error(
                            "FSSSOBrowserArtifactProfileHandler."
                            + "createSAMLResponse:Fatal error, "
                            + "cannot create status or response: ", sse);
                        return null;
                    }
                    //return error response
                } else {
                    try {
                        if (!metaManager.isTrustedProvider(
                            realm, hostedEntityId,providerID)) 
                        { 
                            FSUtils.debug.error(
                                "FSSSOAndFedHandler.processAuthnRequest: "
                                + "RemoteProvider is not trusted");
                            message = FSUtils.bundle.getString(
                                "AuthnRequestProcessingFailed");
                            status = new Status(
                                new StatusCode("samlp:Requester"),
                                message,
                                null);
                            retResponse = new FSResponse(
                                respID, inResponseTo, status, contents);
                            retResponse.setMinorVersion(
                                samlRequest.getMinorVersion());
                            return retResponse;
                        }
                        spDescriptor = metaManager.getSPDescriptor(
                            realm, providerID);
                        spEntityId = providerID;
                        remoteAddr = providerID;
                    } catch(Exception ae){
                        FSUtils.debug.error(
                            "FSSSOBrowserArtifactProfileHandler."
                            + "createSAMLResponse: "
                            + "FSAllianceManagementException "
                            + "Occured while getting" , ae);
                        message = ae.getMessage();
                        try {
                            status = new Status(
                                new StatusCode("samlp:Requester"),
                                message,
                                null);
                            retResponse = new FSResponse(
                                respID,inResponseTo, status, contents);
                            retResponse.setMinorVersion(
                                samlRequest.getMinorVersion());
                            return retResponse;
                        } catch( SAMLException sse ) {
                            FSUtils.debug.error(
                                "FSSSOBrowserArtifactProfileHandler."
                                + "createSAMLResponse:Fatal error, "
                                + "cannot create status or response: ", sse);
                            return null;
                        }
                    }
                }
                //Verify signature
                if (FSServiceUtils.isSigningOn()){
                    if (!verifySAMLRequestSignature(
                        samlRequestElement, soapMsg))
                    {
                        FSUtils.debug.error(
                            "FSSSOBrowserArtifactProfileHandler."
                            + "createSAMLResponse: "
                            + "SAMLRequest signature verification failed");
                        message = FSUtils.bundle.getString(
                            "signatureVerificationFailed");
                        try {
                            status = new Status(
                                new StatusCode("samlp:Requester"),
                                message,
                                null);
                            retResponse = new FSResponse(
                                respID, inResponseTo, status, contents);
                            retResponse.setMinorVersion(
                                samlRequest.getMinorVersion());
                            return retResponse;
                        } catch( SAMLException sse ) {
                            FSUtils.debug.error(
                                "FSSSOBrowserArtifactProfileHandler."
                                + "createSAMLResponse:Fatal error, "
                                + "cannot create status or response: "
                                + sse.getMessage());
                        }
                    } else {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSSSOBrowserArtProfileHandler.createSAMLResp:"
                                + " SAMLRequest signature verified");
                        }
                    }
                }
                //end signature verification
            } else {
                FSUtils.debug.error("FSSSOBrowserArtifactProfileHandler."
                    + "createSAMLResponse: No artifact found in samlRequest");
                message = FSUtils.bundle.getString("missingArtifact");
                try {
                    status = new Status(
                        new StatusCode("samlp:Requester"), message, null);
                    retResponse = new FSResponse(
                        respID,inResponseTo, status, contents);
                    retResponse.setMinorVersion(samlRequest.getMinorVersion());
                    return retResponse;
                } catch( SAMLException sse ) {
                    FSUtils.debug.error("FSSSOBrowserArtifactProfileHandler."
                        + "createSAMLResponse:Fatal error, "
                        + "cannot create status or response: ", sse);
                    return null;
                }
            }
            
            for (int i = 0; i < length; i++) {
                AssertionArtifact artifact =(AssertionArtifact)
                    artifacts.get(i);
                Assertion assertion = null;
                try {
                    assertion = am.getAssertion(artifact, spEntityId);
                } catch(FSException e ) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "FSSSOBrowserArtifactProfileHandler.createSAML"
                            + "Response:could not find matching assertion:", e);
                    }
                    message = e.getMessage();
                    try {
                        status = new Status(
                            new StatusCode("samlp:Success"), message, null);
                        retResponse = new FSResponse(
                            respID,inResponseTo, status, contents);
                        retResponse.setMinorVersion(
                            samlRequest.getMinorVersion());
                    } catch( SAMLException sse ) {
                        FSUtils.debug.error(
                            "FSSSOBrowserArtifactProfileHandler."
                            + "createSAMLResponse:Fatal error, "
                            + "cannot create status or response: ", sse);
                    }
                    if (LogUtil.isAccessLoggable(Level.FINER)) {
                        String[] data = { respPrefix , retResponse.toString() };
                        LogUtil.access(
                            Level.FINER,LogUtil.CREATE_SAML_RESPONSE,data);
                    } else {
                        String[] data = { respPrefix,
                                FSUtils.bundle.getString("responseID") + "=" +
                                retResponse.getResponseID() + "," +
                                FSUtils.bundle.getString("inResponseTo") + "=" +
                                retResponse.getInResponseTo()};
                        LogUtil.access(
                            Level.INFO, LogUtil.CREATE_SAML_RESPONSE,data);
                    }

                    return retResponse;
                }
                if (assertion != null) {   
                    assertions.add(i,assertion);
                }
            }
        }
        int assertionSize = assertions.size();
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSSSOBrowserArtifactProfileHandler."
                + "createSAMLResponse: found " + assertionSize + "assertions.");
        }
        // check that the target restriction condition
        // inside the assertion has the calling host's address in it.
        for (int i = 0; i < assertionSize; i++) {
            Assertion assn = (Assertion)assertions.get(i);
            Conditions conds = assn.getConditions();
            Set trcs =  conds.getAudienceRestrictionCondition();
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSSOBrowserArtifactProfileHandler."
                    + "createSAMLResponse: checking to see if assertions"
                    + " are for host:" + remoteAddr);
                
            }
            if (trcs != null && !trcs.isEmpty()) {
                Iterator trcsIterator = trcs.iterator();
                while (trcsIterator.hasNext()) {
                    if (!((AudienceRestrictionCondition)trcsIterator.next())
                                .containsAudience(remoteAddr))
                    {                        
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSSSOBrowserArtifactProfileHandler."
                                + "createSAMLResponse: removing TRC not"
                                + "meant for this host");
                        }
                        assertions.remove(assn);
                    }
                }
            }
        }
        assertionSize = assertions.size(); 
        if (assertionSize == 0) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSSOBrowserArtifactProfileHandler."
                    + "createSAMLResponse: Matching Assertions(s) not " 
                    + "created for this host");
            }
            message = FSUtils.bundle.getString("mismatchDest");
            try {
                status = 
                    new Status(new StatusCode("samlp:Success"), message, null);
                retResponse = new FSResponse(
                    respID, inResponseTo, status, contents);
                retResponse.setMinorVersion(samlRequest.getMinorVersion());
            } catch( SAMLException se ) {
                FSUtils.debug.error("FSSSOBrowserArtifactProfileHandler."
                    + "createSAMLResponse: Fatal error, "
                    + "cannot create status or response:", se);
            }
            if (LogUtil.isAccessLoggable(Level.FINER)) {
                String[] data = { respPrefix , retResponse.toString() };
                LogUtil.access(Level.FINER,LogUtil.CREATE_SAML_RESPONSE,data);
            } else {
                String[] data = { respPrefix,
                        FSUtils.bundle.getString("responseID") + "=" +
                        retResponse.getResponseID() + "," +
                        FSUtils.bundle.getString("inResponseTo") + "=" +
                        retResponse.getInResponseTo()};
                LogUtil.access(
                    Level.INFO, LogUtil.CREATE_SAML_RESPONSE,data);
            }
            return retResponse;
        }

        if (reqType == Request.ASSERTION_ARTIFACT) {
            if (assertions.size()  == artifacts.size()) {
                message = null;
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSSSOBrowserArtifactProfileHandler."
                        + "createSAMLResponse: Matching Assertion found");
                }
                try {
                    status = new Status(
                        new StatusCode("samlp:Success"), message, null);
                    retResponse = new FSResponse(
                        respID, inResponseTo, status, assertions);
                    retResponse.setMinorVersion(samlRequest.getMinorVersion());
                } catch( SAMLException se ) {
                    FSUtils.debug.error("FSSSOBrowserArtifactProfileHandler."
                        + "createSAMLResponse: Fatal error, "
                        + "cannot create status or response:", se);
                    return null;
                } catch(Exception e ) {
                    FSUtils.debug.error("FSSSOBrowserArtifactProfileHandler."
                        + "createSAMLResponse: Fatal error, "
                        + "cannot create status or response:", e);
                    return null;
                }
                if (LogUtil.isAccessLoggable(Level.FINER)) {
                    String[] data = { respPrefix , retResponse.toString() };
                    LogUtil.access(
                        Level.FINER,LogUtil.CREATE_SAML_RESPONSE, data);
                } else {
                    String[] data = { respPrefix,
                        FSUtils.bundle.getString("responseID") + "=" +
                        retResponse.getResponseID() + "," +
                        FSUtils.bundle.getString("inResponseTo") + "=" +
                        retResponse.getInResponseTo()};
                    LogUtil.access(
                        Level.INFO, LogUtil.CREATE_SAML_RESPONSE,data);
                }
                return retResponse;
            } else {
                message = FSUtils.bundle.getString("unequalMatch");
                try {
                    status = new Status(
                            new StatusCode("samlp:Success"), message, null);
                    retResponse = new FSResponse(
                        respID, inResponseTo, status, assertions);
                    retResponse.setMinorVersion(samlRequest.getMinorVersion());
                } catch( SAMLException se ) {
                    FSUtils.debug.error("FSSSOBrowserArtifactProfileHandler."
                        + "createSAMLResponse: Fatal error, "
                        + "cannot create status or response:", se);
                }
                if (LogUtil.isAccessLoggable(Level.FINER)) {
                    String[] data = { respPrefix , retResponse.toString() };
                    LogUtil.access(
                        Level.FINER,LogUtil.CREATE_SAML_RESPONSE,data);
                } else {
                    String[] data = { respPrefix,
                        FSUtils.bundle.getString("responseID") + "=" +
                        retResponse.getResponseID() + "," +
                        FSUtils.bundle.getString("inResponseTo") + "=" +
                        retResponse.getInResponseTo()};
                    LogUtil.access(
                        Level.INFO, LogUtil.CREATE_SAML_RESPONSE,data);
                }
                return retResponse;
            }
        } else { // build response for all the other type of request
            try {
                message = null;
                status = new Status(
                        new StatusCode("samlp:Success"), message, null);
                retResponse = new FSResponse(
                    respID, inResponseTo, status, assertions);
                retResponse.setMinorVersion(samlRequest.getMinorVersion());
            } catch( SAMLException se ) {
                FSUtils.debug.error("FSSSOBrowserArtifactProfileHandler."
                    + "createSAMLResponse: Fatal error, "
                    + "cannot create status or response:", se);
            }
        }
        if (LogUtil.isAccessLoggable(Level.FINER)) {
            String[] data = { respPrefix , retResponse.toString() };
            LogUtil.access(Level.FINER,LogUtil.CREATE_SAML_RESPONSE, data);
        } else {
            String[] data = { respPrefix,
                        FSUtils.bundle.getString("responseID") + "=" +
                        retResponse.getResponseID() + "," +
                        FSUtils.bundle.getString("inResponseTo") + "=" +
                        retResponse.getInResponseTo()};
            LogUtil.access(Level.INFO, LogUtil.CREATE_SAML_RESPONSE,data);
        }
        return retResponse;
    }
    
    
    
    /**
     * Generates artifact and sends it to <code>SP</code>.
     * @return <code>true</code> always.
     */
    @Override
    protected boolean doSingleSignOn(
        Object ssoToken,
        String inResponseTo,
        NameIdentifier opaqueHandle,
        NameIdentifier idpOpaqueHandle
    )
    {
        FSUtils.debug.message(
            "FSSSOBrowserArtifactProfileHandler.doSingleSignOn: Called");
        this.ssoToken = ssoToken;
        List artList = createSAMLAssertionArtifact(ssoToken, 
                                                  inResponseTo, 
                                                  opaqueHandle,
                                                  idpOpaqueHandle);
        sendSAMLArtifacts(artList);
        return true;
    }
    
    /**
     * Creates assertion and assertion artifact.
     */
    protected List createSAMLAssertionArtifact(
        Object ssoToken,
        String inResponseTo,
        NameIdentifier userHandle,
        NameIdentifier idpHandle
    )
    {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSSSOBrowserArtifactProfileHandler."
                + "createSAMLAssertionArtifact: Called");
        }
        List artifactList = new ArrayList();
        try {
            FSAssertionManager am = 
                FSAssertionManager.getInstance(metaAlias);
            AssertionArtifact artifact = am.createFSAssertionArtifact(
                SessionManager.getProvider().getSessionID(ssoToken),
                realm,
                spEntityId,
                userHandle,
                idpHandle,
                inResponseTo,
                authnRequest.getMinorVersion());
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("AssertionArtifact id = " +
                    artifact.toString());
            }
            String artid = artifact.getAssertionArtifact();
            artifactList.add(artid);
            return artifactList;
        } catch(FSException se) {
            FSUtils.debug.error("FSSSOBrowserArtifactProfileHandler."
                + "createSAMLAssertionArtifact(0): ", se);
            return null;
        } catch(SAMLException se) {
            FSUtils.debug.error("FSSSOBrowserArtifactProfileHandler."
                + "createSAMLAssertionArtifact(1): ", se);
            return null;
        } catch (SessionException se) {
            FSUtils.debug.error("FSSSOBrowserArtifactProfileHandler."
                + "createSAMLAssertionArtifact(2): ", se);
            return null;
        }
    }
    
    private void sendSAMLArtifacts(List artis) {
        FSUtils.debug.message(
            "FSSSOBrowserArtifactProfileHandler.sendSAMLArtifacts: Called");
        if (artis == null) {
            artis = createFaultSAMLArtifact();
        }
        try {
            String targetURL = FSServiceUtils.getAssertionConsumerServiceURL(
                spDescriptor, authnRequest.getAssertionConsumerServiceID());
            StringBuilder sb = new StringBuilder(1000);
            if (artis == null || artis.isEmpty()){
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSSSOBrowserArtifactProfileHandler."
                        + "sendSAMLArtifacts: Sending null artifact");
                }
                sb.append(IFSConstants.ARTIFACT_NAME_DEFAULT)
                    .append("=")
                    .append("&");
            } else {
                Iterator iter = artis.iterator();
                while(iter.hasNext()) {
                    String art = URLEncDec.encode((String)iter.next());
                    if(FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "FSSSOBrowserArtifactProfileHandler."
                            + "sendSAMLArtifacts: " + art);
                    }
                    sb.append(IFSConstants.ARTIFACT_NAME_DEFAULT)
                      .append("=")
                      .append(art)
                      .append("&");
                }
            }
            StringBuilder tmp = new StringBuilder(1000);
            if (targetURL.indexOf('?') == -1){
                tmp.append(targetURL).append("?");
            } else {
                tmp.append(targetURL).append("&");
            }
            tmp.append(sb.toString());
            String relayURL = authnRequest.getRelayState();
            if (relayURL != null){
                tmp.append(IFSConstants.LRURL)
                    .append("=")
                    .append(URLEncDec.encode(relayURL));
            }
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            String redirecto = tmp.toString();
            response.setContentType("text/html");
            response.setHeader("Location", redirecto);
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSSOBrowserArtifactProfileHandler."
                    + "sendSAMLArtifacts: Sending artifacts to: " + redirecto);
            }
            String[] data =  { redirecto };
            LogUtil.access(Level.FINER,LogUtil.REDIRECT_TO, data, ssoToken);
            response.sendRedirect(redirecto);
        } catch(Exception ex){
            FSUtils.debug.error("FSSSOBrowserArtifactProfileHandler."
                + "sendSAMLArtifacts: ", ex);
        }
    }

   /**
    * Generates a valid SAML artifact, in response
    * to a single sign on request for a non federated user.
    */ 
    private List createFaultSAMLArtifact() {

       FSUtils.debug.message(
           "FSSSOBrowserArtifactProfileHandler. In createFaultSAMLArtifacts");
       // create assertion id and artifact
       String handle = SAMLUtils.generateAssertionHandle();
       if (handle == null) {
           if (FSUtils.debug.messageEnabled()) {
               FSUtils.debug.message("FSSSOBrowserArtifactProfileHandler." +
                   "create FaultSAMLArtifacts: couldn't generate assertion " +
                   "handle.");
           }
           return null;
       }
       try {
            String sourceSuccinctID = FSUtils.generateSourceID(hostedEntityId);

            AssertionArtifact art = new FSAssertionArtifact(
                SAMLUtils.stringToByteArray(sourceSuccinctID),
                handle.getBytes(IFSConstants.SOURCEID_ENCODING));
            List artis = new ArrayList();
            artis.add(art.getAssertionArtifact());
            FSAssertionManager am = 
                FSAssertionManager.getInstance( metaAlias );
            am.setErrStatus( art, noFedStatus );
            return artis;
        } catch(Exception e) {
            FSUtils.debug.error(
              "FSBrowserArtifactProfileHandler.createFaultSAMLArtifacts: ", e);
            return null;
        }
    }

    
    protected boolean verifySAMLRequestSignature(
        Element samlRequestElement,
        SOAPMessage msg
    )
    {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSSSOBrowserArtifactProfileHandler."
                + "verifySAMLRequestSignature: Called");
        }
        try {
            X509Certificate cert = KeyUtil.getVerificationCert(
                spDescriptor, spEntityId, false);
            
            if (cert == null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSSSOBrowserArtifactProfileHandler."
                        + "verifySAMLRequestSignature: couldn't obtain "
                        + "this site's cert.");
                }
                throw new SAMLResponderException(
                    FSUtils.bundle.getString(IFSConstants.NO_CERT));
            }
            XMLSignatureManager manager = XMLSignatureManager.getInstance();
            Document doc = (Document)FSServiceUtils.createSOAPDOM(msg);
            return manager.verifyXMLSignature(doc, cert);
        } catch(Exception e){
            FSUtils.debug.error("FSSSOBrowserArtifactProfileHandler."
                + "verifySAMLRequestSignature: Exception occured while "
                + "verifying IDP's signature:" , e);
            return false;
        }
    }
}
