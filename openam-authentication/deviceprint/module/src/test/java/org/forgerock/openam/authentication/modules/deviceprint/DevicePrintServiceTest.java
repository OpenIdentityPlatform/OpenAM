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

import org.forgerock.openam.authentication.modules.deviceprint.comparators.ComparisonResult;
import org.forgerock.openam.authentication.modules.deviceprint.comparators.DevicePrintComparator;
import org.forgerock.openam.authentication.modules.deviceprint.exceptions.NotUniqueUserProfileException;
import org.forgerock.openam.authentication.modules.deviceprint.extractors.DevicePrintExtractorFactory;
import org.forgerock.openam.authentication.modules.deviceprint.extractors.Extractor;
import org.forgerock.openam.authentication.modules.deviceprint.model.DevicePrint;
import org.forgerock.openam.authentication.modules.deviceprint.model.UserProfile;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

public class DevicePrintServiceTest {

    private DevicePrintService devicePrintService;

    private DevicePrintAuthenticationConfig devicePrintAuthenticationConfig;
    private UserProfilesDao userProfilesDao;
    private DevicePrintExtractorFactory extractorFactory;
    private DevicePrintComparator devicePrintComparator;

    @BeforeMethod
    public void setUpMethod() {

        devicePrintAuthenticationConfig = mock(DevicePrintAuthenticationConfig.class);
        userProfilesDao = mock(UserProfilesDao.class);
        extractorFactory = mock(DevicePrintExtractorFactory.class);
        devicePrintComparator = mock(DevicePrintComparator.class);

        given(devicePrintAuthenticationConfig.getInt(
                DevicePrintAuthenticationConfig.PROFILE_EXPIRATION_DAYS)).willReturn(30);
        given(devicePrintAuthenticationConfig.getInt(
                DevicePrintAuthenticationConfig.MAX_STORED_PROFILES)).willReturn(2);

        devicePrintService = new DevicePrintService(devicePrintAuthenticationConfig, userProfilesDao, extractorFactory,
                devicePrintComparator);
    }

    @Test
    public void shouldCheckHasRequiredAttributes() {

        //Given
        DevicePrint devicePrint = mock(DevicePrint.class);

        //When
        devicePrintService.hasRequiredAttributes(devicePrint);

        //Then
        verify(devicePrintAuthenticationConfig).hasRequiredAttributes(devicePrint);
    }

    @Test
    public void shouldGetCurrentDevicePrint() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        Set<Extractor> extractors = new HashSet<Extractor>();
        Extractor extractorOne = mock(Extractor.class);
        Extractor extractorTwo = mock(Extractor.class);

        given(extractorFactory.getExtractors()).willReturn(extractors);
        extractors.add(extractorOne);
        extractors.add(extractorTwo);

        //When
        DevicePrint devicePrint = devicePrintService.getDevicePrint(request);

