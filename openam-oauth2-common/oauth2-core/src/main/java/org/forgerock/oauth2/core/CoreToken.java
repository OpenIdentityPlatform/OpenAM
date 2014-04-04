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
 * Copyright 2012-2014 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import org.forgerock.json.fluent.JsonValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import static org.forgerock.oauth2.core.Utils.stringToSet;

/**
 * Encapsulates the data of a OAuth2 token.
 *
 * @since 11.0.0
 */
public class CoreToken extends JsonValue implements Token {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("OAuth2CoreToken");
    private String id;

    /**
     * Constructs a new, empty, CoreToken.
     */
    public CoreToken() {
        super(new HashMap<String, Object>());
    }

    /**
     * Constructs a new CoreToken with the specified identifier and content.
     *
     * @param id The identifier.
     * @param value The context of the token.
     */
    public CoreToken(final String id, final JsonValue value) {
        super(value);
        this.id = id;
    }

    /**
     * Constructs a new CoreToken with the specified identifier and content.
     *
     * @param id The identifier.
     * @param userName The username.
     * @param realm The realm.
     * @param expireTime The expire time.
     * @param tokenType The token type.
     * @param tokenName The token name.
     * @param nonce The nonce.
     * @param grantType The grant type.
     */
    public CoreToken(final String id, final String userName, final String realm, final long expireTime,
            final String tokenType, final String tokenName, final String nonce, final String grantType) {
        super(new HashMap<String, Object>());
        setTokenID(id);
        setUserName(userName);
        setRealm(realm);
        setExpireTime(expireTime);
        setTokenType(tokenType);
        setTokenName(tokenName);
        setNonce(nonce);
        setGrantType(grantType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> convertToMap() {
        final Map<String, Object> tokenMap = new HashMap<String, Object>();
        tokenMap.put(RESOURCE_BUNDLE.getString(OAuth2Constants.CoreTokenParams.TOKEN_TYPE),
                getParameter(OAuth2Constants.CoreTokenParams.TOKEN_TYPE));
        tokenMap.put(RESOURCE_BUNDLE.getString(OAuth2Constants.CoreTokenParams.EXPIRE_TIME),
                (System.currentTimeMillis() - getExpireTime()) / 1000);
        return tokenMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getTokenInfo() {
        final Map<String, Object> tokenMap = new HashMap<String, Object>();
        tokenMap.put(RESOURCE_BUNDLE.getString(OAuth2Constants.CoreTokenParams.TOKEN_TYPE), getTokenType());
        tokenMap.put(RESOURCE_BUNDLE.getString(OAuth2Constants.CoreTokenParams.EXPIRE_TIME),
                (System.currentTimeMillis() - getExpireTime()) / 1000);
        tokenMap.put(RESOURCE_BUNDLE.getString(OAuth2Constants.CoreTokenParams.REALM), getRealm());
        tokenMap.put(RESOURCE_BUNDLE.getString(OAuth2Constants.CoreTokenParams.SCOPE), getScope());
        return tokenMap;
    }

    /**
     * Sets the tokens identifier.
     *
     * @param id The identifier.
     */
    protected void setTokenID(final String id) {
        this.id = id;
        this.put(OAuth2Constants.CoreTokenParams.ID, stringToSet(id));
    }

    /**
     * Sets the user name.
     *
     * @param userName The username.
     */
    protected void setUserName(final String userName) {
        this.put(OAuth2Constants.CoreTokenParams.USERNAME, stringToSet(userName));
    }

    /**
     * Sets the realm.
     *
     * @param realm The realm.
     */
    protected void setRealm(final String realm) {
        if (realm == null || realm.isEmpty()) {
            this.put(OAuth2Constants.CoreTokenParams.REALM, stringToSet("/"));
        } else {
            this.put(OAuth2Constants.CoreTokenParams.REALM, stringToSet(realm));
        }
    }

    /**
     * Sets the expire time.
     *
     * @param expireTime The expire time.
     */
    protected void setExpireTime(final long expireTime) {
        this.put(OAuth2Constants.CoreTokenParams.EXPIRE_TIME,
                stringToSet(String.valueOf((expireTime * 1000) + System.currentTimeMillis())));
    }

    /**
     * Sets the nonce.
     *
     * @param nonce The nonce.
     */
    protected void setNonce(final String nonce) {
        this.put(OAuth2Constants.Custom.NONCE, stringToSet(nonce));
    }

    /**
     * Sets the token type.
     *
     * @param tokenType The token type.
     */
    protected void setTokenType(final String tokenType) {
        this.put(OAuth2Constants.CoreTokenParams.TOKEN_TYPE, stringToSet(tokenType));
    }

    /**
     * Sets the token name.
     *
     * @param tokenName The token name.
     */
    protected void setTokenName(final String tokenName) {
        this.put(OAuth2Constants.CoreTokenParams.TOKEN_NAME, stringToSet(tokenName));
    }

    /**
     * Sets the grant type.
     *
     * @param grantType The grant type.
     */
    protected void setGrantType(final String grantType) {
        this.put(OAuth2Constants.Params.GRANT_TYPE, stringToSet(grantType));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTokenID() {
        if (id != null) {
            return id;
        } else {
            final JsonValue val = this.get(OAuth2Constants.CoreTokenParams.ID);
            if (val != null) {
                id = val.asString();
                return val.asString();
            }
        }
        return null;
    }

    /**
     * Sets the nonce.
     *
     * @return The nonce.
     */
    public String getNonce() {
        final Set<String> value = this.getParameter(OAuth2Constants.Custom.NONCE);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
    }

    /**
     * Gets the parent token.
     *
     * @return The id of the parent token.
     */
    public String getParent() {
        final Set<String> value = this.getParameter(OAuth2Constants.CoreTokenParams.PARENT);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
    }

    /**
     * Gets the issued state for code type.
     *
     * @return true or false if issued or not.
     */
    public boolean isIssued() {
        if (this.getParameter(OAuth2Constants.CoreTokenParams.ISSUED) != null) {
            return Boolean.parseBoolean(this.getParameter(OAuth2Constants.CoreTokenParams.ISSUED).iterator().next());
        } else {
            return false;
        }
    }

    /**
     * Gets the refresh token id.
     *
     * @return The id of refresh token.
     */
    public String getRefreshToken() {
        final Set<String> value = this.getParameter(OAuth2Constants.CoreTokenParams.REFRESH_TOKEN);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserID() {
        final Set<String> value = this.getParameter(OAuth2Constants.CoreTokenParams.USERNAME);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRealm() {
        final Set<String> value = this.getParameter(OAuth2Constants.CoreTokenParams.REALM);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getExpireTime() {
        final Set<String> value = this.getParameter(OAuth2Constants.CoreTokenParams.EXPIRE_TIME);
        if (value != null && !value.isEmpty()) {
            return Long.parseLong(value.iterator().next());
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getScope() {
        final Set<String> value = this.getParameter(OAuth2Constants.CoreTokenParams.SCOPE);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        return Collections.emptySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isExpired() {
        return (System.currentTimeMillis() > getExpireTime());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTokenType() {
        final Set<String> value = this.getParameter(OAuth2Constants.CoreTokenParams.TOKEN_TYPE);
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
        final Set<String> value = this.getParameter(OAuth2Constants.CoreTokenParams.TOKEN_NAME);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getClientID() {
        final Set<String> value = this.getParameter(OAuth2Constants.CoreTokenParams.CLIENT_ID);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
    }

    /**
     * Returns the redirect_uri associated token.
     *
     * @return The redirect_uri associated with token.
     */
    public String getRedirectURI() {
        final Set<String> value = this.getParameter(OAuth2Constants.CoreTokenParams.REDIRECT_URI);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
    }
    /**
     * Gets any parameter stored in the token.
     *
     * @param paramName The parameter name.
     * @return The parameter stored in the token.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getParameter(String paramName) {
        final JsonValue param = get(paramName);
        if (param != null) {
            return (Set<String>) param.getObject();
        }
        return null;
    }

    /**
     * Whether the token has been issued.
     *
     * @return {@code true} if the token has been issued.
     */
    public String getIssued() {
        final Set<String> value = this.getParameter(OAuth2Constants.CoreTokenParams.ISSUED);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
    }

    /**
     * Sets the token as have being issued.
     */
    public void setIssued() {
        this.put(OAuth2Constants.CoreTokenParams.ISSUED, stringToSet("true"));
    }

    /**
     * Gets the grant type.
     *
     * @return The grant type.
     */
    public String getGrantType() {
        final Set<String> value = this.getParameter(OAuth2Constants.Params.GRANT_TYPE);
        if (value != null && !value.isEmpty()) {
            return value.iterator().next();
        }
        return null;
    }
}
