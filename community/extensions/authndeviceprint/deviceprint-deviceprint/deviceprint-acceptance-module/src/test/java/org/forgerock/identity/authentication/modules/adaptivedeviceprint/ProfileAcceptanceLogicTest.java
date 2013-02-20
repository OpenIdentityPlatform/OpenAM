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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.LoginException;

import junit.framework.Assert;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.configuration.ProfileAcceptanceCommonConfig;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.configuration.ProfileAcceptanceInput;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.UserProfilesHelperIface;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.DevicePrint;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.EmptyUserProfileNameException;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.NotUniqueUserProfileException;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.ProfileMatchingRuleResult;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.UserProfile;
import org.forgerock.identity.authentication.modules.common.config.MapObjectTransformer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ProfileAcceptanceLogicTest {
	
	private static final String ADAPTIVE_PROFILES_FIELD_NAME = "adaptive-profiles";
	
	private ProfileAcceptanceLogic profile;
	private UserProfilesHelperIface saver;
	
	@BeforeMethod
	public void beforeTest() {
		 saver = mock(UserProfilesHelperIface.class);
		 
		 profile = new ProfileAcceptanceLogic();
		 profile.setUserProfileSaver(saver);
	}
	
	@Test
	public void init() throws LoginException {
		ProfileMatchingRuleResult ruleResult = new ProfileMatchingRuleResult();
		UserProfile up = new UserProfile();
		DevicePrint dp = new DevicePrint();
		
		
		ProfileAcceptanceInput profileAcceptanceInput = new ProfileAcceptanceInput();
		profileAcceptanceInput.setMatchingModuleNotFailed(true);
		profileAcceptanceInput.setMatchingResult(ruleResult);
		profileAcceptanceInput.setSelectedUserProfile(up);
		profileAcceptanceInput.setCurrentDevicePrint(dp);
		
		ProfileAcceptanceCommonConfig config = createConfig();
		profileAcceptanceInput.setCommonConfig(config);
		Map<String, Object> sharedState = new HashMap<String, Object>(MapObjectTransformer.convertObjectToMap(profileAcceptanceInput));
		
		profile.init(sharedState);
		
		Assert.assertEquals(ruleResult, profile.getInputParams().getMatchingResult());
		Assert.assertEquals(up, profile.getInputParams().getSelectedUserProfile());
		Assert.assertEquals(dp, profile.getInputParams().getCurrentDevicePrint());
		Assert.assertEquals(ADAPTIVE_PROFILES_FIELD_NAME, profile.getInputParams().getCommonConfig().getAdaptiveProfilesFieldName());
	}

	private ProfileAcceptanceCommonConfig createConfig() {
		ProfileAcceptanceCommonConfig config = new ProfileAcceptanceCommonConfig();
		config.setAdaptiveProfilesFieldName(ADAPTIVE_PROFILES_FIELD_NAME);
		return config;
	}
	
	@Test
	public void proccedNotStoring() throws LoginException, NotUniqueUserProfileException, EmptyUserProfileNameException {
		profile.proccedStoring("", false);
		verify(saver, never()).saveProfile((UserProfile) any());
	}
	
	@Test
	public void proccedStoring() throws LoginException, NotUniqueUserProfileException, EmptyUserProfileNameException {
		ProfileMatchingRuleResult ruleResult = new ProfileMatchingRuleResult();
		
		ruleResult.setCreateCurrentDevicePrintProfileWithPriorConfirmation(true);
		
		UserProfile up = new UserProfile();
		up.setSelectionCounter(0L);
		
		DevicePrint dp = new DevicePrint();
		up.setDevicePrint(dp);
		
		ProfileAcceptanceInput profileAcceptanceInput = new ProfileAcceptanceInput();
		profileAcceptanceInput.setMatchingModuleNotFailed(true);
		profileAcceptanceInput.setMatchingResult(ruleResult);
		profileAcceptanceInput.setSelectedUserProfile(up);
		profileAcceptanceInput.setCurrentDevicePrint(dp);
		ProfileAcceptanceCommonConfig config = createConfig();
		profileAcceptanceInput.setCommonConfig(config);
		Map<String, Object> sharedState = new HashMap<String, Object>(MapObjectTransformer.convertObjectToMap(profileAcceptanceInput));
		
		profile.init(sharedState);
		
		profile.proccedStoring("", true);
		verify(saver, times(1)).saveProfile((UserProfile) anyObject());
		
		ruleResult.setCreateCurrentDevicePrintProfileWithPriorConfirmation(false);
		ruleResult.setUpdateSelectedProfileWithPriorConfirmation(true);
		
		profile.proccedStoring("", true);
		verify(saver, times(2)).saveProfile((UserProfile) anyObject());
		
		ruleResult.setUpdateSelectedProfileWithPriorConfirmation(false);
		
		profile.proccedStoring("", true);
		verify(saver, times(2)).saveProfile((UserProfile) anyObject());
	}

	@Test
	public void proccedInitSkipped() throws LoginException, NotUniqueUserProfileException, EmptyUserProfileNameException {
		ProfileMatchingRuleResult ruleResult = new ProfileMatchingRuleResult();
		
		ruleResult.setCreateCurrentDevicePrintProfileWithoutPriorConfirmation(true);
		
		UserProfile up = new UserProfile();
		up.setSelectionCounter(0L);
		
		DevicePrint dp = new DevicePrint();
		up.setDevicePrint(dp);
		
		ProfileAcceptanceInput profileAcceptanceInput = new ProfileAcceptanceInput();
		profileAcceptanceInput.setMatchingModuleNotFailed(true);
		profileAcceptanceInput.setMatchingResult(ruleResult);
		profileAcceptanceInput.setSelectedUserProfile(up);
		profileAcceptanceInput.setCurrentDevicePrint(dp);
		ProfileAcceptanceCommonConfig config = createConfig();
		profileAcceptanceInput.setCommonConfig(config);
		Map<String, Object> sharedState = new HashMap<String, Object>(MapObjectTransformer.convertObjectToMap(profileAcceptanceInput));
		
		profile.init(sharedState);
	
		Assert.assertEquals(true, profile.proccedInit());
		verify(saver, times(1)).saveProfile((UserProfile) anyObject());
		
		ruleResult.setCreateCurrentDevicePrintProfileWithoutPriorConfirmation(false);
		ruleResult.setUpdateSelectedProfileWithoutPriorConfirmation(true);
		
		Assert.assertEquals(true, profile.proccedInit());
		verify(saver, times(2)).saveProfile((UserProfile) anyObject());
		
		ruleResult.setUpdateSelectedProfileWithoutPriorConfirmation(false);
		ruleResult.setUpdateSelectedProfileWithPriorConfirmation(false);
		
		Assert.assertEquals(true, profile.proccedInit());
		verify(saver, times(3)).saveProfile((UserProfile) anyObject());
	}
	
	@Test
	public void proccedInitNotSkipped() throws LoginException, NotUniqueUserProfileException, EmptyUserProfileNameException {
		ProfileMatchingRuleResult ruleResult = new ProfileMatchingRuleResult();
		
		ProfileAcceptanceInput profileAcceptanceInput = new ProfileAcceptanceInput();
		profileAcceptanceInput.setMatchingModuleNotFailed(true);
		profileAcceptanceInput.setMatchingResult(ruleResult);
		ProfileAcceptanceCommonConfig config = createConfig();
		profileAcceptanceInput.setCommonConfig(config);
		Map<String, Object> sharedState = new HashMap<String, Object>(MapObjectTransformer.convertObjectToMap(profileAcceptanceInput));
		
		profile.init(sharedState);
		
		Assert.assertEquals(false, profile.proccedInit());
		verify(saver, never()).saveProfile((UserProfile) anyObject());
	}
	
	@Test
	public void getDefaultName() throws LoginException {	
		ProfileAcceptanceInput profileAcceptanceInput = new ProfileAcceptanceInput();
		profileAcceptanceInput.setMatchingModuleNotFailed(true);
		ProfileAcceptanceCommonConfig config = createConfig();
		config.setDefaultProfileName("def");
		config.setProfileMaximumProfilesStoredQuantity(30);
		profileAcceptanceInput.setCommonConfig(config);
		Map<String, Object> sharedState = new HashMap<String, Object>(MapObjectTransformer.convertObjectToMap(profileAcceptanceInput));
		
		profile.init(sharedState);
		
		when(saver.isUniqueName(anyString())).thenReturn(true);		
		Assert.assertEquals("def", profile.getDefaultName());
		
		when(saver.isUniqueName(anyString())).thenReturn(false, false, true);		
		Assert.assertEquals("def2", profile.getDefaultName());
	}
	
	@Test
	public void suggestedNameIsSelectedProfileName() throws LoginException {
		UserProfile up = new UserProfile();
		String name = "a";
		up.setName(name);

		ProfileAcceptanceInput profileAcceptanceInput = new ProfileAcceptanceInput();
		profileAcceptanceInput.setMatchingModuleNotFailed(true);
		profileAcceptanceInput.setSelectedUserProfile(up);
		ProfileAcceptanceCommonConfig config = createConfig();
		profileAcceptanceInput.setCommonConfig(config);
		Map<String, Object> sharedState = new HashMap<String, Object>(MapObjectTransformer.convertObjectToMap(profileAcceptanceInput));
		
		profile.init(sharedState);
		
		Assert.assertEquals(name, profile.getDefaultName());
	}

}
