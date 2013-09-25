/**
 * Copyright 2013 ForgeRock, AS.
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
package org.forgerock.openam.cts.api.fields;

import org.forgerock.openam.cts.api.fields.OAuthTokenField;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author robert.wapshott@forgerock.com
 */
public class OAuthTokenFieldTest {
    @Test
    public void shouldGenerateOAuthTokenFieldFromString() {
        // Given
        OAuthTokenField field = OAuthTokenField.ISSUED;

        // When
        OAuthTokenField result = OAuthTokenField.getField(field.getOAuthField());

        // Then
        assertEquals(result, field);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldPreventInvalidFieldsFromString() {
        // Given
        String value = "badger";

        // When / Then
        OAuthTokenField result = OAuthTokenField.getField(value);
    }
}
