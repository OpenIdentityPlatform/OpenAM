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
 * $Id: FSProcessLogoutServlet.java,v 1.7 2008/12/19 06:50:47 exu Exp $
 *
 */


package com.sun.identity.federation.services.logout;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.logging.Level;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.LogUtil;
import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.key.KeyUtil;
import com.sun.identity.federation.message.FSLogoutNotification;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.plugins.FederationSPAdapter;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.federation.services.util.FSSignatureUtil;
import com.sun.identity.federation.services.FSServiceManager;
import com.sun.identity.liberty.ws.meta.jaxb.ProviderDescriptorType;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.common.SAMLResponderException;
import com.sun.identity.saml.common.SAMLException;

/**
 * Handles <code>ID-FF</code> Single Logout request.
 */
public class FSProcessLogoutServlet extends HttpServlet {
    private static IDFFMetaManager metaManager = null;

    /**
     * Initializes the servlet.
     * @param config the <code>ServletConfig</code> object that contains
     *  configutation information for this servlet.
     * @exception ServletException if an exception occurs that interrupts
     *               the servlet's normal operation.
     */
    public void init(ServletConfig config)
        throws ServletException
    {
        super.init(config);
        FSUtils.debug.message("FSProcessLogoutServlet Initializing...");
        metaManager = FSUtils.getIDFFMetaManager();
    }
    
    /**
     * Handles the HTTP GET request.
     *
     * @param request an <code>HttpServletRequest</code> object that contains
     *  the request the client has made of the servlet.
     * @param response an <code>HttpServletResponse</code> object that contains
     *  the response the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     *                             the servlet handles the GET request
     * @exception IOException if the request for the GET could not be handled
     */
    public void doGet(HttpServletRequest  request,
                    HttpServletResponse response)
        throws ServletException, IOException
    {
        doGetPost(request, response);
    }
    
    /**
     * Handles the HTTP POST request.
     *
     * @param request an <code>HttpServletRequest</code> object that contains
     *  the request the client has made of the servlet.
     * @param response an <code>HttpServletResponse</code> object that contains
     *  the response the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     *                             the servlet handles the POST request
     * @exception IOException if the request for the POST could not be handled
     */
    public void doPost(HttpServletRequest  request,
                    HttpServletResponse response)
        throws ServletException, IOException
    {
        doGetPost(request, response);
    }
    
