/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openam.oauth2.store.impl;

import java.util.Set;
import java.util.UUID;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceAccessor;
import org.forgerock.json.resource.JsonResourceContext;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openam.ext.cts.CoreTokenService;
import org.forgerock.openam.ext.cts.repo.JMQTokenRepo;
import org.forgerock.openam.oauth2.model.impl.AccessTokenImpl;
import org.forgerock.openam.oauth2.model.impl.AuthorizationCodeImpl;
import org.forgerock.openam.oauth2.model.impl.RefreshTokenImpl;
import org.forgerock.openam.oauth2.model.impl.SessionClientImpl;
import org.forgerock.restlet.ext.oauth2.OAuthProblemException;
import org.forgerock.restlet.ext.oauth2.model.AccessToken;
import org.forgerock.restlet.ext.oauth2.model.AuthorizationCode;
import org.forgerock.restlet.ext.oauth2.model.RefreshToken;
import org.forgerock.restlet.ext.oauth2.model.SessionClient;
import org.forgerock.restlet.ext.oauth2.provider.OAuth2TokenStore;
import org.restlet.data.Status;

/**
 * Implementation of the OAuthTokenStore interface that uses the
 * CoreTokenService for storing the tokens as JSON objects.
 * 
 * @author Jonathan Scudder
 */
public class DefaultOAuthTokenStoreImpl implements OAuth2TokenStore {

    private static final long AUTHZ_CODE_LIFETIME = 1000 * 60 * 10;
    // 10 minutes TODO: make configurable
    private static final long REFRESH_TOKEN_LIFETIME = 1000 * 60 * 60 * 8;
    // 8 hours TODO: make configurable
    private static final long ACCESS_TOKEN_LIFETIME = 1000 * 60 * 10;
    // 10 minutes TODO: make configurable

    // Removed: long requestTime
    // Removed: String clientID
    // Removed: String redirectURI

    private JsonResource repository;

    /**
     * Constructor, creates the repository instance used.
     * 
     * @throws OAuthProblemException
     */
    public DefaultOAuthTokenStoreImpl() {
        try {
            repository = new CoreTokenService(new JMQTokenRepo());
        } catch (Exception e) {
            // TODO: legacy code throws Exception, look to refactor
            throw new OAuthProblemException(Status.SERVER_ERROR_SERVICE_UNAVAILABLE.getCode(),
                    "Service unavailable", "Could not create underlying storage", null);
        }
    }

    @Override
    public AuthorizationCode createAuthorizationCode(Set<String> scope, String realm, String uuid,
            SessionClient client) {

        String id = UUID.randomUUID().toString();
        long expiresIn = AUTHZ_CODE_LIFETIME;

        AuthorizationCodeImpl code =
                new AuthorizationCodeImpl(id, uuid, client, realm, scope, false, expiresIn);
        JsonValue response = null;

        // Store in CTS
        JsonResourceAccessor accessor =
                new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());
        try {
            response = accessor.create(id, code);
        } catch (JsonResourceException e) {
            // TODO: logging
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not create token in CTS", null);
        }

        if (response == null) {
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not create token in CTS", null);
        }

