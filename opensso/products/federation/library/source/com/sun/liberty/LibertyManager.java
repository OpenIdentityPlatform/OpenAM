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
 * $Id: LibertyManager.java,v 1.7 2008/06/25 05:48:17 qcheng Exp $
 *
 */


package com.sun.liberty;

import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import javax.xml.soap.SOAPMessage;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.URLEncDec;

import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.cot.CircleOfTrustDescriptor;
import com.sun.identity.cot.COTException;
import com.sun.identity.cot.COTConstants;

import com.sun.identity.federation.accountmgmt.FSAccountFedInfo;
import com.sun.identity.federation.accountmgmt.FSAccountManager;
import com.sun.identity.federation.accountmgmt.FSAccountMgmtException;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.federation.message.FSNameIdentifierMappingRequest;
import com.sun.identity.federation.message.FSNameIdentifierMappingResponse;
import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.services.FSLoginHelper;
import com.sun.identity.federation.services.FSLoginHelperException;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.federation.services.FSSessionManager;
import com.sun.identity.federation.services.FSSession;
import com.sun.identity.federation.services.FSSOAPService;
import com.sun.identity.federation.services.FSSessionPartner;
import com.sun.identity.federation.services.namemapping.FSNameMappingHandler;
import com.sun.identity.liberty.ws.meta.jaxb.IDPDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLException;
import org.w3c.dom.*;


/**
 * <code>LibertyManager</code> forms the basis of the Public APIs. It has all
 * the methods which the JSPs etc.  need to use for
 * federation/termination/logout etc. 
 * @supported.all.api
 */
public class LibertyManager {
    
    static Debug debug = null;
    private static IDFFMetaManager metaManager = null;
    
    static {
        debug = Debug.getInstance("libIDFF");
        metaManager = FSUtils.getIDFFMetaManager();
    }
    
    /**
     * Returns a list of all trusted Identity Providers under root realm.
     *
     * @return an iterator to a list of strings, each containing the
     *         entity ID of Identity Providers.
     * @deprecated This method has been deprecated. Please use 
     *  <code>getAllIDPList(String realm)</code> instead.
     * @see #getAllIDPList(String)
     */
    public static Iterator getIDPList() {
        return getAllIDPList(IFSConstants.ROOT_REALM);
    }

    /**
     * Returns a list of all trusted Identity Providers under the realm.
     *
     * @param realm The realm under which the entity resides.
     * @return an iterator to a list of strings, each containing the
     *         entity ID of Identity Providers.
     */
    public static Iterator getAllIDPList(String realm) {
        // returns list of idps... for default org.
        // since all the providers have their description under default org..
        // hence returning the List of all the active idps.
        Set idpList = new HashSet();
        try {
            if (metaManager != null) {
                // TODO: check if the idp is active if we decide to support it
                idpList.addAll(
                    metaManager.getAllHostedIdentityProviderIDs(realm));
                idpList.addAll(
                    metaManager.getAllRemoteIdentityProviderIDs(realm));
            }
        } catch (IDFFMetaException ame) {
            debug.error("LibertyManager: getAllIDPList: Error while getting " +
                " Active ProviderIds  ", ame);
        }
        return idpList.iterator();
    }// end of method.
    
    /**
     * Returns a list of all trusted Identity Providers under root realm
     * for a given hosted provider's entity ID.
     *
     * @param hostedEntityID hosted provider's entity ID.
     * @return an iterator to a list of strings, each containing the provider
     *         ID of an trusted Identity Provider for this hosted provider.
     * @deprecated This method is deprecated. Please use
     *  <code>getIDPList(String,String)</code>
     * @see #getIDPList(String,String)
     */
    public static Iterator getIDPList(String hostedEntityID) {
        return getList(IFSConstants.ROOT_REALM, hostedEntityID, 
            IFSConstants.SP, IFSConstants.IDP);
    }

    /**
     * Returns a list of all trusted Identity Providers under the realm 
     * for a given hosted provider's entity ID.
     *
     * @param realm The realm under which the entity resides.
     * @param hostedEntityID hosted provider's entity ID.
     * @return an iterator to a list of strings, each containing the provider
     *         ID of an trusted Identity Provider for this hosted provider.
     */
    public static Iterator getIDPList(String realm, String hostedEntityID) {
        return getList(
            realm, hostedEntityID, IFSConstants.SP, IFSConstants.IDP);
    }
    
    /**
     * Returns a list of all trusted Service Providers under root realm.
     *
     * @return an iterator to a list of strings, each containing the
     *  entity ID of a Service Provider.
     * @deprecated This method is deprecated. Please use 
     *  <code>getAllSPList(String realm)</code>.
     * @see #getAllSPList(String)
     */
    public static Iterator getSPList() {
        return getSPList(IFSConstants.ROOT_REALM);
    }

    /**
     * Returns a list of all trusted Service Providers under the realm.
     *
     * @param realm The realm under which the entity resides.
     * @return an iterator to a list of strings, each containing the
     *  entity ID of a Service Provider.
     */
    public static Iterator getAllSPList(String realm) {
        // returns list of sps... for default org.
        // since all the providers have their description under default org..
        // hence returning the List of all the active sps.
        Set spList = new HashSet();
        try {
            if (metaManager != null) {
                // TODO: check if the sp is active if we decide to support it
                spList.addAll(
                    metaManager.getAllHostedServiceProviderEntities(realm));
                spList.addAll(
                    metaManager.getAllRemoteServiceProviderEntities(realm));
            }
        } catch (IDFFMetaException ame) {
            debug.error("LibertyManager: getAllSPList: Error while getting " +
                " Active ProviderIds  ", ame);
        }
        return spList.iterator();
    }
    
    /**
     * Returns a list of all trusted Service Providers under root realm for this
     * Hosted Provider.
     *
     * @param hostedEntityID hosted provider's entity ID.
     * @return an iterator to a list of strings, each containing the
     *  entity ID of an Service Provider for the given Hosted Provider.
     * @deprecated This method is deprecated. Please use
     *  <code>getSPList(String,String)</code>
     * @see #getSPList(String,String)
     */
    public static Iterator getSPList(String hostedEntityID) {
        return getList(IFSConstants.ROOT_REALM, hostedEntityID, 
            IFSConstants.IDP, IFSConstants.SP);
    }

    /**
     * Returns a list of all trusted Service Providers for this
     * Hosted Provider.
     *
     * @param realm The realm under which the entity resides.
     * @param hostedEntityID hosted provider's entity ID.
     * @return an iterator to a list of strings, each containing the
     *  entity ID of an Service Provider for the given Hosted Provider.
     */
    public static Iterator getSPList(String realm, String hostedEntityID) {
        return getList(
            realm, hostedEntityID, IFSConstants.IDP, IFSConstants.SP);
    }
    
    /**
     * Returns the federation status of a user with an Identity Provider.
     * This method assumes that the user is already federated with the 
     * provider.
     * @param user The user name obtained by calling <code>getUser()</code> on a
     * Liberty-authenticated <code>HttpServletRequest</code> from the user
     * @param remoteEntityId Entity ID of the Remote Identity Provider.
     * @param hostedEntityId Hosted Provider's entity ID.
     * @param hostedProviderRole Hosted Provider's Role.
     * @return The federation status of a user with an Identity Provider.
     * @deprecated This method is deprecated.
     * @see #getIDPFederationStatus(String,String,String,String)
     */
    public static boolean getIDPFederationStatus(
        String user,
        String remoteEntityId,
        String hostedEntityId,
        String hostedProviderRole)
    {
        return getIDPFederationStatus(
            user, IFSConstants.ROOT_REALM, remoteEntityId, 
            hostedEntityId, hostedProviderRole);
    }

    /**
     * Returns the federation status of a user with an Identity Provider.
     * This method assumes that the user is already federated with the 
     * provider.
     * @param user The user name obtained by calling <code>getUser()</code> on a
     * Liberty-authenticated <code>HttpServletRequest</code> from the user
     * @param realm The realm under which the entity resides.
     * @param remoteEntityId Entity ID of the Remote Identity Provider.
     * @param hostedEntityId Hosted Provider's entity ID.
     * @param hostedProviderRole Hosted Provider's Role.
     * @return The federation status of a user with an Identity Provider.
     */
    public static boolean getIDPFederationStatus(
        String user,
        String realm,
        String remoteEntityId,
        String hostedEntityId,
        String hostedProviderRole)
    {
        boolean result = false;
        if (user == null ||
            remoteEntityId == null ||
            hostedEntityId == null ||
            hostedProviderRole == null)
        {
           debug.error("LibertyManager.getIDPFederationStatus:: null input " +
               " parameters.");
           return result;
        }
        try {
            result = FSAccountManager.getInstance(
                getMetaAlias(realm, hostedEntityId, hostedProviderRole)).
                    isFederationActive(user, remoteEntityId);
        } catch (FSAccountMgmtException ame) {
            debug.error("LibertyManager: getIDPFederationStatus: " +
                "Couldnot get Federation Status ", ame);
        }
        return result;
    }
    
