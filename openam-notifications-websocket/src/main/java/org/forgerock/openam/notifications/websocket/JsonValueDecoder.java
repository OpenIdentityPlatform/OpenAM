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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.notifications.websocket;

import java.io.IOException;
import java.util.Map;

import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;
import jakarta.websocket.EndpointConfig;

import org.forgerock.json.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Transforms a JSON string to a JSON value instance.
 *
 * @since 14.0.0
 */
public final class JsonValueDecoder implements Decoder.Text<JsonValue> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public JsonValue decode(String message) throws DecodeException {
        try {
            return new JsonValue(MAPPER.readValue(message, Map.class));
        } catch (IOException e) {
            throw new DecodeException(message, "Failed to parse json", e);
        }
    }

    @Override
    public boolean willDecode(String s) {
        return s != null;
    }

    @Override
    public void init(EndpointConfig endpointConfig) {
    }

    @Override
    public void destroy() {
    }

}