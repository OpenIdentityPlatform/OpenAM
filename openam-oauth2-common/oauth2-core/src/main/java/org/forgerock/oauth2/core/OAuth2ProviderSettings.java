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

package org.forgerock.oauth2.core;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;

import java.security.KeyPair;
import java.util.Map;
import java.util.Set;

/**
 * Models all of the possible settings the OAuth2 provider can have and that can be configured.
 * <br/>
 * The actual implementation is responsible for providing the method in which these settings are configured. This
 * interface only describes the API for other code to get the OAuth2 provider settings.
 *
 * @since 12.0.0
 */
public interface OAuth2ProviderSettings {

    /**
     * Gets the response types allowed by the OAuth2 provider.
     *
     * @return The allowed response types and their handler implementations.
     * @throws UnsupportedResponseTypeException If the requested response type is not supported by either the client
     *          or the OAuth2 provider.
     * @throws ServerException If any internal server error occurs.
     */
    Map<String, ResponseTypeHandler> getAllowedResponseTypes() throws UnsupportedResponseTypeException, ServerException;

    /**
     * Determines whether a resource owner's consent has been saved from a previous authorize request.
     *
     * @param resourceOwner The resource owner.
     * @param clientId The if of the client making the request.
     * @param scope The requested scope.
     * @return {@code true} if the resource owner has previously requested that consent should be saved from the
     *          specified client and the exact scope.
     */
    boolean isConsentSaved(ResourceOwner resourceOwner, String clientId, Set<String> scope);

    /**
     * Provided as an extension point to allow the OAuth2 provider to customise the scope requested when authorization
     * is requested.
     *
     * @param clientRegistration The client registration.
     * @param scope The requested scope.
     * @return The updated scope used in the remaining OAuth2 process.
     * @throws ServerException If any internal server error occurs.
     */
    Set<String> validateAuthorizationScope(ClientRegistration clientRegistration, Set<String> scope,
            OAuth2Request request) throws ServerException, InvalidScopeException;

    /**
     * Provided as an extension point to allow the OAuth2 provider to customise the scope requested when an access token
     * is requested.
     *
     * @param clientRegistration The client registration.
     * @param scope The requested scope.
     * @param request The OAuth2 request.
     * @return The updated scope used in the remaining OAuth2 process.
     * @throws ServerException If any internal server error occurs.
     */
    Set<String> validateAccessTokenScope(ClientRegistration clientRegistration, Set<String> scope,
            OAuth2Request request) throws ServerException, InvalidScopeException;

    /**
     * Provided as an extension point to allow the OAuth2 provider to customise the scope requested when a refresh token
     * is requested.
     *
     * @param clientRegistration The client registration.
     * @param requestedScope The requested scope.
     * @param tokenScope The scope from the access token.
     * @param request The OAuth2 request.
     * @return The updated scope used in the remaining OAuth2 process.
     * @throws ServerException If any internal server error occurs.
     */
    Set<String> validateRefreshTokenScope(ClientRegistration clientRegistration, Set<String> requestedScope,
            Set<String> tokenScope, OAuth2Request request) throws ServerException, InvalidScopeException;

    /**
     * Gets the resource owners information based on an issued access token.
     *
     * @param token The access token.
     * @param request The OAuth2 request.
     * @return A {@code Map<String, Object>} of the resource owner's information.
     * @throws ServerException If any internal server error occurs.
     * @throws UnauthorizedClientException If the client's authorization fails.
     */
    Map<String, Object> getUserInfo(AccessToken token, OAuth2Request request) throws ServerException,
            UnauthorizedClientException;

    /**
     * Gets the specified access token's information.
     *
     * @param accessToken The access token.
     * @return A {@code Map<String, Object>} of the access token's information.
     * @throws ServerException If any internal server error occurs.
     */
    Map<String, Object> evaluateScope(AccessToken accessToken) throws ServerException;

    /**
     * Provided as an extension point to allow the OAuth2 provider to return additional data from an authorization
     * request.
     *
     * @param tokens The tokens that will be returned from the authorization call.
     * @param request The OAuth2 request.
     * @return A {@code Map<String, String>} of the additional data to return.
     * @throws ServerException If any internal server error occurs.
     */
    Map<String, String> additionalDataToReturnFromAuthorizeEndpoint(Map<String, Token> tokens, OAuth2Request request)
            throws ServerException;

    /**
     * Provided as an extension point to allow the OAuth2 provider to return additional data from an access token
     * request.
     * <br/>
     * Any additional data to be returned should be added to the access token by invoking,
     * AccessToken#addExtraData(String, String).
     *
     * @param accessToken The access token.
     * @param request The OAuth2 request.
     * @throws ServerException If any internal server error occurs.
     * @throws InvalidClientException If either the request does not contain the client's id or the client fails to be
     *          authenticated.
     */
    void additionalDataToReturnFromTokenEndpoint(AccessToken accessToken, OAuth2Request request) throws ServerException,
            InvalidClientException;

    /**
     * Saves the resource owner's consent for the granting authorization for the specified client with the specified
     * scope.
     *
     * @param resourceOwner The resource owner.
     * @param clientId The client id.
     * @param scope The requested scope.
     */
    void saveConsent(ResourceOwner resourceOwner, String clientId, Set<String> scope);

    /**
     * Whether the OAuth2 provider should issue refresh tokens when issuing access tokens.
     *
     * @return {@code true} if refresh tokens should be issued.
     * @throws ServerException If any internal server error occurs.
     */
    boolean issueRefreshTokens() throws ServerException;

    /**
     * Whether the OAuth2 provider should issue refresh tokens when refreshing access tokens.
     *
     * @return {@code true} if refresh tokens should be issued when access tokens are refreshed.
     * @throws ServerException If any internal server error occurs.
     */
    boolean issueRefreshTokensOnRefreshingToken() throws ServerException;

