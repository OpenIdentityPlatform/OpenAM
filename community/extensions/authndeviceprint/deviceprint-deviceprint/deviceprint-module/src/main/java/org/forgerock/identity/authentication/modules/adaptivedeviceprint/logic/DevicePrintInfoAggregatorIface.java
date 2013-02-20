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

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.DevicePrint;

/**
 * This interface is implemented by data aggregators.
 * Device print data comes in many flavours like HTTP header info/form info. 
 * Classes implementing this interface should extract all device print data and aggregate it into single POJO
 * @author ljaromin
 *
 */
public interface DevicePrintInfoAggregatorIface {
	
	/**
	 * Extract device print info from servlet response and aggregate into one object
	 * @param servletResponse
	 * @return
	 */
	DevicePrint aggregateDevicePrint(HttpServletRequest servletRequest);
	
	List<ExtractorIface> getExtractors();
	void setExtractors(List<ExtractorIface> extractors);
	void addExtractor(ExtractorIface extractor);
}
