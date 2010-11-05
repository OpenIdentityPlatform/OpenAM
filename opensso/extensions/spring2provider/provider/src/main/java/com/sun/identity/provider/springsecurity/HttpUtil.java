/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * 
 * "Portions Copyrighted 2008 Robert Dale <robdale@gmail.com>"
 *
 * $Id: HttpUtil.java,v 1.2 2009/02/26 18:20:57 wstrange Exp $
 *
 */
package com.sun.identity.provider.springsecurity;

import com.sun.identity.shared.debug.Debug;
import java.util.Enumeration;

import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class HttpUtil {
    private static Debug debug = Debug.getInstance("amSpring");

    public static void printCookies(HttpServletRequest request) {
        if (debug.messageEnabled()) {
            Enumeration headers = request.getHeaderNames();
            while (headers.hasMoreElements()) {
                String header = (String) headers.nextElement();
                debug.message("Header: " + header + " =" +request.getHeader(header));
            }

            Cookie[] cookies = request.getCookies();
            if (cookies == null) {
                debug.message("Cookies are null!");
                return;
            }
            if (cookies.length == 0) {
                debug.message("Cookies are empty!");
            } else {
                debug.message("Cookies.length: " + cookies.length);
                for (Cookie cookie : cookies) {
                    String comment = cookie.getComment();
                    String domain = cookie.getDomain();
                    Integer maxAge = cookie.getMaxAge();
                    String name = cookie.getName();
                    String path = cookie.getPath();
                    Boolean secure = cookie.getSecure();
                    String value = cookie.getValue();
                    Integer version = cookie.getVersion();
                    debug.message(
                            "Cookie: name: " + name + " domain: " + domain + " path: " + path +
                                    " value: " + value + " secure: " + secure + " maxAge: " + maxAge + " version: " + version
                                    + " comment " + comment );
                           
                }
            }
        }
    }

    public static HttpServletRequest unwrapOriginalHttpServletRequest(HttpServletRequest request) {
        if (request instanceof HttpServletRequestWrapper) {
            debug.message("Found HttpServletRequestWrapper: unwrapping..");
            HttpServletRequestWrapper wrapper = (HttpServletRequestWrapper) request;
            ServletRequest servletRequest = wrapper.getRequest();
            if (servletRequest instanceof HttpServletRequest) {
                debug.message("Unwrapped original HttpServletRequest");
                request = (HttpServletRequest) servletRequest;
            } else {
                debug.message("Unwrapped a " + servletRequest);
            }
        } else {
            debug.message("Found a " +  request);
        }
        return request;
    }
}