    /**
     * Returns the federations status of a user with an Service Provider.
     * This method assumes that the user is already federated with the 
     * provider.
     *
     * @param user The user name obtained by calling
     *  <code>getRemoteUser()</code> on a Liberty-authenticated
     *  <code>HttpServletRequest</code> from the user.
     * @param remoteProviderId The entity ID of the Remote Service Provider.
     * @param hostedProviderId Hosted provider's entity ID.
     * @param hostedProviderRole Hosted Provider Role.
     * @return The federation status of a user with an Service Provider.
     * @deprecated This method is deprecated.
     * @see #getSPFederationStatus(String,String,String,String,String)
     */
    public static boolean getSPFederationStatus(
        String user,
        String remoteProviderId,
        String hostedProviderId,
        String hostedProviderRole)
    {
        return getSPFederationStatus(
            user, IFSConstants.ROOT_REALM, remoteProviderId,
            hostedProviderId, hostedProviderRole);
    }

    /**
     * Returns the federations status of a user with an Service Provider.
     * This method assumes that the user is already federated with the 
     * provider.
     *
     * @param user The user name obtained by calling
     *  <code>getRemoteUser()</code> on a Liberty-authenticated
     *  <code>HttpServletRequest</code> from the user.
     * @param realm The relam under which the entity resides.
     * @param remoteProviderId The entity ID of the Remote Service Provider.
     * @param hostedProviderId Hosted provider's entity ID.
     * @param hostedProviderRole Hosted Provider Role.
     * @return The federation status of a user with an Service Provider.
     */
    public static boolean getSPFederationStatus(
        String user,
        String realm,
        String remoteProviderId,
        String hostedProviderId,
        String hostedProviderRole)
    {
        boolean result = false;
        if (user == null ||
            remoteProviderId == null ||
            hostedProviderId == null ||
            hostedProviderRole == null)
        {
           FSUtils.debug.error("LibertyManager.getSPFederationStatus:: " +
               " null input parameters.");
           return result;
        }
        try {
            result = FSAccountManager.getInstance(
                getMetaAlias(realm, hostedProviderId, hostedProviderRole)).
                    isFederationActive(user, remoteProviderId);
        } catch (FSAccountMgmtException ame) {
            debug.error("LibertyManager: getIDPFederationStatus: " +
                "Couldnot get Federation Status ", ame);
        }
        return result;
    }
    
    /**
     * Returns a nonce for use in forms to be posted to well known servlets.
     * Avoids cross site scripting type attacks.
     *
     * @param user The user obtained by calling
     *  <code>getRemoteUser()</code> on a Liberty-authenticated
     *  <code>HttpServletRequest</code>from the user.
     * @return A string to be put in a hidden form field called "nonce".
     * @deprecated This method has been deprecated. Please use other
     *  means to generate nounce.
     */
    public static String getNonce(String user) {
        Random random = new Random();
        long l = random.nextLong();
        String nonce = String.valueOf(l);
        return nonce;
    }
    
    /**
     * Checks that the given nonce is the same as the last one returned via
     * <code>getNonce()</code>, and invalidates it.
     *
     * @param nonce String containing nonce.
     * @param user User name passed to <code>getNonce</code> to obtain nonce.
     * @deprecated This method has been deprecated. Please use other
     *        means to verify nounce.
     * @return true is <code>nonce</code> is the same as the last one
     *         returned by <code>getNonce</code> method.
     */
    public static boolean checkNonce(String nonce, String user) {
        return true;
    }
    
    /**
     * Returns the ID of the provider discovered via the introduction protocol.
     * If <code>null</code>, no provider was discovered. Can be passed to
     * <code>LoginServlet</code> if <code>null</code>.
     *
     * @param request HTTP servlet request.
     * @return the provider ID
     */
    public static String getIntroducedProvider(HttpServletRequest request) {
        String provider = request.getParameter(IFSConstants.PROVIDER_ID_KEY);
        return provider;
    }
    
    /**
     * The steps for getting the <code>IDPList</code> and <code>SPList</code>
     * are the same (except for a role check). So having this private method
     * which takes in role and does the required function.
     */
    private static Iterator getList(
        String realm,
        String entityID, 
        String providerRole,
        String remoteProviderRole)
    {
        Set trustedProviders = null;
        BaseConfigType providerConfig = IDFFMetaUtils.getExtendedConfig(
            realm, entityID, providerRole, metaManager);
        if (providerConfig != null) {
            trustedProviders = metaManager.getAllTrustedProviders(
                providerConfig.getMetaAlias());
        }
        if (trustedProviders == null) {
            trustedProviders = new HashSet();
        }
        return trustedProviders.iterator();
    }
    
    // From here starts the methods which are outside the publicAPI but are 
    // used by the jsp(Logout/Termination/Federation/CommonLogin...)

    /**
     * Returns the <code>metaAliasKey</code> from <code>IFSConstants</code>.
     *
     * @return the <code>metaAliasKey</code> from <code>IFSConstants</code>.
     */
    public static String getMetaAliasKey() {
        return IFSConstants.META_ALIAS;
    }
    
    /** 
     * Returns the termination <code>providerIDKey</code> from
     * <code>IFSConstants</code>.
     *
     * @return the termination <code>providerIDKey</code> from
     *  <code>IFSConstants</code>.
     */
    public static String getTerminationProviderIDKey() {
        return IFSConstants.TERMINATION_PROVIDER_ID;
    }
    
    /** 
     * Returns the <code>requestIDKey</code> from <code>IFSConstants</code>.
     *
     * @return the <code>requestIDKey</code> from <code>IFSConstants</code>.
     */
    public static String getRequestIDKey() {
        return IFSConstants.AUTH_REQUEST_ID;
    }
    
    /**
     * Returns the <code>providerIDKey</code> from <code>IFSConstants</code>.
     *
     * @return the <code>providerIDKey</code> from <code>IFSConstants</code>.
     */
    public static String getProviderIDKey() {
        return IFSConstants.PROVIDER_ID_KEY;
    }
    
    /** 
     * Returns the <code>LRURLKey</code> from <code>IFSConstants</code>.
     *
     * @return the <code>LRURLKey</code> from <code>IFSConstants</code>.
     */
    public static String getLRURLKey() {
        return IFSConstants.LRURL;
    }
    
    /**
     * Returns the <code>COT</code> key from <code>IFSConstants</code>.
     *
     * @return the <code>COT</code> key from <code>IFSConstants</code>.
     */
    public static String getCOTKey() {
        return IFSConstants.COTKEY;
    }
    
    /**
     * Returns the <code>selectedProviderKey</code> from
     * <code>IFSConstants</code>.
     *
     * @return the <code>selectedProviderKey</code> from
     *  <code>IFSConstants</code>.
     */
    public static String getSelectedProviderKey() {
        return IFSConstants.SELECTEDPROVIDER;
    }
    
    /**
     * Returns Federation Error Key.
     *
     * @return Federation Error Key
     */
    public static String getFedErrorKey() {
        return IFSConstants.FEDERROR;
    }
    
    /**
     * Returns <code>FederationRemark</code> Key.
     *
     * @return <code>FederationRemark</code> Key
     */
    public static String getFedRemarkKey() {
        return IFSConstants.FEDREMARK;
    }
    
