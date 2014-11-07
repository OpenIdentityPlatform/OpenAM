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

import org.forgerock.guava.common.annotations.VisibleForTesting;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.util.time.TimeService;

import java.util.concurrent.TimeUnit;

/**
 * Parses a JWT string and offers methods to validate the JWT is valid for the use as an OAuth2 authorization grant or
 * for OAuth2 client authentication.
 *
 * @since 12.0.0
 */
public class OAuth2Jwt {

    private static final JwtReconstruction JWT_PARSER = new JwtReconstruction();
    private static final long SKEW_ALLOWANCE = TimeUnit.MINUTES.toMillis(5);
    private static final long UNREASONABLE_LIFETIME_LIMIT = TimeUnit.DAYS.toMillis(1); //TODO check this

    /**
     * Creates an {@code OAuth2Jwt} instance from the provided JWT string.
     *
     * @param jwtString The JWT string.
     * @return An {@code OAuth2Jwt} instance.
     */
    public static OAuth2Jwt create(String jwtString) {
        return new OAuth2Jwt(JWT_PARSER.reconstructJwt(jwtString, SignedJwt.class), TimeService.SYSTEM);
    }

    private final SignedJwt jwt;
    private final TimeService timeService;
    private Boolean isSignatureValid;

    @VisibleForTesting
    OAuth2Jwt(SignedJwt jwt, TimeService timeService) {
        this.jwt = jwt;
        this.timeService = timeService;
    }

    /**
     * Verifies that the JWT is valid by:
     * <ul>
     * <li>verifying the signature</li>
     * <li>ensuring the JWT contains the 'iss', 'sub', 'aud' and 'exp' claims</li>
     * <li>ensuring the JWT expiry is not unreasonably far in the future</li>
     * <li>ensuring the JWT has not expired</li>
     * <li>ensuring the JWT is not being used before its 'not before time'</li>
     * <li>ensuring the JWT issued at time is not unreasonably far in the past</li>
     * </ul>
     *
     * @param signingHandler The {@link SigningHandler} instance to verify the JWT signature with.
     * @return {@code true} if the JWT meets all the expectations.
     */
    public boolean isValid(SigningHandler signingHandler) {
        if (isSignatureValid == null) {
            isSignatureValid = jwt.verify(signingHandler);
        }
        return isSignatureValid &&
                contains("iss", "sub", "aud", "exp") &&
                !isExpiryUnreasonable() &&
                !isExpired() &&
                !isNowBeforeNbf() &&
                !isIssuedAtUnreasonable();
        //FIXME: also check if the JWT has been replayed? http://self-issued.info/docs/draft-ietf-oauth-jwt-bearer.html Section 3 point 7
    }

    private boolean contains(String... keys) {
        for (String key : keys) {
            if (jwt.getClaimsSet().getClaim(key) == null) {
                return false;
            }
        }
        return true;
    }

    private boolean isExpiryUnreasonable() {
        return jwt.getClaimsSet().getExpirationTime().getTime() > (timeService.now() + UNREASONABLE_LIFETIME_LIMIT);
    }

    private boolean isExpired() {
        return jwt.getClaimsSet().getExpirationTime().getTime() <= (timeService.now() - SKEW_ALLOWANCE);
    }

    private boolean isNowBeforeNbf() {
        boolean present = jwt.getClaimsSet().get("nbf").getObject() != null;
        return present && timeService.now() + SKEW_ALLOWANCE < jwt.getClaimsSet().getNotBeforeTime().getTime();
    }

    private boolean isIssuedAtUnreasonable() {
        boolean present = jwt.getClaimsSet().get("iat").getObject() != null;
        return present && jwt.getClaimsSet().getIssuedAtTime().getTime() < (timeService.now() - UNREASONABLE_LIFETIME_LIMIT);
    }

    /**
     * Checks that the JWT is intended for the provided audience.
     *
     * @param audience The audience.
     * @return {@code true} if the JWT 'audience' claim contains the provided audience.
     */
    public boolean isIntendedForAudience(String audience) {
        return jwt.getClaimsSet().getAudience().contains(audience);
    }

    /**
     * Gets the JWT subject.
     *
     * @return The JWT subject.
     */
    public String getSubject() {
        return jwt.getClaimsSet().getSubject();
    }
}
