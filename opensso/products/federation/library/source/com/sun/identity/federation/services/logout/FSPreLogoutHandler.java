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
 * $Id: FSPreLogoutHandler.java,v 1.11 2008/12/19 06:50:47 exu Exp $
 *
 */


package com.sun.identity.federation.services.logout;

import com.sun.identity.federation.services.FSSessionPartner;
import com.sun.identity.federation.services.FSSession;
import com.sun.identity.federation.services.FSSessionManager;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.federation.services.util.FSSignatureUtil;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.LogUtil;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.message.FSLogoutResponse;
import com.sun.identity.federation.message.FSLogoutNotification;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.plugins.FederationSPAdapter;
import com.sun.identity.liberty.ws.meta.jaxb.ProviderDescriptorType;
import com.sun.identity.multiprotocol.MultiProtocolUtils;
import com.sun.identity.multiprotocol.SingleLogoutManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.saml.common.SAMLResponderException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.List;
import java.util.Iterator;
import java.util.Set;

/**
 * Pre logout handling.
 */
public  class FSPreLogoutHandler {
    protected HttpServletResponse response = null;
    protected HttpServletRequest request = null;
    protected String locale = null;
    protected Object ssoToken = null;
    protected String userID = null;
    protected IDFFMetaManager metaManager = null;
    protected ProviderDescriptorType hostedDescriptor = null;
    protected BaseConfigType hostedConfig = null;
    protected String currentEntityId = "";
    protected boolean isCurrentProviderIDPRole = false;
    protected FSLogoutNotification reqLogout = null;
    protected boolean isWMLAgent = false;
    protected static String LOGOUT_DONE_URL = null;
    protected static String COMMON_ERROR_URL = null;
    protected ProviderDescriptorType remoteDescriptor = null;
    protected String remoteEntityID = "";
    protected String realm = null;
    protected String hostedEntityId = "";
    protected String hostedRole = null;
    protected String metaAlias = null;
    protected String relayState = null;
    
    /**
     * Constructor.
     * Initializes FSAccountManager, IDFFMetaManager instance.
     */
    public FSPreLogoutHandler() {
        FSUtils.debug.message(
            "FSPreLogoutHandler::FSPreLogoutHandler Constructor");
        metaManager = FSUtils.getIDFFMetaManager();
    }
    
