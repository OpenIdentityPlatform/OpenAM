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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.DevicePrint;

public class DefaultDevicePrintAggregator implements
		DevicePrintInfoAggregatorIface {

	private List<ExtractorIface> extractors = new ArrayList<ExtractorIface>();

	@Override
	public DevicePrint aggregateDevicePrint(HttpServletRequest servletRequest) {
		DevicePrint dp = new DevicePrint();
		
		for(ExtractorIface extractor : extractors) {
			extractor.extractData(dp, servletRequest);
		}

		return dp;
	}
	
	@Override
	public List<ExtractorIface> getExtractors() {
		return extractors;
	}
	
	@Override
	public void setExtractors(List<ExtractorIface> extractors) {
		this.extractors = extractors;
	}

	@Override
	public void addExtractor(ExtractorIface extractor) {
		extractors.add(extractor);
	}

}
