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
 * $Id: CookieResetInitializer.java,v 1.2 2008/06/25 05:51:44 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import java.util.Map;
import java.util.HashSet;
import java.util.Iterator;

import com.sun.identity.agents.arch.AgentBase;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.common.CommonFactory;
import com.sun.identity.agents.common.ICookieResetInitializer;

/**
 * A <code>CookieResetInitializer</code> encapsulates all the intializations,
 * for the CookieHelper. Collects all the agent cookies and application
 * cookies to generate a reset list.
 */
public class CookieResetInitializer extends AgentBase
        implements IFilterConfigurationConstants, ICookieResetInitializer {
    
    public CookieResetInitializer(Manager manager) throws AgentException {
        super(manager);
        
        HashSet cookieSet = new HashSet();
        cookieSet = getAgentCookies();
        
        boolean cookieResetEnabledFlag = getConfigurationBoolean(
                CONFIG_COOKIE_RESET_ENABLE);
        String[] cookieNames = getConfigurationStrings(
                CONFIG_COOKIE_RESET_LIST);
        Map cookieDomains = getConfigurationMap(CONFIG_COOKIE_RESET_DOMAINS);
        Map cookiePaths = getConfigurationMap(CONFIG_COOKIE_RESET_PATHS);
        
        if (cookieResetEnabledFlag || cookieSet.size() > 0) {
            if (cookieNames != null && cookieNames.length > 0) {
                for(int i = 0; i < cookieNames.length; i++) {
                    cookieSet.add(cookieNames[i]);
                }
            }
        }
        
        getCookieNames().addAll(cookieSet);
        setCookieDomains(cookieDomains);
        setCookiePaths(cookiePaths);
        printCookieSet(cookieSet);
    }
    
    /**
     * Get session and profile attribute cookie names when
     * fetch mode is set to HTTP_COOKIE
     */
    private HashSet getAgentCookies() {
        HashSet agentCookies = new HashSet();
        
        String profileAttrFetchMode = getConfigurationString(
                CONFIG_PROFILE_ATTRIBUTE_FETCH_MODE);
        if (profileAttrFetchMode.equals(STR_MODE_COOKIE)) {
            // add profile attribute cookies to cookieList
            Map profileAttrMap = getConfigurationMap(
                    CONFIG_PROFILE_ATTRIBUTE_MAP);
            if (profileAttrMap != null && profileAttrMap.size() > 0) {
                Iterator it = profileAttrMap.keySet().iterator();
                while (it.hasNext()) {
                    String attrName = (String) it.next();
                    String cookieName = (String) profileAttrMap.get(attrName);
                    agentCookies.add(cookieName);
                }
            }
        }
        
        String sessionAttrFetchMode = getConfigurationString(
                CONFIG_SESSION_ATTRIBUTE_FETCH_MODE);
        if (sessionAttrFetchMode.equals(STR_MODE_COOKIE)) {
            // add session attribute cookies to cookieList
            Map sessionAttrMap = getConfigurationMap(
                    CONFIG_SESSION_ATTRIBUTE_MAP);
            if (sessionAttrMap != null && sessionAttrMap.size() > 0) {
                Iterator it = sessionAttrMap.keySet().iterator();
                while (it.hasNext()) {
                    String attrName = (String) it.next();
                    String cookieName = (String) sessionAttrMap.get(attrName);
                    agentCookies.add(cookieName);
                }
            }
        }
        
        return agentCookies;
    }
    
    public HashSet getCookieNames() {
        return _cookieNames;
    }
    
    public Map getCookieDomains() {
        return _cookieDomains;
    }
    
    public Map getCookiePaths() {
        return _cookiePaths;
    }
    
    private void setCookieNames(HashSet cookieNames) {
        _cookieNames = cookieNames;
    }
    
    private void setCookieDomains(Map cookieDomains) {
        _cookieDomains = cookieDomains;
    }
    
    private void setCookiePaths(Map cookiePaths) {
        _cookiePaths = cookiePaths;
    }
    
    private void printCookieSet(HashSet cookieSet) {
        if (isLogMessageEnabled()) {
            StringBuffer cookieBuf  = new StringBuffer();
            Iterator it = cookieSet.iterator();
            while(it.hasNext()) {
                cookieBuf.append(it.next());
                cookieBuf.append(" ");
            }
            logMessage("CookieResetInitializer: cookieSet: "
                    + cookieBuf.toString());
        }
    }
    
    private HashSet _cookieNames = new HashSet();
    private Map _cookieDomains;
    private Map _cookiePaths;
}
