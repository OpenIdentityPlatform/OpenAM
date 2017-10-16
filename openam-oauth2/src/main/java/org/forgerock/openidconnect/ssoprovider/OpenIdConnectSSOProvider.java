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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openidconnect.ssoprovider;

import static org.forgerock.openam.oauth2.OAuth2Constants.JWTTokenParams.*;
import static org.forgerock.openam.utils.CollectionUtils.getFirstItem;

import java.security.Principal;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.exceptions.InvalidJwtException;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.oauth2.core.OAuth2Jwt;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.oauth2.CookieExtractor;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openidconnect.OpenIdConnectClientRegistration;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationStore;
import org.forgerock.openidconnect.OpenIdConnectTokenStore;
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOProviderPlugin;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.encode.CookieUtils;

/**
 * Implements an {@link com.iplanet.sso.SSOProvider} that accepts OpenID Connect ID Tokens issued by this instance of
 * OpenAM and treats them as SSOTokens. It does this by looking up the session associated with the ID Token and using
 * that. If storing OPS tokens is disabled then this will not work and ID Tokens will not be accepted as valid sessions.
 * <p>
 * All methods here apart from createToken and isApplicable throw UnsupportedOperationException as they should never be
 * called: all tokens will ultimately be created by a different SSOProvider, which will handle all methods after
 * creation.
 *
 * @since 14.0.0
 */
