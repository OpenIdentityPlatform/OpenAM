/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock AS. All rights reserved.
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
import com.sun.identity.sm.ldap.exceptions.CoreTokenException;
import com.sun.identity.sm.ldap.exceptions.DeleteFailedException;
import org.forgerock.json.fluent.JsonValue;
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

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of the OAuthTokenStore interface that uses the
 * CoreTokenService for storing the tokens as JSON objects.
 */
@Singleton
public class DefaultOAuthTokenStoreImpl implements OAuth2TokenStore {

    //lifetimes are in seconds
    private long AUTHZ_CODE_LIFETIME = 1;
    private long REFRESH_TOKEN_LIFETIME = 1;
    private long ACCESS_TOKEN_LIFETIME = 1;
    private long JWT_TOKEN_LIFETIME = 1;

    private final OAuthTokenStore oAuthTokenStore;

    private OAuth2ProviderSettings settings = null;

    /**
     * Constructor, creates the repository instance used.
     *
     * @param oAuthTokenStore An instance of the OAuthTokenStore.
     */
    @Inject
    public DefaultOAuthTokenStoreImpl(OAuthTokenStore oAuthTokenStore) {
        this.oAuthTokenStore = oAuthTokenStore;
    }

    void getSettings(){
        settings = OAuth2Utils.getSettingsProvider(Request.getCurrent());
        AUTHZ_CODE_LIFETIME = settings.getAuthorizationCodeLifetime();
        REFRESH_TOKEN_LIFETIME = settings.getRefreshTokenLifetime();
        ACCESS_TOKEN_LIFETIME = settings.getAccessTokenLifetime();
        JWT_TOKEN_LIFETIME = settings.getJWTTokenLifetime();
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

        final BearerToken code = new BearerToken(id, uuid, client, realm, scope, expiresIn, "false");

        // Store in CTS
        try {
            oAuthTokenStore.create(code);
        } catch (CoreTokenException e) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to create authorization code "
                    + ((code != null) ? code.getTokenInfo() : null), e);
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

        BearerToken code2 = new BearerToken(id, code);