    /**
     * Gets the lifetime an authorization code will have before it expires.
     *
     * @return The lifetime of an authorization code in seconds.
     * @throws ServerException If any internal server error occurs.
     */
    long getAuthorizationCodeLifetime() throws ServerException;

    /**
     * Gets the lifetime an access token will have before it expires.
     *
     * @return The lifetime of an access token in seconds.
     * @throws ServerException If any internal server error occurs.
     */
    long getAccessTokenLifetime() throws ServerException;

    /**
     * Gets the lifetime an OpenID token will have before it expires.
     *
     * @return The lifetime of an OpenID token in seconds.
     * @throws ServerException If any internal server error occurs.
     */
    long getOpenIdTokenLifetime() throws ServerException;

    /**
     * Gets the lifetime an refresh token will have before it expires.
     *
     * @return The lifetime of an refresh token in seconds.
     * @throws ServerException If any internal server error occurs.
     */
    long getRefreshTokenLifetime() throws ServerException;

    /**
     * Gets the key pair of the OAuth2 provider.
     *
     * @return The KeyPair.
     * @throws ServerException If any internal server error occurs.
     */
    KeyPair getServerKeyPair() throws ServerException;

    /**
     * Gets the attributes of the resource owner that are used for authenticating resource owners.
     *
     * @return A {@code Set} of resource owner attributes.
     * @throws ServerException If any internal server error occurs.
     */
    Set<String> getResourceOwnerAuthenticatedAttributes() throws ServerException;

    /**
     * Gets the OpenID connect claims with the OAuth2 provider supports.
     *
     * @return A {@code Set} of the supported claims.
     * @throws ServerException If any internal server error occurs.
     */
    Set<String> getSupportedClaims() throws ServerException;

    /**
     * Gets the algorithms that the OAuth2 provider supports for signing OpenID tokens.
     *
     * @return A {@code Set} of the supported algorithms.
     * @throws ServerException If any internal server error occurs.
     */
    Set<String> getSupportedIDTokenSigningAlgorithms() throws ServerException;

    /**
     * Gets the supported version of the OpenID Connect specfication.
     *
     * @return The OpenID Connect version.
     */
    String getOpenIDConnectVersion();

    /**
     * Gets the URI of this OpenID Connect issuer.
     *
     * @return The OpenID Connect issuer.
     */
    String getOpenIDConnectIssuer();

    /**
     * Gets the URI for the OAuth2 authorize endpoint.
     *
     * @return The OAuth2 authorize endpoint.
     */
    String getAuthorizationEndpoint();

    /**
     * Gets the URI for the OAuth2 token endpoint.
     *
     * @return The OAuth2 token endpoint.
     */
    String getTokenEndpoint();

    /**
     * Gets the URI for the OpenID Connect user info endpoint.
     *
     * @return The OpenID Connect user info endpoint.
     */
    String getUserInfoEndpoint();

    /**
     * Gets the URI for the OpenID Connect check session endpoint.
     *
     * @return The OpenID Connect check session endpoint.
     */
    String getCheckSessionEndpoint();

    /**
     * Gets the URI for the OpenID Connect end session endpoint.
     *
     * @return The OpenID Connect end session endpoint.
     */
    String getEndSessionEndpoint();

    /**
     * Gets the JSON Web Key Set URI.
     *
     * @return The JWKS URI.
     * @throws ServerException If any internal server error occurs.
     */
    String getJWKSUri() throws ServerException;

    /**
     * Gets the JWK Set for this OAuth2 Authorization /OpenID Provider.
     *
     * @return The JWK Set of signing and encryption keys.
     */
    JsonValue getJWKSet() throws ServerException;

    /**
     * Gets the created timestamp attribute name.
     *
     * @return The created attribute timestamp attribute name.
     */
    String getCreatedTimestampAttributeName() throws ServerException;

    /**
     * Gets the modified timestamp attribute name.
     *
     * @return The modified attribute timestamp attribute name.
     */
    String getModifiedTimestampAttributeName() throws ServerException;

    /**
     * Gets the OpenID Connect client registration endpoint.
     *
     * @return The OpenID Connect client registration endpoint.
     */
    String getClientRegistrationEndpoint();

    /**
     * Gets the subject types supported by the OAuth2 provider.
     *
     * @return A {@code Set} of supported subject types.
     * @throws ServerException If any internal server error occurs.
     */
    Set<String> getSupportedSubjectTypes() throws ServerException;

    /**
     * Indicates whether clients may register without providing an access token.
     *
     * @return true if allowed, otherwise false.
     * @throws ServerException If any internal server error occurs.
     */
    boolean isOpenDynamicClientRegistrationAllowed() throws ServerException;

    /**
     * Whether to generate access tokens for clients that register without one. Only enabled if
     * {@link #isOpenDynamicClientRegistrationAllowed()} is true.
     *
     * @return true if an access token should be generated for clients that register without one.
     * @throws ServerException If any internal server error occurs.
     */
    boolean isRegistrationAccessTokenGenerationEnabled() throws ServerException;

    /**
     * Returns a mapping from Authentication Context Class Reference (ACR) values (typically a Level of Assurance
     * value) to concrete authentication methods.
     */
    Map<String, AuthenticationMethod> getAcrMapping() throws ServerException;

    /**
     * The default Authentication Context Class Reference (ACR) values to use for authentication if none is specified
     * in the request. This is a space-separated list of values in preference order.
     */
    String getDefaultAcrValues() throws ServerException;

    /**
     * The mappings between amr values and auth module names.
     * @return
     */
    Map<String, String> getAMRAuthModuleMappings() throws ServerException;

}
