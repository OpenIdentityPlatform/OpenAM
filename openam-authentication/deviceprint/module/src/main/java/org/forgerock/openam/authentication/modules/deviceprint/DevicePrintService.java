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
 * Portions Copyrighted 2013 ForgeRock Inc.
 */

package org.forgerock.openam.authentication.modules.deviceprint;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.authentication.modules.deviceprint.comparators.ComparisonResult;
import org.forgerock.openam.authentication.modules.deviceprint.comparators.DevicePrintComparator;
import org.forgerock.openam.authentication.modules.deviceprint.exceptions.NotUniqueUserProfileException;
import org.forgerock.openam.authentication.modules.deviceprint.extractors.Extractor;
import org.forgerock.openam.authentication.modules.deviceprint.extractors.DevicePrintExtractorFactory;
import org.forgerock.openam.authentication.modules.deviceprint.model.DevicePrint;
import org.forgerock.openam.authentication.modules.deviceprint.model.UserProfile;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This class exposes services to parse Device Print information from the client, find matches against stored user
 * profiles, and update the user profiles in LDAP.
 *
 * @author mbilski
 * @author Phill Cunnington
 */
public class DevicePrintService {
	
	private final static Debug DEBUG = Debug.getInstance("amAuthDevicePrint");
	
	private final UserProfilesDao userProfilesDao;
    private final DevicePrintAuthenticationConfig devicePrintAuthenticationConfig;
    private final DevicePrintExtractorFactory extractorFactory;
    private final DevicePrintComparator devicePrintComparator;
	private final Integer profileExpirationDays;
	private final Integer profileMaximumProfilesStoredQuantity;

    /**
     * Constructs an instance of the DevicePrintService.
     *
     * @param devicePrintAuthenticationConfig An instance of the DevicePrintAuthenticationConfig.
     * @param userProfilesDao An instance of the UserProfilesDao.
     * @param extractorFactory An instance of the DevicePrintExtractorFactory.
     * @param devicePrintComparator An instance of the DevicePrintComparator.
     */
    public DevicePrintService(DevicePrintAuthenticationConfig devicePrintAuthenticationConfig,
            UserProfilesDao userProfilesDao, DevicePrintExtractorFactory extractorFactory,
            DevicePrintComparator devicePrintComparator) {

        this.devicePrintAuthenticationConfig = devicePrintAuthenticationConfig;
        this.userProfilesDao = userProfilesDao;
        this.extractorFactory = extractorFactory;
        this.devicePrintComparator = devicePrintComparator;
        profileExpirationDays = devicePrintAuthenticationConfig.getInt(
                DevicePrintAuthenticationConfig.PROFILE_EXPIRATION_DAYS);
        profileMaximumProfilesStoredQuantity = devicePrintAuthenticationConfig.getInt(
                DevicePrintAuthenticationConfig.MAX_STORED_PROFILES);
    }

    /**
     * Checks whether the Device Print information contains the required attributes set in the authentication module
     * settings.
     *
     * @param devicePrint The Device Print information.
     * @return Whether the Device Print information includeds the required attributes.
     */
    public boolean hasRequiredAttributes(DevicePrint devicePrint) {
        return devicePrintAuthenticationConfig.hasRequiredAttributes(devicePrint);
    }

    /**
     * Parses the Device Print information from the Http Request.
     *
     * @param request The Http Servlet Request.
     * @return The Device Print information.
     */
    public DevicePrint getDevicePrint(HttpServletRequest request) {
        DevicePrint devicePrint = new DevicePrint();
        for (Extractor extractor : extractorFactory.getExtractors()) {
            extractor.extractData(devicePrint, request);
        }
        return devicePrint;
    }

    /**
     * Uses the given Device Print information to find the best matching stored Device Print information from stored
     * User Profiles. It uses the penalty points set in the authentication module settings to determine whether a stored
     * Device print matches the given one.
     *
     * If no match is found null is returned.
     *
     * @param devicePrint The Device Print to find a match for.
     * @return The matching User Profile or null.
     */
    public UserProfile getBestMatchingUserProfile(DevicePrint devicePrint) {

        SortedMap<ComparisonResult, UserProfile> comparisonResultMap = new TreeMap<ComparisonResult, UserProfile>();

        for (UserProfile userProfile : getNotExpiredProfiles()) {
            DevicePrint storedDevicePrint = userProfile.getDevicePrint();
            comparisonResultMap.put(devicePrintComparator.compare(devicePrint, storedDevicePrint,
                    devicePrintAuthenticationConfig), userProfile);
        }

        if (comparisonResultMap.isEmpty()) {
            return null;
        }

        ComparisonResult selectedComparisonResult = comparisonResultMap.firstKey();

        UserProfile selectedProfile = null;
        if (selectedComparisonResult.getPenaltyPoints() <= devicePrintAuthenticationConfig.getLong(
                DevicePrintAuthenticationConfig.MAX_TOLERATED_PENALTY_POINTS)) {
            selectedProfile = comparisonResultMap.get(selectedComparisonResult);
        }

        return selectedProfile;
    }

