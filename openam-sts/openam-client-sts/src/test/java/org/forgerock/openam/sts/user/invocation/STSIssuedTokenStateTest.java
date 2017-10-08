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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.sts.user.invocation;

import org.forgerock.openam.sts.TokenType;
import org.testng.annotations.Test;

import static org.forgerock.openam.utils.Time.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;


public class STSIssuedTokenStateTest {
    private static final String TOKEN_ID = "token_id";
    private static final String STS_ID = "sts_id";
    private static final String USER_ID = "user_id";
    private static final TokenType TOKEN_TYPE = TokenType.OPENIDCONNECT;
    private static final long TOKEN_EXPIRATION = currentTimeMillis() / 1000 + 600;

    @Test
    public void testEquals() {
        assertEquals(buildIssuedTokenState(null), buildIssuedTokenState(null));
        assertNotEquals(buildIssuedTokenState(null), buildIssuedTokenState("another_token_id"));
    }

    @Test
    public void testJsonRoundTrip() {
        assertEquals(buildIssuedTokenState(null), STSIssuedTokenState.fromJson(buildIssuedTokenState(null).toJson()));
    }

    private STSIssuedTokenState buildIssuedTokenState(String tokenId) {
        return STSIssuedTokenState.builder()
                .tokenId(tokenId == null ? TOKEN_ID : tokenId)
                .tokenType(TOKEN_TYPE.getId())
                .principalName(USER_ID)
                .stsId(STS_ID)
                .expirationTimeInSecondsFromEpoch(TOKEN_EXPIRATION)
                .build();
    }
}
