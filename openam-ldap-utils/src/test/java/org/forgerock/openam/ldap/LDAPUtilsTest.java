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
 * Portions Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openam.ldap;

import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.opendj.ldap.DN;
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

    @Test
    public void testIsDNWithEqualsInValue() throws Exception {
        // Given
        String candidateDN =
                "ou=https://accounts.google.com/o/saml2?idpid=12345,"
                        + "ou=default,ou=OrganizationConfig,ou=1.0,"
                        + "ou=sunFMSAML2MetadataService,ou=services,dc=openam,dc=org";

        // When
        boolean validationResult = LDAPUtils.isDN(candidateDN);

        // Then
        assertThat(validationResult).isTrue();
    }

    @Test
    public void testIsDNWithNonLeadingSharpInValue() throws Exception {
        // Given
        String candidateDN = "ou=https://idp.example.com/metadata#v1,dc=openam,dc=org";

        // When
        DN dn = LDAPUtils.newDN(candidateDN);

        // Then
        assertThat(LDAPUtils.isDN(candidateDN)).isTrue();
        assertThat(dn.size()).isEqualTo(3);
        assertThat(LDAPUtils.rdnValueFromDn(dn)).isEqualTo("https://idp.example.com/metadata#v1");
        assertThat(LDAPUtils.isDN("ou=a#,dc=x")).isTrue();
        assertThat(LDAPUtils.isDN("cn=a#b+sn=c#d,ou=e#f+l=g#h")).isTrue();
    }

    @Test
    public void testIsDNWithEscapedSharpRoundTrip() throws Exception {
        // Given
        String candidateDN = "ou=https://idp.example.com/metadata\\#v1,dc=openam,dc=org";

        // When
        String serialised = DN.valueOf(candidateDN).toString();

        // Then
        assertThat(LDAPUtils.isDN(candidateDN)).isTrue();
        assertThat(serialised).isEqualTo("ou=https://idp.example.com/metadata#v1,dc=openam,dc=org");
        assertThat(LDAPUtils.isDN(serialised)).isTrue();
    }

    @Test
    public void testNewDNWithLeadingSharpInValue() throws Exception {
        // A leading '#' starts a hexstring, so "#x" must be rejected by the pre-check, not by DN.valueOf
        assertThat(LDAPUtils.newDN("ou=#04024869,dc=x").size()).isEqualTo(2);
        assertThat(LDAPUtils.newDN("ou=#x,dc=x").isRootDN()).isTrue();
        // DN.valueOf skips spaces after '=', so the '#' that follows them is still a leading one
        assertThat(LDAPUtils.newDN("ou= #x,dc=x").isRootDN()).isTrue();
        assertThat(LDAPUtils.newDN("ou= a#x,dc=x").size()).isEqualTo(2);
        assertThat(LDAPUtils.newDN("ou=a,dc=#x").isRootDN()).isTrue();
        assertThat(LDAPUtils.newDN("ou=a+cn=#x,dc=x").isRootDN()).isTrue();
        assertThat(LDAPUtils.newDN("ou=a,dc=x+cn=#x").isRootDN()).isTrue();
    }
}
