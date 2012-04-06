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
 * $Id: OpenSsoAuthenticator.java,v 1.1 2008/09/15 18:39:47 robdale Exp $
 *
 */
package com.sun.identity.provider.seraph;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Category;

import com.atlassian.seraph.auth.DefaultAuthenticator;
import com.atlassian.seraph.util.RedirectUtils;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

public class OpenSsoAuthenticator extends DefaultAuthenticator {

	private final static long serialVersionUID = 0L;

	private static final Category log = Category.getInstance(OpenSsoAuthenticator.class);

	public Principal getUser(HttpServletRequest request, HttpServletResponse response) {
		Principal user = null;

		try {
			request.getSession(true);
			log.info("Trying seamless Single Sign-on...");
			String username = obtainUsername(request);
			log.info("Got username = " + username);
			if (username != null) {
				if (request.getSession() != null && request.getSession().getAttribute(DefaultAuthenticator.LOGGED_IN_KEY) != null) {
					log.info("Session found; user already logged in");
					user = (Principal) request.getSession().getAttribute(DefaultAuthenticator.LOGGED_IN_KEY);
				} else {
					user = getUser(username);
					log.info("Logged in via SSO, with User " + user);
					request.getSession().setAttribute(DefaultAuthenticator.LOGGED_IN_KEY, user);
					request.getSession().setAttribute(DefaultAuthenticator.LOGGED_OUT_KEY, null);
				}
			} else {
				String redirectUrl = RedirectUtils.getLoginUrl(request);
				log.info("Username is null; redirecting to " + redirectUrl);
				// user was not found, or not currently valid
//				response.sendRedirect(redirectUrl);
				return null;
			}
		} catch (Exception e) // catch class cast exceptions
		{
			log.warn("Exception: " + e, e);
		}
		return user;

	}

	private SSOToken getToken(HttpServletRequest request) {
		SSOToken token = null;
		try {
			SSOTokenManager manager = SSOTokenManager.getInstance();
			token = manager.createSSOToken(request);
		} catch (Exception e) {
			log.debug("Error creating SSOToken", e);
		}
		return token;
	}

	private boolean isTokenValid(SSOToken token) {
		boolean result = false;
		try {
			SSOTokenManager manager = SSOTokenManager.getInstance();
			result = manager.isValidToken(token);
		} catch (Exception e) {
			log.debug("Error validating SSOToken", e);
		}
		return result;
	}

	private String obtainUsername(HttpServletRequest request) {
		String result = null;
		SSOToken token = getToken(request);
		if (token != null && isTokenValid(token)) {
			try {
				result = token.getProperty("UserId");
			} catch (SSOException e) {
				log.error("Error getting UserId from SSOToken", e);
			}
		}
		return result;
	}
}
