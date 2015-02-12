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

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.json.fluent.JsonValue.*;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.testng.annotations.Test;

public class JsonValueToJsonBytesConverterTest {

    private JsonValueToJsonBytesConverter converter = new JsonValueToJsonBytesConverter(new ObjectMapper());

    @Test
    public void testMapRoundTrip() throws Exception {
        //Given
        JsonValue jsonValue = json(object(field("name", "fred"), field("list", array(1, 2, 3))));

        //When
        byte[] bytes = converter.convertFrom(jsonValue);
        JsonValue value = converter.convertBack(bytes);

        //Then
        assertThat(value.isMap()).isTrue();
        assertThat(value.isList()).isFalse();
        assertThat(value.asMap().keySet()).contains("name", "list");
        assertThat(value.get("name").isString()).isTrue();
        assertThat(value.get("name").asString()).isEqualTo("fred");
        assertThat(value.get("list").isList()).isTrue();
        assertThat(value.get("list").asList()).contains(1, 2, 3);
    }

    @Test
    public void testListRoundTrip() throws Exception {
        //Given
        JsonValue jsonValue = json(array(1, 2, 3));

        //When
        byte[] bytes = converter.convertFrom(jsonValue);
        JsonValue value = converter.convertBack(bytes);

        //Then
        assertThat(value.isList()).isTrue();
        assertThat(value.isMap()).isFalse();
        assertThat(value.asList()).contains(1, 2, 3);
    }

}