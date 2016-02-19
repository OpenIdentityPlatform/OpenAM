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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.*;
import static org.forgerock.openam.utils.Time.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;

/**
 * Models a OAuth2 access token.
 *
 * @since 12.0.0
 */
public class AccessToken extends JsonValue implements IntrospectableToken, Token {

    private Map<String, Object> extraData = new HashMap<String, Object>();

    /**
     * Constructs a new AccessToken backed with the data in the specified JsonValue.
     *
     * @param token The JsonValue of the token.
     * @throws InvalidGrantException If the given token is not an Access Token.
     */
    public AccessToken(JsonValue token) throws InvalidGrantException {
        super(token);
        validateTokenName(getTokenName(), getTokenId());
    }

    /**
     * Constructs a new AccessToken backed with the data in the specified JsonValue.
     *
     * @param token The JsonValue of the token.
     * @param tokenName The token name.
     * @param tokenId The token identifier.
     * @throws InvalidGrantException If the given token is not an Access Token.
     */
    public AccessToken(JsonValue token, String tokenName, String tokenId) throws InvalidGrantException {
        super(token);
        validateTokenName(tokenName, tokenId);
    }

    /**
     * Constructs a new AccessToken.
     *
     * @param id The token id.
     * @param authorizationCode The authorization code.
     * @param resourceOwnerId The resource owner's id.
     * @param clientId The client's id.
     * @param redirectUri The redirect uri.
     * @param scope The scope.
     * @param expiryTime The expiry time.
     * @param refreshTokenId The refresh token id.
     * @param tokenName The token name.
     * @param grantType The grant type.
     * @param nonce The nonce.
     */
    public AccessToken(String id, String authorizationCode, String resourceOwnerId, String clientId, String redirectUri,
            Set<String> scope, long expiryTime, String refreshTokenId, String tokenName, String grantType,
            String nonce) {
        super(new HashMap<String, Object>());
        setId(id);
        setAuthorizationCode(authorizationCode);
        setResourceOwnerId(resourceOwnerId);
        setClientId(clientId);
        setRedirectUri(redirectUri);
        setScope(scope);
        setExpiryTime(expiryTime);
        if (!Utils.isEmpty(refreshTokenId)) {
            setRefreshTokenId(refreshTokenId);
        }
        setTokenType(OAuth2Constants.Bearer.BEARER);
        setTokenName(tokenName);
        setGrantType(grantType);
        setNonce(nonce);
    }

    /**
     * Sets the token id.
     *
     * @param id The token id.
     */
    protected void setId(String id) {
        put(ID, id);
    }

    /**
     * Sets the authorization code.
     *
     * @param authorizationCode The authorization code.
     */
    protected void setAuthorizationCode(String authorizationCode) {
        put(PARENT, authorizationCode);
    }

    /**
     * Sets the resource owner's id.
     *
     * @param resourceOwnerId The resource owner's id.
     */
    protected void setResourceOwnerId(String resourceOwnerId) {
        put(USERNAME, resourceOwnerId);
    }

    /**
     * Sets the client's id.
     *
     * @param clientId The client's id.
     */
    protected void setClientId(String clientId) {
        put(CLIENT_ID, clientId);
    }

    /**
     * Sets the redirect uri.
     *
     * @param redirectUri The redirect uri.
     */
    protected void setRedirectUri(String redirectUri) {
        put(REDIRECT_URI, redirectUri);
    }

    /**
     * Sets the scope.
     *
     * @param scope The scope.
     */
    protected void setScope(Set<String> scope) {
        put(SCOPE, scope);
    }

    /**
     * Sets the expiry time.
     *
     * @param expiryTime The expiry time.
     */
    protected void setExpiryTime(long expiryTime) {
        put(EXPIRE_TIME, expiryTime);
    }

    /**
     * Sets the refresh token id.
     *
     * @param refreshTokenId The refresh token id.
     */
    protected void setRefreshTokenId(String refreshTokenId) {
        put(REFRESH_TOKEN, refreshTokenId);
    }

    /**
     * Sets the token type.
     *
     * @param tokenType The token type.
     */
    protected void setTokenType(String tokenType) {
        put(TOKEN_TYPE, tokenType);
    }

    /**
     * Sets the token name.
     *
     * @param tokenName The token name.
     */
    protected void setTokenName(String tokenName) {
        put(TOKEN_NAME, tokenName);
    }

    /**
     * Sets the grant type.
     *
     * @param grantType The grant type.
     */
    protected void setGrantType(String grantType) {
        put(OAuth2Constants.Params.GRANT_TYPE, grantType);
    }

    /**
     * Sets the nonce.
     *
     * @param nonce The nonce.
     */
    protected void setNonce(String nonce) {
        put(OAuth2Constants.Custom.NONCE, nonce);
    }

