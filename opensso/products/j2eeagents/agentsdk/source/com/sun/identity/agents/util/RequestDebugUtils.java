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
 * $Id: RequestDebugUtils.java,v 1.3 2009/01/21 18:57:44 kanduls Exp $
 *
 */

package com.sun.identity.agents.util;



import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import java.util.Enumeration;


/**
 * A util class to manage a <code>HttpServletRequest</code> object
 */
public class RequestDebugUtils implements IUtilConstants {

    public static String getDebugString(HttpServletRequest request) {

        StringBuffer buff = new StringBuffer();

        writeln(buff);
        writeln(buff, SEPARATOR);
        writeln(buff, CLASS, request.getClass().getName());
        writeln(buff);
        writeln(buff, CHAR_ENC, request.getCharacterEncoding());
        writeln(buff, CONTENT_LEN,
                String.valueOf(request.getContentLength()));
        writeln(buff, CONTENT_TYPE, request.getContentType());
        writeln(buff, LOCALE, request.getLocale());
        writeln(buff);
        addAcceptLocales(buff, request);
        writeln(buff, PROTOCOL, request.getProtocol());
        writeln(buff, REMOTE_ADDR, request.getRemoteAddr());
        writeln(buff, REMOTE_HOST, request.getRemoteHost());
        writeln(buff, SCHEME, request.getScheme());
        writeln(buff, SERVER_NAME, request.getServerName());
        writeln(buff, SERVER_PORT, String.valueOf(request.getServerPort()));
        writeln(buff, IS_SECURE, String.valueOf(request.isSecure()));
        writeln(buff, AUTH_TYPE, request.getAuthType());
        writeln(buff, CONTEXT_PATH, request.getContextPath());
        addCookies(buff, request);
        addHeaders(buff, request);
        writeln(buff, METHOD, request.getMethod());
        writeln(buff, PATH_INFO, request.getPathInfo());
        writeln(buff, PATH_TRANS, request.getPathTranslated());
        writeln(buff, QUERY_STR, request.getQueryString());
        writeln(buff, REMOTE_USR, request.getRemoteUser());
        writeln(buff, REQ_SESS_ID, request.getRequestedSessionId());
        writeln(buff, REQ_URI, request.getRequestURI());
        writeln(buff, SERVLET_PATH, request.getServletPath());
        writeln(buff, SESSION, String.valueOf(request.getSession(false) != null));
        //writeln(buff, PRINCIPAL, request.getUserPrincipal());
        writeln(buff, PRINCIPAL, "<not queried>");
        addAttributes(buff, request);
        writeln(buff);
        writeln(buff, SEPARATOR);

        return buff.toString();
    }

    public static String getDebugString(Cookie cookie) {

        StringBuffer buff = new StringBuffer();

        writeln(buff);
        writeln(buff, SEPARATOR);
        writeln(buff, COOKIE_CLASS, cookie);
        writeln(buff);

        if(cookie != null) {
            writeln(buff, COOKIE_NAME, cookie.getName());
            writeln(buff, COOKIE_VALUE, cookie.getValue());
            writeln(buff, COOKIE_COMMENT, cookie.getComment());
            writeln(buff, COOKIE_DOMAIN, cookie.getDomain());
            writeln(buff, COOKIE_PATH, cookie.getPath());
            writeln(buff, COOKIE_MAX_AGE, String.valueOf(cookie.getMaxAge()));
            writeln(buff, IS_SECURE_COOKIE,
                    String.valueOf(cookie.getSecure()));
            writeln(buff, COOKIE_VERSION,
                    String.valueOf(cookie.getVersion()));
        }

        writeln(buff);
        writeln(buff, SEPARATOR);

        return buff.toString();
    }

    private static void addAttributes(StringBuffer buff,
                                      HttpServletRequest request) {

        writeln(buff, ATTRIBUTES);

        Enumeration attrEnum = request.getAttributeNames();
        int         count    = 0;

        while((attrEnum != null) && attrEnum.hasMoreElements()) {
            String name  = (String) attrEnum.nextElement();
            Object obj   = request.getAttribute(name);
            String value = (obj == null)
                           ? "null"
                           : obj.toString();

            writeln(buff, 2, name, ": ", value);

            count++;
        }

        if(count == 0) {
            writeln(buff, 2, NO_ATTRIBUTES);
        }

        writeln(buff);
    }

    private static void addHeaders(StringBuffer buff,
                                   HttpServletRequest request) {

        writeln(buff, HEADERS);

        Enumeration headers = request.getHeaderNames();
        int         count   = 0;

        while((headers != null) && headers.hasMoreElements()) {
            String name = (String) headers.nextElement();

            writeln(buff, 2, name, ":");

            Enumeration headerValues = request.getHeaders(name);

            addEnumValues(buff, headerValues, 3, NO_HEADER_VALUE, false);

            count++;
        }

        if(count == 0) {
            writeln(buff, 2, NO_HEADERS);
        }
    }

    private static void addCookies(StringBuffer buff,
                                   HttpServletRequest request) {

        writeln(buff, COOKIES);

        Cookie[] cookie = request.getCookies();

        if((cookie == null) || (cookie.length == 0)) {
            writeln(buff, 2, NO_COOKIES);
        } else {
            for(int i = 0; i < cookie.length; i++) {
                String name  = cookie[i].getName();
                String value = cookie[i].getValue();

                writeln(buff, 2, name, ": ", value);
            }
        }

        writeln(buff);
    }

    private static void addAcceptLocales(StringBuffer buff,
                                         HttpServletRequest request) {

        writeln(buff, ACCEPT_LOCALE);

        Enumeration locales = request.getLocales();

        addEnumValues(buff, locales, 2, NO_ACCEPT_LOCALE);
    }

