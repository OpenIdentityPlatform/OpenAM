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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.devices.OathDeviceSettings;
import org.testng.Assert;
import org.testng.annotations.Test;

public class JsonConversionUtilsTest {

    @Test
    public void shouldPerformRoundTripWithOathDeviceSettingsObject() throws IOException {
        //Given
        OathDeviceSettings object = getOathDeviceSettingsObject("secret", "Device Name", 1431999532, 1, true, -1);

        //When
        JsonValue jsonValue = JsonConversionUtils.toJsonValue(object);
        OathDeviceSettings oathDeviceSettings = JsonConversionUtils.toOathDeviceSettingValue(jsonValue);

        //Then
        Assert.assertEquals(oathDeviceSettings, object, "Expected OathDeviceSettings objects to have same content.");
    }

    @Test
    public void shouldPerformRoundTripWithOathDeviceSettingsList() throws IOException {
        //Given
        OathDeviceSettings object1 =
                getOathDeviceSettingsObject("secret", "Device Name", 1431999532, 1, true, -1);
        OathDeviceSettings object2 =
                getOathDeviceSettingsObject("secret2", "Device Name 2", 1431999533, 2, true, -2);
        List<OathDeviceSettings> list = new ArrayList<>();
        list.add(object1);
        list.add(object2);

        //When
        List<JsonValue> jsonValueList = JsonConversionUtils.toJsonValues(list);
        List<OathDeviceSettings> oathDeviceSettingsList = JsonConversionUtils.toOathDeviceSettingValues(jsonValueList);

        //Then
        Assert.assertEquals(list, oathDeviceSettingsList, "Expected OathDeviceSettings objects to have same content");
    }

    private OathDeviceSettings getOathDeviceSettingsObject(String sharedSecret, String deviceName, long lastLogin,
                                                           int counter, boolean checksumDigit,
                                                           int truncationOffset) {
        OathDeviceSettings oathDeviceSettings = new OathDeviceSettings(sharedSecret, deviceName, lastLogin, counter);
        oathDeviceSettings.setChecksumDigit(checksumDigit);
        oathDeviceSettings.setTruncationOffset(truncationOffset);
        return oathDeviceSettings;
    }
}
