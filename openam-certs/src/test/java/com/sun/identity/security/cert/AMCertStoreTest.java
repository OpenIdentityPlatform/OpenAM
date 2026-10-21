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

import java.net.InetAddress;
import java.net.ServerSocket;
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
    public void aCertificateLookupLeavesTheCallersSearchFilterAlone() throws Exception {
        // CRLValidator keeps one AMLDAPCertStoreParameters in a static and resolves issuer
        // certificates through this method from every thread validating a signature. Writing the
        // lookup's filter into that shared object hands it to whichever other search reads the
        // object next — including an AMCRLStore that is part way through its own.
        AMLDAPCertStoreParameters params = AMCertStore.setLdapStoreParam("127.0.0.1", closedPort(),
                "cn=admin", "secret", "dc=example,dc=com", null, false);
        params.setSearchFilter("(cn=the caller's own)");

        AMCertStore.getCertificate(params, "cn", "TestCA");

        assertThat(params.getSearchFilter()).isEqualTo("(cn=the caller's own)");
    }

    /** A loopback port nothing listens on, so the lookup fails fast instead of hanging. */
    private static int closedPort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }

    @Test
    public void shouldPreserveCompositeSubjectDnValue() {
        // '=' and ',' in the composite CRL search value are not filter metacharacters
        // and must pass through unchanged.
        String filter = AMCertStore.setSearchFilter("cn", "CN=Some CA,serialNumber=123456");
        assertThat(filter).isEqualTo("(cn=CN=Some CA,serialNumber=123456)");
    }
}
