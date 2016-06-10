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
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.*;
import static org.forgerock.openam.oauth2.OAuth2Constants.JWTTokenParams.ACR;
import static org.forgerock.openam.oauth2.OAuth2Constants.Token.OAUTH_REFRESH_TOKEN;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.oauth2.OAuth2Constants;

/**
 * Models a OAuth2 Refresh Token.
 *
 * @since 12.0.0
 */
public class StatefulRefreshToken extends StatefulToken implements RefreshToken {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("OAuth2CoreToken");

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
     * @param authModules The pipe-separated list of auth modules.
     * @param realm The realm.
     * @param auditId The audit id, used for tracking tokens throughout the audit logs.
     */
    public StatefulRefreshToken(String id, String resourceOwnerId, String clientId, String redirectUri, Set<String> scope,
            long expiryTime, String tokenType, String tokenName, String grantType, String realm,
            String authModules, String acr, String auditId, String authGrantId) {
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
        setRealm(realm);
        setAuditTrackingId(auditId);
    }

    /**
     * Sets the requested claims.
     *
     * @param claims Requested claims
     */
    public void setClaims(String claims) {
        setStringProperty(OAuth2Constants.Custom.CLAIMS, claims);
    }

    /**
     * Gets the requested claims.
     *
     * @return The claims.
     */
    public String getClaims() {
        return getStringProperty(OAuth2Constants.Custom.CLAIMS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setScope(Set<String> scope) {
        put(OAuth2Constants.CoreTokenParams.SCOPE, scope);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setExpiryTime(long expiryTime) {
        put(OAuth2Constants.CoreTokenParams.EXPIRE_TIME, stringToSet(String.valueOf(expiryTime)));
    }

    /**
     * Sets the realm.
     *
     * @param realm The realm.
     */
    private void setRealm(String realm) {
        if (realm == null || realm.isEmpty()) {
            this.setStringProperty(OAuth2Constants.CoreTokenParams.REALM, "/");
        } else {
            this.setStringProperty(OAuth2Constants.CoreTokenParams.REALM, realm);
        }
    }

    /**
     * Gets the realm.
     */
    public String getRealm() {
        return this.getStringProperty(OAuth2Constants.CoreTokenParams.REALM);
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
        final Set<String> value = getParameter(OAuth2Constants.CoreTokenParams.SCOPE);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        return Collections.emptySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getExpiryTime() {
        final Set<String> value = getParameter(OAuth2Constants.CoreTokenParams.EXPIRE_TIME);
        if (value != null && !value.isEmpty()) {
            return Long.parseLong(value.iterator().next());
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setStringProperty(String key, String value) {
        put(key, stringToSet(value));
    }

    @Override
    public String getStringProperty(String key) {
        final Set<String> value = getParameter(key);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;

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
    public AuditConstants.TrackingIdKey getAuditTrackingIdKey() {
        return AuditConstants.TrackingIdKey.OAUTH2_REFRESH;
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

    @Override
    public String toString() {
        return getTokenId();
    }
}
