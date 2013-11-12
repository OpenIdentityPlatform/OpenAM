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
 * $Id: OpenSsoLogoutHandler.java,v 1.1 2008/09/15 18:19:46 robdale Exp $
 *
 */
package com.sun.identity.provider.spring;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.acegisecurity.Authentication;
import org.acegisecurity.ui.logout.LogoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

public class OpenSsoLogoutHandler implements LogoutHandler {

	private final Logger log = LoggerFactory.getLogger(OpenSsoLogoutHandler.class);
	
	public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
		request = HttpUtil.unwrapOriginalHttpServletRequest(request);
		try {
			SSOTokenManager manager = SSOTokenManager.getInstance();
			SSOToken token = manager.createSSOToken(request);
			manager.destroyToken(token);
		} catch (SSOException e) {
			log.debug("Error destroying SSOToken", e);
		} catch (UnsupportedOperationException e) {
			log.debug("Error destroying SSOToken", e);
		}
	}

}
