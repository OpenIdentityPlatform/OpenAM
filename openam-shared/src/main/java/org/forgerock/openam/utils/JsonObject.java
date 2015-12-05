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

package org.forgerock.openam.utils;

import org.forgerock.json.JsonValue;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A Builder class for creating JsonValues for json objects.
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
