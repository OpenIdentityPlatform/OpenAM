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

import javax.servlet.http.HttpServletRequest;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.DevicePrintLoginModule;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.DevicePrint;
import org.forgerock.identity.authentication.modules.common.config.ReadableDateMapper;

import com.sun.identity.shared.debug.Debug;

public class FormExtractor implements ExtractorIface {

	private static final String DEVICE_PRINT_INFO = "devicePrintInfo";
	private static final Debug debug = Debug.getInstance(DevicePrintLoginModule.class.getName());

	/**
	 * Extracts (so far) screen params, timezone and plugins from http header to
	 * DevicePrint.
	 */
	@Override
	public void extractData(DevicePrint devicePrint, HttpServletRequest request) {
		String devicePrintInfo = request.getParameter(DEVICE_PRINT_INFO);

		if(devicePrintInfo == null) {
			debug.warning("HTTP form doesn't have " + DEVICE_PRINT_INFO + " attribute. No data extracted from form.");
		}
		
		if(debug.messageEnabled()) {
			debug.message("Extracted device print info: " + devicePrintInfo);
		}
		
		ReadableDateMapper mapper = new ReadableDateMapper();
		DevicePrint devicePrintFromJson = new DevicePrint();

		try {
			devicePrintFromJson = mapper.readValue(devicePrintInfo, DevicePrint.class);
		} catch (Exception e) {
			debug.error("Error while parsing json",e);
		}
		
		devicePrint.merge(devicePrintFromJson);
	}

}
