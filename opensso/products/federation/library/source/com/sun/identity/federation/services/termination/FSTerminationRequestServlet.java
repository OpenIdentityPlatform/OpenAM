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
 * $Id: FSTerminationRequestServlet.java,v 1.4 2008/06/25 05:47:04 qcheng Exp $
 *
 */


package com.sun.identity.federation.services.termination;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.LogUtil;
import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.key.KeyUtil;
import com.sun.identity.federation.message.FSFederationTerminationNotification;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.services.util.FSSignatureUtil;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.federation.services.FSServiceManager;
import com.sun.identity.liberty.ws.meta.jaxb.ProviderDescriptorType;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLResponderException;

public class FSTerminationRequestServlet extends HttpServlet {    
    private static IDFFMetaManager metaManager = null;    
    private String commonErrorPage = null;    
    private HttpServletRequest request = null;
    /**
     * Initializes the servlet.
     * @param config the <code>ServletConfig</code> object that contains 
     *  configutation information for this servlet.
     * @exception ServletException if an exception occurs that interrupts
     *  the servlet's normal operation.
     */
    public void init(ServletConfig config) 
        throws ServletException
    {
        super.init(config);        
        FSUtils.debug.message("Entered FSTerminationRequestServlet Init");
        metaManager = FSUtils.getIDFFMetaManager();      
    }
    
    /**
     * Invoked to set some commonly used termination URLs based on hosted
     * provider.
     * @param hostedConfig hosted provider's extended meta
     * @param metaAlias hosted provider's meta alias
     */
    protected void setTerminationURL(
        BaseConfigType hostedConfig, String metaAlias)
    {
        commonErrorPage = FSServiceUtils.getErrorPageURL(
            request, hostedConfig, metaAlias);
    }

    /**
     * Handles the HTTP GET request.
     * @param request <code>HttpServletRequest</code> object that contains the
     *  request the client has made of the servlet.
     * @param response <code>HttpServletResponse</code> object that contains 
     *  the response the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     *  the servlet handles the GET request
     * @exception IOException if the request for the GET could not be handled
     */
    public void doGet(
        HttpServletRequest  request,
        HttpServletResponse response)
        throws ServletException, IOException 
    {
        doGetPost(request, response);
    }
    
    /**
     * Handles the HTTP POST request.
     * @param request <code>HttpServletRequest</code> object that contains the
     *  request the client has made of the servlet.
     * @param response <code>HttpServletResponse</code> object that contains 
     *  the response the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     * the servlet handles the POST request
     * @exception IOException if the request for the POST could not be handled
     */
    public void doPost(
        HttpServletRequest  request,
        HttpServletResponse response)
        throws ServletException, IOException 
    {
        doGetPost(request, response);
    }
    
