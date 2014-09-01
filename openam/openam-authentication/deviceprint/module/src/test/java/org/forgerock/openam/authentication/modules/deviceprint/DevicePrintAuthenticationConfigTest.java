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

import org.forgerock.openam.authentication.modules.deviceprint.model.DevicePrint;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class DevicePrintAuthenticationConfigTest {

    private DevicePrintAuthenticationConfig devicePrintAuthenticationConfig;

    private Map<String, Set<String>> config;

    @BeforeMethod
    public void setUp() {
        config = new HashMap<String, Set<String>>();
        devicePrintAuthenticationConfig = new DevicePrintAuthenticationConfig(config);
    }

    private void addToConfigSet(String key, String value) {
        Set<String> set = config.get(key);

        if (set == null) {
            set = new HashSet<String>();
            config.put(key, set);
        }

        set.add(value);
    }

    @Test
    public void shouldGetAttributeValue() {

        //Given
        addToConfigSet("KEY", "VALUE");

        //When
        String value = devicePrintAuthenticationConfig.getAttributeValue("KEY");

        //Then
        assertEquals(value, "VALUE");
    }

    @Test
    public void shouldGetInt() {

        //Given
        addToConfigSet("KEY", "1");

        //When
        int value = devicePrintAuthenticationConfig.getInt("KEY");

        //Then
        assertEquals(value, 1);
    }

    @Test
    public void shouldGetBoolean() {

        //Given
        addToConfigSet("KEY", "true");

        //When
        boolean value = devicePrintAuthenticationConfig.getBoolean("KEY");

        //Then
        assertEquals(value, true);
    }

    @Test
    public void shouldGetLong() {

        //Given
        addToConfigSet("KEY", "1000000000");

        //When
        long value = devicePrintAuthenticationConfig.getLong("KEY");

        //Then
        assertEquals(value, 1000000000L);
    }

    @Test
    public void shouldCheckForRequiredAttributesWithAllRequiredAndPresent() {

        //Given
        addToConfigSet(DevicePrintAuthenticationConfig.FONTS_REQUIRED, "true");
        addToConfigSet(DevicePrintAuthenticationConfig.GEO_LOCATION_REQUIRED, "true");
        addToConfigSet(DevicePrintAuthenticationConfig.PLUGINS_REQUIRED, "true");
        addToConfigSet(DevicePrintAuthenticationConfig.SCREEN_PARAMS_REQUIRED, "true");
        addToConfigSet(DevicePrintAuthenticationConfig.TIMEZONE_REQUIRED, "true");
        addToConfigSet(DevicePrintAuthenticationConfig.USER_AGENT_REQUIRED, "true");

        DevicePrint devicePrint = mock(DevicePrint.class);

        given(devicePrint.getInstalledFonts()).willReturn("INSTALLED_FONTS");
        given(devicePrint.getLatitude()).willReturn(2.0);
        given(devicePrint.getLongitude()).willReturn(3.0);
        given(devicePrint.getInstalledPlugins()).willReturn("INSTALLED+PLUGINS");
        given(devicePrint.getScreenColourDepth()).willReturn("SCREEN_COLOUR_DEPTH");
        given(devicePrint.getScreenHeight()).willReturn("SCREEN_HEIGHT");
        given(devicePrint.getScreenWidth()).willReturn("SCREEN_WIDTH");
        given(devicePrint.getTimezone()).willReturn("TIMEZONE");
        given(devicePrint.getUserAgent()).willReturn("USER_AGENT");

        //When
        boolean value = devicePrintAuthenticationConfig.hasRequiredAttributes(devicePrint);

        //Then
        assertTrue(value);
    }

    @Test
    public void shouldCheckForRequiredAttributesWithNoneRequiredOrPresent() {

        //Given
        addToConfigSet(DevicePrintAuthenticationConfig.FONTS_REQUIRED, "false");
        addToConfigSet(DevicePrintAuthenticationConfig.GEO_LOCATION_REQUIRED, "false");
        addToConfigSet(DevicePrintAuthenticationConfig.PLUGINS_REQUIRED, "false");
        addToConfigSet(DevicePrintAuthenticationConfig.SCREEN_PARAMS_REQUIRED, "false");
        addToConfigSet(DevicePrintAuthenticationConfig.TIMEZONE_REQUIRED, "false");
        addToConfigSet(DevicePrintAuthenticationConfig.USER_AGENT_REQUIRED, "false");

        DevicePrint devicePrint = mock(DevicePrint.class);

        //When
        boolean value = devicePrintAuthenticationConfig.hasRequiredAttributes(devicePrint);

        //Then
        assertTrue(value);
    }

    @Test
    public void shouldCheckForRequiredAttributesWithSomeRequiredAndPresent() {

        //Given
        addToConfigSet(DevicePrintAuthenticationConfig.FONTS_REQUIRED, "true");
        addToConfigSet(DevicePrintAuthenticationConfig.GEO_LOCATION_REQUIRED, "false");
        addToConfigSet(DevicePrintAuthenticationConfig.PLUGINS_REQUIRED, "true");
        addToConfigSet(DevicePrintAuthenticationConfig.SCREEN_PARAMS_REQUIRED, "false");
        addToConfigSet(DevicePrintAuthenticationConfig.TIMEZONE_REQUIRED, "false");
        addToConfigSet(DevicePrintAuthenticationConfig.USER_AGENT_REQUIRED, "true");

        DevicePrint devicePrint = mock(DevicePrint.class);

        given(devicePrint.getInstalledFonts()).willReturn("INSTALLED_FONTS");
        given(devicePrint.getLatitude()).willReturn(2.0);
        given(devicePrint.getLongitude()).willReturn(3.0);
        given(devicePrint.getInstalledPlugins()).willReturn("INSTALLED+PLUGINS");
        given(devicePrint.getUserAgent()).willReturn("USER_AGENT");

        //When
        boolean value = devicePrintAuthenticationConfig.hasRequiredAttributes(devicePrint);

        //Then
        assertTrue(value);
    }

    @Test
    public void shouldCheckForRequiredAttributesWithSomeRequiredButNotAllPresent() {

        //Given
        addToConfigSet(DevicePrintAuthenticationConfig.FONTS_REQUIRED, "true");
        addToConfigSet(DevicePrintAuthenticationConfig.GEO_LOCATION_REQUIRED, "false");
        addToConfigSet(DevicePrintAuthenticationConfig.PLUGINS_REQUIRED, "true");
        addToConfigSet(DevicePrintAuthenticationConfig.SCREEN_PARAMS_REQUIRED, "false");
        addToConfigSet(DevicePrintAuthenticationConfig.TIMEZONE_REQUIRED, "true");
        addToConfigSet(DevicePrintAuthenticationConfig.USER_AGENT_REQUIRED, "true");

        DevicePrint devicePrint = mock(DevicePrint.class);

        given(devicePrint.getInstalledFonts()).willReturn("INSTALLED_FONTS");
        given(devicePrint.getLatitude()).willReturn(2.0);
        given(devicePrint.getLongitude()).willReturn(3.0);
        given(devicePrint.getInstalledPlugins()).willReturn("INSTALLED+PLUGINS");
        given(devicePrint.getScreenColourDepth()).willReturn("SCREEN_COLOUR_DEPTH");
        given(devicePrint.getScreenHeight()).willReturn("SCREEN_HEIGHT");
        given(devicePrint.getScreenWidth()).willReturn("SCREEN_WIDTH");
        given(devicePrint.getUserAgent()).willReturn("USER_AGENT");

        //When
        boolean value = devicePrintAuthenticationConfig.hasRequiredAttributes(devicePrint);

        //Then
        assertFalse(value);
    }
}
