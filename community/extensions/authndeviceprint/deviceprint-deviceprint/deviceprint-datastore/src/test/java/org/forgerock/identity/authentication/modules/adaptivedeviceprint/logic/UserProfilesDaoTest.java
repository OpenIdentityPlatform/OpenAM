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

import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import junit.framework.Assert;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.DevicePrint;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.EmptyUserProfileNameException;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.NotUniqueUserProfileException;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.UserProfile;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.IdRepoException;

public class UserProfilesDaoTest {
	
	public static final String ADAPTIVE_PROFILES = "adaptive-profiles";
	
	@Test
	public void init() {
		Scanner s = new Scanner(getClass().getResourceAsStream("/adaptive-profiles.json"));
		
		Set<String> vals = new HashSet<String>();
		vals.add(s.nextLine());
		
		AMIdentityWrapperIface amIdentity = mock(AMIdentityWrapperIface.class);
		try {
			when(amIdentity.getAttribute(ADAPTIVE_PROFILES)).thenReturn(vals);
		} catch (Exception e) {
		}
		
		UserProfilesDaoIface userProfilesDao = new UserProfilesDao();
		userProfilesDao.setAdaptiveUserProfileAttributeName(ADAPTIVE_PROFILES);
		userProfilesDao.setAMIdentityWrapper(amIdentity);
		
		userProfilesDao.init();
		
		List<UserProfile> profiles = userProfilesDao.getProfiles();
		
		Assert.assertEquals(1, profiles.size());
		UserProfile up = profiles.get(0);
		Assert.assertEquals("1", up.getUuid());
		
		DevicePrint dp = up.getDevicePrint();
		Assert.assertEquals("24", dp.getScreenColorDepth());
		Assert.assertEquals("7b674b8c-b2a0-48cd-8a7f-56b22be9486a", dp.getPersistentCookie());
	}
	
	@Test
	public void manageProfiles() {
		UserProfilesDaoIface dao = new UserProfilesDao();
		
		UserProfile up = new UserProfile();
		up.setUuid("1");
		
		dao.addProfile(up);
		
		Assert.assertTrue(dao.size() == 1);
		Assert.assertEquals(up, dao.getProfiles().get(0));
		
		dao.removeProfile("1");
		Assert.assertTrue(dao.size() == 0);
	}
	
	@SuppressWarnings("rawtypes")
	@Test
	public void saveProfiles() throws SSOException, IdRepoException, NotUniqueUserProfileException, EmptyUserProfileNameException {
		Scanner s = new Scanner(getClass().getResourceAsStream("/adaptive-profiles.json"));
		String profiles = s.nextLine();
		
		Set<String> vals = new HashSet<String>();
		vals.add(profiles);
		
		Map<String, Set> attrMap = new HashMap<String, Set>();
		attrMap.put(ADAPTIVE_PROFILES, vals);
		
		AMIdentityWrapperIface amIdentity = mock(AMIdentityWrapperIface.class);
		when(amIdentity.getAttribute(ADAPTIVE_PROFILES)).thenReturn(vals);
		
		UserProfilesDaoIface userProfilesDao = new UserProfilesDao();
		userProfilesDao.setAdaptiveUserProfileAttributeName(ADAPTIVE_PROFILES);
		userProfilesDao.setAMIdentityWrapper(amIdentity);
		
		userProfilesDao.init();
		
		userProfilesDao.saveProfiles();
		
		verify(amIdentity).setAttributes(attrMap);
	}
	
	@Test
	public void getUserProfileByName() {
		String name = "a";
		
		UserProfile up = new UserProfile();
		up.setName(name);
		
		UserProfilesDaoIface userProfilesDao = new UserProfilesDao();
		userProfilesDao.addProfile(up);
		
		Assert.assertEquals(name, userProfilesDao.getUserProfileByName(name).get(0).getName());
	}
	
	@Test(expectedExceptions=NotUniqueUserProfileException.class)
	public void validateUnique() throws NotUniqueUserProfileException, EmptyUserProfileNameException {		
		UserProfilesDaoIface userProfilesDao = new UserProfilesDao();
		
		UserProfile up1 = new UserProfile();
		up1.setName("a");
		up1.setUuid("1");
		
		UserProfile up2 = new UserProfile();
		up2.setName("a");
		up2.setUuid("2");
		
		userProfilesDao.addProfile(up1);
		userProfilesDao.addProfile(up2);
		
		userProfilesDao.validate();
	}
	
	@Test
	public void validate() throws NotUniqueUserProfileException, EmptyUserProfileNameException {
		UserProfile up1 = new UserProfile();
		up1.setName("a");
		up1.setUuid("1");
		
		UserProfilesDaoIface userProfilesDao = new UserProfilesDao();
		userProfilesDao.addProfile(up1);
		
		userProfilesDao.validate();
	}
	
	@Test(expectedExceptions=EmptyUserProfileNameException.class)
	public void validateEmpty() throws NotUniqueUserProfileException, EmptyUserProfileNameException {		
		UserProfilesDaoIface userProfilesDao = new UserProfilesDao();
		
		UserProfile up1 = new UserProfile();
		up1.setName("");
	
		userProfilesDao.addProfile(up1);
		
		userProfilesDao.validate();
	}
}
