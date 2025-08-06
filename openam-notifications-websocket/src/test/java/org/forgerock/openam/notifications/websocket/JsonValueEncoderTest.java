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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import jakarta.websocket.EncodeException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link JsonValueEncoder}.
 *
 * @since 14.0.0
 */
public class JsonValueEncoderTest {

    private JsonValueEncoder encoder;

    @BeforeMethod
    public void setUp() {
        encoder = new JsonValueEncoder();
    }

    @Test
    public void whenValidJsonObjectIsGivenCorrespondingJsonStringIsCreated() throws EncodeException {
        // When
        String json = encoder.encode(json(object(field("some-property", "some-value"))));

        // Then
        assertThat(json).isEqualTo("{\"some-property\":\"some-value\"}");
    }

}