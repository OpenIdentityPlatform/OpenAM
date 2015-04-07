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

package org.forgerock.openam.sts.soap.config.user;

import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenType;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class TokenValidationConfigTest {
    private static final String STRING_VALIDATE_CONFIG_1 = "USERNAME|true";
    private static final String STRING_VALIDATE_CONFIG_2 = "OPENAM|false";

    @Test
    public void testEquals() {
        TokenValidationConfig tvc1 = new TokenValidationConfig(TokenType.OPENAM, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        TokenValidationConfig tvc2 = new TokenValidationConfig(TokenType.OPENAM, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(tvc1, tvc2);
        assertEquals(tvc1.hashCode(), tvc2.hashCode());

        tvc1 = new TokenValidationConfig(TokenType.OPENAM, !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        tvc2 = new TokenValidationConfig(TokenType.OPENAM, !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(tvc1, tvc2);
        assertEquals(tvc1.hashCode(), tvc2.hashCode());

        tvc1 = new TokenValidationConfig(TokenType.USERNAME, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        tvc2 = new TokenValidationConfig(TokenType.USERNAME, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(tvc1, tvc2);
        assertEquals(tvc1.hashCode(), tvc2.hashCode());

        tvc1 = new TokenValidationConfig(TokenType.USERNAME, !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        tvc2 = new TokenValidationConfig(TokenType.USERNAME, !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(tvc1, tvc2);
        assertEquals(tvc1.hashCode(), tvc2.hashCode());


        tvc1 = new TokenValidationConfig(TokenType.X509, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        tvc2 = new TokenValidationConfig(TokenType.X509, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(tvc1, tvc2);
        assertEquals(tvc1.hashCode(), tvc2.hashCode());

        tvc1 = new TokenValidationConfig(TokenType.X509, !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        tvc2 = new TokenValidationConfig(TokenType.X509, !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(tvc1, tvc2);
        assertEquals(tvc1.hashCode(), tvc2.hashCode());
    }

    @Test
    public void testNotEquals() {
        TokenValidationConfig tvc1 = new TokenValidationConfig(TokenType.OPENAM, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        TokenValidationConfig tvc2 = new TokenValidationConfig(TokenType.USERNAME, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertNotEquals(tvc1, tvc2);
        assertNotEquals(tvc1.hashCode(), tvc2.hashCode());
    }
    
    @Test
    public void testJsonRoundTrip() {
        TokenValidationConfig tvc1 = new TokenValidationConfig(TokenType.OPENAM, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(tvc1, TokenValidationConfig.fromJson(tvc1.toJson()));

        tvc1 = new TokenValidationConfig(TokenType.OPENAM, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(tvc1, TokenValidationConfig.fromJson(tvc1.toJson()));
        tvc1 = new TokenValidationConfig(TokenType.OPENAM, !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(tvc1, TokenValidationConfig.fromJson(tvc1.toJson()));

        tvc1 = new TokenValidationConfig(TokenType.USERNAME, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(tvc1, TokenValidationConfig.fromJson(tvc1.toJson()));
        tvc1 = new TokenValidationConfig(TokenType.USERNAME, !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(tvc1, TokenValidationConfig.fromJson(tvc1.toJson()));

        tvc1 = new TokenValidationConfig(TokenType.X509, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(tvc1, TokenValidationConfig.fromJson(tvc1.toJson()));
        tvc1 = new TokenValidationConfig(TokenType.X509, !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(tvc1, TokenValidationConfig.fromJson(tvc1.toJson()));
    }

    @Test
    public void testStringRepresentationRoundTrip() {
        TokenValidationConfig tvc1 = new TokenValidationConfig(TokenType.OPENAM, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(tvc1, TokenValidationConfig.fromSMSString(tvc1.toSMSString()));
        tvc1 = new TokenValidationConfig(TokenType.OPENAM, !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(tvc1, TokenValidationConfig.fromSMSString(tvc1.toSMSString()));

        tvc1 = new TokenValidationConfig(TokenType.USERNAME, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(tvc1, TokenValidationConfig.fromSMSString(tvc1.toSMSString()));
        tvc1 = new TokenValidationConfig(TokenType.USERNAME, !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(tvc1, TokenValidationConfig.fromSMSString(tvc1.toSMSString()));

        tvc1 = new TokenValidationConfig(TokenType.X509, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(tvc1, TokenValidationConfig.fromSMSString(tvc1.toSMSString()));
        tvc1 = new TokenValidationConfig(TokenType.X509, !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(tvc1, TokenValidationConfig.fromSMSString(tvc1.toSMSString()));

    }
    @Test
    public void testStringRepresentationRoundTrip2() {
        TokenValidationConfig tvc1 = TokenValidationConfig.fromSMSString(STRING_VALIDATE_CONFIG_1);
        TokenValidationConfig tvc2 = new TokenValidationConfig(TokenType.USERNAME, AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(tvc1, tvc2);

        tvc1 = TokenValidationConfig.fromSMSString(STRING_VALIDATE_CONFIG_2);
        tvc2 = new TokenValidationConfig(TokenType.OPENAM, !AMSTSConstants.INVALIDATE_INTERIM_OPENAM_SESSION);
        assertEquals(tvc1, tvc2);
    }
}
