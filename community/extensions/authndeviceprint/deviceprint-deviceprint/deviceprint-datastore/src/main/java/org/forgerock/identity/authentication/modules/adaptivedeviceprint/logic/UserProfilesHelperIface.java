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

import java.util.List;

import javax.security.auth.login.LoginException;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.EmptyUserProfileNameException;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.NotUniqueUserProfileException;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.UserProfile;

import com.sun.identity.idm.AMIdentity;

/**
 * User profiles managing logic.
 * @author mbilski
 *
 */
public interface UserProfilesHelperIface {

	/**
	 * Return true if there is no such profile name in already stored profiles.
	 */
	boolean isUniqueName(String name);
	
	boolean isExpiredProfile(UserProfile up);
	
	/**
	 * Initialize connection with datastore.
	 * @param adaptiveProfilesAttributeName datastore attribute name
	 * @param identity
	 * @throws LoginException
	 */
	void init(String adaptiveProfilesAttributeName, AMIdentity identity)
			throws LoginException;
	
	void setProfileExpirationDays(Integer profileExpirationDays);
	
	void setProfileMaximumProfilesStoredQuantity(Integer profileMaximumProfilesStoredQuantity);
		
	/**
	 * Removes profiles which are old then profileExpirationDays
	 */
	void removeExpiredProfiles() throws NotUniqueUserProfileException, EmptyUserProfileNameException;
	
	/**
	 * Remove oldest profile. It is used when maximumProfilesStoredQuantity is reached.
	 */
	void removeOldestProfile();
	
	List<UserProfile> getNotExpiredProfiles();
	
	/**
	 * Saves new profile or updates existing. If too many profiles are stored,
	 * the oldest one is removed.
	 * 
	 * @throws LoginException
	 */
	void saveProfile(UserProfile userProfile) throws LoginException,
			NotUniqueUserProfileException, EmptyUserProfileNameException;

}
