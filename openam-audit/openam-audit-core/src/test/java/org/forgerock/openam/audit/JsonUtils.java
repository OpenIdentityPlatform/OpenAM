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
package org.forgerock.openam.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.json.JsonValue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonFactory;

/**
 * Collection of static helper methods for use by tests.
 *
 * @since 13.0.0
 */
public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper(
            new JsonFactory().configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true));

    private JsonUtils() {
        // Prevent instantiation
    }

    /**
     * Asserts that provided jsonValue matches json in referenced classpath file.
     *
     * @param jsonValue JsonValue to be checked.
     * @param resourceFilePath File containing expected JSON.
     * @throws IOException If an error occurs while trying to read the referenced file.
     */
    public static void assertJsonValue(JsonValue jsonValue, String resourceFilePath) throws IOException {
        assertThat(
                jsonValue.toString()
        ).isEqualTo(
                jsonFromFile(resourceFilePath).toString());

    }

    /**
     * Read JsonValue from the referenced classpath file.
     *
     * @param resourceFilePath File containing JSON.
     * @return JsonValue read from the file.
     * @throws IOException If an error occurs while trying to read the referenced file.
     */
    public static JsonValue jsonFromFile(String resourceFilePath) throws IOException {
        final InputStream configStream = JsonUtils.class.getResourceAsStream(resourceFilePath);
        return new JsonValue(MAPPER.readValue(configStream, Map.class));
    }

}
