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

package org.forgerock.identity.authentication.modules.common;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.authentication.spi.AuthenticationException;

public class AMPostAuthProcessAdapter implements AMPostAuthProcessInterface {

	@SuppressWarnings("rawtypes")
	@Override
	public void onLoginFailure(Map arg0, HttpServletRequest arg1,
			HttpServletResponse arg2) throws AuthenticationException {
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void onLoginSuccess(Map arg0, HttpServletRequest arg1,
			HttpServletResponse arg2, SSOToken arg3)
			throws AuthenticationException {
	}

	@Override
	public void onLogout(HttpServletRequest arg0, HttpServletResponse arg1,
			SSOToken arg2) throws AuthenticationException {
	}

}
