package org.forgerock.openam.utils;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;

import java.io.IOException;
import java.util.Map;

/**
 * Builder factory class for a fluent way of creating JsonValue objects.
 *
 * @author Phill Cunnington
 */
public class JsonValueBuilder {

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
        ObjectMapper mapper = new ObjectMapper();
        try {
            return new JsonValue(mapper.readValue(json, Map.class));
        } catch (IOException e) {
            throw new JsonException("Failed to parse json", e);
        }
    }
}
