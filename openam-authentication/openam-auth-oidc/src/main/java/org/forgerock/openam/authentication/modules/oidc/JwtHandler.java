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

package org.forgerock.openam.authentication.modules.oidc;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.jaspi.modules.openid.exceptions.FailedToLoadJWKException;
import org.forgerock.jaspi.modules.openid.exceptions.OpenIdConnectVerificationException;
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.InvalidJwtException;
import org.forgerock.json.jose.exceptions.JwsSigningException;
import org.forgerock.json.jose.exceptions.JwtReconstructionException;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.util.Reject;

import java.util.List;
import java.util.Set;

import static org.forgerock.openam.authentication.modules.oidc.OpenIdConnectConfig.*;

/**
 * The logic required to validate the integrity of an OIDC ID token JWT.
 */
public class JwtHandler {
    private static Debug logger = Debug.getInstance("amAuth");
    private static final String AUTHORIZED_PARTY_CLAIM_KEY = "azp";

    private OpenIdResolverCache openIdResolverCache;
    private JwtReconstruction jwtReconstruction;
    private JwtHandlerConfig config;

    public JwtHandler(JwtHandlerConfig config) {
        openIdResolverCache = InjectorHolder.getInstance(OpenIdResolverCache.class);
        Reject.ifNull(openIdResolverCache, "OpenIdResolverCache could not be obtained from the InjectorHolder!");
        jwtReconstruction = new JwtReconstruction();
        this.config = config;
    }

    /**
     * Validate the integrity of the JWT OIDC token, according to the spec
     * (http://openid.net/specs/openid-connect-core-1_0.html#IDTokenValidation). Specifically check that the issuer is
     * the expected issuer, the token has not expired, the token has at least one audience claim, and if there is an
     * authorized party claim ("azp"), does it appear in the audience list contained within the token?
     *
     * @param jwtValue The encoded JWT string.
     * @return The validated JWT claims.
     * @throws AuthLoginException
     */
    public JwtClaimsSet validateJwt(String jwtValue) throws AuthLoginException {
        final SignedJwt signedJwt = getSignedJwt(jwtValue);
        JwtClaimsSet jwtClaimSet = signedJwt.getClaimsSet();
        final String jwtClaimSetIssuer = jwtClaimSet.getIssuer();
        if (!config.getConfiguredIssuer().equals(jwtClaimSetIssuer)) {
            logger.error("The issuer configured for the module, " + config.getConfiguredIssuer() + ", and the " +
                    "issuer found in the token, " + jwtClaimSetIssuer + ", do not match. This means that the token " +
                    "authentication was directed at the wrong module, or the targeted module is mis-configured.");
            throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_TOKEN_ISSUER_MISMATCH, null);
        }
        // See if a resolver is present corresponding to jwt issuer, and if not, add, then dispatch validation to
        // resolver.
        OpenIdResolver resolver = openIdResolverCache.getResolverForIssuer(config.getCryptoContextValue());

