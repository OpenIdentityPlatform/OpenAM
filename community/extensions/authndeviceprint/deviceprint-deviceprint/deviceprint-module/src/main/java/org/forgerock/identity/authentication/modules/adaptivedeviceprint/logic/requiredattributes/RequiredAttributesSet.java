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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.requiredattributes;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.DevicePrint;

public class RequiredAttributesSet implements RequiredAttributesSetIface {

	private List<EssentialAttributeCheckerIface> checkers = new ArrayList<EssentialAttributeCheckerIface>();

	@Override
	public void addChecker(EssentialAttributeCheckerIface checker) {
		checkers.add(checker);
	}

	@Override
	public boolean hasRequiredAttributes(DevicePrint dp) {
		for(EssentialAttributeCheckerIface checker : checkers) {
			if(!checker.hasRequiredAttribute(dp)) {
				return false;
			}
		}
		
		return true;
	}

	@Override
	public void init(RequiredAttributesConfig config) {
		
		if(config.isFontsRequired()) {
			addChecker(new AttributesChecker("installedFonts"));
		}
		
		if(config.isGeolocationRequired()) {
			addChecker(new AttributesChecker("latitude","longitude"));
		}
		
		if(config.isPluginsRequired()) {
			addChecker(new AttributesChecker("installedPlugins"));
		}
		
		if(config.isScreenParamsRequired()) {
			addChecker(new AttributesChecker("screenColorDepth","screenHeight","screenWidth"));
		}
		
		if(config.isTimezoneRequired()) {
			addChecker(new AttributesChecker("timezone"));
		}
		
		if(config.isUserAgentRequired()) {
			addChecker(new AttributesChecker("userAgent"));
		}			
	}

}
