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
 * $Id: OpenSsoProcessingFilter.java,v 1.1 2008/09/15 18:19:46 robdale Exp $
 *
 */
package com.sun.identity.provider.spring;

import javax.servlet.http.HttpServletRequest;

import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationException;
import org.acegisecurity.BadCredentialsException;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.acegisecurity.ui.AbstractProcessingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

public class OpenSsoProcessingFilter extends AbstractProcessingFilter {

	private static final Logger log = LoggerFactory.getLogger(OpenSsoProcessingFilter.class);

	public static final String ACEGI_SECURITY_LAST_USERNAME_KEY = "ACEGI_SECURITY_LAST_USERNAME";

	public Authentication attemptAuthentication(HttpServletRequest request) throws AuthenticationException {
		
		String username = obtainUsername(request);
		log.debug("username: {}", (username == null ? "is null" : username));

		if (username == null) {
			throw new BadCredentialsException("User not logged in via Portal! SSO user cannot be validated!");
		}

		UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(username, "", new GrantedAuthority[] {new GrantedAuthorityImpl("foo")});

		// Place the last username attempted into HttpSession for views
		request.getSession().setAttribute(ACEGI_SECURITY_LAST_USERNAME_KEY, username);

		setDetails(request, authRequest);

		return this.getAuthenticationManager().authenticate(authRequest);
	}

	@Override
	public String getDefaultFilterProcessesUrl() {
		return "/ssologin";
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
		if (token == null) {
			throw new IllegalArgumentException("SSOToken is null");
		}

		boolean result = false;
		try {
			SSOTokenManager manager = SSOTokenManager.getInstance();
			result = manager.isValidToken(token);
		} catch (Exception e) {
			log.debug("Error validating SSOToken", e);
		}
		return result;
	}

	protected String obtainUsername(HttpServletRequest request) {
		String result = null;
		request = HttpUtil.unwrapOriginalHttpServletRequest(request);
		HttpUtil.printCookies(request);
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

    protected void setDetails(HttpServletRequest request, UsernamePasswordAuthenticationToken authRequest) {
        authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
    }

}
