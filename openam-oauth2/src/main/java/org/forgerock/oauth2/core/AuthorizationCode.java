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
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.REALM;
import static org.forgerock.openam.oauth2.OAuth2Constants.Custom.*;
import static org.forgerock.openam.oauth2.OAuth2Constants.JWTTokenParams.ACR;
import static org.forgerock.openam.oauth2.OAuth2Constants.Token.OAUTH_CODE_TYPE;
import static org.forgerock.openam.utils.Time.currentTimeMillis;

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

/**
 * Models a OAuth2 Authorization Code.
 *
 * @since 12.0.0
 */
public class AuthorizationCode extends JsonValue implements Token {
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("OAuth2CoreToken");

    /**
     * Constructs a new AuthorizationCode backed with the data in the specified JsonValue.
     *
     * @param token The JsonValue of the token.
     * @throws InvalidGrantException If the given token is not an Authorization Code token.
     */
    public AuthorizationCode(JsonValue token) throws InvalidGrantException {
        super(token);
        if (!OAUTH_CODE_TYPE.equals(getTokenName())) {
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
    public AuthorizationCode(String code, String resourceOwnerId, String clientId, String redirectUri, Set<String> scope,
            String claims, long expiryTime, String nonce, String realm, String authModules, String acr,
            String ssoTokenId, String codeChallenge, String codeChallengeMethod, String authGrantId, String auditId) {
        super(new HashMap<String, Object>());
        setStringProperty(ID, code);
        setStringProperty(USERNAME, resourceOwnerId);
        setStringProperty(CLIENT_ID, clientId);
        setStringProperty(REDIRECT_URI, redirectUri);
        setStringProperty(EXPIRE_TIME, String.valueOf(expiryTime));
        put(SCOPE, scope);
        setStringProperty(TOKEN_TYPE, "Bearer");
        setStringProperty(TOKEN_NAME, OAUTH_CODE_TYPE);
        setStringProperty(NONCE, nonce);
        setStringProperty(AUTH_MODULES, authModules);
        setStringProperty(ACR, acr);
        setStringProperty(CODE_CHALLENGE, codeChallenge);
        setStringProperty(CODE_CHALLENGE_METHOD, codeChallengeMethod);
        setStringProperty(AUTH_GRANT_ID, authGrantId);
        setStringProperty(REALM, realm == null || realm.isEmpty() ? "/" : realm);
        setStringProperty(SSO_TOKEN_ID, ssoTokenId);
        put(CLAIMS, CollectionUtils.asSet(claims));
        setStringProperty(AUDIT_TRACKING_ID, auditId);
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
        final Set<String> value = getParameter(EXPIRE_TIME);
        if (value != null && !value.isEmpty()) {
            return Long.parseLong(value.iterator().next());
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public String getTokenId() {
        return getStringProperty(ID);
    }

    /**
     * Gets the token type.
     *
     * @return The token type.
     */
    public String getTokenType() {
        return getStringProperty(TOKEN_TYPE);
    }

    /**
     * {@inheritDoc}
     */
    public String getTokenName() {
        return getStringProperty(TOKEN_NAME);
    }

    /**
     * Gets the scope.
     *
     * @return The scope.
     */
    public Set<String> getScope() {
        final Set<String> scope = getParameter(OAuth2Constants.CoreTokenParams.SCOPE);
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
        Set<String> issued = getParameter(ISSUED);
        return issued != null && Boolean.parseBoolean(issued.iterator().next());
    }

    /**
     * Gets the redirect uri.
     *
     * @return The redirect uri.
     */
    public String getRedirectUri() {
        return getStringProperty(REDIRECT_URI);
    }

    /**
     * Gets the client's id.
     *
     * @return The client's id.
     */
    public String getClientId() {
        return getStringProperty(CLIENT_ID);
    }

    /**
     * Gets the resource owner's id.
     *
     * @return The resource owner's id.
     */
    public String getResourceOwnerId() {
        return getStringProperty(USERNAME);
    }

    /**
     * Get the auth modules string.
     * @return The pipe-separated list of auth modules.
     */
    public String getAuthModules() {
        return getStringProperty(AUTH_MODULES);
    }

    /**
     * Get the Authentication Context Class Reference (acr).
     * @return The acr string matched, if any.
     */
    public String getAuthenticationContextClassReference() {
        return getStringProperty(ACR);
    }

    /**
     * Gets the nonce.
     *
     * @return The nonce.
     */
    public String getNonce() {
        return getStringProperty(NONCE);
    }

    /**
     * Gets the session id of the authenticating session.
     *
     * @return The session id.
     */
    public String getSessionId() {
        return getStringProperty(SSO_TOKEN_ID);
    }

    /**
     * Sets the authorization code as issued.
     */
    public void setIssued() {
        setStringProperty(OAuth2Constants.CoreTokenParams.ISSUED, "true");
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

    private String getStringProperty(String key) {
        final Set<String> value = getParameter(key);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;

    }

    private void setStringProperty(String key, String value) {
        put(key, stringToSet(value));
    }

    /**
     * Gets the display String for the given String.
     *
     * @param s The String.
     * @return The display String.
     */
    private String getResourceString(final String s) {
        return RESOURCE_BUNDLE.getString(s);
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Object> toMap() {
        final Map<String, Object> tokenMap = new HashMap<String, Object>();
        tokenMap.put(getResourceString(TOKEN_TYPE), getTokenType());
        tokenMap.put(getResourceString(OAuth2Constants.CoreTokenParams.EXPIRE_TIME),
                (getExpiryTime() - currentTimeMillis()) / 1000);
        return tokenMap;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Object> getTokenInfo() {
        final Map<String, Object> tokenInfo = new HashMap<String, Object>();
        tokenInfo.put(getResourceString(TOKEN_TYPE), getTokenType());
        tokenInfo.put(getResourceString(OAuth2Constants.CoreTokenParams.EXPIRE_TIME),
                (getExpiryTime() - currentTimeMillis()) / 1000);
        tokenInfo.put(getResourceString(OAuth2Constants.CoreTokenParams.SCOPE), getScope());
        tokenInfo.put(RESOURCE_BUNDLE.getString(REALM), getRealm());
        return tokenInfo;
    }

    /**
     * Get the code challenge
     * @return code challenge
     */
    public String getCodeChallenge() {
        return getStringProperty(CODE_CHALLENGE);
    }

    /**
     * Get the code challenge method
     * @return code challenge method
     */
    public String getCodeChallengeMethod() {
        return getStringProperty(CODE_CHALLENGE_METHOD);
    }

    /**
     * Gets the authorization grant id
     *
     * @return The authorization grant id
     */
    public String getAuthGrantId() { return getStringProperty(AUTH_GRANT_ID); }

    /**
     * {@inheritDoc}
     */
    public JsonValue toJsonValue() {
        return this;
    }

    @Override
    public String getAuditTrackingId() {
        return getStringProperty(AUDIT_TRACKING_ID);
    }

    @Override
    public AuditConstants.TrackingIdKey getAuditTrackingIdKey() {
        return AuditConstants.TrackingIdKey.OAUTH2_GRANT;
    }

    /**
     * Returns the requested claims.
     *
     * @return The requested claims.
     */
    public String getClaims() {
        return getStringProperty(OAuth2Constants.Custom.CLAIMS);
    }

    /**
     * Gets the realm.
     *
     * @return The realm.
     */
    public String getRealm() {
        return getStringProperty(REALM);
    }

}
