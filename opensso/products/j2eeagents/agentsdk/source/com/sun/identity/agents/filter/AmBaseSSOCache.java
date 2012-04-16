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
 * $Id: AmBaseSSOCache.java,v 1.3 2008/07/02 18:27:11 leiming Exp $
 *
 */

package com.sun.identity.agents.filter;

import java.security.Principal;
import java.util.Hashtable;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenEvent;
import com.iplanet.sso.SSOTokenListener;
import com.sun.identity.agents.arch.AgentBase;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.common.CommonFactory;
import com.sun.identity.agents.common.ISSOTokenValidator;
import com.sun.identity.agents.common.SSOValidationResult;

/**
 * The base class for agent SSO caches
 */
public abstract class AmBaseSSOCache extends AgentBase
        implements IFilterConfigurationConstants, IAmSSOCache {
    
    public AmBaseSSOCache(Manager manager) throws AgentException {
        super(manager);
    }
    
    public void initialize() throws AgentException {
        CommonFactory cf = new CommonFactory(getModule());
        setSSOTokenValidator(cf.newSSOTokenValidator());
    }
    
    private String getSSOTokenForUser(Principal principal) {
        
        String result = null;
        
        if(principal != null) {
            result = getSSOTokenForUser(principal.getName());
        }
        
        return result;
    }
    
    public void addSSOCacheEntry(SSOValidationResult ssoValidationResult)
    throws AgentException {
        
        if(ssoValidationResult.isValid()) {
            String name = ssoValidationResult.getUserPrincipal().toLowerCase();
            String transportString = ssoValidationResult.getTransportString();
            
            try {
                synchronized(LOCK) {
                    String ssoToken = getSSOTokenForUserInternal(name);
                    
                    // If user SSO is not equal to cached SSO, only then
                    // add SSOToken Listener
                    if(!ssoValidationResult.getSSOTokenString().equals(
                            ssoToken)) {
                        ssoValidationResult.getSSOToken().addSSOTokenListener(
                                new AmSSOCacheTokenListener(name,
                                transportString));
                        
                        getSSOCache().put(name, transportString);
                        
                        if(isLogMessageEnabled()) {
                            logMessage(
                                    "AmBaseSSOCache: cached the sso token for "
                                    + "user principal : " 
                                    + name + "  sso token: "
                                    + ssoValidationResult.getSSOTokenString()
                                    + ", cache size = " + getSSOCache().size());
                        }
                    } else {
                        if(isLogMessageEnabled()) {
                            logMessage("AmBaseSSOCache: User with SSO = "
                                    + ssoValidationResult.getSSOTokenString()
                                    + ", has an entry in AmBaseSSOCache. " 
                                    + "Bypass Cache "
                                    + "user token.");
                        }
                    }
                }
            } catch(SSOException ex) {
                if(isLogWarningEnabled()) {
                    logWarning(
                        "AmBaseSSOCache: Failed to add token listener for user "
                        + name, ex);
                }
            }
        } else {
            throw new AgentException("Attempt to cache invalid SSO Token");
        }
    }
    
    private void removeCacheEntry(String name, String transportString) {
        
        synchronized(LOCK) {
            String cachedTransportString = (String) getSSOCache().get(name);
            
            if((cachedTransportString != null)
            && cachedTransportString.equals(transportString)) {
                getSSOCache().remove(name);
                
                if(isLogMessageEnabled()) {
                    logMessage(
                        "AmBaseSSOCache: removed expired sso cache " 
                        + "entry for user: "
                        + name);
                }
            } else {
                if(isLogWarningEnabled()) {
                    logWarning("AmBaseSSOCache: failed to remove sso token for "
                            + name);
                }
            }
            
            if(isLogMessageEnabled()) {
                logMessage("AmBaseSSOCache: cache size = "
                        + getSSOCache().size());
            }
        }
    }
    
    protected String getSSOTokenForUserInternal(String principalName) {
        
        String result          = null;
        String transportString = null;
        String name = principalName.toLowerCase();
        
        synchronized(LOCK) {
            transportString = (String) getSSOCache().get(name);
        }
        
        if(transportString != null) {
            try {
                SSOValidationResult ssoValidationResult =
                        getSSOTokenValidator().validate(transportString);
                
                if(ssoValidationResult.isValid()) {
                    result = ssoValidationResult.getSSOTokenString();
                } else {
                    removeCacheEntry(name, transportString);
                }
            } catch(Exception ex) {
                if(transportString != null) {
                    removeCacheEntry(name, transportString);
                }
            }
        }
        
        return result;
    }
    
    protected ISSOTokenValidator getSSOTokenValidator() {
        return _ssoTokenValidator;
    }
    
    private void setSSOTokenValidator(ISSOTokenValidator validator) {
        _ssoTokenValidator = validator;
    }
    
    private ISSOTokenValidator _ssoTokenValidator;
    
    /////////////////////////////////////////////////////////
    // Static Service Methods for the Cache
    /////////////////////////////////////////////////////////
    
    private static Hashtable getSSOCache() {
        return _ssoCache;
    }
    
    private static Hashtable    _ssoCache = new Hashtable();
    private static final String LOCK      = "amSSOCache_Lock";
    
    ///////////////////////////////////////////////////////////
    // SSO token Listener
    ///////////////////////////////////////////////////////////
    class AmSSOCacheTokenListener implements SSOTokenListener {
        
        AmSSOCacheTokenListener(String name, String transportString) {
            setUserName(name);
            setTransportString(transportString);
        }
        
        public void ssoTokenChanged(SSOTokenEvent ssoTokenEvent) {
            
            removeCacheEntry(getUserName(), getTransportString());
            
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
                    
                    logMessage("AmSSOCacheTokenListener: User " + getUserName()
                    + " Token Event: " + typeStr);
                } catch(SSOException ssoEx) {
                    if(isLogWarningEnabled()) {
                        logWarning(
                                "AmSSOCacheTokenListener: Exception caught",
                                ssoEx);
                    }
                }
            }
        }
        
        private void setUserName(String name) {
            _userName = name;
        }
        
        private String getUserName() {
            return _userName;
        }
        
        private void setTransportString(String transportString) {
            _transportString = transportString;
        }
        
        private String getTransportString() {
            return _transportString;
        }
        
        private String _userName;
        private String _transportString;
    }
}

