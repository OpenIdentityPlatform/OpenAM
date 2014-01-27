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
 * Portions Copyrighted 2013-2014 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.deviceprint;

import com.sun.identity.shared.debug.Debug;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.forgerock.openam.authentication.modules.deviceprint.exceptions.NotUniqueUserProfileException;
import org.forgerock.openam.authentication.modules.deviceprint.model.UserProfile;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DAO class for CRUDL operations on UserProfiles in LDAP.
 */
public class UserProfilesDao {

	private static final Debug DEBUG = Debug.getInstance("amAuthDevicePrint");
	
	private static final String LDAP_DEVICE_PRINT_ATTRIBUTE_NAME = "devicePrintProfiles";

	private final AMIdentityWrapper amIdentity;
	private final List<UserProfile> profiles = new ArrayList<UserProfile>();
	private static final ObjectMapper mapper = new ObjectMapper().configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);

    /**
     * Constructs an instance of the UserProfilesDao.
     *
     * @param amIdentityWrapper An instance of the AMIdentityWrapper.
     */
    public UserProfilesDao(AMIdentityWrapper amIdentityWrapper) {
        this.amIdentity = amIdentityWrapper;
        init();
    }

    /**
     * Initialises the DAO's internal user profiles cache from LDAP.
     */
	public void init() {
		profiles.clear();
		
		try {
			Set<String> set = (Set<String>) amIdentity.getAttribute(LDAP_DEVICE_PRINT_ATTRIBUTE_NAME);

			Iterator<String> i = set.iterator();

			while (i.hasNext()) {
				try {
					profiles.add(mapper.readValue(i.next(), UserProfile.class));
				} catch (Exception e) {
					DEBUG.error("Cannot parse json. " + e);
				}
			}

			DEBUG.message("Read " + profiles.size() + " adaptive profiles.");
		} catch (Exception e) {
			DEBUG.error("Cannot get AdaptiveUserProfiles attribute. " + e);
		}

	}

    /**
     * Returns the DAO's internal cache of user profiles.
     *
     * @return The user's profiles.
     */
	public List<UserProfile> getProfiles() {
		return profiles;
	}

    /**
     * Adds a user profile to the DAO's internal cache.
     *
     * @param userProfile The new user's profile.
     */
	public void addProfile(UserProfile userProfile) {
		profiles.add(userProfile);
	}

    /**
     * Removes a specific user profile from the DAO's internal cache.
     *
     * @param uuid The id of the user's profile.
     */
	public void removeProfile(String uuid) {
		for (int i = 0; i < profiles.size(); i++) {
			if (profiles.get(i).getUuid().equals(uuid)) {
				profiles.remove(i);
				return;
			}
		}
	}

    /**
     * Saves the DAO's internal user profiles cache to LDAP.
     *
     * @throws NotUniqueUserProfileException If two or more user profiles have the same uuid or name.
     */
	public void saveProfiles() throws NotUniqueUserProfileException {
		validate();
		
		Set<String> vals = new HashSet<String>();

		for (UserProfile userProfile : profiles) {
			Writer strWriter = new StringWriter();

			try {
				mapper.writeValue(strWriter, userProfile);
				vals.add(strWriter.toString());
			} catch (Exception e) {
				DEBUG.error("Error while serializing profiles. " + e);
			}
		}

		Map<String, Set> attrMap = new HashMap<String, Set>();
		attrMap.put(LDAP_DEVICE_PRINT_ATTRIBUTE_NAME, vals);

		try {
			amIdentity.setAttributes(attrMap);
			amIdentity.store();

			DEBUG.message("Profiles stored");
		} catch (Exception e) {
			DEBUG.error("Could not store profiles. " + e);
		}
	}

    /**
     * Validates that the user profile's ids are unique.
     *
     * @throws NotUniqueUserProfileException If two or more user profiles have the same uuid or name.
     */
	private void validate() throws NotUniqueUserProfileException {
		List<String> ids = new ArrayList<String>();

		for (UserProfile userProfile : profiles) {
			if (ids.contains(userProfile.getUuid())) {
				throw new NotUniqueUserProfileException("UserProfiles are not unique");
			}
			
			ids.add(userProfile.getUuid());
		}
	}
}
