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
package org.forgerock.openam.utils;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;

public class SingleValueMapperTest {
    private SingleValueMapper<String, Integer> map;
    @BeforeMethod
    public void setup() {
        map = new SingleValueMapper<String, Integer>();
    }

    @Test
    public void shouldStoreValue() {
        String key = "badger";
        int value = 1;
        map.put(key, value);
        assertThat(map.get(key)).isEqualTo(value);
    }

    @Test
    public void shouldStoreReverseValue() {
        String key = "badger";
        int value = 1;
        map.put(key, value);
        assertThat(map.getValue(1)).isEqualTo(key);
    }

    @Test
    public void shouldCleanupPrevious() {
        String firstKey = "badger";
        String secondKey = "ferret";
        int value = 1;
        map.put(firstKey, value);
        map.put(secondKey, value);
        assertThat(map.get(firstKey)).isNull();
    }

    @Test
    public void shouldRemoveMapping() {
        String key = "badger";
        map.put(key, 1);
        map.remove(key);
        assertThat(map.get(key)).isNull();
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldNotAllowNullToBeStored() {
        map.put(null, null);
    }
}