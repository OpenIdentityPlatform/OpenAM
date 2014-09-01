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
 * $Id: FSNameRegistrationHandler.java,v 1.7 2008/12/19 06:50:47 exu Exp $
 *
 */


package com.sun.identity.federation.services.registration;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.federation.accountmgmt.FSAccountManager;
import com.sun.identity.federation.accountmgmt.FSAccountMgmtException;
import com.sun.identity.federation.accountmgmt.FSAccountFedInfo;
import com.sun.identity.federation.accountmgmt.FSAccountFedInfoKey;
import com.sun.identity.federation.services.FSSession;
import com.sun.identity.federation.services.FSSessionManager;
import com.sun.identity.federation.services.FSSOAPService;
import com.sun.identity.federation.services.util.FSSignatureUtil;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.federation.services.util.FSNameIdentifierHelper;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.LogUtil;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.key.KeyUtil;
import com.sun.identity.federation.message.FSNameRegistrationRequest;
import com.sun.identity.federation.message.FSNameRegistrationResponse;
import com.sun.identity.federation.message.common.IDPProvidedNameIdentifier;
import com.sun.identity.federation.message.common.SPProvidedNameIdentifier;
import com.sun.identity.federation.message.common.OldProvidedNameIdentifier;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.plugins.FederationSPAdapter;
import com.sun.identity.liberty.ws.meta.jaxb.ProviderDescriptorType;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.saml.protocol.StatusCode;
import com.sun.identity.saml.protocol.Status;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLResponderException;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.shared.encode.URLEncDec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPException;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.logging.Level;

/**
 * Work class that handles registration process.
 */
public class FSNameRegistrationHandler {
    
    protected HttpServletResponse response = null;
    protected HttpServletRequest request = null;
    protected HashMap regisMap = new HashMap();
    protected FSNameRegistrationResponse regisResponse = null;
    protected FSNameRegistrationRequest regisRequest = null;
    protected Object ssoToken = null;
    protected String userID = null;        
    protected FSAccountFedInfo acctInfo = null;
    protected FSAccountFedInfo newAcctInfo = null;
    protected FSAccountFedInfoKey newAcctKey = null;
    protected FSAccountFedInfoKey oldAcctKey = null;
    protected ProviderDescriptorType remoteDescriptor = null;
    protected ProviderDescriptorType hostedDescriptor = null;
    protected BaseConfigType hostedConfig = null;
    protected String metaAlias = null;
    protected String relayState = "";
    protected FSAccountManager managerInst = null; 
    protected static String REGISTRATION_DONE_URL = null;        
    protected static final String RELAY_STATE = 
        IFSConstants.REGISTRATION_RELAY_STATE;
    protected HashMap returnMap = new HashMap();
    protected static String returnURL = "";
    protected static String regisSource = "";
    protected String remoteEntityId = "";
    protected String realm = "";
    protected String hostedEntityId = "";
    protected String hostedProviderRole = null;;
         
    /**
     * Constructor. Initializes FSAccountManager, meta Manager instance.
     */
    public FSNameRegistrationHandler() {
        FSUtils.debug.message("FSNameRegistrationHandler Constructor...");
    }
    
    /**
     * Invoked to set some commonly used URLs based on hosted provider.
     */
    protected void setRegistrationURL() {
        REGISTRATION_DONE_URL = FSServiceUtils.getRegistrationDonePageURL(
            request, hostedConfig, metaAlias);
    }   

    /**
     * Sets hosted provider descriptor.
     * This function is called from FSServiceManager to give state to the 
     * name registration handler that is handling the current 
     * name registration. The hosted provider identifies the provider who 
     * is handling the registration request or initiating it locally.
     * @param hostedDescriptor the Hosted provider Descriptor
     */
    public void setHostedDescriptor(ProviderDescriptorType hostedDescriptor)
    {
        FSUtils.debug.message(
                "Entered FSNameRegistrationHandler::setHostedDescriptor");
        this.hostedDescriptor = hostedDescriptor;
    } 
        
    /**
     * Sets hosted provider's extended meta.
     * @param hostedConfig hosted provider's extended meta.
     */
    public void setHostedDescriptorConfig(BaseConfigType hostedConfig) {
        this.hostedConfig = hostedConfig;
    }

    /**
     * Sets hosted provider's entity ID.
     * @param hostedId hosted provider's entity ID
     */
    public void setHostedEntityId(String hostedId) {
        hostedEntityId = hostedId;
    }

    /**
     * Sets hosted provider's role.
     * @param hostedRole hosted provider's role
     */
    public void setHostedProviderRole(String hostedRole) {
        this.hostedProviderRole = hostedRole;
    }

    /**
     * Sets hosted provider's meta alias.
     * @param metaAlias hosted provider's meta alias
     */
    public void setMetaAlias(String metaAlias) {
        this.metaAlias = metaAlias;
        try {
            managerInst = FSAccountManager.getInstance(metaAlias);
        } catch (FSAccountMgmtException e){
            FSUtils.debug.error("FSNameRegistrationHandler " +
                FSUtils.bundle.getString(
                    IFSConstants.FEDERATION_FAILED_ACCOUNT_INSTANCE));
            managerInst = null;
        }
    }

    /**
     * Sets realm.
     *
     * @param realm The realm under which the entity resides.
     */
    public void setRealm(String realm) {
        this.realm = realm;
    }

    /**
     * Sets remote provider's entity ID.
     * @param remoteId remote provider's entity ID
     */
    public void setRemoteEntityId(String remoteId) {
        remoteEntityId = remoteId;
    }

    /**
     * Sets remote provider descriptor.
     * This function is called to give state to the Name registration 
     * handler The remote provider identifies the provider who sent a request 
     *  or with whom registration is to be initiated.
     * @param remoteDescriptor the Remote provider Descriptor
     */
    public void setRemoteDescriptor(ProviderDescriptorType remoteDescriptor) {
        FSUtils.debug.message(
                "Entered FSNameRegistrationHandler::setRemoteDescriptor");
        this.remoteDescriptor =  remoteDescriptor;
    } 
    
    /**
     * Sets the UserDN.
     * @param userID the user who is initiating
     */
    public void setUserID(String userID) 
    {
        this.userID = userID;
    } 
    
    /**
     * Sets the federation account information for the
     * user with a specific remote provider.
     * @param acctInfo the account fed info object
     */
    public void setAccountInfo(FSAccountFedInfo acctInfo) 
    {
        this.acctInfo = acctInfo;
    }
    
