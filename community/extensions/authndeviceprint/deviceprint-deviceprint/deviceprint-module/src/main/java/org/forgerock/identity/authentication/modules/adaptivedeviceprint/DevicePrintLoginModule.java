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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.configuration.DevicePrintLoginModuleCommonConfig;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.configuration.ProfileAcceptanceCommonConfig;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.configuration.ProfileAcceptanceInput;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.CookiesExtractor;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.DefaultDevicePrintAggregator;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.DevicePrintInfoAggregatorIface;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.FormExtractor;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.HttpHeadersExtractor;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.PersistentCookieLogic;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.PersistentCookieLogicIface;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.UserProfilesHelper;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.UserProfilesHelperIface;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.ProfileMatchingRunner;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.ProfileMatchingRunnerResult;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.RiskBasedRunner;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.RiskBasedRunnerConfig;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.requiredattributes.RequiredAttributesConfig;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.requiredattributes.RequiredAttributesSet;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.requiredattributes.RequiredAttributesSetIface;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.DevicePrint;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.UserProfile;
import org.forgerock.identity.authentication.modules.common.AbstractEnhancedLoginModule;
import org.forgerock.identity.authentication.modules.common.config.AbstractObjectAttributesTransformer;
import org.forgerock.identity.authentication.modules.common.config.ConfigurationObjectTransformer;
import org.forgerock.identity.authentication.modules.common.config.MapObjectTransformer;

import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;

public class DevicePrintLoginModule extends AbstractEnhancedLoginModule {

	/**
	 * Name of the resource-bundle
	 */
	private static final String RESOURCE_BUNDLE_NAME = "amAuthDevicePrintLoginModule";
	
	/**
	 * Name of the shared state skip hotp field
	 */
	private static final String SHARED_STATE_SKIP_HOTP = "skipHOTP";	

	/**
	 * Response device print data aggregator
	 */
	private DevicePrintInfoAggregatorIface devicePrintInfoAggregator = new DefaultDevicePrintAggregator();

	/**
	 * Profile matcher instance. It selects matching profile.
	 */
	private ProfileMatchingRunner profileMatchingRunner;

	private AbstractObjectAttributesTransformer configurationReader;
	
	private PersistentCookieLogicIface persistentCookieLogic = new PersistentCookieLogic();
	private boolean storeCookie = false;
	
	private RequiredAttributesSetIface requiredAttributesSet = new RequiredAttributesSet();
	
	private UserProfilesHelperIface userProfiles = new UserProfilesHelper();

	/**
	 * Logger
	 */
	private static final Debug debug = Debug
			.getInstance(DevicePrintLoginModule.class.getName());

	@SuppressWarnings("unchecked")
	@Override
	public void init(Subject subject, @SuppressWarnings("rawtypes") Map sharedState, @SuppressWarnings("rawtypes") Map options) {
		super.init(subject, sharedState, options);
		
		configurationReader = new ConfigurationObjectTransformer(options);
		
		DevicePrintLoginModuleCommonConfig commonConfig = configurationReader.createObjectUsingAttributes(DevicePrintLoginModuleCommonConfig.class);
		
		devicePrintInfoAggregator.addExtractor(new FormExtractor());
		devicePrintInfoAggregator.addExtractor(new HttpHeadersExtractor());
		devicePrintInfoAggregator.addExtractor(new CookiesExtractor());
		
		RiskBasedRunner riskBasedRunner = new RiskBasedRunner();
		
		riskBasedRunner.init(configurationReader.createObjectUsingAttributes(RiskBasedRunnerConfig.class));
		profileMatchingRunner = riskBasedRunner;
		
		requiredAttributesSet.init(configurationReader.createObjectUsingAttributes(RequiredAttributesConfig.class));
		
		persistentCookieLogic.setPersistentCookieAge(commonConfig.getPersistenceCookieAge());
		
		userProfiles.setProfileExpirationDays(commonConfig.getProfileExpirationDays());
		
		try {
			userProfiles.init(commonConfig.getAdaptiveProfilesFieldName(), getIdentity(getUsername()));
		} catch (LoginException e) {
			debug.error("Could not get profiles", e);
		}		
	}


