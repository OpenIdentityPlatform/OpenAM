/*
 * Copyright 2013-2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.openam.utils;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Builder factory class for a fluent way of creating JsonValue objects.
 *
 * @author Phill Cunnington
 */
public class JsonValueBuilder {
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Creates a Builder object for creating JsonValue objects.
     *
     * @return A Builder class for creating JsonValues.
     */
    public static JsonObject jsonValue() {
        return new JsonObject();
    }

    /**
     * Converts a String into a JsonValue.
     *
     * @param json The json String.
     * @return A JsonValue object.
     * @throws IOException If there is a problem parsing the json String.
     */
    public static JsonValue toJsonValue(String json) throws JsonException {
        try {
            return new JsonValue(mapper.readValue(json, Map.class));
        } catch (IOException e) {
            throw new JsonException("Failed to parse json", e);
        }
    }

    /**
     * Converts the passed json string into a {@link JsonValue} represented as a list.
     *
     * @param json
     *         the json string
     *
     * @return a JsonValue instance represented as a list
     *
     * @throws JsonException
     *         should an error occur whilst parsing the json
     */
    public static JsonValue toJsonArray(final String json) throws JsonException {
        try {
            return new JsonValue(mapper.readValue(json, List.class));
        } catch (IOException e) {
            throw new JsonException("Failed to parse json", e);
        }
    }

    /**
     * Get singleton ObjectMapper instance for serialising to/from JSON.
     *
     * @return the shared ObjectMapper instance.
     * @see <a href="http://wiki.fasterxml.com/JacksonBestPracticesPerformance">Jackson Best Practices: Performance</a>
     */
    public static ObjectMapper getObjectMapper() {
        return mapper;
    }
}
