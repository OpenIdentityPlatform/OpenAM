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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.ProfileMatchingRunner;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.ProfileMatchingRunnerResult;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.ComparisonResult;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.DevicePrint;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.UserProfile;

import com.sun.identity.shared.debug.Debug;

public class RiskBasedRunner implements ProfileMatchingRunner{
	
	private static final int NOT_TOLERABLE_PENALTY_POINTS_VALUE = 100;

	private RiskBasedRunnerConfig config;
	
	private RiskBasedSingleProfileMatcher profileMatcher = new RiskBasedSingleProfileMatcher();
	
	private static final Debug debug = Debug
			.getInstance(RiskBasedRunner.class.getName());
	
	public void init(RiskBasedRunnerConfig config) {
		this.config = config;
	}
	
	@Override
	public ProfileMatchingRunnerResult runProfileMatching(
			List<UserProfile> userProfiles, DevicePrint currentDevicePrint) {
		ProfileMatchingRunnerResult result = new ProfileMatchingRunnerResult();
		SortedMap<ComparisonResult, UserProfile> comparisonResultMap = new TreeMap<ComparisonResult, UserProfile>();
		
		for (UserProfile oneUserProfile : userProfiles) {
			 comparisonResultMap.put(profileMatcher.matchDevicePrintAgainstProfile(oneUserProfile, currentDevicePrint, config), oneUserProfile);
		}
		
		//no profiles stored 
		if(comparisonResultMap.isEmpty()) {
			result.getMatchingRuleResult().setRequireHOTPConfirmation(true);
			result.getMatchingRuleResult().setCreateCurrentDevicePrintProfileWithPriorConfirmation(true);
			return result;
		}
		
		ComparisonResult selectedComparisonResult = comparisonResultMap.firstKey();
		UserProfile selectedProfile = comparisonResultMap.get(selectedComparisonResult);
		
		if(debug.messageEnabled()) {
			debug.message("Selected profile: " + selectedProfile);
			debug.message("Aggregated comparison result: " + selectedComparisonResult);
		}
		
		if(selectedComparisonResult.isSuccessful()) {
			result.setSelectedUserProfile(selectedProfile);
			if(selectedComparisonResult.isAdditionalInfoInCurrentValue()) {
				debug.message("additional information in current device print detected. Update the profile.");
				result.getMatchingRuleResult().setUpdateSelectedProfileWithoutPriorConfirmation(true);
			}
			//exact match
		} else if(selectedComparisonResult.getPenaltyPoints() >= NOT_TOLERABLE_PENALTY_POINTS_VALUE) {
			debug.message("selected profile penalty points are not in tolerable area. HOTP+store prompt");
			result.getMatchingRuleResult().setRequireHOTPConfirmation(true);
			
			if(config.getStoreProfilesWithoutConfirmation()) {
				result.getMatchingRuleResult().setCreateCurrentDevicePrintProfileWithoutPriorConfirmation(true);
			} else {
				result.getMatchingRuleResult().setCreateCurrentDevicePrintProfileWithPriorConfirmation(true);
			}
			
		} else {
			result.setSelectedUserProfile(selectedProfile);
			//selected profile penalty points are in tolerable area
			//TODO make it configurable
			//result.getMatchingRuleResult().setCreateCurrentDevicePrintProfileWithoutPriorConfirmation(true);
		}
		return result;
	}
}
