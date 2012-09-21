/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [2012] [Forgerock AS]"
 */

package org.forgerock.openam.authentication.modules.passphrase.security.login.module.plugin;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.shared.debug.Debug;

@SuppressWarnings("unchecked")
public class PassphraseTimeLogin implements AMPostAuthProcessInterface {

	private static Debug debug = Debug.getInstance("FirstTimeLogin");

	/**
	 * Post processing on successful authentication.
	 * 
	 * @param requestParamsMap contains HttpServletRequest parameters
	 * @param request HttpServlet request
	 * @param response HttpServlet response
	 * @param ssoToken user's session
	 * @throws AuthenticationException  if there is an error while setting the session paswword property
	 */
	public void onLoginSuccess(Map requestParamsMap, HttpServletRequest request, HttpServletResponse response, SSOToken ssoToken)
			throws AuthenticationException {
		if (debug.messageEnabled()) {
			debug.message("FirstTimeLogin.onLoginSuccess called: Req:" + request.getRequestURL());
		}

		request.setAttribute(AMPostAuthProcessInterface.POST_PROCESS_LOGIN_SUCCESS_URL, "/opensso/console");
		System.out.println("Redirecting to POST Success URL /opensso/console");

		if (debug.messageEnabled()) {
			debug.message("FirstTimeLogin.onLoginSuccess: FirstTimeLogin concluded successfully");
		}
	}

	/**
	 * Post processing on failed authentication.
	 * 
	 * @param requestParamsMap contains HttpServletRequest parameters
	 * @param req HttpServlet request
	 * @param res HttpServlet response
	 * @throws AuthenticationException if there is an error
	 */
	public void onLoginFailure(Map requestParamsMap, HttpServletRequest req, HttpServletResponse res) throws AuthenticationException {
		debug.message("FirstTimeLogin.onLoginFailure: called");
	}

	/**
	 * Post processing on Logout.
	 * 
	 * @param req HttpServlet request
	 * @param res HttpServlet response
	 * @param ssoToken user's session
	 * @throws AuthenticationException if there is an error
	 */
	public void onLogout(HttpServletRequest req, HttpServletResponse res, SSOToken ssoToken) throws AuthenticationException {
		debug.message("FirstTimeLogin.onLogout called");
	}
}