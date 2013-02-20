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
 * $Id: AmRealmMembershipCache.java,v 1.2 2008/06/25 05:51:58 qcheng Exp $
 *
 */

package com.sun.identity.agents.realm;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenEvent;
import com.iplanet.sso.SSOTokenListener;
import com.sun.identity.agents.arch.AgentBase;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.common.SSOValidationResult;


/**
 * The agent realm cache that stores the user membership information.
 * This class is not only used by some agents, maybe tomcat and IBM
 * since many containers keep the membership info fore a user as part of 
 * credentials or somewhere. It is optionally enabled by overideing the 
 * ServiceRseolver method public boolean getRealmMembershipCacheFlag() inside 
 * a appserver containers agent specific serviceresolver and setting the 
 * flag to true. When set to true, the AMRealmBase class will use the cache.
 */
public class AmRealmMembershipCache extends AgentBase {
    
    public AmRealmMembershipCache(Manager manager) throws AgentException {
        super(manager);
         if (getResolver().getRealmMembershipCacheFlag()) {
             _membershipCache = new Hashtable();   //SEAN -copied from old initialize() method
            if(isLogMessageEnabled()) {
                logMessage("AmRealmMembershipCache constructor: "
                    + "getRealmMembershipCacheFlag is true so setting cache to"
                    + " new empty memberships cache");
            }
         }           
    }
    
    /** 
     * @return null if getRealmMembershipCacheFlag = false 
     *         and if getRealmMembershipCacheFlag = true then returns the set of
     *         user memberships.
     **/
    public Set getMembershipFromCache(String userName) {
        Set result = null;
        
        if (getResolver().getRealmMembershipCacheFlag()) {
            synchronized(LOCK) {
                result = (Set) getMembershipCache().get(userName);
            }
        
            if (result == null) {
                result = Collections.EMPTY_SET;
                if (isLogWarningEnabled()) {
                    logWarning("AmRealmMembershipCache.getMembershipFromCache:"
                        + " No memberships found for: "
                        + userName + ", may be expired!");
                }
            }     
            if (isLogMessageEnabled()) {
                logMessage("AmRealmMembershipCache.getMembershipFromCache:"
                    + " Lookup for " + userName 
                    + " found memberships: " + result);
            }
        }
        return result;
    }
    
    /** 
     * If getRealmMembershipCacheFlag = false this method does no processing,
     * and listener is not registered.
     * If getRealmMembershipCacheFlag = true then adds user to memberships 
     * cache and registers this cache entry with a listener so later it can 
     * be removed on a notification.
     */
    public void addMembershipCacheEntry(String userName, Set membership, 
            SSOValidationResult ssoValidationResult) throws AgentException
    {
        if (getResolver().getRealmMembershipCacheFlag()) {
            try {
                ssoValidationResult.getSSOToken().addSSOTokenListener(
                    new AmRealmMembershipCacheListener(userName));
                if (isLogMessageEnabled()) {
                    logMessage("AmRealmMembershipCache.addMembershipCacheEntry:"                          
                    + "  caching membership for user " + userName);
                }
                synchronized(LOCK) {
                    getMembershipCache().put(userName, membership);

                    if(isLogMessageEnabled()) {
                      logMessage("AmRealmMembershipCache.addMembershipCacheEntry:" 
                         + " cache size = "
                         + getMembershipCache().size());
                    }
                }
            } catch (Exception ex) {
                throw new AgentException("Failed to add cache entry", ex);
            }
        }
    }
    
    /*
     * Called by inner class which is a listener for sso token events, which 
     * may cause the user's membership to be removed from cache. This listener
     * mechanism is only way this method should be invoked. Since data is only 
     * added to cache if  getRealmMembershipCacheFlag = true, then if false this
     * method would not be called as listener registration would not have 
     * occurred so notification would never happen either.
     */
    private void removeMembershipCacheEntry(String userName) {
        synchronized(LOCK) {
            getMembershipCache().remove(userName);
            if(isLogMessageEnabled()) {
               logMessage("AmRealmMembershipCache.removeMembershipCacheEntry:"
                        + " removed expired membership"
                        + " cache entry for user: " + userName
                        + ", cache size = " + getMembershipCache().size());
            }
        }    
    }
    
    class AmRealmMembershipCacheListener implements SSOTokenListener {
        
        AmRealmMembershipCacheListener(String userName) {
            setUserName(userName);
        }
        
        public void ssoTokenChanged(SSOTokenEvent ssoTokenEvent) {

            removeMembershipCacheEntry(getUserName());

            if(isLogMessageEnabled()) {
                try {
                    int    type    = ssoTokenEvent.getType();
                    String typeStr = null;

                    switch(type) {

                    case SSOTokenEvent.SSO_TOKEN_DESTROY :
                        typeStr = "SSO_TOKEN_DESTROY";
                        break;

                    case SSOTokenEvent.SSO_TOKEN_IDLE_TIMEOUT :
                        typeStr = "SSO_TOKEN_IDLE_TIMEOUT";
                        break;

                    case SSOTokenEvent.SSO_TOKEN_MAX_TIMEOUT :
                        typeStr = "SSO_TOKEN_MAX_TIMEOUT";
                        break;

                    default :
                        typeStr = "UNKNOWN TYPE EVENT = " + type;
                        break;
                    }

                    logMessage("AmRealmMembershipCacheListener.ssoTokenChanged:"
                            + " User " 
                            + getUserName()
                               + " Token Event: " + typeStr);
                } catch(SSOException ssoEx) {
                    if(isLogWarningEnabled()) {
                        logWarning(
                            "AmRealmMembershipCacheListener.ssoTokenChanged:"
                                + " Exception caught",
                            ssoEx);
                    }
                }
            }
        }
        
        private void setUserName(String userName) {
            _userName = userName;
        }
        
        private String getUserName() {
            return _userName;
        }
        
        private String _userName;
    }
    
    private Hashtable getMembershipCache() {
        return _membershipCache;
    }
    
    private Hashtable _membershipCache = null;
    private static final String LOCK = "amRealm.AmRealmMembershipCache_LOCK";

}
