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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.DevicePrint;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.UserProfile;
import org.testng.annotations.Test;

import com.sun.identity.authentication.spi.AuthenticationException;

public class PersistentCookieAdderTest {

	@Test(enabled=false)
	public void addData() {
		UserProfile up = new UserProfile();
		
		DevicePrint dp = new DevicePrint();
		String cookie = "hash";

		dp.setPersistentCookie(cookie);
		up.setDevicePrint(dp);
		
		PostAuthenticationCookieAdder p = new PostAuthenticationCookieAdder();
		
		HttpServletResponse response = mock(HttpServletResponse.class);
		HttpServletRequest request = mock(HttpServletRequest.class);
		request.getSession().setAttribute("a", "b");
		
		try {
			p.onLoginSuccess(null, request, response, null);
		} catch (AuthenticationException e) {
			e.printStackTrace();
		}
		
		verify(response, times(1)).addCookie((Cookie) any());
	}
}
