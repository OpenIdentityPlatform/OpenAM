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
 * $Id: CookieUtils.java,v 1.6 2009/10/02 00:08:26 ericow Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 */

package com.sun.identity.shared.encode;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

/**
 * Implements utility methods for handling Cookie.
 */
public class CookieUtils {
    static boolean secureCookie = 
        (SystemPropertiesManager.get(Constants.AM_COOKIE_SECURE) != null) && 
        (SystemPropertiesManager.get(Constants.AM_COOKIE_SECURE).
            equalsIgnoreCase("true"));

    static boolean cookieHttpOnly = 
        (SystemPropertiesManager.get(Constants.AM_COOKIE_HTTPONLY) != null) && 
        (SystemPropertiesManager.get(Constants.AM_COOKIE_HTTPONLY).
            equalsIgnoreCase("true"));

    static boolean cookieEncoding = 
        (SystemPropertiesManager.get(Constants.AM_COOKIE_ENCODE) != null) && 
        (SystemPropertiesManager.get(Constants.AM_COOKIE_ENCODE)
            .equalsIgnoreCase("true"));

    static String amCookieName = SystemPropertiesManager.get(
        Constants.AM_COOKIE_NAME);
    static String amPCookieName = SystemPropertiesManager.get(
        Constants.AM_PCOOKIE_NAME);
    static String cdssoCookiedomain = SystemPropertiesManager.get(
        Constants.SERVICES_CDSSO_COOKIE_DOMAIN);
    static String fedCookieName = SystemPropertiesManager.get(
        Constants.FEDERATION_FED_COOKIE_NAME);

    private static Set cookieDomains = null;
    private static int defAge = -1;

    static Debug debug = Debug.getInstance("amCookieUtils");
    private static final Method setHttpOnlyMethod;

    static {
        Method method = null;
        try {
            method = Cookie.class.getMethod("setHttpOnly", boolean.class);
        } catch (NoSuchMethodException nsme) {
            debug.message("This is not a Java EE 6+ container, Cookie#setHttpOnly(boolean) is not available");
        }
        setHttpOnlyMethod = method;
    }

    /**
     * Gets property value of "com.iplanet.am.cookie.name"
     * 
     * @return the property value of "com.iplanet.am.cookie.name"
     */
    public static String getAmCookieName() {
        return amCookieName;
    }

    /**
     * Returns property value of "com.iplanet.am.pcookie.name"
     * 
     * @return the property value of "com.iplanet.am.pcookie.name"
     */
    public static String getAmPCookieName() {
        return amPCookieName;
    }

    /**
     * Returns property value of "com.iplanet.services.cdsso.cookiedomain"
     * 
     * @return the property value of "com.iplanet.services.cdsso.cookiedomain"
     */
    public static Set getCdssoCookiedomain() {
        if (cookieDomains != null) {
            return cookieDomains;
        }

        Set cookieDomains = new HashSet();
        if (cdssoCookiedomain == null || cdssoCookiedomain.length() < 1) {
            return Collections.EMPTY_SET;
        }

        StringTokenizer st = new StringTokenizer(cdssoCookiedomain, ",");
        while (st.hasMoreTokens()) {
            String token = st.nextToken().trim();
            if (token.length() > 0) {
                cookieDomains.add(token);
            }
        }

        return cookieDomains.isEmpty() ? Collections.EMPTY_SET : cookieDomains; 
    }

    /**
     * Returns property value of "com.sun.identity.federation.fedCookieName"
     * 
     * @return the property value of "com.sun.identity.federation.fedCookieName"
     */
    public static String getFedCookieName() {
        return fedCookieName;
    }

    /**
     * Returns property value of "com.iplanet.am.cookie.secure"
     * 
     * @return the property value of "com.iplanet.am.cookie.secure"
     */
    public static boolean isCookieSecure() {
        return secureCookie;
    }

    /**
     * Returns property value of "com.sun.identity.cookie.httponly"
     * 
     * @return the property value of "com.sun.identity.cookie.httponly"
     */
    public static boolean isCookieHttpOnly() {
        return cookieHttpOnly;
    }

    /**
     * Returns value of cookie that has mached name in servlet request
     * 
     * @param req HTTP Servlet Request.
     * @param name Name in servlet request
     * @return value of that name of cookie
     */
    public static String getCookieValueFromReq(
        HttpServletRequest req,
        String name
    ) {
        String cookieValue = null;
        Cookie cookie = getCookieFromReq(req, name);
        if (cookie != null) {
            cookieValue = getCookieValue(cookie);
        } else {
            debug.message("No Cookie is in the request");
        }
        return cookieValue;
    }

