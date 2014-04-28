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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Models a OAuth2 Authorization Code.
 *
 * @since 12.0.0
 */
public class AuthorizationCode extends JsonValue implements Token {

    /**
     * Constructs a new AuthorizationCode backed with the data in the specified JsonValue.
     *
     * @param token The JsonValue of the token.
     */
    protected AuthorizationCode(JsonValue token) {
        super(token);
    }

    /**
     * Constructs a new AuthorizationCode.
     *
     * @param code The authorization code.
     * @param resourceOwnerId The resource owner's id.
     * @param clientId The client's id.
     * @param redirectUri The redirect uri.
     * @param scope The scopes.
     * @param expiryTime The expiry time.
     * @param nonce The nonce.
     */
    public AuthorizationCode(String code, String resourceOwnerId, String clientId, String redirectUri,
            Set<String> scope, long expiryTime, String nonce) {
        super(new HashMap<String, Object>());
        setCode(code);
        setResourceOwnerId(resourceOwnerId);
        setClientId(clientId);
        setRedirectUri(redirectUri);
        setScope(scope);
        setExpiryTime(expiryTime);
        setTokenType("Bearer");
        setTokenName(OAuth2Constants.Token.OAUTH_CODE_TYPE);
        setNonce(nonce);
    }

    /**
     * Sets the authorization code.
     *
     * @param code The authorization code.
     */
    protected void setCode(String code) {
        put(OAuth2Constants.CoreTokenParams.ID, code);
    }

    /**
     * Sets the resource owner's id.
     *
     * @param resourceOwnerId The resource owner's id.
     */
    protected void setResourceOwnerId(String resourceOwnerId) {
        put(OAuth2Constants.CoreTokenParams.USERNAME, resourceOwnerId);
    }

    /**
     * Sets the client's id.
     *
     * @param clientId The client's id.
     */
    protected void setClientId(String clientId) {
        put(OAuth2Constants.CoreTokenParams.CLIENT_ID, clientId);
    }

    /**
     * Sets the redirect uri.
     *
     * @param redirectUri The redirect uri.
     */
    protected void setRedirectUri(String redirectUri) {
        put(OAuth2Constants.CoreTokenParams.REDIRECT_URI, redirectUri);
    }

    /**
     * Sets the scope.
     *
     * @param scope The scope.
     */
    protected void setScope(Set<String> scope) {
        put(OAuth2Constants.CoreTokenParams.SCOPE, scope);
    }

    /**
     * Sets the expiry time.
     *
     * @param expiryTime The expiry time.
     */
    protected void setExpiryTime(long expiryTime) {
        put(OAuth2Constants.CoreTokenParams.EXPIRE_TIME, expiryTime);
    }

    /**
     * Sets the token type.
     *
     * @param tokenType The token type.
     */
    protected void setTokenType(String tokenType) {
        put(OAuth2Constants.CoreTokenParams.TOKEN_TYPE, tokenType);
    }

    /**
     * Sets the token name.
     *
     * @param tokenName The token name.
     */
    protected void setTokenName(String tokenName) {
        put(OAuth2Constants.CoreTokenParams.TOKEN_NAME, tokenName);
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
     * Determines if the Authorization Code is expired.
     *
     * @return {@code true} if current time is greater than the expiry time.
     */
    public final boolean isExpired() {
        return System.currentTimeMillis() > getExpiryTime();
    }

    /**
     * Gets the expiry time.
     *
     * @return The Expiry time.
     */
    public long getExpiryTime() {
        if (isDefined(OAuth2Constants.CoreTokenParams.EXPIRE_TIME)) {
            return get(OAuth2Constants.CoreTokenParams.EXPIRE_TIME).asLong();
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public String getTokenId() {
        if (isDefined(OAuth2Constants.CoreTokenParams.ID)) {
            return get(OAuth2Constants.CoreTokenParams.ID).asString();
        }
        return null;
    }

    /**
     * Gets the token type.
     *
     * @return The token type.
     */
    public String getTokenType() {
        if (isDefined(OAuth2Constants.CoreTokenParams.TOKEN_TYPE)) {
            return get(OAuth2Constants.CoreTokenParams.TOKEN_TYPE).asString();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getTokenName() {
        if (isDefined(OAuth2Constants.CoreTokenParams.TOKEN_NAME)) {
            return get(OAuth2Constants.CoreTokenParams.TOKEN_NAME).asString();
        }
        return null;
    }

    /**
     * Gets the scope.
     *
     * @return The scope.
     */
    public Set<String> getScope() {
        final Set<String> scope = (Set<String>) get(OAuth2Constants.CoreTokenParams.SCOPE).getObject();
        if (!Utils.isEmpty(scope)) {
            return scope;
        }
        return Collections.emptySet();
    }

    /**
     * Determines whether the authorization code has been issued.
     *
     * @return {@code true} if the authorization code has been issued.
     */
    public boolean isIssued() {
        if (isDefined(OAuth2Constants.CoreTokenParams.ISSUED)) {
            return get(OAuth2Constants.CoreTokenParams.ISSUED).asBoolean();
        }
        return false;
    }

    /**
     * Gets the redirect uri.
     *
     * @return The redirect uri.
     */
    public String getRedirectUri() {
        if (isDefined(OAuth2Constants.CoreTokenParams.REDIRECT_URI)) {
            return get(OAuth2Constants.CoreTokenParams.REDIRECT_URI).asString();
        }
        return null;
    }

    /**
     * Gets the client's id.
     *
     * @return The client's id.
     */
    public String getClientId() {
        if (isDefined(OAuth2Constants.CoreTokenParams.CLIENT_ID)) {
            return get(OAuth2Constants.CoreTokenParams.CLIENT_ID).asString();
        }
        return null;
    }

    /**
     * Gets the resource owner's id.
     *
     * @return The resource owner's id.
     */
    public String getResourceOwnerId() {
        if (isDefined(OAuth2Constants.CoreTokenParams.USERNAME)) {
            return get(OAuth2Constants.CoreTokenParams.USERNAME).asString();
        }
        return null;
    }

    /**
     * Sets the authorization code as issued.
     */
    public void setIssued() {
        put(OAuth2Constants.CoreTokenParams.ISSUED, true);
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
     * Gets the display String for the given String.
     *
     * @param s The String.
     * @return The display String.
     */
    protected String getResourceString(final String s) {
        return s;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Object> toMap() {
        final Map<String, Object> tokenMap = new HashMap<String, Object>();
        tokenMap.put(getResourceString(OAuth2Constants.CoreTokenParams.TOKEN_TYPE), getTokenType());
        tokenMap.put(getResourceString(OAuth2Constants.CoreTokenParams.EXPIRE_TIME),
                (getExpiryTime() - System.currentTimeMillis()) / 1000);
        return tokenMap;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Object> getTokenInfo() {
        final Map<String, Object> tokenInfo = new HashMap<String, Object>();
        tokenInfo.put(getResourceString(OAuth2Constants.CoreTokenParams.TOKEN_TYPE), getTokenType());
        tokenInfo.put(getResourceString(OAuth2Constants.CoreTokenParams.EXPIRE_TIME),
                (getExpiryTime() - System.currentTimeMillis()) / 1000);
        tokenInfo.put(getResourceString(OAuth2Constants.CoreTokenParams.SCOPE), getScope());
        return tokenInfo;
    }
}
