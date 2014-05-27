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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.service.invocation;

import org.forgerock.openam.sts.TokenMarshalException;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class UsernameTokenStateTest {
    private static final String USERNAME = "bobo";
    private static final String PASSWORD = "dodo";

    @Test
    public void testJsonRoundTrip() throws TokenMarshalException {
        UsernameTokenState untState =
                UsernameTokenState.builder().username(USERNAME.getBytes()).password(PASSWORD.getBytes()).build();
        assertEquals(untState, UsernameTokenState.fromJson(untState.toJson()));
    }

    @Test(expectedExceptions = TokenMarshalException.class)
    public void testNoPassword() throws TokenMarshalException {
        UsernameTokenState.builder().username(USERNAME.getBytes()).build();
    }

    @Test(expectedExceptions = TokenMarshalException.class)
    public void testNoUsername() throws TokenMarshalException {
        UsernameTokenState.builder().password(PASSWORD.getBytes()).build();
    }
}
