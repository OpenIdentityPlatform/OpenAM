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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.devices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;
import org.testng.Assert;
import org.testng.annotations.Test;

public class DeviceJsonUtilsTest {

    DeviceJsonUtils<OathDeviceSettings> deviceJsonUtils = new DeviceJsonUtils<>(OathDeviceSettings.class);

    @Test
    public void shouldPerformRoundTripWithDeviceSettingsObject() throws IOException {
        //Given
        OathDeviceSettings object = getDeviceSettingsObject("secret", "Device Name", 1431999532, 1, true, -1);

        //When
        JsonValue jsonValue = deviceJsonUtils.toJsonValue(object);
        OathDeviceSettings oathDeviceSettings = deviceJsonUtils.toDeviceSettingValue(jsonValue);

        //Then
        Assert.assertEquals(oathDeviceSettings, object, "Expected OathDeviceSettings objects to have same content.");
    }

    @Test
    public void shouldPerformRoundTripWithDeviceSettingsList() throws IOException {
        //Given
        OathDeviceSettings object1 =
                getDeviceSettingsObject("secret", "Device Name", 1431999532, 1, true, -1);
        OathDeviceSettings object2 =
                getDeviceSettingsObject("secret2", "Device Name 2", 1431999533, 2, true, -2);
        List<OathDeviceSettings> list = new ArrayList<>();
        list.add(object1);
        list.add(object2);

        //When
        List<JsonValue> jsonValueList = deviceJsonUtils.toJsonValues(list);
        List<OathDeviceSettings> oathDeviceSettingsList = deviceJsonUtils.toDeviceSettingValues(jsonValueList);

        //Then
        Assert.assertEquals(list, oathDeviceSettingsList, "Expected OathDeviceSettings objects to have same content");
    }

    private OathDeviceSettings getDeviceSettingsObject(String sharedSecret, String deviceName, long lastLogin,
                                                           int counter, boolean checksumDigit,
                                                           int truncationOffset) {
        OathDeviceSettings oathDeviceSettings = new OathDeviceSettings(sharedSecret, deviceName, lastLogin, counter);
        oathDeviceSettings.setChecksumDigit(checksumDigit);
        oathDeviceSettings.setTruncationOffset(truncationOffset);
        return oathDeviceSettings;
    }
}