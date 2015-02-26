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
 * $Id: CookieResetHelper.java,v 1.2 2008/06/25 05:51:39 qcheng Exp $
 *
 */

package com.sun.identity.agents.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.agents.arch.Module;
import com.sun.identity.agents.arch.SurrogateBase;
import com.sun.identity.agents.util.CookieUtils;
import com.sun.identity.agents.util.RequestDebugUtils;

/**
 * This class handles Cookie Reset
 *
 */
public class CookieResetHelper extends SurrogateBase
        implements ICookieResetHelper {
    
    public CookieResetHelper(Module module) {
        super(module);
    }
    
    public boolean isActive() {
        return isCookieResetEnabled();
    }
    
    public int doCookiesReset(HttpServletRequest request,
            HttpServletResponse response) {
        int count = 0;
        if (isCookieResetEnabled()) {
            Iterator it = getCookieResetList().iterator();
            Map reqCookieMap = CookieUtils.getRequestCookies(request);
            while (it.hasNext()) {
                Cookie nextCookie = (Cookie) it.next();
                if (reqCookieMap.containsKey(nextCookie.getName())) {
                    response.addCookie(nextCookie);
                    if (isLogMessageEnabled()) {
                        logMessage("CookieResetHelper: reset cookie: "
                                + RequestDebugUtils.getDebugString(nextCookie));
                    }
                    count++;
                }
            }
        }
        return count;
    }
    
    public void initialize(ICookieResetInitializer cookieResetInitializer) {
        
        ArrayList cookieResetList = new ArrayList();
        HashSet cookieNames = cookieResetInitializer.getCookieNames();
        Map cookieDomains = cookieResetInitializer.getCookieDomains();
        Map cookiePaths = cookieResetInitializer.getCookiePaths();
        
        Iterator it = cookieNames.iterator();
        while (it.hasNext()) {
            String cookieName = (String) it.next();
            String domain = (String) cookieDomains.get(cookieName);
            String path = (String) cookiePaths.get(cookieName);
            Cookie resetCookie = CookieUtils.getExpiredCookie(
                    cookieName, domain, path);
            if (isLogMessageEnabled()) {
                logMessage("CookieResetHelper: Reset Cookie -> "
                        + RequestDebugUtils.getDebugString(resetCookie));
            }
            cookieResetList.add(resetCookie);
        }
        
        setCookieResetEnableFlag(cookieResetList.size() > 0);
        setCookieResetList(cookieResetList);
    }
    
    private boolean isCookieResetEnabled() {
        return _cookieResetEnabled;
    }
    
    private void setCookieResetEnableFlag(boolean flag) {
        _cookieResetEnabled = flag;
    }
    
    private ArrayList getCookieResetList() {
        return _cookieResetList;
    }
    
    private void setCookieResetList(ArrayList list) {
        _cookieResetList = list;
    }
    
    private boolean _cookieResetEnabled;
    private ArrayList _cookieResetList;
}