    /**
     * Handles termination request.
     * @param request <code>HttpServletRequest</code> object that contains the
     *  request the client has made of the servlet.
     * @param response <code>HttpServletResponse</code> object that contains
     *  the response the servlet sends to the client.
     * @exception IOException if the request could not be handled
     */
    private void doGetPost(
        HttpServletRequest  request,
        HttpServletResponse response)
        throws ServletException, IOException 
    {        
        // Alias processing
        String providerAlias = FSServiceUtils.getMetaAlias(request);
        if (providerAlias == null || providerAlias.length() < 1) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Unable to retrieve alias, Hosted "
                    + "Provider. Cannot process request");
            }
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                FSUtils.bundle.getString("aliasNotFound"));
            return;
        }        
        if (metaManager == null) {        
            FSUtils.debug.error(
                "Cannot retrieve hosted descriptor. Cannot process request");
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                FSUtils.bundle.getString(
                    IFSConstants.FAILED_HOSTED_DESCRIPTOR));
            return;
        }
        ProviderDescriptorType hostedProviderDesc = null;
        BaseConfigType hostedConfig = null;
        String realm = IDFFMetaUtils.getRealmByMetaAlias(providerAlias);
        String hostedEntityId = null;
        String hostedProviderRole = null;
        try {
            hostedProviderRole = metaManager.getProviderRoleByMetaAlias(
                providerAlias);
            hostedEntityId = metaManager.getEntityIDByMetaAlias(providerAlias);
            if (hostedProviderRole != null &&
                hostedProviderRole.equalsIgnoreCase(IFSConstants.IDP))
            {
                hostedProviderDesc =
                    metaManager.getIDPDescriptor(realm, hostedEntityId);
                hostedConfig =
                    metaManager.getIDPDescriptorConfig(realm, hostedEntityId);
            } else if (hostedProviderRole != null &&
                hostedProviderRole.equalsIgnoreCase(IFSConstants.SP))
            {
                hostedProviderDesc =
                    metaManager.getSPDescriptor(realm, hostedEntityId);
                hostedConfig =
                    metaManager.getSPDescriptorConfig(realm, hostedEntityId);
            }
            if (hostedProviderDesc == null) {
                throw new IDFFMetaException((String) null);
            }
        }catch (IDFFMetaException eam) {
            FSUtils.debug.error(
                "Unable to find Hosted Provider. not process request");
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                FSUtils.bundle.getString(
                    IFSConstants.FAILED_HOSTED_DESCRIPTOR));
            return;
        }
        this.request = request;
        setTerminationURL(hostedConfig, providerAlias);
        FSFederationTerminationNotification fedTermObj = 
            new FSFederationTerminationNotification();
        try {
            fedTermObj = FSFederationTerminationNotification.
                parseURLEncodedRequest(request);
        } catch (FSMsgException e) {            
            FSServiceUtils.showErrorPage(
                response,
                commonErrorPage,
                IFSConstants.TERMINATION_REQUEST_IMPROPER,
                IFSConstants.TERMINATION_LOCAL_FAILED);
            return;                        
        } catch (SAMLException e) {            
            FSServiceUtils.showErrorPage(
                response,
                commonErrorPage,
                IFSConstants.TERMINATION_REQUEST_IMPROPER,
                IFSConstants.TERMINATION_LOCAL_FAILED);
            return;                        
        }        
        if (fedTermObj == null) {
            FSServiceUtils.showErrorPage(
                response,
                commonErrorPage,
                IFSConstants.TERMINATION_REQUEST_IMPROPER,
                IFSConstants.TERMINATION_LOCAL_FAILED);
        } else {
            doRequestProcessing(request, response, hostedProviderDesc, 
                hostedConfig, hostedProviderRole, realm, hostedEntityId,
                providerAlias,fedTermObj);
        }
        return;
    }   
    

    
    /**
     * Called when a Termination request is received from a remote provider.
     * @param request <code>HTTPServletRequest</code> object received via HTTP
     *  Redirect
     * @param response <code>HTTPServletResponse</code> object to send the 
     *  response back to user agent
     * @param hostedProviderDesc the provider for whom request is received
     * @param hostedConfig hosted provider's extended meta
     * @param hostedProviderRole hosted provider's role
     * @param realm The realm under which the entity resides
     * @param hostedEntityId hosted provider's entity ID
     * @param providerAlias hosted provider's meta alias
     * @param reqTermination the federation termination request
     */
    private void doRequestProcessing (
        HttpServletRequest request, 
        HttpServletResponse response,
        ProviderDescriptorType hostedProviderDesc,
        BaseConfigType hostedConfig,
        String hostedProviderRole,
        String realm,
        String hostedEntityId,
        String providerAlias,
        FSFederationTerminationNotification reqTermination)
    {
        FSUtils.debug.message(
            "Entered FSTerminationRequestServlet::doRequestProcessing");

        String remoteEntityId = reqTermination.getProviderId();

        String retURL = null;
        ProviderDescriptorType remoteDesc = null;
        boolean isIDP = false;
        try {
            if (hostedProviderRole.equalsIgnoreCase(IFSConstants.SP)) {
                remoteDesc = metaManager.getIDPDescriptor(
                    realm, remoteEntityId);
                isIDP = true;
            } else {
                remoteDesc = metaManager.getSPDescriptor(realm, remoteEntityId);
            }
            if (remoteDesc == null) {
                throw new IDFFMetaException((String) null);
            }
            retURL = remoteDesc.getFederationTerminationServiceReturnURL();
        } catch (IDFFMetaException e) {
            FSUtils.debug.error("FSTerminationRequestServlet.doRequest " +
                "Processing: Can not retrieve remote provider data."
                + remoteEntityId);
            String[] data = { remoteEntityId, realm };
            LogUtil.error(Level.INFO,LogUtil.INVALID_PROVIDER,data);
            FSServiceUtils.returnToSource(
                response, 
                retURL,
                commonErrorPage,
                IFSConstants.TERMINATION_INVALID_FEDERATION,
                IFSConstants.METADATA_ERROR);
            return;
        }

        boolean bVerify = true;
        if (FSServiceUtils.isSigningOn()) {                        
            try {
                if (remoteDesc != null) {
                    FSUtils.debug.message("Calling verifyTerminationSignature");
                    bVerify = verifyTerminationSignature(
                        request, remoteDesc, remoteEntityId, isIDP);
                } else{
                    FSUtils.debug.error("Remote provider metadata not found.");
                    String[] data = { remoteEntityId, realm };
                    LogUtil.error(Level.INFO,LogUtil.INVALID_PROVIDER,data);
                    FSServiceUtils.returnToSource(
                        response, 
                        retURL,
                        commonErrorPage,
                        IFSConstants.TERMINATION_INVALID_FEDERATION,
                        IFSConstants.METADATA_ERROR);
                    return;        
                }
            } catch(FSException e) {
                FSUtils.debug.error(
                    "FSTerminationRequestServlet::doRequestProcessing " + 
                    "Signature on termination request is invalid" +
                    "Cannot proceed federation termination");
                String[] data = { FSUtils.bundle.getString(
                    IFSConstants.TERMINATION_INVALID_SIGNATURE) };
                LogUtil.error(Level.INFO,LogUtil.INVALID_SIGNATURE,data);
                FSServiceUtils.returnToSource(
                    response, 
                    retURL,
                    commonErrorPage,
                    IFSConstants.TERMINATION_INVALID_FEDERATION,
                    IFSConstants.METADATA_ERROR);
                return;
            } catch(SAMLException e) {
                FSUtils.debug.error(
                    "FSFedTerminationHandler::doRequestProcessing " + 
                    "Signature on termination request is invalid" +
                    "Cannot proceed federation termination");
                String[] data = { FSUtils.bundle.getString(
                    IFSConstants.TERMINATION_INVALID_SIGNATURE) };
                LogUtil.error(Level.INFO,LogUtil.INVALID_SIGNATURE,data);
                FSServiceUtils.returnToSource(
                    response, 
                    retURL,
                    commonErrorPage,
                    IFSConstants.TERMINATION_INVALID_FEDERATION,
                    IFSConstants.METADATA_ERROR);
                return;
            }
        }                        
        if (bVerify) {       
            // Check if trusted provider
            if (metaManager.isTrustedProvider(
                realm, hostedEntityId, remoteEntityId)) 
            {
                FSServiceManager instService =
                    FSServiceManager.getInstance();            
                if (instService != null){
                    FSFedTerminationHandler termHandler = 
                        instService.getFedTerminationHandler(
                            reqTermination, hostedConfig, realm, hostedEntityId,
                            hostedProviderRole, providerAlias,
                            remoteEntityId);
                    if (termHandler != null) {
                        termHandler.setHostedDescriptor(hostedProviderDesc);
                        termHandler.setHostedDescriptorConfig(hostedConfig);
                        termHandler.setRealm(realm);
                        termHandler.setHostedEntityId(hostedEntityId);
                        termHandler.setHostedProviderRole(hostedProviderRole);
                        termHandler.setMetaAlias(providerAlias);
                        termHandler.setRemoteEntityId(remoteEntityId);
                        termHandler.setRemoteDescriptor(remoteDesc);
                        termHandler.processTerminationRequest(request, 
                            response, reqTermination);
                        return; 
                    } else {
                        FSUtils.debug.error("Unable to get termination " +
                            "handler. User account Not valid");
                    } 
                } else {
                    FSUtils.debug.error("FSServiceManager instance is null"
                        + " Cannot process termination request");
                }                                
            } else {
                FSUtils.debug.error("Remote provider not in trusted list");
            }
        } else {
            FSUtils.debug.error(
                "FSTerminationRequestServlet::doRequestProcesing " + 
                "Signature on termination request is invalid" +
                "Cannot proceed federation termination");
            String[] data = { FSUtils.bundle.getString(
                IFSConstants.TERMINATION_INVALID_SIGNATURE) };
            LogUtil.error(Level.INFO,LogUtil.INVALID_SIGNATURE,data);
        }               
        FSServiceUtils.returnToSource(
            response, 
            retURL,
            commonErrorPage,
            IFSConstants.TERMINATION_INVALID_FEDERATION,
            IFSConstants.METADATA_ERROR);
    }   
    
    /** 
     * Verifies signature on Termination request received from the remote end.
     * @param request <code>HttpServletRequest</code> containing the signed
     *  termination request
     * @param remoteDescriptor provider who sent the termination request
     * @param remoteEntityId remote provider's entity ID
     * @param isIDP whether the remote provider is an IDP or not
     * @return <code>true</code> if signatre is valid; <code>false</code>
     *  otherwise.
     * @exception SAMLException, FSException if an error occurred during the
     *  process
     */
    private boolean verifyTerminationSignature(
        HttpServletRequest request,
        ProviderDescriptorType remoteDescriptor,
        String remoteEntityId,
        boolean isIDP)
        throws SAMLException, FSException 
    {
        FSUtils.debug.message(
            "Entered FSTerminationRequestServlet::verifyTerminationSignature");
        // Verify the signature on the request
        X509Certificate cert = KeyUtil.getVerificationCert(
            remoteDescriptor, remoteEntityId, isIDP);
        if (cert == null) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSFedTerminationRequestServlet.verifyTerminationSignature:"
                    + "couldn't obtain this site's cert .");
            }
            throw new SAMLResponderException(
                FSUtils.bundle.getString(IFSConstants.NO_CERT));
        }
        boolean isValidSign =
            FSSignatureUtil.verifyRequestSignature(request, cert);
        if (!isValidSign) {
            FSUtils.debug.error(
                "Termination request is not properly signed");
            return false;
        } else {
            FSUtils.debug.message("Termination request is properly signed");
            return true;
        }
    }
}   // FSTerminationRequestServlet
