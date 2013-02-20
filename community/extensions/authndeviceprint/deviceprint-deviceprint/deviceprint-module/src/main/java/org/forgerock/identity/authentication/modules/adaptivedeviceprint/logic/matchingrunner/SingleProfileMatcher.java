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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner;

import java.util.List;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.DevicePrint;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.ProfileMatchingRuleResult;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.UserProfile;

/**
 * Single profile matcher. It matches single device print against single profile.
 * @author ljaromin
 *
 */
public interface SingleProfileMatcher {
	
	/**
	 * This method is matching single device print against single profile. 
	 * Auxiliary variables are also provided which can be useful in decision process.
	 *  
	 * @param userProfile The profile currently analyzed
	 * @param devicePrint Current device print
	 * @param allProfiles All profiles including currently analyzed profile. Helpful during historical user habits.
	 * @return Matching result
	 */
	ProfileMatchingRuleResult matchDevicePrintAgainstProfile(UserProfile userProfile, DevicePrint devicePrint, List<UserProfile> allProfiles);
}
