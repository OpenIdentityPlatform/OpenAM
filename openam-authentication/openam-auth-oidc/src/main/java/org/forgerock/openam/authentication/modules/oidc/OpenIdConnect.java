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

import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.Map;

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

    private static final String RESOURCE_BUNDLE_NAME = "amAuthOpenIdConnect";
    private static final String HEADER_NAME_KEY = "openam-auth-openidconnect-header-name";
    private static final String ISSUER_NAME_KEY = "openam-auth-openidconnect-issuer-name";
    private static final String CRYPTO_CONTEXT_TYPE_KEY = "openam-auth-openidconnect-crypto-context-type";
    private static final String CRYPTO_CONTEXT_VALUE_KEY = "openam-auth-openidconnect-crypto-context-value";

    static final String CRYPTO_CONTEXT_TYPE_CONFIG_URL = ".well-known/openid-configuration_url";
    static final String CRYPTO_CONTEXT_TYPE_JWK_URL = "jwk_url";
    static final String CRYPTO_CONTEXT_TYPE_CLIENT_SECRET = "client_secret";

    private static final String BUNDLE_KEY_VERIFICATION_FAILED = "verification_failed";
    private static final String BUNDLE_KEY_ISSUER_MISMATCH = "issuer_mismatch";
    private static final String BUNDLE_KEY_TOKEN_ISSUER_MISMATCH = "token_issuer_mismatch";
    private static final String BUNDLE_KEY_JWT_PARSE_ERROR = "jwt_parse_error";
    private static final String BUNDLE_KEY_MISSING_HEADER = "missing_header";
    private static final String BUNDLE_KEY_JWK_NOT_LOADED = "jwk_not_loaded";

    private String headerName;
    private String configuredIssuer;
    private String cryptoContextType;
    private String cryptoContextValue;
    private URL cryptoContextUrlValue;
    private OpenIdResolverCache openIdResolverCache;
    private JwtReconstruction jwtReconstruction;


    @Override
    public void init(Subject subject, Map sharedState, Map options) {
        headerName = CollectionHelper.getMapAttr(options, HEADER_NAME_KEY);
        configuredIssuer = CollectionHelper.getMapAttr(options, ISSUER_NAME_KEY);
        cryptoContextType = CollectionHelper.getMapAttr(options, CRYPTO_CONTEXT_TYPE_KEY);
        cryptoContextValue = CollectionHelper.getMapAttr(options, CRYPTO_CONTEXT_VALUE_KEY);
        Reject.ifNull(headerName, HEADER_NAME_KEY + " must be set in LoginModule options.");
        Reject.ifNull(configuredIssuer, ISSUER_NAME_KEY + " must be set in LoginModule options.");
        Reject.ifNull(cryptoContextType, CRYPTO_CONTEXT_TYPE_KEY + " must be set in LoginModule options.");
        Reject.ifNull(cryptoContextValue, CRYPTO_CONTEXT_VALUE_KEY + " must be set in LoginModule options.");
        Reject.ifFalse(CRYPTO_CONTEXT_TYPE_CLIENT_SECRET.equals(cryptoContextType) ||
                CRYPTO_CONTEXT_TYPE_JWK_URL.equals(cryptoContextType) ||
                CRYPTO_CONTEXT_TYPE_CONFIG_URL.equals(cryptoContextType), "The value corresponding to key " +
                CRYPTO_CONTEXT_TYPE_KEY + " does not correspond to an expected value. Its value:" + cryptoContextType);
        if (CRYPTO_CONTEXT_TYPE_CONFIG_URL.equals(cryptoContextType) || CRYPTO_CONTEXT_TYPE_JWK_URL.equals(cryptoContextType)) {
            try {
                cryptoContextUrlValue = new URL(cryptoContextValue);
            } catch (MalformedURLException e) {
                final String message = "The crypto context value string, " + cryptoContextValue + " is not in valid URL format: " + e;
                logger.error(message, e);
                throw new IllegalArgumentException(message);
            }
        }
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
        final String jwtValue = request.getHeader(headerName);
        if (jwtValue == null || jwtValue.isEmpty()) {
            logger.error("No OpenIdConnect ID Token referenced by header value: " + headerName);
            throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_MISSING_HEADER, null);
        }

        final SignedJwt retrievedJwt;
        try {
            retrievedJwt = jwtReconstruction.reconstructJwt(jwtValue, SignedJwt.class);
        } catch (InvalidJwtException ije) {
            logger.error("Could not reconstruct jwt from header value: " + ije);
            throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_JWT_PARSE_ERROR, null);
        } catch (JwtReconstructionException jre) {
            logger.error("Could not reconstruct jwt from header value: " + jre);
            throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_JWT_PARSE_ERROR, null);
        }

        final JwtClaimsSet jwtClaimSet = retrievedJwt.getClaimsSet();
        final String jwtClaimSetIssuer = jwtClaimSet.getIssuer();
        if (!configuredIssuer.equals(jwtClaimSetIssuer)) {
            logger.error("The issuer configured for the module, " + configuredIssuer + ", and the issuer found in the token, " +
                jwtClaimSetIssuer +", do not match. This means that the token authentication was directed at the wrong module, " +
                    "or the targeted module is mis-configured.");
            throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_TOKEN_ISSUER_MISMATCH, null);
        }
        OpenIdResolver resolver = openIdResolverCache.getResolverForIssuer(cryptoContextValue);

        if (resolver == null) {
            if (logger.messageEnabled()) {
                if (CRYPTO_CONTEXT_TYPE_CLIENT_SECRET.equals(cryptoContextType)) {
                    logger.message("Creating OpenIdResolver for issuer " + jwtClaimSetIssuer + " using client secret");
                } else {
                    logger.message("Creating OpenIdResolver for issuer " + jwtClaimSetIssuer + " using config url "
                            + cryptoContextValue);
                }
            }
            try {
                resolver = openIdResolverCache.createResolver(
                        jwtClaimSetIssuer, cryptoContextType, cryptoContextValue, cryptoContextUrlValue);
            } catch (IllegalStateException e) {
                logger.error("Could not create OpenIdResolver for issuer " + jwtClaimSetIssuer +
                        " using crypto context value " + cryptoContextValue + " :" + e);
                throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_ISSUER_MISMATCH, null);
            } catch (FailedToLoadJWKException e) {
                logger.error("Could not create OpenIdResolver for issuer " + jwtClaimSetIssuer +
                        " using crypto context value " + cryptoContextValue + " :" + e, e);
                throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_JWK_NOT_LOADED, null);
            }
        }
        try {
            resolver.validateIdentity(retrievedJwt);
            return ISAuthConstants.LOGIN_SUCCEED;
        } catch (OpenIdConnectVerificationException oice) {
            logger.warning("Verification of ID Token failed: " + oice);
            throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_VERIFICATION_FAILED, null);
        }
    }

    @Override
    public Principal getPrincipal() {
        /*
        TODO. A story will be added in sprint 53 to allow this authN module to be configured with an OIDC profile
        url, which will be hit with the sub in the OIDC ID Token jwt, to pull some user-specific attributes, which will
        then be mapped to the corresponding OpenAM attributes (like the OAuth2 LoginModule), which will then drive
        user lookup in the OpenAM data store.
        Right now, I am just returning the demo user, as this user is present in the root realm, and the Principal returned
        from this method must be a Principal in the OpenAM datastore for authentication to succeed.
         */
        return new Principal() {
            public String getName() {
                return "demo";
            }
        };
    }
}