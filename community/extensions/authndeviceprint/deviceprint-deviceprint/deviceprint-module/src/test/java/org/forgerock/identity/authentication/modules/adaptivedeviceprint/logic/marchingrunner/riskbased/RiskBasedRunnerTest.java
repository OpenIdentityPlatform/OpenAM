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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.marchingrunner.riskbased;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.ProfileMatchingRunnerResult;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.RiskBasedRunner;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.RiskBasedRunnerConfig;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.DevicePrint;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.UserProfile;
import org.forgerock.identity.authentication.modules.common.config.JsonHelper;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class RiskBasedRunnerTest {
	
	private RiskBasedRunner cut = new RiskBasedRunner();
	
	private static final String RISK_CONFIG_PATH = "/org/forgerock/identity/authentication/modules/adaptivedeviceprint/logic/marchingrunner/riskbased/RiskBasedConfiguration1.json";
	private static final String RISK_AUTO_STORE_CONFIG_PATH = "/org/forgerock/identity/authentication/modules/adaptivedeviceprint/logic/marchingrunner/riskbased/RiskBasedConfigurationAutoStoreProfile.json";
	private static final String FULL_DEVICE_PRINT_PATH = "/org/forgerock/identity/authentication/modules/adaptivedeviceprint/logic/marchingrunner/riskbased/FullDevicePrint1.json";
	private static final String PROFILES_PATH = "/org/forgerock/identity/authentication/modules/adaptivedeviceprint/logic/marchingrunner/riskbased/StoredProfilesSet1.json";
	private static final String FULL_DEVICE_PRINT_MODIFIED_COOKIE_PATH = "/org/forgerock/identity/authentication/modules/adaptivedeviceprint/logic/marchingrunner/riskbased/FullDevicePrintModifiedCookie.json";
	private static final String FULL_DEVICE_PRINT_WITHOUT_SCREEN_DIMENSION_PATH = "/org/forgerock/identity/authentication/modules/adaptivedeviceprint/logic/marchingrunner/riskbased/FullDevicePrintWOScreenDimension1.json";
	
	@BeforeClass
	public void initCut() {
		RiskBasedRunnerConfig config = JsonHelper.readJSONFromFile(RISK_CONFIG_PATH, RiskBasedRunnerConfig.class);
		cut.init(config);
	}
	
	@Test
	public void testNoProfilesStored() {
		DevicePrint devicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_PATH, DevicePrint.class);
		//List<UserProfile> userProfiles = JsonHelper.readList(PROFILES_PATH, UserProfile.class);
		ProfileMatchingRunnerResult result = cut.runProfileMatching(new ArrayList<UserProfile>(), devicePrint);
		assertTrue(result.getMatchingRuleResult().isRequireHOTPConfirmation());
		assertTrue(result.getMatchingRuleResult().isCreateCurrentDevicePrintProfileWithPriorConfirmation());
		assertFalse(result.getMatchingRuleResult().isCreateCurrentDevicePrintProfileWithoutPriorConfirmation());
		assertFalse(result.getMatchingRuleResult().isUpdateSelectedProfileWithoutPriorConfirmation());
		assertFalse(result.getMatchingRuleResult().isUpdateSelectedProfileWithPriorConfirmation());
	}
	
	@Test
	public void testExactMatch() {
		DevicePrint devicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_PATH, DevicePrint.class);
		List<UserProfile> userProfiles = Arrays.asList(JsonHelper.readJSONFromFile(PROFILES_PATH, UserProfile[].class));
		
		ProfileMatchingRunnerResult result = cut.runProfileMatching(userProfiles, devicePrint);
		assertFalse(result.getMatchingRuleResult().isRequireHOTPConfirmation());
		assertFalse(result.getMatchingRuleResult().isCreateCurrentDevicePrintProfileWithPriorConfirmation());
		assertFalse(result.getMatchingRuleResult().isCreateCurrentDevicePrintProfileWithoutPriorConfirmation());
		assertFalse(result.getMatchingRuleResult().isUpdateSelectedProfileWithoutPriorConfirmation());
		assertFalse(result.getMatchingRuleResult().isUpdateSelectedProfileWithPriorConfirmation());
	}
	
	@Test
	public void testNonExactMatch() {
		DevicePrint devicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_WITHOUT_SCREEN_DIMENSION_PATH, DevicePrint.class);
		List<UserProfile> userProfiles = Arrays.asList(JsonHelper.readJSONFromFile(PROFILES_PATH, UserProfile[].class));
		
		ProfileMatchingRunnerResult result = cut.runProfileMatching(userProfiles, devicePrint);
		assertFalse(result.getMatchingRuleResult().isRequireHOTPConfirmation());
		assertFalse(result.getMatchingRuleResult().isCreateCurrentDevicePrintProfileWithPriorConfirmation());
		assertFalse(result.getMatchingRuleResult().isCreateCurrentDevicePrintProfileWithoutPriorConfirmation());
		assertFalse(result.getMatchingRuleResult().isUpdateSelectedProfileWithoutPriorConfirmation());
		assertFalse(result.getMatchingRuleResult().isUpdateSelectedProfileWithPriorConfirmation());
	}
	
	@Test
	public void testNoMatch() {
		DevicePrint devicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_MODIFIED_COOKIE_PATH, DevicePrint.class);
		List<UserProfile> userProfiles = Arrays.asList(JsonHelper.readJSONFromFile(PROFILES_PATH, UserProfile[].class));
		
		ProfileMatchingRunnerResult result = cut.runProfileMatching(userProfiles, devicePrint);
		assertTrue(result.getMatchingRuleResult().isRequireHOTPConfirmation());
		assertTrue(result.getMatchingRuleResult().isCreateCurrentDevicePrintProfileWithPriorConfirmation());
		assertFalse(result.getMatchingRuleResult().isCreateCurrentDevicePrintProfileWithoutPriorConfirmation());
		assertFalse(result.getMatchingRuleResult().isUpdateSelectedProfileWithoutPriorConfirmation());
		assertFalse(result.getMatchingRuleResult().isUpdateSelectedProfileWithPriorConfirmation());
	}
	
	@Test
	public void testNoMatchAutomaticallyStoreProfile() {
		RiskBasedRunnerConfig config = JsonHelper.readJSONFromFile(RISK_AUTO_STORE_CONFIG_PATH, RiskBasedRunnerConfig.class);
		cut.init(config);
		
		DevicePrint devicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_MODIFIED_COOKIE_PATH, DevicePrint.class);
		List<UserProfile> userProfiles = Arrays.asList(JsonHelper.readJSONFromFile(PROFILES_PATH, UserProfile[].class));
		
		ProfileMatchingRunnerResult result = cut.runProfileMatching(userProfiles, devicePrint);
		assertTrue(result.getMatchingRuleResult().isRequireHOTPConfirmation());
		assertFalse(result.getMatchingRuleResult().isCreateCurrentDevicePrintProfileWithPriorConfirmation());
		assertTrue(result.getMatchingRuleResult().isCreateCurrentDevicePrintProfileWithoutPriorConfirmation());
		assertFalse(result.getMatchingRuleResult().isUpdateSelectedProfileWithoutPriorConfirmation());
		assertFalse(result.getMatchingRuleResult().isUpdateSelectedProfileWithPriorConfirmation());
	}
	
}
