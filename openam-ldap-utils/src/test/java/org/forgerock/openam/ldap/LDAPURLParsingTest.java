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
 * Copyright 2013 ForgeRock AS.
 */
package org.forgerock.openam.ldap;

import org.testng.annotations.Test;
import static org.assertj.core.api.Assertions.*;

@Test
public class LDAPURLParsingTest {

    public void parsingWorksWithValidInput() {
        LDAPURL url = LDAPURL.valueOf("localhost:1389");
        assertThat(url.getPort()).isEqualTo(1389);
        assertThat(url.getHost()).isEqualTo("localhost");
        assertThat(url.isSSL()).isNull();
    }

    public void parsingWorksWithoutPort() {
        LDAPURL url = LDAPURL.valueOf("localhost");
        assertThat(url.getPort()).isEqualTo(389);
        assertThat(url.getHost()).isEqualTo("localhost");
    }

    public void parsingWithInvalidPortFallsBackToDefaultPort() {
        LDAPURL url = LDAPURL.valueOf("localhost:abc");
        assertThat(url.getPort()).isEqualTo(389);
        assertThat(url.getHost()).isEqualTo("localhost");
    }

    public void parsingWithInvalidPortFallsBackToDefaultPort2() {
        LDAPURL url = LDAPURL.valueOf("localhost:2389:2");
        assertThat(url.getPort()).isEqualTo(389);
        assertThat(url.getHost()).isEqualTo("localhost");
    }

    public void parsingWithInvalidPortFallsBackToDefaultPort3() {
        LDAPURL url = LDAPURL.valueOf("localhost:2389|01");
        assertThat(url.getPort()).isEqualTo(389);
        assertThat(url.getHost()).isEqualTo("localhost");
    }

    public void parsingWithInvalidPortFallsBackToDefaultPort4() {
        LDAPURL url = LDAPURL.valueOf("ldap://localhost:2389|010");
        assertThat(url.getPort()).isEqualTo(389);
        assertThat(url.getHost()).isEqualTo("localhost");
        assertThat(url.isSSL()).isFalse();
    }

    public void parsingWithInvalidPortFallsBackToDefaultPort5() {
        LDAPURL url = LDAPURL.valueOf("ldap://localhost:def");
        assertThat(url.getPort()).isEqualTo(389);
        assertThat(url.getHost()).isEqualTo("localhost");
        assertThat(url.isSSL()).isFalse();
    }

    public void parsingWithNegativePortFallsBackToDefaultPort() {
        LDAPURL url = LDAPURL.valueOf("localhost:-4");
        assertThat(url.getPort()).isEqualTo(389);
        assertThat(url.getHost()).isEqualTo("localhost");
    }

    public void parsingWithHighPortFallsBackToDefaultPort() {
        LDAPURL url = LDAPURL.valueOf("localhost:65536");
        assertThat(url.getPort()).isEqualTo(389);
        assertThat(url.getHost()).isEqualTo("localhost");
    }

    public void parsingWithLDAPSchemeWorksWithValidInput() {
        String value = "ldap://localhost:1389";
        LDAPURL url = LDAPURL.valueOf(value);
        assertThat(url.getPort()).isEqualTo(1389);
        assertThat(url.getHost()).isEqualTo("localhost");
        assertThat(url.isSSL()).isFalse();
        assertThat(url.toString()).isEqualTo(value);
    }

    public void parsingWithLDAPSSchemeWorksWithValidInput() {
        String value = "ldaps://localhost:10389";
        LDAPURL url = LDAPURL.valueOf(value);
        assertThat(url.getPort()).isEqualTo(10389);
        assertThat(url.getHost()).isEqualTo("localhost");
        assertThat(url.isSSL()).isTrue();
        assertThat(url.toString()).isEqualTo(value);
    }
}
