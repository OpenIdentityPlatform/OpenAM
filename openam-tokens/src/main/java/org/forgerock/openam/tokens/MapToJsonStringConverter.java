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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.tokens;

import java.io.IOException;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A custom converter that converted {@code Map}s to JSON {@code String}s.
 *
 * @since 13.0.0
 */
@Singleton
public class MapToJsonStringConverter implements Converter<Map<String, ?>, String> {

    private final ObjectMapper mapper;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };

    /**
     * Constructs a new MapToJsonStringConverter instance.
     *
     * @param mapper A {@code ObjectMapper} instance.
     */
    @Inject
    public MapToJsonStringConverter(@Named("cts-json-object-mapper") ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String convertFrom(Map<String, ?> map) {
        try {
            return mapper.writeValueAsString(map);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not convert input to JSON", e);
        }
    }

    @Override
    public Map<String, ?> convertBack(String s) {
        try {
            return mapper.readValue(s, MAP_TYPE);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not convert input to a Map", e);
        }
    }
}
