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

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.EmptyUserProfileNameException;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.NotUniqueUserProfileException;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.UserProfile;
import org.forgerock.identity.authentication.modules.common.config.ReadableDateMapper;

import com.sun.identity.shared.debug.Debug;

public class UserProfilesDao implements UserProfilesDaoIface {

	private static Debug debug = Debug.getInstance(UserProfilesDao.class.getName());
	
	private String adaptiveUserProfileAttributeName;
	private AMIdentityWrapperIface amIdentity;
	private List<UserProfile> profiles = new ArrayList<UserProfile>();
	private ReadableDateMapper mapper = new ReadableDateMapper();
	
	/** {@inheritDoc} */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void init() {	
		profiles.clear();
		
		try {
			Set set = amIdentity.getAttribute(adaptiveUserProfileAttributeName);

			Iterator<String> i = set.iterator();

			while (i.hasNext()) {
				try {
					profiles.add(mapper.readValue(i.next(), UserProfile.class));
				} catch (Exception e) {
					debug.error("Cannot parse json. " + e);
				}
			}

			debug.message("Readed " + profiles.size() + " adaptive profiles.");
		} catch (Exception e) {
			debug.error("Cannot get AdaptiveUserProfiles attribute. " + e);
		}

	}
	
	/** {@inheritDoc} */
	@Override
	public List<UserProfile> getProfiles() {
		return profiles;
	}
	
	/** {@inheritDoc} */
	@Override
	public void addProfile(UserProfile userProfile) {
		profiles.add(userProfile);
	}
	
	/** {@inheritDoc} */
	@Override
	public void removeProfile(String uuid) {
		for (int i = 0; i < profiles.size(); i++) {
			if (profiles.get(i).getUuid().equals(uuid)) {
				profiles.remove(i);
				return;
			}
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public int size() {
		return profiles.size();
	}
	
	/** {@inheritDoc} */
	@SuppressWarnings("rawtypes")
	@Override
	public void saveProfiles() throws NotUniqueUserProfileException, EmptyUserProfileNameException {
		validate();
		
		Set<String> vals = new HashSet<String>();

		for (UserProfile userProfile : profiles) {
			Writer strWriter = new StringWriter();

			try {
				mapper.writeValue(strWriter, userProfile);
				vals.add(strWriter.toString());
			} catch (Exception e) {
				debug.error("Error while serializing profiles. " + e);
			}
		}

		Map<String, Set> attrMap = new HashMap<String, Set>();
		attrMap.put(adaptiveUserProfileAttributeName, vals);

		try {
			amIdentity.setAttributes(attrMap);
			amIdentity.store();

			debug.message("Profiles stored");
		} catch (Exception e) {
			debug.error("Could not store profiles. " + e);
		}
	}

	public AMIdentityWrapperIface getAmIdentity() {
		return amIdentity;
	}
	
	/** {@inheritDoc} */
	@Override
	public void setAMIdentityWrapper(AMIdentityWrapperIface am) {
		this.amIdentity = am;
	}

	public void setProfiles(List<UserProfile> profiles) {
		this.profiles = profiles;
	}

	public String getAdaptiveUserProfileAttributeName() {
		return adaptiveUserProfileAttributeName;
	}
	
	/** {@inheritDoc} */
	public void setAdaptiveUserProfileAttributeName(
			String adaptiveUserProfileAttributeName) {
		this.adaptiveUserProfileAttributeName = adaptiveUserProfileAttributeName;
	}
	
	/** {@inheritDoc} */
	@Override
	public UserProfile getUserProfileByUuid(String id) {
		for(UserProfile up : profiles) {
			if( up.getUuid().equals(id) ) {
				return up;
			}
		}
		
		return null;
	}
	
	/** {@inheritDoc} */
	@Override
	public List<UserProfile> getUserProfileByName(String name) {
		List<UserProfile> ret = new ArrayList<UserProfile>();
		
		for(UserProfile up : profiles) {
			if( up.getName().equals(name) ) {
				ret.add(up);
			}
		}
		
		return ret;
	}

	@Override
	public void validate() throws NotUniqueUserProfileException, EmptyUserProfileNameException {
		List<String> ids = new ArrayList<String>();
		List<String> names = new ArrayList<String>();
		
		for(UserProfile up : profiles) {
			//unique validator
			if( ids.contains(up.getUuid()) || names.contains(up.getName()) ) {
				throw new NotUniqueUserProfileException("UserProfiles are not unique");
			}
			
			//empty validator
			if( up.getName() == null || up.getName().equals("") ) {
				throw new EmptyUserProfileNameException("UserProfile name cannot be blank");
			}
			
			ids.add(up.getUuid());
			names.add(up.getName());
		}		
	}

}
