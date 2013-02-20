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

import java.util.Scanner;

import javax.servlet.http.HttpServletRequest;

import org.easymock.EasyMock;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.DevicePrint;
import org.testng.Assert;
import org.testng.annotations.Test;

public class FormExtractorTest {
	
	@Test
	public void aggregateDevicePrint() {		
		Scanner s = new Scanner(getClass().getResourceAsStream("/devicePrintInfo.json"));
		
		HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
		EasyMock.expect(request.getParameter("devicePrintInfo")).andReturn(s.nextLine());
		EasyMock.replay(request);
		
		FormExtractor formExtractor = new FormExtractor();
		DevicePrint dp = new DevicePrint();
		formExtractor.extractData(dp, request);
		
		Assert.assertEquals(dp.getInstalledPlugins(), "internal-remoting-viewer;libppGoogleNaClPluginChrome.so");		
		Assert.assertEquals(dp.getScreenColorDepth(), "24");		
		Assert.assertEquals(dp.getScreenHeight(), "1080");		
		Assert.assertEquals(dp.getScreenWidth(), "1920");
		Assert.assertEquals(dp.getTimezone(), "-120");
	}

}