    private static void addEnumValues(StringBuffer buff, Enumeration anEnum,
                                      int indentLevel,
                                      String emptyEnumMessage) {
        addEnumValues(buff, anEnum, indentLevel, emptyEnumMessage, true);
    }

    private static void addEnumValues(StringBuffer buff, Enumeration anEnum,
                                      int indentLevel,
                                      String emptyEnumMessage,
                                      boolean newLine) {

        int count = 0;

        while((anEnum != null) && anEnum.hasMoreElements()) {
            Object obj   = anEnum.nextElement();
            String value = (obj == null)
                           ? "null"
                           : obj.toString();

            writeln(buff, indentLevel, value);

            count++;
        }

        if(count == 0) {
            writeln(buff, indentLevel, emptyEnumMessage);
        }

        if(newLine) {
            writeln(buff);
        }
    }

    private static void writeln(StringBuffer buff, int indentLevel,
                                String str1, String str2, String str3) {

        buff.append(INDENT[indentLevel]);
        buff.append(str1);
        buff.append(str2);
        buff.append(str3);
        buff.append(NEW_LINE);
    }

    private static void writeln(StringBuffer buff, int indentLevel,
                                String str1, String str2) {

        buff.append(INDENT[indentLevel]);
        buff.append(str1);
        buff.append(str2);
        buff.append(NEW_LINE);
    }

    private static void writeln(StringBuffer buff, int indentLevel,
                                String str) {

        buff.append(INDENT[indentLevel]);
        buff.append(str);
        buff.append(NEW_LINE);
    }

    private static void writeln(StringBuffer buff, String str1, String str2) {

        buff.append(str1);
        buff.append(str2);
        buff.append(NEW_LINE);
    }

    private static void writeln(StringBuffer buff, String str1, Object obj) {

        buff.append(str1);
        buff.append((obj == null)
                    ? "null"
                    : obj.toString());
        buff.append(NEW_LINE);
    }

    private static void writeln(StringBuffer buff, String str) {
        buff.append(str);
        buff.append(NEW_LINE);
    }

    private static void writeln(StringBuffer buff) {
        buff.append(NEW_LINE);
    }

    private static final String[] INDENT = new String[]{ "", "\t", "\t\t",
                                                         "\t\t\t" };
    private static final String CLASS = "HttpServletRequest: class => ";
    private static final String CHAR_ENC      = "\tCharacter Encoding\t: ";
    private static final String CONTENT_LEN   = "\tContent Lenght\t\t: ";
    private static final String CONTENT_TYPE  = "\tContent Type\t\t: ";
    private static final String LOCALE        = "\tLocale\t\t\t: ";
    private static final String ACCEPT_LOCALE = "\tAccept Locales: ";
    private static final String NO_ACCEPT_LOCALE =
        "*** No Accept Locales ***";
    private static final String PROTOCOL        = "\tProtocol\t\t: ";
    private static final String REMOTE_ADDR     = "\tRemote Address\t\t: ";
    private static final String REMOTE_HOST     = "\tRemote Host\t\t: ";
    private static final String SCHEME          = "\tScheme\t\t\t: ";
    private static final String SERVER_NAME     = "\tServer Name\t\t: ";
    private static final String SERVER_PORT     = "\tServer Port\t\t: ";
    private static final String IS_SECURE       = "\tIs Secure\t\t: ";
    private static final String AUTH_TYPE       = "\tAuth Type\t\t: ";
    private static final String CONTEXT_PATH    = "\tContext Path\t\t: ";
    private static final String COOKIES         = "\tCookies:";
    private static final String NO_COOKIES      = "*** No Cookies ***";
    private static final String HEADERS         = "\tHeaders:";
    private static final String NO_HEADERS      = "*** No Headers ***";
    private static final String NO_HEADER_VALUE = "*** No Header Value ***";
    private static final String METHOD          = "\tMethod\t\t\t: ";
    private static final String PATH_INFO       = "\tPath Info\t\t: ";
    private static final String PATH_TRANS      = "\tPath Trans\t\t: ";
    private static final String QUERY_STR       = "\tQuery String\t\t: ";
    private static final String REMOTE_USR      = "\tRemote User\t\t: ";
    private static final String REQ_SESS_ID = "\tRequested Session ID\t: ";
    private static final String REQ_URI         = "\tRequest URI\t\t: ";
    private static final String REQ_URL         = "\tRequest URL\t\t: ";
    private static final String SERVLET_PATH    = "\tServlet Path\t\t: ";
    private static final String SESSION         = "\tSession\t\t\t: ";
    private static final String PRINCIPAL       = "\tUser Principal\t\t: ";
    private static final String ATTRIBUTES      = "\tAttributes:";
    private static final String NO_ATTRIBUTES   = "*** No Attributes ***";
    private static final String PARAMETERS      = "\tParameters:";
    private static final String NO_PARAMETERS   = "*** No Parameters ***";
    private static final String NO_PARAM_VALUES =
        "*** No Parameter Values ***";
    private static final String COOKIE_CLASS     = "Cookie: class => ";
    private static final String COOKIE_NAME      = "\tName\t\t: ";
    private static final String COOKIE_VALUE     = "\tValue\t\t: ";
    private static final String COOKIE_DOMAIN    = "\tDomain\t\t: ";
    private static final String COOKIE_PATH      = "\tPath\t\t: ";
    private static final String COOKIE_MAX_AGE   = "\tMax-Age\t\t: ";
    private static final String IS_SECURE_COOKIE = "\tIs Secure?\t: ";
    private static final String COOKIE_VERSION   = "\tVersion\t\t: ";
    private static final String COOKIE_COMMENT   = "\tComment\t\t: ";
}

