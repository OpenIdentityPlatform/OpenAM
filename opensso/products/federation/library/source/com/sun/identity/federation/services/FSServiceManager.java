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
 * $Id: FSServiceManager.java,v 1.5 2008/06/25 05:46:56 qcheng Exp $
 *
 */

package com.sun.identity.federation.services;

import com.sun.identity.federation.accountmgmt.FSAccountFedInfo;
import com.sun.identity.federation.accountmgmt.FSAccountFedInfoKey;
import com.sun.identity.federation.accountmgmt.FSAccountManager;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.message.FSAuthnResponse;
import com.sun.identity.federation.message.FSFederationTerminationNotification;
import com.sun.identity.federation.message.FSNameRegistrationRequest;
import com.sun.identity.federation.message.FSRequest;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.services.fednsso.FSAssertionArtifactHandler;
import com.sun.identity.federation.services.fednsso.FSBrowserArtifactConsumerHandler;
import com.sun.identity.federation.services.fednsso.FSBrowserPostConsumerHandler;
import com.sun.identity.federation.services.fednsso.FSLECPConsumerHandler;
import com.sun.identity.federation.services.fednsso.FSSSOAndFedHandler;
import com.sun.identity.federation.services.fednsso.FSSSOBrowserArtifactProfileHandler;
import com.sun.identity.federation.services.fednsso.FSSSOBrowserPostProfileHandler;
import com.sun.identity.federation.services.fednsso.FSSSOLECPProfileHandler;
import com.sun.identity.federation.services.fednsso.FSSSOWMLPostProfileHandler;
import com.sun.identity.federation.services.fednsso.FSWMLPostConsumerHandler;
import com.sun.identity.federation.services.logout.FSPreLogoutHandler;
import com.sun.identity.federation.services.registration.FSNameRegistrationHandler;
import com.sun.identity.federation.services.termination.FSFedTerminationHandler;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.liberty.ws.meta.jaxb.ProviderDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.IDPDescriptorType;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.protocol.Request;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Services use this class to obtain appropriate handlers for different
 * profiles.
 */
public class FSServiceManager {
    private static FSServiceManager instance = null;
    
    /**
     * Private constructor.
     */
    private FSServiceManager() {
        FSUtils.debug.message("FSServiceManager(): Called");
    }
    
    /**
     * Returns handler at <code>SP</code> side which will handle fed/sso
     * profile.
     * @param request http request object
     * @param response http response object
     * @param authnRequest authentication request object
     * @param authnResponse authentication response object
     * @param idpDescriptor identity provider descriptor who issued the
     *  authentication response
     * @param idpEntityId identity provider's entity ID
     * @return <code>FSAssertionArtifactHandler</code> object
     */
    public FSAssertionArtifactHandler getAssertionArtifactHandler(
        HttpServletRequest request,
        HttpServletResponse response,
        FSAuthnRequest authnRequest,
        FSAuthnResponse authnResponse,
        IDPDescriptorType idpDescriptor,
        String idpEntityId
    ) {
        FSUtils.debug.message(
            "FSServiceManager.getAssertionArtifactHandler: Called");
        if ((request == null) ||
            (response == null) ||
            (authnRequest == null) ||
            (authnResponse == null))
        {
            FSUtils.debug.error("FSServiceManager.getAssertionArtifactHandler: "
                + FSUtils.bundle.getString("nullInputParameter"));
            return null;
        }
        FSAssertionArtifactHandler returnHandler = null;
        String profile = authnRequest.getProtocolProfile();
        boolean doFederate = authnRequest.getFederate();
        String relayState = authnResponse.getRelayState();
        if (profile != null) {
            if (profile.equals(IFSConstants.SSO_PROF_BROWSER_POST)) {
                returnHandler = new FSBrowserPostConsumerHandler(
                    request,
                    response, 
                    idpDescriptor,
                    idpEntityId,
                    authnRequest,
                    doFederate,
                    relayState);
            } else if (profile.equals(IFSConstants.SSO_PROF_WML_POST)) {
                returnHandler = new FSWMLPostConsumerHandler(
                    request,
                    response, 
                    idpDescriptor,
                    idpEntityId,
                    authnRequest,
                    doFederate, 
                    relayState);
            } else if (profile.equals(IFSConstants.SSO_PROF_LECP)) {
                returnHandler = new FSLECPConsumerHandler(
                    request,
                    response,
                    idpDescriptor,
                    idpEntityId,
                    authnRequest,
                    doFederate,
                    relayState);
            } else {
                FSUtils.debug.error(
                    "FSServiceManager.getAssertionArtifactHandler: "
                    + "Unknown Protocol profile request");
                return null;
            }
        } else {
            FSUtils.debug.error("FSServiceManager.getAssertionArtifactHandler: "
                + "No protocol profile in the Request");
            return null;
        }
        return returnHandler;
    }
    