    /** 
     * Returns the user from <code>HttpServletRequest</code>.
     *
     * @param request HTTP servlet request.
     * @return the user from <code>HttpServletRequest</code>.
     */
    public static String getUser(HttpServletRequest request) {
        Object ssoToken = null;
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            ssoToken = sessionProvider.getSession(request);
            if (ssoToken != null && sessionProvider.isValid(ssoToken)) {
                debug.message("LibertyManager: getUser: token is valid" );
                return sessionProvider.getPrincipalName(ssoToken);
            }
            return null;
        } catch (SessionException ssoe) {
            debug.error("LibertyManager: getUser: SessionException: ", ssoe);
            return null;
        }
    }
    
    /**
     * Returns Provider's <code>HomePageURL</code>.
     *
     * @param providerID Provider's entity ID.
     * @param providerRole Provider Role.
     * @return Provider's <code>HomePageURL</code>.
     * @deprecated This method is deprecated.
     * @see #getHomeURL(String,String,String)
     */
    public static String getHomeURL(String providerID, String providerRole) {
        return getHomeURL(IFSConstants.ROOT_REALM, providerID, providerRole);
    }

    /**
     * Returns Provider's <code>HomePageURL</code>.
     *
     * @param realm The realm under which the entity resides.
     * @param providerID Provider's entity ID.
     * @param providerRole Provider Role.
     * @return Provider's <code>HomePageURL</code>.
     */
    public static String getHomeURL(
        String realm, String providerID, String providerRole) 
    {
        String homeURL = null;
        BaseConfigType config = IDFFMetaUtils.getExtendedConfig(
            realm, providerID, providerRole, metaManager);
        if (config != null) {
            homeURL = IDFFMetaUtils.getFirstAttributeValue(
                IDFFMetaUtils.getAttributes(config),
                    IFSConstants.PROVIDER_HOME_PAGE_URL);
        }
        return homeURL;
    }
    
    /** 
     * Returns <code>PreLoginServlet</code> URL and appends
     * <code>metaAlias</code> to it.
     *
     * @param providerID Provider's entity ID.
     * @param providerRole Provider Role.
     * @param request HTTP servlet request.
     * @return <code>PreLoginServlet</code> URL and appends
     * <code>metaAlias</code> to it.
     * @deprecated This method is deprecated.
     * @see #getPreLoginServletURL(String,String,String,HttpServletRequest)
     */
    public static String getPreLoginServletURL(
        String providerID, 
        String providerRole,
        HttpServletRequest request)
    {
        return getPreLoginServletURL(
            IFSConstants.ROOT_REALM, providerID, providerRole, request);
    }

    /** 
     * Returns <code>PreLoginServlet</code> URL and appends
     * <code>metaAlias</code> to it.
     *
     * @param realm The realm under which the entity resides.
     * @param providerID Provider's entity ID.
     * @param providerRole Provider Role.
     * @param request HTTP servlet request.
     * @return <code>PreLoginServlet</code> URL and appends
     * <code>metaAlias</code> to it.
     */
    public static String getPreLoginServletURL(
        String realm,
        String providerID, 
        String providerRole,
        HttpServletRequest request)
    {
        String metaAlias = getMetaAlias(realm, providerID, providerRole);
        String baseURL = FSServiceUtils.getServicesBaseURL(request);
        return baseURL + IFSConstants.PRE_LOGIN_PAGE + "?" +
            IFSConstants.META_ALIAS + "=" + metaAlias;
    }
        
    /** 
     * Returns the <code>LoginURL</code> from <code>IFSConstants</code>.
     *
     * @param request HTTP servlet request.
     * @return the <code>LoginURL</code> from <code>IFSConstants</code>
     */
    public static String getLoginURL(HttpServletRequest request) {
        String returnURL = FSServiceUtils.getServicesBaseURL(request) +
            IFSConstants.LOGIN_PAGE + 
            "?" + IFSConstants.ARGKEY + "=" + IFSConstants.NEWSESSION;
        if (debug.messageEnabled()) {
            debug.message("LibertyManager: getLoginURL: " + 
                " returnURL = " + returnURL);
        }
        return returnURL;
    }
    
    /**
     * Returns the <code>interSiteURL</code> from <code>IFSConstants</code>.
     *
     * @param request HTTP servlet request.
     * @return the <code>interSiteURL</code> from <code>IFSConstants</code>.
     */
    public static String getInterSiteURL(HttpServletRequest request) {
        String returnURL = FSServiceUtils.getServicesBaseURL(request) +
            "/" + IFSConstants.INTERSITE_URL;
        if (debug.messageEnabled()) {
            debug.message("LibertyManager::getInterSiteURL:: "
                + "returnURL = " + returnURL);
        }
        return returnURL;
    }
    
    
    /** 
     * Returns <code>entityID</code> from the provider Alias
     * using <code>meta manager</code> calls.
     *
     * @param metaAlias The <code>metaAlias</code> of the provider
     * @return <code>entityID</code> corresponding to the 
     *  <code>metaAlias</code>.
     */
    public static String getEntityID(String metaAlias) {
        try {
            if (metaManager == null) {
                debug.error("LibertyManager: getEntityID: meta manager isnull");
                return null;
            }
            return metaManager.getEntityIDByMetaAlias(metaAlias);
        } catch (IDFFMetaException ame) {
            debug.error("LibertyManager: getEntityID: Error getting ID", ame);
            return null;
        }
    } 
    
    /**
     * Returns the list of all Trusted Identity Providers of this user not
     * already federated with.  This is a subset of the Set returned by
     * <code>getIDPList()</code>. This method is used to show the drop-down
     * menu consisting of all the Identity Providers that the user is not
     * already federated with.
     *
     * @param providerID provider's entity ID.
     * @param providerRole provider Role.
     * @param userName name of user.
     * @return Set containing all the Identity Provider IDs which the user is
     *  not already federated with.
     * @deprecated This method is deprecated. Please use 
     *  <code>getProvidersToFederate(String, String, String,String)</code>
     * @see #getProvidersToFederate(String,String,String,String)
     */
    public static Set getProvidersToFederate(
        String providerID, 
        String providerRole,
        String userName) 
    {
        return getProvidersToFederate(
            IFSConstants.ROOT_REALM, providerID, providerRole, userName);
    }

    /**
     * Returns the list of all Trusted Identity Providers of this user not
     * already federated with.  This is a subset of the Set returned by
     * <code>getIDPList()</code>. This method is used to show the drop-down
     * menu consisting of all the Identity Providers that the user is not
     * already federated with.
     *
     * @param realm the realm that the provider resides
     * @param providerID provider's entity ID.
     * @param providerRole provider Role.
     * @param userName name of user.
     * @return Set containing all the Identity Provider IDs which the user is
     *  not already federated with.
     */
    public static Set getProvidersToFederate(
        String realm,
        String providerID, 
        String providerRole,
        String userName) 
    {
        Set unFederatedIDPs = new HashSet();
        if (providerID == null ||
            providerRole == null ||
            userName == null)
        {
            debug.error("LibertyManager.getProvidersToFederate:: null" +
                " parameter values");
            return unFederatedIDPs;
        }

        if (!providerRole.equals(IFSConstants.SP) &&
            !providerRole.equals(IFSConstants.IDP)) 
        {
            debug.error("LibertyManager.getProvidersToFederate:: Invalid" +
                " ProviderRole.");
            return unFederatedIDPs;
        }
        Iterator idpList = getIDPList(realm, providerID);
        Set alreadyFederatedProviders = null;
        try {
            alreadyFederatedProviders = FSAccountManager.getInstance(
                getMetaAlias(realm, providerID, providerRole)).
                    readAllFederatedProviderID(providerID, userName); 
            String idp = null;
            while (idpList.hasNext()) {
                idp = (String) idpList.next();
                if (!alreadyFederatedProviders.contains(idp)) {
                    unFederatedIDPs.add(idp);
                }
            }
        } catch (FSAccountMgmtException ame) {
            debug.error("LibertyManager: getUnFederatedIDPList: Error while " +
                " getting allFederatedProviderID from Account Mgmt", ame);
        }
        
        return unFederatedIDPs;
    }
    
    /** 
     * Returns the set of federated providers for an user
     * using Account Management API.
     *
     * @param userName for which the federated providers are to be returned.
     * @param hostProviderId Hosted provider's entity ID.
     * @param hostProviderRole Hosted Provider Role.
     * @return federated providers a Set containing the provider IDs of
     *  federated providers for the given <code>userName</code>.
     * @deprecated This method is deprecated.
     * @see #getFederatedProviders(String, String, String, String)
     */
    public static Set getFederatedProviders(
        String userName,
        String hostProviderId,
        String hostProviderRole)
    {
        return getFederatedProviders(
            userName, IFSConstants.ROOT_REALM,hostProviderId, hostProviderRole);
    }

    /** 
     * Returns the set of federated providers for an user
     * using Account Management API.
     *
     * @param userName for which the federated providers are to be returned.
     * @param realm The realm under which the entity resides.
     * @param hostProviderId Hosted provider's entity ID.
     * @param hostProviderRole Hosted Provider Role.
     * @return federated providers a Set containing the provider IDs of
     *  federated providers for the given <code>userName</code>.
     */
    public static Set getFederatedProviders(
        String userName,
        String realm,
        String hostProviderId,
        String hostProviderRole)
    {
        Set federatedProviders = new HashSet();
        try {
            federatedProviders = FSAccountManager.getInstance(
                getMetaAlias(realm, hostProviderId, hostProviderRole)).
                    readAllFederatedProviderID(userName); 
        } catch (FSAccountMgmtException ame) {
            debug.error("LibertyManager: getFederatedProviders: Error while " +
                " getting federatedProviderIDs from Account Mgmt", ame);
        }
        return federatedProviders;
    }
    
    /**
     * Returns the List of COTs for the given Provider under root realm.
     *
     * @param providerId The ID of the provider whose <code>COTList</code>
     *  is to be found
     * @param providerRole The Role of the provider whose <code>COTList</code>
     *  is to be found
     * @return The set containing the authentication domains for the given
     *  provider.
     * @deprecated This method is deprecated.
     * @see #getListOfCOTs(String,String,String)
     */
    public static Set getListOfCOTs(String providerId, String providerRole) {
        return getListOfCOTs(IFSConstants.ROOT_REALM, providerId, providerRole);
    }

    /**
     * Returns the List of COTs for the given Provider under a realm.
     *
     * @param realm The realm under which the entity resides.
     * @param providerId The ID of the provider whose <code>COTList</code>
     *  is to be found
     * @param providerRole The Role of the provider whose <code>COTList</code>
     *  is to be found
     * @return The set containing the authentication domains for the given
     *  provider.
     */
    public static Set getListOfCOTs(
        String realm, String providerId, String providerRole) 
    {
        Set returnSet = new HashSet();
        BaseConfigType hostConfig = IDFFMetaUtils.getExtendedConfig(
            realm, providerId, providerRole, metaManager);
        if (hostConfig != null) {
            List cotSet = IDFFMetaUtils.getAttributeValueFromConfig(
                hostConfig, IFSConstants.COT_LIST);
            if (cotSet != null && !cotSet.isEmpty()) {
                Iterator iter = cotSet.iterator();
                while (iter.hasNext()) {
                    String cotID = (String) iter.next();
                    try {
                        CircleOfTrustManager cotManager = 
                            new CircleOfTrustManager();
                        CircleOfTrustDescriptor cotDesc =
                            cotManager.getCircleOfTrust(realm, cotID);
                        String tldURL = cotDesc.getIDFFWriterServiceURL();
                        String cotStatus = cotDesc.getCircleOfTrustStatus();
                        if (tldURL != null && tldURL.length() > 0 &&
                            cotStatus.equalsIgnoreCase(IFSConstants.ACTIVE)) 
                        {
                            returnSet.add((String)cotID);
                        }
                    } catch (COTException fsExp) {
                        debug.error("LibertyManager: getListOfCots " +
                            "COTException caught ", fsExp);
                    }
                }
            }
            if (returnSet != null && returnSet.size() > 0) {
                if (debug.messageEnabled()) {
                    debug.message("LibertyManager: getListOfCots returning " +
                        " cot set with " + returnSet);
                }
            } else {
                if (debug.messageEnabled()) {
                    debug.message("LibertyManager::getListOfCots returning" +
                        " null. Looks like COT is not set");
                }
            }
        }
        return returnSet;
    }

    /** 
     * Returns <code>metaAlias</code> from provider ID under root realm.
     *
     * @param providerID Provider's entity ID.
     * @param providerRole Provider Role.
     * @return <code>metaAlias</code> from provider ID
     * @deprecated This method is deprecated by 
     *  <code>getMetaAlias(String,String,String)</code>.
     * @see #getMetaAlias(String,String,String)
     */
    public static String getMetaAlias(String providerID, String providerRole) {
        return getMetaAlias(null, providerID, providerRole);
    }

    /** 
     * Returns <code>metaAlias</code> from provider ID under a realm.
     *
     * @param realm The realm under which the entity resides.
     * @param providerID Provider's entity ID.
     * @param providerRole Provider Role.
     * @return <code>metaAlias</code> from provider ID
     */
    public static String getMetaAlias(
        String realm, String providerID, String providerRole) 
    {
        BaseConfigType providerConfig = IDFFMetaUtils.getExtendedConfig(
            realm, providerID, providerRole, metaManager);

        String metaAlias = "";
        if (providerConfig != null) {
            metaAlias = providerConfig.getMetaAlias();
        }
        if (debug.messageEnabled()) {
            debug.message("LibertyManager: getMetaAlias: providerID is " +
                providerID + " and corresponding metaAlias is " + metaAlias);
        }
        return metaAlias;
     }

    /** 
     * Returns the <code>FederationDonePageURL</code> from the provider ID
     * under root realm.
     *
     * @param providerID Provider's entity ID.
     * @param providerRole Provider Role.
     * @param request HTTP servlet request.
     * @return the <code>FederationDonePageURL</code> from the provider ID.
     * @deprecated This method is deprecated.
     * @see #getFederationDonePageURL(String,String,String,HttpServletRequest)
     */
    public static String getFederationDonePageURL(
        String providerID, String providerRole, HttpServletRequest request) 
    {
        return getFederationDonePageURL(
            IFSConstants.ROOT_REALM, providerID, providerRole, request);
    }

    /** 
     * Returns the <code>FederationDonePageURL</code> from the provider ID
     * under a realm.
     *
     * @param realm The realm under which the entity resides.
     * @param providerID Provider's entity ID.
     * @param providerRole Provider Role.
     * @param request HTTP servlet request.
     * @return the <code>FederationDonePageURL</code> from the provider ID.
     * @deprecated This method is deprecated.
     * @see #getFederationDonePageURL(String,String,String,HttpServletRequest)
     */
    public static String getFederationDonePageURL(
        String realm, String providerID, 
        String providerRole, HttpServletRequest request) 
    {
        BaseConfigType providerConfig = IDFFMetaUtils.getExtendedConfig(
            realm, providerID, providerRole, metaManager);
        String metaAlias = null;
        if (providerConfig != null) {
            metaAlias = providerConfig.getMetaAlias();
        }
        return FSServiceUtils.getFederationDonePageURL(
            request, providerConfig, metaAlias);
    }
    
    
    /** 
     * Returns the <code>TerminationDonePageURL</code> from the provider ID
     * under root realm.
     *
     * @param providerID Provider's entity ID.
     * @param providerRole Provider Role.
     * @param request HTTP servlet request.
     * @return the <code>TerminationDonePageURL</code> from the provider ID.
     * @deprecated This method is deprecated
     * @see #getTerminationDonePageURL(String,String,String,HttpServletRequest)
     */
    public static String getTerminationDonePageURL(
        String providerID, String providerRole, HttpServletRequest request) 
    {
        return getTerminationDonePageURL(
            IFSConstants.ROOT_REALM, providerID, providerRole, request);
    }

    /** 
     * Returns the <code>TerminationDonePageURL</code> from the provider ID
     * under a realm.
     *
     * @param realm The realm under which the entity resides.
     * @param providerID Provider's entity ID.
     * @param providerRole Provider Role.
     * @param request HTTP servlet request.
     * @return the <code>TerminationDonePageURL</code> from the provider ID.
     */
    public static String getTerminationDonePageURL(
        String realm, String providerID, 
        String providerRole, HttpServletRequest request) 
    {
        BaseConfigType providerConfig = IDFFMetaUtils.getExtendedConfig(
            realm, providerID, providerRole, metaManager);
        String metaAlias = null;
        if (providerConfig != null) {
            metaAlias = providerConfig.getMetaAlias();
        }
        return FSServiceUtils.getTerminationDonePageURL(
            request, providerConfig, metaAlias);
       
    }
    
    /**
     * Returns Termination URL.
     *
     * @param providerID Provider's entity ID.
     * @param providerRole Provider Role.
     * @param request HTTP servlet request.
     * @return Termination URL.
     * @deprecated This method is deprecated
     * @see #getTerminationURL(String,String,String,HttpServletRequest)
     */
    public  static String getTerminationURL(
        String providerID,
        String providerRole,
        HttpServletRequest request)
    {
        return getTerminationURL(IFSConstants.ROOT_REALM, providerID, 
            providerRole, request);
    }

    /**
     * Returns Termination URL.
     *
     * @param realm The realm under which the entity resides.
     * @param providerID Provider's entity ID.
     * @param providerRole Provider Role.
     * @param request HTTP servlet request.
     * @return Termination URL.
     */
    public  static String getTerminationURL(
        String realm,
        String providerID,
        String providerRole,
        HttpServletRequest request)
    {
        String metaAlias = getMetaAlias(realm, providerID, providerRole);
        String baseURL = FSServiceUtils.getServicesBaseURL(request);
        return baseURL + IFSConstants.TERMINATE_SERVLET + "?"
            + IFSConstants.META_ALIAS + "=" + metaAlias;
    }
    
    /**
     * Returns <code>NameRegistrationURL</code>.
     *
     * @param providerID Provider's entity ID.
     * @param providerRole Provider Role.
     * @param request HTTP servlet request.
     * @return <code>NameRegistrationURL</code>.
     * @deprecated This method is deprecated.
     * @see #getNameRegistrationURL(String,String,String,HttpServletRequest)
     */
    public static String getNameRegistrationURL(
        String providerID,
        String providerRole,
        HttpServletRequest request)
    {
        return getNameRegistrationURL(
            IFSConstants.ROOT_REALM, providerID, providerRole, request);
    }

    /**
     * Returns <code>NameRegistrationURL</code>.
     *
     * @param realm The realm under which the entity resides.
     * @param providerID Provider's entity ID.
     * @param providerRole Provider Role.
     * @param request HTTP servlet request.
     * @return <code>NameRegistrationURL</code>.
     */
    public static String getNameRegistrationURL(
        String realm,
        String providerID,
        String providerRole,
        HttpServletRequest request)
    {
        String metaAlias = getMetaAlias(realm, providerID, providerRole);
        String baseURL = FSServiceUtils.getServicesBaseURL(request);
        return baseURL + IFSConstants.REGISTRATION_SERVLET + "?"
            + IFSConstants.META_ALIAS + "=" + metaAlias;
        
    }

    /**
     * Returns the provider's error page.
     *
     * @param providerId Provider's entity ID.
     * @param providerRole Provider Role.
     * @param request HTTP servlet request.
     * @return the provider's error page.
     * @deprecated This method is deprecated. Please use
     *  <code>getErrorPageURL(String,String,String,HttpServletRequest)</code>
     * @see #getErrorPageURL(String,String,String,HttpServletRequest)
     */
    public static String getErrorPageURL(
        String providerId, 
        String providerRole,
        HttpServletRequest request) 
    {
        return getErrorPageURL(IFSConstants.ROOT_REALM, providerId, 
            providerRole, request);
    }

    /**
     * Returns the provider's error page.
     *
     * @param realm The realm under which the entity resides.
     * @param providerId Provider's entity ID.
     * @param providerRole Provider Role.
     * @param request HTTP servlet request.
     * @return the provider's error page.
     */
    public static String getErrorPageURL(
        String realm,
        String providerId, 
        String providerRole,
        HttpServletRequest request) 
    {
        BaseConfigType providerConfig = IDFFMetaUtils.getExtendedConfig(
            realm, providerId, providerRole, metaManager);
        String metaAlias = null;
        if (providerConfig != null) {
            metaAlias = providerConfig.getMetaAlias();
        }
        return FSServiceUtils.getErrorPageURL(
            request, providerConfig, metaAlias);
    }
    
    /**
     * Returns the <code>FederationHandler</code>.
     *
     * @param request HTTP servlet request
     * @return the <code>FederationHandler</code>.
     */
    public static String getFederationHandlerURL(HttpServletRequest request) {
        String returnURL = FSServiceUtils.getServicesBaseURL(request)
            + "/" + IFSConstants.FEDERATION_HANDLER;
        if (debug.messageEnabled()) {
            debug.message(
                "LibertyManager: getFederationHandler: returnURL = " + 
                returnURL);
        }
        return returnURL;
    }
    
    /**
     * Returns the <code>ConsentHandler</code>.
     *
     * @param request HTTP servlet request.
     * @return the <code>ConsentHandler</code>.
     */
    public static String getConsentHandlerURL(HttpServletRequest request) {
        String returnURL =FSServiceUtils.getServicesBaseURL(request)
            + "/" + IFSConstants.CONSENT_HANDLER;
        if (debug.messageEnabled()) {
            debug.message(
                "LibertyManager: getConsentHandler: returnURL = " + returnURL);
        }
        return returnURL;
    }
    
    /** 
     * Returns true if logout succeeded.
     *
     * @param request HTTP servlet request.
     * @return true if logout succeeded.
     */
    public static boolean isLogoutSuccess(HttpServletRequest request) {
        String status = request.getParameter(IFSConstants.LOGOUT_STATUS);
        if (status == null || 
            status.equalsIgnoreCase(IFSConstants.LOGOUT_SUCCESS)) 
        {
            return true;
        } else {
            return false;
        }
    }
    
    /** 
     * Returns true if Termination succeeds.
     *
     * @param request HTTP servlet request.
     * @return true if Termination succeeds.
     */
    public static boolean isTerminationSuccess(HttpServletRequest request) {
        String status = request.getParameter(IFSConstants.TERMINATION_STATUS);
        if (status == null ||
            status.equalsIgnoreCase(IFSConstants.TERMINATION_SUCCESS))
        {
            return true;
        } else {
            return false;
        }
    }
    
    /** 
     * Returns true if Federation is cancelled.
     *
     * @param request HTTP servlet request.
     * @return true if Federation is cancelled.
     */
    public static boolean isFederationCancelled(HttpServletRequest request) {
        String status = request.getParameter(IFSConstants.TERMINATION_STATUS);
        if (status != null &&
            status.equalsIgnoreCase(IFSConstants.CANCEL))
        {
            return true;
        } else {
            return false;
        }
    }

    /** 
     * Returns true if termination is cancelled.
     *
     * @param request HTTP servlet request.
     * @return true if termination is cancelled.
     */
    public static boolean isTerminationCancelled(HttpServletRequest request) {
        String status = request.getParameter(IFSConstants.TERMINATION_STATUS);
        if (status != null &&
            status.equalsIgnoreCase(IFSConstants.CANCEL))
        {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Returns the realm by parsing the metaAlias. MetaAlias format is
     * <pre>
     * &lt;realm>/&lt;any string without '/'> for non-root realm or
     * /&lt;any string without '/'> for root realm.
     * </pre>
     * @param metaAlias The metaAlias.
     * @return the realm associated with the metaAlias.
     */
    public static String getRealmByMetaAlias(String metaAlias) {
        return IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
    }

    /** 
     * Creates New Request ID from the <code>HttpRequestServlet</code>.
     *
     * @param request HTTP servlet request.
     * @return New Request ID from the <code>HttpRequestServlet</code>.
     */
    public static String getNewRequest(HttpServletRequest request) {
        String targetURL = request.getParameter(IFSConstants.LRURL);
        String metaAlias = request.getParameter(IFSConstants.META_ALIAS);
        String entityID = getEntityID(metaAlias);
        String realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
        Map headerMap = getHeaderMap(request);
        String homePage = null;
        if (targetURL == null || targetURL.length() <= 0 ) {
            try {
                if (metaManager != null) {
                    BaseConfigType providerConfig = 
                        metaManager.getSPDescriptorConfig(realm, entityID);
                    homePage = IDFFMetaUtils.getFirstAttributeValue(
                        IDFFMetaUtils.getAttributes(providerConfig),
                        IFSConstants.PROVIDER_HOME_PAGE_URL);
                }
            } catch (IDFFMetaException ame) {
                debug.error("LibertyManager: getNewRequest: Error" +
                    " while getting the HostedProvider from meta mgmt",
                    ame);
            }
            
            if (debug.messageEnabled()) {
                debug.message("LibertyManager: getNewRequestID." +
                    " no goto in queryString.Assinging targetURL = " +
                    homePage);
            }
            targetURL = homePage;
        }
        
        try {
            FSLoginHelper loginHelper = new FSLoginHelper(request);
            // get the authlevel key
            HttpSession httpSession = request.getSession();
            String authLevel = (String) httpSession.getAttribute(
                IFSConstants.AUTH_LEVEL_KEY);
            Map retMap = loginHelper.createAuthnRequest(
                headerMap, targetURL, authLevel, metaAlias, null, true);
            if (retMap != null) {
                String reqID = (String)retMap.get(IFSConstants.AUTH_REQUEST_ID);
                if (debug.messageEnabled()) {
                    debug.message("LibertyManager: getNewRequestID: " +
                        "new request created with id " + reqID);
                } 
                return reqID;
            } else {
                debug.error("LibertyManager: getNewRequestID " +
                    " Could not create new request ");
                return null;
            }
        } catch (FSLoginHelperException exp) {
            debug.error("LibertyManager::getNewRequestID" +
                "In login helper exception ", exp);
            return null;
        }
    }
    
    /**
     * Sets the authentication request to be sent to identity provider under
     * root realm.
     * 
     * @param request <code>FSAuthnRequest</code> associated with a user
     *  session.
     * @param entityID Hosted Provider's entity ID
     * @return <code>true</code> if the operation is successful; 
     *  <code>false</code> otherwise.
     * @deprecated This method is deprecated.
     * @see #setAuthnRequest(FSAuthnRequest,String,String)
     */
    public static boolean setAuthnRequest(
        FSAuthnRequest request, String entityID) 
    {
        return setAuthnRequest(request, IFSConstants.ROOT_REALM, entityID);
    }

    /**
     * Sets the authentication request to be sent to identity provider.
     * 
     * @param request <code>FSAuthnRequest</code> associated with a user
     *  session.
     * @param realm the realm in which the entity resides
     * @param entityID Hosted Provider's entity ID
     * @return <code>true</code> if the operation is successful; 
     *  <code>false</code> otherwise.
     */
    public static boolean setAuthnRequest(
        FSAuthnRequest request, String realm, String entityID) 
    {
  
        if (request == null || entityID == null) {
            debug.message("LibertyManager.setAuthnRequest: null Input params");
            return false; 
        }
        try {
            FSSessionManager sessionManager = FSSessionManager.getInstance(
                getMetaAlias(realm, entityID, IFSConstants.SP));
            String requestID = request.getRequestID();
            if (requestID != null) {
                sessionManager.setAuthnRequest(requestID, request);
                return true;
            }
        } catch(Exception ex) {
            FSUtils.debug.error("LibertyManager.setAuthnRequest"+
                " Exception while setting authn request.", ex);
        }
        return false;
    }

    /**
     * Returns the HeaderMap.
     */
    private static Map getHeaderMap(HttpServletRequest request) {
        Map headerMap = new HashMap();
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String hn = headerNames.nextElement().toString();
            String hv = request.getHeader(hn);
            headerMap.put(hn, hv);
        }
        return headerMap;
    }
    
    public static String cleanQueryString(HttpServletRequest request) {
        Enumeration paramEnum = request.getParameterNames();
        String returnString = new String();
        while (paramEnum.hasMoreElements()) {
            String paramKey = (String)paramEnum.nextElement();
            if (paramKey.equalsIgnoreCase(IFSConstants.META_ALIAS) ||
                paramKey.equalsIgnoreCase(IFSConstants.AUTH_REQUEST_ID) ||
                paramKey.equalsIgnoreCase(IFSConstants.LRURL)) 
            {
                if (debug.messageEnabled()) {
                    debug.message("Libertymanager::cleanQueryString " +
                        " found metaAlias or LRURL or AUTH_REQUEST_ID.");
                }
            } else if (!paramKey.equals(IFSConstants.ARTIFACT_NAME_DEFAULT)) {
                String paramValue = request.getParameter(paramKey);
                if (returnString == null || returnString.length() < 1) {
                    returnString =  paramKey + "="
                        + URLEncDec.encode(paramValue);
                } else {
                    returnString = returnString + "&amp;"
                        +  paramKey + "="
                        + URLEncDec.encode(paramValue);
                }
            }
        }
        // check and append the authlevel key
        HttpSession httpSession = request.getSession();
        String authLevel = (String) httpSession.getAttribute(
                          IFSConstants.AUTH_LEVEL_KEY);
        if (authLevel != null) {
            if (returnString == null || returnString.length() < 1) {
                returnString = IFSConstants.AUTH_LEVEL_KEY + "=" + authLevel;
            } else {
                returnString = returnString + "&amp;" +
                    IFSConstants.AUTH_LEVEL_KEY + "=" + authLevel;
            }
        }
        if (debug.messageEnabled()) {
            debug.message("Libertymanager::cleanQueryString " +
                " returning with " + returnString);
        }
        return returnString;
    }
   
    /**
     * Returns succinct ID of a provider.
     *
     * @param entityID provider's entity ID.
     * @return succinct ID of a provider.
     * @deprecated This method has been deprecated. Use
     *   {@link #getSuccinctID(String, String)}
     */
    public static String getSuccinctID(String entityID) {
        return FSUtils.generateSourceID(entityID);
    }
   
    /**
     * Returns succinct ID of a provider.
     *
     * @param providerID provider's entity ID.
     * @param providerRole provider Role.
     * @return succinct ID of a provider.
     */
    public static String getSuccinctID(String providerID, String providerRole) {
        return FSUtils.generateSourceID(providerID);
    }

    /**
     * Returns registered providers of an user.
     *
     * @param userName user ID.
     * @param hostProviderId Hosted provider's entity ID.
     * @param providerRole Hosted Provider Role.
     * @return registered providers.
     * @deprecated This method is deprecated. Please use
     *  <code>getRegisteredProviders(String,String,String,String)</code>
     * @see #getRegisteredProviders(String,String,String,String)
     */
    public static Set getRegisteredProviders(
        String userName,
        String hostProviderId,
        String providerRole)
    {
        return getRegisteredProviders(
            userName, IFSConstants.ROOT_REALM, hostProviderId, providerRole);
    }

    /**
     * Returns registered providers of an user.
     *
     * @param userName user ID.
     * @param realm The realm under which the entity resides.
     * @param hostProviderId Hosted provider's entity ID.
     * @param providerRole Hosted Provider Role.
     * @return registered providers.
     */
    public static Set getRegisteredProviders(
        String userName,
        String realm,
        String hostProviderId,
        String providerRole)
    {
        Set registeredProviders = new HashSet();
        try {
            registeredProviders = FSAccountManager.getInstance(
                getMetaAlias(realm, hostProviderId, providerRole)).
                    readAllFederatedProviderID(userName); 
        } catch (FSAccountMgmtException ame) {
            debug.error("LibertyManager: getRegisteredProviders: Error while " +
                " getting federatedProviderIDs from Account Mgmt", ame);
        }
        return registeredProviders;
    }

    /**
     * Returns name registration provider ID key.
     *
     * @return name registration provider ID key.
     */
    public static String getNameRegistrationProviderIDKey() {
        return IFSConstants.REGISTRATION_PROVIDER_ID;
    }
    
    /** 
     * Returns true if name registration is cancelled.
     *
     * @param request HTTP servlet request.
     * @return true if name registration is cancelled.
     */
    public static boolean isNameRegistrationCancelled(
        HttpServletRequest request) 
    {
        String status = request.getParameter(IFSConstants.REGISTRATION_STATUS);
        if (status != null && status.equalsIgnoreCase(IFSConstants.CANCEL)) {
            return true;
        } else {
            return false;
        }
    }
    
    /** 
     * Returns true if name registration succeeds.
     *
     * @param request HTTP servlet request.
     * @return true if name registration succeeds.
     */
    public static boolean isNameRegistrationSuccess(HttpServletRequest request)
    {
        String status = request.getParameter(IFSConstants.REGISTRATION_STATUS);
        if (status != null &&
            status.equalsIgnoreCase(IFSConstants.REGISTRATION_SUCCESS))
        {
            return true;
        } else {
            return false;
        }
    }
    
    /** 
     * Returns the Name <code>RegistrationDonePageURL</code> from the
     * <code>providerID</code> under root realm.
     *
     * @param providerID provider's entity ID.
     * @param providerRole provider Role.
     * @param request HTTP servlet request.
     * @return the Name <code>RegistrationDonePageURL</code> from the
     *  <code>providerID</code>.
     * @deprecated This method is deprecated. Please use
     *  <code>getNameRegistrationDonePageURL(
     *  String,String,String,HttpServletRequest)</code>
     * @see #getNameRegistrationDonePageURL(String,String,String,HttpServletRequest)
     */
    public static String getNameRegistrationDonePageURL(
        String providerID, String providerRole, HttpServletRequest request) 
    {      
        return getNameRegistrationDonePageURL(
            IFSConstants.ROOT_REALM, providerID, providerRole, request);
    }

    /** 
     * Returns the Name <code>RegistrationDonePageURL</code> from the
     * <code>providerID</code> under a realm.
     *
     * @param realm The realm under which the entity resides.
     * @param providerID provider's entity ID.
     * @param providerRole provider Role.
     * @param request HTTP servlet request.
     * @return the Name <code>RegistrationDonePageURL</code> from the
     *  <code>providerID</code>.
     */
    public static String getNameRegistrationDonePageURL(
        String realm, 
        String providerID, 
        String providerRole, 
        HttpServletRequest request) 
    {      
        BaseConfigType extendedConfig = IDFFMetaUtils.getExtendedConfig(
            realm, providerID, providerRole, metaManager);
        String metaAlias = null;
        if (extendedConfig != null) {
            metaAlias = extendedConfig.getMetaAlias();
        }
        return FSServiceUtils.getRegistrationDonePageURL(
            request, extendedConfig, metaAlias);
    }
    
    /** 
     * Returns Authentication Request Envelope from a HTTP servlet request.
     * @param request a HTTP servlet request
     * @return Authentication Request Envelope in String
     */
    public static String getAuthnRequestEnvelope(HttpServletRequest request)
    {
        FSLoginHelper loginHelper = new FSLoginHelper(request);
        return loginHelper.createAuthnRequestEnvelope(request); 
    }

    /** 
     * Determines if a HTTP servlet request is Liberty-enabled client and 
     * proxy profile.
     * @param request a HTTP servlet request
     * @return <code>true</code> if it is Liberty-enabled client and 
     *  proxy profile
     */
    public static boolean isLECPProfile(HttpServletRequest request) {
        return FSServiceUtils.isLECPProfile(request);
    }

    /** 
     * Returns Liberty-enabled client and proxy profile HTTP header name.
     * @return header name
     */
    public static String getLECPHeaderName(){
        return IFSConstants.LECP_HEADER_NAME;
    }

    /** 
     * Returns Liberty-enabled client and proxy profile HTTP content type.
     * @return content type
     */
    public static String getLECPContentType(){
        return IFSConstants.LECP_CONTENT_TYPE_HEADER;
    }

    /**
     * Returns the Discovery Service Resource Offerings nodes in an attribute
     * statement. After a single sign-on with an Identity Provider, a service
     * provider may get Discovery Service Resource Offerings through a SAML
     * assertion. This APIs helps in retrieving the resource offerings
     * if the user has been authenticated through the liberty SSO. It will
     * need to have a valid single sign on token (generated through the
     * liberty SSO).
     *
     * @param request <code>HttpServletRequest</code> associated with a user
     *        session.
     * @param providerID Hosted Provider's entity ID
     * @return <code>NodeList</code> Discovery Resource Offering Nodes,
     *  <code>null</code> if there is any failure  or if there is not one
     * @deprecated This method has been deprecated. Use
     *   {@link #getDiscoveryBootStrapResourceOfferings(
     *     HttpServletRequest request, String providerID, String providerRole)}
     */
    public static NodeList getDiscoveryBootStrapResourceOfferings(
       HttpServletRequest request, String providerID) 
    {
       if (request == null || providerID == null) {
          if (debug.messageEnabled()) {
             debug.message("LibertyManager.getDiscoveryResource" +
                 "Offering: null Input params");
          }
          return null;
       }
       try {
           Object token  = SessionManager.getProvider().getSession(request);
           FSSessionManager sessionManager = FSSessionManager.getInstance(
               getMetaAlias(
                   IFSConstants.ROOT_REALM, providerID, IFSConstants.SP));
           FSSession session = sessionManager.getSession(token);
           if (session == null) {
               if (FSUtils.debug.messageEnabled()) {
                   FSUtils.debug.message("LibertyManager.getDiscoveryResource" +
                       "Offerings: Theres no liberty session for this token"); 
               }
               return null;
           }
           return session.getBootStrapResourceOfferings();
       } catch(Exception ex) {
           FSUtils.debug.error("LibertyManager.getDiscoveryResourceOfferings"+
               " Exception while retrieving discovery boot strap info.", ex);
           return null;
       }
       
    }
    
    /**
     * Returns the Discovery Service Resource Offerings nodes in an attribute
     * statement. After a single sign-on with an Identity Provider, a service
     * provider may get Discovery Service Resource Offerings through a SAML
     * assertion. This APIs helps in retrieving the resource offerings
     * if the user has been authenticated through the liberty SSO. It will
     * need to have a valid single sign on token (generated through the
     * liberty SSO). The service provider should be under root realm.
     *
     * @param request <code>HttpServletRequest</code> associated with a user
     *  session.
     * @param providerID Hosted Provider's entity ID
     * @param providerRole Hosted Provider Role
     * @return <code>NodeList</code> Discovery Resource Offering Nodes,
     *  <code>null</code> if there is any failure  or if there is not one
     */
    public static NodeList getDiscoveryBootStrapResourceOfferings(
       HttpServletRequest request, String providerID, String providerRole) 
    {
       if (request == null || providerID == null) {
          if (debug.messageEnabled()) {
              debug.message("LibertyManager.getDiscoveryResource" +
                  "Offering: null Input params");
          }
          return null;
       }
       try {
           Object token  = SessionManager.getProvider().getSession(request);
           FSSessionManager sessionManager = FSSessionManager.getInstance(
               getMetaAlias(
                   IFSConstants.ROOT_REALM, providerID, IFSConstants.SP));
           FSSession session = sessionManager.getSession(token);
           if (session == null) {
               if (debug.messageEnabled()) {
                   debug.message("LibertyManager.getDiscoveryResource" +
                       "Offerings: Theres no liberty session for this token"); 
               }
               return null;
           }
           return session.getBootStrapResourceOfferings();
       } catch(Exception ex) {
           FSUtils.debug.error("LibertyManager.getDiscoveryResourceOfferings"+
               " Exception while retrieving discovery boot strap info.", ex);
           return null;
       }
    }

    /**
     * Returns the Discovery Service Credentials in the Advice element.
     * After a single sign-on with an Identity Provider, a service
     * provider may get Discovery Service Resource Offerings and Credentials
     * through a SAML assertion. This APIs helps in retrieving the Credentials
     * if the user has been authenticated through the liberty SSO. It will
     * need to have a valid single sign on token (generated through the
     * liberty SSO). The service provider should be under root realm.
     *
     * @param request <code>HttpServletRequest</code> associated with a user
     *  session.
     * @param providerID Hosted Provider's entity ID
     * @return <code>List</code> of <code>SecurityAssertions</code>,
     *         null if there is any failure  or if there is not one
     * @deprecated This method has been deprecated. Use
     *   {@link #getDiscoveryBootStrapCredentials(
     *     HttpServletRequest request, String providerID, String providerRole)}
     */
    public static List getDiscoveryBootStrapCredentials(
       HttpServletRequest request, String providerID) 
    {
  
       if (request == null || providerID == null) {
           if (debug.messageEnabled()) {
               debug.message("LibertyManager.getDiscoveryCredentials:" +
                   " null Input params");
           }
           return null;
       }
       try {
           Object token  = SessionManager.getProvider().getSession(request);
           FSSessionManager sessionManager = FSSessionManager.getInstance(
               getMetaAlias(
                   IFSConstants.ROOT_REALM, providerID, IFSConstants.SP));
           FSSession session = sessionManager.getSession(token);
           if (session == null) {
               if (debug.messageEnabled()) {
                   debug.message("LibertyManager.getDiscoveryCredentials" 
                       + ": Theres no liberty session for this token"); 
               }
               return null;
           }
           return session.getBootStrapCredential();
       } catch(Exception ex) {
           FSUtils.debug.error("LibertyManager.getDiscoveryCredentials"+
               " Exception while retrieving discovery boot strap info.", ex);
           return null;
       }
       
    }

    /**
     * Returns the Discovery Service Credentials in the Advice element.
     * After a single sign-on with an Identity Provider, a service
     * provider may get Discovery Service Resource Offerings and Credentials
     * through a SAML assertion. This APIs helps in retrieving the Credentials
     * if the user has been authenticated through the liberty SSO. It will
     * need to have a valid single sign on token (generated through the
     * liberty SSO). The service provider should be under root realm.
     *
     * @param request <code>HttpServletRequest</code> associated with a user
     *  session.
     * @param providerID Hosted Provider's entity ID
     * @param providerRole Hosted Provider Role
     * @return <code>List</code> of <code>SecurityAssertions</code>,
     *  <code>null</code> if there is any failure  or if there is not one
     */
    public static List getDiscoveryBootStrapCredentials(
       HttpServletRequest request, String providerID, String providerRole) 
    {
       if (request == null || providerID == null) {
           if (debug.messageEnabled()) {
               debug.message("LibertyManager.getDiscoveryCredentials:" +
                   " null Input params");
           }
           return null;
       }
       try {
           Object token  = SessionManager.getProvider().getSession(request);
           FSSessionManager sessionManager = FSSessionManager.getInstance(
               getMetaAlias(
                   IFSConstants.ROOT_REALM, providerID, IFSConstants.SP));
           FSSession session = sessionManager.getSession(token);
           if (session == null) {
               if (debug.messageEnabled()) {
                   debug.message("LibertyManager.getDiscoveryCredentials" 
                       + ": Theres no liberty session for this token"); 
               }
               return null;
           }
           return session.getBootStrapCredential();
       } catch(Exception ex) {
           FSUtils.debug.error("LibertyManager.getDiscoveryCredentials"+
               " Exception while retrieving discovery boot strap info.", ex);
           return null;
       }
       
    }


    /**
     * Returns the authentication context used in liberty single sign-on.
     * After single sign-on with an Identity Provider, a service
     * provider may obtain the authentication context used by the identity
     * provider that authenticates the user. It will need to have a valid
     * single sign on token (generated through the liberty SSO). The providers
     * are under root realm.
     *
     * @param request <code>HttpServletRequest</code> associated with a user
     *  session.
     * @param entityID Hosted Provider's entity ID 
     * @return authentication context string;
     *         <code>null</code> if there is any failure, or no liberty
     *         session is found.
     * @deprecated This method is deprecated.
     * @see #getAuthnContext(HttpServletRequest,String,String)
     */
    public static String getAuthnContext(
        HttpServletRequest request, String entityID) 
    {
        return getAuthnContext(request, IFSConstants.ROOT_REALM, entityID);
    }

    /**
     * Returns the authentication context used in liberty single sign-on.
     * After single sign-on with an Identity Provider, a service
     * provider may obtain the authentication context used by the identity
     * provider that authenticates the user. It will need to have a valid
     * single sign on token (generated through the liberty SSO).
     *
     * @param request <code>HttpServletRequest</code> associated with a user
     *  session.
     * @param realm the realm in which the provider resides
     * @param entityID Hosted Provider's entity ID
     * @return authentication context string;
     *         <code>null</code> if there is any failure, or no liberty
     *         session is found.
     */
    public static String getAuthnContext(
       HttpServletRequest request, String realm, String entityID) 
    {
  
       if (request == null || entityID == null) {
           debug.message("LibertyManager.getAuthnContext: null Input params");
           return null;
       }
       try {
           Object token  = SessionManager.getProvider().getSession(request);
           FSSessionManager sessionManager = FSSessionManager.getInstance(
               getMetaAlias(realm, entityID, IFSConstants.SP));
           FSSession session = sessionManager.getSession(token);
           if (session == null) {
               if (debug.messageEnabled()) {
                   debug.message("LibertyManager.getAuthnContext" 
                       + ": There is no liberty session for this token"); 
               }
               return null;
           }
           return session.getAuthnContext();
       } catch(Exception ex) {
           FSUtils.debug.error("LibertyManager.getAuthnContext"+
               " Exception while retrieving authncontext.", ex);
           return null;
       }
    }

    /**
     * Returns the federation information associated with current liberty 
     * session.
     * It will need to have a valid single sign on token (generated through 
     * the liberty SSO). It is for a service provider in a root realm.
     *
     * @param request <code>HttpServletRequest</code> associated with a user
     *  session.
     * @param entityID Hosted Provider's entity ID
     * @return <code>FSAccountFedInfo</code> associated with this session.
     *         <code>null</code> if there is any failure, or no liberty session
     *         is found.
     * @deprecated This method is deprecated.
     * @see #getAccountFedInfo(HttpServletRequest,String,String,String)
     */
    public static FSAccountFedInfo getAccountFedInfo(
        HttpServletRequest request, String entityID) 
    {
        return getAccountFedInfo(
            request, IFSConstants.ROOT_REALM, entityID, IFSConstants.SP);
    }

    /**
     * Returns the federation information associated with current liberty 
     * session. 
     * It will need to have a valid single sign on token (generated through 
     * the liberty SSO).
     *
     * @param request <code>HttpServletRequest</code> associated with a user
     *  session.
     * @param realm the realm in which the provider resides
     * @param entityID Hosted Provider's entity ID
     * @param providerRole the role of the provider
     * @return <code>FSAccountFedInfo</code> associated with this session.
     *         <code>null</code> if there is any failure, or no liberty session
     *         is found.
     */
    public static FSAccountFedInfo getAccountFedInfo(
       HttpServletRequest request, String realm, String entityID, 
       String providerRole) 
    {
  
       if (request == null || entityID == null) {
           debug.message("LibertyManager.getAccountFedInfo: null Input params");
           return null;
       }
       try {
           Object token  = SessionManager.getProvider().getSession(request);
           FSSessionManager sessionManager = FSSessionManager.getInstance(
               getMetaAlias(realm, entityID, providerRole));
           FSSession session = sessionManager.getSession(token);
           if (session == null) {
               if (debug.messageEnabled()) {
                   debug.message("LibertyManager.getAccountFedInfo" 
                       + ": Theres no liberty session for this token"); 
               }
               return null;
           }
           return session.getAccountFedInfo();
       } catch(Exception ex) {
           FSUtils.debug.error("LibertyManager.getAccountFedInfo"+
               " Exception while retrieving federation info.", ex);
           return null;
       }
    }

    /**
     * Returns <code>providerRole</code> from the <code>ProviderAlias</code>
     * using <code>meta Mgmt</code> calls.
     *
     * @param metaAlias The <code>metaAlias</code> of the provider
     * @return <code>providerRole</code> corresponding to the
     *  <code>metaAlias</code>.
     */
    public static String getProviderRole(String metaAlias) {
        try {
            if (metaManager == null) {
                return null;
            }
            return metaManager.getProviderRoleByMetaAlias(metaAlias);
        } catch (IDFFMetaException ame) {
            debug.error("LibertyManager: getProviderRole: Error getting " +
                "Provider Role", ame);
            return null;
        }
    }

    /**
     * Returns <code>NameIdentifier</code> between the IDP and
     * the other SP for the same principal. This method should
     * be used by the code on the hosted SP, where the user
     * has logged in, and has an account linking with the IDP.
     * Futhermore, the same principal has an account linking
     * between his/her other (remote) SP account and the IDP
     * account.
     *
     * @param hostedSPMetaAlias The <code>metaAlias</code> of
     * the local service provider.
     * @param ssoToken The session token of the logged-
     * in user on the local service provider.
     * @param remoteSPEntityID The entity ID of the remote
     * service provider. In other words, it is the targeted
     * name space for the returned name identifier.
     * @return <code>NameIdentifier</code> of the same principal
     * but original established between the remote SP and
     * the IDP
     */
    public static NameIdentifier getMappedNameIdentifier(
        String hostedSPMetaAlias,
        Object ssoToken,
        String remoteSPEntityID) 
    {

        String classMethod = "LibertyManager.getMappedNameIdentifier: ";
        
        String hostedEntityID = getEntityID(hostedSPMetaAlias);
        String realm = IDFFMetaUtils.getRealmByMetaAlias(hostedSPMetaAlias);
        
        if (debug.messageEnabled()) {
            debug.message(
                "NameMappingRequester: hostedEntityID="+
                hostedEntityID
            );
        }

        SPDescriptorType hostedDescriptor = null;
        BaseConfigType hostedConfig = null;
        try {
            hostedDescriptor = metaManager.getSPDescriptor(
                realm, hostedEntityID);
            hostedConfig = metaManager.getSPDescriptorConfig(
                realm, hostedEntityID);
        } catch (IDFFMetaException ie) {
            debug.error(classMethod + "couldn't obtain hosted meta:", ie);
            return null;
        }

        String userID = null;
        try {
            userID = SessionManager.getProvider().getPrincipalName(ssoToken);
        } catch (SessionException ssoe) {
            debug.error(
                "SessionException caught when trying to "+
                "get user DN from session token:", ssoe
            );
            return null;
        }
        if (debug.messageEnabled()) {
            debug.message(classMethod + "userID="+ userID);
        }
        FSNameMappingHandler handler = new FSNameMappingHandler(
            hostedEntityID, hostedDescriptor, hostedConfig, hostedSPMetaAlias);
        
        NameIdentifier ni = null;
        
        if (debug.messageEnabled()) {
            debug.message(
                classMethod +
                "targetNamespace (remoteSPEntityID)=" +
                remoteSPEntityID);
        }
        FSSessionManager sMgr =
            FSSessionManager.getInstance(hostedSPMetaAlias);
        FSSession sess = null;
        if (sMgr != null) {
            sess = sMgr.getSession(ssoToken);
        }
        FSSessionPartner partner = null;
        if (sess != null) {
            partner = sess.getCurrentSessionPartner();
        } else {
            debug.error(classMethod +"session is null");
            return null;
        }
        String remoteIDPEntityID = null;
        if (partner != null) {
            remoteIDPEntityID = partner.getPartner();
        }
        if (debug.messageEnabled()) {
            debug.message(
                classMethod + "Remote IDP EntityID = " +
                remoteIDPEntityID);
        }
        try {
            ni = handler.getNameIdentifier(
                userID,
                remoteIDPEntityID,
                true);
        } catch (Exception e) {
            debug.error(
                classMethod+
                "Exception caught when trying to get Name "+
                "Identifier between local SP and remote IDP: ",
                e);
            return null;
        }
        if (debug.messageEnabled()) {
            debug.message(
                classMethod +
                "Name Identifier between local SP and " +
                " remote IDP: " + ni.toString());
        }
        
        FSNameIdentifierMappingRequest mappingRequest = null;
        try {
            mappingRequest = new
                FSNameIdentifierMappingRequest(
                    hostedEntityID,
                    ni,
                    remoteSPEntityID);
        } catch (com.sun.identity.federation.message.common.FSMsgException fe) {
            debug.error(classMethod, fe);
            return null;
        }
        if (FSServiceUtils.isSigningOn()) {
            try {
                mappingRequest.signXML(
                    IDFFMetaUtils.getFirstAttributeValueFromConfig(
                        hostedConfig, IFSConstants.SIGNING_CERT_ALIAS));
            } catch (SAMLException se) {
                debug.error(classMethod, se);
                return null;
            }
        } 
        IDPDescriptorType remoteProviderDesc = null;
        try {
            remoteProviderDesc = metaManager.getIDPDescriptor(
                realm, remoteIDPEntityID);
        } catch (IDFFMetaException fme1) {
            debug.error(classMethod, fme1);
            return null;
        }            
        String remoteSOAPEndPoint = remoteProviderDesc.getSoapEndpoint();
        
        if (debug.messageEnabled()) {
            debug.message(
                classMethod +
                "IDP's soap end point=" +
                remoteSOAPEndPoint);
        }
        FSSOAPService soapService = FSSOAPService.getInstance();
        
        SOAPMessage returnMsg = null;
        try {
            SOAPMessage msg = 
                soapService.bind(mappingRequest.toXMLString(true, true));
            returnMsg =
                soapService.sendMessage(msg, remoteSOAPEndPoint);
        } catch (FSMsgException mex) {
            debug.error(classMethod, mex);
            return null; 
        } catch (java.io.IOException ioe) {
            debug.error(classMethod, ioe);
            return null;
        } catch (javax.xml.soap.SOAPException soape) {
            debug.error(classMethod, soape);
            return null;
        }            
        Element elt = soapService.parseSOAPMessage(returnMsg);
        FSNameIdentifierMappingResponse mappingResponse = null;
        try {
            mappingResponse =
                new FSNameIdentifierMappingResponse(elt);
        } catch (FSMsgException fme2) {
            debug.error(classMethod, fme2);
            return null;
        }                        
        if (debug.messageEnabled()) {
            String resStr = null;
            try {
                resStr = mappingResponse.toXMLString();
            } catch (FSMsgException fme3)
            {
                debug.error(classMethod, fme3);
                return null;
            }                        
            debug.message(
                classMethod +
                "NameIdentifierMappingResponse: " +
                resStr);
        }
        if (FSServiceUtils.isSigningOn()) {
            if (FSNameMappingHandler.
                verifyNameIdMappingResponseSignature(elt, returnMsg, realm)) {
                
                if (debug.messageEnabled()) {
                    debug.message(
                        classMethod +
                        "Success in verifying Name Identifier Mapping"+
                        " Response Signature");
                    }
            } else {
                debug.error(
                    classMethod +
                    "Failed verifying Name Identifier Mapping "+
                    "Response");
                return null;        
                }
        }
        return mappingResponse.getNameIdentifier();
    }
}
