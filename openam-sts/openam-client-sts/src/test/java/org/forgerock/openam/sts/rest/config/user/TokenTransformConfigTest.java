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
 * Copyright © 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest.config.user;

import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenType;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class TokenTransformConfigTest {
    private static final String STRING_TRANSFORM = "USERNAME|SAML2|true";

    @Test
    public void testEquals() {
        TokenTransformConfig ttc1 = new TokenTransformConfig(TokenType.OPENAM, TokenType.SAML2, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        TokenTransformConfig ttc2 = new TokenTransformConfig(TokenType.OPENAM, TokenType.SAML2, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(ttc1, ttc2);
        assertEquals(ttc1.hashCode(), ttc2.hashCode());

        TokenTransformConfig ttc3 = new TokenTransformConfig(TokenType.OPENAM, TokenType.SAML2, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        TokenTransformConfig ttc4 = new TokenTransformConfig(TokenType.OPENAM, TokenType.SAML2, !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(ttc3, ttc4);
        assertEquals(ttc3.hashCode(), ttc4.hashCode());

    }

    @Test
    public void testNotEquals() {
        TokenTransformConfig ttc1 = new TokenTransformConfig(TokenType.OPENAM, TokenType.SAML2, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        TokenTransformConfig ttc2 = new TokenTransformConfig(TokenType.USERNAME, TokenType.SAML2, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertNotEquals(ttc1, ttc2);
        assertNotEquals(ttc1.hashCode(), ttc2.hashCode());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidInputToken() {
        new TokenTransformConfig(TokenType.SAML2, TokenType.SAML2, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidOutputToken() {
        new TokenTransformConfig(TokenType.USERNAME, TokenType.OPENAM, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
    }

    @Test
    public void testJsonRoundTrip() {
        TokenTransformConfig ttc1 = new TokenTransformConfig(TokenType.OPENAM, TokenType.SAML2, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(ttc1, TokenTransformConfig.fromJson(ttc1.toJson()));

        TokenTransformConfig ttc4 = new TokenTransformConfig(TokenType.OPENAM, TokenType.SAML2, !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(ttc4, TokenTransformConfig.fromJson(ttc4.toJson()));
    }

    @Test
    public void testStringRepresentationRoundTrip() {
        TokenTransformConfig ttc1 = new TokenTransformConfig(TokenType.OPENAM, TokenType.SAML2, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(ttc1, TokenTransformConfig.fromSMSString(ttc1.toSMSString()));

    }
    @Test
    public void testStringRepresentationRoundTrip2() {
        TokenTransformConfig ttc1 = TokenTransformConfig.fromSMSString(STRING_TRANSFORM);
        TokenTransformConfig ttc2 = new TokenTransformConfig(TokenType.USERNAME, TokenType.SAML2, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(ttc1, ttc2);
    }
}
