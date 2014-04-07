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

import static org.forgerock.openam.authentication.modules.oidc.OpenIdConnectConfig.*;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.jaspi.modules.openid.exceptions.FailedToLoadJWKException;
import org.forgerock.jaspi.modules.openid.exceptions.OpenIdConnectVerificationException;
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.InvalidJwtException;
import org.forgerock.json.jose.exceptions.JwtReconstructionException;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.util.Reject;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Map;
import java.util.Set;

/**
 * Because the OpenIdResolver instances, responsible for validating ID Tokens for a given issuer, require pulling
 * url state to initialize, I want to cache these instances once created. This raises concerns of cache coherence, especially
 * if the OpenIdResolvers are mapped with the issuer key, which could be the same for different OIDC ID Token providers
 * (at least nothing in the specs was found that mandate this uniqueness, and nothing enforces this uniqueness either).
 * So the OpenIdResolver instances will be mapped with unique keys corresponding to their crypto context - so either:
 * 1. the .well-known/openid-connect configuration url or 2. the jwk url or 3. the client_secret. Caching the OpenIdResolvers
 * with keys unique to a given crypto context excludes the cache conflict resulting from multiple OpenIdConnect modules
 * being created with the same issuer name (previously, the issuer name was used as the cache key). This will allow
 * for the definition of multiple OpenIdConnect modules with the same name, and if these multiple modules reference the
 * same crypto state with this name (e.g. they share the same config url, jwk url, or client_secret), a single cache entry
 * will satisfy both, as the cache key defines the crypto context. Likewise, if multiple OpenIdConnect instances are created
 * with the same issuer name, but different crypto context, cache entries will exist for both, as the respective OpenIdResolver
 * instances are cached with a key corresponding to the crypto context. And because the authN framework insures that a given
 * OpenIdConnect instance is initialized with the appropriate configuration state, it will be possible to determine which cache key to use,
 * as each module will be created with the specification of only a single crypto context (config url, jwk url, or client_secret).
 * Finally, I will validate the configured issuer name against the iss field in the ID Token jwk, so I can catch scenarios
 * where a ID Token is dispatched for validation against the wrong, or incorrectly configured, OpenIdConnect module.
 */
public class OpenIdConnect extends AMLoginModule {
    private static Debug logger = Debug.getInstance("amAuth");

    private OpenIdResolverCache openIdResolverCache;
    private JwtReconstruction jwtReconstruction;
    private OpenIdConnectConfig config;
    private String principalName;


    @Override
    public void init(Subject subject, Map sharedState, Map options) {
        config = new OpenIdConnectConfig(options);
        openIdResolverCache = InjectorHolder.getInstance(OpenIdResolverCache.class);
        Reject.ifNull(openIdResolverCache, "OpenIdResolverCache could not be obtained from the InjectorHolder!");
        jwtReconstruction = new JwtReconstruction();
    }

    @Override
    public int process(Callback[] callbacks, int state) throws LoginException {
        /*
        See if a resolver is present corresponding to jwt issuer, and if not, add.
        Then dispatch validation to resolver.
         */
        final HttpServletRequest request = getHttpServletRequest();
        final String jwtValue = request.getHeader(config.getHeaderName());
        if (jwtValue == null || jwtValue.isEmpty()) {
            logger.error("No OpenIdConnect ID Token referenced by header value: " + config.getHeaderName());
            throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_MISSING_HEADER, null);
        }

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

        final JwtClaimsSet jwtClaimSet = signedJwt.getClaimsSet();
        final String jwtClaimSetIssuer = jwtClaimSet.getIssuer();
        if (!config.getConfiguredIssuer().equals(jwtClaimSetIssuer)) {
            logger.error("The issuer configured for the module, " + config.getConfiguredIssuer() + ", and the issuer found in the token, " +
                jwtClaimSetIssuer +", do not match. This means that the token authentication was directed at the wrong module, " +
                    "or the targeted module is mis-configured.");
            throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_TOKEN_ISSUER_MISMATCH, null);
        }
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
                        jwtClaimSetIssuer, config.getCryptoContextType(), config.getCryptoContextValue(), config.getCryptoContextUrlValue());
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
            principalName = mapPrincipal(jwtClaimSet);
            return ISAuthConstants.LOGIN_SUCCEED;
        } catch (OpenIdConnectVerificationException oice) {
            logger.warning("Verification of ID Token failed: " + oice);
            throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_VERIFICATION_FAILED, null);
        }
    }

    private String mapPrincipal(JwtClaimsSet jwtClaimsSet) throws AuthLoginException {
        PrincipalMapper principalMapper = instantiatePrincipalMapper();
        Map<String, Set<String>> lookupAttrs =
                principalMapper.getAttributesForPrincipalLookup(config.getLocalToJwkAttributeMappings(), jwtClaimsSet);
        if (lookupAttrs.isEmpty()) {
            throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_NO_ATTRIBUTES_MAPPED, null);
        }
        return principalMapper.lookupPrincipal(getAMIdentityRepository(getRequestOrg()), lookupAttrs);
    }

    private PrincipalMapper instantiatePrincipalMapper() throws AuthLoginException {
        try {
            return Class.forName(config.getPrincipalMapperClass()).asSubclass(PrincipalMapper.class).
                            newInstance();
        } catch (Exception e) {
            logger.error("Exception caught instantiating principal mapper class: " + e, e);
            throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_PRINCIPAL_MAPPER_INSTANTIATION_ERROR, null);
        }
    }

    @Override
    public Principal getPrincipal() {
        return new Principal() {
            public String getName() {
                return principalName;
            }
        };
    }
}