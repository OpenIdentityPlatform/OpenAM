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
package com.iplanet.am.sdk.common;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.utils.TimeTravelUtil;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.am.sdk.AMHashMap;

/**
 * Unit test for {@link CacheBlock}.
 */
public class CacheBlockExpiryTest {

    private static final String ENTRY_DN = "uid=test,ou=People,dc=example,dc=com";
    private static final String PRINCIPAL_DN = "uid=admin,ou=People,dc=example,dc=com";

    private AMHashMap attributes;

    @BeforeMethod
    public void setup() {
        System.setProperty(CacheBlock.ENTRY_EXPIRATION_ENABLED_KEY, "true");
        System.setProperty(CacheBlock.ENTRY_DEFAULT_EXPIRE_TIME_KEY, "5"); // 5 mins
        System.setProperty(CacheBlock.ENTRY_USER_EXPIRE_TIME_KEY, "5"); // 5 mins

        Set<String> values = new HashSet<>(1);
        values.add("Test Value");
        attributes = new AMHashMap(1);
        attributes.put("Test", values);
    }

    @Test
    public void expiredEntryTest() throws Exception {

        CacheBlock cb = new CacheBlock(ENTRY_DN, true);

        Assert.assertTrue(cb.isEntryExpirationEnabled());

        cb.putAttributes(PRINCIPAL_DN, attributes, null, true, false);

        Assert.assertTrue(cb.hasCache(PRINCIPAL_DN));
        Assert.assertTrue(cb.hasCompleteSet(PRINCIPAL_DN));

        // Go past the 5 min cache expiry timeout, expect cache entry to have expired.
        TimeTravelUtil.fastForward(10 * 60000); // 10 mins

        Assert.assertFalse(cb.hasCache(PRINCIPAL_DN));
        Assert.assertFalse(cb.hasCompleteSet(PRINCIPAL_DN));

        Map cachedAttributes = cb.getAttributes(PRINCIPAL_DN, false);
        Assert.assertTrue(cachedAttributes.isEmpty());
    }

    @Test
    public void notExpiredEntryTest() throws Exception {

        CacheBlock cb = new CacheBlock(ENTRY_DN, true);

        Assert.assertTrue(cb.isEntryExpirationEnabled());

        cb.putAttributes(PRINCIPAL_DN, attributes, null, true, false);

        Assert.assertTrue(cb.hasCache(PRINCIPAL_DN));
        Assert.assertTrue(cb.hasCompleteSet(PRINCIPAL_DN));

        // Stay within the cache expire timeout, expect cache entries to still be valid.
        TimeTravelUtil.fastForward(4 * 60000); // 4 mins

        Assert.assertTrue(cb.hasCache(PRINCIPAL_DN));
        Assert.assertTrue(cb.hasCompleteSet(PRINCIPAL_DN));

        Map cachedAttributes = cb.getAttributes(PRINCIPAL_DN, false);
        Assert.assertFalse(cachedAttributes.isEmpty());
    }
}