    /**
     * Determines the user based on the registration request received from 
     * a remote provider.
     * @param regisRequest the name registration request
     * @return <code>true</code> a user is found; <code>false</code> otherwise.
     */
    public boolean setUserDN(FSNameRegistrationRequest regisRequest) 
    {
        try {
            /**
             * UserDN needs to be figured from registration request
             * 1. If OldNameIdentifier does not exist then its from SP to IdP
             *    (first time)
             * 2. If OldNameIdentifier exist then we could be SP, IdP 
             * Need to find out whether to replace with SPNI or IDPNI based on 
             * acctFedInfo that is retrieved based on OldNameIdentifier or
             * IdpNameIdentifier
             * If isIDP true then remote is IdP so replace remoteIdentifier
             * with IDPNameIdentifier.
             * If isIDP false then  remote is SP so replace remoteIdentifier 
             * with SPNameIdentifier
             */
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("remoteEntityId : " + remoteEntityId);
            }
            Map env = new HashMap();
            env.put(IFSConstants.FS_USER_PROVIDER_ENV_REGISTRATION_KEY,
                        regisRequest);
            OldProvidedNameIdentifier oldNameIdentifier =
                            regisRequest.getOldProvidedNameIdentifier();
            IDPProvidedNameIdentifier idpNameIdentifier = 
                    regisRequest.getIDPProvidedNameIdentifier();
            SPProvidedNameIdentifier spNameIdentifier = 
                    regisRequest.getSPProvidedNameIdentifier();
            if (oldNameIdentifier == null)
            {
                FSUtils.debug.message("oldProvidedNameIdentifier is null :" );
                String opaqueHandle = idpNameIdentifier.getName();
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "processRegistrationRequest IdPName : " + opaqueHandle);
                }
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Realm : " + realm);
                }

                String searchDomain = hostedEntityId;
                String nameQualifier = idpNameIdentifier.getNameQualifier(); 
                if (nameQualifier != null &&
                        (nameQualifier.length() != 0) &&
                        !nameQualifier.equals(remoteEntityId))
                {
                   searchDomain = nameQualifier;
                }
                FSAccountFedInfoKey acctkey = new FSAccountFedInfoKey(
                                        searchDomain, opaqueHandle);

                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Search based on:" +
                        hostedEntityId +
                        opaqueHandle );
                }
                this.userID = managerInst.getUserID(acctkey, realm, env);
                if (this.userID == null) {                    
                    FSUtils.debug.message("UserID is null");
                    return false;
                }
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("user id is "+ userID);
                }
                try {
                    acctInfo = managerInst.readAccountFedInfo(
                        userID, remoteEntityId, opaqueHandle);
                } catch (FSAccountMgmtException e){
                    FSUtils.debug.message("Failed to read account information");
                    return false;
                }
                newAcctInfo = new FSAccountFedInfo (remoteEntityId, 
                       idpNameIdentifier, spNameIdentifier, false);
                newAcctInfo.setAffiliation(acctInfo.getAffiliation());
                newAcctKey = new FSAccountFedInfoKey(searchDomain, 
                       idpNameIdentifier.getName());

                return true;
            } else {
                FSUtils.debug.message("oldProvidedNameIdentifier not null");
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Realm : " + realm);
                }
                String opaqueHandle = "";
                String nameQualifier = null;
                boolean isSPEmpty = false;                                
                String searchDomain = hostedEntityId;
                if (spNameIdentifier != null && 
                    !(spNameIdentifier.equals(oldNameIdentifier)))
                {
                    opaqueHandle =  spNameIdentifier.getName();
                    nameQualifier = spNameIdentifier.getNameQualifier();
                } else {
                    isSPEmpty = true;
                    opaqueHandle =  idpNameIdentifier.getName();
                    nameQualifier = idpNameIdentifier.getNameQualifier();
                }
                if (nameQualifier != null &&
                    (nameQualifier.length() != 0) &&
                    !nameQualifier.equals(hostedEntityId))
                {
                    searchDomain = nameQualifier;
                }
                FSAccountFedInfoKey acctkey = new FSAccountFedInfoKey(
                        searchDomain, opaqueHandle);

                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Search based on :" +
                        searchDomain + " " + opaqueHandle );
                }
                this.userID = managerInst.getUserID(acctkey, realm, env);
                if (this.userID == null) {
                    FSUtils.debug.message("UserID is null in step 3");
                    opaqueHandle = idpNameIdentifier.getName();
                    nameQualifier = idpNameIdentifier.getNameQualifier();
                    if (nameQualifier != null &&
                        (nameQualifier.length() != 0) &&
                        !nameQualifier.equals(hostedEntityId))
                    {
                        searchDomain = nameQualifier;
                    }
                    acctkey = new FSAccountFedInfoKey(
                                searchDomain, opaqueHandle);
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("Search based on :"+
                            searchDomain + " " + opaqueHandle );
                    }
                    this.userID = managerInst.getUserID(acctkey, realm, env);
                    if (this.userID == null) {
                        opaqueHandle = idpNameIdentifier.getName();
                        searchDomain = hostedEntityId;
                        acctkey = new FSAccountFedInfoKey(
                            searchDomain, opaqueHandle);
                        this.userID = managerInst.getUserID(acctkey, realm,env);
                        if (this.userID == null) {
                            if (FSUtils.debug.messageEnabled()) {
                                 FSUtils.debug.message("UserID is null in "+
                                 "step 4");
                            }
                            opaqueHandle = oldNameIdentifier.getName();
                            searchDomain = oldNameIdentifier.getNameQualifier();
                            if ((searchDomain != null) &&
                                (searchDomain.length() != 0))
                            {
                                 acctkey = new FSAccountFedInfoKey(
                                     searchDomain, opaqueHandle);
                                 if (FSUtils.debug.messageEnabled()) {
                                     FSUtils.debug.message(
                                         "Search based on :" + 
                                         searchDomain +
                                         " " +
                                         opaqueHandle );
                                }
                                this.userID = managerInst.getUserID(
                                              acctkey, realm, env);
                            }
                        }
                        if (this.userID == null) {
                            return false;
                        } else {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message("Found user : " + userID);
                            }
                            acctInfo = managerInst.readAccountFedInfo(
                                userID, searchDomain, opaqueHandle);
                            if (acctInfo == null || 
                                !acctInfo.isFedStatusActive())
                            { 
                                acctInfo = managerInst.readAccountFedInfo(
                                    userID, remoteEntityId, opaqueHandle);
                            }
                            if (acctInfo.isRoleIDP()) {
                                if (isSPEmpty) {
                                    // set spNI to null since is empty
                                    newAcctInfo = new FSAccountFedInfo(
                                        remoteEntityId,
                                        null,
                                        idpNameIdentifier, 
                                        acctInfo.isRoleIDP());
                                    newAcctInfo.setAffiliation(
                                        acctInfo.getAffiliation());
                                    newAcctKey = new FSAccountFedInfoKey(
                                        searchDomain, 
                                        idpNameIdentifier.getName());
                               } else {
                                    newAcctInfo = new FSAccountFedInfo (
                                        remoteEntityId,
                                        spNameIdentifier, 
                                        idpNameIdentifier,
                                        acctInfo.isRoleIDP());
                                    newAcctInfo.setAffiliation(
                                        acctInfo.getAffiliation());
                                    newAcctKey = new FSAccountFedInfoKey(
                                        searchDomain, 
                                        spNameIdentifier.getName());
                                }
                            } else {
                                newAcctInfo = new FSAccountFedInfo (
                                    remoteEntityId,
                                    idpNameIdentifier, 
                                    spNameIdentifier,
                                    acctInfo.isRoleIDP());
                                newAcctInfo.setAffiliation(
                                    acctInfo.getAffiliation());
                                newAcctKey = new FSAccountFedInfoKey(
                                    hostedEntityId, 
                                    idpNameIdentifier.getName());
                            }
                        }
                    } else {
                        acctInfo = managerInst.readAccountFedInfo(
                            userID, searchDomain, opaqueHandle);
                        if (acctInfo == null || !acctInfo.isFedStatusActive()) {
                            acctInfo = managerInst.readAccountFedInfo(
                                userID, remoteEntityId, opaqueHandle);
                        }
                        newAcctInfo = new FSAccountFedInfo (
                            remoteEntityId,
                            idpNameIdentifier, 
                            spNameIdentifier,
                            false);
                        newAcctInfo.setAffiliation(acctInfo.getAffiliation());
                        newAcctKey = new FSAccountFedInfoKey(
                            searchDomain, idpNameIdentifier.getName());
                    }
                } else {
                    acctInfo = managerInst.readAccountFedInfo(
                        userID, searchDomain,opaqueHandle);
                    if (acctInfo == null || !acctInfo.isFedStatusActive()) { 
                        acctInfo = managerInst.readAccountFedInfo(
                            userID, remoteEntityId, opaqueHandle);
                    }
                    if (acctInfo.isRoleIDP()) {
                        if (isSPEmpty) {
                            // set spNI to null since is empty
                            newAcctInfo = new FSAccountFedInfo(
                                remoteEntityId,
                                null,
                                idpNameIdentifier, 
                                acctInfo.isRoleIDP());
                            newAcctInfo.setAffiliation(
                                acctInfo.getAffiliation());
                            newAcctKey = new FSAccountFedInfoKey(
                                remoteEntityId, idpNameIdentifier.getName());
                        } else {
                            newAcctInfo = new FSAccountFedInfo (
                                remoteEntityId,
                                spNameIdentifier, 
                                idpNameIdentifier,
                                acctInfo.isRoleIDP());
                            newAcctInfo.setAffiliation(
                                acctInfo.getAffiliation());
                            newAcctKey = new FSAccountFedInfoKey(
                                hostedEntityId, spNameIdentifier.getName());
                        }
                    } else {
                        newAcctInfo = new FSAccountFedInfo(
                            remoteEntityId,
                            idpNameIdentifier,
                            spNameIdentifier, 
                            acctInfo.isRoleIDP());
                        newAcctInfo.setAffiliation(acctInfo.getAffiliation());
                        newAcctKey = new FSAccountFedInfoKey(
                            hostedEntityId, idpNameIdentifier.getName());
                    }
                }
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("user id is "+ userID);                
            }
            return true;
        } catch(FSAccountMgmtException e) {
            FSUtils.debug.error("In FSNameRegistrationHandler::setUserID: ", e);
        }
        this.userID = null;
        return false;
    }
    
    /**
     * Initiates the name registration operation.
     * @param request HTTP request
     * @param response HTTP response
     * @param ssoToken corresponding to the user's session
     * @return the status of the registration initiation operation. 
     *  <code>true</code> if successful; <code>false</code> otherwise.
     */
    public boolean handleNameRegistration(
        HttpServletRequest request,
        HttpServletResponse response, 
        Object ssoToken
    ) 
    {
        regisSource = IFSConstants.REGIS_LINK;
        FSUtils.debug.message(
            "Entered FSNameRegistrationHandler::handleNameRegistration");
        this.request = request;
        this.response = response;
        this.ssoToken = ssoToken;
        setRegistrationURL();
        if (managerInst == null) {
            FSUtils.debug.error("FSNameRegistrationHandler " +
                "Account Manager instance is null");
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSNameRegistrationHandler::handleNameRegistration" +
                    "failed to get Account Manager instance");
            }
            returnLocallyAtSource(response, false);
            return false;
        }                                

        try {
            this.userID = 
                SessionManager.getProvider().getPrincipalName(ssoToken);
        } catch(SessionException e) {
            FSUtils.debug.error(
                "FSNameRegistrationHandler Constructor::SessionException:",e);
            // cannot proceed without user
            LogUtil.error(Level.INFO,"USER_NOT_FOUND",null,ssoToken);
            return false;
        }        
        return (doRemoteRegistration());                
    }

    /**
     * Handles the name registration after sso.
     * @param request HTTP request
     * @param response HTTP response
     * @param ssoToken corresponding to the users's session
     * @return the status of the registration initiation operation. 
     *  <code>true</code> if successful; <code>false</code> otherwise.
     */
    public boolean handleNameRegistration(
        HttpServletRequest request,
        HttpServletResponse response, 
        Object ssoToken,
        HashMap valMap) 
    {
        regisSource = IFSConstants.REGIS_SSO;
        FSUtils.debug.message(
            "Entered FSNameRegistrationHandler::handleNameRegistration");
        this.request = request;
        this.response = response;
        this.ssoToken = ssoToken;
        this.regisMap = valMap;
        setRegistrationURL();
        if (managerInst == null) {
            FSUtils.debug.error("FSNameRegistrationHandler " +
                "Account Manager instance is null");
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSNameRegistrationHandler::handleNameRegistration" +
                    "failed to get Account Manager instance");
            }
            returnLocallyAtSource(response, false);
            return false;
        }                                
        
        try {
            this.userID = 
                SessionManager.getProvider().getPrincipalName(ssoToken);
        } catch(SessionException e) {
            FSUtils.debug.error(
                "FSNameRegistrationHandler Constructor::SessionException", e);
            // cannot proceed without user
            LogUtil.error(Level.INFO,"USER_NOT_FOUND",null, ssoToken);
            return false;
        }        
        boolean bRemoteStatus = doRemoteRegistration();        
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "Returning cntrol to SIngle Sign On with status " +
                bRemoteStatus);
        }
        return bRemoteStatus;        
    }

    /**
     * Does local name registration and initiates remote registration with the
     * IDP. 
     * @param LRURL the final return URL after Name registration is complete
     * @param response HTTP response object
     * @return <code>true</code> if successful; <code>false</code> otherwise.
     */
    public boolean handleRegistrationAfterFederation( 
        String LRURL, HttpServletResponse response) 
    {                
        regisSource = IFSConstants.REGIS_FEDERATION;
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("Entered FSNameRegistrationHandler:: " +
                "handleNameRegistration");
        }                                
        this.returnURL = LRURL;
        this.response = response;

        if (managerInst == null) {
            FSUtils.debug.error(
                "FSNameRegistrationHandler " + FSUtils.bundle.getString(
                IFSConstants.FEDERATION_FAILED_ACCOUNT_INSTANCE));
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSNameRegistrationHandler::handleNameRegistration" +
                    "failed to get Account Manager instance");
            }
            return false;
        }
        boolean bRemoteStatus = doRemoteRegistration();        
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "Completed registration after federation with status " +
                bRemoteStatus);
        }
        return bRemoteStatus;      
    }


    /**
     * Initiates the name registration operation.
     * @param request HTTP request
     * @param response HTTP response
     * @param regisResponse <code>FSNameRegistrationResponse</code> object
     * @return <code>true</code> if the operation succeeds; <code>false</code>
     *  otherwise.
     */
    public boolean processRegistrationResponse(
        HttpServletRequest request,
        HttpServletResponse response, 
        FSNameRegistrationResponse regisResponse) 
    {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("Entered FSNameRegistrationHandler::" +
                "handleRegistrationResponse");
        }
        this.request = request;
        this.response = response;
        this.regisResponse = regisResponse;
        setRegistrationURL();
        if (managerInst == null) {
            FSUtils.debug.error("FSNameRegistrationHandler " +
                "Account Manager instance is null");
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSNameRegistrationHandler::handleNameRegistration" +
                    "failed to get Account Manager instance");
            }
            returnLocallyAtSource(response, false);
            return false;
        }
        String responseStatus = 
            ((regisResponse.getStatus()).getStatusCode()).getValue();
        if (responseStatus.equals (IFSConstants.SAML_SUCCESS)) {
            FSUtils.debug.message("Name registration Successful");
            relayState = regisResponse.getRelayState();                        
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Relay State : " + relayState);
            }
            if (relayState == null) {
                returnLocallyAtSource(response,false);
                return true;
            } else {
                FSRegistrationManager regisManager = 
                    FSRegistrationManager.getInstance(metaAlias);
                HashMap valMap = regisManager.getRegistrationMap(relayState);
                if (valMap == null) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("Map does not contain request "
                            + "for state = " + relayState);
                    }
                    returnLocallyAtSource(response,false);
                    return false;
                } else {
                    // remove from the registration manager map
                    regisManager.removeRegistrationMapInfo(relayState);
                    regisMap = (HashMap) valMap.get("SSODetails");
                    HashMap returnMap = (HashMap) valMap.get("ReturnEntry");
                    oldAcctKey = (FSAccountFedInfoKey) returnMap.get(
                        "OldAccountKey");
                    if (oldAcctKey != null) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("Get OldAcctKet Name : " + 
                                oldAcctKey.getName() +
                                "\nGet OldAcctKet Qualifier : " + 
                                oldAcctKey.getNameSpace());
                        }
                    } else {
                        FSUtils.debug.message("OldAccount Key is null");
                    }
                    newAcctKey = 
                        (FSAccountFedInfoKey) returnMap.get("AccountKey");
                    if (newAcctKey != null) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("Get newAcctKey Name : " + 
                                newAcctKey.getName() +
                                "Get newAcctKey Qualifier : " + 
                                newAcctKey.getNameSpace());
                        }
                    } else {
                        FSUtils.debug.message("newAcctKey Key is null");
                    }
                    newAcctInfo =(FSAccountFedInfo)returnMap.get("AccountInfo");
                    userID = (String)returnMap.get("userID");
                    regisSource = (String)returnMap.get("RegisSource");
                    returnURL = (String) returnMap.get(IFSConstants.LRURL);
                    boolean bStatus = doCommonRegistration();
                    // Call SP Adapter for SP/IDP initiated HTTP profile
                    if (bStatus && hostedProviderRole != null &&
                        hostedProviderRole.equalsIgnoreCase(IFSConstants.SP)) 
                    {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("processRegResponse/HTTP, " +
                                "call postRegisterNameIdentifier success");
                        }
                        callPostRegisterNameIdentifierSuccess(
                            request, response, userID, null, regisResponse,
                            IFSConstants.NAME_REGISTRATION_SP_HTTP_PROFILE);
                    }
                    returnLocallyAtSource(response, bStatus);
                    return bStatus;
                }
            }
        } else if (responseStatus.equals(
            IFSConstants.FEDERATION_NOT_EXISTS_STATUS))
        {
            FSUtils.debug.message(
                "Name registration Failed. Federation does not exist");
            returnLocallyAtSource(response, false);
            return false; 
        } else if (responseStatus.equals (
            IFSConstants.REGISTRATION_FAILURE_STATUS))
        {
            FSUtils.debug.message("Name registration Failed.");
            returnLocallyAtSource(response, false); 
            return false;
        }
        return false;
    }

    /**
     * Initiates federation registration at remote end.
     * The registration request is constructed and based on the profile the
     * request is sent over SOAP or as HTTP redirect. Profile is always based on
     * the SPs profile.
     * @return <code>true</code> if the process is successful;
     *  <code>false</code> otherwise.
     */
    private boolean doRemoteRegistration()
    {
        FSUtils.debug.message(
            "Entered FSNameRegistrationHandler::doRemoteRegistration");
        try {
            try {
                if (acctInfo == null) {
                    acctInfo = managerInst.readAccountFedInfo(
                        userID, remoteEntityId);
                }
            } catch (FSAccountMgmtException e){                                
                returnLocallyAtSource(response, false);
                return false;
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSNameRegistrationHandler::doRemoteRegistration create" +
                    " request start");
            }
            FSNameRegistrationRequest regisRequest = 
                createNameRegistrationRequest(acctInfo);
            if (regisRequest == null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSNameRegistrationHandler::Registration request could "
                            + "not be formed");
                }
                returnLocallyAtSource(response, false);
                return false;
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSNameRegistrationHandler::Registration request formed" +
                    "successfully");
            }
            // Find out which profile to use
            boolean isSOAPProfile = true;
            if (acctInfo.isRoleIDP()) {
                List hostProfiles = 
                    hostedDescriptor.getRegisterNameIdentifierProtocolProfile();
                if (hostProfiles == null || hostProfiles.isEmpty()) {
                    FSUtils.debug.error("FSNameRegistrationHandler::" +
                        "doRemoteRegistration no registration profile" +
                        " cannot process request");
                    returnLocallyAtSource(response, false);
                    return false;  
                }    
                String hostProfile = (String) hostProfiles.iterator().next();
                if (hostProfile.equalsIgnoreCase(
                        IFSConstants.REGISTRATION_SP_SOAP_PROFILE) || 
                    hostProfile.equalsIgnoreCase(
                        IFSConstants.REGISTRATION_IDP_SOAP_PROFILE)) 
                {
                    isSOAPProfile = true;
                } else if (hostProfile.equalsIgnoreCase(
                        IFSConstants.REGISTRATION_SP_HTTP_PROFILE) || 
                    hostProfile.equalsIgnoreCase(
                        IFSConstants.REGISTRATION_IDP_HTTP_PROFILE))
                {
                    isSOAPProfile = false;
                } else {
                    FSUtils.debug.error("FSNameRegistrationHandler::" +
                        "doRemoteRegistration Invalid registration profile" +
                        " cannot process request");
                    returnLocallyAtSource(response, false);
                    return false;  
                }
            } else  {       
                List remoteProfiles = 
                    remoteDescriptor.getRegisterNameIdentifierProtocolProfile();
                if (remoteProfiles == null || remoteProfiles.isEmpty()) {
                    FSUtils.debug.error("FSNameRegistrationHandler::" +
                        "doRemoteRegistration no registration profile" +
                        " cannot process request");
                    returnLocallyAtSource(response, false);
                    return false;  
                }    
                String remoteProfile = (String)remoteProfiles.iterator().next();
                if (remoteProfile.equalsIgnoreCase(
                        IFSConstants.REGISTRATION_SP_SOAP_PROFILE) || 
                    remoteProfile.equalsIgnoreCase(
                        IFSConstants.REGISTRATION_IDP_SOAP_PROFILE))
                {
                    isSOAPProfile = true;
                } else if (remoteProfile.equalsIgnoreCase(
                        IFSConstants.REGISTRATION_SP_HTTP_PROFILE) || 
                    remoteProfile.equalsIgnoreCase(
                        IFSConstants.REGISTRATION_IDP_HTTP_PROFILE))
                {
                    isSOAPProfile = false;
                } else {
                    FSUtils.debug.error("FSNameRegistrationHandler::" +
                        "doRemoteRegistration Invalid registration profile" +
                        " cannot process request");
                    returnLocallyAtSource(response, false);
                    return false; 
                }
            }
            if (isSOAPProfile) 
            {
                FSSOAPService instSOAP = FSSOAPService.getInstance();
                if (instSOAP != null) {
                    FSUtils.debug.message(
                        "Signing suceeded. To call bindRegistrationRequest");
                    regisRequest.setID(IFSConstants.REGISTRATIONID);
                    SOAPMessage msgRegistration = 
                        instSOAP.bind(regisRequest.toXMLString(true, true));
                    if (msgRegistration != null) {
                        SOAPMessage retSOAPMessage = null;
                        try {
                            if(FSServiceUtils.isSigningOn()) {
                                int minorVersion =
                                   regisRequest.getMinorVersion(); 
                                if (minorVersion == 
                                    IFSConstants.FF_11_PROTOCOL_MINOR_VERSION)
                                {
                                    msgRegistration = signRegistrationRequest(
                                        msgRegistration,
                                        IFSConstants.ID, 
                                        regisRequest.getID()); 
                               } else if(minorVersion == 
                                   IFSConstants.FF_12_PROTOCOL_MINOR_VERSION)
                               {
                                   msgRegistration = signRegistrationRequest(
                                       msgRegistration, 
                                       IFSConstants.REQUEST_ID, 
                                       regisRequest.getRequestID()); 
                               } else { 
                                   FSUtils.debug.message(
                                       "invalid minor version.");
                                   } 
                            }
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message("calling " +
                                    "sendRegistrationMessage");
                            }
                            retSOAPMessage =  instSOAP.sendMessage(
                                msgRegistration, 
                                remoteDescriptor.getSoapEndpoint());
                        } catch (SOAPException e) {
                            FSUtils.debug.error("Error in sending request ",e);
                            returnLocallyAtSource(response, false);
                            return false;
                        } catch (Exception ex) {
                            FSUtils.debug.error("Error in sending request:",ex);
                            returnLocallyAtSource(response, false);
                            return false;
                        }
                        if (retSOAPMessage == null) {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message("sendRegistrationMessage"
                                    + "return response is null");
                            }
                            returnLocallyAtSource(response, false);
                            return false;
                        }
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("calling"
                                + "parseSOAPMessage after return from IDP");
                        }
                        Element elt = instSOAP.parseSOAPMessage(retSOAPMessage);
                        if (FSServiceUtils.isSigningOn() && regisResponse!=null)
                        {
                            if (!verifyResponseSignature(retSOAPMessage,
                                    acctInfo.isRoleIDP())) 
                            {  
                                if (FSUtils.debug.messageEnabled()) {
                                    FSUtils.debug.message("Response "
                                        + "signature verification failed");
                                    FSUtils.debug.message("Name registration" +
                                        " failed in doRemoteRegistration");
                                }
                                returnLocallyAtSource(response, false);
                                return false;
                            }
                        }
                        FSUtils.debug.message(
                            "Response signature verification succeeded");
                        if(elt.getLocalName().equalsIgnoreCase(
                            IFSConstants.NAME_REGISTRATION_RESPONSE))
                        {
                            FSNameRegistrationResponse regisResponse = null;
                            try {            
                                regisResponse = 
                                    new FSNameRegistrationResponse (elt);
                            } catch (SAMLException e){
                                regisResponse = null;
                            }
                            if (regisResponse != null){
                                String responseStatus = 
                                    ((regisResponse.getStatus()).
                                        getStatusCode()).getValue();
                                if (responseStatus.equals(
                                    IFSConstants.SAML_SUCCESS))
                                {
                                    FSUtils.debug.message(
                                        "Name registration Successful");
                                    // do local update
                                    oldAcctKey = (FSAccountFedInfoKey)
                                        returnMap.get("OldAccountKey");
                                    if (oldAcctKey != null) {
                                        if (FSUtils.debug.messageEnabled()) {
                                            FSUtils.debug.message(
                                                "Get OldAcctKet Name : " +
                                                oldAcctKey.getName() +
                                                "\nGet OldAcctKet Qualifier:" +
                                                oldAcctKey.getNameSpace());
                                        }
                                    } else {
                                        FSUtils.debug.message(
                                            "OldAccount Key is null");
                                    }
                                    newAcctKey = (FSAccountFedInfoKey)
                                            returnMap.get("AccountKey");
                                    if (newAcctKey != null)
                                    {
                                        if (FSUtils.debug.messageEnabled()) {
                                            FSUtils.debug.message(
                                                "Get newAcctKey Name : " +
                                                newAcctKey.getName() +
                                                "\nGet newAcctKey Qualifier:" +
                                                newAcctKey.getNameSpace());
                                        }
                                    } else {
                                        FSUtils.debug.message(
                                            "newAcctKey Key is null");
                                    }
                                    newAcctInfo = (FSAccountFedInfo)
                                        returnMap.get("AccountInfo");
                                    userID = (String)returnMap.get("userID");
                                    regisSource = (String)
                                        returnMap.get("RegisSource");
                                    returnURL = (String) returnMap.get(
                                        IFSConstants.LRURL);
                                    boolean bStatus = doCommonRegistration();
                                    if (FSUtils.debug.messageEnabled()) {
                                        FSUtils.debug.message(
                                            "doCommonRegistration returns " +
                                            bStatus);
                                    }
                                    // Call SP Adapter
                                    if (bStatus && hostedProviderRole != null &&
                                        hostedProviderRole.equalsIgnoreCase(
                                            IFSConstants.SP)) 
                                    {
                                        FSUtils.debug.message("doRemoteRegis");
                                        callPostRegisterNameIdentifierSuccess(
                                           request, response, userID,
                                           regisRequest, regisResponse,
                                           IFSConstants.NAME_REGISTRATION_SP_SOAP_PROFILE);
                                    }
                                    returnLocallyAtSource(
                                        response, bStatus);
                                    return bStatus;
                                } else if (responseStatus.equals(
                                    IFSConstants.FEDERATION_NOT_EXISTS_STATUS))
                                {
                                    if (FSUtils.debug.messageEnabled()) {
                                        FSUtils.debug.message("Name " +
                                            "registration Failed. " +
                                            "Federation does not exist");
                                    }
                                    returnLocallyAtSource(
                                        response, false);
                                    return false; 
                                } else if (responseStatus.equals (
                                    IFSConstants.REGISTRATION_FAILURE_STATUS))
                                {
                                    FSUtils.debug.message(
                                            "Name registration Failed.");
                                    returnLocallyAtSource(
                                        response, false);
                                    return false;
                                }
                            }
                        }                                        
                    }
                }
                returnLocallyAtSource(response, false);
                return false;
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSNameRegistrationHandler::doRemoteRegistration " +
                        "In Redirect profile");
                }
                // addition of relay state
                FSNameIdentifierHelper nameHelper =
                    new FSNameIdentifierHelper(hostedConfig);
                String newId = nameHelper.createNameIdentifier();
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Registration Id : "  + newId);
                }
                regisRequest.setRelayState (newId);
                // add information to FSRegistrationMap        
                HashMap valMap = new HashMap();
                valMap.put("SSODetails", regisMap);
                valMap.put("ReturnEntry", returnMap);
                if (returnURL != null) {
                    valMap.put(IFSConstants.LRURL, returnURL);
                }
                FSRegistrationManager registInst = 
                        FSRegistrationManager.getInstance(metaAlias);
                registInst.setRegistrationMapInfo(newId, valMap);

                // sat1 add null checks 
                Set ketSet = valMap.keySet();
                Iterator iter = ketSet.iterator();
                String key = null;
                String value = null;                   
                while (iter.hasNext()) {                        
                    key = (String)iter.next();                        
                    value = (String)regisMap.get(key);
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("Putting in Map Key : "  + key +
                            "\nPutting in Map Value : "  + value);
                    }
                }
                String urlEncodedRequest = 
                    regisRequest.toURLEncodedQueryString();
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
                            FSUtils.bundle.getString(
                                IFSConstants.NO_CERT_ALIAS));
                    }
                    urlEncodedRequest =
                        FSSignatureUtil.signAndReturnQueryString(
                                urlEncodedRequest, certAlias);
                }
                StringBuffer redirectURL = new StringBuffer();                
                String retURL = remoteDescriptor.
                    getRegisterNameIdentifierServiceURL();
                redirectURL.append(retURL);
                if (retURL.indexOf(IFSConstants.QUESTION_MARK) == -1) {
                    redirectURL.append(IFSConstants.QUESTION_MARK);
                } else {
                    redirectURL.append(IFSConstants.AMPERSAND);
                }
                redirectURL.append(urlEncodedRequest);
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Request to be sent : " + 
                                          redirectURL.toString());
                }                                
                // end of addition                               
                response.sendRedirect(redirectURL.toString());
                return true;
            }
        } catch (IOException e) {
            FSUtils.debug.error("FSNameRegistrationHandler" + 
                FSUtils.bundle.getString(
                    IFSConstants.FEDERATION_REDIRECT_FAILED));
        } catch (FSMsgException e) {
            FSUtils.debug.error(
                "FSNameRegistrationHandler::doRemoteRegistration " + 
                FSUtils.bundle.getString(
                    IFSConstants.TERMINATION_FAILED_SEND_REMOTE));            
        } catch (SAMLResponderException e) {
            FSUtils.debug.error(
                "FSNameRegistrationHandler::doRemoteRegistration " + 
                FSUtils.bundle.getString(
                    IFSConstants.TERMINATION_FAILED_SEND_REMOTE));            
        }    
        returnLocallyAtSource(response, false);
        return false;    
    }
    
      
    
    /**
     * Processes the registration request received from a
     * remote provider. Invoded when Http redirect profile is used.
     * @param request HTTP request
     * @param response HTTP response
     * @param regisRequest the name registration request received from 
     * remote provider
     */
    public void processRegistrationRequest(
        HttpServletRequest request, 
        HttpServletResponse response,
        FSNameRegistrationRequest regisRequest) 
    {        
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("Entered FSNameRegistrationHandler::" +
                "processRegistrationRequest...");
        }
        this.request = request;
        this.response = response;
        this.regisRequest = regisRequest;                
        this.relayState = regisRequest.getRelayState();
        setRegistrationURL();
                        
        if (managerInst == null) {
            FSUtils.debug.error(
                "FSNameRegistrationHandler " + 
                FSUtils.bundle.getString(
                    IFSConstants.FEDERATION_FAILED_ACCOUNT_INSTANCE));
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSNameRegistrationHandler::handleNameRegistration" +
                    "failed to get Account Manager instance");
            }
            sendRegistrationResponse();
            return;
        }
        boolean bUserStatus = setUserDN(regisRequest);                 
        if (!bUserStatus) {            
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Failed to get UserDN. Invalid " +
                    "Name registration request");
            }
            sendRegistrationResponse();
            return;
        }                                    
        boolean retStatus = doCommonRegistration();    
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("doCommonRegistration returns " + retStatus);
        }
        if (retStatus) {
            StatusCode statusCode; 
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSNameRegistrationHandler::handleNameRegistration" 
                    + "registration in DS completed successfully");
            }

            try {        
                statusCode = new StatusCode(IFSConstants.SAML_SUCCESS);
                regisResponse = new FSNameRegistrationResponse(
                    null, 
                    regisRequest.getRequestID(),
                    new Status(statusCode),
                    hostedEntityId,
                    relayState);
                regisResponse.setMinorVersion(regisRequest.getMinorVersion());
                // Call SP Adapter for SP/IDP initiated SOAP profile
                if (hostedProviderRole != null &&
                    hostedProviderRole.equalsIgnoreCase(IFSConstants.SP)) 
                {
                    FSUtils.debug.message("processRegistration IDP/HTTP");
                    callPostRegisterNameIdentifierSuccess(
                        request, response, userID, regisRequest, regisResponse,
                        IFSConstants.NAME_REGISTRATION_IDP_HTTP_PROFILE);
                }
            } catch (FSMsgException e) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSNameRegistrationHandler::" +
                        "failed to create registration response", e);
                }
            } catch (SAMLException e) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSNameRegistrationHandler::" +
                        "failed to create registration response", e);
                }                                
            }        
        } else { // retStatus is false
            StatusCode statusCode; 
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSNameRegistrationHandler::handleNameRegistration" 
                    + "registration in DS failed");
            }
            
            try { 
                statusCode = new StatusCode(
                    IFSConstants.REGISTRATION_FAILURE_STATUS);
                regisResponse = new FSNameRegistrationResponse(
                    null, 
                    regisRequest.getRequestID(),
                    new Status(statusCode),
                    hostedEntityId,
                    relayState);
                regisResponse.setMinorVersion(regisRequest.getMinorVersion());
            } catch (FSMsgException e) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSNameRegistrationHandler::" +
                        "failed to create registration response", e);
                }
            } catch (SAMLException e) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSNameRegistrationHandler::" +
                        "failed to create registration response, e");
                }
            }                
        }        
        sendRegistrationResponse();                
        return;                   
    }
    
    /**
     * Processes the name registration request received from a
     * remote provider. Invoded when SOAP profile is used.
     * @param regisRequest the name registration request received from 
     *  remote provider
     */
    public FSNameRegistrationResponse processSOAPRegistrationRequest(
        HttpServletRequest request,
        HttpServletResponse response,
        FSNameRegistrationRequest regisRequest) 
    {
        relayState = regisRequest.getRelayState();
        try {
            boolean regisSucceed = false;
            FSNameRegistrationResponse regisResponse = null;
            StatusCode statusCode;
            FSUtils.debug.message(
            "Entered FSNameRegistrationHandler::processRegistrationRequest");
            if (managerInst == null) {
                FSUtils.debug.error(
                "FSNameRegistrationHandler Account Manager instance is null");
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSNameRegistrationHandler::handleNameRegistration" +
                        "failed to get Account Manager instance");
                }            
                statusCode = new StatusCode(
                    IFSConstants.REGISTRATION_FAILURE_STATUS);
                try {
                    regisResponse = new FSNameRegistrationResponse(
                        null,
                        regisRequest.getRequestID(),
                        new Status(statusCode),
                        hostedEntityId,
                        relayState);
                } catch (FSMsgException e) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSNameRegistrationHandler::" 
                            + "failed to create registration response");
                    }
                    return null;
                }
                regisResponse.setID(IFSConstants.REGISTRATIONID);
                regisResponse.setMinorVersion(regisRequest.getMinorVersion());
                return regisResponse;
            }

            FSUtils.debug.message(
                "Begin processRegistrationRequest SOAP profile...");
            if (regisRequest != null) {
                boolean bUserStatus = setUserDN(regisRequest);                 
                if (bUserStatus) {
                    boolean retStatus = doCommonRegistration();
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("doCommonRegistration returns " +
                            retStatus);        
                    }
                    if (retStatus) {                                         
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSNameRegistrationHandler::handleNameRegistra"
                                + "tion:registration in DS completed "
                                + "successfully");
                        }                                

                        try {
                            statusCode = new StatusCode(
                                IFSConstants.SAML_SUCCESS);
                            regisResponse = new FSNameRegistrationResponse(
                                null, 
                                regisRequest.getRequestID(),
                                new Status(statusCode),
                                hostedEntityId,
                                relayState);
                             regisSucceed = true;
                        } catch (FSMsgException e) {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message(
                                    "FSNameRegistrationHandler::" +
                                    "failed to create registration response");
                            }
                            return null;
                        } catch (SAMLException ex) {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message(
                                    "FSNameRegistrationHandler::" +
                                    "failed to create registration response");
                            }
                            return null;
                        }
                    } else {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSNameRegistrationHandler::handleNameRegis" +
                                "tration: registration in DS failed");
                        }

                        try {        
                            statusCode = new StatusCode(
                                IFSConstants.REGISTRATION_FAILURE_STATUS);
                            regisResponse = new FSNameRegistrationResponse(
                                null,
                                regisRequest.getRequestID(),
                                new Status(statusCode),
                                hostedEntityId,
                                relayState);
                        } catch (FSMsgException e) {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message(
                                    "FSNameRegistrationHandler::" +
                                    "failed to create registration response");
                            }
                            return null;
                        } catch (SAMLException ex) {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message(
                                    "FSNameRegistrationHandler::" +
                                    "failed to create registration response");
                            }
                            return null;
                        }                
                    }        
                } else {
                    FSUtils.debug.message(
                        "Failed to get UserDN. Invalid registration request");
                    try {        
                        statusCode = new StatusCode(
                            IFSConstants.FEDERATION_NOT_EXISTS_STATUS);
                        regisResponse = new FSNameRegistrationResponse(
                            null, 
                            regisRequest.getRequestID(),
                            new Status(statusCode),
                            hostedEntityId,
                            relayState);
                    } catch (FSMsgException e) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSNameRegistrationHandler::" +
                                "failed to create registration response");
                        }
                        return null;
                    } catch (SAMLException ex) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSNameRegistrationHandler::" +
                                "failed to create registration response");
                        }
                        return null;
                    }                                
                }
            } else {
                FSUtils.debug.error(
                    "FSNameRegistrationHandler::processRegistrationRequest " + 
                    "name registration request is improper");                
                return null;
            }
            regisResponse.setID(IFSConstants.REGISTRATIONID);
            regisResponse.setMinorVersion(regisRequest.getMinorVersion());
            if (regisSucceed && hostedProviderRole != null &&
                hostedProviderRole.equalsIgnoreCase(IFSConstants.SP)) 
            {
                callPostRegisterNameIdentifierSuccess(request, response,
                    userID, regisRequest, regisResponse,
                    IFSConstants.NAME_REGISTRATION_IDP_SOAP_PROFILE);
            }
            return regisResponse;  
        } catch (SAMLException e){
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSNameRegistrationHandler::SAMLException", e);
            }
            return null;
        }  
    }   
    
    
    /**
     * Redirects to final destination after registration. Invoked at the end of
     * the registration operation at the end where registration was initiated.
     * The isSuccess determines if a success message or a failure message is 
     * displayed.
     * @param response <code>HttpServletResponse</code> object
     * @param isSuccess determines the content of the registration-done.jsp
     */
    public void returnLocallyAtSource(
        HttpServletResponse response, boolean isSuccess) 
    {    
        if (regisSource.equals(IFSConstants.REGIS_FEDERATION)) {        
            try {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("redirecting to Resource : " +
                        returnURL);
                }
                response.sendRedirect(returnURL);
            } catch (IOException e) {
                FSUtils.debug.error("Error when redirecting back to resource");
                return;
            }                        
        } else if (regisSource.equals(IFSConstants.REGIS_SSO)) {        
            StringBuffer ssoURL = new StringBuffer();
            ssoURL.append(FSServiceUtils.getBaseURL(request))
                .append(IFSConstants.SSO_URL)
                .append("/")
                .append(IFSConstants.META_ALIAS)
                .append(FSServiceUtils.getMetaAlias(request))
                .append(IFSConstants.QUESTION_MARK);
            // sat1 add null checks 
            Set ketSet = regisMap.keySet();
            Iterator iter = ketSet.iterator();
            String key = null;
            String value = null;                   
            while (iter.hasNext()) {                        
                key = (String)iter.next();                        
                value = (String)regisMap.get(key);
                ssoURL.append(key)
                    .append(IFSConstants.EQUAL_TO)
                    .append(URLEncDec.encode(value))
                    .append(IFSConstants.AMPERSAND);
            }
            ssoURL.append(IFSConstants.AUTHN_INDICATOR_PARAM)
                .append(IFSConstants.EQUAL_TO)
                .append(IFSConstants.TRUE)
                .append(IFSConstants.AMPERSAND)
                .append(IFSConstants.NAMEREGIS_INDICATOR_PARAM)
                .append(IFSConstants.EQUAL_TO)
                .append(IFSConstants.TRUE);
            try {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("redirecting to SSO : " +
                        ssoURL.toString());
                }
                response.sendRedirect(ssoURL.toString());
            } catch (IOException e) {
                FSUtils.debug.error(
                    "Error when redirecting back to SSO service", e);
                return;
            }
        } else if (regisSource.equals(IFSConstants.REGIS_LINK)) {
            try {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "Entered  returnLocallyAtSource with isSuccess set to "
                        + isSuccess);
                }

                StringBuffer finalReturnURL = new StringBuffer();
                finalReturnURL.append(REGISTRATION_DONE_URL);
                char delimiter;
                if (REGISTRATION_DONE_URL.indexOf(IFSConstants.QUESTION_MARK)
                    < 0)
                {
                    delimiter = IFSConstants.QUESTION_MARK;
                } else {
                    delimiter = IFSConstants.AMPERSAND;
                }
                finalReturnURL.append(delimiter)
                    .append(IFSConstants.REGISTRATION_STATUS)
                    .append(IFSConstants.EQUAL_TO);
                if (isSuccess) {
                    finalReturnURL.append(IFSConstants.REGISTRATION_SUCCESS);
                } else {
                    finalReturnURL.append(IFSConstants.REGISTRATION_FAILURE);
                }
                response.sendRedirect(finalReturnURL.toString());
            } catch(IOException e) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "Exception in  returnLocallyAtSource:" , e);
                }
            }
        }
        return;                
    }

    /** 
     * Signs the Name registration request before sending it to the IDP.
     * @param msg the request message to be sent to IDP
     * @param idAttrName name of the id attribute to be signed
     * @param id the value of the id attribute to be signed
     * @return signed Name registration request
     * @exception SAMLException, FSMsgException if error occurred.
     */
    protected SOAPMessage signRegistrationRequest(
        SOAPMessage msg, String idAttrName, String id) 
        throws SAMLException, FSMsgException
    {
        FSUtils.debug.message(
            "Entered FSNameRegistrationHandler::signRegistrationRequest");
        String certAlias = IDFFMetaUtils.getFirstAttributeValueFromConfig(
            hostedConfig, IFSConstants.SIGNING_CERT_ALIAS);
        if (certAlias == null || certAlias.length() == 0) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSNameRegistrationHandler::" +
                    "signRegistrationRequest: couldn't obtain "
                    + "this site's cert alias.");
            }
            throw new SAMLResponderException(
                FSUtils.bundle.getString(IFSConstants.NO_CERT_ALIAS));
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSNameRegistrationHandler.signRegistration"
                + "Request Provider's certAlias is found: " + certAlias);
        }
        XMLSignatureManager manager = XMLSignatureManager.getInstance();
        Document doc = (Document)FSServiceUtils.createSOAPDOM(msg);
        String xpath = "//*[local-name()=\'ProviderID\']";
        manager.signXML(doc, 
                        certAlias, 
                        SystemConfigurationUtil.getProperty(
                            SAMLConstants.XMLSIG_ALGORITHM),
                        idAttrName,
                        id,
                        false,
                        xpath);     
        return FSServiceUtils.convertDOMToSOAP(doc);
    }
    
    
    
    /** 
     * Verifies the Name registration response received
     * from the IDP before processing locally.
     * @param msg the response message
     * @param isIDP whether the remote provider is an IDP or not
     * @return <code>true</code> if signature is valid; <code>false</code>
     *  otherwise.
     */
    protected boolean verifyResponseSignature(
        SOAPMessage msg, boolean isIDP){
        FSUtils.debug.message(
            "Entered FSNameRegistrationHandler::verifyResponseSignature");
        try{
            X509Certificate cert = KeyUtil.getVerificationCert(
                remoteDescriptor, remoteEntityId, isIDP);
            if (cert == null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Registration.verifyResponseSignature"
                        + "couldn't obtain this site's cert .");
                }
                throw new SAMLResponderException(
                    FSUtils.bundle.getString(IFSConstants.NO_CERT));
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("NameRegistration.verifyResponseSignature"
                    + ": Provider's cert is found.");
            }
            XMLSignatureManager manager = XMLSignatureManager.getInstance();
            Document doc = (Document)FSServiceUtils.createSOAPDOM(msg);
            return manager.verifyXMLSignature(doc, cert);
        } catch (SAMLException e){
            FSUtils.debug.error("Error in verifying response ", e);
            return false;
        }
    }

    /**
     * Generates the Name Registration request.
     * @return FSNameRegistrationRequest
     */
    private FSNameRegistrationRequest createNameRegistrationRequest(
        FSAccountFedInfo acctInfo)
    {        
        try {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Entered FSNameRegistrationHandler:: " +
                    "createNameRegistrationRequest");
            }

            FSNameRegistrationRequest reqName = new FSNameRegistrationRequest();
            if (reqName != null)
            {
                reqName.setProviderId(hostedEntityId);
                if (acctInfo.isRoleIDP()) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "calling of generateNameIdentifier Role : IdP ? " +
                            acctInfo.isRoleIDP());
                    }
                    NameIdentifier nameIdentifier = generateNameIdentifier();
                    if (acctInfo.getAffiliation()) {
                        String affiliationID = FSServiceUtils.getAffiliationID(
                            realm, remoteEntityId);
                        if (affiliationID != null) {
                            nameIdentifier.setNameQualifier(affiliationID);
                        }
                    } else {
                       nameIdentifier.setNameQualifier(hostedEntityId);
                    }
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "out of generateNameIdentifier ****" + 
                            "\nNew SP nameIdentifier Qualifier: " +
                            nameIdentifier.getNameQualifier() +
                            "\nNew SP nameIdentifier Name :" +
                            nameIdentifier.getName());
                    }

                    SPProvidedNameIdentifier newNameIdenifier = 
                        new SPProvidedNameIdentifier(nameIdentifier.getName(), 
                            nameIdentifier.getNameQualifier(),
                            nameIdentifier.getFormat());

                    NameIdentifier remoteIdentifier = 
                        acctInfo.getRemoteNameIdentifier();
                    NameIdentifier localIdentifier = 
                        acctInfo.getLocalNameIdentifier();
                    reqName.setIDPProvidedNameIdentifier(
                        new IDPProvidedNameIdentifier(
                            remoteIdentifier.getName(), 
                            remoteIdentifier.getNameQualifier(),
                            remoteIdentifier.getFormat()));
                    reqName.setSPProvidedNameIdentifier(newNameIdenifier);
                    if (localIdentifier != null) {
                        reqName.setOldProvidedNameIdentifier(
                            new OldProvidedNameIdentifier(
                                localIdentifier.getName(), 
                                localIdentifier.getNameQualifier(),
                                localIdentifier.getFormat()));
                        try {
                            oldAcctKey = new FSAccountFedInfoKey(
                                localIdentifier.getNameQualifier(), 
                                localIdentifier.getName());
                        } catch (FSAccountMgmtException e){
                            oldAcctKey = null;
                        }
                    } else  {
                        // when Service Provider sends the name reg. request 
                        // for the first time, OldProvidedNameIdentifier is
                        // same as the IDPProvidedNameIdentifier as per the spec
                        reqName.setOldProvidedNameIdentifier( 
                            new OldProvidedNameIdentifier ( 
                                remoteIdentifier.getName(), 
                                remoteIdentifier.getNameQualifier(),
                                remoteIdentifier.getFormat()));
                        try {
                            oldAcctKey = new FSAccountFedInfoKey(
                            remoteIdentifier.getNameQualifier(), 
                            remoteIdentifier.getName());
                        } catch (FSAccountMgmtException e){
                            oldAcctKey = null;
                        }
                    }

                    try {
                        FSAccountFedInfoKey tmpKey = new FSAccountFedInfoKey(
                            nameIdentifier.getNameQualifier(), 
                            nameIdentifier.getName());
                        FSAccountFedInfo tmpInfo = new FSAccountFedInfo(
                            remoteEntityId,
                            newNameIdenifier,
                            remoteIdentifier, 
                            acctInfo.isRoleIDP());
                        tmpInfo.setAffiliation(acctInfo.getAffiliation());

                        returnMap.put("userID", userID);
                        returnMap.put("OldAccountKey", oldAcctKey);
                        if (oldAcctKey != null) {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message("Get OldAcctKet Name : " +
                                    oldAcctKey.getName() +
                                    "\nGet OldAcctKet Qualifier : "+
                                    oldAcctKey.getNameSpace());
                            }
                        } else {
                            FSUtils.debug.message("OldAccount Key is null");
                        }
                        returnMap.put("AccountKey", tmpKey);
                        returnMap.put("AccountInfo", tmpInfo);
                        returnMap.put("RegisSource", regisSource);
                        returnMap.put(IFSConstants.LRURL, returnURL);
                    } catch (FSAccountMgmtException e){
                        return null;
                    }
                } else {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("calling of generateNameIdenti" +
                            "fier Role : IdP ? " + acctInfo.isRoleIDP());
                    }
                    NameIdentifier nameIdentifier = generateNameIdentifier();
                    if (acctInfo.getAffiliation()) {
                        String affiliationID = FSServiceUtils.getAffiliationID(
                            realm, remoteEntityId);
                        if (affiliationID != null) {
                            nameIdentifier.setNameQualifier(affiliationID);
                        }
                    }

                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "New IDP nameIdentifier Name : " +
                            nameIdentifier.getName() +
                            "\nNew IDP nameIdentifier Qualifier :"+
                            nameIdentifier.getNameQualifier() +
                            "out of generateNameIdentifier*****");
                    }

                    IDPProvidedNameIdentifier newNameIdenifier =
                        new IDPProvidedNameIdentifier(
                            nameIdentifier.getName(), 
                            nameIdentifier.getNameQualifier(), 
                            nameIdentifier.getFormat());

                    NameIdentifier remoteIdentifier = 
                        acctInfo.getRemoteNameIdentifier(); // SP
                    NameIdentifier localIdentifier = 
                        acctInfo.getLocalNameIdentifier(); // IdP

                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "Old IDP nameIdentifier Name : " + 
                            localIdentifier.getName() +
                            "\nOld IDP nameIdentifier Qualifier :"+
                            localIdentifier.getNameQualifier());
                    }
                    FSUtils.debug.message("To set OldProvidedNameIdentifier");
                    reqName.setOldProvidedNameIdentifier(
                        new OldProvidedNameIdentifier(
                            localIdentifier.getName(), 
                            localIdentifier.getNameQualifier(),
                            localIdentifier.getFormat()));
                    FSUtils.debug.message("To set IdpProvidedNameIdentifier");
                    reqName.setIDPProvidedNameIdentifier(newNameIdenifier);
                    if (remoteIdentifier != null) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "SP nameIdentifier Name : " + 
                                remoteIdentifier.getName() +
                                "SP nameIdentifier Qualifier :" +
                                remoteIdentifier.getNameQualifier());
                        }
                        SPProvidedNameIdentifier spNameIdentifier = 
                            new SPProvidedNameIdentifier (
                                remoteIdentifier.getName(), 
                                remoteIdentifier.getNameQualifier(),
                                remoteIdentifier.getFormat());
                        reqName.setSPProvidedNameIdentifier(spNameIdentifier);
                    }
                    try {
                        oldAcctKey = new FSAccountFedInfoKey(hostedEntityId, 
                                        localIdentifier.getName());
                        FSAccountFedInfoKey tmpKey = new FSAccountFedInfoKey(
                            nameIdentifier.getNameQualifier(), 
                            nameIdentifier.getName());
                        FSAccountFedInfo tmpInfo = new FSAccountFedInfo(
                            remoteEntityId,
                            newNameIdenifier,
                            remoteIdentifier, 
                            acctInfo.isRoleIDP());
                        returnMap.put("userID", userID);
                        returnMap.put("OldAccountKey", oldAcctKey);
                        returnMap.put("AccountKey", tmpKey);
                        returnMap.put("AccountInfo", tmpInfo);
                        returnMap.put("RegisSource", regisSource);
                        returnMap.put(IFSConstants.LRURL, returnURL);
                        if (oldAcctKey != null) {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message(
                                    "Get OldAcctKet Name : " +
                                    oldAcctKey.getName() +
                                    "\nGet OldAcctKet Qualifier: "+
                                    oldAcctKey.getNameSpace());
                            }
                        } else {
                            FSUtils.debug.message("OldAccount Key is null");
                        }
                    } catch (FSAccountMgmtException e){
                        return null;
                    }        
                }
                reqName.setMinorVersion(FSServiceUtils.getMinorVersion(
                    remoteDescriptor.getProtocolSupportEnumeration()));
                return reqName;
            }
        } catch (SAMLException e){
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("got SAMLException:", e);
            }
        }
        return null;
    }

    
    /**
     * Performs the operations on the users federated account at both the SP 
     * side, IDP side. The input parameters are generated
     * at SP side and at the IDP side it is retrieved from the request received.
     * @return <code>true</code> if the operation succeeded; <code>false</code>
     *  otherwise.
     */
    private boolean doCommonRegistration() {
        try{
            // Get userID
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("user id is "+ userID);
                FSUtils.debug.message("To write account fed info to DS");
            }
            if (oldAcctKey != null)        
            {                                                        
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Old Account Key : " + oldAcctKey);
                }
                String oldNameIDValue = oldAcctKey.getName();
                FSAccountFedInfo oldInfo = managerInst.readAccountFedInfo(
                    userID, remoteEntityId, oldNameIDValue);
                if (oldInfo != null) {
                    managerInst.removeAccountFedInfo(userID, oldInfo);
                }
                managerInst.writeAccountFedInfo(
                    userID, newAcctKey, newAcctInfo, oldAcctKey);
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Old Account Key : " + oldAcctKey);
                }
                if (acctInfo != null) {
                    managerInst.removeAccountFedInfo(userID, acctInfo);
                }
                managerInst.writeAccountFedInfo(
                    userID, newAcctKey, newAcctInfo);
            }
            if ((ssoToken != null) && 
                (hostedProviderRole.equalsIgnoreCase(IFSConstants.SP)) )
            {
                FSSessionManager sessManager =
                    FSSessionManager.getInstance(metaAlias);
                FSSession ssoSession = sessManager.getSession(ssoToken);
                if (ssoSession != null) {
                    ssoSession.setAccountFedInfo(newAcctInfo);
                }
            }
            return true;                                    
        } catch(FSAccountMgmtException e){
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Error when writing user information:",e);
            }                        
            return false;
        }
    }
    

    /**
     * Generates the <code>SPProvidedNameIdentifier</code> that will be 
     * communicated to the IDP. The IDP will in all future communication use 
     * this Name Identifier instead of the
     * <code>IDPProvidedNameIdentifier</code>.
     * @return Service Provider generated Name identifier    
     */
    private NameIdentifier generateNameIdentifier() {
        try {
            FSUtils.debug.message(
                "Entered FSNameRegistrationHandler::generateNameIdentifier");
            NameIdentifier nameIdentifier;
            FSNameIdentifierHelper nameHelper = 
                new FSNameIdentifierHelper(hostedConfig);
            String handleName = nameHelper.createNameIdentifier();
            if (handleName == null || handleName.trim().length() < 1) {
                FSUtils.debug.error("FSNameIdentifierHelper::createNameIdentif"
                    + "ier returned null");                        
                return null;        
            } else {
                FSUtils.debug.message("To set nameIdentifier");
                nameIdentifier = new NameIdentifier(handleName, remoteEntityId);
                nameIdentifier.setFormat(IFSConstants.NI_FEDERATED_FORMAT_URI);
                FSUtils.debug.message("completed set nameIdentifier");
                return nameIdentifier;
            }                        
        } catch (SAMLException e){
            String[] data = { FSUtils.bundle.getString(
                IFSConstants.REGISTRATION_FAILED_SP_NAME_IDENTIFIER)};
            LogUtil.error(Level.INFO,"REGISTRATION_FAILED_SP_NAME_IDENTIFIER",
                data, ssoToken);
            return null;
        }
    }

    private void sendRegistrationResponse()                
    {
        StringBuffer redirectURL = new StringBuffer();
        String retURL = 
            remoteDescriptor.getRegisterNameIdentifierServiceReturnURL();
        redirectURL.append(retURL);                        
        if (regisResponse != null) {
            String urlEncodedRequest = null;
            try {
                urlEncodedRequest = regisResponse.toURLEncodedQueryString(); 
            } catch (FSMsgException e){
                urlEncodedRequest = null;
            }                                       
            // Sign the request querystring
            if (urlEncodedRequest != null){
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
                        urlEncodedRequest = null;
                    }
                    if (urlEncodedRequest != null) {
                        urlEncodedRequest = 
                            FSSignatureUtil.signAndReturnQueryString(
                                urlEncodedRequest, certAlias);
                    }
                }
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("URLEncodedRequest to be sent : " + 
                        urlEncodedRequest);
                }
                if (urlEncodedRequest != null) {
                    if ((redirectURL.toString()).indexOf(
                        IFSConstants.QUESTION_MARK) == -1)
                    {
                        redirectURL.append(IFSConstants.QUESTION_MARK);
                    } else {
                        redirectURL.append(IFSConstants.AMPERSAND);
                    }
                    redirectURL.append(urlEncodedRequest);
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "FSNameRegistrationHandler::Redirect URL is " + 
                            redirectURL.toString());
                    }
                }
            }
        }

        try {
            response.sendRedirect(redirectURL.toString());
            return;
        } catch (IOException e) {
            FSUtils.debug.error("Error in sending registration response");
            return;
        }                
    }

    private void callPostRegisterNameIdentifierSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        String userID,
        FSNameRegistrationRequest regRequest,
        FSNameRegistrationResponse regResponse,
        String regProfile) 
    {
        FederationSPAdapter spAdapter =
            FSServiceUtils.getSPAdapter(hostedEntityId, hostedConfig);
        if (spAdapter != null) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("processRegResponse, " +
                    "call postRegisterNameIdentifier success");
            }
            try {
                spAdapter.postRegisterNameIdentifierSuccess(
                    hostedEntityId,
                    request, response, userID, regRequest,
                    regResponse, regProfile);
            } catch (Exception e) {
                // ignore adapter exception
                FSUtils.debug.error("postRegisterNameIdentifierSuccess." +
                    regProfile, e);
            }
        }
    }
}