    /**
     * Gets cookie object that has mached name in servlet request
     * 
     * @param req HTTP Servlet Request.
     * @param name Name in servlet request
     * @return value of that name of cookie
     */
    public static Cookie getCookieFromReq(HttpServletRequest req, String name) {
        Cookie cookies[] = req.getCookies();
        if (cookies != null) {
            for (int nCookie = 0; nCookie < cookies.length; nCookie++) {
                if (cookies[nCookie].getName().equalsIgnoreCase(name)) {
                    return cookies[nCookie];
                }
            }
        }
        return null;
    }

    /**
     * Returns normalized value of cookie
     * 
     * @param cookie Cookie object.
     * @return normalized value of cookie.
     */
    public static String getCookieValue(Cookie cookie) {
        String cookieValue = checkDoubleQuote(cookie.getValue());

        // Check property value and it decode value
        // Bea, IBM
        if (cookieValue != null && cookieEncoding) {
            return URLEncDec.decode(cookieValue);
        }
        return cookieValue;
    }

    /**
     * Gets Array of cookie in servlet request.
     * 
     * @param req HTTP Servlet Request.
     */
    public static Cookie[] getCookieArrayFromReq(HttpServletRequest req) {
        Cookie cookies[] = req.getCookies();

        if (!cookieEncoding) {
            return cookies;
        }

        if (cookies != null) {
            for (int nCookie = 0; nCookie < cookies.length; nCookie++) {
                String cookieValue = checkDoubleQuote(cookies[nCookie]
                        .getValue());
                if (cookieValue != null) {
                    cookies[nCookie].setValue(URLEncDec.decode(cookieValue));
                }
            }
        }
        return cookies;
    }

