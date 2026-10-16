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

package org.forgerock.openidconnect;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Date;

import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.jose.utils.Utils;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * A verified hint is one this provider signed, which is not the same as one whose claims are of the
 * shape this code expects: a client registered for HMAC holds the key its own id_tokens are signed
 * with, so it writes every claim in them.
 */
public class OpenIDConnectEndSessionTest {

    private static final String SECRET = "a-client-secret-which-is-long-enough";

    private OpenIDConnectProvider openIDConnectProvider;
    private OpenIDConnectEndSession endSession;

    @BeforeMethod
    public void setUp() {
        openIDConnectProvider = mock(OpenIDConnectProvider.class);
        endSession = new OpenIDConnectEndSession(openIDConnectProvider);
    }

    /** The session named by the hint is the one destroyed. */
    @Test
    public void destroysTheSessionTheHintNames() throws Exception {

        endSession.endSession(mock(OAuth2Request.class), hintWithOps("a-session-identifier"));

        verify(openIDConnectProvider).destroySession("a-session-identifier");
    }

    /**
     * An {@code ops} claim that is not a string used to be cast to one, and the ClassCastException
     * escaped past the endpoint's OAuth2Exception handler as a server error - after the hint had
     * been verified, so a client could raise it on itself with a claim of its own choosing. It now
     * identifies no session, which destroySession reports as the ServerException the endpoint
     * already tolerates.
     */
    @Test
    public void doesNotCrashOnASessionIdentifierThatIsNotAString() throws Exception {

        JwtClaimsSet claims = claims();
        claims.setClaim(OAuth2Constants.JWTTokenParams.OPS, 42);
        claims.setClaim(OAuth2Constants.JWTTokenParams.LEGACY_OPS, 42);

        endSession.endSession(mock(OAuth2Request.class), sign(claims));

        verify(openIDConnectProvider).destroySession(null);
    }

    /** The primary claim being unreadable does not stop the legacy one from being read. */
    @Test
    public void fallsBackToTheLegacyClaimWhenThePrimaryOneIsNotAString() throws Exception {

        JwtClaimsSet claims = claims();
        claims.setClaim(OAuth2Constants.JWTTokenParams.OPS, 42);
        claims.setClaim(OAuth2Constants.JWTTokenParams.LEGACY_OPS, "a-legacy-session-identifier");

        endSession.endSession(mock(OAuth2Request.class), sign(claims));

        verify(openIDConnectProvider).destroySession("a-legacy-session-identifier");
    }

    @Test
    public void readsTheLegacySessionIdentifierWhenThereIsNoOps() throws Exception {

        JwtClaimsSet claims = claims();
        claims.setClaim(OAuth2Constants.JWTTokenParams.LEGACY_OPS, "a-legacy-session-identifier");

        endSession.endSession(mock(OAuth2Request.class), sign(claims));

        verify(openIDConnectProvider).destroySession("a-legacy-session-identifier");
    }

    @Test
    public void refusesToEndASessionWithNoToken() {

        assertThatThrownBy(() -> endSession.endSession(mock(OAuth2Request.class), null))
                .isInstanceOf(BadRequestException.class);
    }

    private SignedJwt hintWithOps(Object ops) {
        JwtClaimsSet claims = claims();
        claims.setClaim(OAuth2Constants.JWTTokenParams.OPS, ops);
        return sign(claims);
    }

    private JwtClaimsSet claims() {
        return new JwtBuilderFactory().claims()
                .claim("azp", "the-clients-own-id")
                .exp(new Date(System.currentTimeMillis() + 60_000))
                .build();
    }

    private SignedJwt sign(JwtClaimsSet claims) {
        String jwt = new JwtBuilderFactory()
                .jws(new SigningManager().newHmacSigningHandler(SECRET.getBytes(Utils.CHARSET)))
                .headers().alg(JwsAlgorithm.HS256).done()
                .claims(claims)
                .build();
        return new JwtReconstruction().reconstructJwt(jwt, SignedJwt.class);
    }
}
