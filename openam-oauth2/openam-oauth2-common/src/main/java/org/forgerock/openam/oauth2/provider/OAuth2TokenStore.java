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
 * "Portions Copyrighted [2012] [ForgeRock Inc]"
 */
package org.forgerock.openam.oauth2.provider;

import java.util.Set;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.AccessToken;
import org.forgerock.openam.oauth2.model.AuthorizationCode;
import org.forgerock.openam.oauth2.model.RefreshToken;
import org.forgerock.openam.oauth2.model.SessionClient;

/**
 * Interface to govern the high level store interactions, applying configuration
 * and business logic to token lifetimes, deletion of token chains, and other
 * storage-related issues.
 *
 */
public interface OAuth2TokenStore {

    /**
     * Creates and stores an authorization code. The code is not marked as
     * having been used, and may be used once only in a call to
     * createAccessToken or createRefreshToken
     * 
     * @param scopes the scopes of the token
     * @param realm the realm of this token
     * @param uuid the user identifier (resource owner)
     * @param client the client of this token
     * @return Generated authorization code
     * @throws OAuthProblemException
     * 
     */
    AuthorizationCode createAuthorizationCode(Set<String> scopes, String realm, String uuid,
            SessionClient client) throws OAuthProblemException;

    /**
     * Updates an authorization code.
     *
     * @param code the code to update
     * @return Generated authorization code
     * @throws OAuthProblemException
     *
     */
    public void updateAuthorizationCode(String id, AuthorizationCode code) throws OAuthProblemException;

    /**
     * Retrieves an authorization code from store.
     * 
     * @param id
     *            the id of the authorization code to find
     * @return an existing authorization code, or null if not found
     * @throws OAuthProblemException
     * 
     */
    AuthorizationCode readAuthorizationCode(String id) throws OAuthProblemException;

    /**
     * Invalidates an authorization code, ensuring that it cannot be used to
     * issue any tokens. The implementation is responsible for deletion of
     * authorization code, and must make sure that the code is only deleted when
     * all tokens issued directly or indirectly using this code have also been
     * deleted.
     * 
     * @param id
     *            the id of the authorization code that should be deleted
     * @throws OAuthProblemException
     * 
     */
    void deleteAuthorizationCode(String id) throws OAuthProblemException;

    /**
     * Creates and stores an access token using an authorization code
     * (authentication code flow). The authorization code can only be used once,
     * and will be updated as a result of calling this method to indicate that
     * it has been used; attempts to reuse the same authorization code will
     * result in all related tokens being invalidated. The implementation may
     * issue a refresh token at the same time (referenced by the access token)
     * with the same parent. TODO: should the interface include a boolean
     * "createRefreshToken" or is this purely down to pre-config
     * 
     * @param accessTokenType
     *            MAC, Bearer or an extended access token type
     * @param scopes
     *            the scope(s) for which to issue the token, must be identical
     *            to or a subset of authz code scopes
     * @param code
     *            the authorization code that will be the parent of this access
     *            token
     * @param realm
     *            the realm of this token
     * @return a newly created and stored access token
     */
    AccessToken createAccessToken(String accessTokenType, Set<String> scopes, AuthorizationCode code, String realm);

    /**
     * Creates and stores an access token using a refresh token (refresh token
     * flow). The refresh token may be used multiple times and for different
     * scope subsets.
     * 
     * @param accessTokenType
     *            MAC, Bearer or an extended access token type
     * @param scopes
     *            the scope(s) for which to issue the token, must be identical
     *            to or a subset of authz code scopes
     * @param refreshToken
     *            the refresh token that will be the parent of this access token
     * @param realm
     *            the realm of this token
     * @return a newly created and stored access token
     */
    AccessToken createAccessToken(String accessTokenType, Set<String> scopes,
            RefreshToken refreshToken, String realm);

