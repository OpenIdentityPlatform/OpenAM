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

import java.util.Date;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.configuration.ProfileAcceptanceCommonConfig;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.configuration.ProfileAcceptanceInput;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.UserProfilesHelper;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.UserProfilesHelperIface;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.DevicePrint;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.EmptyUserProfileNameException;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.NotUniqueUserProfileException;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.UserProfile;
import org.forgerock.identity.authentication.modules.common.config.MapObjectTransformer;
import org.forgerock.identity.authentication.modules.utils.RandomHashGenerator;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.debug.Debug;

public class ProfileAcceptanceLogic implements ProfileAcceptanceLogicIface {

	private static final Debug debug = Debug
			.getInstance(ProfileAcceptanceLogic.class.getName());

	private AMIdentity amIdentity;

	private ProfileAcceptanceInput inputParams;

	private UserProfilesHelperIface userProfileHelper;

	private ProfileAcceptanceCommonConfig config;
	
	private String newPersistentCookieValue;

	/** {@inheritDoc} */
	@SuppressWarnings("rawtypes")
	@Override
	public void init(Map sharedState) throws LoginException {
		readSharedStateVariables(sharedState);

		if (userProfileHelper == null) {
			userProfileHelper = new UserProfilesHelper();
		}

		if (config != null) {
			userProfileHelper.init(config.getAdaptiveProfilesFieldName(), amIdentity);

			userProfileHelper.setProfileExpirationDays(config
					.getProfileExpirationDays());
			userProfileHelper.setProfileMaximumProfilesStoredQuantity(config
					.getProfileMaximumProfilesStoredQuantity());
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws EmptyUserProfileNameException
	 * @throws NotUniqueUserProfileException
	 */
	public boolean proccedInit() throws LoginException,
			NotUniqueUserProfileException, EmptyUserProfileNameException {
		// if failed, skip
		if (!inputParams.getMatchingModuleNotFailed()) {
			debug.message("DevicePrintModule failed. Skipping.");
			return true;
		}

		// every time
		userProfileHelper.removeExpiredProfiles();

		// if true creates profile and save add it to profile set. Skip next
		// steps.
		if (inputParams.getMatchingResult()
				.isCreateCurrentDevicePrintProfileWithoutPriorConfirmation()) {
			debug.message("Saving new user profile without confirmation");
			
			createNewProfile(inputParams.getCurrentDevicePrint(), getDefaultName());

			return true;
		}

		// if true don't ask and just update selected profile with current
		// device print
		if (inputParams.getMatchingResult()
				.isUpdateSelectedProfileWithoutPriorConfirmation()) {
			debug.message("Updating user profile without confirmation");

			updateProfile(inputParams.getSelectedUserProfile(),
					inputParams.getCurrentDevicePrint());

			return true;
		}

		if (!inputParams.getMatchingResult()
				.isUpdateSelectedProfileWithPriorConfirmation()
				&& inputParams.getSelectedUserProfile() != null) {
			debug.message("Updating only user profile.");

			updateProfile(inputParams.getSelectedUserProfile());

			return true;
		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws EmptyUserProfileNameException
	 * @throws NotUniqueUserProfileException
	 */
	public void proccedStoring(String profileName, boolean storeProfile)
			throws LoginException, NotUniqueUserProfileException,
			EmptyUserProfileNameException {
		debug.message("Store? " + storeProfile);

		if (!storeProfile) {
			return;
		}

		if (inputParams.getMatchingResult()
				.isCreateCurrentDevicePrintProfileWithPriorConfirmation() || inputParams.getMatchingResult()
				.isCreateCurrentDevicePrintProfileWithoutPriorConfirmation()) {
			debug.message("Saving new user profile");

			createNewProfile(inputParams.getCurrentDevicePrint(), profileName);
		} else if (inputParams.getMatchingResult()
				.isUpdateSelectedProfileWithPriorConfirmation() || inputParams.getMatchingResult()
				.isUpdateSelectedProfileWithoutPriorConfirmation()) {
			debug.message("Updating profile with or without confirmation");

			inputParams.getSelectedUserProfile().setName(profileName);
			updateProfile(inputParams.getSelectedUserProfile(),
					inputParams.getCurrentDevicePrint());
		} else {
			//in which cases this can occur ? cookie refresh, anything else ?
			if(inputParams.getSelectedUserProfile() == null) {
				prepareResponseCookie(inputParams.getSelectedUserProfile());
			} else {
				debug.warning("Cookie should be refreshed but the selected profile was null");
			}
		}

		return;
	}

	/**
	 * Creates new profile with given DevicePrint.
	 * 
	 * @throws EmptyUserProfileNameException
	 * @throws NotUniqueUserProfileException
	 */
	private void createNewProfile(DevicePrint dp, String profileName)
			throws LoginException, NotUniqueUserProfileException,
			EmptyUserProfileNameException {
		UserProfile profile = new UserProfile(new Date(), new Date(), 1L);

		profile.setDevicePrint(dp);
		profile.setUuid(new RandomHashGenerator().getRandomHash());
		profile.setName(profileName);

		prepareResponseCookie(profile);
		userProfileHelper.saveProfile(profile);
	}

	/**
	 * Updates devicePrint, lastSelectedDate and selectionCounter.
	 * 
	 * @throws EmptyUserProfileNameException
	 * @throws NotUniqueUserProfileException
	 */
	private void updateProfile(UserProfile profile, DevicePrint dp)
			throws LoginException, NotUniqueUserProfileException,
			EmptyUserProfileNameException {
		profile.setSelectionCounter(profile.getSelectionCounter() + 1L);
		profile.setLastSelectedDate(new Date());
		profile.setDevicePrint(dp);

		prepareResponseCookie(profile);
		userProfileHelper.saveProfile(profile);
	}

	/**
	 * Updates only lastSelectedDate and selectionCounter.
	 * 
	 * @throws EmptyUserProfileNameException
	 * @throws NotUniqueUserProfileException
	 */
	private void updateProfile(UserProfile profile) throws LoginException,
			NotUniqueUserProfileException, EmptyUserProfileNameException {
		profile.setSelectionCounter(profile.getSelectionCounter() + 1L);
		profile.setLastSelectedDate(new Date());

		prepareResponseCookie(profile);
		userProfileHelper.saveProfile(profile);
	}

	/** {@inheritDoc} */
	public void prepareResponseCookie(UserProfile profile) {
		if (inputParams.getStoreCookie()) {
			newPersistentCookieValue = profile.getDevicePrint().getPersistentCookie();
			profile.setLastPersistentCookieUpdateDate(new Date());
		}
	}

	/**
	 * Load shared state variables.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void readSharedStateVariables(Map sharedState) {
		inputParams = new MapObjectTransformer(sharedState)
				.createObjectUsingAttributes(ProfileAcceptanceInput.class);
		if (inputParams != null) {
			config = inputParams.getCommonConfig();
		}
	}

	public UserProfilesHelperIface getUserProfileSaver() {
		return userProfileHelper;
	}

	/** {@inheritDoc} */
	public void setUserProfileSaver(UserProfilesHelperIface userProfileSaver) {
		this.userProfileHelper = userProfileSaver;
	}

	public AMIdentity getAMIdentity() {
		return amIdentity;
	}

	public void setAMIdentity(AMIdentity identity) {
		this.amIdentity = identity;
	}

	/** {@inheritDoc} */
	@Override
	public String getDefaultName() {
		if (inputParams.getSelectedUserProfile() != null) {
			return inputParams.getSelectedUserProfile().getName();
		}

		if (userProfileHelper.isUniqueName(config.getDefaultProfileName())) {
			return config.getDefaultProfileName();
		}

		for (int i = 1; i < config.getProfileMaximumProfilesStoredQuantity(); i++) {
			String name = config.getDefaultProfileName() + Integer.toString(i);

			if (userProfileHelper.isUniqueName(name)) {
				return name;
			}
		}

		return new RandomHashGenerator().getRandomHash();
	}

	@Override
	public ProfileAcceptanceInput getInputParams() {
		return inputParams;
	}

	@Override
	public void setInputParams(ProfileAcceptanceInput inputParams) {
		this.inputParams = inputParams;
	}

	public ProfileAcceptanceCommonConfig getConfig() {
		return config;
	}

	public void setConfig(ProfileAcceptanceCommonConfig config) {
		this.config = config;
	}

	public String getNewPersistentCookieValue() {
		return newPersistentCookieValue;
	}

}
