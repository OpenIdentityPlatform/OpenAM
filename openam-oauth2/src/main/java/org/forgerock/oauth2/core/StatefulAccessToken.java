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

import static java.lang.String.valueOf;
import static org.forgerock.json.JsonValueFunctions.setOf;
import static org.forgerock.openam.oauth2.OAuth2Constants.Bearer.BEARER;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.AUDIT_TRACKING_ID;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.AUTH_TIME;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.CLIENT_ID;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.CONFIRMATION_KEY;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.EXPIRE_TIME;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.ID;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.PARENT;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.REALM;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.REDIRECT_URI;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.REFRESH_TOKEN;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.SCOPE;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.TOKEN_NAME;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.TOKEN_TYPE;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.USERNAME;
import static org.forgerock.openam.oauth2.OAuth2Constants.Custom.NONCE;
import static org.forgerock.openam.oauth2.OAuth2Constants.Custom.SSO_TOKEN_ID;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.ACCESS_TOKEN;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.GRANT_TYPE;
import static org.forgerock.openam.oauth2.OAuth2Constants.Token.OAUTH_ACCESS_TOKEN;
import static org.forgerock.openam.utils.Time.currentTimeMillis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;

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
     * @param confirmationKey JSON confirmation key
     */
    public StatefulAccessToken(String id, String authorizationCode, String resourceOwnerId, String clientId,
            String redirectUri, Set<String> scope, long expiryTime, RefreshToken refreshToken,
            String tokenName, String grantType, String nonce, String realm, String claims,
            String auditTrackingId, JsonValue confirmationKey) {
        this(id, authorizationCode, resourceOwnerId, clientId, redirectUri, scope, expiryTime, refreshToken,
            tokenName, grantType, nonce, realm, claims, auditTrackingId,
            TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis()), confirmationKey);
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
     * @param authTime The end user's original auth time.
     * @param confirmationKey JSON confirmation key
     */
    public StatefulAccessToken(String id, String authorizationCode, String resourceOwnerId, String clientId,
            String redirectUri, Set<String> scope, long expiryTime, RefreshToken refreshToken,
            String tokenName, String grantType, String nonce, String realm, String claims,
            String auditTrackingId, long authTime, JsonValue confirmationKey) {
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
        setAuthTime(authTime);

        if (!StringUtils.isBlank(claims)) {
            setClaims(claims);
        }

        if (confirmationKey != null) {
            setConfirmationKey(confirmationKey);
        }
    }

    public void setClaims(String claims) {
        setStringProperty(OAuth2Constants.Custom.CLAIMS, claims);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setId(String id) {
        setStringProperty(ID, id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setResourceOwnerId(String resourceOwnerId) {
        setStringProperty(USERNAME, resourceOwnerId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setClientId(String clientId) {
        setStringProperty(CLIENT_ID, clientId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setRedirectUri(String redirectUri) {
        setStringProperty(REDIRECT_URI, redirectUri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setScope(Set<String> scope) {
        put(SCOPE, new ArrayList<>(scope));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setExpiryTime(long expiryTime) {
        setStringProperty(EXPIRE_TIME, valueOf(expiryTime));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setTokenType(String tokenType) {
        setStringProperty(TOKEN_TYPE, tokenType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setTokenName(String tokenName) {
        setStringProperty(TOKEN_NAME, tokenName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setGrantType(String grantType) {
        setStringProperty(GRANT_TYPE, grantType);
    }


    /**
     * Sets the realm.
     * <br/>
     * If the specified realm is {@code null} or an empty String. '/' is used instead.
     *
     * @param realm The realm.
     */
    private void setRealm(final String realm) {
        setStringProperty(REALM, realm == null || realm.isEmpty() ? "/" : realm);
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
        return getStringProperty(CLIENT_ID);
    }

    /**
     * {@inheritDoc}
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
     * Gets the realm.
     *
     * @return The realm.
     */
    public String getRealm() {
        return getStringProperty(REALM);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTokenName() {
        return getStringProperty(TOKEN_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getExpiryTime() {
        final String value = getStringProperty(EXPIRE_TIME);
        if (value != null && !value.isEmpty()) {
            return Long.parseLong(value);
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTokenType() {
        return getStringProperty(TOKEN_TYPE);
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
    public JsonValue getConfirmationKey() {
        return get(CONFIRMATION_KEY);
    }

    /**
     * Sets the confirmation key.
     *
     * @param confirmationKey
     *         the non-null JSON confirmation key
     */
    protected void setConfirmationKey(JsonValue confirmationKey) {
        Reject.ifNull(confirmationKey);
        put(CONFIRMATION_KEY, confirmationKey.getObject());
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
            return param.as(setOf(String.class));
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

        JsonValue confirmationKey = getConfirmationKey();
        if (confirmationKey.isNotNull()) {
            tokenInfo.put(getResourceString(CONFIRMATION_KEY), confirmationKey.getObject());
        }

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
     * {@inheritDoc}
     */
    @Override
    protected void setAuthTime(long authTime) {
        setStringProperty(AUTH_TIME, valueOf(authTime));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAuthTimeSeconds() {
        final String value = getStringProperty(OAuth2Constants.CoreTokenParams.AUTH_TIME);
        return value == null ? TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis()) : Long.parseLong(value);
    }

    @Override
    public String toString() {
        return getTokenId();
    }
}
