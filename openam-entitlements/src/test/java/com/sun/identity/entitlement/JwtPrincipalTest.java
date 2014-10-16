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

package com.sun.identity.entitlement;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.*;

public class JwtPrincipalTest {

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullJwt() {
        new JwtPrincipal(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectInvalidJwt() {
        new JwtPrincipal(json(object()));
    }

    @Test
    public void shouldUseSubAsPrincipalName() {
        // Given
        final String name = "test subject";
        final JwtPrincipal principal = new JwtPrincipal(json(object(field("sub", name))));

        // When
        final String result = principal.getName();

        // Then
        assertThat(result).isEqualTo(name);
    }
}