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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.user.invocation;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class OpenIdConnectTokenCreationStateTest {
    private static final String NONCE = "abcddd";
    private static final boolean ALLOW_ACCESS = true;

    @Test
    public void testEquals() {
        assertEquals(buildCreationState(NONCE, ALLOW_ACCESS), buildCreationState(NONCE, ALLOW_ACCESS));
        assertEquals(buildCreationState(NONCE, !ALLOW_ACCESS), buildCreationState(NONCE, !ALLOW_ACCESS));
        assertEquals(buildCreationState(null, !ALLOW_ACCESS), buildCreationState(null, !ALLOW_ACCESS));

        assertNotEquals(buildCreationState(NONCE, ALLOW_ACCESS), buildCreationState(NONCE, !ALLOW_ACCESS));
        assertNotEquals(buildCreationState("abc", ALLOW_ACCESS), buildCreationState(NONCE, ALLOW_ACCESS));
        assertNotEquals(buildCreationState(null, ALLOW_ACCESS), buildCreationState(NONCE, ALLOW_ACCESS));
    }

    @Test
    public void testJsonRoundTrip() {
        OpenIdConnectTokenCreationState tokenCreationState = buildCreationState(NONCE, ALLOW_ACCESS);
        assertEquals(tokenCreationState, OpenIdConnectTokenCreationState.fromJson(tokenCreationState.toJson()));

        tokenCreationState = buildCreationState(NONCE, !ALLOW_ACCESS);
        assertEquals(tokenCreationState, OpenIdConnectTokenCreationState.fromJson(tokenCreationState.toJson()));

        tokenCreationState = buildCreationState(null, !ALLOW_ACCESS);
        assertEquals(tokenCreationState, OpenIdConnectTokenCreationState.fromJson(tokenCreationState.toJson()));
    }

    private OpenIdConnectTokenCreationState buildCreationState(String nonce, boolean allowAccess) {
        return OpenIdConnectTokenCreationState.builder().nonce(nonce).allowAccess(allowAccess).build();
    }
}