        //Then
        verify(extractorOne).extractData(Matchers.<DevicePrint>anyObject(), eq(request));
        verify(extractorTwo).extractData(Matchers.<DevicePrint>anyObject(), eq(request));
        assertNotNull(devicePrint);
    }

    @Test
    public void shouldGetBestMatchingUserProfileWithNoStoredProfiles() {

        //Given
        DevicePrint devicePrint = mock(DevicePrint.class);
        List<UserProfile> userProfiles = new ArrayList<UserProfile>();

        given(userProfilesDao.getProfiles()).willReturn(userProfiles);

        //When
        UserProfile selectedUserProfile = devicePrintService.getBestMatchingUserProfile(devicePrint);

        //Then
        assertNull(selectedUserProfile);
    }

    @Test
    public void shouldGetBestMatchingUserProfile() {

        //Given
        DevicePrint devicePrint = mock(DevicePrint.class);
        List<UserProfile> userProfiles = new ArrayList<UserProfile>();
        UserProfile userProfileOne = mock(UserProfile.class);
        UserProfile userProfileTwo = mock(UserProfile.class);
        UserProfile userProfileThree = mock(UserProfile.class);
        DevicePrint userProfileOneDevicePrint = mock(DevicePrint.class);
        DevicePrint userProfileTwoDevicePrint = mock(DevicePrint.class);
        DevicePrint userProfileThreeDevicePrint = mock(DevicePrint.class);
        ComparisonResult userProfileOneResult = new ComparisonResult(30L);
        ComparisonResult userProfileThreeResult = new ComparisonResult(20L);

        userProfiles.add(userProfileOne);
        userProfiles.add(userProfileTwo);
        userProfiles.add(userProfileThree);
        given(userProfilesDao.getProfiles()).willReturn(userProfiles);

        given(userProfileOne.getLastSelectedDate()).willReturn(getDate(10));
        given(userProfileTwo.getLastSelectedDate()).willReturn(getDate(31));
        given(userProfileThree.getLastSelectedDate()).willReturn(getDate(29));

        given(userProfileOne.getDevicePrint()).willReturn(userProfileOneDevicePrint);
        given(userProfileTwo.getDevicePrint()).willReturn(userProfileTwoDevicePrint);
        given(userProfileThree.getDevicePrint()).willReturn(userProfileThreeDevicePrint);

        given(devicePrintComparator.compare(devicePrint, userProfileOneDevicePrint,
                devicePrintAuthenticationConfig)).willReturn(userProfileOneResult);
        given(devicePrintComparator.compare(devicePrint, userProfileThreeDevicePrint,
                devicePrintAuthenticationConfig)).willReturn(userProfileThreeResult);

        given(devicePrintAuthenticationConfig.getLong(
                DevicePrintAuthenticationConfig.MAX_TOLERATED_PENALTY_POINTS)).willReturn(50L);

        //When
        UserProfile selectedUserProfile = devicePrintService.getBestMatchingUserProfile(devicePrint);

        //Then
        assertEquals(selectedUserProfile, userProfileThree);
    }

    private Date getDate(int daysAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo);
        return calendar.getTime();
    }

    @Test
    public void shouldCreateNewProfile() throws NotUniqueUserProfileException {

        //Given
        DevicePrint devicePrint = mock(DevicePrint.class);

        given(userProfilesDao.getProfiles()).willReturn(new ArrayList<UserProfile>());

        //When
        devicePrintService.createNewProfile(devicePrint);

        //Then
        verify(userProfilesDao).removeProfile(anyString());
        ArgumentCaptor<UserProfile> userProfileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfilesDao).addProfile(userProfileCaptor.capture());
        UserProfile userProfile = userProfileCaptor.getValue();
        assertEquals(userProfile.getDevicePrint(), devicePrint);
        verify(userProfilesDao).saveProfiles();
    }

    @Test
    public void shouldCreateNewProfileAndDeleteOlderOnes() throws NotUniqueUserProfileException {

        //Given
        DevicePrint devicePrint = mock(DevicePrint.class);
        List<UserProfile> userProfiles = spy(new ArrayList<UserProfile>());
        UserProfile userProfileOne = mock(UserProfile.class);
        UserProfile userProfileTwo = mock(UserProfile.class);
        UserProfile userProfileThree = mock(UserProfile.class);

        userProfiles.add(userProfileOne);
        userProfiles.add(userProfileTwo);
        userProfiles.add(userProfileThree);
        given(userProfilesDao.getProfiles()).willReturn(userProfiles);

        given(userProfileOne.getLastSelectedDate()).willReturn(getDate(10));
        given(userProfileTwo.getLastSelectedDate()).willReturn(getDate(31));
        given(userProfileThree.getLastSelectedDate()).willReturn(getDate(30));

        //When
        devicePrintService.createNewProfile(devicePrint);

        //Then
        verify(userProfilesDao).removeProfile(anyString());

        verify(userProfiles).remove(userProfileTwo);
        verify(userProfiles).remove(userProfileThree);

        ArgumentCaptor<UserProfile> userProfileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfilesDao).addProfile(userProfileCaptor.capture());
        UserProfile userProfile = userProfileCaptor.getValue();
        assertEquals(userProfile.getDevicePrint(), devicePrint);
        verify(userProfilesDao).saveProfiles();
    }

    @Test
    public void shouldUpdateProfile() throws NotUniqueUserProfileException {

        //Given
        UserProfile userProfile = mock(UserProfile.class);
        DevicePrint devicePrint = mock(DevicePrint.class);

        given(userProfile.getUuid()).willReturn("USER_PROFILE_UUID");
        given(userProfilesDao.getProfiles()).willReturn(new ArrayList<UserProfile>());

        //When
        devicePrintService.updateProfile(userProfile, devicePrint);

        //Then
        verify(userProfile).setSelectionCounter(anyLong());
        verify(userProfile).setLastSelectedDate(Matchers.<Date>anyObject());
        verify(userProfile).setDevicePrint(devicePrint);
        verify(userProfilesDao).removeProfile(anyString());
        verify(userProfilesDao).addProfile(userProfile);
        verify(userProfilesDao).saveProfiles();
    }
}
