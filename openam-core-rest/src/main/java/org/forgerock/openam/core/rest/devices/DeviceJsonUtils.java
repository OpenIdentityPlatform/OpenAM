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
package org.forgerock.openam.core.rest.devices;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.utils.JsonValueBuilder;

/**
 * Utility functions for ease of conversion between generic DeviceSettings objects and Json
 * representations of those objects.
 *
 * @since 13.0.0
 */
public class DeviceJsonUtils<T extends DeviceSettings> {

    private final Class<T> classType;

    /**
     * Constructs a new DeviceJsonUtils which will handle device settings of class supplied as the classType param.
     *
     * @param classType The class this DeviceJsonUtils instance will handle.
     */
    public DeviceJsonUtils(Class<T> classType) {
        this.classType = classType;
    }

    /**
     * Converts an {@link DeviceSettings} object to a {@link JsonValue} object which represents it.
     *
     * @param deviceSetting The {@link DeviceSettings} to convert.
     * @return The {@link JsonValue} object.
     * @throws IOException if the {@link DeviceSettings} object could not be converted to a
     * {@link JsonValue} object
     */
    public JsonValue toJsonValue(T deviceSetting) throws IOException {
        StringWriter stringWriter = new StringWriter();
        JsonValueBuilder.getObjectMapper().writeValue(stringWriter, deviceSetting);
        return JsonValueBuilder.toJsonValue(stringWriter.toString());
    }

    /**
     * Converts a {@link JsonValue} object to the {@link DeviceSettings} object which it represents.
     *
     * @param jsonValue The {@link JsonValue} to convert.
     * @return The {@link DeviceSettings} object.
     * @throws IOException if the {@link JsonValue} object could not be converted to an
     * {@link DeviceSettings} object
     */
    public T toDeviceSettingValue(JsonValue jsonValue) throws IOException {
        return JsonValueBuilder.getObjectMapper().readValue(jsonValue.toString(), classType);
    }

    /**
     * Converts a {@code List} of {@link DeviceSettings} objects to a {@code List} of {@link JsonValue} objects
     * which represents it.
     *
     * @param deviceSettings The {@code List} of {@link DeviceSettings} to convert.
     * @return The {@code List} of {@link JsonValue} objects.
     * @throws IOException if any {@link DeviceSettings} object could not be converted to a
     * {@link JsonValue} object
     */
    public List<JsonValue> toJsonValues(List<T> deviceSettings) throws IOException {
        List<JsonValue> list = new ArrayList<>(deviceSettings.size());

        for (T deviceSettingsItem : deviceSettings) {
            list.add(toJsonValue(deviceSettingsItem));
        }

        return list;
    }

    /**
     * Converts a {@code List} of {@link JsonValue} objects to the {@code List} of {@link DeviceSettings} objects
     * which it represents.
     *
     * @param jsonValues The {@code List} of {@link JsonValue} to convert.
     * @return The {@code List} of {@link DeviceSettings} objects.
     * @throws IOException if any {@link JsonValue} object could not be converted to an
     * {@link DeviceSettings} object
     */
    public List<T> toDeviceSettingValues(List<JsonValue> jsonValues) throws IOException {
        List<T> list = new ArrayList<>(jsonValues.size());

        for (JsonValue jsonValueItem : jsonValues) {
            list.add(toDeviceSettingValue(jsonValueItem));
        }

        return list;
    }

}
