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
package com.sun.identity.delegation;

import org.testng.Assert;
import org.testng.annotations.Test;

public class DelegationUtilsTest {

    private static final String VALUE = "*REALM/*";


    @Test
    public void testSwapRealmTagWithREALMInName() throws Exception {

        String realm = "o=aREALM,ou=services,dc=openam,dc=openidentityplatform,dc=org";

        String result = DelegationUtils.swapRealmTag(realm, VALUE);

        Assert.assertEquals(result, "*o=aREALM,ou=services,dc=openam,dc=openidentityplatform,dc=org/*");
    }

    @Test
    public void testSwapRealmTagWithREALMInSubRealmName() throws Exception {

        String realm = "o=aSUBREALM,o=aREALM,ou=services,dc=openam,dc=openidentityplatform,dc=org";

        String result = DelegationUtils.swapRealmTag(realm, VALUE);

        Assert.assertEquals(result, "*o=aSUBREALM,o=aREALM,ou=services,dc=openam,dc=openidentityplatform,dc=org/*");
    }
}