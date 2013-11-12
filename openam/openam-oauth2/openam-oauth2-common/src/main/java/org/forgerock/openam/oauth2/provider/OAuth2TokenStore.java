/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock AS All rights reserved.
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

package org.forgerock.openam.oauth2.provider;

import java.security.PrivateKey;
import java.util.Set;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.*;

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
    public CoreToken createAuthorizationCode(Set<String> scopes, String realm, String uuid,
            SessionClient client, String nonce) throws OAuthProblemException;

    /**
     * Updates an authorization code.
     *
     * @param code the code to update
     * @return Generated authorization code
     * @throws OAuthProblemException
     *
     */
    public void updateAuthorizationCode(String id, CoreToken code) throws OAuthProblemException;

    /**
     * Retrieves an authorization code from store.
     * 
     * @param id
     *            the id of the authorization code to find
     * @return an existing authorization code, or null if not found
     * @throws OAuthProblemException
     * 
     */
    public CoreToken readAuthorizationCode(String id) throws OAuthProblemException;

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
    public void deleteAuthorizationCode(String id) throws OAuthProblemException;

    /**
     * Creates and stores a access token.
     * @param accessTokenType
     * @param scopes
     * @param realm
     * @param uuid
     * @param clientID
     * @param redirectURI
     * @param parent
     * @param refreshToken
     * @return
     */
    public CoreToken createAccessToken(String accessTokenType, Set<String> scopes, String realm, String uuid,
                                String clientID, String redirectURI, String parent, String refreshToken) throws OAuthProblemException;

    /**
     * Retrieves an access token from store.
     * 
     * @param id
     *            the unique identifier of the token
     * @return the access token object, or null if it does not exist
     * @throws OAuthProblemException
     * 
     */
    public CoreToken readAccessToken(String id) throws OAuthProblemException;

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
    public void deleteAccessToken(String id) throws OAuthProblemException;

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
    public CoreToken createRefreshToken(Set<String> scopes, String realm, String uuid, String clientId, String redirect_uri)
            throws OAuthProblemException;

    /**
     * Retrieves a refresh token from store.
     * 
     * @param id
     *            the unique identifier of the token
     * @return the refresh token object, or null if it does not exist
     * @throws OAuthProblemException
     * 
     */
    public CoreToken readRefreshToken(String id) throws OAuthProblemException;

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
    public void deleteRefreshToken(String id) throws OAuthProblemException;

    public JsonValue queryForToken(String id) throws OAuthProblemException;

    /**
     * Creates a  JWT token
     * @param realm realm of the token
     * @param uuid  the subject of the token
     * @param clientID the audience of the token
     * @param authorizationParty the client allowed to use the token as an access token
     * @param nonce The nonce passed in from the request
     * @param ops The ssotoken id used to create the JWT
     * @return
     */
    public CoreToken createJWT(String realm, String uuid, String clientID, String authorizationParty, String nonce, String ops);

}
