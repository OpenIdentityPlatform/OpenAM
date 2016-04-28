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

import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.*;
import static org.forgerock.openam.oauth2.OAuth2Constants.JWTTokenParams.ACR;
import static org.forgerock.openam.oauth2.OAuth2Constants.Token.OAUTH_REFRESH_TOKEN;

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
public class StatefulRefreshToken extends StatefulToken implements RefreshToken {

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
            String acr,
            String authGrantId) {
        super(new HashMap<String, Object>());
        setId(id);
        setResourceOwnerId(resourceOwnerId);
        setClientId(clientId);
        setRedirectUri(redirectUri);
        setScope(scope);
        setExpiryTime(expiryTime);
        setAuthGrantId(authGrantId);
        setTokenType(tokenType);
        setTokenName(tokenName);
        setGrantType(grantType);
        setAuthModules(authModules);
        setAuthenticationContextClassReference(acr);
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
     * Gets the client's redirect uri.
     *
     * @return The client's redirect uri.
     */
    @Override
    public String getRedirectUri() {
        return getStringProperty(REDIRECT_URI);
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
        return ! isNeverExpires() && super.isExpired();
    }

    protected long defaultExpireTime() {
        return -1;
    }

    /**
     * Get whether or not token expires.
     *
     * @return Whether or not token expires.
     */
    private boolean isNeverExpires() {
        return getExpiryTime() == defaultExpireTime();
    }

    /**
     * Gets the auth modules used for authentication.
     * @return A pipe-delimited string of auth module names.
     */
    @Override
    public String getAuthModules() {
        return getStringProperty(AUTH_MODULES);
    }

    @Override
    public Map<String, Object> toMap() {
        final Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put(getResourceString(TOKEN_TYPE), getTokenType());
        tokenMap.put(getResourceString(EXPIRE_TIME), getTimeLeft());
        return tokenMap;
    }

    @Override
    public Map<String, Object> getTokenInfo() {
        final Map<String, Object> tokenInfo = new HashMap<>();
        tokenInfo.put(getResourceString(TOKEN_TYPE), getTokenType());
        tokenInfo.put(getResourceString(EXPIRE_TIME), getTimeLeft());
        tokenInfo.put(getResourceString(SCOPE), getScope());
        return tokenInfo;
    }

    @Override
    public String getAuthGrantId() {
        return getStringProperty(AUTH_GRANT_ID);
    }

    protected Long getTimeLeft() {
        if (isNeverExpires()) {
            return null;
        } else {
            return super.getTimeLeft();
        }
    }
}
