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

public class OpenIdConnect extends AMLoginModule {
    private static Debug logger = Debug.getInstance("amAuth");

    private static final String RESOURCE_BUNDLE_NAME = "amAuthOpenIdConnect";
    private static final String HEADER_NAME_KEY = "openam-auth-openidconnect-header-name";
    private static final String ISSUER_NAME_KEY = "openam-auth-openidconnect-issuer-name";
    private static final String CONFIGURATION_URL_KEY = "openam-auth-openidconnect-configuration-url";

    private static final String BUNDLE_KEY_VERIFICATION_FAILED = "verification_failed";
    private static final String BUNDLE_KEY_ISSUER_MISMATCH = "issuer_mismatch";
    private static final String BUNDLE_KEY_JWT_PARSE_ERROR = "jwt_parse_error";
    private static final String BUNDLE_KEY_MISSING_HEADER = "missing_header";
    private static final String BUNDLE_KEY_JWK_NOT_LOADED = "jwk_not_loaded";

    private String headerName;
    private String clientId;
    private URL configurationUrl;
    private OpenIdResolverCache openIdResolverCache;
    private JwtReconstruction jwtReconstruction;


    @Override
    public void init(Subject subject, Map sharedState, Map options) {
        headerName = CollectionHelper.getMapAttr(options, HEADER_NAME_KEY);
        clientId = CollectionHelper.getMapAttr(options, ISSUER_NAME_KEY);
        String configurationUrlString = CollectionHelper.getMapAttr(options, CONFIGURATION_URL_KEY);
        Reject.ifNull(headerName, HEADER_NAME_KEY + " must be set in LoginModule options.");
        Reject.ifNull(clientId, ISSUER_NAME_KEY + " must be set in LoginModule options.");
        Reject.ifNull(configurationUrlString, CONFIGURATION_URL_KEY + " must be set in LoginModule options.");
        try {
            configurationUrl = new URL(configurationUrlString);
        } catch (MalformedURLException e) {
            final String message = "The provider configuration string, " + configurationUrlString + " is not in valid URL format: " + e;
            logger.error(message, e);
            throw new IllegalArgumentException(message);
        }
        openIdResolverCache = InjectorHolder.getInstance(OpenIdResolverCache.class);
        Reject.ifNull(openIdResolverCache, "OpenIdResolverCache could not be obtained from the InjectorHolder!");
        jwtReconstruction = new JwtReconstruction();
    }

    @Override
    public int process(Callback[] callbacks, int state) throws LoginException {
        /*
        See if a resolver is present corresponding to jwt issuer, and if not, add. Then dispatch validation to resolver.
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
        final String issuer = jwtClaimSet.getIssuer();
        OpenIdResolver resolver = openIdResolverCache.getResolverForIssuer(issuer);

        if (resolver == null) {
            if (logger.messageEnabled()) {
                logger.message("Creating OpenIdResolver for issuer " + issuer + " using config url " + configurationUrl);
            }
            try {
                resolver = openIdResolverCache.createResolver(issuer, configurationUrl);
            } catch (IllegalStateException e) {
                logger.error("Could not create OpenIdResolver for issuer " + issuer +
                        " using configuration url " + configurationUrl + " :" + e);
                throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_ISSUER_MISMATCH, null);
            } catch (FailedToLoadJWKException e) {
                logger.error("Could not create OpenIdResolver for issuer " + issuer +
                        " using configuration url " + configurationUrl + " :" + e, e);
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
        TODO. In Sprint 52, a story will be added to allow this authN module to be configured with an OIDC profile
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