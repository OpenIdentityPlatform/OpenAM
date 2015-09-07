/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.fr.oath;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import com.sun.identity.shared.debug.Debug;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.devices.OathDeviceSettings;
import org.forgerock.openam.core.rest.devices.OathDevicesDao;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OathMakerTest {
    private static final int SECRET_HEX_LENGTH = 20;
    private static final String USER = "testUser";
    private static final String REALM = "/test";

    @Mock
    private OathDevicesDao mockDao;

    @Mock
    private Debug mockDebug;

    private OathMaker testFactory;

    @BeforeMethod
    public void createFactory() {
        MockitoAnnotations.initMocks(this);
        testFactory = new OathMaker(mockDao, mockDebug, new SecureRandom());
    }

    @Test
    public void shouldGenerateCorrectLengthSecret() throws Exception {
        // Given

        // When
        OathDeviceSettings deviceSettings = testFactory.createDeviceProfile(SECRET_HEX_LENGTH);

        // Then
        assertThat(deviceSettings.getSharedSecret()).hasSize(SECRET_HEX_LENGTH);
    }

    @Test
    public void shouldNotGenerateLessThan8BytesOfSecret() throws Exception {
        // Given

        // When
        OathDeviceSettings deviceSettings = testFactory.createDeviceProfile(0);

        // Then
        assertThat(deviceSettings.getSharedSecret()).hasSize(16);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void shouldSaveGeneratedDevice() throws Exception {
        // Given
        OathDeviceSettings deviceSettings = new OathDeviceSettings();
        deviceSettings.setCounter(42);
        deviceSettings.setSharedSecret("sekret");
        deviceSettings.setChecksumDigit(true);
        deviceSettings.setLastLogin(99, TimeUnit.MILLISECONDS);
        deviceSettings.setDeviceName("test device");
        deviceSettings.setTruncationOffset(32);
        JsonValue expectedJson = JsonConversionUtils.toJsonValue(deviceSettings);

        // When
        testFactory.saveDeviceProfile(USER, REALM, deviceSettings);

        // Then
        ArgumentCaptor<List> savedProfileList = ArgumentCaptor.forClass(List.class);
        verify(mockDao).saveDeviceProfiles(eq(USER), eq(REALM), savedProfileList.capture());
        assertThat(savedProfileList.getValue()).hasSize(1);
        // JsonValue has no sensible .equals() method, so rely on canonical string representation
        assertThat(savedProfileList.getValue().get(0).toString()).isEqualTo(expectedJson.toString());
    }

    @Test
    public void shouldDefaultCounterToZero() throws Exception {
        // Given

        // When
        OathDeviceSettings deviceSettings = testFactory.createDeviceProfile(SECRET_HEX_LENGTH);

        // Then
        assertThat(deviceSettings.getCounter()).isEqualTo(0);
    }

    @Test
    public void shouldDefaultLastLoginTimeToZero() throws Exception {
        // Given

        // When
        OathDeviceSettings deviceSettings = testFactory.createDeviceProfile(SECRET_HEX_LENGTH);

        // Then
        assertThat(deviceSettings.getLastLogin()).isEqualTo(0);
    }

    @Test
    public void shouldDefaultDeviceName() throws Exception {
        // Given

        // When
        OathDeviceSettings deviceSettings = testFactory.createDeviceProfile(SECRET_HEX_LENGTH);

        // Then
        assertThat(deviceSettings.getDeviceName()).isNotEmpty();
    }
}