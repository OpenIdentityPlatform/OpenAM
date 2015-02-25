/*
 * Copyright 2014 ForgeRock, AS.
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

package org.forgerock.openam.idm;

import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class IdServicesDecoratorUtilsTest {

    @Test
    public void shouldReturnNullResultForNullInput() {
        assertNull(IdServicesDecoratorUtils.toLowerCaseKeys(null));
    }

    @Test
    public void shouldReturnLowerCaseKeys() {
        // Given
        String key = "ASampleKey";
        Object value = "Some Value";
        Map input = Collections.singletonMap(key, value);

        // When
        Map output = IdServicesDecoratorUtils.toLowerCaseKeys(input);

        // Then
        assertEquals(output, Collections.singletonMap(key.toLowerCase(), value));
    }
}
