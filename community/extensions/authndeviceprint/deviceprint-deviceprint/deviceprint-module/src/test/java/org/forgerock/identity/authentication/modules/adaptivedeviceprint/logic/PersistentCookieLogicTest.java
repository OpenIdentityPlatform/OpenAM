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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic;

import java.util.Date;

import junit.framework.Assert;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.DevicePrint;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.UserProfile;
import org.testng.annotations.Test;

public class PersistentCookieLogicTest {

	@Test
	public void proceed() {
		PersistentCookieLogicIface logic = new PersistentCookieLogic();
		logic.setPersistentCookieAge(5);
		
		DevicePrint dp = new DevicePrint();
		UserProfile up = new UserProfile();
		up.setLastPersistentCookieUpdateDate(new Date(110,1,1));
		
		Assert.assertTrue(logic.proceed(null, dp));
		Assert.assertNotNull(dp.getPersistentCookie());
		
		dp.setPersistentCookie(null);
		
		up.setDevicePrint(dp);
		Assert.assertTrue(logic.proceed(up, dp));
		Assert.assertNotNull(dp.getPersistentCookie());
		
		up.setLastPersistentCookieUpdateDate(new Date());
		Assert.assertFalse(logic.proceed(up, dp));
	}	
}
