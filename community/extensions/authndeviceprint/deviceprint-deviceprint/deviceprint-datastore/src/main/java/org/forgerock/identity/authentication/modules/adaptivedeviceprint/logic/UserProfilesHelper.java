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
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.security.auth.login.LoginException;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.EmptyUserProfileNameException;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.NotUniqueUserProfileException;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.UserProfile;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.debug.Debug;

/**
 * Helper for managing user profiles.
 * 
 * @author mbilski
 * 
 */
public class UserProfilesHelper implements UserProfilesHelperIface {
	
	private final static Debug debug = Debug.getInstance(UserProfilesHelper.class.getName());
	
	private UserProfilesDaoIface userProfilesDao = new UserProfilesDao();

	private Integer profileExpirationDays;

	private Integer profileMaximumProfilesStoredQuantity;

	/** {@inheritDoc} */
	public void init(String adaptiveProfilesAttributeName, AMIdentity identity)
			throws LoginException {
		// get stored profiles info
		userProfilesDao.setAdaptiveUserProfileAttributeName(adaptiveProfilesAttributeName);
		userProfilesDao.setAMIdentityWrapper(new AMIdentityWrapper(identity));

		userProfilesDao.init();
	}

	/** {@inheritDoc} */
	public void saveProfile(UserProfile userProfile) throws LoginException,
			NotUniqueUserProfileException, EmptyUserProfileNameException {
		if (userProfile != null) {			
			if (userProfile.getUuid() != null
					&& !userProfile.getUuid().equals("")) {
				// updating profile
				userProfilesDao.removeProfile(userProfile.getUuid());
			} 
			
			//creating new profile, so need to check if
			//maximum stored profiles had been reached			
			if( profileMaximumProfilesStoredQuantity != null ) {
				while(userProfilesDao.size() >= profileMaximumProfilesStoredQuantity ) {
					debug.message("Removing oldest user profile due to maximum profiles stored quantity");
					removeOldestProfile();						
				}
			}

			userProfilesDao.addProfile(userProfile);
			userProfilesDao.saveProfiles();
		}
	}

	@Override
	public boolean isUniqueName(String name) {
		for (UserProfile up : userProfilesDao.getProfiles()) {
			if (up.getName().equals(name)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public void setProfileExpirationDays(Integer profileExpirationDays) {
		this.profileExpirationDays = profileExpirationDays;
	}

	/** {@inheritDoc} 
	 * @throws EmptyUserProfileNameException 
	 * @throws NotUniqueUserProfileException */
	@Override
	public void removeExpiredProfiles() throws NotUniqueUserProfileException, EmptyUserProfileNameException {
		if(profileExpirationDays != null) {			
			for(int i = 0; i < userProfilesDao.getProfiles().size(); i++) {
				UserProfile up = userProfilesDao.getProfiles().get(i);
				
				if(isExpiredProfile(up)) {
					debug.message("Removing expired user profile");
					userProfilesDao.getProfiles().remove(up);
					i--;
				}
			}
		}
		
		userProfilesDao.saveProfiles();
	}

	@Override
	public void setProfileMaximumProfilesStoredQuantity(
			Integer profileMaximumProfilesStoredQuantity) {
		this.profileMaximumProfilesStoredQuantity = profileMaximumProfilesStoredQuantity;
	}

	public Integer getProfileExpirationDays() {
		return profileExpirationDays;
	}

	public Integer getProfileMaximumProfilesStoredQuantity() {
		return profileMaximumProfilesStoredQuantity;
	}

	public UserProfilesDaoIface getUserProfilesDao() {
		return userProfilesDao;
	}

	public void setUserProfilesDao(UserProfilesDaoIface userProfilesDao) {
		this.userProfilesDao = userProfilesDao;
	}

	@Override
	public void removeOldestProfile() {
		UserProfile oldest = null;
		Date oldestDate = new Date();
		
		for(UserProfile up : userProfilesDao.getProfiles()) {
			if(up.getLastSelectedDate().before(oldestDate)) {
				oldestDate = up.getLastSelectedDate();
				oldest = up;
			}
		}
		
		if( oldest != null ) {
			userProfilesDao.getProfiles().remove(oldest);
		}
	}

	@Override
	public List<UserProfile> getNotExpiredProfiles() {
		List<UserProfile> profiles = new ArrayList<UserProfile>();		
			
		for(UserProfile up : userProfilesDao.getProfiles()) {
			if(!isExpiredProfile(up)) {
				profiles.add(up);
			}
		}
		
		return profiles;
	}

	@Override
	public boolean isExpiredProfile(UserProfile up) {		
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, -profileExpirationDays);

		if(up.getLastSelectedDate().before(c.getTime())) {
			return true;
		}
		
		return false;
	}

}
