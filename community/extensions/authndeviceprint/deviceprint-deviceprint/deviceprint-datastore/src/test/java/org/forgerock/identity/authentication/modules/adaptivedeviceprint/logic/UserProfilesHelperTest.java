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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.security.auth.login.LoginException;

import junit.framework.Assert;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.EmptyUserProfileNameException;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.NotUniqueUserProfileException;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.UserProfile;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UserProfilesHelperTest {

	private UserProfilesHelper helper;
	private UserProfilesDaoIface dao;
	private List<UserProfile> profiles;
	
	@BeforeMethod
	public void beforeMethod() {
		helper = new UserProfilesHelper();
		profiles = new ArrayList<UserProfile>();
		
		dao = mock(UserProfilesDaoIface.class);
		helper.setUserProfilesDao(dao);
		when(dao.getProfiles()).thenReturn(profiles);
	}
	
	@Test
	public void isUniqueName() {
		UserProfile up = new UserProfile();
		up.setName("a");
		profiles.add(up);
		
		Assert.assertTrue(helper.isUniqueName("b"));
		Assert.assertFalse(helper.isUniqueName("a"));
	}

	@Test
	public void removeOldProfiles() throws NotUniqueUserProfileException, EmptyUserProfileNameException {		
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, -20);
		
		profiles.add(new UserProfile(new Date(), c.getTime(), 0L));
		profiles.add(new UserProfile(new Date(), new Date(), 0L));
				
		helper.setProfileExpirationDays(10);		
		helper.removeExpiredProfiles();
		
		Assert.assertEquals(1, profiles.size());
	}
	
	@Test
	public void testGetNotExpiredProfiles() throws NotUniqueUserProfileException, EmptyUserProfileNameException {		
		Date now = Calendar.getInstance().getTime();
		Calendar lastUsedTwentyDaysBeforeCal = Calendar.getInstance();
		lastUsedTwentyDaysBeforeCal.add(Calendar.DAY_OF_MONTH, -20);
		
		profiles.add(new UserProfile(now, lastUsedTwentyDaysBeforeCal.getTime(), 0L));
		profiles.add(new UserProfile(now, now, 0L));
				
		helper.setProfileExpirationDays(10);		
		List<UserProfile> notExpiredProfiles = helper.getNotExpiredProfiles();
		
		Assert.assertEquals(1, notExpiredProfiles.size());
	}

	@Test
	public void saveProfile() throws LoginException, NotUniqueUserProfileException, EmptyUserProfileNameException {
		UserProfile up = new UserProfile();
		up.setUuid("a");
		profiles.add(up);
		
		helper.saveProfile(up);
		
		verify(dao, times(1)).removeProfile("a");
		verify(dao, times(1)).addProfile(up);
		verify(dao, times(1)).saveProfiles();
	}
	
	@Test
	public void removeOldestProfile() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_MONTH, -20);
		
		UserProfile u = new UserProfile(new Date(), new Date(), 0L);
		profiles.add(u);
		profiles.add(new UserProfile(new Date(), c.getTime(), 0L));
		
		Assert.assertEquals(2, profiles.size());
		
		helper.removeOldestProfile();
		
		Assert.assertEquals(1, profiles.size());
		Assert.assertEquals(u, profiles.get(0));
	}
	
	@Test
	public void profileMaximumProfilesStoredQuantity() throws LoginException, NotUniqueUserProfileException, EmptyUserProfileNameException {
		helper.setProfileMaximumProfilesStoredQuantity(2);		
		when(dao.size()).thenAnswer(new Answer<Integer>() {
			@Override
			public Integer answer(InvocationOnMock invocation)
					throws Throwable {
				return profiles.size();
			}
		});
		
		profiles.add(new UserProfile(new Date(), new Date(), 0L));	
		
		helper.saveProfile(new UserProfile(new Date(), new Date(), 0L));	
		profiles.add(new UserProfile(new Date(), new Date(), 0L));
		Assert.assertEquals(2, profiles.size());
		
		helper.saveProfile(new UserProfile(new Date(), new Date(), 0L));
		profiles.add(new UserProfile(new Date(), new Date(), 0L));
		Assert.assertEquals(2, profiles.size());
		
		profiles.add(new UserProfile(new Date(), new Date(), 0L));
		profiles.add(new UserProfile(new Date(), new Date(), 0L));
		profiles.add(new UserProfile(new Date(), new Date(), 0L));
		
		helper.saveProfile(new UserProfile(new Date(), new Date(), 0L));	
		profiles.add(new UserProfile(new Date(), new Date(), 0L));
		Assert.assertEquals(2, profiles.size());
		
	}
}