    /**
     * This method creates Map from the name values of cookies
     * present in the given <code>HttpServletRequest</code>
     *
     * @param request reference to <code>HttpServletRequest</code>
     * @return Map containing name value pairs from cookies present
     */
    public static Map<String, String> getRequestCookies(HttpServletRequest request) {
        Map<String, String> cookieMap = new HashMap<String, String>();
        if (request != null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null && cookies.length > 0) {
                for (Cookie nextCookie : cookies) {
                    String name = nextCookie.getName();
                    String value = nextCookie.getValue();
                    cookieMap.put(name, value);
                }
            }
        }
        return cookieMap;
    }

    /**
     * Returns a cookie with a specified name and value.
     * 
     * @param name Name of the cookie.
     * 
     * @param value Value of the cookie.
     * 
     * @return constructed cookie.
     */
    public static Cookie newCookie(String name, String value) {
        return newCookie(name, value, defAge, null, null);
    }

    /**
     * Returns a cookie with a specified name and value and sets the maximum
     * age of the cookie in seconds.
     * 
     * @param name Name of the cookie
     * @param value Value of the cookie
     * @param maxAge Maximum age of the cookie in seconds; if negative, means 
     *        the cookie is not stored; if zero, deletes the cookie.
     * @return constructed cookie
     */
    public static Cookie newCookie(String name, String value, int maxAge) {
        return newCookie(name, value, maxAge, null, null);
    }

    /**
     * Returns a cookie with a specified name and value and sets a path for
     * the cookie to which the client should return the cookie.
     * 
     * @param name Name of the cookie
     * @param value Value of the cookie
     * @param path Path
     * @return constructed cookie
     */
    public static Cookie newCookie(String name, String value, String path) {
        return newCookie(name, value, defAge, path, null);
    }

    /**
     * Returns a cookie with a specified name and value and sets a path for
     * the cookie to which the client should return the cookie and sets the
     * domain within which this cookie should be presented.
     * 
     * @param name Name of the cookie
     * @param value Value of the cookie
     * @param path Path
     * @param domain Domain name within which this cookie is visible; form is 
     *        according to <code>RFC 2109</code>
     * @return constructed cookie
     */
    public static Cookie newCookie(
        String name,
        String value,
        String path,
        String domain
    ) {
        return newCookie(name, value, defAge, path, domain);
    }

    /**
     * Returns a cookie with a specified name and value and sets the maximum
     * age of the cookie in seconds and sets a path for the cookie to which the
     * client should return the cookie and sets the domain within which this
     * cookie should be presented.
     * 
     * @param name Name of the cookie
     * @param value Value of the cookie
     * @param maxAge Maximum age of the cookie in seconds; if negative, means 
     *        the cookie is not stored; if zero, deletes the cookie.
     * @param path Path
     * @param domain Domain name within which this cookie is visible; form is 
     *        according to <code>RFC 2109</code>
     * @return constructed cookie
     */
    public static Cookie newCookie(
        String name,
        String value, 
        int maxAge,
        String path,
        String domain
    ) {
        Cookie cookie = null;

        // Based on property value it does url encoding.
        // BEA, IBM
        if (cookieEncoding && value != null) {
            cookie = new Cookie(name, URLEncDec.encode(value));
        } else {
            cookie = new Cookie(name, value);
        }

        cookie.setMaxAge(maxAge);

        if ((path != null) && (path.length() > 0)) {
            cookie.setPath(path);
        } else {
            cookie.setPath("/");
        }

        if ((domain != null) && (domain.length() > 0)) {
            cookie.setDomain(domain);
        }

        cookie.setSecure(isCookieSecure());

        return cookie;
    }

    /**
     * Check cookie value whether it has double quote or not. Remove start /
     * ending double quote from cookie and returns cookie value only.
     * 
     * @param cookie Value of the Cookie
     * @return cookie value without double quote
     */
    public static String checkDoubleQuote(String cookie) {
        String double_quote = "\"";
        if ((cookie != null) && cookie.startsWith(double_quote)
                && cookie.endsWith(double_quote)) {
            int last = cookie.length() - 1;
            cookie = cookie.substring(1, last);
        }
        return cookie;
    }

    /**
     * Add cookie to HttpServletResponse as custom header
     * 
     * @param response
     * @param cookie 
     */
    public static void addCookieToResponse(HttpServletResponse response, Cookie cookie) {
        if (response==null || cookie == null) {
            return;
        }
        if (!isCookieHttpOnly()) {
            response.addCookie(cookie);
            return;
        }

        if (setHttpOnlyMethod != null) {
            try {
                setHttpOnlyMethod.invoke(cookie, true);
                response.addCookie(cookie);
                return;
            } catch (IllegalAccessException iae) {
                debug.warning("IllegalAccessException while trying to add HttpOnly cookie: " + iae.getMessage());
            } catch (InvocationTargetException ite) {
                debug.error("An error occurred while trying to add HttpOnly cookie", ite);
            }
        }

        StringBuilder sb = new StringBuilder(150);
        sb.append(cookie.getName()).append("=").append(cookie.getValue());
        String path = cookie.getPath();
        if (path != null && path.length() > 0) {
            sb.append(";path=").append(path);
        } else {
            sb.append(";path=/");
        }
        String domain = cookie.getDomain();
        if (domain != null && domain.length() > 0) {
            sb.append(";domain=").append(domain);
        }
        int age = cookie.getMaxAge();
        if (age > -1) {
            Date date = new Date(currentTimeMillis() + age * 1000l);
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss zzz", Locale.UK);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            sb.append(";max-age=").append(age);
            // set Expires as < IE 8 does not support max-age
            sb.append(";Expires=").append(sdf.format(date));
        }
        if (CookieUtils.isCookieSecure() || cookie.getSecure()) {
            sb.append(";secure");
        }
        sb.append(";httponly");
        if (debug.messageEnabled()) {
            debug.message("CookieUtils:addCookieToResponse adds " + sb);
        }
        response.addHeader("Set-Cookie", sb.toString());
    }

    /**
     * Matches the provided cookie domains against the current request's domain and returns the resulting set of
     * matching cookie domains if the 'com.sun.identity.authentication.setCookieToAllDomains' advanced property is set
     * to false.
     *
     * @param request The HTTP request.
     * @param cookieDomains The configured cookie domains to match against.
     * @return The set of matching cookie domains. May contain null.
     */
    public static Set<String> getMatchingCookieDomains(HttpServletRequest request, Collection<String> cookieDomains) {
        if (SystemPropertiesManager.getAsBoolean(Constants.SET_COOKIE_TO_ALL_DOMAINS, true)) {
            return new HashSet<>(cookieDomains);
        }

        String host = request.getServerName();
        Set<String> domains = new HashSet<>();

        for (String domain : cookieDomains) {
            if (domain != null && StringUtils.endsWithIgnoreCase(host, domain)) {
                domains.add(domain);
            }
        }
        //IE fix: allow add .app.domain.com after .domain.com without SystemPropertiesManager.getAsBoolean(Constants.SET_COOKIE_TO_ALL_DOMAINS, true) 
        for (String domain :  new HashSet<>(domains)) { //.domain.com
			for (String cookieDomain : cookieDomains) { //.app.domain.com 
				if (StringUtils.endsWithIgnoreCase(cookieDomain,domain)) //.app.domain.com ends with .domain.com ?
					domains.add(cookieDomain); //add .app.domain.com after .domain.com
			}
		}
        return domains;
    }
}
