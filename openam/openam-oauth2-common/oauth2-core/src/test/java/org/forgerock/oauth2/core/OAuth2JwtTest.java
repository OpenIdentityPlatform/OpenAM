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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import org.forgerock.json.jose.jws.JwsHeader;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.NOPSigningHandler;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.util.time.TimeService;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Date;

import static org.testng.Assert.assertTrue;

/**
 * @since 12.0.0
 */
public class OAuth2JwtTest {

    public static final int VALID_EXPIRATION_TIME = 1000000;
    public static final int VALID_NOT_BEFORE_TIME = -1000000;
    public static final int INVALID_NOT_BEFORE_TIME = 1000000;
    public static final int INVALID_EXPIRATION_TIME = -1000000;

    @Test
    public void notBeforeTimeSetInPastJWTShouldBeValid() {
        JwsHeader header = new JwsHeader(Collections.<String, Object>emptyMap());
        JwtClaimsSet claims = getJwtClaimsSet(VALID_NOT_BEFORE_TIME, VALID_EXPIRATION_TIME);
        SigningHandler handler = new NOPSigningHandler();
        OAuth2Jwt oAuth2Jwt = getOAuth2Jwt(header, claims, handler);

        assertTrue(oAuth2Jwt.isValid(handler));
    }

    @Test
    public void notBeforeTimeSetInFutureJWTShouldBeInvalid() {
        JwsHeader header = new JwsHeader(Collections.<String, Object>emptyMap());
        JwtClaimsSet claims = getJwtClaimsSet(INVALID_NOT_BEFORE_TIME, VALID_EXPIRATION_TIME);
        SigningHandler handler = new NOPSigningHandler();
        OAuth2Jwt oAuth2Jwt = getOAuth2Jwt(header, claims, handler);

        assertTrue(!oAuth2Jwt.isValid(handler));
    }

    @Test
    public void notBeforeTimeSetAsNowShouldBeValid() {
        //The skew set in oAuth2Jwt is significant in this test
        JwsHeader header = new JwsHeader(Collections.<String, Object>emptyMap());
        JwtClaimsSet claims = getJwtClaimsSet(0, VALID_EXPIRATION_TIME);
        SigningHandler handler = new NOPSigningHandler();
        OAuth2Jwt oAuth2Jwt = getOAuth2Jwt(header, claims, handler);

        assertTrue(oAuth2Jwt.isValid(handler));
    }

    @Test
    public void expirationTimeSetInPastJWTShouldBeInvalid() {
        JwsHeader header = new JwsHeader(Collections.<String, Object>emptyMap());
        JwtClaimsSet claims = getJwtClaimsSet(VALID_NOT_BEFORE_TIME, INVALID_EXPIRATION_TIME);
        SigningHandler handler = new NOPSigningHandler();
        OAuth2Jwt oAuth2Jwt = getOAuth2Jwt(header, claims, handler);

        assertTrue(!oAuth2Jwt.isValid(handler));
    }

    @Test
    public void expirationTimeSetInFutureJWTShouldBeValid() {
        JwsHeader header = new JwsHeader(Collections.<String, Object>emptyMap());
        JwtClaimsSet claims = getJwtClaimsSet(VALID_NOT_BEFORE_TIME, VALID_EXPIRATION_TIME);
        SigningHandler handler = new NOPSigningHandler();
        OAuth2Jwt oAuth2Jwt = getOAuth2Jwt(header, claims, handler);

        assertTrue(oAuth2Jwt.isValid(handler));
    }

    @Test
    public void expirationTimeSetAsNowJWTShouldBeValid() {
        //The skew set in oAuth2Jwt is significant in this test
        JwsHeader header = new JwsHeader(Collections.<String, Object>emptyMap());
        JwtClaimsSet claims = getJwtClaimsSet(VALID_NOT_BEFORE_TIME, 0);
        SigningHandler handler = new NOPSigningHandler();
        OAuth2Jwt oAuth2Jwt = getOAuth2Jwt(header, claims, handler);

        assertTrue(oAuth2Jwt.isValid(handler));
    }

    private OAuth2Jwt getOAuth2Jwt(JwsHeader header, JwtClaimsSet claims, SigningHandler handler) {
        SignedJwt jwt = new SignedJwt(header, claims, handler);
        String jwtString = jwt.build();

        return OAuth2Jwt.create(jwtString);
    }

    private JwtClaimsSet getJwtClaimsSet(long notBeforeTimeOffset, long expirationTimeOffset) {
        JwtClaimsSet claims = new JwtClaimsSet();

        final long currentTimeMillis = TimeService.SYSTEM.now();
        claims.setNotBeforeTime(new Date(currentTimeMillis + notBeforeTimeOffset));
        claims.setExpirationTime(new Date(currentTimeMillis + expirationTimeOffset));
        claims.setIssuedAtTime(new Date(currentTimeMillis));
        claims.setIssuer("TEST_ISSUER");
        claims.setSubject("TEST_SUBJECT");
        claims.addAudience("TEST_AUDIENCE");
        return claims;
    }
}
