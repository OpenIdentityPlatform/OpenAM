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

import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;

import jakarta.websocket.DecodeException;

import org.forgerock.json.JsonValue;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link JsonValueDecoder}.
 *
 * @since 14.0.0
 */
public final class JsonValueDecoderTest {

    private JsonValueDecoder decoder;

    @BeforeMethod
    public void setUp() {
        decoder = new JsonValueDecoder();
    }

    @Test
    public void whenValidJsonStringIsGivenJsonObjectIsCreated() throws DecodeException {
        // When
        JsonValue json = decoder.decode("{\"some-property\":\"some-value\"}");

        // Then
        assertThat(json).stringAt("some-property").isEqualTo("some-value");
    }

    @Test(expectedExceptions = DecodeException.class)
    public void whenInvalidJsonStringIsGivenWillThrownException() throws DecodeException {
        decoder.decode("some random string");
    }

}