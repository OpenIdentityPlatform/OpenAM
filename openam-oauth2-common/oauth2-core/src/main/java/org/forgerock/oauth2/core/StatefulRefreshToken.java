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
import static org.forgerock.oauth2.core.OAuth2Constants.Token.OAUTH_REFRESH_TOKEN;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.ID;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.USERNAME;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.CLIENT_ID;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.REDIRECT_URI;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.SCOPE;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.EXPIRE_TIME;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.TOKEN_TYPE;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.TOKEN_NAME;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.GRANT_TYPE;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.AUTH_MODULES;
import static org.forgerock.oauth2.core.OAuth2Constants.JWTTokenParams.ACR;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.REALM;
import static org.forgerock.oauth2.core.OAuth2Constants.Custom.CLAIMS;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;

/**
 * Models a OAuth2 Refresh Token.
 *
 * @since 12.0.0
 */
public class StatefulRefreshToken extends JsonValue implements RefreshToken {

    /**
     * Constructs a new RefreshToken backed with the data in the specified JsonValue.
     *
     * @param token The JsonValue of the token.
     * @throws InvalidGrantException If the given token is not a Refresh Token.
     */
    public StatefulRefreshToken(JsonValue token) throws InvalidGrantException {
        super(token);
        if (! OAUTH_REFRESH_TOKEN.equals(getTokenName())) {
            throw new InvalidGrantException("Token is not an refresh token: " + getTokenId());
        }
    }

    /**
     * Constructs a new RefreshToken.
     *
     * @param id The token id.
     * @param resourceOwnerId The resource owner's id.
     * @param clientId The client's id.
     * @param redirectUri The redirect uri.
     * @param scope The scope.
     * @param expiryTime The expiry time.
     * @param tokenType The token type.
     * @param tokenName The token name.
     * @param grantType The grant type.
     */
    public StatefulRefreshToken(
            String id,
            String resourceOwnerId,
            String clientId,
            String redirectUri,
            Set<String> scope,
            long expiryTime,
            String tokenType,
            String tokenName,
            String grantType,
            String authModules,
            String acr) {
        super(new HashMap<String, Object>());
        setTokenId(id);
        setResourceOwnerId(resourceOwnerId);
        setClientId(clientId);
        setRedirectUri(redirectUri);
        setScope(scope);
        setExpiryTime(expiryTime);
        setTokenType(tokenType);
        setTokenName(tokenName);
        setGrantType(grantType);
        setAuthModules(authModules);
        setAuthenticationContextClassReference(acr);
    }

    /**
     * Sets the token id.
     *
     * @param tokenId The token id.
     */
    protected void setTokenId(final String tokenId) {
        setStringProperty(ID, tokenId);
    }

    /**
     * Sets the resource owner's id.
     *
     * @param resourceOwnerId The resource owner's id.
     */
    protected void setResourceOwnerId(final String resourceOwnerId) {
        setStringProperty(USERNAME, resourceOwnerId);
    }

    /**
     * Sets the client's id.
     *
     * @param clientId The client's id.
     */
    protected void setClientId(final String clientId) {
        setStringProperty(CLIENT_ID, clientId);
    }

    /**
     * Sets the redirect uri.
     *
     * @param redirectUri The redirect uri.
     */
    protected void setRedirectUri(final String redirectUri) {
        setStringProperty(REDIRECT_URI, redirectUri);
    }

    /**
     * Sets the scope.
     *
     * @param scope The scope.
     */
    protected void setScope(final Set<String> scope) {
        put(SCOPE, scope);
    }

    /**
     * Sets the expiry time.
     *
     * @param expiryTime The expiry time.
     */
    protected void setExpiryTime(final long expiryTime) {
        put(EXPIRE_TIME, expiryTime);
    }

    /**
     * Sets the token type.
     *
     * @param tokenType The token type.
     */
    protected void setTokenType(final String tokenType) {
        setStringProperty(TOKEN_TYPE, tokenType);
    }

    /**
     * Sets the token name.
     *
     * @param tokenName The token name.
     */
    protected void setTokenName(final String tokenName) {
        setStringProperty(TOKEN_NAME, tokenName);
    }

    /**
     * Sets the grant type.
     *
     * @param grantType The grant type.
     */
    protected void setGrantType(final String grantType) {
        setStringProperty(GRANT_TYPE, grantType);
    }

    /**
     * Sets the auth modules used for authentication.
     * @param authModules A pipe-delimited string of auth module names.
     */
    protected void setAuthModules(String authModules) {
        setStringProperty(AUTH_MODULES, authModules);
    }

    /**
     * Sets the authentication context class reference (acr).
     *
     * @param acr The acr.
     */
    protected void setAuthenticationContextClassReference(String acr) {
        setStringProperty(ACR, acr);
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
     * Gets the resource owner's id.
     *
     * @return The resource owner's id.
     */
    @Override
    public String getResourceOwnerId() {
        return getStringProperty(USERNAME);
    }

    /**
     * Gets the client's redirect uri.
     *
     * @return The client's redirect uri.
     */
    @Override
    public String getRedirectUri() {
        return getStringProperty(REDIRECT_URI);
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
     * Get the Authentication Context Class Reference (acr).
     * @return The acr string matched, if any.
     */
    @Override
    public String getAuthenticationContextClassReference() {
        return getStringProperty(ACR);
    }

    /**
     * Determines if the Refresh Token has expired.
     *
     * @return {@code true} if current time is greater than the expiry time.
     */
    @Override
    public boolean isExpired() {
        if (isNeverExpires()) {
            return false;
        }
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
     * Get whether or not token expires.
     *
     * @return Whether or not token expires.
     */
    @Override
    public boolean isNeverExpires() {
        return getExpiryTime() == -1;
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
     * Gets the auth modules used for authentication.
     * @return A pipe-delimited string of auth module names.
     */
    @Override
    public String getAuthModules() {
        return getStringProperty(AUTH_MODULES);
    }

    /**
     * Gets the display String for the given String.
     *
     * @param string The String.
     * @return The display String.
     */
    protected String getResourceString(final String string) {
        return string;
    }

    /**
     * Get a string property from the store.
     * @param key The property key.
     * @return The value.
     */
    public String getStringProperty(String key) {
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
        return -1;
    }

    @SuppressWarnings("unchecked")
    protected Set<String> getSetProperty(String key) {
        final Set<String> scope = (Set<String>) get(key).getObject();
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
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> toMap() {
        final Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put(getResourceString(TOKEN_TYPE), getTokenType());
        tokenMap.put(getResourceString(EXPIRE_TIME), getExpireTime());
        return tokenMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getTokenInfo() {
        final Map<String, Object> tokenInfo = new HashMap<>();
        tokenInfo.put(getResourceString(TOKEN_TYPE), getTokenType());
        tokenInfo.put(getResourceString(EXPIRE_TIME), getExpireTime());
        tokenInfo.put(getResourceString(SCOPE), getScope());
        return tokenInfo;
    }

    private long getExpireTime() {
        return getExpiryTime() == -1 ? null : (getExpiryTime() - currentTimeMillis()) / 1000;
    }

        @Override
    public String getAuditTrackingId() {
        return null;
    }

    public JsonValue toJsonValue() {
        return this;
    }
}
