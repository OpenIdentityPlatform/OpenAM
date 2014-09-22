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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.impl.query;

import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class PartialTokenTest {
    @Test (expectedExceptions = UnsupportedOperationException.class)
    public void shouldNotAllowFieldsToBeModified() {
        new PartialToken(Collections.<CoreTokenField, Object>emptyMap()).getFields().clear();
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldNotAllowNullMap() {
        new PartialToken(null);
    }

    @Test
    public void shouldSupportEmptyCollectionOfFields() {
        Map<CoreTokenField, Object> empty = Collections.emptyMap();
        assertThat(new PartialToken(empty).getFields()).isEmpty();
    }

    @Test
    public void shouldContainNewFieldInCopyConstructor() {
        // Given
        String id = "badger";
        CoreTokenField field = CoreTokenField.TOKEN_ID;
        PartialToken first = new PartialToken(Collections.<CoreTokenField, Object>emptyMap());

        // When
        PartialToken clone = new PartialToken(first, field, id);

        // Then
        assertThat(clone.<String>getValue(field)).isEqualTo(id);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldNotAllowNullTokenInCopyConstructor() {
        new PartialToken(null, CoreTokenField.BLOB, "");
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldNotAllowNullFieldInCopyConstructor() {
        new PartialToken(mock(PartialToken.class), null, "");
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldNotAllowNullValueInCopyConstructor() {
        new PartialToken(mock(PartialToken.class), CoreTokenField.BLOB, null);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldReturnNullForNonRequestedField() {
        new PartialToken(Collections.<CoreTokenField, Object>emptyMap()).getValue(CoreTokenField.BLOB);
    }
}