    /**
     * Invoked to set some commonly used URLs based on hosted provider.
     */
    protected void setLogoutURL() {
        LOGOUT_DONE_URL = FSServiceUtils.getLogoutDonePageURL(
            request, hostedConfig, metaAlias);
        COMMON_ERROR_URL = FSServiceUtils.getErrorPageURL(
            request, hostedConfig, metaAlias);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("LOGOUT_DONE_URL : " + LOGOUT_DONE_URL +
                "\nCOMMON_ERROR_URL : " + COMMON_ERROR_URL);
        }
    }
   
    /**
     * Sets the value of <code>RelayState</code> attribute.
     *
     * @param relayState the value of <code>RelayState</code> attribute.
     */
    public void setRelayState(String relayState) {
        this.relayState = relayState;
    } 

    /**
     * Sets the hosted provider details.
     * @param hostedProviderDesc the descriptor of the hosted provider
     *  handling logout
     */
    public void setHostedDescriptor(
        ProviderDescriptorType hostedProviderDesc)
    {
        this.hostedDescriptor = hostedProviderDesc;
    }

    /**
     * Sets hosted provider's realm.
     * @param realm the realm in which the provider resides
     */
    public void setRealm(String realm) {
        this.realm = realm;
    }

    /**
     * Sets hosted provider entity id.
     * @param hostedEntityId hosted provider's entity id to be set
     */
    public void setHostedEntityId(String hostedEntityId) {
        this.hostedEntityId = hostedEntityId;
    }

    /**
     * Sets hosted provider's extended meta config.
     * @param hostedConfig hosted provider's extended meta
     */
    public void setHostedDescriptorConfig(BaseConfigType hostedConfig) {
        this.hostedConfig = hostedConfig;
    }

    /**
     * Sets hosted provider's meta alias.
     * @param metaAlias hosted provider's meta alias to be set
     */
    public void setMetaAlias(String metaAlias) {
        this.metaAlias = metaAlias;
    }

    /**
     * Sets hosted provider's role.
     * @param hostedRole hosted provider's role.
     */
    public void setHostedProviderRole(String hostedRole) {
        this.hostedRole = hostedRole;
    }

    /**
     * Sets remote provider's entity id.
     */
    public void setRemoteEntityId(String remoteEntityId) {
        remoteEntityID = remoteEntityId;
    }

    /**
     * Sets the Remote Descriptor.
     * @param remoteDesc Remote Provider Descriptor.
     */
     public void setRemoteDescriptor(ProviderDescriptorType remoteDesc) {
         this.remoteDescriptor = remoteDesc;
     }
    
    /*
     * Sets the logout request received from remote provider.
     * @param reqLogout the <code>FSLogoutNotification</code> request from
     *  remote provider
     */
    public void setLogoutRequest(FSLogoutNotification reqLogout) {
        this.reqLogout = reqLogout;
    }
    
    
    /**
     * Initiates logout at this provider when the user has clicked on the
     * logout option.
     * @param request <code>HttPServletRequest</code> object from the user agent
     * @param response <code>HttPServletRsponse</code> to be sent back to the
     *  user agent
     * @param ssoToken used to identify the principal who wants to logout
     * @param sourceCheck where the logout coming from
     * @return <code>true</code> if the logout is successful; <code>false</code>
     *  otherwise.
     */
    public FSLogoutStatus handleSingleLogout(
        HttpServletRequest request,
        HttpServletResponse response,
        Object ssoToken,
        String sourceCheck)
    {
        this.request = request;
        setLogoutURL();
        FSUtils.debug.message(
            "Entered FSPreLogoutHandler::handleSingleLogout");
        try {
            this.response = response;
            this.ssoToken = ssoToken;
            this.userID = 
                SessionManager.getProvider().getPrincipalName(ssoToken);
            String acceptString = request.getHeader("Accept");
            if ((acceptString != null) &&
                (acceptString.indexOf("text/vnd.wap.wml") != -1))
            {
                isWMLAgent = true;
            }

            FSSessionManager sMgr = FSSessionManager.getInstance(metaAlias);
            FSSession session = sMgr.getSession(ssoToken);
           
            String sessionIndex = null;
            List partners = null;
            if (session != null) {
                sessionIndex = session.getSessionIndex();
                partners = session.getSessionPartners();
            }
          
            if (FSUtils.debug.messageEnabled()) {
                if (partners != null &&  partners.size() != 0) {
                    Iterator iter = partners.iterator();
                    while(iter.hasNext()) {
                        FSSessionPartner partner = 
                            (FSSessionPartner)iter.next();
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "PARTNER:" + partner.getPartner());
                        }
                    }
                }
            }

            if (FSLogoutUtil.liveConnectionsExist(
                userID, metaAlias))
            {
                HashMap providerMap = FSLogoutUtil.getCurrentProvider(
                    userID, metaAlias, ssoToken);
                if (providerMap != null) {
                    FSSessionPartner currentSessionProvider =
                        (FSSessionPartner)providerMap.get(
                            IFSConstants.PARTNER_SESSION);
                    sessionIndex =
                        (String)providerMap.get(IFSConstants.SESSION_INDEX);
                    if (currentSessionProvider != null) {
                        // this is IDP initiated based single logout
                        // HTTP or SOAP is based on metadata
                        FSUtils.debug.message("creating IDP handler");
                        FSSingleLogoutHandler handlerObj =
                            new FSSingleLogoutHandler();
                        handlerObj.setHostedDescriptor(hostedDescriptor);
                        handlerObj.setHostedDescriptorConfig(hostedConfig);
                        handlerObj.setRealm(realm);
                        handlerObj.setHostedEntityId(hostedEntityId);
                        handlerObj.setHostedProviderRole(hostedRole);
                        handlerObj.setMetaAlias(metaAlias);
                        handlerObj.setRelayState(relayState);
                        
                        return handlerObj.handleSingleLogout(
                            response, request, currentSessionProvider, userID,
                            sessionIndex, isWMLAgent, ssoToken);
                    }
                }
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "No more providers, nothing to broadcast " +
                        "\ndestroy user session call destroyPrincipalSession");
                }
                FSLogoutUtil.destroyPrincipalSession(
                    userID, metaAlias, sessionIndex, request, response);
                // control could come here when local login has happened
                // In this FSSessionmap will not have anything and so we destroy
                // the session based on ssoToken
                FSLogoutUtil.destroyLocalSession(ssoToken, request, response);
                returnToPostLogout(IFSConstants.SAML_SUCCESS);
                return new FSLogoutStatus(IFSConstants.SAML_SUCCESS);
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("No live connections, destroy user" +
                        " session call destroyPrincipalSession. source=" +
                        sourceCheck);
                }
                FSLogoutResponse logoutResponse = null;
                FederationSPAdapter spAdapter = null;
                // Call SP Adapter preSingleLogoutProcess for SP/HTTP
                if (hostedRole != null && 
                    hostedRole.equalsIgnoreCase(IFSConstants.SP) &&
                    sourceCheck.equals("remote"))
                {
                    spAdapter = FSServiceUtils.getSPAdapter(
                       hostedEntityId, hostedConfig);
                    if (spAdapter != null) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSPreLogoutHandler, " +
                                "call preSingleLogoutProcess, SP/HTTP");
                        }
                        try {
                            logoutResponse = FSLogoutResponse.
                                parseURLEncodedRequest(request);
                            relayState = logoutResponse.getRelayState();
                            // unabled to access logoutRequest here
                            spAdapter.preSingleLogoutProcess(
                                hostedEntityId,
                                request, response, userID, null, logoutResponse,
                                IFSConstants.LOGOUT_SP_REDIRECT_PROFILE);
                        } catch (Exception e) {
                            // ignore adapter error
                            FSUtils.debug.error(
                                "preSingleLogoutProcess.SP/HTTP", e);
                        }
                    }
                }

                FSLogoutUtil.destroyPrincipalSession(
                    userID, metaAlias, sessionIndex, request, response);
                // control will come here when local login has happened
                // In this FSSessionmap will not have anything and so we destroy
                // the session based on ssoToken
                if (SessionManager.getProvider().isValid(ssoToken)) { 
                    FSLogoutUtil.destroyLocalSession(
                        ssoToken, request, response);
                }
 
                // Call SP Adapter postSingleLogoutProcess for SP/HTTP
                if (hostedRole != null &&
                    hostedRole.equalsIgnoreCase(IFSConstants.SP) &&
                    sourceCheck.equals("remote"))
                {
                    if (spAdapter != null) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSPreLogoutHandler, " +
                                "call postSingleLogoutProcess, SP/HTTP");
                        }
                        try {
                            spAdapter.postSingleLogoutSuccess(
                                hostedEntityId,
                                request, response, userID, null, logoutResponse,
                                IFSConstants.LOGOUT_SP_REDIRECT_PROFILE);
                        } catch (Exception e) {
                            // ignore adapter exception
                            FSUtils.debug.error(
                                "postSingleLogoutSuccess.SP/HTTP:", e);
                        } 
                           
                    }
                }
                returnToPostLogout(IFSConstants.SAML_SUCCESS);
                return new FSLogoutStatus(IFSConstants.SAML_SUCCESS);
            }
        } catch(SessionException e) {
            FSUtils.debug.error("SessionException in liveConnectionsExist"
                + " So destroy self and exit");
            FSLogoutUtil.destroyPrincipalSession(
                userID, metaAlias, null, request, response);
            // cannot call FSLogoutUtil.destroyLocalSession(ssoToken)
            // since session exception has occurred
            returnToPostLogout(IFSConstants.SAML_SUCCESS);
            return new FSLogoutStatus(IFSConstants.SAML_SUCCESS);
        }
    }
    
    
    /**
     * Processes logout request received via HTTP redirect/GET.
     * @param request <code>HttpServletRequest</code> object from the user agent
     * @param response <code>HttpServletRsponse</code> to be sent back to the
     *  user agent
     * @param ssoToken used to identify the principal who wants to logout
     * @return <code>FSLogoutStatus</code> object to indicate the status of
     *  the logout process.
     */
    public FSLogoutStatus processHttpSingleLogoutRequest(
        HttpServletRequest request,
        HttpServletResponse response,
        Object ssoToken)
    {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("Entered FSPrelogoutHandler::" +
                "processSingleLogoutRequest HTTP Redirect");
        }
        this.request = request;
        this.locale = FSServiceUtils.getLocale(request);
        setLogoutURL();
        this.response = response;
        this.ssoToken = ssoToken;

        FSSessionManager sMgr = FSSessionManager.getInstance(metaAlias);
        FSSession session = sMgr.getSession(ssoToken);
        String sessionIndex = session.getSessionIndex();

        try {
            if (session!=null && session.getOneTime()) {
                this.userID = 
                    SessionManager.getProvider().getPrincipalName(ssoToken);
                FSUtils.debug.message("FSPH:processSingleLogout: Onetime case");
            } else {
                this.userID = FSLogoutUtil.getUserFromRequest(reqLogout, 
                    realm, hostedEntityId, hostedRole, hostedConfig, metaAlias);
            }
        } catch (SessionException se) {
            FSUtils.debug.error("processSingleLogoutRequest", se);
            this.userID = null;
        }

        if (userID == null) {
            FSUtils.debug.message("FSPrelogoutHandler::User Not found");
            FSLogoutUtil.returnToSource(response,
                            remoteDescriptor, 
                            IFSConstants.SAML_RESPONDER,
                            COMMON_ERROR_URL,
                            reqLogout.getMinorVersion(),
                            hostedConfig,
                            hostedEntityId,
                            userID);
            return new FSLogoutStatus(IFSConstants.SAML_RESPONDER);
        }
        String acceptString = request.getHeader("Accept");
        if ((acceptString != null) &&
            (acceptString.indexOf("text/vnd.wap.wml") != -1))
        {
            isWMLAgent = true;
        }

        String relayState = reqLogout.getRelayState();

        FSLogoutUtil.cleanSessionMapPartnerList(
            userID, remoteEntityID, metaAlias, session);

        FSUtils.debug.message("FSPrelogoutHandler::calling getCurrentProvider");
        boolean bHasAnyOtherProvider = false;
        HashMap providerMap = new HashMap();
        FSSessionPartner sessionPartner = null;
        providerMap = FSLogoutUtil.getCurrentProvider(
            userID, metaAlias, ssoToken);

        if (providerMap != null) {
            sessionPartner = (FSSessionPartner)providerMap.get(
                IFSConstants.PARTNER_SESSION);
            sessionIndex = (String)providerMap.get(IFSConstants.SESSION_INDEX);
            if (sessionPartner != null) {
                bHasAnyOtherProvider = true;
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("bHasAnyOtherProvider = " +
                        bHasAnyOtherProvider);
                }
                currentEntityId = sessionPartner.getPartner();
            }
        }

        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("bHasAnyOtherProvider other than source : " +
                bHasAnyOtherProvider);
        }
        // this is SP initiated HTTP based single logout
        FSUtils.debug.message("FSPreLogout::creating FSSingleLogoutHandler");
        FSSingleLogoutHandler handlerObj = new FSSingleLogoutHandler();
        handlerObj.setHostedDescriptor(hostedDescriptor);
        handlerObj.setHostedDescriptorConfig(hostedConfig);
        handlerObj.setRealm(realm);
        handlerObj.setHostedEntityId(hostedEntityId);
        handlerObj.setHostedProviderRole(hostedRole);
        handlerObj.setMetaAlias(metaAlias);
        handlerObj.setSingleLogoutProtocol(
            IFSConstants.LOGOUT_SP_REDIRECT_PROFILE);
        //handlerObj.setRemoteDescriptor(remoteDescriptor);
        //handlerObj.setRemoteEntityId(remoteEntityID);
        return handlerObj.processHttpSingleLogoutRequest(
            response, request, reqLogout,
            sessionPartner, userID, ssoToken, remoteEntityID, sessionIndex, 
            isWMLAgent, relayState, 
            (hostedRole.equals(IFSConstants.SP) ? 
                IFSConstants.IDP : IFSConstants.SP));
    }
    
    /**
     * Processes logout request received via SOAP profile.
     * @param reqLogout <code>FSLogoutNotification</code> request received from 
     *                  remote provider
     * @return <code>FSLogoutStatus</code> object indicates the status of
     *  the logout process
     */
    public FSLogoutStatus processSingleLogoutRequest(
        FSLogoutNotification reqLogout)
    {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("Entered FSPreLogoutHandler::" +
                " processSingleLogoutRequest SOAP Profile");
        }
        // User DN needs to be figured from logout request
        userID = FSLogoutUtil.getUserFromRequest(reqLogout, realm, 
            hostedEntityId, hostedRole, hostedConfig, metaAlias);
        if (userID == null) {
            FSUtils.debug.error("User does not exist. Invalid request");
            return new FSLogoutStatus(IFSConstants.SAML_REQUESTER);
        }

        FSSessionManager sessionManager = 
            FSSessionManager.getInstance(metaAlias);

        String sessionIndex = reqLogout.getSessionIndex();

        FSSession session = sessionManager.getSession(
            sessionManager.getSessionList(userID), sessionIndex);

        FSLogoutUtil.cleanSessionMapPartnerList(
            userID, remoteEntityID, metaAlias, session);

        boolean bHasAnyOtherProvider = false;
        HashMap providerMap = new HashMap();
        FSSessionPartner sessionPartner = null;
        providerMap = FSLogoutUtil.getCurrentProvider(
            userID, metaAlias, ssoToken, session);
       

        if (providerMap != null) {
            sessionPartner = (FSSessionPartner)providerMap.get(
                IFSConstants.PARTNER_SESSION);
            sessionIndex = (String)providerMap.get(IFSConstants.SESSION_INDEX);
            if (sessionPartner != null) {
                bHasAnyOtherProvider = true;
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("bHasAnyOtherProvider = " +
                        bHasAnyOtherProvider);
                }
                currentEntityId = sessionPartner.getPartner();
            }
        }

        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("bHasAnyOtherProvider other than source : " +
                bHasAnyOtherProvider);
        }
       
        // this is SP initiated SOAP based single logout
        FSUtils.debug.message("creating FSSingleLogoutHandler");
        FSSingleLogoutHandler handlerObj = new FSSingleLogoutHandler();
        handlerObj.setHostedDescriptor(hostedDescriptor);
        handlerObj.setHostedDescriptorConfig(hostedConfig);
        handlerObj.setRealm(realm);
        handlerObj.setHostedEntityId(hostedEntityId);
        handlerObj.setHostedProviderRole(hostedRole);
        handlerObj.setMetaAlias(metaAlias);
        handlerObj.setSingleLogoutProtocol(IFSConstants.LOGOUT_SP_SOAP_PROFILE);
        //handlerObj.setRemoteDescriptor(remoteDescriptor);
        //handlerObj.setRemoteEntityId(remoteEntityID);
        return handlerObj.processSingleLogoutRequest(
            reqLogout,
            sessionPartner,
            userID,
            remoteEntityID,
            sessionIndex,
            isWMLAgent,
            (hostedRole.equals(IFSConstants.SP)?
                IFSConstants.IDP : IFSConstants.SP));
    }
    
    /**
     * Determines the return location and redirects based on
     * logout Return URL of the provider that initially sent the logout request.
     * If request was not sent by remote provider then the local logout-done
     * page is thrown back to the user
     */
    private void returnToPostLogout(String logoutStatus) {
        FSUtils.debug.message("Entered FSPreLogoutHandler::returnToPostLogout");
        boolean error = false;
        boolean logoutSuccess = true;
        if (!logoutStatus.equals(IFSConstants.SAML_SUCCESS)) {
            logoutSuccess = false;
        }
        boolean multiProtocolInvoked = false;
        boolean toInvokeMultiProtocol = false;
        if (MultiProtocolUtils.isMultipleProtocolSession(request,
            SingleLogoutManager.IDFF) &&
            hostedRole.equalsIgnoreCase(IFSConstants.IDP) &&
            !MultiProtocolUtils.isMultiProtocolRelayState(relayState)) {
            toInvokeMultiProtocol = true;
        }
        try {
            String returnProviderId = "";
            String relayState = "";
            String gLogoutStatus = "";
            String inResponseTo = "";
            String retURL = null;
            
            FSLogoutResponse responseLogout = new FSLogoutResponse();
            FSReturnSessionManager mngInst =
                FSReturnSessionManager.getInstance(metaAlias);
            HashMap providerMap = new HashMap();

            if (mngInst != null) {
                providerMap = mngInst.getUserProviderInfo(userID);
            }
            if (providerMap == null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "Return URL based on local postlogout URL" +
                        "\nNo Source in ReturnMAP : rs=" + this.relayState);
                }
                if (toInvokeMultiProtocol) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSPreLogHandler.retToPostLogout:"
                            + " call MP HTTP, status=" + logoutStatus);
                    }
                    multiProtocolInvoked = true;
                    int retStatus = 
                        handleMultiProtocolLogout(logoutStatus, null);
                    if (retStatus == 
                        SingleLogoutManager.LOGOUT_REDIRECTED_STATUS) {
                        return;
                    } else {
                        if ((retStatus == 
                                SingleLogoutManager.LOGOUT_FAILED_STATUS) ||
                            (retStatus == 
                                SingleLogoutManager.LOGOUT_PARTIAL_STATUS)) {
                            logoutSuccess = false;
                        }
                    }
                }
                if ((this.relayState == null) || 
                    (this.relayState.length() == 0)) {
                    FSServiceUtils.returnLocallyAfterOperation(
                        response, LOGOUT_DONE_URL, logoutSuccess,
                        IFSConstants.LOGOUT_SUCCESS, 
                        IFSConstants.LOGOUT_FAILURE);
                } else {
                    FSServiceUtils.returnLocallyAfterOperation(
                        response, this.relayState, logoutSuccess,
                        IFSConstants.LOGOUT_SUCCESS, 
                        IFSConstants.LOGOUT_FAILURE);
                }
                return;
            }
            returnProviderId = (String) providerMap.get(IFSConstants.PROVIDER);
            ProviderDescriptorType descriptor = null;
            if (hostedRole.equalsIgnoreCase(IFSConstants.IDP)) {
                descriptor = metaManager.getSPDescriptor(
                    realm, returnProviderId);
            } else {
                descriptor = metaManager.getIDPDescriptor(
                    realm, returnProviderId);
            }
            retURL = descriptor.getSingleLogoutServiceReturnURL();
            relayState =
                (String) providerMap.get(IFSConstants.LOGOUT_RELAY_STATE);
            gLogoutStatus =
                (String) providerMap.get(IFSConstants.LOGOUT_STATUS);
            inResponseTo =
                (String) providerMap.get(IFSConstants.RESPONSE_TO);
            mngInst.removeUserProviderInfo(userID);
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Deleted " + userID +" from return list");
            }
            responseLogout.setResponseTo(inResponseTo);
            responseLogout.setRelayState(relayState);
            responseLogout.setProviderId(hostedEntityId);
            if (gLogoutStatus != null ) {
                responseLogout.setStatus(logoutStatus);
            }
            responseLogout.setID(IFSConstants.LOGOUTID);
            responseLogout.setMinorVersion(
                FSServiceUtils.getMinorVersion(
                    descriptor.getProtocolSupportEnumeration()));
            // call multi-federation protocol processing
            if (toInvokeMultiProtocol) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSPreLogHandler.retToPostLogout:"
                        + " call MP HTTP, response=" 
                        + responseLogout.toXMLString());
                }
                multiProtocolInvoked = true;
                int retStatus = handleMultiProtocolLogout(logoutStatus, 
                    responseLogout.toXMLString(true, true));
                if (retStatus == 
                    SingleLogoutManager.LOGOUT_REDIRECTED_STATUS) {
                    return;
                } else {
                    if ((retStatus == 
                            SingleLogoutManager.LOGOUT_FAILED_STATUS) ||
                        (retStatus == 
                            SingleLogoutManager.LOGOUT_PARTIAL_STATUS)) {
                        logoutSuccess = false;
                        responseLogout.setStatus(IFSConstants.SAML_RESPONDER);
                    }
                }
            }            
            String urlEncodedResponse =
                responseLogout.toURLEncodedQueryString();
            // Sign the request querystring
            if (FSServiceUtils.isSigningOn()) {
                String certAlias = 
                    IDFFMetaUtils.getFirstAttributeValueFromConfig(
                        hostedConfig, IFSConstants.SIGNING_CERT_ALIAS);
                if (certAlias == null || certAlias.length() == 0) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "FSBrowserArtifactConsumerHandler:: " +
                            "signSAMLRequest:" +
                            "couldn't obtain this site's cert alias.");
                    }
                    throw new SAMLResponderException(
                        FSUtils.bundle.getString(IFSConstants.NO_CERT_ALIAS));
                    
                }
                urlEncodedResponse = FSSignatureUtil.signAndReturnQueryString(
                    urlEncodedResponse, certAlias);
            }
            StringBuffer redirectURL = new StringBuffer();
            redirectURL.append(retURL);
            if (retURL.indexOf(IFSConstants.QUESTION_MARK) == -1) {
                redirectURL.append(IFSConstants.QUESTION_MARK);
            } else {
                redirectURL.append(IFSConstants.AMPERSAND);
            }
            redirectURL.append(urlEncodedResponse);
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Response to be sent (3) : " +
                    redirectURL.toString());
            }
            response.sendRedirect(redirectURL.toString());
            return;
        } catch (IOException e){
            FSUtils.debug.error(
                "Unable to get LRURL. No location to redirect." +
                "processing completed:", e);
            error = true;
        } catch (IDFFMetaException e){
            FSUtils.debug.error("Unable to get LRURL. No location to redirect" +
                " processing completed:", e);
            error = true;
        } catch (Exception e) {
            FSUtils.debug.error(
                "FSPreLogoutHandler::General exception thrown :", e);
            error = true;
        }
        if (error) {
            String[] data =
                {FSUtils.bundle.getString(IFSConstants.LOGOUT_REDIRECT_FAILED)};
            LogUtil.error(Level.INFO,LogUtil.LOGOUT_REDIRECT_FAILED,data,
                ssoToken);
            logoutSuccess = false;
        }
        // call multi-federation protocol processing
        if (toInvokeMultiProtocol && !multiProtocolInvoked) {
            // invoke multiple federation protocol in exception case
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSPreLogHandler.retToPostLogout:"
                        + " call MP HTTP, error=" + error);
            }
            multiProtocolInvoked = true;
            int retStatus = handleMultiProtocolLogout(logoutStatus, null);
            if (retStatus == SingleLogoutManager.LOGOUT_REDIRECTED_STATUS) {
                return;
            } else {
                if ((retStatus == SingleLogoutManager.LOGOUT_FAILED_STATUS) ||
                    (retStatus == SingleLogoutManager.LOGOUT_PARTIAL_STATUS)) {
                    logoutSuccess = false;
                }
            }
        }
        FSServiceUtils.returnLocallyAfterOperation(
            response, LOGOUT_DONE_URL, logoutSuccess,
            IFSConstants.LOGOUT_SUCCESS, IFSConstants.LOGOUT_FAILURE);
        return;
    }
    
    private int handleMultiProtocolLogout(String status, String responseXML) {
        int currentStatus = SingleLogoutManager.LOGOUT_FAILED_STATUS;
        if ((status != null) && status.equals(IFSConstants.SAML_SUCCESS)) {
            currentStatus = SingleLogoutManager.LOGOUT_SUCCEEDED_STATUS;
        }
        Set set = new HashSet();
        set.add(ssoToken);
        int retStatus = SingleLogoutManager.LOGOUT_SUCCEEDED_STATUS;
        try {
            String requestXML = (reqLogout == null) ?
                null : reqLogout.toXMLString(true, true);
            boolean isSOAPProfile = true;
            String[] propVals = SessionManager.getProvider()
                .getProperty(ssoToken, IFSConstants.IS_SOAP_PROFILE);
            if ((propVals != null) && (propVals.length != 0) &&
                (propVals[0].equals("false"))) {
                isSOAPProfile = false;
            }
            String finalRelayState = relayState;
            if ((finalRelayState == null) || (finalRelayState.length() == 0)) {
                finalRelayState = LOGOUT_DONE_URL;
            }
            retStatus = SingleLogoutManager.getInstance().
                doIDPSingleLogout(set, userID, request, response, isSOAPProfile,
                    true, SingleLogoutManager.IDFF, realm, hostedEntityId,
                    remoteEntityID, finalRelayState, requestXML, responseXML,
                    currentStatus);
        } catch (Exception e) {
            FSUtils.debug.error("FSSLOHandler.doIDPProfile: MP/SOAP", e);
            retStatus = SingleLogoutManager.LOGOUT_FAILED_STATUS;
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSSLOHandler.doIDPSoapProfile: "
                    + "return status = " + retStatus);
        }
        return retStatus;
    }
}
