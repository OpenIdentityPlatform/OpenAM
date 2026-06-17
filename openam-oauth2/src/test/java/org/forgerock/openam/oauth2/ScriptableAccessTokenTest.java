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
 * Copyright 2026 3A Systems LLC.
 */
package org.forgerock.openam.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

/**
 * Unit test for {@link ScriptableAccessToken}.
 */
public class ScriptableAccessTokenTest {

    @Test
    public void setFieldExposesValueAndCollectsIt() {
        // Given
        ScriptableAccessToken accessToken = new ScriptableAccessToken(null);

        // When
        accessToken.setField("department", "engineering");

        // Then
        assertThat(accessToken.getField("department")).isEqualTo("engineering");
        assertThat(accessToken.getFields()).containsEntry("department", "engineering");
    }

    @Test
    public void getFieldFallsBackToReadOnlyContext() {
        // Given
        Map<String, Object> context = new HashMap<>();
        context.put("sub", "owner-id");
        context.put("realm", "/");
        ScriptableAccessToken accessToken = new ScriptableAccessToken(context);

        // When / Then
        assertThat(accessToken.getField("sub")).isEqualTo("owner-id");
        assertThat(accessToken.getField("realm")).isEqualTo("/");
        // Context values are not treated as script modifications.
        assertThat(accessToken.getFields()).doesNotContainKey("sub");
    }

    @Test
    public void setFieldOverridesContextValueWhenReadBack() {
        // Given
        Map<String, Object> context = new HashMap<>();
        context.put("sub", "owner-id");
        ScriptableAccessToken accessToken = new ScriptableAccessToken(context);

        // When
        accessToken.setField("sub", "overridden");

        // Then
        assertThat(accessToken.getField("sub")).isEqualTo("overridden");
        assertThat(accessToken.getFields()).containsEntry("sub", "overridden");
    }

    @Test
    public void removeFieldDropsModificationAndRecordsRemoval() {
        // Given
        ScriptableAccessToken accessToken = new ScriptableAccessToken(null);
        accessToken.setField("department", "engineering");

        // When
        accessToken.removeField("department");

        // Then
        assertThat(accessToken.getFields()).doesNotContainKey("department");
        assertThat(accessToken.getRemovedFields()).contains("department");
    }

    @Test
    public void infoMapIsImmutable() {
        // Given
        Map<String, Object> context = new HashMap<>();
        context.put("realm", "/");
        ScriptableAccessToken accessToken = new ScriptableAccessToken(context);

        // When / Then
        try {
            accessToken.getInfo().put("foo", "bar");
            org.testng.Assert.fail("Expected info map to be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void nullNamesAreIgnored() {
        // Given
        ScriptableAccessToken accessToken = new ScriptableAccessToken(null);

        // When
        accessToken.setField(null, "value");
        accessToken.removeField(null);

        // Then
        assertThat(accessToken.getFields()).isEmpty();
        assertThat(accessToken.getRemovedFields()).isEmpty();
    }
}