@Singleton
public class OpenIdConnectSSOProvider implements SSOProviderPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("OpenIdConnectSSOProvider");

    private static final String MAX_CACHE_SIZE_PROPERTY = "org.forgerock.openidconnect.ssoprovider.maxcachesize";
    private static final long MAX_CACHE_SIZE = SystemProperties.getAsLong(MAX_CACHE_SIZE_PROPERTY, 5000L);

    private final LoadingCache<String, String> idTokenToSessionIdCache =
            CacheBuilder.newBuilder().weakValues().maximumSize(MAX_CACHE_SIZE).build(new SessionIdCacheLoader());

    private final SSOTokenManager ssoTokenManager;
    private final OpenIdConnectClientRegistrationStore clientRegistrationStore;
    private final OpenIdConnectTokenStore tokenStore;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;

    private final IdTokenParser idTokenParser;
    private final CookieExtractor cookieExtractor;
    private final String cookieName;

    @Inject
    OpenIdConnectSSOProvider(final SSOTokenManager ssoTokenManager,
            final OpenIdConnectClientRegistrationStore clientRegistrationStore,
            final OpenIdConnectTokenStore tokenStore,
            final CookieExtractor cookieExtractor,
            final OAuth2ProviderSettingsFactory providerSettingsFactory) {
        this(ssoTokenManager, clientRegistrationStore, tokenStore, cookieExtractor, providerSettingsFactory,
                CookieUtils.getAmCookieName(), new IdTokenParser());
    }

    @VisibleForTesting
    OpenIdConnectSSOProvider(final SSOTokenManager ssoTokenManager,
            final OpenIdConnectClientRegistrationStore clientRegistrationStore,
            final OpenIdConnectTokenStore tokenStore,
            final CookieExtractor cookieExtractor,
            final OAuth2ProviderSettingsFactory providerSettingsFactory,
            final String cookieName,
            final IdTokenParser idTokenParser) {
        this.ssoTokenManager = ssoTokenManager;
        this.clientRegistrationStore = clientRegistrationStore;
        this.tokenStore = tokenStore;
        this.idTokenParser = idTokenParser;
        this.cookieExtractor = cookieExtractor;
        this.cookieName = cookieName;
        this.providerSettingsFactory = providerSettingsFactory;
    }

    @Override
    public boolean isApplicable(final HttpServletRequest request) {
        return request != null && isApplicable(cookieExtractor.extract(request, cookieName));
    }

    @Override
    public boolean isApplicable(final String tokenId) {
        try {
            return StringUtils.isNotEmpty(tokenId) && isEnabledFor(idTokenParser.parse(tokenId));
        } catch (SSOException e) {
            return false;
        }
    }

    private boolean isEnabledFor(OAuth2Jwt idToken) {
        if (idToken == null) {
            return false;
        }

        final String realm = idToken.getSignedJwt().getClaimsSet().get(REALM).defaultTo("/").asString();
        try {
            final OAuth2ProviderSettings settings = providerSettingsFactory.getRealmProviderSettings(realm);
            return settings != null && settings.isOpenIDConnectSSOProviderEnabled();
        } catch (NotFoundException | ServerException e) {
            LOGGER.debug("OpenIdConnectSSOProvider: Error looking up OAuth2 provider settings for realm {}", realm, e);
            return false;
        }
    }

    @Override
    public SSOToken createSSOToken(final HttpServletRequest request) throws SSOException {
        return createSSOToken(cookieExtractor.extract(request, cookieName));
    }

    @Override
    public SSOToken createSSOToken(final Principal user, final String password) throws SSOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SSOToken createSSOToken(final String idToken) throws SSOException {
        return ssoTokenManager.createSSOToken(toSessionId(idToken));
    }

    @Override
    public SSOToken createSSOToken(final String idToken, final boolean invokedByAuth,
            final boolean possiblyResetIdleTime) throws SSOException {
        // There is a mismatch between the methods that SSOTokenManager provides and SSOProvider, so we have to guess
        // what method was originally invoked. invokedByAuth is only ever true when invoked directly on
        // the dpro SSOProviderImpl from AuthContext#getSSOToken, so will never be relevant here.
        if (!possiblyResetIdleTime) {
            return ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime(toSessionId(idToken));
        } else {
            return createSSOToken(idToken);
        }
    }

    @Override
    public SSOToken createSSOToken(final String idToken, final String clientIP) throws SSOException {
        return ssoTokenManager.createSSOToken(toSessionId(idToken), clientIP);
    }

    @Override
    public void destroyToken(final SSOToken token) throws SSOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isValidToken(final SSOToken token) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isValidToken(final SSOToken token, final boolean refresh) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void validateToken(final SSOToken token) throws SSOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refreshSession(final SSOToken token) throws SSOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refreshSession(final SSOToken token, final boolean resetIdle) throws SSOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroyToken(final SSOToken destroyer, final SSOToken destroyed) throws SSOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void logout(final SSOToken token) throws SSOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<SSOToken> getValidSessions(final SSOToken requester, final String server) throws SSOException {
        throw new UnsupportedOperationException();
    }

    private String toSessionId(final String idToken) throws SSOException {
        if (StringUtils.isBlank(idToken)) {
            throw new SSOException("no id_token in request");
        }
        try {
            return idTokenToSessionIdCache.get(idToken);
        } catch (ExecutionException e) {
            Throwables.propagateIfPossible(e.getCause(), SSOException.class);
            throw new SSOException(e.getCause());
        }
    }

    private final class SessionIdCacheLoader extends CacheLoader<String, String> {
        @Override
        public String load(final @Nonnull String idTokenString) throws SSOException {
            final OAuth2Jwt idToken = idTokenParser.parse(idTokenString);

            if (idToken.isExpired()) {
                throw new SSOException("id_token has expired");
            }

            if (!isEnabledFor(idToken)) {
                throw new SSOException("id_token SSOProvider not enabled");
            }

            final JwtClaimsSet claims = idToken.getSignedJwt().getClaimsSet();
            final String clientId = getFirstItem(claims.getAudience());
            final String realm = claims.get(REALM).defaultTo("/").asString();

            final OpenIdConnectClientRegistration clientRegistration;
            try {
                clientRegistration = clientRegistrationStore.get(clientId, realm, null);
            } catch (InvalidClientException | NotFoundException e) {
                throw new SSOException(e);
            }

            if (!clientRegistration.verifyJwtIdentity(idToken)) {
                throw new SSOException("invalid id_token");
            }

            // First check to see if there is an SSOToken claim directly in the JWT (Agents 5)
            final String ssoToken = claims.getClaim(SSOTOKEN, String.class);
            if (ssoToken != null) {
                return ssoToken;
            }

            // Otherwise look up the session based on the OPS claim in the CTS
            final String ops = claims.getClaim(OPS, String.class);

            if (ops == null) {
                throw new SSOException("no session linked to id_token");
            }

            try {
                final JsonValue idTokenData = tokenStore.read(ops);
                if (idTokenData == null) {
                    throw new SSOException("session not found");
                }
                final String sessionId = getFirstItem(idTokenData.get(LEGACY_OPS).asCollection(String.class));
                if (sessionId == null) {
                    throw new SSOException("no session linked to id_token");
                }
                return sessionId;
            } catch (ServerException | NotFoundException e) {
                throw new SSOException(e);
            }
        }
    }

    static class IdTokenParser {
        private static final String BASE_64_URL = "[A-Za-z0-9-_]+";
        /** JWT is 3 or 5 dot-separated base64url encoded strings */
        private static final Pattern JWT_PATTERN = Pattern.compile("((" + BASE_64_URL + "\\.){2}){1,2}" + BASE_64_URL);

        public OAuth2Jwt parse(final String jwt) throws SSOException {
            try {
                if (!JWT_PATTERN.matcher(jwt).matches()) {
                    throw new SSOException("invalid id_token: not a valid JWT");
                }
                return OAuth2Jwt.create(jwt);
            } catch (InvalidJwtException e) {
                throw new SSOException("invalid id_token: " + e.getMessage());
            }
        }
    }
}
