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

import java.util.Map;

import javax.security.auth.login.LoginException;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.configuration.ProfileAcceptanceInput;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.UserProfilesHelperIface;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.EmptyUserProfileNameException;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.NotUniqueUserProfileException;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.UserProfile;

import com.sun.identity.idm.AMIdentity;

public interface ProfileAcceptanceLogicIface {

	/**
	 * Reads configuration from sharedState. It should be preceded with
	 * setUserName and setHttpServletResponse.
	 */
	@SuppressWarnings("rawtypes")
	void init(Map sharedState) throws LoginException;

	/**
	 * Based on sharedState configuration it consider if module should be
	 * skipped. It also execute proper actions.
	 * 
	 * It returns true when: 
	 * - profile can be created without confirmation.  
	 * - profile can be updated (with new DevicePrint) without confirmation.
	 * - profile do not have to be updated with confirmation and profile is selected
	 * 
	 * According to configuration profile is simultaneously created or updated.
	 * 
	 * @throws LoginException
	 */
	boolean proccedInit() throws LoginException, NotUniqueUserProfileException, EmptyUserProfileNameException;
	
	/**
	 * Based on sharedState configuration profile is created, updated or just prepare response. 
	 * This action required asking user for confirmation.
	 * 
	 * @param storeProfile user response if profile should be stored
	 * @param profileName human readable name of profile
	 * @throws LoginException
	 */
	void proccedStoring(String profileName, boolean storeProfile) throws LoginException, NotUniqueUserProfileException, EmptyUserProfileNameException;
	
	/**
	 * User login name needed for UserProfileSaver
	 * @param userName
	 */
	void setAMIdentity(AMIdentity identity);
	
	/**
	 * By default it is initialized in init method if userName has been set.
	 */
	void setUserProfileSaver(UserProfilesHelperIface userProfileSaver);
	
	ProfileAcceptanceInput getInputParams();

	void setInputParams(ProfileAcceptanceInput inputParams);
	
	/**
	 * It returns default and always unique UserProfile name if new profile is creating.
	 * 
	 * Otherwise, if existing profile is updating, it returns its name.
	 */
	String getDefaultName();
	
	/**
	 * Creates new persistence cookie and stores it in profile and http header.
	 */
	void prepareResponseCookie(UserProfile profile);
	
	public String getNewPersistentCookieValue();
}
