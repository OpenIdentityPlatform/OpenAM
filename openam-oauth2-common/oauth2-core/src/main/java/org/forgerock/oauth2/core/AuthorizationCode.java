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

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;

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
     * @throws InvalidGrantException If the given token is not an Authorization Code token.
     */
    protected AuthorizationCode(JsonValue token) throws InvalidGrantException {
        super(token);
        if (!OAuth2Constants.Token.OAUTH_CODE_TYPE.equals(getTokenName())) {
            throw new InvalidGrantException("Token is not an authorization code token: " + getTokenId());
        }
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
            Set<String> scope, long expiryTime, String nonce, String authModules, String acr, String codeChallenge,
                             String codeChallengeMethod) {
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
        setAuthModules(authModules);
        setAuthenticationContextClassReference(acr);
        setCodeChallenge(codeChallenge);
        setCodeChallengeMethod(codeChallengeMethod);
    }

    /**
     * Sets the authorization code.
     *
     * @param code The authorization code.
     */
    protected void setCode(String code) {
        setStringProperty(OAuth2Constants.CoreTokenParams.ID, code);
    }

    /**
     * Sets the resource owner's id.
     *
     * @param resourceOwnerId The resource owner's id.
     */
    protected void setResourceOwnerId(String resourceOwnerId) {
        setStringProperty(OAuth2Constants.CoreTokenParams.USERNAME, resourceOwnerId);
    }

    /**
     * Sets the client's id.
     *
     * @param clientId The client's id.
     */
    protected void setClientId(String clientId) {
        setStringProperty(OAuth2Constants.CoreTokenParams.CLIENT_ID, clientId);
    }

    /**
     * Sets the redirect uri.
     *
     * @param redirectUri The redirect uri.
     */
    protected void setRedirectUri(String redirectUri) {
        setStringProperty(OAuth2Constants.CoreTokenParams.REDIRECT_URI, redirectUri);
    }

    /**
     * Sets the token type.
     *
     * @param tokenType The token type.
     */
    protected void setTokenType(String tokenType) {
        setStringProperty(OAuth2Constants.CoreTokenParams.TOKEN_TYPE, tokenType);
    }

    /**
     * Sets the token name.
     *
     * @param tokenName The token name.
     */
    protected void setTokenName(String tokenName) {
        setStringProperty(OAuth2Constants.CoreTokenParams.TOKEN_NAME, tokenName);
    }

    /**
     * Sets the auth modules
     * @param authModules The auth modules string.
     */
    protected void setAuthModules(String authModules) {
        setStringProperty(OAuth2Constants.CoreTokenParams.AUTH_MODULES, authModules);
    }

    /**
     * Sets the nonce.
     *
     * @param nonce The nonce.
     */
    protected void setNonce(String nonce) {
        setStringProperty(OAuth2Constants.Custom.NONCE, nonce);
    }

    /**
     * Sets the authentication context class reference (acr).
     *
     * @param acr The acr.
     */
    protected void setAuthenticationContextClassReference(String acr) {
        setStringProperty(OAuth2Constants.JWTTokenParams.ACR, acr);
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
     * Determines if the Authorization Code is expired.
     *
     * @return {@code true} if current time is greater than the expiry time.
     */
    public final boolean isExpired() {
        return currentTimeMillis() > getExpiryTime();
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
        return getStringProperty(OAuth2Constants.CoreTokenParams.ID);
    }

    /**
     * Gets the token type.
     *
     * @return The token type.
     */
    public String getTokenType() {
        return getStringProperty(OAuth2Constants.CoreTokenParams.TOKEN_TYPE);
    }

    /**
     * {@inheritDoc}
     */
    public String getTokenName() {
        return getStringProperty(OAuth2Constants.CoreTokenParams.TOKEN_NAME);
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
        return getStringProperty(OAuth2Constants.CoreTokenParams.REDIRECT_URI);
    }

    /**
     * Gets the client's id.
     *
     * @return The client's id.
     */
    public String getClientId() {
        return getStringProperty(OAuth2Constants.CoreTokenParams.CLIENT_ID);
    }

    /**
     * Gets the resource owner's id.
     *
     * @return The resource owner's id.
     */
    public String getResourceOwnerId() {
        return getStringProperty(OAuth2Constants.CoreTokenParams.USERNAME);
    }

    /**
     * Get the auth modules string.
     * @return The pipe-separated list of auth modules.
     */
    public String getAuthModules() {
        return getStringProperty(OAuth2Constants.CoreTokenParams.AUTH_MODULES);
    }

    /**
     * Get the Authentication Context Class Reference (acr).
     * @return The acr string matched, if any.
     */
    public String getAuthenticationContextClassReference() {
        return getStringProperty(OAuth2Constants.JWTTokenParams.ACR);
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
        return getStringProperty(OAuth2Constants.Custom.NONCE);
    }

    /**
     * Gets the session id of the authenticating session.
     *
     * @return The session id.
     */
    public String getSessionId() {
        return getStringProperty(OAuth2Constants.Custom.SSO_TOKEN_ID);
    }

    protected String getStringProperty(String key) {
        return isDefined(key) ? get(key).asString() : null;
    }

    protected void setStringProperty(String key, String value) {
        put(key, value);
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
                (getExpiryTime() - currentTimeMillis()) / 1000);
        return tokenMap;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Object> getTokenInfo() {
        final Map<String, Object> tokenInfo = new HashMap<String, Object>();
        tokenInfo.put(getResourceString(OAuth2Constants.CoreTokenParams.TOKEN_TYPE), getTokenType());
        tokenInfo.put(getResourceString(OAuth2Constants.CoreTokenParams.EXPIRE_TIME),
                (getExpiryTime() - currentTimeMillis()) / 1000);
        tokenInfo.put(getResourceString(OAuth2Constants.CoreTokenParams.SCOPE), getScope());
        return tokenInfo;
    }

    /**
     * Sets the code challenge
     * @param codeChallenge
     */
    public void setCodeChallenge(String codeChallenge) {
        setStringProperty(OAuth2Constants.Custom.CODE_CHALLENGE, codeChallenge);
    }

    /**
     * Get the code challenge
     * @return code challenge
     */
    public String getCodeChallenge() {
        return getStringProperty(OAuth2Constants.Custom.CODE_CHALLENGE);
    }

    /**
     * Sets the code challenge method
     * @param codeChallengeMethod
     */
    public void setCodeChallengeMethod(String codeChallengeMethod) {
        setStringProperty(OAuth2Constants.Custom.CODE_CHALLENGE_METHOD, codeChallengeMethod);
    }

    /**
     * Get the code challenge method
     * @return code challenge method
     */
    public String getCodeChallengeMethod() {
        return getStringProperty(OAuth2Constants.Custom.CODE_CHALLENGE_METHOD);
    }
}
