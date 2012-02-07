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
 * $Id: CookieUtils.java,v 1.2 2008/06/25 05:51:59 qcheng Exp $
 *
 */

package com.sun.identity.agents.util;

import java.util.Map;
import java.util.HashMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;


public class  CookieUtils implements IUtilConstants {
    
    /**
     * This method creates Map from the name values of cookies
     * present in the given <code>HttpServletRequest</code>
     *
     * @param request reference to <code>HttpServletRequest</code>
     * @return Map containing name value pairs from cookies present
     */
    public static Map getRequestCookies(HttpServletRequest request) {
        
        HashMap cookieMap = new HashMap();
        if (request != null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null && cookies.length > 0) {
                for (int i=0; i<cookies.length; i++) {
                    Cookie nextCookie = cookies[i];
                    String name = nextCookie.getName();
                    String value = nextCookie.getValue();
                    cookieMap.put(name, value);
                }
            }
        }
        return cookieMap;
    }
    
    
    /**
     * This method creates a named cookie that is set to expire. This cookie is
     * not automatically added to the <code>HttpServletResponse</code> and is
     * the responsibility of the caller to set it when needed.
     *
     * @param cookieName the name of the cookie to be created
     * @return the named cookie which is set to expire
     */
    public static Cookie getExpiredCookie(String cookieName) {
        return getExpiredCookie(cookieName, null, null);
    }
    
    
    /**
     * This method creates a named cookie that is set to expire. This cookie is
     * not automatically added to the <code>HttpServletResponse</code> and is
     * the responsibility of the caller to set it when needed.
     *
     * @param cookieName the name of the cookie to be created
     * @param domain the domain of the cookie to be created
     * @param path the path of the cookie to be created
     * @return the named cookie which is set to expire
     */
    public static Cookie getExpiredCookie(String cookieName, String domain, 
            String path) {
        String value = String.valueOf(COOKIE_RESET_STRING);
        return getResponseCookie(cookieName, value, domain, path, 0);
    }
    
    
    /**
     * This method creates a named session cookie. This cookie is
     * not automatically added to the <code>HttpServletResponse</code> and is
     * the responsibility of the caller to set it when needed.
     *
     * @param cookieName the name of the cookie to be created
     * @param value the value of the Cookie
     *
     * @return the named session cookie
     */
    
    public static Cookie getResponseCookie(String cookieName, String value ){
        return getResponseCookie(cookieName, value, null, null);
    }
    
    
    /**
     * This method creates a named session cookie. This cookie is
     * not automatically added to the <code>HttpServletResponse</code> and is
     * the responsibility of the caller to set it when needed.
     *
     * @param cookieName the name of the cookie to be created
     * @param value the value of the Cookie
     * @param domain the domain of the cookie to be created
     * @param path the path of the cookie to be created
     *
     * @return the named session cookie
     */
    
    public static Cookie getResponseCookie(String cookieName, String value,
            String domain, String path) {
        return getResponseCookie(cookieName, value, domain, path, -1);
    }
    
    
    /**
     * This method creates a named cookie. This cookie is
     * not automatically added to the <code>HttpServletResponse</code> and is
     * the responsibility of the caller to set it when needed.
     *
     * @param cookieName the name of the cookie to be created
     * @param value the value of the Cookie
     * @param domain the domain of the cookie to be created
     * @param path the path of the cookie to be created
     * @param maxAge the maximum age of cookie in seconds
     *
     * @return the named cookie
     */
    
    public static Cookie getResponseCookie(String cookieName, String value,
            String domain, String path,int maxAge) {
        return getResponseCookie(cookieName, value, domain, path,
                maxAge, false, 0);
    }
    
    
    
    /**
     * This method creates a named cookie. This cookie is
     * not automatically added to the <code>HttpServletResponse</code> and is
     * the responsibility of the caller to set it when needed.
     *
     * @param cookieName the name of the cookie to be created
     * @param value the value of the Cookie
     * @param domain the domain of the cookie to be created
     * @param path the path of the cookie to be created
     * @param maxAge the maximum age of cookie in seconds
     * @param isSecure whether the cookie should only be sent using
     *                  a secure protocol, such as HTTPS or SSL
     * @param version the version of the cookie protocol this cookie complies 
     * with
     *
     * @return the named cookie
     */
    
    public static Cookie getResponseCookie(String cookieName, String value,
            String domain, String path,
            int maxAge, boolean isSecure,
            int version ) {
        
        Cookie cookie = new Cookie(cookieName, value);
        
        if (domain != null) {
            cookie.setDomain(domain);
        }
        if (path != null) {
            cookie.setPath(path);
        }else {
            cookie.setPath(DEFAULT_COOKIE_PATH);
        }
        if (maxAge >= 0) {
            cookie.setMaxAge(maxAge);
        }
        cookie.setSecure(isSecure);
        cookie.setVersion(version);
        
        return cookie;
    }
    
}
