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
 * Copyright 20142-2015 ForgeRock AS.
 */

package org.forgerock.openam.oauth2;

import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.*;
import static org.forgerock.oauth2.core.Utils.*;

import java.util.Collections;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;

/**
 * Models a OpenAM OAuth2 access token.
 *
 * @since 12.0.0
 */
public class OpenAMAccessToken extends AccessToken {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("OAuth2CoreToken");

    /**
     * Constructs a new OpenAMAccessToken backed with the data in the specified JsonValue.
     *
     * @param token The JsonValue of the token.
     * @throws InvalidGrantException If the given token is not an Access Token.
     */
    public OpenAMAccessToken(JsonValue token) throws InvalidGrantException {
        super(token);
    }

    /**
     * Constructs a new OpenAMAccessToken backed with the data in the specified JsonValue.
     *
     * @param token The JsonValue of the token.
     * @param tokenName The token name.
     * @param tokenId The token identifier.
     * @throws InvalidGrantException If the given token is not an Access Token.
     */
    public OpenAMAccessToken(JsonValue token, String tokenName, String tokenId) throws InvalidGrantException {
        super(token, tokenName, tokenId);
    }

    /**
     * Constructs a new OpenAMAccessToken.
     *
     * @param id                The token id.
     * @param authorizationCode The authorization code.
     * @param resourceOwnerId   The resource owner's id.
     * @param clientId          The client's id.
     * @param redirectUri       The redirect uri.
     * @param scope             The scope.
     * @param expiryTime        The expiry time.
     * @param refreshTokenId    The refresh token id.
     * @param tokenName         The token name.
     * @param grantType         The grant type.
     * @param nonce             The nonce.
     * @param realm             The realm.
     * @param claims            The requested claims.
     */
    public OpenAMAccessToken(String id, String authorizationCode, String resourceOwnerId, String clientId,
                             String redirectUri, Set<String> scope, long expiryTime, String refreshTokenId,
                             String tokenName, String grantType, String nonce, String realm, String claims) {
        super(id, authorizationCode, resourceOwnerId, clientId, redirectUri, scope, expiryTime, refreshTokenId,
                tokenName, grantType, nonce);
        setRealm(realm);

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
    protected void setAuthorizationCode(String authorizationCode) {
        put(PARENT, stringToSet(authorizationCode));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setResourceOwnerId(String resourceOwnerId) {
        put(USERNAME, stringToSet(resourceOwnerId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setClientId(String clientId) {
        put(CLIENT_ID, stringToSet(clientId));
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
    protected void setRefreshTokenId(String refreshToken) {
        put(REFRESH_TOKEN, stringToSet(refreshToken));
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
     * {@inheritDoc}
     */
    @Override
    protected void setNonce(String nonce) {
        put(OAuth2Constants.Custom.NONCE, stringToSet(nonce));
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
    public String getNonce() {
        final Set<String> value = getParameter(OAuth2Constants.Custom.NONCE);
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
     * {@inheritDoc}
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getTokenInfo() {
        final Map<String, Object> tokenInfo = super.getTokenInfo();
        tokenInfo.put(getResourceString(REALM), getRealm());
        return tokenInfo;
    }

}