        return code;
    }

    @Override
    public AuthorizationCode readAuthorizationCode(String id) {
        JsonValue response = null;

        // Read from CTS
        JsonResourceAccessor accessor =
                new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());
        try {
            response = accessor.read(id);
        } catch (JsonResourceException e) {
            // TODO: logging
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not read token from CTS: " + e.getMessage(), null);
        }

        if (response == null) {
            throw new OAuthProblemException(Status.CLIENT_ERROR_NOT_FOUND.getCode(), "Not found",
                    "Could not find token from CTS", null);
        }

        // Construct an AuthorizationCode object and return it
        // TODO use _id instead of id?
        AuthorizationCode ac = new AuthorizationCodeImpl(id, response);
        return ac;
    }

    @Override
    public void deleteAuthorizationCode(String id) {
        JsonValue response = null;

        // Read from CTS
        JsonResourceAccessor accessor =
                new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());
        try {
            response = accessor.read(id);
        } catch (JsonResourceException e) {
            // TODO: logging
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not read token from CTS: " + e.getMessage(), null);
        }

        if (response == null) {
            throw new OAuthProblemException(Status.CLIENT_ERROR_NOT_FOUND.getCode(), "Not found",
                    "Could not find token using CTS", null);
        }

        // Create a query for other tokens with this as a parent
        // TODO secondary key search via query

        // Delete the code
        try {
            response = accessor.delete(id, null);
        } catch (JsonResourceException e) {
            // TODO: logging
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not delete token from CTS: " + e.getMessage(), null);
        }

        // TODO check if delete can return null without exception?
    }

    @Override
    public AccessToken createAccessToken(String accessTokenType, Set<String> scope,
            AuthorizationCode code) {
        JsonValue response = null;

        String id = UUID.randomUUID().toString();
        // TODO expiry time cascading config
        long expireTime = System.currentTimeMillis() + ACCESS_TOKEN_LIFETIME;

        AccessTokenImpl accessToken = new AccessTokenImpl(id, scope, expireTime, code);
        // TODO decide where the scope in the access token is checked against
        // the authorization code

        // Create in CTS
        JsonResourceAccessor accessor =
                new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());
        try {
            response = accessor.create(id, accessToken);
        } catch (JsonResourceException e) {
            // TODO: logging
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not create token in CTS: " + e.getMessage(), null);
        }

        if (response == null) {
            throw new OAuthProblemException(Status.CLIENT_ERROR_NOT_FOUND.getCode(), "Not found",
                    "Could not create token in CTS", null);
        }

        return accessToken;
    }

    @Override
    public AccessToken createAccessToken(String accessTokenType, Set<String> scope,
            RefreshToken refreshToken) {
        JsonValue response = null;

        String id = UUID.randomUUID().toString();
        // TODO expiry time cascading config
        long expireTime = System.currentTimeMillis() + ACCESS_TOKEN_LIFETIME;

        AccessTokenImpl accessToken = new AccessTokenImpl(id, scope, expireTime, refreshToken);
        // TODO find out where the scope in the access token is checked against
        // the authorization
        // code

        // Create in CTS
        JsonResourceAccessor accessor =
                new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());
        try {
            response = accessor.create(id, accessToken);
        } catch (JsonResourceException e) {
            // TODO: logging
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not create token in CTS: " + e.getMessage(), null);
        }

        if (response == null) {
            throw new OAuthProblemException(Status.CLIENT_ERROR_NOT_FOUND.getCode(), "Not found",
                    "Could not create token in CTS", null);
        }

        return accessToken;
    }

    @Override
    public AccessToken createAccessToken(String accessTokenType, Set<String> scope, String realm,
            String uuid) {
        JsonValue response = null;

        String id = UUID.randomUUID().toString();
        // TODO expiry time cascading config
        long expireTime = System.currentTimeMillis() + ACCESS_TOKEN_LIFETIME;

        AccessTokenImpl accessToken =
                new AccessTokenImpl(id, null, uuid, null, realm, scope, expireTime);

        // Create in CTS
        JsonResourceAccessor accessor =
                new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());
        try {
            response = accessor.create(id, accessToken);
        } catch (JsonResourceException e) {
            // TODO: logging
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not create token in CTS: " + e.getMessage(), null);
        }

        if (response == null) {
            throw new OAuthProblemException(Status.CLIENT_ERROR_NOT_FOUND.getCode(), "Not found",
                    "Could not create token in CTS", null);
        }

        return accessToken;
    }

    @Override
    public AccessToken createAccessToken(String accessTokenType, Set<String> scope, String realm,
            String uuid, SessionClient client) {
        JsonValue response = null;

        String id = UUID.randomUUID().toString();
        // TODO expiry time cascading config
        long expireTime = System.currentTimeMillis() + ACCESS_TOKEN_LIFETIME;

        AccessTokenImpl accessToken =
                new AccessTokenImpl(id, null, uuid, client, realm, scope, expireTime);
        // TODO should scope be checked against client settings?

        // Create in CTS
        JsonResourceAccessor accessor =
                new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());
        try {
            response = accessor.create(id, accessToken);
        } catch (JsonResourceException e) {
            // TODO: logging
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not create token in CTS: " + e.getMessage(), null);
        }

        if (response == null) {
            throw new OAuthProblemException(Status.CLIENT_ERROR_NOT_FOUND.getCode(), "Not found",
                    "Could not create token in CTS", null);
        }

        return accessToken;
    }

    @Override
    public AccessToken createAccessToken(String accessTokenType, Set<String> scope, String realm,
            String uuid, String clientId) {
        JsonValue response = null;

        String id = UUID.randomUUID().toString();
        // TODO expiry time cascading config
        long expireTime = System.currentTimeMillis() + ACCESS_TOKEN_LIFETIME;

        AccessTokenImpl accessToken =
                new AccessTokenImpl(id, null, uuid, new SessionClientImpl(clientId, null), realm,
                        scope, expireTime);
        // TODO should scope be checked against client settings?

        // Create in CTS
        JsonResourceAccessor accessor =
                new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());
        try {
            response = accessor.create(id, accessToken);
        } catch (JsonResourceException e) {
            // TODO: logging
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not create token in CTS: " + e.getMessage(), null);
        }

        if (response == null) {
            throw new OAuthProblemException(Status.CLIENT_ERROR_NOT_FOUND.getCode(), "Not found",
                    "Could not create token in CTS", null);
        }

        return accessToken;
    }

    @Override
    public AccessToken readAccessToken(String id) {
        JsonValue response = null;

        // Create in CTS
        JsonResourceAccessor accessor =
                new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());
        try {
            response = accessor.read(id);
        } catch (JsonResourceException e) {
            // TODO: logging
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not read token in CTS: " + e.getMessage(), null);
        }

        if (response == null) {
            throw new OAuthProblemException(Status.CLIENT_ERROR_NOT_FOUND.getCode(), "Not found",
                    "Could not read token in CTS", null);
        }

        // TODO use _id rather than id
        AccessToken accessToken = new AccessTokenImpl(id, response);
        return accessToken;
    }

    @Override
    public void deleteAccessToken(String id) {
        JsonValue response = null;

        // Delete the code
        JsonResourceAccessor accessor =
                new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());
        try {
            response = accessor.delete(id, null);
        } catch (JsonResourceException e) {
            // TODO: logging
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not delete token from CTS: " + e.getMessage(), null);
        }

        // TODO check if delete can return null without exception?
    }

    @Override
    public RefreshToken createRefreshToken(Set<String> scope, String realm, String uuid,
            String clientId) {
        JsonValue response = null;

        String id = UUID.randomUUID().toString();
        // TODO expiry time cascading config
        long expireTime = System.currentTimeMillis() + REFRESH_TOKEN_LIFETIME;

        RefreshTokenImpl refreshToken =
                new RefreshTokenImpl(id, null, uuid, null, realm, scope, expireTime);

        // Create in CTS
        JsonResourceAccessor accessor =
                new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());
        try {
            response = accessor.create(id, refreshToken);
        } catch (JsonResourceException e) {
            // TODO: logging
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not create token in CTS: " + e.getMessage(), null);
        }

        if (response == null) {
            throw new OAuthProblemException(Status.CLIENT_ERROR_NOT_FOUND.getCode(), "Not found",
                    "Could not create token in CTS", null);
        }

        return refreshToken;
    }

    @Override
    public RefreshToken readRefreshToken(String id) {
        JsonValue response = null;

        // Read from CTS
        JsonResourceAccessor accessor =
                new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());
        try {
            response = accessor.read(id);
        } catch (JsonResourceException e) {
            // TODO: logging
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not read token from CTS: " + e.getMessage(), null);
        }

        if (response == null) {
            throw new OAuthProblemException(Status.CLIENT_ERROR_NOT_FOUND.getCode(), "Not found",
                    "Could not find token from CTS", null);
        }

        // Construct a RefreshToken object and return it
        // TODO use _id instead of id?
        RefreshToken rt = new RefreshTokenImpl(id, response.get("value"));
        return rt;
    }

    @Override
    public void deleteRefreshToken(String id) {
        JsonValue response = null;

        // Delete the code
        JsonResourceAccessor accessor =
                new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());
        try {
            response = accessor.delete(id, null);
        } catch (JsonResourceException e) {
            // TODO: logging
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not delete token from CTS: " + e.getMessage(), null);
        }

        // TODO check if delete can return null without exception?
    }

}
