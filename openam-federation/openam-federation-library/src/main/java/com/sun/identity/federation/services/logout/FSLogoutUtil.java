/*
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
 * $Id: FSLogoutUtil.java,v 1.12 2008/11/10 22:56:58 veiming Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.federation.services.logout;

import com.sun.identity.federation.message.FSLogoutNotification;
import com.sun.identity.federation.message.FSLogoutResponse;
import com.sun.identity.federation.accountmgmt.FSAccountManager;
import com.sun.identity.federation.accountmgmt.FSAccountMgmtException;
import com.sun.identity.federation.accountmgmt.FSAccountFedInfoKey;
import com.sun.identity.federation.accountmgmt.FSAccountFedInfo;
import com.sun.identity.federation.services.FSSession;
import com.sun.identity.federation.services.FSSessionManager;
import com.sun.identity.federation.services.FSSessionPartner;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.federation.services.util.FSSignatureUtil;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.liberty.ws.meta.jaxb.AffiliationDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.ProviderDescriptorType;
import com.sun.identity.multiprotocol.MultiProtocolUtils;
import com.sun.identity.multiprotocol.SingleLogoutManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.common.SAMLResponderException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.List;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Utility class for single logout.
 */
public class FSLogoutUtil {
    
    static IDFFMetaManager metaManager = null;
    static {
        metaManager = FSUtils.getIDFFMetaManager();
    };
    
    /**
     * Destroys the principal's session.
     * In order to destroy the user's session the following things need
     * to be done
     * 1. Destroy the Federation Session cookie (eg. iPlanetDirectoryPro)
     * 2. Clean the Session manager (FSSessionManager related API call)
     * @param userID the principal whose session needs to be destroyed
     * @param metaAlias the hostedProvider's meta alias.
     * @param sessionIndex Session Index of the user session.
     * @param request HTTP Request Object.
     * @param response HTTP Response Object.
     * @return <code>true</code> if session cleanup was successful;
     *  <code>false</code> otherwise.
     */
    protected static boolean destroyPrincipalSession(
        String userID, 
        String metaAlias,
        String sessionIndex,
        HttpServletRequest request,
        HttpServletResponse response)
    {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("Entered destroyPrincipalSession" +
                " for user : " + userID + " SessionIndex = " + sessionIndex);
        }
        Vector sessionObjList = getSessionObjectList(
            userID, metaAlias, sessionIndex);
        if (sessionObjList == null) {
            return false;
        }
        // Invalidate all such session ids 
        // session manager cleanup
        invalidateActiveSessionIds(sessionObjList, request, response);