    /**
     * Creates and stores an access token using the implicit flow where no
     * client is identified. The resulting token will have no parent token.
     * 
     * @param accessTokenType
     *            MAC, Bearer or an extended access token type
     * @param scopes
     *            the scope(s) for which to issue the token, must be identical
     *            to or a subset of authz code scopes
     * @param realm
     *            the name of the realm where this token should be created
     * @param uuid
     *            the user identifier (resource owner)
     * @param client
     *            the client making the request
     * @return a newly created and stored access token
     */
    AccessToken createAccessToken(String accessTokenType, Set<String> scopes, String realm,
            String uuid, SessionClient client);

    /**
     * Creates and stores an access token using the resource owner password,
     * where the client passes the credentials to the OAuth endpoint. The
     * resulting token has no parent. There is no redirect UR in this case since
     * the token is sent directly as a response to a POST.
     * 
     * @param accessTokenType
     *            MAC, Bearer or an extended access token type
     * @param scopes
     *            the scope(s) for which to issue the token, must be identical
     *            to or a subset of authz code scopes
     * @param realm
     *            the name of the realm where this token should be created
     * @param uuid
     *            the user identifier (resource owner)
     * @param clientId
     *            the client making the request
     * @param refreshToken
     *            the optional refresh token. null if there is no refresh token
     * @return a newly created and stored access token
     */
    AccessToken createAccessToken(String accessTokenType, Set<String> scopes, String realm,
            String uuid, String clientId, RefreshToken refreshToken);

    /**
     * Creates and stores an access token using the client credential flow,
     * where the client only is authenticated and identified. The resulting
     * token will not be tied to the resource owner, and will have no parent
     * token. There is no redirect URI in this case since the client is the same
     * as the resource owner and redirection is not needed.
     * 
     * @param accessTokenType
     *            MAC, Bearer or an extended access token type
     * @param scopes
     *            the scope(s) for which to issue the token, must be identical
     *            to or a subset of authz code scopes
     * @param realm
     *            the name of the realm where this token should be created
     * @param clientId
     *            the client making the request
     * @return a newly created and stored access token
     */
    AccessToken createAccessToken(String accessTokenType, Set<String> scopes, String realm,
            String clientId) throws OAuthProblemException;

    /**
     * Retrieves an access token from store. TODO: exception subclasses
     * 
     * @param id
     *            the unique identifier of the token
     * @return the access token object, or null if it does not exist
     * @throws OAuthProblemException
     * 
     */
    AccessToken readAccessToken(String id) throws OAuthProblemException;

    /**
     * Deletes an access token, ensuring that it cannot be used to access any
     * further resources. This does not imply that any parent tokens
     * (authorization token or refresh token) should be deleted at the same
     * time.
     * 
     * @param id
     *            the id of the access token that should be deleted
     * @throws OAuthProblemException
     * 
     */
    void deleteAccessToken(String id) throws OAuthProblemException;

    /**
     * Creates and stores a refresh token in the resource owner password flow.
     * There is no parent token to the refresh token in this case.
     * 
     * @param scopes
     *            the scope(s) for which to issue the token, must be identical
     *            to or a subset of authz code scopes
     * @param realm
     *            the name of the realm where this token should be created
     * @param uuid
     *            the user identifier (resource owner)
     * @param clientId
     *            the client making the request
     * @return a newly created refresh token
     * @throws OAuthProblemException
     * 
     */
    RefreshToken createRefreshToken(Set<String> scopes, String realm, String uuid, String clientId, AuthorizationCode parent)
            throws OAuthProblemException;

    /**
     * Retrieves a refresh token from store. TODO: exception subclass
     * 
     * @param id
     *            the unique identifier of the token
     * @return the refresh token object, or null if it does not exist
     * @throws OAuthProblemException
     * 
     */
    RefreshToken readRefreshToken(String id) throws OAuthProblemException;

    /**
     * Deletes a refresh token, ensuring that it cannot be used to issue any
     * further access tokens and deleting any access tokens that have been
     * issued using this refresh token.
     * 
     * @param id
     *            the id of the refresh token that should be deleted
     * @throws OAuthProblemException
     * 
     */
    void deleteRefreshToken(String id) throws OAuthProblemException;

    public JsonValue queryForToken(String id) throws OAuthProblemException;

}
