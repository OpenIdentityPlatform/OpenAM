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
 * $Id: NotenforcedIPHelper.java,v 1.2 2008/06/25 05:51:40 qcheng Exp $
 *
 */

package com.sun.identity.agents.common;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Module;
import com.sun.identity.agents.arch.SurrogateBase;
import com.sun.identity.agents.util.AgentCache;


/**
 * Class <code>NotenforcedIPHelper</code> provides the necessary functionalities
 * to match a client ip address against a list of predefined ip addresses in
 * agent configuration
 *
 *
 */
public class NotenforcedIPHelper extends SurrogateBase
        implements INotenforcedIPHelper {
    
    public NotenforcedIPHelper(Module module) {
        super(module);
    }
    
    public void initialize(boolean cacheEnabled, int maxSize,
            boolean invertList, String[] notenforcedIPs)
            throws AgentException {
        setCacheEnabledFlag(cacheEnabled);
        setInvertNotenforcedFlag(invertList);
        initCache(maxSize);
        
        String [] ips = new String[notenforcedIPs.length];
        if (notenforcedIPs.length > 0) {
            for (int i = 0; i < notenforcedIPs.length; i++) {
                if (!(notenforcedIPs[i].regionMatches(true, 0,
                        LOOP_BACK_PREFIX, 0, LOOP_BACK_PREFIX.length()))) {
                    ips[i] = notenforcedIPs[i];
                } else {
                    if (isLogMessageEnabled()) {
                        logMessage("NotenforcedIPHelper: Ignoring Loopback "
                                + "address: " + notenforcedIPs[i]);
                    }
                }
            }
        }
        
        CommonFactory cf = new CommonFactory(getModule());
        setMatcher(cf.newPatternMatcher(ips));
        
        // Set the handler active if addresses specified in config and
        // inversion is false
        if ((ips.length == 0) && !isInverted()) {
            setActive(false);
        } else {
            setActive(true);
        }
        
        if(isLogMessageEnabled()) {
            logMessage("NotenforcedIPHelper: isActive: "
                    + isActive()
                    + ", isInverted: " + this.isInverted());
            for(int i=0; i<ips.length; i++) {
                logMessage("NotenforcedIPHelper: next ip: "
                        + ips[i]);
            }
        }
    }
    
    public boolean isNotenforced(String clientIP) {
        
        boolean result = false;
        boolean matchStatus  = false;
        Object  enforcedCacheEntry = null;
        Object  notEnforcedCacheEntry = null;
        
        enforcedCacheEntry = getEnforcedCacheEntry(clientIP);
        notEnforcedCacheEntry = getNotenforcedCacheEntry(clientIP);
        
        //check in enforced cache
        if(enforcedCacheEntry != null) {
            if(isLogMessageEnabled()) {
                logMessage("NotenforcedIPHelper.isNotenforced(): "  + clientIP
                        + " found in enforced cache");
            }
            
            //check in not enforced cache
        } else if(notEnforcedCacheEntry != null) {
            result = true;
            if(isLogMessageEnabled()) {
                logMessage("NotenforcedIPHelper.isNotenforced(): "+ clientIP
                        + " found in not-enforced cache");
            }
            
            //not found in either cache
        } else {
            
            //try to match against specified addresses in Agent Config
            matchStatus = getMatcher().match(clientIP);
            if ( matchStatus ) {
                //there is a match
                if(isLogMessageEnabled()) {
                    logMessage("NotenforcedIPHelper.isNotenforced(): IP "
                            + clientIP
                            + " matches an IP in the Not-Enforced list");
                }
                
                if ( isInverted() ) {//if it is inverted, add to enforced cache
                    addToEnforcedListCache(clientIP, clientIP);
                    
                } else { //if not inverted add to not enforced cache
                    result = true;
                    addToNotenforcedListCache(clientIP, clientIP);
                    
                }
                
            } else { //if there is no match
                
                if(isLogMessageEnabled()) {
                    logMessage("NotenforcedIPHelper.isNotenforced(): "
                            + "IP: " + clientIP
                            + " Does Not match any Not-Enforced IP");
                }
                
                if(isInverted()) {//if not inverted add to not enforced cache
                    result = true;
                    addToNotenforcedListCache(clientIP, clientIP);
                } else {
                    addToEnforcedListCache(clientIP, clientIP);
                }
            }
        }
        
        if(isLogMessageEnabled()) {
            logMessage("NotenforcedIPHelper.isNotenforced(): "
                    + clientIP + " => "        + result);
        }
        
        return result;
        
    }
    
    /**
     * Returns a boolean value indicating if the NotenforcedIP manager is
     * active or not.
     * <br>
     * NOTE: The NotenforcedIP manager will be inactive when the not enforced ip
     * addresses are not specified in the agent configuration
     *
     * @return true if the NotenforcedIPHelper is active, false otherwise.
     */
    public boolean isActive() {
        return _isActive;
    }
    
    
    private void setActive(boolean status) {
        _isActive = status;
    }
    
    private void initCache(int maxSize) {
        if(isCacheEnabled()) {
            //Initialize both enforced and notenforced caches
            setNotenforcedCache(new AgentCache("NotenofrcedList", maxSize));
            setEnforcedCache(new AgentCache("EnforcedList", maxSize));
            setMaxCacheSize(maxSize);
            
            if(isLogMessageEnabled()) {
                logMessage("NotenforcedIPHelper.initCache(): "
                        + "Caching is Enabled. Cache size => "
                        + getMaxCacheSize() + " entries");
            }
            
        } else { //cache is disabled
            
            if(isLogMessageEnabled()) {
                logMessage("NotenforcedIPHelper.initCache(): " +
                        "Caching is Disabled");
            }
        }
    }
    
    /**
     * Method getCacheEntry returns the value object associated with
     * the supplied cache entry key
     *
     * @param key name in String format
     * @param cache is the CacheManager object to get the value from
     * @return result an Object containing the value of the specified key
     */
    private Object getCacheEntry(String key, AgentCache cache) {
        return cache.get(key);
    }
    
    private void addCacheEntry( String key,
            String value,
            AgentCache cache) {
        cache.put(key, value);
    }
    
    /**
     * Method addToNotenforcedListCache
     * Add the supplied ip address to Not Enforced IP List Cache
     * @param key String key
     * @param value String value of the key
     *
     */
    private void addToNotenforcedListCache(String key, String value) {
        if(isCacheEnabled()) {
            addCacheEntry(key, value, getNotenforcedCache());
        }
    }
    
    /**
     * Method addToEnforcedListCache
     * Add the supplied ip address to Enforced IP List Cache
     * @param key String key
     * @param value String value of the key
     *
     */
    private void addToEnforcedListCache(String key, String value) {
        if(isCacheEnabled()) {
            addCacheEntry(key, value, getEnforcedCache());
        }
    }
    
    /**
     * Method getEnforcedCacheEntry
     * @param key a String key whose value is to be looked up
     * @return result Object value of the mapped key in this cache; or null if
     * the key does not map to a value in the cache or caching is disabled
     */
    private Object getEnforcedCacheEntry(String key) {
        Object result = null;
        if(isCacheEnabled()) {
            result = getCacheEntry(key, getEnforcedCache());
        }
        return result;
    }
    
    /**
     * Method getNotenforcedCacheEntry
     *
     * @param key String whose value is to be looked up
     * @return result Object value of supplied key in cache  or null if
     * the key does not map to a value in the cache or caching is disabled
     *
     */
    private Object getNotenforcedCacheEntry(String key) {
        Object result = null;
        if(isCacheEnabled()) {
            result = getCacheEntry(key, getNotenforcedCache());
        }
        return result;
    }
    
    private void setNotenforcedCache(AgentCache cache) {
        _notEnforcedCache = cache;
    }
    
    private void setEnforcedCache(AgentCache cache) {
        _enforcedCache = cache;
    }
    
    private AgentCache getNotenforcedCache() {
        return _notEnforcedCache;
    }
    
    private AgentCache getEnforcedCache() {
        return _enforcedCache;
    }
    
    private void setCacheEnabledFlag(boolean cacheEnabled) {
        _cacheEnabled = cacheEnabled;
    }
    
    private boolean isCacheEnabled() {
        return _cacheEnabled;
    }
    
    private int getMaxCacheSize() {
        return _maxCacheSize;
    }
    
    private void setMaxCacheSize(int size) {
        _maxCacheSize = size;
    }
    
    private boolean isInverted() {
        return _invertNotenforcedFlag;
    }
    
    private void setInvertNotenforcedFlag(boolean invertList) {
        _invertNotenforcedFlag = invertList;
    }
    
    private IPatternMatcher getMatcher() {
        return _matcher;
    }
    
    private void setMatcher(IPatternMatcher matcherObj) {
        _matcher = matcherObj;
    }
    
    private int       _maxCacheSize;
    private boolean   _invertNotenforcedFlag;
    private boolean   _cacheEnabled;
    private boolean   _isActive;
    
    private AgentCache _notEnforcedCache = null;
    private AgentCache _enforcedCache = null;
    private IPatternMatcher _matcher;
    
    public static final String LOOP_BACK_PREFIX = "127";
}