    /**
     * Creates a new User Profile with the given Device Print information in LDAP.
     *
     * @param devicePrint The Device Print to store.
     * @throws NotUniqueUserProfileException If the Device print information's id is not unique.
     */
    public void createNewProfile(DevicePrint devicePrint) throws NotUniqueUserProfileException {

        UserProfile profile = new UserProfile(new Date(), new Date(), 1L);
        profile.setDevicePrint(devicePrint);
        profile.setUuid(new RandomHashGenerator().getRandomHash());

        saveProfile(profile);
    }

    /**
     * Updates the given User Profile, with the given Device print information and increments the selection counter
     * and resets the last selected date.
     *
     * @param profile The User profile to update.
     * @param devicePrint The Device print information to update.
     * @throws NotUniqueUserProfileException If the Device print information's id is not unique.
     */
    public void updateProfile(UserProfile profile, DevicePrint devicePrint) throws NotUniqueUserProfileException {

        profile.setSelectionCounter(profile.getSelectionCounter() + 1L);
        profile.setLastSelectedDate(new Date());
        profile.setDevicePrint(devicePrint);

        saveProfile(profile);
    }

    /**
     * Saves the given User Profile in LDAP.
     *
     * Checks to ensure the max number of user profiles is not exceeded. If it is as many as needed are removed to
     * ensure the max is not exceeded.
     *
     * @param userProfile The User Profile to save.
     * @throws NotUniqueUserProfileException If the Device print information's id is not unique.
     */
	private void saveProfile(UserProfile userProfile) throws NotUniqueUserProfileException {

        if (userProfile != null) {
			if (userProfile.getUuid() != null && !userProfile.getUuid().equals("")) {
				// updating profile
				userProfilesDao.removeProfile(userProfile.getUuid());
			} 
			
			//creating new profile, so need to check if
			//maximum stored profiles had been reached			
			if (profileMaximumProfilesStoredQuantity != null) {
				while (userProfilesDao.getProfiles().size() >= profileMaximumProfilesStoredQuantity) {
					DEBUG.message("Removing oldest user profile due to maximum profiles stored quantity");
					removeOldestProfile();						
				}
			}

			userProfilesDao.addProfile(userProfile);
			userProfilesDao.saveProfiles();
		}
	}

    /**
     * Finds the oldest User's profile and removes it from LDAP.
     */
	private void removeOldestProfile() {
		UserProfile oldestProfile = null;
		Date oldestDate = new Date();
		
		for (UserProfile userProfile : userProfilesDao.getProfiles()) {
			if (userProfile.getLastSelectedDate().before(oldestDate)) {
				oldestDate = userProfile.getLastSelectedDate();
				oldestProfile = userProfile;
			}
		}
		
		if (oldestProfile != null) {
			userProfilesDao.getProfiles().remove(oldestProfile);
		}
	}

    /**
     * Gets the list of valid, non-expired User's profiles.
     *
     * @return The valid User profiles.
     */
	private List<UserProfile> getNotExpiredProfiles() {
		List<UserProfile> profiles = new ArrayList<UserProfile>();		
			
		for (UserProfile userProfile : userProfilesDao.getProfiles()) {
			if (!isExpiredProfile(userProfile)) {
				profiles.add(userProfile);
			}
		}
		
		return profiles;
	}

    /**
     * Determines whether a User's profile has expired due to it not being accessed within the profile expiration
     * authentication module setting.
     *
     * @param userProfile The User profile to check if has expired.
     * @return If the user profile has expired or not.
     */
	private boolean isExpiredProfile(UserProfile userProfile) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, -profileExpirationDays);

		if (userProfile.getLastSelectedDate().before(c.getTime())) {
			return true;
		}
		
		return false;
	}
}