        if (resolver == null) {
            if (logger.messageEnabled()) {
                if (CRYPTO_CONTEXT_TYPE_CLIENT_SECRET.equals(config.getCryptoContextType())) {
                    logger.message("Creating OpenIdResolver for issuer " + jwtClaimSetIssuer + " using client secret");
                } else {
                    logger.message("Creating OpenIdResolver for issuer " + jwtClaimSetIssuer + " using config url "
                            + config.getCryptoContextValue());
                }
            }
            try {
                resolver = openIdResolverCache.createResolver(
                        jwtClaimSetIssuer, config.getCryptoContextType(), config.getCryptoContextValue(),
                        config.getCryptoContextUrlValue());
            } catch (IllegalStateException e) {
                logger.error("Could not create OpenIdResolver for issuer " + jwtClaimSetIssuer +
                        " using crypto context value " + config.getCryptoContextValue() + " :" + e);
                throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_ISSUER_MISMATCH, null);
            } catch (FailedToLoadJWKException e) {
                logger.error("Could not create OpenIdResolver for issuer " + jwtClaimSetIssuer +
                        " using crypto context value " + config.getCryptoContextValue() + " :" + e, e);
                throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_JWK_NOT_LOADED, null);
            }
        }
        try {
            resolver.validateIdentity(signedJwt);
            List<String> audienceClaim = jwtClaimSet.getAudience();
            if (!jwtHasAudienceClaim(jwtClaimSet)) {
                logger.error("No audience claim present in ID token.");
                throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_NO_AUDIENCE_CLAIM,
                        null);
            }
            if (jwtHasAuthorizedPartyClaim(jwtClaimSet)) {
                String authorizedPartyClaim = 
                        (String) jwtClaimSet.getClaim(AUTHORIZED_PARTY_CLAIM_KEY);
                if (!audienceClaim.contains(authorizedPartyClaim)) {
                    logger.error("Authorized party was present in ID token, but its value was not found in the " +
                            "audience claim.");
                    throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_AUTHORIZED_PARTY_NOT_IN_AUDIENCE,
                            null);
                }
            }
        } catch (OpenIdConnectVerificationException oice) {
            logger.warning("Verification of ID Token failed: " + oice);
            throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_VERIFICATION_FAILED, null);
        } catch (JwsSigningException jse) {
            logger.error("JwsSigningException", jse);
            throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_JWS_SIGNING_EXCEPTION, null);
        }
        return jwtClaimSet;
    }

    /**
     * Retrieve the actual JWT token from the encoded JWT token.
     *
     * @param jwtValue The encoded JWT string.
     * @return The reconstructed JWT object.
     * @throws AuthLoginException
     */
    private SignedJwt getSignedJwt(String jwtValue) throws AuthLoginException {
        final SignedJwt signedJwt;
        try {
            signedJwt = jwtReconstruction.reconstructJwt(jwtValue, SignedJwt.class);
        } catch (InvalidJwtException ije) {
            logger.error("Could not reconstruct jwt from header value: " + ije);
            throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_JWT_PARSE_ERROR, null);
        } catch (JwtReconstructionException jre) {
            logger.error("Could not reconstruct jwt from header value: " + jre);
            throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_JWT_PARSE_ERROR, null);
        }

        return signedJwt;
    }

    /**
     * Check whether or not the token is designated for the specified audience.
     *
     * @param audienceName The audience name to check that the token is intended for.
     * @param jwtClaims The parsed JWT claims.
     * @return true if the token is intended for the specified audience, false if it is not.
     * @throws AuthLoginException
     */
    public static boolean isIntendedForAudience(String audienceName, JwtClaimsSet jwtClaims) throws AuthLoginException {
        List<String> jwtAudiences = jwtClaims.getAudience();

        return jwtAudiences.contains(audienceName);
    }

    /**
     * Check whether or not the token is from one of the accepted authorized parties specified.
     *
     * @param acceptedAuthorizedParties A list of accepted authorized parties.
     * @param jwtClaims The parsed JWT claims.
     * @return true if the token's authorized party is in the list of accepted authorized parties, false if it is not,
     * or the token does not contain an authorized party entry.
     * @throws AuthLoginException
     */
    public static boolean isFromValidAuthorizedParty(Set<String> acceptedAuthorizedParties, JwtClaimsSet jwtClaims)
            throws AuthLoginException {
        String authorizedPartyClaim = (String) jwtClaims.getClaim(AUTHORIZED_PARTY_CLAIM_KEY);

        if (jwtHasAuthorizedPartyClaim(jwtClaims)) {
            return acceptedAuthorizedParties.contains(authorizedPartyClaim);
        }

        logger.error("No authorized party found in JWT claims set.");
        return false;
    }

    /**
     * Check if the token has an authorized party ("azp") entry.
     *
     * @param jwtClaims The parsed JWT claims.
     * @return true if the token contains an authorized party claim and that claim is not an empty string, otherwise
     * false.
     * @throws AuthLoginException
     */
    private static boolean jwtHasAudienceClaim(JwtClaimsSet jwtClaims) throws AuthLoginException {
        List<String> audienceClaim = jwtClaims.getAudience();

        return (audienceClaim != null && !audienceClaim.isEmpty());
    }

    /**
     * Check if the token has an authorized party ("azp") entry.
     *
     * @param jwtClaims The parsed JWT claims.
     * @return true if the token contains an authorized party claim and that claim is not an empty string, otherwise
     * false.
     * @throws AuthLoginException
     */
    public static boolean jwtHasAuthorizedPartyClaim(JwtClaimsSet jwtClaims) throws AuthLoginException {
        String authorizedPartyClaim = (String) jwtClaims.getClaim(AUTHORIZED_PARTY_CLAIM_KEY);

        return (authorizedPartyClaim != null && !authorizedPartyClaim.isEmpty());
    }

    /**
     * Get the set of claims contained within the specified token.
     *
     * @param jwtValue The encoded JWT string.
     * @return The set of claims contained within the token.
     * @throws AuthLoginException
     */
    public JwtClaimsSet getJwtClaims(String jwtValue) throws AuthLoginException {
        SignedJwt signedJwt = getSignedJwt(jwtValue);
        return signedJwt.getClaimsSet();
    }

}
