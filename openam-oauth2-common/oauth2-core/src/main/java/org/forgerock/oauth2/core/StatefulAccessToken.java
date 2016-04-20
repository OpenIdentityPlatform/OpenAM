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

import static org.forgerock.openam.utils.Time.*;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.ID;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.PARENT;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.USERNAME;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.CLIENT_ID;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.REDIRECT_URI;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.SCOPE;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.EXPIRE_TIME;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.REFRESH_TOKEN;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.TOKEN_TYPE;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.TOKEN_NAME;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.ACCESS_TOKEN;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.GRANT_TYPE;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.REALM;
import static org.forgerock.oauth2.core.OAuth2Constants.Bearer.BEARER;
import static org.forgerock.oauth2.core.OAuth2Constants.Custom.NONCE;
import static org.forgerock.oauth2.core.OAuth2Constants.Custom.SSO_TOKEN_ID;
import static org.forgerock.oauth2.core.OAuth2Constants.Custom.CLAIMS;
import static org.forgerock.oauth2.core.OAuth2Constants.Token.OAUTH_ACCESS_TOKEN;

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
public class StatefulAccessToken extends JsonValue implements AccessToken {

    protected Map<String, Object> extraData = new HashMap<>();

    /**
     * Constructs a new AccessToken backed with the data in the specified JsonValue.
     *
     * @param token The JsonValue of the token.
     * @throws InvalidGrantException If the given token is not an Access Token.
     */
    public StatefulAccessToken(JsonValue token) throws InvalidGrantException {
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
    public StatefulAccessToken(JsonValue token, String tokenName, String tokenId) throws InvalidGrantException {
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
    public StatefulAccessToken(
            String id,
            String authorizationCode,
            String resourceOwnerId,
            String clientId,
            String redirectUri,
            Set<String> scope,
            long expiryTime,
            String refreshTokenId,
            String tokenName,
            String grantType,
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
        setTokenType(BEARER);
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
        setStringProperty(ID, id);
    }

    /**
     * Sets the authorization code.
     *
     * @param authorizationCode The authorization code.
     */
    protected void setAuthorizationCode(String authorizationCode) {
        setStringProperty(PARENT, authorizationCode);
    }

    /**
     * Sets the resource owner's id.
     *
     * @param resourceOwnerId The resource owner's id.
     */
    protected void setResourceOwnerId(String resourceOwnerId) {
        setStringProperty(USERNAME, resourceOwnerId);
    }

    /**
     * Sets the client's id.
     *
     * @param clientId The client's id.
     */
    protected void setClientId(String clientId) {
        setStringProperty(CLIENT_ID, clientId);
    }

    /**
     * Sets the redirect uri.
     *
     * @param redirectUri The redirect uri.
     */
    protected void setRedirectUri(String redirectUri) {
        setStringProperty(REDIRECT_URI, redirectUri);
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
        setStringProperty(REFRESH_TOKEN, refreshTokenId);
    }

    /**
     * Sets the token type.
     *
     * @param tokenType The token type.
     */
    protected void setTokenType(String tokenType) {
        setStringProperty(TOKEN_TYPE, tokenType);
    }

    /**
     * Sets the token name.
     *
     * @param tokenName The token name.
     */
    protected void setTokenName(String tokenName) {
        setStringProperty(TOKEN_NAME, tokenName);
    }

    /**
     * Sets the grant type.
     *
     * @param grantType The grant type.
     */
    protected void setGrantType(String grantType) {
        setStringProperty(GRANT_TYPE, grantType);
    }

    /**
     * Sets the nonce.
     *
     * @param nonce The nonce.
     */
    protected void setNonce(String nonce) {
        setStringProperty(NONCE, nonce);
    }

    /**
     * Gets the scope.
     *
     * @return The scope.
     */
    @Override
    public Set<String> getScope() {
        return getSetProperty(SCOPE);
    }

    /**
     * Gets the client's id.
     *
     * @return The client's id.
     */
    @Override
    public String getClientId() {
        return getStringProperty(CLIENT_ID);
    }

    /**
     * Gets the nonce.
     *
     * @return The nonce.
     */
    @Override
    public String getNonce() {
        return getStringProperty(NONCE);
    }

    /**
     * Gets the session id used to create the authorisation code
     *
     * @return The session id.
     */
    @Override
    public String getSessionId() {
        return (String) extraData.get(SSO_TOKEN_ID);
    }

    /**
     * Gets the resource owner's id.
     *
     * @return The resource owner's id.
     */
    @Override
    public String getResourceOwnerId() {
        return getStringProperty(USERNAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTokenId() {
        return getStringProperty(ID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTokenName() {
        return getStringProperty(TOKEN_NAME);
    }

    /**
     * Determines if the Access Token is expired.
     *
     * @return {@code true} if current time is greater than the expiry time.
     */
    @Override
    public boolean isExpired() {
        return currentTimeMillis() > getExpiryTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRealm() {
        return getStringProperty(REALM);
    }

    /**
     * Gets the requested claims associated w/ this access token.
     *
     * @return Requested claims (JSON as a String).
     */
    @Override
    public String getClaims() {
        return getStringProperty(CLAIMS);
    }

    /**
     * Gets the expiry time.
     *
     * @return The Expiry time.
     */
    @Override
    public long getExpiryTime() {
        return getLongProperty(EXPIRE_TIME);
    }

    /**
     * Gets the token type.
     *
     * @return The token type.
     */
    @Override
    public String getTokenType() {
        return getStringProperty(TOKEN_TYPE);
    }

    /**
     * Gets the grant type.
     *
     * @return The grant type.
     */
    @Override
    public String getGrantType() {
        return getStringProperty(GRANT_TYPE);
    }

    /**
     * Get a string property from the store.
     * @param key The property key.
     * @return The value.
     */
    protected String getStringProperty(String key) {
        if (isDefined(key)) {
            JsonValue value = get(key);
            if (value.isString()) {
                return value.asString();
            } else if (value.isCollection()) {
                return (String) value.asList().iterator().next();
            }
        }
        return null;
    }

    protected long getLongProperty(String key) {
        if (isDefined(key)) {
            return get(key).asLong();
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    protected Set<String> getSetProperty(String key) {
        final Set<String> scope = (Set<String>) get(SCOPE).getObject();
        if (!Utils.isEmpty(scope)) {
            return scope;
        }
        return Collections.emptySet();
    }

    /**
     * Set a string property in the store.
     * @param key The property key.
     * @param value The value.
     */
    protected void setStringProperty(String key, String value) {
        put(key, value);
    }

    /**
     * Gets the display String for the given String.
     *
     * @param string The String.
     * @return The display String.
     */
    protected String getResourceString(String string) {
        return string;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> toMap() {
        final Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put(getResourceString(ACCESS_TOKEN), getTokenId());
        tokenMap.put(getResourceString(TOKEN_TYPE), getTokenType());
        tokenMap.put(getResourceString(EXPIRE_TIME), getExpireTime());
        tokenMap.putAll(extraData);
        return tokenMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getTokenInfo() {
        Map<String, Object> tokenInfo = new HashMap<>();
        tokenInfo.put(getResourceString(ID), getTokenId());
        tokenInfo.put(getResourceString(TOKEN_TYPE), getTokenType());
        tokenInfo.put(getResourceString(EXPIRE_TIME), getExpireTime());
        tokenInfo.put(getResourceString(SCOPE), getScope());
        tokenInfo.put(getResourceString(CLIENT_ID), getClientId());
        tokenInfo.put(getResourceString(GRANT_TYPE), getGrantType());
        return tokenInfo;
    }

    private long getExpireTime() {
        return (getExpiryTime() - currentTimeMillis()) / 1000;
    }

    /**
     * <p>Adds additional data to the Access Token.</p>
     *
     * <p>If the value is {@code null} then this method will ensure that the key is not present in the map.</p>
     *
     * @param key The key.
     * @param value The value.
     */
    @Override
    public void addExtraData(String key, String value) {
        if (value != null) {
            extraData.put(key, value);
        } else {
            extraData.remove(key);
        }
    }

    @Override
    public String getAuditTrackingId() {
        return null;
    }

    private void validateTokenName(String tokenName, String tokenId) throws InvalidGrantException {
        if (! OAUTH_ACCESS_TOKEN.equals(tokenName)) {
            throw new InvalidGrantException("Token is not an access token: " + tokenId);
        }
    }

    @Override
    public String toString() {
        return getTokenId();
    }

    public JsonValue toJsonValue() {
        return this;
    }
}
