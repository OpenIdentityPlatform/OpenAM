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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.ldap;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/**
 * Unit test for {@link LDAPUtils}.
 *
 * @since 14.0.0
 */
public final class LDAPUtilsTest {

    @Test
    public void testIsDN() throws Exception {
        // Given
        String candidateDN = "dc=forgerock";

        // When
        boolean validationResult = LDAPUtils.isDN(candidateDN);

        // Then
        assertThat(validationResult).isTrue();
    }

    @Test
    public void testIsDNInvalid() throws Exception {
        // Given
        String candidateDN = "dc=forgerock,dc";

        // When
        boolean validationResult = LDAPUtils.isDN(candidateDN);

        // Then
        assertThat(validationResult).isFalse();
    }

    @Test
    public void testIsDNInvalid2() throws Exception {
        // Given
        String candidateDN = "app_1@app.test.ru@e.s.GqF55GZjM6dzAE1u3r6w\\=\\=,dc=am,dc=com";

        // When
        boolean validationResult = LDAPUtils.isDN(candidateDN);

        // Then
        assertThat(validationResult).isFalse();
    }
}
