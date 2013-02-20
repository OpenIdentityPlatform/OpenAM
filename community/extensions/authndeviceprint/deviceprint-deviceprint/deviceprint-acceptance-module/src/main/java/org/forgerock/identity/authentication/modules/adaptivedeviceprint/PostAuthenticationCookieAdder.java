/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 */

package org.forgerock.identity.authentication.modules.adaptivedeviceprint;

import static org.forgerock.identity.authentication.modules.adaptivedeviceprint.ProfileAcceptanceConstants.PERSISTENT_COOKIE_NAME;
import static org.forgerock.identity.authentication.modules.adaptivedeviceprint.ProfileAcceptanceConstants.PERSISTENT_COOKIE_VALUE_SESSION_ATTRIBUTE_NAME;

import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.identity.authentication.modules.common.AMPostAuthProcessAdapter;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.CookieUtils;

public class PostAuthenticationCookieAdder extends AMPostAuthProcessAdapter {

	private static final String COOKIE_PATH = "/";
	
	/**
	 * Logger
	 */
	private static final Debug debug = Debug
			.getInstance(PostAuthenticationCookieAdder.class.getName());
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void onLoginSuccess(Map arg0, HttpServletRequest request,
			HttpServletResponse response, SSOToken arg3)
			throws AuthenticationException {
		Object attribute = request.getSession().getAttribute(PERSISTENT_COOKIE_VALUE_SESSION_ATTRIBUTE_NAME);
		if(attribute != null) {
			debug.message("Cookie to be set found in session. Cookie value=" + attribute);
			
			Set<String> domains = AuthUtils.getCookieDomainsForReq(request);
			
			for(String domain : domains) {
				response.addCookie(CookieUtils.newCookie(PERSISTENT_COOKIE_NAME, (String) attribute, COOKIE_PATH, domain));
			}
		}
	}
	
}
