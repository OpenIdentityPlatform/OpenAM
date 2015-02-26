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

package org.forgerock.openam.authentication.modules.deviceprint.comparators;

import org.forgerock.openam.authentication.modules.deviceprint.DevicePrintAuthenticationConfig;
import org.forgerock.openam.authentication.modules.deviceprint.model.DevicePrint;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

public class DevicePrintComparatorTest {

    private DevicePrintComparator devicePrintComparator;

    private MultiValueAttributeComparator multiValueAttributeComparator;
    private ColocationComparator colocationComparator;

    @BeforeClass
    public void setUp() {

        multiValueAttributeComparator = mock(MultiValueAttributeComparator.class);
        colocationComparator = mock(ColocationComparator.class);

        devicePrintComparator = new DevicePrintComparator(multiValueAttributeComparator, colocationComparator);
    }

    @Test
    public void shouldCompareDevicePrints() {

        //Given
        DevicePrint currentDevicePrint = mock(DevicePrint.class);
        DevicePrint storedDevicePrint = mock(DevicePrint.class);
        DevicePrintAuthenticationConfig config = mock(DevicePrintAuthenticationConfig.class);

        given(currentDevicePrint.getUserAgent()).willReturn("USER_AGENT");
        given(storedDevicePrint.getUserAgent()).willReturn("USER_AGENT");
        given(config.getLong(DevicePrintAuthenticationConfig.USER_AGENT_PENALTY_POINTS)).willReturn(100L);
        given(config.getBoolean(DevicePrintAuthenticationConfig.IGNORE_VERSION_IN_USER_AGENT)).willReturn(false);

        given(currentDevicePrint.getInstalledFonts()).willReturn("INSTALLED_FONTS");
        given(storedDevicePrint.getInstalledFonts()).willReturn("INSTALLED_FONTS");
        given(config.getInt(DevicePrintAuthenticationConfig.MAX_TOLERATED_DIFFS_IN_INSTALLED_FONTS)).willReturn(5);
        given(config.getInt(DevicePrintAuthenticationConfig.MAX_TOLERATED_PERCENTAGE_TO_MARK_AS_DIFFERENT_INSTALLED_FONTS)).willReturn(10);
        given(config.getLong(DevicePrintAuthenticationConfig.INSTALLED_FONTS_PENALTY_POINTS)).willReturn(100L);

        given(currentDevicePrint.getInstalledPlugins()).willReturn("INSTALLED_PLUGINS");
        given(storedDevicePrint.getInstalledPlugins()).willReturn("INSTALLED_PLUGINS");
        given(config.getInt(DevicePrintAuthenticationConfig.MAX_TOLERATED_DIFFS_IN_INSTALLED_PLUGINS)).willReturn(5);
        given(config.getInt(DevicePrintAuthenticationConfig.MAX_TOLERATED_PERCENTAGE_TO_MARK_AS_DIFFERENT_PLUGINS)).willReturn(10);
        given(config.getLong(DevicePrintAuthenticationConfig.INSTALLED_PLUGINS_PENALTY_POINTS)).willReturn(100L);

        given(currentDevicePrint.getScreenColourDepth()).willReturn("SCREEN_COLOUR_DEPTH");
        given(storedDevicePrint.getScreenColourDepth()).willReturn("SCREEN_COLOUR_DEPTH");
        given(config.getLong(DevicePrintAuthenticationConfig.SCREEN_COLOUR_DEPTH_PENALTY_POINTS)).willReturn(100L);

        given(currentDevicePrint.getTimezone()).willReturn("TIMEZONE");
        given(storedDevicePrint.getTimezone()).willReturn("TIMEZONE");
        given(config.getLong(DevicePrintAuthenticationConfig.TIMEZONE_PENALTY_POINTS)).willReturn(100L);

        given(currentDevicePrint.getScreenWidth()).willReturn("SCREEN_WIDTH");
        given(storedDevicePrint.getScreenWidth()).willReturn("SCREEN_WIDTH");
        given(currentDevicePrint.getScreenHeight()).willReturn("SCREEN_HEIGHT");
        given(storedDevicePrint.getScreenHeight()).willReturn("SCREEN_HEIGHT");
        given(config.getLong(DevicePrintAuthenticationConfig.SCREEN_RESOLUTION_PENALTY_POINTS)).willReturn(100L);

        given(currentDevicePrint.getLatitude()).willReturn(2.0);
        given(storedDevicePrint.getLatitude()).willReturn(2.0);
        given(currentDevicePrint.getLongitude()).willReturn(3.0);
        given(storedDevicePrint.getLongitude()).willReturn(3.0);
        given(config.getLong(DevicePrintAuthenticationConfig.LOCATION_ALLOWED_RANGE)).willReturn(100L);
        given(config.getLong(DevicePrintAuthenticationConfig.LOCATION_PENALTY_POINTS)).willReturn(100L);

        ComparisonResult cr = new ComparisonResult(10L);
        given(multiValueAttributeComparator.compare(anyString(), anyString(), anyInt(), anyInt(), anyLong()))
                .willReturn(cr);
        given(colocationComparator.compare(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyLong(), anyLong()))
                .willReturn(cr);

        //When
        ComparisonResult comparisonResult = devicePrintComparator.compare(currentDevicePrint, storedDevicePrint,
                config);

        //Then
        verify(multiValueAttributeComparator, times(2)).compare(anyString(), anyString(), anyInt(), anyInt(),
                anyLong());
        verify(colocationComparator).compare(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyLong(), anyLong());
        assertEquals((long) comparisonResult.getPenaltyPoints(), 30L);
        assertFalse(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldCompareWithNoPenaltyPoints() {

        //Given

        //When
        ComparisonResult comparisonResult = devicePrintComparator.compare("CURRENT_VALUE", "STORED_VALUE", 0L);

        //Then
        assertEquals((long) comparisonResult.getPenaltyPoints(), 0L);
        assertFalse(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldCompareWhenStoredValueIsDifferentToCurrentValue() {

        //Given

        //When
        ComparisonResult comparisonResult = devicePrintComparator.compare("CURRENT_VALUE", "STORED_VALUE", 10L);

        //Then
        assertEquals((long) comparisonResult.getPenaltyPoints(), 10L);
        assertFalse(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldCompareWhenStoredValueIsNotNullAndCurrentValueIsNull() {

        //Given

        //When
        ComparisonResult comparisonResult = devicePrintComparator.compare(null, "STORED_VALUE", 10L);

        //Then
        assertEquals((long) comparisonResult.getPenaltyPoints(), 10L);
        assertFalse(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldCompareWhenStoredValueIsNullAndCurrentValueIsNotNull() {

        //Given

        //When
        ComparisonResult comparisonResult = devicePrintComparator.compare("CURRENT_VALUE", null, 10L);

        //Then
        assertEquals((long) comparisonResult.getPenaltyPoints(), 0L);
        assertTrue(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldCompareUserAgentsIgnoringVersionNumbers() {

        //Given

        //When
        ComparisonResult comparisonResult = devicePrintComparator.compareUserAgent("USER_AGENT_1234567890.",
                "1234USER_.567890AGENT_", 10L, true);

        //Then
        assertEquals((long) comparisonResult.getPenaltyPoints(), 0L);
        assertFalse(comparisonResult.getAdditionalInfoInCurrentValue());
    }

    @Test
    public void shouldCompareScreenResolutionWhenNotTheSame() {

        //Given

        //When
        ComparisonResult comparisonResult = devicePrintComparator.compareScreenResolution("CURRENT_WIDTH",
                "CURRENT_HEIGHT", "STORED_WIDTH", "STORED_HEIGHT", 10L);

        //Then
        assertEquals((long) comparisonResult.getPenaltyPoints(), 10L);
        assertFalse(comparisonResult.getAdditionalInfoInCurrentValue());
    }
}
