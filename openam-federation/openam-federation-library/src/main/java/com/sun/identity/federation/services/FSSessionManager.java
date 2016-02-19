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
 * $Id: FSSessionManager.java,v 1.6 2009/08/03 18:18:36 bigfatrat Exp $
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 */


package com.sun.identity.federation.services;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.shared.stats.Stats;
import com.sun.identity.shared.Constants;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.common.SystemTimerPool;
import com.sun.identity.common.TimerPool;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.liberty.ws.meta.jaxb.IDPDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;
import com.sun.identity.plugin.monitoring.FedMonAgent;
import com.sun.identity.plugin.monitoring.FedMonIDFFSvc;
import com.sun.identity.plugin.monitoring.MonitorManager;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * The class <code>FSSessionManager</code> is a <code>final</code> class
 * that provides interfaces to manage <code>FSSession</code>s.
 * <p>
 * It is a singleton class; an instance of this class can be obtained by
 * calling <code>FSSessionManager.getInstance()</code>.
 */
public final class FSSessionManager {

    // property name for request cleanup internal
    private static final String REQUEST_CLEANUP_INTERVAL = 
        "com.sun.identity.federation.request.cleanup_interval";
    // default value for request cleanup internal
    private static final long DEFAULT_REQUEST_CLEANUP_INTERVAL = 300000;
    // property name for request time out
    private static final String REQUEST_TIMEOUT = 
        "com.sun.identity.federation.request.timeout";
    // default value for request time out
    private static final long DEFAULT_REQUEST_TIMEOUT = 300000;
    
    // Singleton instance of FSSessionManager
    private static Map instanceMap = new HashMap ();
    // used to store list of FSSession for a user
    // key : user DN, value List of FSSession object
    private Map userIDSessionListMap = 
        Collections.synchronizedMap(new HashMap ());
    private Map idAuthnRequestMap = 
        Collections.synchronizedMap(new HashMap ());
    private Map idLocalSessionTokenMap = 
        Collections.synchronizedMap(new HashMap ()); 
    private Map idDestnMap = 
        Collections.synchronizedMap(new HashMap ());
    private String hostEntityId = null;
    private String realm = null;
    private Map relayStateMap =
        Collections.synchronizedMap(new HashMap());
    private Map proxySPDescMap = 
        Collections.synchronizedMap(new HashMap ());
    private Map proxySPAuthnReqMap = 
        Collections.synchronizedMap(new HashMap ());

    private static long cleanupInterval = 0;
    private static long requestTimeout = 0;
    private FSRequestCleanUpRunnable cRunnable = null;

    private FSSessionMapStats dnStats;
    private FSSessionMapStats reqStats;
    private FSSessionMapStats tokenStats;
    private FSSessionMapStats idStats;
    private FSSessionMapStats relayStats;
    /**
     * For managing session statistics.
     */
    public static Stats sessStats = Stats.getInstance("libIDFFSessionMaps");

    private static FedMonAgent agent;
    private static FedMonIDFFSvc idffSvc;

