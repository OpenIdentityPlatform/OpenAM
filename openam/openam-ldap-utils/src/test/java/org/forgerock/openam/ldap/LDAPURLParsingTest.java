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
 * Copyright 2013 ForgeRock Inc.
 */
package org.forgerock.openam.ldap;

import org.testng.annotations.Test;
import static org.fest.assertions.Assertions.*;

@Test
public class LDAPURLParsingTest {

    public void parsingWorksWithValidInput() {
        LDAPURL url = new LDAPURL("localhost:1389");
        assertThat(url.getPort()).isEqualTo(1389);
        assertThat(url.getUrl()).isEqualTo("localhost");
    }

    public void parsingWorksWithoutPort() {
        LDAPURL url = new LDAPURL("localhost");
        assertThat(url.getPort()).isEqualTo(389);
        assertThat(url.getUrl()).isEqualTo("localhost");
    }

    public void parsingWithInvalidPortFallsBackToDefaultPort() {
        LDAPURL url = new LDAPURL("localhost:abc");
        assertThat(url.getPort()).isEqualTo(389);
        assertThat(url.getUrl()).isEqualTo("localhost");
    }

    public void parsingWithInvalidPortFallsBackToDefaultPort2() {
        LDAPURL url = new LDAPURL("localhost:2389:2");
        assertThat(url.getPort()).isEqualTo(389);
        assertThat(url.getUrl()).isEqualTo("localhost");
    }

    public void parsingWithInvalidPortFallsBackToDefaultPort3() {
        LDAPURL url = new LDAPURL("localhost:2389|01");
        assertThat(url.getPort()).isEqualTo(389);
        assertThat(url.getUrl()).isEqualTo("localhost");
    }

    public void parsingWithNegativePortFallsBackToDefaultPort() {
        LDAPURL url = new LDAPURL("localhost:-4");
        assertThat(url.getPort()).isEqualTo(389);
        assertThat(url.getUrl()).isEqualTo("localhost");
    }

    public void parsingWithHighPortFallsBackToDefaultPort() {
        LDAPURL url = new LDAPURL("localhost:65536");
        assertThat(url.getPort()).isEqualTo(389);
        assertThat(url.getUrl()).isEqualTo("localhost");
    }
}
