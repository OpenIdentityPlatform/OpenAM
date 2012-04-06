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
 * $Id: OpenSSOLogoutHandler.java,v 1.1 2009/02/26 18:18:50 wstrange Exp $
 *
 */
package com.sun.identity.provider.springsecurity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.Authentication;
import org.springframework.security.ui.logout.LogoutHandler;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.debug.Debug;

/**
 * It is in charge of doing the logout in the application and in every application
 * where the user logged via Single sign-on.
 * @see LogoutHandler
 */
public class OpenSSOLogoutHandler implements LogoutHandler {
        private static Debug debug = Debug.getInstance("amSpring");
	
	public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
		request = HttpUtil.unwrapOriginalHttpServletRequest(request);
		try {
			SSOTokenManager manager = SSOTokenManager.getInstance();
			SSOToken token = manager.createSSOToken(request);
			manager.destroyToken(token);
		} catch (SSOException e) {
			debug.error("Error destroying SSOToken", e);
		} catch (UnsupportedOperationException e) {
			debug.error("Error destroying SSOToken", e);
		}
	}
}
