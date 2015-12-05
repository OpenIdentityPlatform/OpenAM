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
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A custom converter that converted {@code Set}s to {@code byte} arrays.
 *
 * @since 13.0.0
 */
public class SetToJsonBytesConverter implements Converter<Set<?>, byte[]> {

    private final ObjectMapper mapper;
    private static final TypeReference<Set<Object>> MAP_TYPE = new TypeReference<Set<Object>>() {
    };

    /**
     * Constructs a new SetToJsonBytesConverter instance.
     *
     * @param mapper A {@code ObjectMapper} instance.
     */
    @Inject
    public SetToJsonBytesConverter(@Named("cts-json-object-mapper") ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public byte[] convertFrom(Set<?> objects) {
        try {
            return mapper.writeValueAsBytes(objects);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not convert input to JSON", e);
        }
    }

    @Override
    public Set<?> convertBack(byte[] bytes) {
        try {
            return mapper.readValue(bytes, MAP_TYPE);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not convert input to a Set", e);
        }
    }

}
