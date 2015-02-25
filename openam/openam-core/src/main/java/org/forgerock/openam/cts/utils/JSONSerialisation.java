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
 * Copyright 2013-2015 ForgeRock AS.
 */
package org.forgerock.openam.cts.utils;

import java.io.IOException;
import java.text.MessageFormat;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.openam.cts.api.CoreTokenConstants;

/**
 * Responsible for serialising and deserialising objects to and from JSON.
 *
 * Note: This serialisation mechanism uses Jackson's ability to detect fields in an object to serialise.
 * It does not use getters and setters (JavaBean based) which is the default. See the configuration that
 * is defined in the constructor for more details.
 *
 * Note: Important caveat, JSON does not handle Maps with keys that are not strings (Enums, Objects etc). This
 * is covered here http://stackoverflow.com/questions/11246748/deserializing-non-string-map-keys-with-jackson?lq=1
 * the recommendation is to switch the map to use strings as keys and convert accordingly. If necessary it is
 * possible to implement custom KeySerializer/KeyDeserializer implementations to ensure the map keys are correctly
 * handled during serialisation and deserialisation. The default KeySerializer implementation calls toString on the
 * key object, which may be suitable in certain cases.
 *
 * Note: Another Important caveat, It appears that Jackson basically handles poorly any Object that is not a
 * String, Integer, Map or a List. Based on this I cannot recommend this class as a general purpose Object to JSON
 * serialisation class, unless the caller makes extra effort to ensure that their objects are being
 * serialised following the above guidelines.
 */
public class JSONSerialisation {
    private final ObjectMapper mapper;

    /**
     * New default instance of the JSONSerialsation.
     */
    @Inject
    public JSONSerialisation(@Named(CoreTokenConstants.OBJECT_MAPPER) ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Serialise an object to JSON.
     *
     * @param <T> The generic type of the passed in object.
     * @param object Non null object to serialise.
     * @return Non null JSON text.
     */
    public <T> String serialise(T object) {
        try {
            String value = mapper.writeValueAsString(object);
            return value;
        } catch (IOException e) {
            throw new IllegalStateException(
                    MessageFormat.format(
                            "Failed to serialise {0}:{1}",
                            object.getClass().getSimpleName(),
                            object),
                    e);
        }
    }

    /**
     * Deserialise JSON to an object of type T.
     *
     * @param text Non null JSON text to parse and deserialise.
     * @param clazz Class which contains the type of the value stored in JSON, required for deserialsiation.
     * @param <T> Type to cast the created object to when deserialising.
     * @return Non null object of type T.
     */
    public <T> T deserialise(String text, Class<T> clazz) {
        try {
            T value = mapper.readValue(text, clazz);
            return value;
        } catch (IOException e) {
            throw new IllegalStateException(
                    MessageFormat.format(
                            "Failed to deserailise {0}",
                            clazz.getSimpleName()),
                    e);
        }
    }

    /**
     * Wrap the attribute name in quotes and a colon to make it look like a JSON attribute.
     *
     * @param name Non null text to wrap.
     * @return A string that represents a JSON Attribute Name
     */
    public static String jsonAttributeName(String name) {
        return "\"" + name + "\":";
    }

}
