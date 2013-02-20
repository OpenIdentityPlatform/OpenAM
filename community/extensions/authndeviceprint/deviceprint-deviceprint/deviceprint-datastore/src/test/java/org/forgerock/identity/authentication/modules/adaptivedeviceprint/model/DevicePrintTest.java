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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint.model;

import org.testng.Assert;
import org.testng.annotations.Test;

public class DevicePrintTest {
	
	private static final String VALUE = "a";
	private static final double POSITION = 15.5;
	
	@Test
	public void hashcode() {
		DevicePrint dp = new DevicePrint();
		Assert.assertTrue(dp.hashCode() != 0);
	}
	
	@Test
	public void equals() {
		DevicePrint dp1 = new DevicePrint();
		dp1.setInstalledFonts(VALUE);
		dp1.setInstalledPlugins(VALUE);
		dp1.setLatitude(POSITION);
		dp1.setLongitude(POSITION);
		dp1.setPersistentCookie(VALUE);
		dp1.setScreenColorDepth(VALUE);
		dp1.setScreenHeight(VALUE);
		dp1.setScreenWidth(VALUE);
		dp1.setTimezone(VALUE);
		dp1.setUserAgent(VALUE);
		
		DevicePrint dp2 = new DevicePrint();
		dp2.setInstalledFonts(VALUE);
		dp2.setInstalledPlugins(VALUE);
		dp2.setLatitude(POSITION);
		dp2.setLongitude(POSITION);
		dp2.setPersistentCookie(VALUE);
		dp2.setScreenColorDepth(VALUE);
		dp2.setScreenHeight(VALUE);
		dp2.setScreenWidth(VALUE);
		dp2.setTimezone(VALUE);
		dp2.setUserAgent(VALUE);
		
		Assert.assertTrue(dp1.equals(dp1));
		Assert.assertTrue(dp1.equals(dp2));
	}
}
