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

import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.api.fields.CoreTokenFieldTypes;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.testng.annotations.Test;

import java.util.Calendar;

/**
 * @author robert.wapshott@forgerock.com
 */
public class CoreTokenFieldTypesTest {
    @Test
    public void shouldValidateIntegerField() throws CoreTokenException {
        // Given
        CoreTokenField key = CoreTokenField.INTEGER_ONE;
        Integer value = 1234;

        // When / Then
        CoreTokenFieldTypes.validateType(key, value);
    }

    @Test
    public void shouldValidateStringField() throws CoreTokenException {
        // Given
        CoreTokenField key = CoreTokenField.STRING_ONE;
        String value = "badger";

        // When / Then
        CoreTokenFieldTypes.validateType(key, value);
    }

    @Test
    public void shouldValidateDateField() throws CoreTokenException {
        // Given
        CoreTokenField key = CoreTokenField.DATE_ONE;
        Calendar value = Calendar.getInstance();

        // When / Then
        CoreTokenFieldTypes.validateType(key, value);
    }

    @Test
    public void shouldValidateByteField() throws CoreTokenException {
        // Given
        CoreTokenField key = CoreTokenField.BLOB;
        byte[] value = new byte[]{1, 2, 3, 4};

        // When / Then
        CoreTokenFieldTypes.validateType(key, value);
    }

    @Test (expectedExceptions = CoreTokenException.class)
    public void shouldValidateInvalidType() throws CoreTokenException {
        // Given
        CoreTokenField key = CoreTokenField.BLOB;
        Integer value = 1234;

        // When / Then
        CoreTokenFieldTypes.validateType(key, value);
    }
}
