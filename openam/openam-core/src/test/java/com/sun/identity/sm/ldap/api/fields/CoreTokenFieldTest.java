/**
 * Copyright 2013 ForgeRock, Inc.
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
package com.sun.identity.sm.ldap.api.fields;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author robert.wapshott@forgerock.com
 */
public class CoreTokenFieldTest {
    @Test
    public void shouldConvertFromStringToCoreTokenField() {
        // Given
        CoreTokenField field = CoreTokenField.EXPIRY_DATE;
        String key = field.toString();

        // When
        CoreTokenField result = CoreTokenField.fromLDAPAttribute(key);

        // Then
        assertEquals(result, field);

    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldNotAllowUnknownTextForField() {
        // Given
        String key = "badger";

        // When / Then
        CoreTokenField.fromLDAPAttribute(key);
    }

}
