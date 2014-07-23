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

package org.forgerock.openam.oauth2;

import org.forgerock.oauth2.core.AuthorizationCode;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;

import java.util.Collections;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import static org.forgerock.oauth2.core.Utils.stringToSet;

/**
 * Models a OpenAm OAuth2 Authorization Code.
 *
 * @since 12.0.0
 */
public class OpenAMAuthorizationCode extends AuthorizationCode {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("OAuth2CoreToken");

    /**
     * Constructs a new OpenAMAuthorizationCode backed with the data in the specified JsonValue.
     *
     * @param token The JsonValue of the token.
     * @throws InvalidGrantException If the given token is not an Authorization Code token.
     */
    protected OpenAMAuthorizationCode(JsonValue token) throws InvalidGrantException {
        super(token);
    }

    /**
     * Constructs a new OpenAMAuthorizationCode.
     *
     * @param code The authorization code.
     * @param resourceOwnerId The resource owner's id.
     * @param clientId The client's id.
     * @param redirectUri The redirect uri.
     * @param scope The scopes.
     * @param expiryTime The expiry time.
     * @param nonce The nonce.
     * @param realm The realm.
     */
    OpenAMAuthorizationCode(String code, String resourceOwnerId, String clientId, String redirectUri, Set<String> scope,
            long expiryTime, String nonce, String realm) {
        super(code, resourceOwnerId, clientId, redirectUri, scope, expiryTime, nonce);
        setRealm(realm);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setCode(String code) {
        put(OAuth2Constants.CoreTokenParams.ID, stringToSet(code));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setResourceOwnerId(String resourceOwnerId) {
        put(OAuth2Constants.CoreTokenParams.USERNAME, stringToSet(resourceOwnerId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setClientId(String clientId) {
        put(OAuth2Constants.CoreTokenParams.CLIENT_ID, stringToSet(clientId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setRedirectUri(String redirectUri) {
        put(OAuth2Constants.CoreTokenParams.REDIRECT_URI, stringToSet(redirectUri));
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
     * {@inheritDoc}
     */
    @Override
    protected void setTokenType(String tokenType) {
        put(OAuth2Constants.CoreTokenParams.TOKEN_TYPE, stringToSet(tokenType));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setTokenName(String tokenName) {
        put(OAuth2Constants.CoreTokenParams.TOKEN_NAME, stringToSet(tokenName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setNonce(String nonce) {
        put(OAuth2Constants.Custom.NONCE, stringToSet(nonce));
    }

    /**
     * Sets the realm.
     * <br/>
     * If the specified realm is {@code null} or an empty String, '/' is used instead.
     *
     * @param realm The realm.
     */
    private void setRealm(String realm) {
        if (realm == null || realm.isEmpty()) {
            this.put(OAuth2Constants.CoreTokenParams.REALM, stringToSet("/"));
        } else {
            this.put(OAuth2Constants.CoreTokenParams.REALM, stringToSet(realm));
        }
    }

    /**
     * Gets the realm.
     *
     * @return The realm.
     */
    public String getRealm() {
        final Set<String> value = getParameter(OAuth2Constants.CoreTokenParams.REALM);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getTokenInfo() {
        final Map<String, Object> tokenInfo = super.getTokenInfo();
        tokenInfo.put(RESOURCE_BUNDLE.getString(OAuth2Constants.CoreTokenParams.REALM), getRealm());
        return tokenInfo;
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
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTokenId() {
        final Set<String> value = getParameter(OAuth2Constants.CoreTokenParams.ID);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTokenType() {
        final Set<String> value = getParameter(OAuth2Constants.CoreTokenParams.TOKEN_TYPE);
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
        final Set<String> value = getParameter(OAuth2Constants.CoreTokenParams.TOKEN_NAME);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
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
    public boolean isIssued() {
        if (getParameter(OAuth2Constants.CoreTokenParams.ISSUED) != null) {
            return Boolean.parseBoolean(getParameter(OAuth2Constants.CoreTokenParams.ISSUED).iterator().next());
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRedirectUri() {
        final Set<String> value = getParameter(OAuth2Constants.CoreTokenParams.REDIRECT_URI);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getClientId() {
        final Set<String> value = getParameter(OAuth2Constants.CoreTokenParams.CLIENT_ID);
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
        final Set<String> value = getParameter(OAuth2Constants.CoreTokenParams.USERNAME);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIssued() {
        this.put(OAuth2Constants.CoreTokenParams.ISSUED, stringToSet("true"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNonce() {
        final Set<String> value = getParameter(OAuth2Constants.Custom.NONCE);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
    }
}
