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
 * Copyright 2026 3A Systems LLC.
 */

package com.sun.identity.security.cert;

import java.util.Locale;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AMCertStoreTest {

    @Test
    public void shouldEscapeWildcardInAssertionValue() {
        String filter = AMCertStore.setSearchFilter("cn", "*");
        assertThat(filter).isNotEqualTo("(cn=*)");
        assertThat(filter.toLowerCase(Locale.ROOT)).contains("\\2a");
    }

    @Test
    public void shouldEscapeParenthesesInAssertionValue() {
        String filter = AMCertStore.setSearchFilter("cn", "foo)(objectClass=*");
        assertThat(filter).isNotEqualTo("(cn=foo)(objectClass=*)");
        assertThat(filter.toLowerCase(Locale.ROOT)).contains("\\29");
        assertThat(filter.toLowerCase(Locale.ROOT)).contains("\\28");
    }

    @Test
    public void shouldEscapeBackslashInAssertionValue() {
        String filter = AMCertStore.setSearchFilter("cn", "foo\\bar");
        assertThat(filter).isNotEqualTo("(cn=foo\\bar)");
        assertThat(filter.toLowerCase(Locale.ROOT)).contains("\\5c");
    }

    @Test
    public void shouldEscapeNulByteInAssertionValue() {
        String filter = AMCertStore.setSearchFilter("cn", "foo\0bar");
        assertThat(filter.toLowerCase(Locale.ROOT)).contains("\\00");
    }

    @Test
    public void shouldPreserveNormalAssertionValue() {
        String filter = AMCertStore.setSearchFilter("cn", "Barbara Jensen");
        assertThat(filter).isEqualTo("(cn=Barbara Jensen)");
    }

    @Test
    public void shouldPreserveCompositeSubjectDnValue() {
        // '=' and ',' in the composite CRL search value are not filter metacharacters
        // and must pass through unchanged.
        String filter = AMCertStore.setSearchFilter("cn", "CN=Some CA,serialNumber=123456");
        assertThat(filter).isEqualTo("(cn=CN=Some CA,serialNumber=123456)");
    }
}
