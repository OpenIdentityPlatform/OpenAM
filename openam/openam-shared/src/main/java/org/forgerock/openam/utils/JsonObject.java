package org.forgerock.openam.utils;

import org.forgerock.json.fluent.JsonValue;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A Builder class for creating JsonValues for json objects.
 *
 * @author Phill Cunnington
 */
public class JsonObject {

    private final Map<String, Object> content = new LinkedHashMap<String, Object>();

    /**
     * Adds a key value pair to the json object.
     *
     * @param key The key.
     * @param value The value.
     * @return The json object builder.
     */
    public JsonObject put(String key, Object value) {
        content.put(key, value);
        return this;
    }

    /**
     * Creates a builder for creating json arrays.
     *
     * @param key The key the json array will be inserted with into the json object.
     * @return The json array builder.
     */
    public JsonArray array(String key) {
        return new JsonArray(this, key);
    }

    /**
     * Takes the json object map and creates a JsonValue from it.
     *
     * @return A JsonValue.
     */
    public JsonValue build() {
        return new JsonValue(content);
    }
}
