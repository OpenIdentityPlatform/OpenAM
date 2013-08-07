/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock Inc. All rights reserved.
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
 * "Portions copyright [year] [name of copyright owner]"
 */

package org.forgerock.openam.ext.cts.repo;

import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceAccessor;
import org.forgerock.json.resource.JsonResourceContext;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openam.ext.cts.CoreTokenService;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.BearerToken;
import org.forgerock.openam.oauth2.model.CoreToken;
import org.forgerock.openam.oauth2.model.JWTToken;
import org.forgerock.openam.oauth2.model.SessionClient;
import org.forgerock.openam.oauth2.model.SessionClientImpl;
import org.forgerock.openam.oauth2.provider.OAuth2ProviderSettings;
import org.forgerock.openam.oauth2.provider.OAuth2TokenStore;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.Request;
import org.restlet.data.Status;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of the OAuthTokenStore interface that uses the
 * CoreTokenService for storing the tokens as JSON objects.
 */
public class DefaultOAuthTokenStoreImpl implements OAuth2TokenStore {

    //lifetimes are in seconds
    private long AUTHZ_CODE_LIFETIME = 1;
    private long REFRESH_TOKEN_LIFETIME = 1;
    private long ACCESS_TOKEN_LIFETIME = 1;

    private JsonResource repository;
    private OAuth2ProviderSettings settings = null;

    /**
     * Constructor, creates the repository instance used.
     * 
     * @throws OAuthProblemException
     */
    public DefaultOAuthTokenStoreImpl() {
        try {
            repository = new CoreTokenService(OpenDJTokenRepo.getInstance());
        } catch (Exception e) {
            throw new OAuthProblemException(Status.SERVER_ERROR_SERVICE_UNAVAILABLE.getCode(),
                    "Service unavailable", "Could not create underlying storage. Exception: " + e, null);
        }

    }
    private void getSettings(){
        settings = OAuth2Utils.getSettingsProvider(Request.getCurrent());
        AUTHZ_CODE_LIFETIME = settings.getAuthorizationCodeLifetime();
        REFRESH_TOKEN_LIFETIME = settings.getRefreshTokenLifetime();
        ACCESS_TOKEN_LIFETIME = settings.getAccessTokenLifetime();
    }

