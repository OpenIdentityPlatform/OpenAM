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

import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.ID;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.PARENT;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.CLIENT_ID;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.SCOPE;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.EXPIRE_TIME;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.REFRESH_TOKEN;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.TOKEN_TYPE;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.ACCESS_TOKEN;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.GRANT_TYPE;
import static org.forgerock.openam.oauth2.OAuth2Constants.Bearer.BEARER;
import static org.forgerock.openam.oauth2.OAuth2Constants.Custom.NONCE;
import static org.forgerock.openam.oauth2.OAuth2Constants.Custom.SSO_TOKEN_ID;
import static org.forgerock.openam.oauth2.OAuth2Constants.Token.OAUTH_ACCESS_TOKEN;

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
public class StatefulAccessToken extends StatefulToken implements AccessToken {

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
     * @param refreshToken The refresh token.
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
            RefreshToken refreshToken,
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
        if (refreshToken != null) {
            setRefreshTokenId(refreshToken.getTokenId());
            setAuthGrantId(refreshToken.getAuthGrantId());
        }
        setTokenType(BEARER);
        setTokenName(tokenName);
        setGrantType(grantType);
        setNonce(nonce);
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
     * Sets the refresh token id.
     *
     * @param refreshTokenId The refresh token id.
     */
    protected void setRefreshTokenId(String refreshTokenId) {
        setStringProperty(REFRESH_TOKEN, refreshTokenId);
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

    protected long defaultExpireTime() {
        return 0;
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

    @Override
    public Map<String, Object> toMap() {
        final Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put(getResourceString(ACCESS_TOKEN), getTokenId());
        tokenMap.put(getResourceString(TOKEN_TYPE), getTokenType());
        tokenMap.put(getResourceString(EXPIRE_TIME), getTimeLeft());
        tokenMap.putAll(extraData);
        return tokenMap;
    }

    @Override
    public Map<String, Object> getTokenInfo() {
        Map<String, Object> tokenInfo = new HashMap<>();
        tokenInfo.put(getResourceString(ID), getTokenId());
        tokenInfo.put(getResourceString(TOKEN_TYPE), getTokenType());
        tokenInfo.put(getResourceString(EXPIRE_TIME), getTimeLeft());
        tokenInfo.put(getResourceString(SCOPE), getScope());
        tokenInfo.put(getResourceString(CLIENT_ID), getClientId());
        tokenInfo.put(getResourceString(GRANT_TYPE), getGrantType());
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
    @Override
    public void addExtraData(String key, String value) {
        if (value != null) {
            extraData.put(key, value);
        } else {
            extraData.remove(key);
        }
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
}