    /**
     * Gets the scope.
     *
     * @return The scope.
     */
    public Set<String> getScope() {
        final Set<String> scope = (Set<String>) get(SCOPE).getObject();
        if (!Utils.isEmpty(scope)) {
            return scope;
        }
        return Collections.emptySet();
    }

    /**
     * Gets the client's id.
     *
     * @return The client's id.
     */
    public String getClientId() {
        if (isDefined(CLIENT_ID)) {
            return get(CLIENT_ID).asString();
        }
        return null;
    }

    /**
     * Gets the nonce.
     *
     * @return The nonce.
     */
    public String getNonce() {
        if (isDefined(OAuth2Constants.Custom.NONCE)) {
            return get(OAuth2Constants.Custom.NONCE).asString();
        }
        return null;
    }

    /**
     * Gets the session id used to create the authorisation code
     *
     * @return The session id.
     */
    public String getSessionId() {
        return (String) extraData.get(OAuth2Constants.Custom.SSO_TOKEN_ID);
    }

    /**
     * Gets the resource owner's id.
     *
     * @return The resource owner's id.
     */
    public String getResourceOwnerId() {
        if (isDefined(USERNAME)) {
            return get(USERNAME).asString();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getTokenId() {
        if (isDefined(OAuth2Constants.Params.ID)) {
            return get(OAuth2Constants.Params.ID).asString();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getTokenName() {
        if (isDefined(TOKEN_NAME)) {
            return get(TOKEN_NAME).asString();
        }
        return null;
    }

    /**
     * Determines if the Access Token is expired.
     *
     * @return {@code true} if current time is greater than the expiry time.
     */
    public boolean isExpired() {
        return currentTimeMillis() > getExpiryTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRealm() {
        return getStringProperty(OAuth2Constants.Params.REALM);
    }

    /**
     * Gets the requested claims associated w/ this access token.
     *
     * @return Requested claims (JSON as a String).
     */
    public String getClaims() {
        if (isDefined(OAuth2Constants.Custom.CLAIMS)) {
            return (String) get(OAuth2Constants.Custom.CLAIMS).asSet().iterator().next();
        }

        return null;
    }

    /**
     * Gets the expiry time.
     *
     * @return The Expiry time.
     */
    public long getExpiryTime() {
        if (isDefined(EXPIRE_TIME)) {
            return get(EXPIRE_TIME).asLong();
        }
        return 0;
    }

    /**
     * Gets the token type.
     *
     * @return The token type.
     */
    public String getTokenType() {
        if (isDefined(TOKEN_TYPE)) {
            return get(TOKEN_TYPE).asString();
        }
        return null;
    }

    /**
     * Gets the grant type.
     *
     * @return The grant type.
     */
    public String getGrantType() {
        if (isDefined(OAuth2Constants.Params.GRANT_TYPE)) {
            return get(OAuth2Constants.Params.GRANT_TYPE).asString();
        }
        return null;
    }

    /**
     * Get a string property from the store.
     * @param key The property key.
     * @return The value.
     */
    protected String getStringProperty(String key) {
        if (isDefined(key)) {
            return get(key).asString();
        }
        return null;
    }

    /**
     * Gets the display String for the given String.
     *
     * @param s The String.
     * @return The display String.
     */
    protected String getResourceString(String s) {
        return s;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Object> toMap() {
        final Map<String, Object> tokenMap = new HashMap<String, Object>();
        tokenMap.put(getResourceString(OAuth2Constants.Params.ACCESS_TOKEN), getTokenId());
        tokenMap.put(getResourceString(TOKEN_TYPE), getTokenType());
        tokenMap.put(getResourceString(EXPIRE_TIME),
                (getExpiryTime() - currentTimeMillis()) / 1000);
        tokenMap.putAll(extraData);
        return tokenMap;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Object> getTokenInfo() {
        Map<String, Object> tokenInfo = new HashMap<String, Object>();
        tokenInfo.put(getResourceString(ID), getTokenId());
        tokenInfo.put(getResourceString(TOKEN_TYPE), getTokenType());
        tokenInfo.put(getResourceString(EXPIRE_TIME),
                (getExpiryTime() - currentTimeMillis())/1000);
        tokenInfo.put(getResourceString(SCOPE), getScope());
        tokenInfo.put(getResourceString(OAuth2Constants.Params.GRANT_TYPE), getGrantType());
        return tokenInfo;
    }

    /**
     * <p>Adds additional data to the Access Token.</p>
     *
     * <p>If the value is {@code null} then this method will ensure that the key is not present in the map.</p>
     *
     * @param key The key.
     * @param value The value.
     */
    public void addExtraData(String key, String value) {
        if (value != null) {
            extraData.put(key, value);
        } else {
            extraData.remove(key);
        }
    }

    private void validateTokenName(String tokenName, String tokenId) throws InvalidGrantException {
        if (!OAuth2Constants.Token.OAUTH_ACCESS_TOKEN.equals(tokenName)) {
            throw new InvalidGrantException("Token is not an access token: " + tokenId);
        }
    }
}