    /**
     * {@inheritDoc}
     */
    public CoreToken createAuthorizationCode(Set<String> scope, String realm, String uuid,
            SessionClient client) {
        if (OAuth2Utils.DEBUG.messageEnabled()){
            OAuth2Utils.DEBUG.message("DefaultOAuthTokenStoreImpl::Creating Authorization code");
        }
        getSettings();
        String id = UUID.randomUUID().toString();
        long expiresIn = AUTHZ_CODE_LIFETIME;

        BearerToken code =
                new BearerToken(id, uuid, client,
                        realm, scope, expiresIn, "false");
        JsonValue response = null;

        // Store in CTS
        JsonResourceAccessor accessor =
                new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());
        try {
            response = accessor.create(id, code);
        } catch (JsonResourceException e) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to create authorization code "
                    + ((code != null) ? code.getTokenInfo() : null), e);
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not create token in CTS", null);
        }

        if (response == null) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to create authorization code "
                    + ((code != null) ? code.getTokenInfo() : null));
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not create token in CTS", null);
        }

        return code;
    }

    /**
     * {@inheritDoc}
     */
    public void updateAuthorizationCode(String id, CoreToken code) throws OAuthProblemException{
        deleteAuthorizationCode(id);
        JsonValue response = null;

        // Store in CTS
        JsonResourceAccessor accessor =
                new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());

        BearerToken code2 = new BearerToken(id, code);
        try {
            response = accessor.create(id, code2);
        } catch (JsonResourceException e) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to create authorization code "
                    + ((code2 != null) ? code2.getTokenInfo() : null), e);
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not create token in CTS", null);
        }

        if (response == null) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to create authorization code "
                    + ((code2 != null) ? code2.getTokenInfo() : null));
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not create token in CTS", null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public CoreToken readAuthorizationCode(String id) {
        if (OAuth2Utils.DEBUG.messageEnabled()){
            OAuth2Utils.DEBUG.message("DefaultOAuthTokenStoreImpl::Reading Authorization code: " + id);
        }
        JsonValue response = null;

        // Read from CTS
        JsonResourceAccessor accessor =
                new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());
        try {
            response = accessor.read(id);
        } catch (JsonResourceException e) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to read authorization code corresponding to id: " + id, e);
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not read token from CTS: " + e.getMessage(), null);
        }

        if (response == null) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to read authorization code corresponding to id: " + id);
            throw new OAuthProblemException(Status.CLIENT_ERROR_NOT_FOUND.getCode(), "Not found",
                    "Could not find token from CTS", null);
        }

        CoreToken ac = new CoreToken(id, response);
        return ac;
    }

    /**
     * {@inheritDoc}
     */
    public void deleteAuthorizationCode(String id) {
        if (OAuth2Utils.DEBUG.messageEnabled()){
            OAuth2Utils.DEBUG.message("DefaultOAuthTokenStoreImpl::Deleting Authorization code: " + id);
        }
        JsonValue response = null;

        // Read from CTS
        JsonResourceAccessor accessor =
                new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());

        try {
            response = accessor.read(id);
        } catch (JsonResourceException e) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to read authorization code corresponding to id: " + id, e);
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not read token from CTS: " + e.getMessage(), null);
        }

        if (response == null) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to read authorization code corresponding to id: " + id);
            throw new OAuthProblemException(Status.CLIENT_ERROR_NOT_FOUND.getCode(), "Not found",
                    "Could not find token using CTS", null);
        }

        // Delete the code
        try {
            response = accessor.delete(id, null);
        } catch (JsonResourceException e) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to delete authorization code corresponding to id: " + id, e);
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not delete token from CTS: " + e.getMessage(), null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public CoreToken createAccessToken(String accessTokenType, Set<String> scopes, String realm, String uuid,
                                String clientID, String redirectURI, String parent, String refreshToken)
            throws OAuthProblemException{
        if (OAuth2Utils.DEBUG.messageEnabled()){
            OAuth2Utils.DEBUG.message("DefaultOAuthTokenStoreImpl::Creating access token");
        }
        getSettings();
        JsonValue response = null;

        String id = UUID.randomUUID().toString();
        long expireTime = ACCESS_TOKEN_LIFETIME;
        BearerToken accessToken;

        if (refreshToken == null || refreshToken.isEmpty()){
            accessToken =
                    new BearerToken(id, parent, uuid, new SessionClientImpl(clientID, redirectURI),
                            realm, scopes, expireTime, OAuth2Constants.Token.OAUTH_ACCESS_TOKEN);
        } else {
            accessToken =
                    new BearerToken(id, parent, uuid, new SessionClientImpl(clientID, redirectURI),
                            realm, scopes, expireTime, refreshToken, OAuth2Constants.Token.OAUTH_ACCESS_TOKEN);
        }

        // Create in CTS
        JsonResourceAccessor accessor =
                new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());
        try {
            response = accessor.create(id, accessToken);
        } catch (JsonResourceException e) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to create access token: "
                    + ((accessToken != null) ? accessToken.getTokenInfo() : null), e);
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not create token in CTS: " + e.getMessage(), null);
        }

        if (response == null) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to create access token: "
                    + ((accessToken != null) ? accessToken.getTokenInfo() : null) );
            throw new OAuthProblemException(Status.CLIENT_ERROR_NOT_FOUND.getCode(), "Not found",
                    "Could not create token in CTS", null);
        }

        return accessToken;
    }

    /**
     * {@inheritDoc}
     */
    public CoreToken readAccessToken(String id) {
        if (OAuth2Utils.DEBUG.messageEnabled()){
            OAuth2Utils.DEBUG.message("DefaultOAuthTokenStoreImpl::Reading access token");
        }
        JsonValue response = null;

        // Create in CTS
        JsonResourceAccessor accessor =
                new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());
        try {
            response = accessor.read(id);
        } catch (JsonResourceException e) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to read access token corresponding to id: " + id, e);
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not read token in CTS: " + e.getMessage(), null);
        }

        if (response == null) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to read access token corresponding to id: " + id);
            throw new OAuthProblemException(Status.CLIENT_ERROR_NOT_FOUND.getCode(), "Not found",
                    "Could not read token in CTS", null);
        }

        BearerToken accessToken = new BearerToken(id, response);
        return accessToken;
    }

    /**
     * {@inheritDoc}
     */
    public void deleteAccessToken(String id) {
        if (OAuth2Utils.DEBUG.messageEnabled()){
            OAuth2Utils.DEBUG.message("DefaultOAuthTokenStoreImpl::Deleting access token");
        }
        JsonValue response = null;

        // Delete the code
        JsonResourceAccessor accessor =
                new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());
        try {
            response = accessor.delete(id, null);
        } catch (JsonResourceException e) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to delete access token corresponding to id: " + id, e);
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not delete token from CTS: " + e.getMessage(), null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public CoreToken createRefreshToken(Set<String> scopes, String realm, String uuid, String clientId, String redirectURI)
            throws OAuthProblemException{
        if (OAuth2Utils.DEBUG.messageEnabled()){
            OAuth2Utils.DEBUG.message("DefaultOAuthTokenStoreImpl::Create refresh token");
        }
        getSettings();
        JsonValue response;

        String id = UUID.randomUUID().toString();
        long expireTime = REFRESH_TOKEN_LIFETIME;

        BearerToken refreshToken =
                    new BearerToken(id, null, uuid, new SessionClientImpl(clientId, redirectURI), realm, scopes,
                                    expireTime, OAuth2Constants.Token.OAUTH_REFRESH_TOKEN);

        // Create in CTS
        JsonResourceAccessor accessor =
                new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());
        try {
            response = accessor.create(id, refreshToken);
        } catch (JsonResourceException e) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to create refresh token: " +
                    ((refreshToken != null) ? refreshToken.getTokenInfo() : null), e);
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not create token in CTS: " + e.getMessage(), null);
        }

        if (response == null) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to create refresh token: " +
                    ((refreshToken != null) ? refreshToken.getTokenInfo() : null));
            throw new OAuthProblemException(Status.CLIENT_ERROR_NOT_FOUND.getCode(), "Not found",
                    "Could not create token in CTS", null);
        }

        return refreshToken;
    }

    /**
     * {@inheritDoc}
     */
    public CoreToken readRefreshToken(String id) {
        if (OAuth2Utils.DEBUG.messageEnabled()){
            OAuth2Utils.DEBUG.message("DefaultOAuthTokenStoreImpl::Read refresh token");
        }
        JsonValue response = null;

        // Read from CTS
        JsonResourceAccessor accessor =
                new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());
        try {
            response = accessor.read(id);
        } catch (JsonResourceException e) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to read refresh token corresponding to id: " + id, e);
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(Request.getCurrent());
        }

        if (response == null) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to read refresh token corresponding to id: " + id);
            throw new OAuthProblemException(Status.CLIENT_ERROR_NOT_FOUND.getCode(), "Not found",
                    "Could not find token from CTS", null);
        }

        BearerToken rt = new BearerToken(id, response);
        return rt;
    }

    /**
     * {@inheritDoc}
     */
    public void deleteRefreshToken(String id) {
        JsonValue response = null;

        // Delete the code
        JsonResourceAccessor accessor =
                new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());
        try {
            response = accessor.delete(id, null);
        } catch (JsonResourceException e) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to delete refresh token corresponding to id: " + id, e);
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(Request.getCurrent());
        }

    }

    /**
     * {@inheritDoc}
     */
    public JsonValue queryForToken(String id) throws OAuthProblemException{
        JsonValue response = null;

        // Delete the code
        JsonResourceAccessor accessor =
                new JsonResourceAccessor(repository, JsonResourceContext.newRootContext());

        //construct the filter
        Map query = new HashMap<String,String>();
        query.put(OAuth2Constants.CoreTokenParams.PARENT, id);
        query.put(OAuth2Constants.CoreTokenParams.REFRESH_TOKEN, id);
        JsonValue queryFilter = new JsonValue(new HashMap<String, HashMap<String, String>>());
        if (query != null){
            queryFilter.put("filter", query);
        }

        try {
            response = accessor.query(id, queryFilter);
        } catch (JsonResourceException e) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to query refresh token corresponding to id: " + id, e);
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(Request.getCurrent());
        }

        return response;
    }

    /**
     * @{@inheritDoc}
     */
    public CoreToken createJWT(String realm, String uuid, String clientID, String authorizationParty, String nonce){
        long timeInSeconds = System.currentTimeMillis()/1000;
        getSettings();
        return new JWTToken(OAuth2Utils.getDeploymentURL(Request.getCurrent()), uuid, clientID,
                authorizationParty, timeInSeconds + ACCESS_TOKEN_LIFETIME, timeInSeconds, timeInSeconds, realm, nonce);
    }

}
