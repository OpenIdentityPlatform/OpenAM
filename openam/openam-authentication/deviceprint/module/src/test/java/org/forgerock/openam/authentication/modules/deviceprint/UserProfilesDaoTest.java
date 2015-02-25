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

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.IdRepoException;
import org.forgerock.openam.authentication.modules.deviceprint.exceptions.NotUniqueUserProfileException;
import org.forgerock.openam.authentication.modules.deviceprint.model.DevicePrint;
import org.forgerock.openam.authentication.modules.deviceprint.model.UserProfile;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

public class UserProfilesDaoTest {

    private UserProfilesDao userProfilesDao;

    private AMIdentityWrapper amIdentityWrapper;

    private static final String USER_PROFILE_ONE = "{\"uuid\":\"UUID1\",\"lastSelectedDate\":\"2013-05-15T16:11:00.825+0000\",\"selectionCounter\":1,\"devicePrint\":{\"screenColourDepth\":\"SCREEN_COLOUR_DEPTH\",\"screenHeight\":\"SCREEN_HEIGHT\",\"screenWidth\":\"SCREEN_WIDTH\",\"installedPlugins\":\"INSTALLED_PLUGINS\",\"installedFonts\":\"INSTALLED_FONTS\",\"timezone\":\"TIMEZONE\",\"longitude\":2.0,\"latitude\":3.0,\"userAgent\":\"USER_AGENT\"}}";
    private static final String USER_PROFILE_TWO = "{\"uuid\":\"UUID2\",\"lastSelectedDate\":\"2013-05-15T16:11:00.825+0000\",\"selectionCounter\":1,\"devicePrint\":{\"screenColourDepth\":\"SCREEN_COLOUR_DEPTH\",\"screenHeight\":\"SCREEN_HEIGHT\",\"screenWidth\":\"SCREEN_WIDTH\",\"installedPlugins\":\"INSTALLED_PLUGINS\",\"installedFonts\":\"INSTALLED_FONTS\",\"timezone\":\"TIMEZONE\",\"longitude\":2.0,\"latitude\":3.0,\"userAgent\":\"USER_AGENT\"}}";
    private static final String USER_PROFILE_THREE = "{\"uuid\":\"UUID3\",\"lastSelectedDate\":\"2013-05-15T16:11:00.825+0000\",\"selectionCounter\":1,\"devicePrint\":{\"screenColourDepth\":\"SCREEN_COLOUR_DEPTH\",\"screenHeight\":\"SCREEN_HEIGHT\",\"screenWidth\":\"SCREEN_WIDTH\",\"installedPlugins\":\"INSTALLED_PLUGINS\",\"installedFonts\":\"INSTALLED_FONTS\",\"timezone\":\"TIMEZONE\",\"longitude\":2.0,\"latitude\":3.0,\"userAgent\":\"USER_AGENT\"}}";

    @BeforeMethod
    public void setUpMethod() {
        amIdentityWrapper = mock(AMIdentityWrapper.class);

        userProfilesDao = new UserProfilesDao(amIdentityWrapper);
    }

    @Test
    public void shouldInit() throws IdRepoException, SSOException {

        //Given
        Set<String> userProfiles = new HashSet<String>();

        userProfiles.add(USER_PROFILE_ONE);
        userProfiles.add(USER_PROFILE_TWO);
        userProfiles.add(USER_PROFILE_THREE);

        given(amIdentityWrapper.getAttribute("devicePrintProfiles")).willReturn((Set) userProfiles);

        //When
        userProfilesDao.init();

        //Then
        assertEquals(userProfilesDao.getProfiles().size(), 3);
    }

    @Test
    public void shouldRemoveProfile() throws IdRepoException, SSOException {

        //Given
        userProfilesDao.addProfile(createUserProfile("1"));
        userProfilesDao.addProfile(createUserProfile("2"));
        userProfilesDao.addProfile(createUserProfile("3"));

        //When
        userProfilesDao.removeProfile("2");

        //Then
        assertEquals(userProfilesDao.getProfiles().size(), 2);
    }

    @Test
    public void shouldNotRemoveProfileIfDoesNotExist() throws IdRepoException, SSOException {

        //Given
        userProfilesDao.addProfile(createUserProfile("1"));
        userProfilesDao.addProfile(createUserProfile("2"));
        userProfilesDao.addProfile(createUserProfile("3"));

        //When
        userProfilesDao.removeProfile("4");

        //Then
        assertEquals(userProfilesDao.getProfiles().size(), 3);
    }

    @Test
    public void shouldSaveProfiles() throws NotUniqueUserProfileException,
            IdRepoException, SSOException {

        //Given
        userProfilesDao.addProfile(createUserProfile("1"));
        userProfilesDao.addProfile(createUserProfile("2"));
        userProfilesDao.addProfile(createUserProfile("3"));

        //When
        userProfilesDao.saveProfiles();

        //Then
        ArgumentCaptor<Map> attrMapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(amIdentityWrapper).setAttributes(attrMapCaptor.capture());
        Map<String, Set> attrMap = attrMapCaptor.getValue();
        assertNotNull(attrMap.get("devicePrintProfiles"));
        assertEquals(attrMap.get("devicePrintProfiles").size(), 3);
        verify(amIdentityWrapper).store();
    }

    @Test (expectedExceptions = NotUniqueUserProfileException.class)
    public void shouldNotSaveProfilesWithSameUUID() throws NotUniqueUserProfileException,
            IdRepoException, SSOException {

        //Given
        userProfilesDao.addProfile(createUserProfile("1"));
        userProfilesDao.addProfile(createUserProfile("1"));

        //When
        userProfilesDao.saveProfiles();

        //Then
        fail();
    }

    private Date getDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2013, 4, 15, 17, 11, 0);
        return calendar.getTime();
    }

    private UserProfile createUserProfile(String name) {
        return createUserProfile(name, name);
    }

    private UserProfile createUserProfile(String uuid, String name) {

        UserProfile userProfile = new UserProfile(getDate(), getDate(), 1L);
        DevicePrint devicePrint = new DevicePrint();
        devicePrint.setScreenColourDepth("SCREEN_COLOUR_DEPTH");
        devicePrint.setScreenHeight("SCREEN_HEIGHT");
        devicePrint.setScreenWidth("SCREEN_WIDTH");
        devicePrint.setInstalledPlugins("INSTALLED_PLUGINS");
        devicePrint.setInstalledFonts("INSTALLED_FONTS");
        devicePrint.setTimezone("TIMEZONE");
        devicePrint.setLongitude(2.0);
        devicePrint.setLatitude(3.0);
        devicePrint.setUserAgent("USER_AGENT");
        userProfile.setDevicePrint(devicePrint);
        userProfile.setUuid(uuid);

        return userProfile;
    }
}