	@Override
	public int process(Callback[] arg0, int arg1) throws LoginException {		
		debug.message("start process");				

		DevicePrint currentDevicePrint = getCurrentDevicePrint(getHttpServletRequest());
		
		if(!requiredAttributesSet.hasRequiredAttributes(currentDevicePrint)) {
			debug.message("DevicePrint does not have all required attributes. Profile will not be stored");
			
			//its optional because its their default value
			sharedState.put(SHARED_STATE_SKIP_HOTP, false);
			ProfileAcceptanceInput profileAcceptanceInput = new ProfileAcceptanceInput();
			profileAcceptanceInput.setMatchingModuleNotFailed(false);
			sharedState.putAll(MapObjectTransformer.convertObjectToMap(profileAcceptanceInput));
			
			return ISAuthConstants.LOGIN_SUCCEED;
		}
		
		List<UserProfile> storedUserProfiles = userProfiles.getNotExpiredProfiles();
		if(debug.messageEnabled()) { debug.message(storedUserProfiles.size() + " profiles found"); }

		//select matching profile
		ProfileMatchingRunnerResult profileMatchinRunnerResult = profileMatchingRunner.runProfileMatching(storedUserProfiles, currentDevicePrint);
		
		//cookie storing logic
		storeCookie = persistentCookieLogic.proceed(profileMatchinRunnerResult.getSelectedUserProfile(), currentDevicePrint);
		
		//set shared state variables for next modules
		setSharedStateVariables(profileMatchinRunnerResult, currentDevicePrint);

		return ISAuthConstants.LOGIN_SUCCEED;
	}

	/**
	 * Create and set shared state variables. Variables are transformed based on {@link ProfileMatchingRunnerResult}
	 * @param profileMatchinRunnerResult
	 */
	private void setSharedStateVariables(
			ProfileMatchingRunnerResult profileMatchinRunnerResult, DevicePrint devicePrint) {
			debug.message("Setting shared state variables");
			
			ProfileAcceptanceInput profileAcceptanceInput = new ProfileAcceptanceInput();
			profileAcceptanceInput.setMatchingModuleNotFailed(true);
			profileAcceptanceInput.setMatchingResult(profileMatchinRunnerResult.getMatchingRuleResult());
			profileAcceptanceInput.setSelectedUserProfile(profileMatchinRunnerResult.getSelectedUserProfile());
			profileAcceptanceInput.setCurrentDevicePrint(devicePrint);
			profileAcceptanceInput.setCommonConfig(configurationReader.createObjectUsingAttributes(ProfileAcceptanceCommonConfig.class));
			profileAcceptanceInput.setStoreCookie(storeCookie);
			
			sharedState.putAll(MapObjectTransformer.convertObjectToMap(profileAcceptanceInput));
			
			sharedState.put(SHARED_STATE_SKIP_HOTP, !profileMatchinRunnerResult.getMatchingRuleResult().isRequireHOTPConfirmation());
			
			if(debug.messageEnabled()) {
				Set<Entry<Object, Object>> entrySet = sharedState.entrySet();
				debug.message("List of set shared variables");
				for (Entry<Object, Object> entry : entrySet) {
					if(!entry.getKey().equals("javax.security.auth.login.password")) {
						debug.message("key: " + entry.getKey() + " val: " + entry.getValue());
					} else {
						debug.message("key: " + entry.getKey() + " val: ********");
					}
				}
			}
	}

	/**
	 * Obtain current device print
	 * @param servletRequest
	 * @return
	 */
	private DevicePrint getCurrentDevicePrint(HttpServletRequest servletRequest) {
		// get deviceprint info from request
		DevicePrint aggregateDevicePrint = devicePrintInfoAggregator
				.aggregateDevicePrint(servletRequest);
		
		if(debug.messageEnabled()) { debug.message("Current device print: " + aggregateDevicePrint); }
		
		return aggregateDevicePrint;
	}
	
	@Override
	protected String getBundleName() {
		return RESOURCE_BUNDLE_NAME;
	}


}