        FSSession session = null;
        if (sessionIndex != null && 
            (sessionObjList != null && sessionObjList.size() == 1))
        {
           session = (FSSession)sessionObjList.elementAt(0);
        }
        // clean FSSession map
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("To call cleanSessionMap for user : " 
                + userID);
        }
        cleanSessionMap(userID, metaAlias, session);
        return true;
    }

    /**
     * Destroys local session.
     * @param ssoToken session of the principal
     * @return <code>true</code> if the local session is deleted;
     *  <code>false</code> otherwise.
     */
    protected static boolean destroyLocalSession(Object ssoToken,
        HttpServletRequest request, HttpServletResponse response)
    {
        try{
            FSUtils.debug.message("FSLogoutUtil.destroyLocalSession, enter");
            SessionProvider sessionProvider = SessionManager.getProvider();
            if (sessionProvider.isValid(ssoToken)) {
                MultiProtocolUtils.invalidateSession(ssoToken,
                        request, response, SingleLogoutManager.IDFF);
            }
            FSUtils.debug.message("FSLogoutUtil.destroyLocalSession, deleted");
            return true;                
        } catch (SessionException e){
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "SessionException in destroyLocalSession", e);
            }
            return false;
        }
    }
    
    /**
     * Destroys the principal's session information
     * maintained by <code>FSSessionManager</code>.
     * @param sessionObjList the Vector of <code>sessionId</code>s
     * @param request <code>HttpServletRequest</code> object
     * @param response <code>HttpServletResponse</code> object
     */    
    private static void invalidateActiveSessionIds(Vector sessionObjList,
        HttpServletRequest request, HttpServletResponse response) 
    {
        FSUtils.debug.message("FSLogoutUtil.invalidateActiveSessionIds, start");
        if (sessionObjList != null && !sessionObjList.isEmpty()) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(sessionObjList.size() +
                    " Active Session exists");
            }
            SessionProvider sessionProvider = null;
            try {
                sessionProvider = SessionManager.getProvider();
            } catch (SessionException se) {
                FSUtils.debug.error("invalidateActiveSessionIds:" +
                    "Couldn't obtain session provider:", se);
                return;
            }
            for (int i = 0; i < sessionObjList.size(); i++) {
                String sessionId = (String)(((FSSession) 
                    sessionObjList.elementAt(i)).getSessionID());
                if (sessionId != null) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("To Invalidate session : " 
                            + sessionId);
                    }
                    //Invalidate session
                    try {
                        Object ssoToken = sessionProvider.getSession(sessionId);
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("Destroying token : " +
                                sessionProvider.getPrincipalName(ssoToken));
                        }
                        MultiProtocolUtils.invalidateSession(ssoToken,
                            request, response, SingleLogoutManager.IDFF);
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "Completed Destroying token for sessionID :" + 
                                sessionId);
                        }
                    } catch (SessionException e) {
                        FSUtils.debug.error("invalidateActiveSessionIds : " +
                            sessionId + " - ", e);
                        continue;
                    }
                }
            }
        } else {
            FSUtils.debug.message("No active Session exists");
        }
    }
    
    /**
     * Gets the list of the principal's active sessionID
     * that is maintained by <code>FSSessionManager</code>.
     * @param userDn the principal whose session needs to be destroyed
     * @param metaAlias the hosted Entity doing logout cleanup
     * @param sessionIndex index of the user's session
     * @return Vector list of active Session IDs
     */
    protected static Vector getSessionObjectList(
        String userDn, 
        String metaAlias,
        String sessionIndex)
    {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("Entered getSessionObjectList for user : " + 
                userDn);
        }
        Vector retList = new Vector();
        FSSessionManager sessionMgr =
            FSSessionManager.getInstance(metaAlias);
        synchronized (sessionMgr) {
            List sessionList = sessionMgr.getSessionList(userDn);
            if (sessionList != null){
                FSUtils.debug.message("Session list is not null");
                Iterator iter = sessionList.iterator();
                FSSession sessionObj;
                while (iter.hasNext()) {
                    sessionObj = (FSSession)iter.next();
                    if (sessionIndex != null && 
                        sessionIndex.equals(sessionObj.getSessionIndex())) 
                    {
                       Vector destroySessObj = new Vector();
                       destroySessObj.addElement(sessionObj);
                       return destroySessObj;
                    }
                    retList.addElement(sessionObj);
                }
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Returning session list with number" +
                        " of elements = " + retList.size());
                }
                return retList;
            } else {
                FSUtils.debug.message("Session list is null");
                return null;
            }
        }
    }
    
    /**
     * Cleans the <code>FSSessionManager</code> maintained session
     * for the given principal, provider Id and removes all references to 
     * the provider since logout notification has already been sent to 
     * that provider.
     * @param userDN the principal whose session needs to be destroyed
     * @param currentEntityId the provider to whom logout notification is 
     *  about to be sent
     * @param metaAlias the hostedProvider doing logout cleanup
     * @param session Liberty session.
     */    
    public static void cleanSessionMapPartnerList(
        String userDN,
        String currentEntityId,
        String metaAlias, 
        FSSession session)
    {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("In cleanSessionMapPartnerList for user : " +
                userDN + "and provider : " + currentEntityId);
        }
        FSSessionManager sessionMgr = 
            FSSessionManager.getInstance(metaAlias);
        sessionMgr.removeProvider(userDN, currentEntityId, session);
    }
    
    /**
     * Cleans the FSSessionManager maintained session for the given principal, 
     * provider Id and removes all references to the provider since logout 
     * notification has already been sent to that provider.
     * @param userDN the principal whose session needs to be destroyed
     * @param currentEntityId the provider to whom logout notification is 
     * about to be sent
     * @param  the hostedProvider doing logout cleanup
     */