    /**
     * Handles single logout request.
     * @param request an <code>HttpServletRequest</code> object that contains
     *  the request the client has made of the servlet.
     * @param response an <code>HttpServletResponse</code> object that contains
     *  the response the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     *                             the servlet handles the request
     * @exception IOException if the request could not be handled
     */
    private void doGetPost(HttpServletRequest request,
                        HttpServletResponse response)
        throws ServletException, IOException
    {
        FSUtils.debug.message("FSProcessLogoutServlet doGetPost...");
        // Alias processing
        String providerAlias = request.getParameter(IFSConstants.META_ALIAS);
        if (providerAlias == null || providerAlias.length() == 0) {
            providerAlias = FSServiceUtils.getMetaAlias(request);
        }
        if (providerAlias == null || providerAlias.length() < 1) {
            FSUtils.debug.error("Unable to retrieve alias, Hosted Provider. "
                + "Cannot process request");
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                FSUtils.bundle.getString("aliasNotFound"));
            return;
        }
        if (metaManager == null) {
            FSUtils.debug.error("Cannot retrieve hosted descriptor. " +
                "Cannot process request");
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                FSUtils.bundle.getString(
                    IFSConstants.FAILED_HOSTED_DESCRIPTOR));
            return;
        }
        String realm = IDFFMetaUtils.getRealmByMetaAlias(providerAlias);
        ProviderDescriptorType hostedProviderDesc = null;
        BaseConfigType hostedConfig = null;
        String hostedRole = null;
        String hostedEntityId = null;
        try {
            hostedRole = metaManager.getProviderRoleByMetaAlias(providerAlias);
            hostedEntityId = metaManager.getEntityIDByMetaAlias(providerAlias);
            if (hostedRole != null) {
                if (hostedRole.equalsIgnoreCase(IFSConstants.IDP)) {
                    hostedProviderDesc = metaManager.getIDPDescriptor(
                        realm, hostedEntityId);
                    hostedConfig = metaManager.getIDPDescriptorConfig(
                        realm, hostedEntityId);
                } else if (hostedRole.equalsIgnoreCase(IFSConstants.SP)) {
                    hostedProviderDesc = metaManager.getSPDescriptor(
                        realm, hostedEntityId);
                    hostedConfig = metaManager.getSPDescriptorConfig(
                        realm, hostedEntityId);
                }
            }
            if (hostedProviderDesc == null){
                throw new IDFFMetaException((String)null);
            }
        } catch (IDFFMetaException eam) {
            FSUtils.debug.error("Unable to find Hosted Provider. " +
                "not process request", eam);
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                FSUtils.bundle.getString(
                    IFSConstants.FAILED_HOSTED_DESCRIPTOR));
            return;
        }

        String logoutDoneURL = FSServiceUtils.getLogoutDonePageURL(
            request, hostedConfig, providerAlias);
        String commonErrorPage = FSServiceUtils.getErrorPageURL(
            request, hostedConfig, providerAlias);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("logoutDoneURL : " + logoutDoneURL +
                "\ncommonErrorPage : " + commonErrorPage);
        }

        String sourceCheck = (String) request.getAttribute("logoutSource");
        if (sourceCheck == null) {
            sourceCheck = request.getParameter("logoutSource");
        }
        Object ssoToken = getValidToken(request);
        String userID = null;
        if (ssoToken == null) {
            if (sourceCheck != null) {
                if (sourceCheck.equalsIgnoreCase("local")) {
                    // need to redirect to LogoutDone.jsp with
                    // status=noSession
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSProcessLogoutServlet, " +
                            "control where Source is local");
                    }
                    FSServiceUtils.returnLocallyAfterOperation(
                        response, logoutDoneURL, false,
                        IFSConstants.LOGOUT_SUCCESS,
                        IFSConstants.LOGOUT_NO_SESSION);
                    return;
                } else if (sourceCheck.equalsIgnoreCase("remote")){
                    // logout return
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "Control where Source is remote - not from app" +
                            "link but from other provider");
                    }
                    FSServiceUtils.returnLocallyAfterOperation(
                        response, logoutDoneURL, true,
                        IFSConstants.LOGOUT_SUCCESS,
                        IFSConstants.LOGOUT_FAILURE);
                    return;
                } else if (sourceCheck.equalsIgnoreCase("logoutGet")){
                    // logout Get profile
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "Control where Source is Http Get action - " +
                            "not from app link ");
                    }
                    FSServiceUtils.returnLocallyAfterOperation(
                        response, logoutDoneURL, true,
                        IFSConstants.LOGOUT_SUCCESS,
                        IFSConstants.LOGOUT_FAILURE);
                    return;
                }
            }
        } else {
            try {
                userID = 
                    SessionManager.getProvider().getPrincipalName(ssoToken);
            } catch (SessionException ssoExp) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Couldn't get user object:", ssoExp);
                }
            }
            if (sourceCheck != null) {
                if (sourceCheck.equalsIgnoreCase("local")) {
                    // initiate logout
                    FSUtils.debug.message(
                        "Control where Source is local -  from applink");
                    doLogoutInitiation(request, response, hostedProviderDesc, 
                        hostedConfig, realm, hostedEntityId, hostedRole, 
                        providerAlias, ssoToken, logoutDoneURL, sourceCheck);
                    return;
                } else if (sourceCheck.equalsIgnoreCase("remote")){
                    // logout return
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "Control where Source is remote - not from app" +
                            "link but from other provider. Token valid");
                    }
                    doLogoutInitiation(request, response, hostedProviderDesc, 
                        hostedConfig, realm, hostedEntityId, hostedRole, 
                        providerAlias, ssoToken, logoutDoneURL, sourceCheck);
                    return;
                } else if (sourceCheck.equalsIgnoreCase("logoutGet")){
                    // logout Get profile
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "Control where Source is Http Get action - not from"
                            + " applink. Initiation will take care in "
                            + "preLogouthandler ");
                    }
                    doLogoutInitiation(request, response, hostedProviderDesc, 
                        hostedConfig, realm, hostedEntityId, hostedRole, 
                        providerAlias, ssoToken, logoutDoneURL, sourceCheck);
                    return;
                }
            }
        }

        // received logout request from remote provider
        FSLogoutNotification logoutObj = null;
        try {
            logoutObj = FSLogoutNotification.parseURLEncodedRequest(request);
        } catch (FSMsgException e) {
            // FSMsgException would mean that the request does not have the
            // FSLogoutNotification message, so show error page
            FSUtils.debug.message(
                "Bad Logout request. calling showErrorPage");
            FSServiceUtils.showErrorPage(response,
                        commonErrorPage,
                        IFSConstants.LOGOUT_REQUEST_IMPROPER,
                        IFSConstants.LOGOUT_FAILED);
            return;
        }
        if (logoutObj == null) {
            FSUtils.debug.message(
                "Bad Logout request. calling showErrorPage");
            FSServiceUtils.showErrorPage(
                        response,
                        commonErrorPage,
                        IFSConstants.LOGOUT_REQUEST_IMPROPER,
                        IFSConstants.LOGOUT_FAILED);
        } else {
            doRequestProcessing(
                request,
                response,
                hostedProviderDesc,
                hostedConfig,
                hostedRole,
                realm,
                hostedEntityId,
                providerAlias,
                logoutObj,
                commonErrorPage,
                userID,
                ssoToken);
        }
        return;
    }
    
    /**
     * Retrieves valid session from HTTP Request.
     * @param request HTTP request object
     * @return session if the session is valid; <code>null</code>
     *  otherwise.
     */
    private Object getValidToken(HttpServletRequest request) {
        FSUtils.debug.message(
            "Entered FSProcessLogoutServlet::getValidToken");
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            Object ssoToken = sessionProvider.getSession(request);
            if ((ssoToken == null) || (!sessionProvider.isValid(ssoToken))) {
                FSUtils.debug.message(
                    "session is not valid, redirecting for authentication");
                return null;
            }
            return ssoToken;
        } catch (SessionException e){
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("SessionException caught: " + e);
            }
            return null;
        }
    }
    
    
    /**
     * Initiates logout request processing. It is called when a logout request
     * is received from a remote provider.
     * @param request <code>HTTPServletRequest</code> object received via a
     *  HTTP Redirect
     * @param response <code>HTTPServletResponse</code> object to be sent back
     *  to user agent
     * @param hostedDescriptor the provider for whom request is received
     * @param hostedConfig hosted provider's extended meta config
     * @param hostedRole hosted provider's role
     * @param realm the realm in which the entity resides
     * @param hostedEntityId hosted provider's entity id
     * @param metaAlias hosted provider's meta alias
     * @param reqLogout the single logout request
     * @param commonErrorPage where to go if an error occurred
     * @param userID user id
     * @param ssoToken user session object
     */
    private void doRequestProcessing(
        HttpServletRequest request,
        HttpServletResponse response,
        ProviderDescriptorType hostedDescriptor,
        BaseConfigType hostedConfig,
        String hostedRole,
        String realm,
        String hostedEntityId,
        String metaAlias,
        FSLogoutNotification reqLogout,
        String commonErrorPage,
        String userID,
        Object ssoToken)
    {
        FSUtils.debug.message(
            "Entered FSProcessLogoutServlet::doRequestProcessing");
        int minorVersion = reqLogout.getMinorVersion();

        String remoteEntityId = reqLogout.getProviderId();

        ProviderDescriptorType remoteDesc = null;
        boolean isIDP = false;
        try {
            if (hostedRole != null) {
                if (hostedRole.equalsIgnoreCase(IFSConstants.IDP)) {
                    remoteDesc = metaManager.getSPDescriptor(
                        realm, remoteEntityId);
                } else if (hostedRole.equalsIgnoreCase(IFSConstants.SP)) {
                    remoteDesc = metaManager.getIDPDescriptor(
                        realm, remoteEntityId);
                    isIDP = true;
                }
            }
            if (remoteDesc == null) {
                throw new IDFFMetaException((String) null);
            }
        } catch(IDFFMetaException e) {
            FSUtils.debug.error("Remote provider metadata not found.");
            String[] data = { remoteEntityId, realm };
            LogUtil.error(Level.INFO,LogUtil.INVALID_PROVIDER,data, ssoToken);
            FSLogoutUtil.returnToSource(response, 
                remoteDesc,
                IFSConstants.SAML_RESPONDER, commonErrorPage,
                minorVersion, hostedConfig, hostedEntityId, userID);
            return;
        }

        boolean bVerify = true;
        if (FSServiceUtils.isSigningOn()) {
            try {
                FSUtils.debug.message("Calling verifyLogoutSignature");
                bVerify = verifyLogoutSignature(
                    request, remoteDesc, remoteEntityId, isIDP);
            } catch(FSException e) {
                FSUtils.debug.error(
                    "FSProcessLogoutServlet::doRequestProcessing " +
                    "Signature on Logout request is invalid" +
                    "Cannot proceed federation Logout");
                String[] data = { userID };
                LogUtil.error(Level.INFO, LogUtil.INVALID_SIGNATURE,data,
                    ssoToken);
                FSLogoutUtil.returnToSource(response, remoteDesc, 
                    IFSConstants.SAML_REQUESTER, commonErrorPage,
                    minorVersion, hostedConfig, hostedEntityId, userID);
                return;
            } catch(SAMLException e) {
                FSUtils.debug.error(
                    "FSProcessLogoutServlet::doRequestProcessing(SAML) " +
                    "Signature on Logout request is invalid" +
                    "Cannot proceed federation Logout");
                String[] data = { userID };
                LogUtil.error(Level.INFO, LogUtil.INVALID_SIGNATURE,data,
                    ssoToken);
                FSLogoutUtil.returnToSource(response, remoteDesc,
                    IFSConstants.SAML_REQUESTER, commonErrorPage,
                    minorVersion, hostedConfig, hostedEntityId, userID);
                return;
            }
        }
        String errorStatus = IFSConstants.SAML_RESPONDER;
        if (bVerify) {
            // Check if trusted provider
            if (metaManager.isTrustedProvider(
                realm, hostedEntityId,remoteEntityId)) 
            {
                //Object ssoToken = getValidToken(request);
                if (ssoToken != null) {
                    // session is valid, start single logout
                    // Invoke Messaging APIs to get providerid from request
                    FSServiceManager instSManager =
                        FSServiceManager.getInstance();
                    if (instSManager != null) {
                        FSUtils.debug.message(
                            "FSServiceManager Instance not null");
                        // Call SP Adapter preSingleLogoutProcess
                        // for IDP/HTTP case
                        callPreSingleLogoutProcess(request, response,
                            hostedRole, hostedConfig, hostedEntityId, 
                            userID, reqLogout);
                        FSPreLogoutHandler handlerObj =
                            instSManager.getPreLogoutHandler();
                        if (handlerObj != null) {
                            handlerObj.setLogoutRequest(reqLogout);
                            handlerObj.setHostedDescriptor(
                                hostedDescriptor);
                            handlerObj.setHostedDescriptorConfig(
                                hostedConfig);
                            handlerObj.setRealm(realm);
                            handlerObj.setHostedEntityId(hostedEntityId);
                            handlerObj.setHostedProviderRole(hostedRole);
                            handlerObj.setMetaAlias(metaAlias);
                            handlerObj.setRemoteEntityId(remoteEntityId);
                            handlerObj.setRemoteDescriptor(remoteDesc);
                            handlerObj.processHttpSingleLogoutRequest(
                                request, response, ssoToken);
                            return;
                        }
                    } else {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSServiceManager Instance null. Cannot" +
                                " continue logout");
                        }
                        String[] data = { userID };
                        LogUtil.error(
                            Level.INFO,LogUtil.LOGOUT_FAILED, data, ssoToken);
                        FSLogoutUtil.returnToSource(
                            response,
                            remoteDesc, 
                            IFSConstants.SAML_RESPONDER,
                            commonErrorPage,
                            minorVersion,
                            hostedConfig,
                            hostedEntityId,
                            userID);
                        return;
                    }
                } else { // ssoToken is null
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "Invalid session in request processing. " +
                            "Nothing to logout");
                    }
                    //Verify request,getUserDNcall destroyPrincipalSession
                        userID = FSLogoutUtil.getUserFromRequest(
                        reqLogout, realm, hostedEntityId, hostedRole,
                        hostedConfig, metaAlias);
                    if (userID != null) {
                        FSLogoutUtil.destroyPrincipalSession(
                            userID,
                            metaAlias,
                            reqLogout.getSessionIndex(),
                            request,
                            response);
                        // Here we need to send back to source
                        // provider's return URL
                        FSLogoutUtil.returnToSource(
                            response, remoteDesc,
                            IFSConstants.SAML_RESPONDER, commonErrorPage,
                            minorVersion, hostedConfig, hostedEntityId,
                            userID);
                        return;
                    }
                }
            } else {
                FSUtils.debug.error("Remote provider not in trusted list");
            }
        } else {
            FSUtils.debug.error(
                "FSProcessLogoutServlet::doRequestProcesing " +
                "Signature on Logout request is invalid" +
                "Cannot proceed federation Logout");
            String[] data = { userID };
            LogUtil.error(Level.INFO,LogUtil.INVALID_SIGNATURE, data, ssoToken);
            errorStatus = IFSConstants.SAML_REQUESTER;
        }
        FSLogoutUtil.returnToSource(
            response, remoteDesc, errorStatus,
            commonErrorPage, minorVersion, hostedConfig, hostedEntityId,userID);
        return;
    }
    
    /**
     * Initiates logout request processing. Called when a logout is to be 
     * initiated or when returned from a remote provider.
     * @param request <code>HTTPServletRequest</code> object received via a
     *  HTTP Redirect
     * @param response <code>HTTPServletResponse</code> object to be sent back
     *  to user agent
     * @param hostedDescriptor the provider for whom request is received
     * @param hostedConfig hosted provider's extended meta config
     * @param realm the realm in which the provider resides
     * @param hostedEntityId hosted provider's entity id
     * @param metaAlias hosted provider's meta alias
     * @param ssoToken session token of the user
     * @param logoutDoneURL where to go when logout is done
     * @param sourceCheck source check string. Possible value:
     * <code>local</code> : single logout initiated from local host
     * <code>remote</code> : single logout initiated from remmote host
     * <code>logoutGet</code> : Http Get action.
     */
    private void doLogoutInitiation(
        HttpServletRequest request,
        HttpServletResponse response,
        ProviderDescriptorType hostedDescriptor,
        BaseConfigType hostedConfig,
        String realm,
        String hostedEntityId,
        String hostedRole,
        String metaAlias,
        Object ssoToken,
        String logoutDoneURL,
        String sourceCheck)
    {
        FSUtils.debug.message("FSProcessLogoutServlet::doLogoutInitiation");
        FSServiceManager instSManager = FSServiceManager.getInstance();
        String relayState = 
            request.getParameter(IFSConstants.LOGOUT_RELAY_STATE);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSProcessLogoutServlet.doLogoutInit: relay="
                + relayState);
        }
        if (instSManager != null) {
            FSUtils.debug.message("FSServiceManager Instance not null");
            FSPreLogoutHandler handlerObj =
                instSManager.getPreLogoutHandler();
            if (handlerObj != null) {
                handlerObj.setHostedDescriptor(hostedDescriptor);
                handlerObj.setHostedDescriptorConfig(hostedConfig);
                handlerObj.setRealm(realm);
                handlerObj.setHostedEntityId(hostedEntityId);
                handlerObj.setHostedProviderRole(hostedRole);
                handlerObj.setMetaAlias(metaAlias);
                handlerObj.setRelayState(relayState);
                handlerObj.handleSingleLogout(
                    request, response, ssoToken, sourceCheck);
                return;
            } else {
                FSUtils.debug.error(
                    "FSPreLogoutHandler is null.Cannot continue logout");
                String[] data = { logoutDoneURL };
                LogUtil.error(Level.INFO,
                        LogUtil.LOGOUT_FAILED_INVALID_HANDLER,data,ssoToken);
            }
        } else {
            FSUtils.debug.message(
                "FSServiceManager Instance null. Cannot continue logout");
        }
        FSServiceUtils.returnLocallyAfterOperation(
            response, logoutDoneURL, false,
            IFSConstants.LOGOUT_SUCCESS, IFSConstants.LOGOUT_FAILURE);
        return;
    }
    
    
    /**
     * Verifies logout request signature received from the remote end.
     * @param request <code>HttpServletRequest</code> object containing the
     *  signed Logout request
     * @param remoteDescriptor the remote Provider descriptor. Used to get cert
     * @param remoteEntity Id the remote provider's entity id
     * @return <code>true</code> if the signature is valid; <code>false</code>
     *  otherwise.
     * @exception SAMLException, FSException if an error occurred during the
     *  process
     */
    private boolean verifyLogoutSignature(
        HttpServletRequest request,
        ProviderDescriptorType remoteDescriptor,
        String remoteEntityId,
        boolean isIDP
    ) throws SAMLException, FSException
    {
        FSUtils.debug.message(
            "Entered FSProcessLogoutServlet::verifyLogoutSignature");
        // Verify the signature on the request
        X509Certificate cert = KeyUtil.getVerificationCert(
            remoteDescriptor, remoteEntityId, isIDP);
        if (cert == null) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSProcessLogoutServlet.verifyLogoutSignature: " +
                    "couldn't obtain this site's cert.");
            }
            throw new SAMLResponderException(
                FSUtils.bundle.getString(IFSConstants.NO_CERT));
        }
        boolean isValidSign =
            FSSignatureUtil.verifyRequestSignature(request, cert);
        if (!isValidSign) {
            FSUtils.debug.error("Logout request is not properly signed");
            return false;
        } else {
            FSUtils.debug.message("Logout request is properly signed");
            return true;
        }
    }

    private void callPreSingleLogoutProcess(
        HttpServletRequest request,
        HttpServletResponse response,
        String hostedRole,
        BaseConfigType hostedConfig,
        String hostedEntityId,
        String userID,
        FSLogoutNotification reqLogout) {
        // Call SP Adapter preSingleLogout for remote IDP initated HTTP request
        if (hostedRole != null && hostedRole.equalsIgnoreCase(IFSConstants.SP))
        {
            FederationSPAdapter spAdapter =
                FSServiceUtils.getSPAdapter(hostedEntityId, hostedConfig);
            if (spAdapter != null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSProcessLogoutServlet, " +
                        "call preSingleLogoutProcess");
                }
                try {
                    spAdapter.preSingleLogoutProcess(
                        hostedEntityId,
                        request, response, userID, reqLogout, null,
                        IFSConstants.LOGOUT_IDP_REDIRECT_PROFILE);
                } catch (Exception e) {
                    // ignore adapter exception
                    FSUtils.debug.error("preSingleLogoutProcess.IDP/HTTP", e);
                }
            }
        }
    }
}   // FSProcessLogoutServlet
