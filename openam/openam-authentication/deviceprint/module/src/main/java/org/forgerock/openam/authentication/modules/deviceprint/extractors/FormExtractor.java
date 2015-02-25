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
/*
 * Portions Copyrighted 2013 Syntegrity.
 * Portions Copyrighted 2013-2014 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.deviceprint.extractors;

import com.sun.identity.shared.debug.Debug;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.forgerock.openam.authentication.modules.deviceprint.model.DevicePrint;

import javax.servlet.http.HttpServletRequest;

/**
 * Extracts Device Print information from the Http Form.
 */
public class FormExtractor implements Extractor {
    /**
     * Singleton ObjectMapper instance for performance.
     */
    private static final ObjectMapper mapper = new ObjectMapper().configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
	private static final String DEVICE_PRINT_INFO = "IDToken0";
	private static final Debug DEBUG = Debug.getInstance("amAuthDevicePrint");

	/**
     * {@inheritDoc}
     *
	 * Extracts screen params, timezone and plugins from http header to DevicePrint.
	 */
	public void extractData(DevicePrint devicePrint, HttpServletRequest request) {
		String devicePrintInfo = request.getParameter(DEVICE_PRINT_INFO);

		if (devicePrintInfo == null) {
			DEBUG.warning("HTTP form doesn't have " + DEVICE_PRINT_INFO + " attribute. No data extracted from form.");
		}
		
		if (DEBUG.messageEnabled()) {
			DEBUG.message("Extracted device print info: " + devicePrintInfo);
		}

		DevicePrint devicePrintFromJson = new DevicePrint();

		try {
			devicePrintFromJson = mapper.readValue(devicePrintInfo, DevicePrint.class);
		} catch (Exception e) {
			DEBUG.error("Error while parsing json", e);
		}
		
		devicePrint.merge(devicePrintFromJson);
	}
}