        // Store in CTS
        try {
            oAuthTokenStore.create(code2);
        } catch (CoreTokenException e) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to create authorization code "
                    + ((code2 != null) ? code2.getTokenInfo() : null), e);
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
        JsonValue oAuthToken;

        // Read from CTS
        try {
            oAuthToken = oAuthTokenStore.read(id);
        } catch (CoreTokenException e) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to read authorization code corresponding to id: " + id, e);
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not read token from CTS: " + e.getMessage(), null);
        }

        if (oAuthToken == null) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to read authorization code corresponding to id: " + id);
            throw new OAuthProblemException(Status.CLIENT_ERROR_NOT_FOUND.getCode(), "Not found",
                    "Could not find token from CTS", null);
        }

        CoreToken ac = new CoreToken(id, oAuthToken);
        return ac;
    }

    /**
     * {@inheritDoc}
     */
    public void deleteAuthorizationCode(String id) {
        if (OAuth2Utils.DEBUG.messageEnabled()){
            OAuth2Utils.DEBUG.message("DefaultOAuthTokenStoreImpl::Deleting Authorization code: " + id);
        }
        JsonValue oAuthToken;

        // Read from CTS
        try {
            oAuthToken = oAuthTokenStore.read(id);
        } catch (CoreTokenException e) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to read authorization code corresponding to id: " + id, e);
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not read token from CTS: " + e.getMessage(), null);
        }

        if (oAuthToken == null) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to read authorization code corresponding to id: " + id);
            throw new OAuthProblemException(Status.CLIENT_ERROR_NOT_FOUND.getCode(), "Not found",
                    "Could not find token using CTS", null);
        }

        // Delete the code
        try {
            oAuthTokenStore.delete(id);
        } catch (DeleteFailedException e) {
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
        try {
            oAuthTokenStore.create(accessToken);
        } catch (CoreTokenException e) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to create access token: "
                    + ((accessToken != null) ? accessToken.getTokenInfo() : null), e);
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not create token in CTS: " + e.getMessage(), null);
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
        JsonValue oAuthToken;

        // Read from CTS
        try {
            oAuthToken = oAuthTokenStore.read(id);
        } catch (CoreTokenException e) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to read access token corresponding to id: " + id, e);
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not read token in CTS: " + e.getMessage(), null);
        }

        if (oAuthToken == null) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to read access token corresponding to id: " + id);
            throw new OAuthProblemException(Status.CLIENT_ERROR_NOT_FOUND.getCode(), "Not found",
                    "Could not read token in CTS", null);
        }

        BearerToken accessToken = new BearerToken(id, oAuthToken);
        return accessToken;
    }

    /**
     * {@inheritDoc}
     */
    public void deleteAccessToken(String id) {
        if (OAuth2Utils.DEBUG.messageEnabled()){
            OAuth2Utils.DEBUG.message("DefaultOAuthTokenStoreImpl::Deleting access token");
        }

        // Delete the code
        try {
            oAuthTokenStore.delete(id);
        } catch (DeleteFailedException e) {
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

        String id = UUID.randomUUID().toString();
        long expireTime = REFRESH_TOKEN_LIFETIME;

        BearerToken refreshToken =
                    new BearerToken(id, null, uuid, new SessionClientImpl(clientId, redirectURI), realm, scopes,
                                    expireTime, OAuth2Constants.Token.OAUTH_REFRESH_TOKEN);

        // Create in CTS
        try {
            oAuthTokenStore.create(refreshToken);
        } catch (CoreTokenException e) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to create refresh token: " +
                    ((refreshToken != null) ? refreshToken.getTokenInfo() : null), e);
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not create token in CTS: " + e.getMessage(), null);
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
        JsonValue oAuthToken;

        // Read from CTS
        try {
            oAuthToken = oAuthTokenStore.read(id);
        } catch (CoreTokenException e) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to read refresh token corresponding to id: " + id, e);
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(Request.getCurrent());
        }

        if (oAuthToken == null) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to read refresh token corresponding to id: " + id);
            throw new OAuthProblemException(Status.CLIENT_ERROR_NOT_FOUND.getCode(), "Not found",
                    "Could not find token from CTS", null);
        }

        BearerToken rt = new BearerToken(id, oAuthToken);
        return rt;
    }

    /**
     * {@inheritDoc}
     */
    public void deleteRefreshToken(String id) {

        // Delete the code
        try {
            oAuthTokenStore.delete(id);
        } catch (DeleteFailedException e) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to delete refresh token corresponding to id: " + id, e);
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(Request.getCurrent());
        }
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue queryForToken(String id) throws OAuthProblemException{

        JsonValue results;

        //construct the filter
        Map<String, Object> query = new HashMap<String, Object>();
        query.put(OAuth2Constants.CoreTokenParams.PARENT, id);
        query.put(OAuth2Constants.CoreTokenParams.REFRESH_TOKEN, id);

        try {
            results = oAuthTokenStore.query(query);
        } catch (CoreTokenException e) {
            OAuth2Utils.DEBUG.error("DefaultOAuthTokenStoreImpl::Unable to query refresh token corresponding to id: " + id, e);
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(Request.getCurrent());
        }

        return results;
    }

    /**
     * @{@inheritDoc}
     */
    public CoreToken createJWT(String realm, String uuid, String clientID, String authorizationParty, String nonce, String ops){
        long timeInSeconds = System.currentTimeMillis()/1000;
        getSettings();
        return new JWTToken(OAuth2Utils.getDeploymentURL(Request.getCurrent()), uuid, clientID,
                authorizationParty, timeInSeconds + JWT_TOKEN_LIFETIME, timeInSeconds, timeInSeconds, realm, nonce, ops);
    }
}
