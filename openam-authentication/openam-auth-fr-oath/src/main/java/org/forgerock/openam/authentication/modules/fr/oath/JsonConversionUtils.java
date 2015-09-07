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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.devices.OathDeviceSettings;
import org.forgerock.openam.utils.JsonValueBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility functions for ease of conversion between OathDeviceSettings objects and Json
 * representations of those objects.
 *
 * @since 13.0.0
 */
public class JsonConversionUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Converts an {@link OathDeviceSettings} object to a {@link JsonValue} object which represents it.
     *
     * @param oathDeviceSetting The {@link OathDeviceSettings} to convert.
     * @return The {@link JsonValue} object.
     * @throws IOException if the {@link OathDeviceSettings} object could not be converted to a
     * {@link JsonValue} object
     */
    public static JsonValue toJsonValue(OathDeviceSettings oathDeviceSetting) throws IOException {
        StringWriter stringWriter = new StringWriter();
        mapper.writeValue(stringWriter, oathDeviceSetting);
        return JsonValueBuilder.toJsonValue(stringWriter.toString());
    }

    /**
     * Converts a {@link JsonValue} object to the {@link OathDeviceSettings} object which it represents.
     *
     * @param jsonValue The {@link JsonValue} to convert.
     * @return The {@link OathDeviceSettings} object.
     * @throws IOException if the {@link JsonValue} object could not be converted to an
     * {@link OathDeviceSettings} object
     */
    public static OathDeviceSettings toOathDeviceSettingValue(JsonValue jsonValue) throws IOException {
        return mapper.readValue(jsonValue.toString(), OathDeviceSettings.class);
    }

    /**
     * Converts a {@code List} of {@link OathDeviceSettings} objects to a {@code List} of {@link JsonValue} objects
     * which represents it.
     *
     * @param oathDeviceSettings The {@code List} of {@link OathDeviceSettings} to convert.
     * @return The {@code List} of {@link JsonValue} objects.
     * @throws IOException if any {@link OathDeviceSettings} object could not be converted to a
     * {@link JsonValue} object
     */
    public static List<JsonValue> toJsonValues(List<OathDeviceSettings> oathDeviceSettings) throws IOException {
        List<JsonValue> list = new ArrayList<>(oathDeviceSettings.size());

        for (OathDeviceSettings oathDeviceSettingsItem : oathDeviceSettings) {
            list.add(toJsonValue(oathDeviceSettingsItem));
        }

        return list;
    }

    /**
     * Converts a {@code List} of {@link JsonValue} objects to the {@code List} of {@link OathDeviceSettings} objects
     * which it represents.
     *
     * @param jsonValues The {@code List} of {@link JsonValue} to convert.
     * @return The {@code List} of {@link OathDeviceSettings} objects.
     * @throws IOException if any {@link JsonValue} object could not be converted to an
     * {@link OathDeviceSettings} object
     */
    public static List<OathDeviceSettings> toOathDeviceSettingValues(List<JsonValue> jsonValues) throws IOException {
        List<OathDeviceSettings> list = new ArrayList<>(jsonValues.size());

        for (JsonValue jsonValueItem : jsonValues) {
            list.add(toOathDeviceSettingValue(jsonValueItem));
        }

        return list;
    }

}
