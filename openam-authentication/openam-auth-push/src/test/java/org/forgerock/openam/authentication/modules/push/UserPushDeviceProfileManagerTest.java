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
* Copyright 2016 ForgeRock AS.
*/
package org.forgerock.openam.authentication.modules.push;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.devices.DeviceJsonUtils;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.core.rest.devices.push.PushDevicesDao;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class UserPushDeviceProfileManagerTest {

    private static final String USER = "testUser";
    private static final String REALM = "/test";

    private UserPushDeviceProfileManager userPushDeviceProfileManager;
    private DeviceJsonUtils<PushDeviceSettings> deviceJsonUtils = new DeviceJsonUtils<>(PushDeviceSettings.class);

    private PushDevicesDao mockDevicesDao = mock(PushDevicesDao.class);
    private Debug mockDebug = mock(Debug.class);
    private SecureRandom mockSecureRandom = mock(SecureRandom.class);

    @BeforeTest
    public void theSetUp() { //you need this
        userPushDeviceProfileManager = new UserPushDeviceProfileManager(
                mockDevicesDao, mockDebug, mockSecureRandom, deviceJsonUtils);
    }

    @Test
    public void shouldCreateBasicProfile() {

        //given

        //when
        PushDeviceSettings profile = userPushDeviceProfileManager.createDeviceProfile();

        //then
        assertThat(profile.getCommunicationId()).isNull();
        assertThat(profile.getDeviceName()).isEqualTo(UserPushDeviceProfileManager.DEVICE_NAME);
        assertThat(profile.getSharedSecret()).isNotEmpty();
    }

    @Test
    public void shouldSaveProfile() throws IOException, AuthLoginException {
        // Given
        PushDeviceSettings deviceSettings = new PushDeviceSettings();
        deviceSettings.setSharedSecret("sekret");
        deviceSettings.setDeviceName("test device");

        JsonValue expectedJson = new DeviceJsonUtils<>(PushDeviceSettings.class).toJsonValue(deviceSettings);

        // When
        userPushDeviceProfileManager.saveDeviceProfile(USER, REALM, deviceSettings);

        // Then
        ArgumentCaptor<List> savedProfileList = ArgumentCaptor.forClass(List.class);
        verify(mockDevicesDao).saveDeviceProfiles(eq(USER), eq(REALM), savedProfileList.capture());
        assertThat(savedProfileList.getValue()).hasSize(1);
        assertThat(savedProfileList.getValue().get(0).toString()).isEqualTo(expectedJson.toString());
    }

    @Test
    public void correctNumRandomBytesAreProducedAsBase64() {

        //given
        int num = 10;
        String byteArray = userPushDeviceProfileManager.createRandomBytes(num);

        //when

        //then
        assertThat(Base64.decode(byteArray)).hasSize(num);
    }

}
