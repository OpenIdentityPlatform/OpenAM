/**
 * Copyright 2013 ForgeRock AS.
 *
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
 */
package org.forgerock.openam.utils;

import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author robert.wapshott@forgerock.com
 */
public class ModifiedPropertyTest {
    @Test
    public void shouldReturnValueStoredInProperty() {
        // Given
        String key = "badger";
        ModifiedProperty<String> stringProperty = new ModifiedProperty<String>();
        // When
        stringProperty.set(key);
        // Then
        assertThat(stringProperty.get()).isEqualTo(key);
    }

    @Test
    public void shouldIndicateValuesHaveChanged() {
        // Given
        String key = "badger";
        String next = "Weasel";
        ModifiedProperty<String> stringProperty = new ModifiedProperty<String>();
        stringProperty.set(key);
        // When
        stringProperty.set(next);
        // Then
        assertThat(stringProperty.hasChanged()).isTrue();
    }

    @Test
    public void shouldIndicateValuesHaveNotChanged() {
        // Given
        String key = "badger";
        ModifiedProperty<String> stringProperty = new ModifiedProperty<String>();
        stringProperty.set(key);
        // When
        stringProperty.set(key);
        // Then
        assertThat(stringProperty.hasChanged()).isFalse();
    }

    @Test
    public void shouldHandleNullProperty() {
        // Given
        ModifiedProperty<String> stringProperty = new ModifiedProperty<String>();
        // When
        stringProperty.set(null);
        // Then
        assertThat(stringProperty.hasChanged()).isFalse();
    }

    @Test
    public void shouldHandlePropertyBeingNulled() {
        // Given
        ModifiedProperty<String> stringProperty = new ModifiedProperty<String>();
        stringProperty.set("badger");
        // When
        stringProperty.set(null);
        // Then
        assertThat(stringProperty.hasChanged()).isTrue();
    }
}