    static {
        try {
            String temp = SystemConfigurationUtil.getProperty(
                REQUEST_CLEANUP_INTERVAL);
            if (temp == null || temp.trim().length() == 0) {
                cleanupInterval = DEFAULT_REQUEST_CLEANUP_INTERVAL;
            } else {
                cleanupInterval = Integer.parseInt(temp) * 1000;
            }
        } catch (Exception e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "Unable to get fed request cleanup prop.", e);
            }
            cleanupInterval = DEFAULT_REQUEST_CLEANUP_INTERVAL;
        }
       
        try {
            String temp = SystemConfigurationUtil.getProperty(REQUEST_TIMEOUT);
            if (temp == null || temp.trim().length() == 0) {
                requestTimeout = DEFAULT_REQUEST_TIMEOUT;
            } else {
                requestTimeout = Integer.parseInt(temp) * 1000;
            }
        } catch (Exception e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "Unable to get fed request timeout prop.", e);
            }
            requestTimeout = DEFAULT_REQUEST_TIMEOUT;
        }

        agent = MonitorManager.getAgent();
        idffSvc = MonitorManager.getIDFFSvc();
        
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSSessionManager, cleanup interval=" 
                + cleanupInterval + " ms, timeout=" + requestTimeout + " ms");
        }
    }

    /**
     * Gets the <code>RelayState</code> value which maps
     * to the request ID.
     *
     * @param requestID request ID
     * @return the <code>RelayState</code> value
     */
    public String getRelayState(String requestID) {
        return (String) relayStateMap.get(requestID);
    }

    /**
     * Sets the <code>RelayState</code> value with the specified
     * request ID in the <code>relayStateMap</code>.
     *
     * @param requestID request ID
     * @param relayState the <code>RelayState</code> value
     */
    public void setRelayState(String requestID, String relayState) {
        relayStateMap.put(requestID, relayState);
    }

    /**
     * Removes the mapping for this request ID from the
     * <code>relayStateMap</code>.
     *
     * @param requestID request ID
     */
    public void removeRelayState(String requestID) {
        relayStateMap.remove(requestID);
    }
    
    /**
     * Returns authentication request associated with <code>requestID</code>.
     * @param requestID authentication request ID
     * @return authentication request associated with the request ID
     */
    public FSAuthnRequest getAuthnRequest (String requestID){
        FSUtils.debug.message ("FSSessionManager.getAuthnRequest: Called");
        return (FSAuthnRequest)idAuthnRequestMap.get (requestID);
    }
    
    /**
     * Sets authentication request.
     * @param requestID authentication request ID
     * @param authnRequest authentication request
     */
    public void setAuthnRequest (String requestID, FSAuthnRequest authnRequest){
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSSessionManager.setAuthnRequest: Called, ID=" + requestID );
        }
        removeAuthnRequest (requestID);
        idAuthnRequestMap.put (requestID, authnRequest);
        if (cRunnable != null) {
            cRunnable.addElement(requestID);
        }
    }
    
    /**
     * Removes an authentication request.
     * @param requestID ID of the request to be removed
     */
    public void removeAuthnRequest (String requestID){
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message (
                "FSSessionManager.removeAuthnRequest: Called, ID=" + requestID);
        }
        idAuthnRequestMap.remove(requestID);
        idDestnMap.remove(requestID);
        if ((agent != null) && agent.isRunning() && (idffSvc != null)) {
            idffSvc.setIdDestn((long)idDestnMap.size());
        }
        if (cRunnable != null) {
            cRunnable.removeElement(requestID);
        }
    }

    /**
     * Returns local token associated with the request ID.
     * @param requestID request ID
     * @return session object associated with the request ID
     */
    public Object getLocalSessionToken(String requestID) {
        FSUtils.debug.message ("FSSessionManager.getLocalSessionToken: Called");
        return idLocalSessionTokenMap.get(requestID);
    }

    /**
     * Sets local token.
     * @param requestID request ID
     * @param localSession token to be set
     */
    public void setLocalSessionToken (String requestID, Object localSession){
        FSUtils.debug.message ("FSSessionManager.setLocalSessionToken: Called");
        idLocalSessionTokenMap.put (requestID, localSession);
        if ((agent != null) && agent.isRunning() && (idffSvc != null)) {
            idffSvc.setIdLocalSessToken((long)idLocalSessionTokenMap.size());
        }
    }

    /**
     * Removes a local token associated with <code>requestID</code>.
     * @param requestID request ID
     */
    public void removeLocalSessionToken (String requestID){
        FSUtils.debug.message(
            "FSSessionManager.removeLocalSessionToken: Called");
        idLocalSessionTokenMap.remove(requestID);
        if ((agent != null) && agent.isRunning() && (idffSvc != null)) {
            idffSvc.setIdLocalSessToken((long)idLocalSessionTokenMap.size());
        }
    }

    /**
     * Returns IDP's entity ID associated with <code>requestID</code>.
     * @param requestID request ID
     * @return identity provider's entity ID
     */
    public String getIDPEntityID(String requestID){
        FSUtils.debug.message("FSSessionManager.getIDPEntityID: Called");
        return (String) idDestnMap.get (requestID);
    }
    
    /**
     * Sets IDP's entity ID.
     * @param requestID authentication request ID
     * @param idpEntityId identity provider's entity ID to be set
     */
    public void setIDPEntityID(
        String requestID,
        String idpEntityId)
    {
        FSUtils.debug.message ("FSSessionManager.setIDPEntityID");
        idDestnMap.put (requestID, idpEntityId);
        if ((agent != null) && agent.isRunning() && (idffSvc != null)) {
            idffSvc.setIdDestn((long)idDestnMap.size());
        }
    }

    /**
     * Sets proxy service provider descriptor.
     * @param requestID authentication request ID
     * @param spDescriptor provider descriptor to be set
     */
    public void setProxySPDescriptor(
        String requestID,
        SPDescriptorType spDescriptor) 
    {
        proxySPDescMap.put(requestID, spDescriptor);
    }

    /**
     * Returns proxy service provider descriptor.
     * @param requestID request ID
     * @return provider descriptor
     */
    public SPDescriptorType getProxySPDescriptor(String requestID) { 
        return (SPDescriptorType)proxySPDescMap.get(requestID);
    }

    /**
     * Returns proxy authentication request.
     * @param requestID authentication request ID
     */
    public FSAuthnRequest getProxySPAuthnRequest(String requestID) {
        return (FSAuthnRequest)proxySPAuthnReqMap.get(requestID);
    }

    /**
     * Sets proxy authentication request.
     * @param requestID request ID
     * @param authnRequest proxy authentication request to be set.
     */
    public void setProxySPAuthnRequest(
        String requestID, 
        FSAuthnRequest authnRequest) 
    {
        proxySPAuthnReqMap.put(requestID, authnRequest);
    }

    /**
     * Returns list of sessions associated with <code>userID</code>.
     * @param userID user ID
     * @return list of sessions
     */
    public List getSessionList(String userID){
        FSUtils.debug.message ("FSSessionManager.getSessionList: Called");
        return (List)userIDSessionListMap.get(userID.toLowerCase());
    }
    
    /**
     * Sets session list.
     * @param userID user ID
     * @param sessionList list of sessions to be set
     */
    public void setSessionList (String userID, List sessionList){
        FSUtils.debug.message ("FSSessionManager.setSessionList: Called");
        userIDSessionListMap.put(userID.toLowerCase(), sessionList);
        if ((agent != null) && agent.isRunning() && (idffSvc != null)) {
            idffSvc.setUserIDSessionList((long)userIDSessionListMap.size());
        }
    }
    
    /**
     * Removes session list associated with <code>userID</code>.
     * @param userID user ID
     */
    public void removeSessionList (String userID){
        FSUtils.debug.message ("FSSessionManager.removeSessionList: Called ");
        userIDSessionListMap.remove(userID.toLowerCase());
        if ((agent != null) && agent.isRunning() && (idffSvc != null)) {
            idffSvc.setUserIDSessionList((long)userIDSessionListMap.size());
        }
    }
    
    /**
     * Returns session with <code>sessionID</code> for <code>userID</code>.
     * @param userID user ID
     * @param sessionID session ID
     * @return <code>FSSession</code> object
     */
    public FSSession getSession(String userID, String sessionID) {
        FSUtils.debug.message ("FSSessionManager.getSession: Called ");
        List sessions = getSessionList (userID);
        if (sessions != null){
            synchronized (sessions) {
                Iterator i = sessions.iterator ();
                while (i.hasNext ()) {
                    FSSession session = (FSSession)i.next ();
                    if (session.isEquals(sessionID)){
                        return session;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the session in <code>sessions</code> whose session index is
     * <code>sessionIndex</code>.
     * @param sessions list of <code>FSSession</code>s.
     * @param sessionIndex session index
     * @return <code>FSSession</code> object whose session index is
     *  <code>sessionIndex</code>
     */
    public FSSession getSession(List sessions, String sessionIndex) {
        FSUtils.debug.message("FSSessionManager.getSession(sessionIndex):");
        if (sessions == null || sessionIndex == null) {
            FSUtils.debug.error("FSSessionManager.getSession(sessionIndex):" +
                "sessions or sessionIndex is null");
            return null;
        }

        synchronized (sessions) {
            Iterator i = sessions.iterator ();
            while (i.hasNext ()) {
                FSSession session = (FSSession)i.next ();
                String tmpIndex = session.getSessionIndex();
                if (tmpIndex != null && tmpIndex.equals(sessionIndex)) {
                    return session;
                }
            }
        }

        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSSessionManager.getSession(sessionIndex):" +
                "No session found for the given session index.");
        }
        return null;
    }
    
    /**
     * Returns session associated with <code>token</code>.
     * @param token session object
     * @return <code>FSSession</code> associated with the token
     */
    public FSSession getSession(Object token){
        FSUtils.debug.message ("FSSessionManager.getSession: Called");
        if (token == null) {
            return null;
        }
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            String univId = sessionProvider.getPrincipalName(token);
            String sessionID = sessionProvider.getSessionID(token);
            return getSession(univId, sessionID);
        } catch (Exception e) {
            FSUtils.debug.error("FSSessionManager.getSession(token) : ", e);
            return null;
        }
    }
    
    /**
     * Removes <code>entityID</code> from <code>userID</code>'s 
     * session partner list.
     * @param userID user ID
     * @param entityID entity ID of the provider to be removed
     * @param localSession <code>FSSession</code> object
     */
    public void removeProvider(
        String userID, String entityID, FSSession localSession) 
    {
        FSUtils.debug.message ("FSSessionManager.removeProvider: Called ");
        if (localSession != null) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSessionManager.removeProvider: " +
                    "localSession is not null");
            }
            localSession.removeSessionPartner(entityID);
            return;
        }
 
        List sessions = getSessionList(userID);
        if (sessions != null) {
            synchronized (sessions) {
                Iterator i = sessions.iterator();
                while (i.hasNext ()) {
                    FSSession session = (FSSession)i.next ();
                    if (session != null){
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("removeSessionPartner" +
                                entityID);
                        }
                        session.removeSessionPartner(entityID);
                    }
                }
            }
        }
    }
    
    /**
     * Removes a federation session of an user.
     * @param userID user ID
     * @param session federation session to be removed
     */
    public void removeSession (String userID, FSSession session){
        FSUtils.debug.message ("FSSessionManager.removeSession: Called");
        List sessions = getSessionList(userID);
        if (sessions != null){
            synchronized (sessions) {
                Iterator i = sessions.iterator ();
                while (i.hasNext ()) {
                    FSSession oldsession = (FSSession)i.next ();
                    if (oldsession.equals(session)) {
                        sessions.remove(oldsession);
                        break;
                    }
                }
            }
            if (sessions.isEmpty()) {
                removeSessionList(userID);
            }
        }
    }
    
    /**
     * Adds a federation session to a user.
     * @param userID user ID
     * @param session federation session to be added
     */
    public void addSession (String userID, FSSession session){
        FSUtils.debug.message ("FSSessionManager.addSession: Called");
        List sessions = getSessionList(userID);
        if (sessions != null) {
            synchronized (sessions) {
                Iterator i = sessions.iterator ();
                while (i.hasNext ()) {
                    FSSession oldsession = (FSSession)i.next();
                    if (oldsession.equals(session)){
                        sessions.remove(oldsession);
                        break;
                    }
                }
                sessions.add(session);
                return;
            }
        }
        List newSessionList = new ArrayList ();
        newSessionList.add(session);
        setSessionList(userID, newSessionList);
    }
    
    private FSSessionManager (String metaAlias){
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message ("FSSessionManager(): created " + metaAlias);
        }
        realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
        try {
            IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
            hostEntityId = metaManager.getEntityIDByMetaAlias(metaAlias);
        } catch (Exception ie) {
            if (FSUtils.debug.warningEnabled()) {
                FSUtils.debug.warning("FSSessionManager constructor: couldnot "
                    + "obtain hosted entity ID:", ie);
            }
        }

        if (sessStats.isEnabled()) {
            dnStats = new FSSessionMapStats(userIDSessionListMap,
                "userIDSessionListMap", realm, hostEntityId);
            sessStats.addStatsListener(dnStats);

            reqStats = new FSSessionMapStats(idAuthnRequestMap,
                "idAuthnRequestMap", realm, hostEntityId);
            sessStats.addStatsListener(reqStats);

            tokenStats = new FSSessionMapStats(idLocalSessionTokenMap,
                "idLocalSessionTokenMap", realm, hostEntityId);
            sessStats.addStatsListener(tokenStats);

            idStats = new FSSessionMapStats(idDestnMap,
                "idDestnMap", realm, hostEntityId);
            sessStats.addStatsListener(idStats);

            relayStats = new FSSessionMapStats(relayStateMap,
                "relayStateMap", realm, hostEntityId);
            sessStats.addStatsListener(relayStats);
        }

        if (cleanupInterval != 0 &&
            requestTimeout != 0 &&
            SystemConfigurationUtil.isServerMode()) 
        {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSessionManager.getInstance: " +
                    "start cleanup thread for " + hostEntityId +
                    " in realm " + realm);
            }
            cRunnable = new FSRequestCleanUpRunnable(idAuthnRequestMap,
                idDestnMap, cleanupInterval, requestTimeout);
            SystemTimerPool.getTimerPool().schedule(cRunnable, new Date(
                ((currentTimeMillis() + cleanupInterval) / 1000) *
                1000));
        }
    }
    
    /**
     * Gets the singleton instance of <code>FSSessionManager</code>.
     * @param metaAlias hosted provider's metaAlias
     * @return The singleton <code>FSSessionManager</code> instance
     *  for this provider
     */
    public static synchronized FSSessionManager getInstance (String metaAlias){
        // not throwing any exception
        if (FSUtils.debug.messageEnabled () ) {
            FSUtils.debug.message ("FSSessionManager.getInstance: Called " 
                + metaAlias);
        }
        if (metaAlias == null) {
            FSUtils.debug.error(
                "FSSessionManager.getInstance: null provider meta alias");
            return null;
        }
        FSSessionManager instance = 
            (FSSessionManager)instanceMap.get(metaAlias);
        if (instance == null) {
            if (FSUtils.debug.messageEnabled () ) {
                FSUtils.debug.message ("FSSessionManager.getInstance: " +
                    "new instance of FSSessionManager: " + metaAlias);
            }
            instance = new FSSessionManager(metaAlias);
            synchronized (instanceMap) {
                instanceMap.put(metaAlias, instance);
            }
        }
        return (instance);
    }
}// end class
