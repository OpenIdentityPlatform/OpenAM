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
 * Copyright 2013-2016 ForgeRock AS.
 */
package org.forgerock.openam.cts.utils;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

public class JSONSerialisationTest extends AbstractModule {

    private final Injector injector = Guice.createInjector(this);
    private JSONSerialisation serialization = injector.getInstance(JSONSerialisation.class);

    @Override
    protected void configure() {
        bind(ObjectMapper.class).annotatedWith(Names.named(CoreTokenConstants.OBJECT_MAPPER)).to(ObjectMapper.class);
    }

    @BeforeMethod
    public void setup() throws Exception {
        serialization = injector.getInstance(JSONSerialisation.class);
    }

    @Test
    public void shouldSerialiseAString() {
        // Given
        String test = "Badger";
        // When
        String result = serialization.deserialise(serialization.serialise(test), String.class);
        // Then
        assertEquals(test, result);
    }

    @Test
    public void shouldSerialiseAMap() {
        // Given
        Map<String, Object> test = new HashMap<String, Object>();
        test.put("badger", 1234);
        test.put("ferret", 4321);

        // When
        String text = serialization.serialise(test);
        Map<String, Object> result = serialization.deserialise(text, Map.class);
        // Then
        assertEquals(test, result);
    }

    @Test
    public void shouldChangeAttributeName() {
        String name = "badger";
        assertNotEquals(name, JSONSerialisation.jsonAttributeName(name));
    }
}
