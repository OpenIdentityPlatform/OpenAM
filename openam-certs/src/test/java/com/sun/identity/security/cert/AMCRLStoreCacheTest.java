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
 * Copyright 2026 3A Systems, LLC.
 */

package com.sun.identity.security.cert;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.security.cert.X509CRL;
import java.util.Map;

import org.testng.annotations.Test;

/**
 * Verifies that the JVM-wide CRL cache is bounded. It is keyed on issuer DN and shared across
 * realms, so a stream of certificates from unrelated issuers could otherwise grow it without
 * limit and never release any of it.
 */
public class AMCRLStoreCacheTest extends CertTestBase {

    @Test
    public void cacheEvictsTheLeastRecentlyUsedEntryOverTheLimit() throws Exception {
        X509CRL crl = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.ONE);
        Map<String, X509CRL> cache = AMCRLStore.createCrlCache();

        withSystemProperty(AMCRLStore.CRL_CACHE_MAX_SIZE, "2", () -> {
            cache.put("CN=A", crl);
            cache.put("CN=B", crl);
            cache.put("CN=C", crl);

            assertThat(cache).hasSize(2).containsOnlyKeys("CN=B", "CN=C");
        });
    }

    @Test
    public void cacheKeepsTheEntriesThatAreStillBeingRead() throws Exception {
        X509CRL crl = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.ONE);
        Map<String, X509CRL> cache = AMCRLStore.createCrlCache();

        withSystemProperty(AMCRLStore.CRL_CACHE_MAX_SIZE, "2", () -> {
            cache.put("CN=A", crl);
            cache.put("CN=B", crl);
            cache.get("CN=A");
            cache.put("CN=C", crl);

            assertThat(cache).containsOnlyKeys("CN=A", "CN=C");
        });
    }
}
