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

import org.apache.commons.lang.StringUtils;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import org.forgerock.openam.authentication.modules.passphrase.common.utility.CommonUtilities;
import org.forgerock.openam.authentication.modules.passphrase.common.utility.PassphraseConstants;
import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;

/**
 * This post authentication processing plug-in cross checks if password reset flag is enabled
 * or passphrase not provided and forwads the request to 'PassphraseEntryService' else to the
 * 'PassphraseService' after processing the initial DS authentication.
 */
@SuppressWarnings("unchecked")
public class FirstTimeLogin implements AMPostAuthProcessInterface {

	private static Debug debug = Debug.getInstance("FirstTimeLogin");

	/**
	 * Post processing on successful authentication.
	 * 
	 * @param requestParamsMap contains HttpServletRequest parameters
	 * @param request HttpServlet request
	 * @param response HttpServlet response
	 * @param ssoToken user's session
	 * @throws AuthenticationException if there is an error while setting the session paswword property
	 */
	public void onLoginSuccess(Map requestParamsMap, HttpServletRequest request, HttpServletResponse response, SSOToken ssoToken)
			throws AuthenticationException {
		if (debug.messageEnabled()) {
			debug.message("FirstTimeLogin.onLoginSuccess called: Req:" + request.getRequestURL());
		}

		try {
			AMIdentity user = IdUtils.getIdentity(ssoToken);
			String pwdForceResetValue = CollectionHelper.getMapAttr(user.getAttributes(), CommonUtilities.getProperty(PassphraseConstants.USER_PASSWORD_RESET_FLAG_ATTRIBUTE));
			String passphraseValue = CollectionHelper.getMapAttr(user.getAttributes(), CommonUtilities.getProperty(PassphraseConstants.USER_PASSPHRASE_ATTRIBUTE));
			debug.message("SSO token principal name : " + ssoToken.getPrincipal().getName());

			if ((pwdForceResetValue != null && pwdForceResetValue.equalsIgnoreCase("true")) || StringUtils.isEmpty(passphraseValue)) {
					request.setAttribute(AMPostAuthProcessInterface.POST_PROCESS_LOGIN_SUCCESS_URL,
									"/opensso/UI/Login?service=PassphraseEntryService" + getQueryString(request, user));
					debug.message("Redirecting to POST Success URL: opensso/UI/Login?service=PassphraseEntryService" + getQueryString(request, user));
			} else {
				request.setAttribute(AMPostAuthProcessInterface.POST_PROCESS_LOGIN_SUCCESS_URL,
								"/opensso/UI/Login?service=PassphraseService" + getQueryString(request, user));
				debug.message("Redirecting to POST Success URL  opensso/UI/Login?service=PassphraseService" + getQueryString(request, user));
			}

			if (debug.messageEnabled()) {
				debug.message("FirstTimeLogin.onLoginSuccess: FirstTimeLogin " + "concluded successfully");
			}
		} catch (IdRepoException ire) {
			debug.error("FirstTimeLogin.onLoginSuccess: IOException while " + "fetching user attributes: " + ire);
		} catch (SSOException sse) {
			debug.error("FirstTimeLogin.onLoginSuccess: SSOException while " + "setting session password property: " + sse);
		}
	}

	/**
	 * This method is uset to create a query string based on user realm and provided goto url.
	 * 
	 * @param request
	 * @param user 
	 * @return
	 */
	private String getQueryString(HttpServletRequest request, AMIdentity user) {
		StringBuilder sb = new StringBuilder();
		if (user.getRealm().contains("o=")) {
			String realm = user.getRealm();
			int index = realm.indexOf("o=") + 2;
			realm = realm.substring(index, realm.indexOf(",", index));
			sb.append("&").append("realm=").append(realm);
		}
		if (StringUtils.isNotBlank(request.getParameter("goto")))
			sb.append("&").append("goto=").append(request.getParameter("goto"));
		return sb.toString();
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