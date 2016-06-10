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

import static org.forgerock.oauth2.core.Utils.stringToSet;
import static org.forgerock.openam.oauth2.OAuth2Constants.Bearer.BEARER;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.*;
import static org.forgerock.openam.oauth2.OAuth2Constants.Custom.SSO_TOKEN_ID;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.ACCESS_TOKEN;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.GRANT_TYPE;
import static org.forgerock.openam.oauth2.OAuth2Constants.Token.OAUTH_ACCESS_TOKEN;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;

/**
 * Models a OAuth2 access token.
 *
 * @since 12.0.0
 */
public class StatefulAccessToken extends StatefulToken implements AccessToken {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("OAuth2CoreToken");

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
     * @param realm The realm.
     * @param claims The requested claims.
     * @param auditTrackingId The tracking ID, used for tracking tokens throughout the audit logs.
     */
    public StatefulAccessToken(String id, String authorizationCode, String resourceOwnerId, String clientId,
            String redirectUri, Set<String> scope, long expiryTime, RefreshToken refreshToken,
            String tokenName, String grantType, String nonce, String realm, String claims,
            String auditTrackingId) {
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
        setRealm(realm);
        setAuditTrackingId(auditTrackingId);

        if (!StringUtils.isBlank(claims)) {
            setClaims(claims);
        }
    }

    public void setClaims(String claims) {
        put(OAuth2Constants.Custom.CLAIMS, CollectionUtils.asSet(claims));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setId(String id) {
        put(ID, stringToSet(id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setResourceOwnerId(String resourceOwnerId) {
        put(USERNAME, CollectionUtils.asSet(resourceOwnerId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setClientId(String clientId) {
        put(CLIENT_ID, CollectionUtils.asSet(clientId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setRedirectUri(String redirectUri) {
        put(REDIRECT_URI, stringToSet(redirectUri));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setScope(Set<String> scope) {
        put(SCOPE, scope);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setExpiryTime(long expiryTime) {
        put(EXPIRE_TIME, stringToSet(String.valueOf(expiryTime)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setTokenType(String tokenType) {
        put(TOKEN_TYPE, stringToSet(tokenType));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setTokenName(String tokenName) {
        put(TOKEN_NAME, stringToSet(tokenName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setGrantType(String grantType) {
        put(OAuth2Constants.Params.GRANT_TYPE, stringToSet(grantType));
    }


    /**
     * Sets the realm.
     * <br/>
     * If the specified realm is {@code null} or an empty String. '/' is used instead.
     *
     * @param realm The realm.
     */
    private void setRealm(final String realm) {
        if (realm == null || realm.isEmpty()) {
            this.put(REALM, stringToSet("/"));
        } else {
            this.put(REALM, stringToSet(realm));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getResourceString(String s) {
        return RESOURCE_BUNDLE.getString(s);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getScope() {
        final Set<String> value = getParameter(SCOPE);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        return Collections.emptySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getClientId() {
        final Set<String> value = getParameter(CLIENT_ID);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResourceOwnerId() {
        final Set<String> value = getParameter(USERNAME);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTokenId() {
        final Set<String> value = getParameter(ID);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
    }

    /**
     * Gets the realm.
     *
     * @return The realm.
     */
    public String getRealm() {
        final Set<String> value = getParameter(OAuth2Constants.Custom.REALM);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTokenName() {
        final Set<String> value = getParameter(TOKEN_NAME);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getExpiryTime() {
        final Set<String> value = getParameter(EXPIRE_TIME);
        if (value != null && !value.isEmpty()) {
            return Long.parseLong(value.iterator().next());
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTokenType() {
        final Set<String> value = getParameter(TOKEN_TYPE);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
    }

    /**
     * Sets the authorization code.
     *
     * @param authorizationCode The authorization code.
     */
    protected void setAuthorizationCode(String authorizationCode) {
        put(PARENT, stringToSet(authorizationCode));
    }

    /**
     * Sets the refresh token id.
     *
     * @param refreshTokenId The refresh token id.
     */
    protected void setRefreshTokenId(String refreshTokenId) {
        put(REFRESH_TOKEN, stringToSet(refreshTokenId));
    }

    /**
     * Sets the nonce.
     *
     * @param nonce The nonce.
     */
    protected void setNonce(String nonce) {
        put(OAuth2Constants.Custom.NONCE, stringToSet(nonce));
    }

    /**
     * Gets the nonce.
     *
     * @return The nonce.
     */
    @Override
    public String getNonce() {
        final Set<String> value = getParameter(OAuth2Constants.Custom.NONCE);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
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
        final Set<String> value = getParameter(OAuth2Constants.Params.GRANT_TYPE);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
    }

    /**
     * Gets the specified parameter from the JsonValue.
     *
     * @param paramName The parameter name.
     * @return A {@code Set} of the parameter values.
     */
    private Set<String> getParameter(String paramName) {
        final JsonValue param = get(paramName);
        if (param != null) {
            return (Set<String>) param.getObject();
        }
        return null;
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
        tokenInfo.put(getResourceString(REALM), getRealm());
        return tokenInfo;
    }

    @Override
    public AuditConstants.TrackingIdKey getAuditTrackingIdKey() {
        return AuditConstants.TrackingIdKey.OAUTH2_ACCESS;
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

    /**
     * Sets the audit id.
     *
     * @param auditId The audit id.
     */
    protected void setAuditTrackingId(String auditId) {
        setStringProperty(AUDIT_TRACKING_ID, auditId);
    }

    /**
     * Gets the audit id.
     *
     * @return The audit id.
     */
    @Override
    public String getAuditTrackingId() {
        return getStringProperty(AUDIT_TRACKING_ID);
    }

    /**
     * Set a string property in the store.
     * @param key The property key.
     * @param value The value.
     */
    protected void setStringProperty(String key, String value) {
        put(key, stringToSet(value));
    }

    @Override
    public String toString() {
        return getTokenId();
    }
}