/* 
    protected static void cleanSessionWithNoPartners(
      String userDN,
      String currentEntityId,
      String metaAlias)
    {
        FSSessionManager sessionMgr =
            FSSessionManager.getInstance(metaAlias);
        synchronized (sessionMgr) {
            List sessionList = sessionMgr.getSessionList(userDN);
            if (sessionList != null){
                FSUtils.debug.message("Session list is not null");
                Iterator iter = sessionList.iterator();
                FSSession sessionObj;
                while (iter.hasNext()){
                    sessionObj = (FSSession)iter.next();
                    if ((sessionObj.getSessionPartners()).isEmpty()) {
                        sessionMgr.removeSession(userDN, sessionObj);
                    }
                }
            } else {
                FSUtils.debug.message("Session list is null");
            }
        }
    }
*/
    
    /**
     * Cleans the <code>FSSessionManager</code> maintained session
     * for the given principal. Logout notification has already been sent to all
     * providers that had live connections for this user
     * If <code>FSSession</code> is null, then it cleans up the user's all 
     * sessions.
     * @param userDn the principal whose session needs to be destroyed
     * @param metaAlias the hostedProvider doing logout cleanup
     * @param session Liberty session.
     * @return <code>true</code> if session map cleaning was successful;
     *  <code>false</code> otherwise.
     */
    protected static boolean cleanSessionMap(
        String userDn,
        String metaAlias,
        FSSession session) 
    {
        FSUtils.debug.message("Entered cleanSessionMap");
        FSSessionManager sessionMgr =
            FSSessionManager.getInstance(metaAlias);
        synchronized (sessionMgr) {
            if (session == null) {
                sessionMgr.removeSessionList(userDn);
            } else {
                sessionMgr.removeSession(userDn, session);
            }
        }
        FSUtils.debug.message("Leaving cleanSessionMap");
        return true;     
    }
    
    /**
     * Retrieves the session token from the Http Request, and
     * validates the token with the OpenAM session manager.
     * @param request <code>HTTPServletRequest</code> object containing the 
     *  session cookie information
     * @return session token if request contained valid
     *  session info; <code>false</code> otherwise.
     */
    protected static Object getValidToken(HttpServletRequest request) {
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            Object ssoToken = sessionProvider.getSession(request);
            if ((ssoToken == null) || (!sessionProvider.isValid(ssoToken))) {
                FSUtils.debug.message(
                    "session is not valid,redirecting for authentication");
                return null;
            }
            return ssoToken;
        } catch (SessionException e){
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("getValidToken: SessionException caught:",
                    e);
            }
            return null;
        }
    }
    
    /**
     * Returns the <code>FSAccountFedInfo</code> object for the given 
     * principal and provider Id.
     * @param userID principal whose working account we want to retrieve
     * @param entityID the provider Id to whom logout notification needs to 
     *  be sent
     * @param metaAlias hosted provider's meta alias
     * @return account object for the given user, provider
     */
    protected static FSAccountFedInfo getCurrentWorkingAccount(
        String userID,
        String entityID,
        String metaAlias) 
    {
        try {
            FSAccountManager accountInst = FSAccountManager.getInstance(
                metaAlias);
            
            if (metaManager != null) {
                try {
                    String realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
                    Set affiliates = metaManager.getAffiliateEntity(
                        realm, entityID);
                    if (affiliates != null && !affiliates.isEmpty()) {
                        Iterator iter = affiliates.iterator();
                        while(iter.hasNext()) {
                            AffiliationDescriptorType desc = 
                                (AffiliationDescriptorType)iter.next();
                            String affiliationID = desc.getAffiliationID();
                            FSAccountFedInfo accountInfo =
                                accountInst.readAccountFedInfo(
                                    userID, affiliationID);
                            if ((accountInfo != null) &&
                                (accountInfo.isFedStatusActive()))
                            {
                                return accountInfo;
                            }
                        }
                    } else {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSLogoutUtil.getCurrent" +
                                "WorkingAccount: No affiliations");
                        }
                    }
                } catch (Exception ex) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSLogoutUtil.getCurrentWorking"+
                            "Account. No Affiliation for:" + entityID, ex);
                    }
                }
                FSAccountFedInfo acctInfo =
                    accountInst.readAccountFedInfo(userID, entityID);
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSLogoutUtil::" +
                        "getCurrentWorkingAccount after readAccountFedInfo");
                }
                return acctInfo;
            } else {
                return null;
            }
        } catch (Exception e) {
            FSUtils.debug.error("FSLogoutUtil::getCurrentWorkingAccount" +
                " readAccountFedInfo failed", e);
        }
        return null;
    }
    
    /**
     * Returns the information for the given principal and one of the live 
     * connections (provider that received/issued assertion for this user) 
     * including <code>sessionIndex</code>, provider Id etc.
     * @param userID principal who needs to be logged out
     * @param metaAlias the hostedProvider doing logout cleanup
     * @return HashMap information about live connection provider
     */
    protected static HashMap getCurrentProvider(
        String userID,
        String metaAlias)
    {
        return getCurrentProvider(userID, metaAlias, null);
    }

    public static HashMap getCurrentProvider(
        String userID,
        String metaAlias,
        Object ssoToken) 
    {
        return getCurrentProvider(userID, metaAlias, ssoToken, null);
    }

    public static HashMap getCurrentProvider(
        String userID,
        String metaAlias,
        Object ssoToken,
        FSSession curSession) 
    {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("Entered getCurrentProvider for user : " +
                userID);
        }

        HashMap providerMap = new HashMap();
        
        try {
            FSSessionManager sessionMgr = FSSessionManager.getInstance(
                metaAlias);

            FSSession session = sessionMgr.getSession(ssoToken);
            if (session == null && curSession != null) {
                session = curSession;
            }
            if (session != null) {
                List partners = session.getSessionPartners();
                if (partners != null && !partners.isEmpty()) {
                    Iterator iter = partners.iterator();
                    FSSessionPartner sessionPartner = 
                        (FSSessionPartner)iter.next();
                    providerMap.put(
                        IFSConstants.PARTNER_SESSION, sessionPartner);
                    providerMap.put(IFSConstants.SESSION_INDEX, 
                    session.getSessionIndex());
                    return providerMap;
                } else {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "FSLogoutUtil.getCurrentProvider:"+
                            "No more session partners");
                    }
                    return null;
                }
            }

            return null;
        } catch(Exception e) {
            FSUtils.debug.error("FSLogoutUtil.getCurrentProvider:: Exception" +
                " in getting the current provider", e); 
            return null;
        } 
    }

    /**
     * Finds out the role of the provider in live connection list 
     * (provider that received/issued assertion for user).
     * @param userID principal who needs to be logged out
     * @param entityId to whom logout notification needs to be sent
     * @param metaAlias the hostedProvider performing logout
     * @return <code>true</code> if provider has IDP role;
     *  <code>false</code> otherwise.
     */
