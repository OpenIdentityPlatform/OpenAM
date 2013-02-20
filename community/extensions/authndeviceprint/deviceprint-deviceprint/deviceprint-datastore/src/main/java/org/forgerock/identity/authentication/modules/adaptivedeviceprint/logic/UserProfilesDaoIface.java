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

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.EmptyUserProfileNameException;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.NotUniqueUserProfileException;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.UserProfile;

/**
 * CRUD for adaptive profiles. It uses AMIdentity as data store.
 * 
 * @author mbilski
 * 
 */
public interface UserProfilesDaoIface {

	/**
	 * @param am AMIdentity instance with admin privileges of logged user
	 * @see com.sun.identity.idm.IdUtils#getIdentity(com.iplanet.sso.SSOToken, String)
	 * @see com.sun.identity.security.AdminTokenAction#getInstance()
	 */
	void setAMIdentityWrapper(AMIdentityWrapperIface am);

	/**
	 * @param adaptiveUserProfileAttributeName
	 *            name of the attribute in data store schema
	 */
	void setAdaptiveUserProfileAttributeName(String adaptiveUserProfileAttributeName);

	/**
	 * Run it after setting AMIdentityWrapperIface and
	 * adaptiveUserProfileAttributeName to load user profiles
	 */
	void init();
	
	/**
	 * @return the list of loaded profiles
	 */
	List<UserProfile> getProfiles();
	
	/**
	 * Add profile to the list. Need to run saveProfiles() to commit changes.
	 */
	void addProfile(UserProfile userProfile);
	
	/**
	 * Gets user profile by unique id;
	 */
	UserProfile getUserProfileByUuid(String name);
	
	/**
	 * Profile name might not be unique, so it returns a list.
	 */
	List<UserProfile> getUserProfileByName(String name);
	
	/**
	 * Remove profile from the list by uuid. Need to run saveProfiles() to commit changes.
	 * @param uuid
	 */
	void removeProfile(String uuid);
	
	/**
	 * @return size of profiles list
	 */
	int size();

	/**
	 * Saves profiles list in repository. Only this function makes real changes.
	 */
	void saveProfiles() throws NotUniqueUserProfileException, EmptyUserProfileNameException;
	
	/**
	 * Validates user profiles.
	 * @return
	 */
	void validate() throws NotUniqueUserProfileException, EmptyUserProfileNameException;
}
