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

package org.forgerock.openam.tokens;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

@Singleton
public class MapToJsonBytesConverter implements Converter<Map<String, ?>, byte[]> {

    private final ObjectMapper mapper;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };

    @Inject
    public MapToJsonBytesConverter(@Named("cts-json-object-mapper") ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public byte[] convertFrom(Map<String, ?> map) {
        try {
            return mapper.writeValueAsBytes(map);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not convert input to JSON", e);
        }
    }

    @Override
    public Map<String, ?> convertBack(byte[] s) {
        try {
            return mapper.readValue(s, MAP_TYPE);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not convert input to a Map", e);
        }
    }
}