    /**
     * Obtains handler at <code>SP</code> side that will handle browser
     * artifact profile.
     * @param request http request object
     * @param response http response object
     * @param realm The realm under which the entity resides.
     * @param idpSuccinctId identity provider's succinct ID
     * @param samlRequest <code>SAML</code> request object
     * @param relayState where to go after the process is done
     * @return <code>FSAssertionArtifactHandler</code> object
     */
    public FSAssertionArtifactHandler getBrowserArtifactHandler(
        HttpServletRequest request,
        HttpServletResponse response,
        String realm,
        String idpSuccinctId,
        FSRequest samlRequest,
        String relayState
    ) {
        FSUtils.debug.message(
            "FSServiceManager.getBrowserArtifactHandler: Called");
        if ((request == null) ||
            (response == null) ||
            (idpSuccinctId == null) ||
            (samlRequest == null)) 
        {
            FSUtils.debug.error("FSServiceManager.getBrowserArtifactHandler: "
                + FSUtils.bundle.getString("nullInputParameter"));
            return null;
        }
        try {
            IDFFMetaManager metaManager =
                FSUtils.getIDFFMetaManager();
            String idpEntityId = metaManager.getEntityIDBySuccinctID(
                realm, idpSuccinctId);
            IDPDescriptorType idpDescriptor =
                metaManager.getIDPDescriptor(realm, idpEntityId);
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSServiceManager.getBrowserArtifactHandler:" +
                    " IDP Id of the provider to communicate: " +
                    idpEntityId);
            }
            return new FSBrowserArtifactConsumerHandler(
                request, response, idpDescriptor, idpEntityId,
                relayState, samlRequest);
        } catch(Exception ex){
            FSUtils.debug.error(
                "FSServiceManager.getBrowserArtifactHandler:Exception Occured:",
                ex);
            return null;
        }
    }
    
    /**
     * Returns handler at <code>IDP</code> side that handles single sign on and
     * federation requests.
     * @param request http request object
     * @param response http response object
     * @param authnRequest authentication request sent by service provider
     * @param realm The realm under which the entity resides.
     * @return <code>FSSSOAndFedHandler</code> object
     */
    public FSSSOAndFedHandler getSSOAndFedHandler(
        HttpServletRequest request,
        HttpServletResponse response,
        FSAuthnRequest authnRequest,
        String realm
    ) {
        FSUtils.debug.message("FSServiceManager.getSSOAndFedHandler: Called ");
        if ((request == null) ||(response == null) ||(authnRequest == null)) {
            FSUtils.debug.error("FSServiceManager.getSSOAndFedHandler: "
                + FSUtils.bundle.getString("nullInputParameter"));
            return null;
        }
        try {
            FSSSOAndFedHandler returnHandler = null;
            String profile = authnRequest.getProtocolProfile();
            IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
            String spEntityId = authnRequest.getProviderId();
            SPDescriptorType spDescriptor = 
                metaManager.getSPDescriptor(realm, spEntityId);
            BaseConfigType spConfig =
                metaManager.getSPDescriptorConfig(realm, spEntityId);
            String relayState = authnRequest.getRelayState();
            
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSServiceManager.getSSOAndFedHandler: requested profile:"
                    + profile);
            }
            if (profile != null) {
                if (profile.equals(IFSConstants.SSO_PROF_BROWSER_ART)) {
                    returnHandler = new FSSSOBrowserArtifactProfileHandler(
                        request,
                        response,
                        authnRequest,
                        spDescriptor,
                        spConfig,
                        spEntityId,
                        relayState);
                } else if (profile.equals(IFSConstants.SSO_PROF_BROWSER_POST)){
                    returnHandler = new FSSSOBrowserPostProfileHandler(
                        request,
                        response,
                        authnRequest,
                        spDescriptor,
                        spConfig,
                        spEntityId,
                        relayState);
                } else if(profile.equals(IFSConstants.SSO_PROF_WML_POST)){
                    returnHandler = new FSSSOWMLPostProfileHandler(
                        request,
                        response,
                        authnRequest,
                        spDescriptor, 
                        spConfig,
                        spEntityId,
                        relayState);
                } else if(profile.equals(IFSConstants.SSO_PROF_LECP)){
                    returnHandler = new FSSSOLECPProfileHandler(
                        request,
                        response,
                        authnRequest,
                        spDescriptor,
                        spConfig,
                        spEntityId,
                        relayState);
                } else {
                    FSUtils.debug.error("FSServiceManager.getSSOAndFedHandler: "
                        + "Unknown Protocol profile request");
                    returnHandler = new FSSSOBrowserArtifactProfileHandler(
                        request,
                        response,
                        authnRequest,
                        spDescriptor,
                        spConfig,
                        spEntityId,
                        relayState);
                }
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSServiceManager.getSSOAndFedHandler: "
                        + "No protocol profile in the Request");
                }
                returnHandler = new FSSSOBrowserArtifactProfileHandler(
                    request,
                    response,
                    authnRequest,
                    spDescriptor,
                    spConfig,
                    spEntityId,
                    relayState);
            }
            return returnHandler;
        } catch(IDFFMetaException ex){
            FSUtils.debug.error("FSServiceManager.getSSOAndFedHandler: ", ex);
            return null;
        }
    }
    
    /**
     * Returns handler for <code>IDP</code> to handle browser artifact profile.
     * @param request http request object
     * @param response http response object
     * @param samlRequest <code>SAML</code> request
     * @return <code>FSSSOAndFedHandler</code> object
     */
    public FSSSOAndFedHandler getBrowserArtifactSSOAndFedHandler(
        HttpServletRequest request,
        HttpServletResponse response,
        Request samlRequest
    ) {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSServiceManager::" +
                " getBrowserArtifactSSOAndFedHandler: Called");
        }
        if ((request == null) ||(response == null) ||(samlRequest == null)) {
            FSUtils.debug.error("FSServiceManager.getSSOAndFedHandler: "
                + FSUtils.bundle.getString("nullInputParameter"));
            return null;
        }
        return new FSSSOBrowserArtifactProfileHandler(
            request, response, samlRequest);
    }
    
    /**
     * Returns handler at <code>IDP</code> that handles <code>LECP</code>
     * profile.
     * @param request http request object
     * @param response http response object
     * @param authnRequest authentication request
     * @param realm The realm under which the entity resides.
     * @return <code>FSSSOLECPProfileHandler</code> object
     */
    public FSSSOLECPProfileHandler getLECPProfileHandler(
        HttpServletRequest request,
        HttpServletResponse response,
        FSAuthnRequest authnRequest,
        String realm
    ) {
        FSUtils.debug.message("FSServiceManager.getLECPProfileHandler:Called");
        try {
            if ((request == null) ||
                (response == null) ||
                (authnRequest == null))
            {
                FSUtils.debug.error("FSServiceManager.getLECPProfileHandler: "
                    + FSUtils.bundle.getString("nullInputParameter"));
                return null;
            }
            IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
            String spEntityId = authnRequest.getProviderId();
            return new FSSSOLECPProfileHandler(
                request,
                response, 
                authnRequest,
                metaManager.getSPDescriptor(realm, spEntityId),
                metaManager.getSPDescriptorConfig(realm, spEntityId),
                spEntityId,
                authnRequest.getRelayState());
        } catch(IDFFMetaException ex){
            FSUtils.debug.error("FSServiceManager.getLECPProfileHandler: ", ex);
            return null;
        }
    }
    
    /*
     * Returns name registration handler. This method is invoked by the
     * <code>SP</code> at the end of account federation if name registration is
     * turned on.
     * The <code>remoteEntityId</code> passed is that of the <code>IdP</code>
     * with whom registration will be done.
     * @param realm the realm in which the provider resides
     * @param remoteEntityId remote Provider Entity ID. 
     * @param remoteProviderRole remote Provider Role.
     * @return <code>FSNameRegistrationHandler</code> the name registration 
     *  handler
     */
    public FSNameRegistrationHandler getNameRegistrationHandler(
        String realm,
        String remoteEntityId,
        String remoteProviderRole)
    {
        FSNameRegistrationHandler handlerRegistration =
            new FSNameRegistrationHandler();
        if (handlerRegistration != null) {
            try {
                IDFFMetaManager metaManager = 
                     FSUtils.getIDFFMetaManager();
                if (metaManager == null) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSNameRegistrationHandler::" +
                            "getSPNameIdentifier failed to get meta " +
                            "Manager instance");
                    }
                    return null;
                }
                ProviderDescriptorType remoteDesc = null;
                if (remoteProviderRole == null) {
                    return null;
                } else if (remoteProviderRole.equalsIgnoreCase(
                    IFSConstants.IDP))
                {
                    remoteDesc = metaManager.getIDPDescriptor(
                        realm, remoteEntityId);
                } else if (remoteProviderRole.equalsIgnoreCase(IFSConstants.SP))
                {
                    remoteDesc = metaManager.getSPDescriptor(
                        realm, remoteEntityId);
                }

                if (remoteDesc != null) {
                    handlerRegistration.setRemoteDescriptor(remoteDesc);
                    handlerRegistration.setRemoteEntityId(remoteEntityId);
                    handlerRegistration.setRealm(realm);
                    return handlerRegistration;
                } else {
                    return null;
                }
            } catch(IDFFMetaException e){
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSNameRegistrationHandler::Failed " +
                        "to get remote descriptor:", e);
                }
                return null;
            }
        }
        return handlerRegistration;
    }
    
    /*
     * Returns <code>FSFedTerminationHandler</code>. This method is invoked at
     * the end where the termination request is received. The handler is 
     * responsible for doing account defederation.
     * @param terminationRequest federation termination request
     * @param hostedConfig Hosted Provider's extended meta
     * @param hostedEntityId hosted provider's entity ID
     * @param hostedProviderRole hosted provider's role
     * @param metaAlias hosted provider's meta alias
     * @param remoteEntityId remote provider's entity ID
     * @return <code>FSFedTerminationHandler</code> object
     */
    public FSFedTerminationHandler getFedTerminationHandler(
        FSFederationTerminationNotification terminationRequest,
        BaseConfigType hostedConfig,
        String realm,
        String hostedEntityId,
        String hostedProviderRole,
        String metaAlias,
        String remoteEntityId
    ) {
        try {
            FSUtils.debug.message(
                "Entered FSServicemanager::getFedTerminationHandler");
            FSAccountManager managerInst = FSAccountManager.getInstance(
                metaAlias);
            if (managerInst == null) {
                FSUtils.debug.error(
                    "Error in retrieving account manager");
                return null;
            }

            NameIdentifier nameIdObj = terminationRequest.getNameIdentifier();
            String nameIDValue = nameIdObj.getName();
            // Get amId
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Remote provider : " + remoteEntityId
                    + ", Name Qualifier : " + nameIdObj.getNameQualifier()
                    + ", Name : " + nameIDValue + ", Realm : " + realm);
            }
            String nameQualifier = nameIdObj.getNameQualifier();
            String searchDomain = hostedEntityId;
            if (nameQualifier != null && 
                !nameQualifier.equals(remoteEntityId))
            {
               searchDomain = nameQualifier;
            }
            FSAccountFedInfoKey acctkey = null;
            // for SP, search local, then remote IDP,
            // for IDP,  search remote SP, then local
            if (hostedProviderRole.equalsIgnoreCase(IFSConstants.SP)) {
                acctkey = new FSAccountFedInfoKey(
                    searchDomain, nameIDValue);
            } else {
                acctkey = new FSAccountFedInfoKey(remoteEntityId, nameIDValue);
            }
            Map env = new HashMap();
            env.put(IFSConstants.FS_USER_PROVIDER_ENV_TERMINATION_KEY,
                terminationRequest);
            String userID = managerInst.getUserID(acctkey, realm, env);
            if (userID == null) {
                if (hostedProviderRole.equalsIgnoreCase(IFSConstants.SP)) {
                    acctkey = new FSAccountFedInfoKey(
                        remoteEntityId, nameIDValue);
                } else {
                    acctkey = new FSAccountFedInfoKey(
                        hostedEntityId, nameIDValue);
                }
                userID = managerInst.getUserID(acctkey, realm, env);
                if (userID == null) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("UserID is null");
                    }
                    return null;
                }
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("user ID is "+ userID);
            }
            FSAccountFedInfo acctInfo = managerInst.readAccountFedInfo(
                userID, remoteEntityId, nameIDValue);
            if (acctInfo == null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Account federation with provider " +
                        remoteEntityId + " does not exist");
                }
                return null;
            }

            // Pass USERID TO HANDLER to AVOID SEARCH AGAIN
            FSFedTerminationHandler handlerTermination = 
                new FSFedTerminationHandler();
            if (handlerTermination != null){
                handlerTermination.setUserID(userID);
                handlerTermination.setAccountInfo(acctInfo);
                return handlerTermination;
            } else {
                FSUtils.debug.message("Termination Handler is null");
                return null;
            }
        } catch(Exception e){
            FSUtils.debug.error("FSServiceManager::getFedTerminationHandler " +
                "failed to get termination handler");
        }
        return null;
    }
    
    /*
     * Returns <code>FSPreLogouHandler</code>. This method is invoked when a 
     * logout request is to be processed.
     * @return FSPreLogoutHandler PreLogout handler
     */
    public FSPreLogoutHandler getPreLogoutHandler() {
        FSUtils.debug.message(
            "Entered FSServicemanager::getPreLogoutHandler");
        try {
            FSPreLogoutHandler handlerLogout = null;
            handlerLogout = new FSPreLogoutHandler();
            if (handlerLogout != null) {
                return handlerLogout;
            }
            FSUtils.debug.message("PreLogoutHandler is null");
        }catch (Exception e){
            FSUtils.debug.error("FSServiceManager::getSingleLogoutHandler " +
                "failed to get logout handler");
        }
        return null;
    }
    
    /*
     * Returns federation termination handler. This method is invoked at the
     * end where the termination is initiated. The handler is responsible
     * for doing account defederation locally and then invoking termination at
     * remote provider end.
     * @param remoteEntityId provider with whom termination needs to be done
     * @param remoteProviderRole role of remote provider
     * @param userID user who is terminating federation with remote provider
     * @param realm the realm under which the entity resides
     * @param hostedEntityId hosted provider's entity id
     * @param metaAlias hosted provider's meta alias
     * @return <code>FSFedTerminationHandler</code> object
     */
    public FSFedTerminationHandler getFedTerminationHandler(
        String remoteEntityId,
        String remoteProviderRole,
        String userID,
        String realm,
        String hostEntityId,
        String metaAlias
    ) {
        try {
            if(FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Entered FSServicemanager::" +
                " getFedTerminationHandler");
            }
            FSFedTerminationHandler handlerTermination = null; // check for null
            IDFFMetaManager metaManager =
                FSUtils.getIDFFMetaManager();
            FSAccountManager managerInst = FSAccountManager.getInstance(
                metaAlias);
            if (metaManager == null || managerInst == null) {
                FSUtils.debug.message(
                    "Error in retrieving meta, account manager");
                return null;
            }
            FSAccountFedInfo acctInfo =
                managerInst.readAccountFedInfo(userID, remoteEntityId);
            if (acctInfo == null){
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Account federation with provider " +
                    remoteEntityId + " does not exist");
                }
                return null;
            }
            // Pass USERDN TO HANDLER to AVOID SEARCH AGAIN
            if(FSUtils.debug.messageEnabled()) {
               FSUtils.debug.message("FSServiceManager.getFedTermination" +
               "Handler: remoteEntityID = " + remoteEntityId +
               " remoteProviderRole = " + remoteProviderRole);
            }

            handlerTermination = new FSFedTerminationHandler();
            if(handlerTermination != null){
                ProviderDescriptorType remoteDesc = null;
                if (remoteProviderRole.equalsIgnoreCase(IFSConstants.IDP)) {
                    remoteDesc = metaManager.getIDPDescriptor(
                        realm, remoteEntityId);
                } else {
                    remoteDesc = metaManager.getSPDescriptor(
                        realm, remoteEntityId);
                }
                handlerTermination.setRemoteDescriptor(remoteDesc);
                handlerTermination.setRemoteEntityId(remoteEntityId);
                handlerTermination.setUserID(userID);
                handlerTermination.setAccountInfo(acctInfo);
                return handlerTermination;
            } else {
                FSUtils.debug.message("Termination Handler is null");
                return null;
            }
        } catch(Exception e){
            FSUtils.debug.error("FSServiceManager::getFedTerminationHandler " +
            "failed to get termination handler");
        }
        return null;
    }

    /*
     * Returns <code>FSNameRegistrationHandler</code> instance. This method is
     * invoked at the end where the registration is initiated. The handler 
     * is responsible for doing name registration at remote provider end and 
     * then locally.
     * @param remoteEntityId provider with whom registration is to be done
     * @param remoteProviderRole role of the remote provider
     * @param userID user for whom registration will be done with remote
     *  provider
     * @param hostEntityId hosted provider's entity ID
     * @param metaAlias hosted provider's meta alias
     * @return Name registration handler
     */
    public FSNameRegistrationHandler getNameRegistrationHandler(
       String remoteEntityId,
       String remoteProviderRole,
       String userID,
       String hostEntityId,
       String metaAlias
    ) {
        try {
            FSUtils.debug.message(
                "Entered FSServiceManager::getNameRegistrationHandler");
            IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
            FSAccountManager managerInst =
                                FSAccountManager.getInstance(metaAlias);
            if (metaManager == null || managerInst == null) {
                FSUtils.debug.message(
                    "Error in retrieving meta, account manager");
                return null;
            }
            FSAccountFedInfo acctInfo =
                managerInst.readAccountFedInfo(userID, remoteEntityId);
            if (acctInfo == null){
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Account federation with provider " +
                        remoteEntityId + " does not exist");
                }
                return null;
            }
            String realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
            // Pass USERID TO HANDLER to AVOID SEARCH AGAIN
            FSNameRegistrationHandler handlerRegistration = 
                new FSNameRegistrationHandler();
            if (handlerRegistration != null){
                ProviderDescriptorType remoteDesc = null;
                BaseConfigType remoteConfig = null;
                if (remoteProviderRole.equalsIgnoreCase(IFSConstants.SP)) {
                    remoteDesc = metaManager.getSPDescriptor(
                        realm, remoteEntityId);
                    remoteConfig = metaManager.getSPDescriptorConfig(
                        realm, remoteEntityId);
                } else {
                    remoteDesc = metaManager.getIDPDescriptor(
                        realm, remoteEntityId);
                    remoteConfig = metaManager.getIDPDescriptorConfig(
                        realm, remoteEntityId);
                }
                handlerRegistration.setRealm(realm);
                handlerRegistration.setRemoteEntityId(remoteEntityId);
                handlerRegistration.setRemoteDescriptor(remoteDesc);
                handlerRegistration.setUserID(userID);
                handlerRegistration.setAccountInfo(acctInfo);
                return handlerRegistration;
            } else {
                FSUtils.debug.message("Registration Handler is null");
                return null;
            }
        } catch(Exception e){
            FSUtils.debug.error("FSServiceManager::getNameRegistrationHandler "+
                "failed to get registration handler");
        }
        return null;
    }
    
    /**
     * Returns <code>FSServiceManager</code> instance.
     * @return <code>FSServiceManager</code> instance
     */
    public static FSServiceManager getInstance() {
        FSUtils.debug.message("FSServiceManager.getInstance: Called ");
        if (instance == null) {
            synchronized(FSServiceManager.class) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSServiceManager.getInstance: " +
                        " Creating a new instance of ServiceManager");
                }
                instance = new FSServiceManager();
            }
        }
        return instance;
    }
    
}// end class
