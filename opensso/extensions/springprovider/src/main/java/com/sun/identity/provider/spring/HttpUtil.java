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
 * $Id: HttpUtil.java,v 1.1 2008/09/15 18:19:46 robdale Exp $
 *
 */
package com.sun.identity.provider.spring;

import java.util.Enumeration;

import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtil {

	private static final Logger log = LoggerFactory.getLogger(HttpUtil.class);

	public static void printCookies(HttpServletRequest request) {
		if (log.isTraceEnabled()) {
			Enumeration headers = request.getHeaderNames();
			while (headers.hasMoreElements()) {
				String header = (String) headers.nextElement();
				log.trace("Header: {} = {}", header, request.getHeader(header));
			}

			Cookie[] cookies = request.getCookies();
			if (cookies == null) {
				log.trace("Cookies are null!");
				return;
			}
			if (cookies.length == 0) {
				log.trace("Cookies are empty!");
			} else {
				log.trace("Cookies.length: {}", cookies.length);
				for (Cookie cookie : cookies) {
					String comment = cookie.getComment();
					String domain = cookie.getDomain();
					Integer maxAge = cookie.getMaxAge();
					String name = cookie.getName();
					String path = cookie.getPath();
					Boolean secure = cookie.getSecure();
					String value = cookie.getValue();
					Integer version = cookie.getVersion();
					log.trace(
							"Cookie: name: {}, domain: {}, path: {}, value: {}, secure: {}, maxAge: {}, version: {}, comment {}",
							new Object[] { name, domain, path, value, secure, maxAge, version, comment });
				}
			}
		}
	}

	public static HttpServletRequest unwrapOriginalHttpServletRequest(HttpServletRequest request) {
		if (request instanceof HttpServletRequestWrapper) {
			log.debug("Found HttpServletRequestWrapper: unwrapping..");
			HttpServletRequestWrapper wrapper = (HttpServletRequestWrapper) request;
			ServletRequest servletRequest = wrapper.getRequest();
			if (servletRequest instanceof HttpServletRequest) {
				log.debug("Unwrapped original HttpServletRequest");
				request = (HttpServletRequest) servletRequest;
			} else {
				log.debug("Unwrapped a {}", servletRequest);
			}
		} else {
			log.debug("Found a {}", request);
		}
		return request;
	}

}