/*
    public static boolean getCurrentProviderRole(
        String userID,
        String entityId,
        String metaAlias)
    {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("Entered getCurrentProviderRole" +
                " for user : " + userID);
        }
        FSSessionManager sessionMgr = FSSessionManager.getInstance(
                   metaAlias);
        synchronized(sessionMgr) {
            List sessionList = sessionMgr.getSessionList(userID);
            if (sessionList != null) {
                FSUtils.debug.message("sessionList is not null");
                Iterator iSessionIter = sessionList.iterator();
                FSSession currentSession;
                while (iSessionIter.hasNext()) {
                    currentSession = (FSSession)iSessionIter.next();
                    List providerList = currentSession.getSessionPartners();
                    Iterator iProviderIter = providerList.iterator();
                    while (iProviderIter.hasNext()) {
                        FSSessionPartner sessionPartner = 
                            (FSSessionPartner)iProviderIter.next();
                        if (sessionPartner.isEquals(entityId)) {
                            return sessionPartner.getIsRoleIDP();
                        }
                    }
                }
            } else {
                FSUtils.debug.message("sessionList is null");
                return false;
            }
        }      
        return false;
    }
*/ 
    
    /**
     * Finds out if there is at least one more partner who should be notified 
     * of logout
     * @param userID principal who needs to be logged out
     * @param metaAlias ther provider performing logout
     * @return <code>true</code> if any provider exists; <code>false</code>
     *  otherwise.
     */    
    public static boolean liveConnectionsExist(
        String userID,
        String metaAlias) 
    {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("Entered liveConnectionsExist for user : " +
                userID);
        }

        FSSessionManager sessionMgr =
            FSSessionManager.getInstance(metaAlias);
        synchronized(sessionMgr) {
            FSUtils.debug.message("About to call getSessionList");
            List sessionList = sessionMgr.getSessionList(userID);
            if (sessionList != null && !sessionList.isEmpty()) {
                FSUtils.debug.message("List is not empty");
                Iterator iSessionIter = sessionList.iterator();
                FSSession sessionObj = null;
                while (iSessionIter.hasNext()) {
                    sessionObj = (FSSession)iSessionIter.next();
                    if ((sessionObj.getSessionPartners()).isEmpty()) {
                        continue;
                    } else {
                        return true;
                    }
                }
                return false;
            } else {
                FSUtils.debug.message("List is  empty");
                return false;
            }
        }
    }
    
    /**
     * Cleans the <code>FSSessionManager</code> maintained session
     * information for the user for the given list of sessions.
     * @param userID principal who needs to be logged out
     * @param sessionList is the list of session Ids to be cleaned for the user
     * @param metaAlias the provider performing logout
     * @return always return <code>true</code>
     */    
    protected static boolean cleanSessionMapProviders(
        String userID,
        Vector sessionList,
        String metaAlias) 
    {
        if (sessionList != null) {
            for (int i=0; i < sessionList.size(); i++) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("cleaning provider " + 
                        (String)sessionList.elementAt(i) + " from session map");
                }
                cleanSessionMapPartnerList(
                     userID, 
                     (String)sessionList.elementAt(i),
                     metaAlias,
                     null);
            }
        }
        return true;
    }
    
    /**
     * Returns the list of all providers who want to be
     * notified of logout using HTTP GET profile.
     * @param userID principal who needs to be logged out
     * @param entityId current provider who uses HTTP GET profile for logout
     * @param sessionIndex for the current provider
     * @param realm the realm in which the provider resides
     * @param metaAlias the hosted provider performing logout
     * @return HashMap list of providers who indicate preference to be notified 
     * of logout using GET profile
     */    
    protected static HashMap getLogoutGETProviders(
                String userID,
                String entityId,
                String sessionIndex,
                String realm,
                String metaAlias)
    {
        try {
            FSUtils.debug.message(
                "Entered FSLogoutUtil::getLogoutGETProviders");
            HashMap retMap = new HashMap();
            Vector providerVector = new Vector();
            HashMap sessionProvider = new HashMap();
            providerVector.addElement(entityId);
            sessionProvider.put(entityId, sessionIndex);
            FSSessionManager sessionMgr = FSSessionManager.getInstance(
                   metaAlias);
            synchronized(sessionMgr) {
                FSUtils.debug.message("About to call getSessionList");
                List sessionList = sessionMgr.getSessionList(userID);
                if (sessionList  != null && !sessionList.isEmpty()) {
                    FSUtils.debug.message("Session List is not empty");
                    Iterator iSessionIter = sessionList.iterator();
                    FSSession sessionObj;
                    while (iSessionIter.hasNext()) {
                        sessionObj = (FSSession)iSessionIter.next();
                        if ((sessionObj.getSessionPartners()).isEmpty()){
                            continue;
                        } else {
                            String nSessionIndex = sessionObj.getSessionIndex();
                            List sessionPartners =         
                                sessionObj.getSessionPartners();
                            Iterator iPartnerIter = sessionPartners.iterator();
                            FSSessionPartner sessionPartner;
                            while (iPartnerIter.hasNext()) {
                                sessionPartner = 
                                    (FSSessionPartner)iPartnerIter.next();
                                // Only SP can specify GET profile for logout
                                if (!sessionPartner.getIsRoleIDP()){    
                                    String curEntityId =
                                        sessionPartner.getPartner();
                                    ProviderDescriptorType curDesc =
                                        metaManager.getSPDescriptor(
                                            realm, curEntityId);
                                    if (curDesc != null) {
                                        List profiles = curDesc.
                                            getSingleLogoutProtocolProfile();
                                        if (profiles != null &&
                                            !profiles.isEmpty())
                                        {
                                            if (((String)profiles.iterator().
                                                next()).equals(IFSConstants.
                                                    LOGOUT_IDP_GET_PROFILE))
                                            {
                                                if (FSUtils.debug.
                                                    messageEnabled())
                                                {
                                                    FSUtils.debug.message(
                                                        "provider " + 
                                                        curEntityId +
                                                        " Added for GET");
                                                }
                                                providerVector.addElement(
                                                    curEntityId);
                                                sessionProvider.put(curEntityId,
                                                    nSessionIndex);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "Session List is  empty, returning " +
                            "current provider from getLogoutGETProviders");
                    }
                }
                retMap.put(IFSConstants.PROVIDER, providerVector);
                retMap.put(IFSConstants.SESSION_INDEX, sessionProvider);
                return retMap;
            }
        } catch(IDFFMetaException e){
            FSUtils.debug.error("IDFFMetaException in function " +
                " getLogoutGETProviders", e);
            return null;
        }
    }
    
    /**
     * Determines the user name from the logout request.
     * @param reqLogout the logout rerquest received
     * @param realm the realm under which the entity resides
     * @param hostedEntityId the hosted provider performing logout
     * @param hostedRole the role of the hosted provider
     * @param hostedConfig extended meta config for hosted provider
     * @param metaAlias hosted provider's meta alias
     * @return user id if the user is found; <code>null</code> otherwise.
     */
    public static String getUserFromRequest(FSLogoutNotification reqLogout, 
        String realm, String hostedEntityId, String hostedRole, 
        BaseConfigType hostedConfig, String metaAlias)
    {
        FSAccountManager accountInst = null;
        try {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Realm : " + realm + 
                    ", entityID : " + hostedEntityId);
            }
            accountInst = FSAccountManager.getInstance(metaAlias);
        } catch (FSAccountMgmtException fe) {
            FSUtils.debug.message("In FSAccountManagementException :: cannot" +
                " get account manager:" + fe);
            return null;
        }
        try {
            // User Name needs to be figured from logout request
            String opaqueHandle =
                (reqLogout.getNameIdentifier()).getName().trim();
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Name : " + opaqueHandle);
            }
            String associatedDomain =
                (reqLogout.getNameIdentifier().getNameQualifier()).trim();
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Name Qualifier : " + associatedDomain);
            }
            if ((associatedDomain == null) ||
                (associatedDomain.length() == 0) || 
                associatedDomain.equals(reqLogout.getProviderId()))
            {
                associatedDomain = hostedEntityId;
            }
            // Get userDN
            FSAccountFedInfoKey acctkey = null;
            // for SP, search local domain first, for IDP, search
            // remote domain(SP) first
            if (hostedRole.equalsIgnoreCase(IFSConstants.SP)) {
                acctkey = new FSAccountFedInfoKey(
                    associatedDomain, opaqueHandle);
            } else {
                acctkey = new FSAccountFedInfoKey(
                    reqLogout.getProviderId(), opaqueHandle);
            }
            Map env = new HashMap();
            env.put(IFSConstants.FS_USER_PROVIDER_ENV_LOGOUT_KEY, reqLogout);
            String userID = accountInst.getUserID(acctkey, realm, env);
            if (userID == null) {
                // could not find userDN, search using other domain
                // for backward compitability
                if (hostedRole.equalsIgnoreCase(IFSConstants.SP)) {
                    acctkey = new FSAccountFedInfoKey(reqLogout.getProviderId(),
                        opaqueHandle);
                } else {
                    acctkey = new FSAccountFedInfoKey(
                        associatedDomain, opaqueHandle);
                }
                userID = accountInst.getUserID(acctkey, realm, env);
            }
            if (userID == null) {
                FSUtils.debug.message("UserID is null");
                return null;
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("user id is "+ userID);
            }
            return userID;
        } catch(FSAccountMgmtException e) {
            FSUtils.debug.message("In FSAccountMgmtException :: ", e);
            return null;
        } 
    }

    /*
     * Cleans the FSSessionMap when the session token expires, idles out and/or 
     * when the user has closed his browser without actually performing a 
     * logout.
     * @param token the session token used to identify the user's 
     *  session
     * @param metaAlias the hosted provider performing logout
     */
    public static void removeTokenFromSession(
        Object token, String metaAlias)
    {
        String univId = "";
        String tokenId = "";
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            univId = sessionProvider.getPrincipalName(token);
            tokenId = sessionProvider.getSessionID(token);
        } catch (SessionException e) {
            if (FSUtils.debug.warningEnabled()) {
                FSUtils.debug.warning(
                    "SessionException in removeTokenFromSession", e);
            }
            return;
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("Entered removeTokenFromSession for user: " +
                univId);
        }
        FSSessionManager sessionMgr = 
            FSSessionManager.getInstance(metaAlias);
        FSSession currentSession = sessionMgr.getSession(univId, tokenId);
        if (currentSession != null) {
            sessionMgr.removeSession(univId, currentSession);
        }
    }

    /**
     * Builds signed logout response.
     * @param retURL logout return url
     * @param bArgStatus logout status
     * @param minorVersion minor version of the response should be set to
     * @param hostedConfig hosted provider's extended meta
     * @param hostedEntityId hosted provider's entity id
     * @param userID user id
     * @return signed logout response in string format
     */
    private static String buildSignedResponse(
        String retURL,
        String bArgStatus,
        int minorVersion,
        BaseConfigType hostedConfig,
        String hostedEntityId,
        String userID) 
    {
        try {
            String inResponseTo = "";
            String logoutStatus = "";
            String relayState = "";
            // If userID exists read ReturnManager
            // If manager has entry use that ResponseTo field else default
            FSLogoutResponse responseLogout = new FSLogoutResponse();
            responseLogout.setID(IFSConstants.LOGOUTID);
            if (userID != null) {
                FSReturnSessionManager mngInst =
                    FSReturnSessionManager.getInstance(
                        hostedConfig.getMetaAlias());
                HashMap providerMap = new HashMap();
                if (mngInst != null) {
                    providerMap = mngInst.getUserProviderInfo(userID);
                }
                if (providerMap != null) {
                    inResponseTo =
                        (String) providerMap.get(IFSConstants.RESPONSE_TO);
                    relayState = (String)
                        providerMap.get(IFSConstants.LOGOUT_RELAY_STATE);
                    logoutStatus =
                        (String) providerMap.get(IFSConstants.LOGOUT_STATUS);
                    inResponseTo =
                        (String) providerMap.get(IFSConstants.RESPONSE_TO);
                    mngInst.removeUserProviderInfo(userID);
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("Deleted " + userID +
                            " from return list");
                    }
                    responseLogout.setResponseTo(inResponseTo);
                    responseLogout.setRelayState(relayState);
                    responseLogout.setProviderId(hostedEntityId);
                    responseLogout.setStatus(logoutStatus);
                } else {
                    responseLogout.setStatus(bArgStatus);
                    responseLogout.setProviderId(hostedEntityId);
                }
            } else {
                responseLogout.setStatus(bArgStatus);
                responseLogout.setProviderId(hostedEntityId);
            }
            responseLogout.setMinorVersion(minorVersion);

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
                            "FSLogoutUtil::buildSignedResponse:" +
                            "couldn't obtain this site's cert alias.");
                    }
                    throw new SAMLResponderException(
                        FSUtils.bundle.getString(IFSConstants.NO_CERT_ALIAS));

                }
                urlEncodedResponse =
                    FSSignatureUtil.signAndReturnQueryString(
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
                FSUtils.debug.message("FSLogoutUtil : Response to be sent : " +
                    redirectURL.toString());
            }
            return redirectURL.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Determines the return location and redirects based on
     * logout Return URL of the provider that sent the logout request.
     */
    protected static void returnToSource(
        HttpServletResponse response,
        ProviderDescriptorType remoteDescriptor,
        String bLogoutStatus,
        String commonErrorPage,
        int minorVersion,
        BaseConfigType hostedConfig,
        String hostedEntityId,
        String userID)
    {
        try {
            String retURL = null;
            if (remoteDescriptor != null) {
                retURL = remoteDescriptor.getSingleLogoutServiceReturnURL();
                if (retURL == null || retURL.length() < 1) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("returnToSource returns sendError"                            + "as source provider is unknown");
                    }
                    FSServiceUtils.showErrorPage(response,
                                commonErrorPage,
                                IFSConstants.LOGOUT_FAILED,
                                IFSConstants.METADATA_ERROR);
                    return;
                } else {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("returnToSource returns URL : " +
                            retURL);
                    }
                    String responseURL = buildSignedResponse(
                        retURL,
                        bLogoutStatus,
                        minorVersion,
                        hostedConfig,
                        hostedEntityId,
                        userID);
                    response.sendRedirect(responseURL);
                    return;
                }
            }
            FSUtils.debug.message("Meta Manager instance is null");
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                FSUtils.bundle.getString("unableToReturnToSource"));
            return;
        } catch(IOException exx) {
            FSUtils.debug.error(
                "Redirect/sendError failed. Control halted", exx);
        }
    }

    /**
     * Returns the hosted provider's failure page to the user.
     * @param request the <code>HttpServletRequest</code> object
     * @param response the <code>HttpServletResponse</code> object
     * @param providerAlias the provider alias corresponding to the hosted
     *  provider
     */
    protected static void sendErrorPage(HttpServletRequest request,
        HttpServletResponse response,
        String providerAlias)
    {
        try {
            String retURL = "";
            String realm = IDFFMetaUtils.getRealmByMetaAlias(providerAlias);
            if (metaManager != null) {
                String hostedRole = metaManager.getProviderRoleByMetaAlias(
                    providerAlias);
                String hostedEntityId = metaManager.getEntityIDByMetaAlias(
                    providerAlias);
                BaseConfigType hostedConfig = null;
                if (hostedEntityId != null &&
                    IFSConstants.IDP.equalsIgnoreCase(hostedRole))
                {
                    hostedConfig = metaManager.getIDPDescriptorConfig(
                        realm, hostedEntityId);
               } else if (hostedEntityId != null &&
                    IFSConstants.SP.equalsIgnoreCase(hostedRole))
                {
                    hostedConfig = metaManager.getSPDescriptorConfig(
                        realm, hostedEntityId);
                }
                retURL = FSServiceUtils.getLogoutDonePageURL(
                    request, hostedConfig, providerAlias);
                if (retURL == null || retURL.length() < 1) {
                    FSServiceUtils.showErrorPage(
                        response,
                        FSServiceUtils.getErrorPageURL(
                            request, hostedConfig, providerAlias),
                        IFSConstants.LOGOUT_FAILED,
                        IFSConstants.METADATA_ERROR);
                } else {
                    StringBuffer finalReturnURL = new StringBuffer();
                    finalReturnURL.append(retURL);
                    char delimiter;
                    if (retURL.indexOf(IFSConstants.QUESTION_MARK) < 0){
                        delimiter = IFSConstants.QUESTION_MARK;
                    } else {
                        delimiter = IFSConstants.AMPERSAND;
                    }
                    finalReturnURL.append(delimiter)
                        .append(IFSConstants.LOGOUT_STATUS)
                        .append(IFSConstants.EQUAL_TO)
                        .append(IFSConstants.LOGOUT_FAILURE);
                    response.sendRedirect(finalReturnURL.toString());
                }
                return;
            } else {
                FSUtils.debug.error("Meta manager instance is null");
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                    FSUtils.bundle.getString("failedToReadDataStore"));
                return;
            }
        } catch(IOException ex) {
            FSUtils.debug.error(
                "FSSingleLogoutServlet: IOException caught:", ex);
            return;
        }catch(IDFFMetaException e) {
            FSUtils.debug.error(
                "FSSingleLogoutServlet:IDFFMetaException:", e);
            return;
        }
    }

    /**
     * Removes current session partner from the session partner list.
     *
     * @param metaAlias meta alias of the hosted provider
     * @param remoteEntityId id of the remote provider
     * @param ssoToken session object of the principal who presently login
     * @param userID id of the principal
     */
    public static void removeCurrentSessionPartner(
        String metaAlias,
        String remoteEntityId,
        Object ssoToken,
        String userID)
    {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSLogoutUtil.removeCSP, hosted=" +
                metaAlias + ", remote=" + remoteEntityId +
                ", userID=" + userID);
        }
        FSSessionManager sessionManager =
            FSSessionManager.getInstance(metaAlias);
        FSSession session = sessionManager.getSession(ssoToken);
        FSLogoutUtil.cleanSessionMapPartnerList(
            userID, remoteEntityId, metaAlias, session);
    }
  
    /**
     * Returns true if this is IDP initiated profiles, false otherwise.
     * @param profile profile to be checked.
     * @return true if specified profile is IDP initiated, false otherwise.
     */
    public static boolean isIDPInitiatedProfile(String profile) {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSLogoutUtil.isIDPInitiatedProfile: proto="
                    + profile);
        }
        if ((profile != null)  &&
            ((profile.equals(IFSConstants.LOGOUT_IDP_REDIRECT_PROFILE) ||
            (profile.equals(IFSConstants.LOGOUT_IDP_SOAP_PROFILE)) ||
            (profile.equals(IFSConstants.LOGOUT_IDP_GET_PROFILE))))) {
            return true;
        } else {
            return false;
        }
    }

}

