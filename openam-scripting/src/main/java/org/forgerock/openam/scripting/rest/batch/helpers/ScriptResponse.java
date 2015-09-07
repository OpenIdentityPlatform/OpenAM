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
package org.forgerock.openam.scripting.rest.batch.helpers;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.util.Reject;

/**
 * Simple device to allow easy generation of appropriate return format for SDK scripts.
 */
public class ScriptResponse {

    private final Map<String, String> responses = new HashMap<>();

    public void put(String responseName, JsonValue response) {
        responses.put(responseName, response.toString());
    }

    /**
     * Store a response in the ScriptResponse.
     *
     * @param responseId Non null responseID.
     * @param json Non null JsonValue representing the JSON returned.
     */
    public void putResponse(String responseId, JsonValue json) {
        Reject.ifNull(responseId);
        Reject.ifNull(json);
        responses.put(responseId, json.toString());
    }

    /**
     * Retrieve a specific response from the ScriptResponse.
     *
     * @param responseId Non null responseID.
     * @return Possibly null JsonValue corresponding to the responseID.
     */
    public String getResponse(String responseId) {
        Reject.ifNull(responseId);
        return responses.get(responseId);
    }

    /**
     * Returns an unmodifiable map of the responses this ScriptResponse holds.
     *
     * @return a map of the current responses.
     */
    public Map<String, String> getResponses() {
        return Collections.unmodifiableMap(responses);
    }

    /**
     * Builds this ScriptResponse into a JsonValue for transport to the SDK.
     *
     * @return a JsonValue representation of this class.
     * @throws IOException if there were issues converting to Json.
     */
    public JsonValue build() throws IOException {
        return JsonValueBuilder.toJsonValue(JsonValueBuilder.getObjectMapper().writeValueAsString(this));
    }
}
