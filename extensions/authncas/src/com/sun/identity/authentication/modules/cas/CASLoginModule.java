/**
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
 * $Id: CASLoginModule.java,v 1.1 2009/07/16 20:17:25 superpat7 Exp $
 *
 * Portions Copyrighted 2009 Qingfeng Zhang
 *
 */

package com.sun.identity.authentication.modules.cas;

import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import com.iplanet.am.util.Debug;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;

import edu.yale.its.tp.cas.client.ServiceTicketValidator;

public class CASLoginModule extends AMLoginModule {

	private String userTokenId;

	private java.security.Principal userPrincipal = null;

	private String casLoginURL;

	private String casValidateURL;

	private String urlOfThisService;

	protected static Debug debug = Debug.getInstance("CASAMLoginModule");

	/** Creates a new instance of Login */
	public CASLoginModule() throws LoginException {
		if (debug.messageEnabled())
			debug.message("CASLoginModule instance was created");
	}

	public void init(Subject subject, Map sharedState, Map options) {
		if (debug.messageEnabled()) {
			debug.message("CASAMLoginModule.init()");
		}

		casLoginURL = (String) ((HashSet) (options.get("casloginurl")))
				.iterator().next();
		casValidateURL = (String) ((HashSet) (options.get("casvalidationurl")))
				.iterator().next();
		urlOfThisService = URLEncoder.encode((String) ((HashSet) (options
				.get("serviceurl"))).iterator().next());
	}

	public int process(Callback[] callbacks, int state)
			throws AuthLoginException {
		int currentState = state;

		HttpServletRequest request = getHttpServletRequest();

		String loginFailureURL = casLoginURL + "?service=" + urlOfThisService;
		setLoginFailureURL(loginFailureURL);

		String ticket = request.getParameter("ticket");
		System.out.println("CASAMLoginModule ticket" + ticket);

		if (ticket == null) {
			if (debug.messageEnabled()) {
				debug.message("CASAMLoginModule Authentication No Ticket!");
			}
			throw new AuthLoginException("no ticket found!");
		}

		String user = null;
		// String errorCode = null;
		String errorMessage = null;

		/* instantiate a new ServiceTicketValidator */
		ServiceTicketValidator sv = new ServiceTicketValidator();
		/* set its parameters */
		sv.setCasValidateUrl(casValidateURL);
		sv.setService(urlOfThisService);
		sv.setServiceTicket(ticket);

		/* contact CAS and validate */
		try {
			sv.validate();
		} catch (Exception e) {
			if (debug.messageEnabled()) {
				debug.message("CASAMLoginModule Authentication Validate Fail!");
			}
			throw new AuthLoginException(e.getMessage());
		}

		if (sv.isAuthenticationSuccesful()) {
			user = sv.getUser();
		} else {
			// errorCode = sv.getErrorCode();
			errorMessage = sv.getErrorMessage();
			/* handle the error */
			if (debug.messageEnabled()) {
				debug.message("CASAMLoginModule Authentication Fail!"
						+ errorMessage);
			}
			throw new AuthLoginException(errorMessage);
		}

		/* The user is now authenticated. */
		userTokenId = user;

		if (debug.messageEnabled()) {
			debug.message("CASAMLoginModule Authentication Pass!");
		}

		return -1;
	}

	public java.security.Principal getPrincipal() {
		if (userPrincipal != null) {
			return userPrincipal;
		} else if (userTokenId != null) {
			userPrincipal = new CASAMPrincipal(userTokenId);
			return userPrincipal;
		} else {
			return null;
		}
	}

}