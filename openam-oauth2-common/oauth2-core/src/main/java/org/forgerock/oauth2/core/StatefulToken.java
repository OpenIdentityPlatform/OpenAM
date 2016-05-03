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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import org.forgerock.json.JsonValue;

import java.util.Collections;
import java.util.Set;

import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.ID;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.USERNAME;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.CLIENT_ID;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.REDIRECT_URI;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.SCOPE;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.EXPIRE_TIME;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.TOKEN_TYPE;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.TOKEN_NAME;
import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.AUDIT_TRACKING_ID;
import static org.forgerock.oauth2.core.OAuth2Constants.Custom.CLAIMS;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.GRANT_TYPE;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.REALM;
import static org.forgerock.openam.utils.Time.currentTimeMillis;

public abstract class StatefulToken extends JsonValue {

    public StatefulToken(Object object) {
        super(object);
    }

    /**
     * Sets the token id.
     *
     * @param id The token id.
     */
    protected void setId(String id) {
        setStringProperty(ID, id);
    }

    /**
     * Sets the resource owner's id.
     *
     * @param resourceOwnerId The resource owner's id.
     */
    protected void setResourceOwnerId(String resourceOwnerId) {
        setStringProperty(USERNAME, resourceOwnerId);
    }

    /**
     * Sets the client's id.
     *
     * @param clientId The client's id.
     */
    protected void setClientId(String clientId) {
        setStringProperty(CLIENT_ID, clientId);
    }

    /**
     * Sets the redirect uri.
     *
     * @param redirectUri The redirect uri.
     */
    protected void setRedirectUri(String redirectUri) {
        setStringProperty(REDIRECT_URI, redirectUri);
    }

    /**
     * Sets the scope.
     *
     * @param scope The scope.
     */
    protected void setScope(Set<String> scope) {
        put(SCOPE, scope);
    }

    /**
     * Sets the expiry time.
     *
     * @param expiryTime The expiry time.
     */
    protected void setExpiryTime(long expiryTime) {
        put(EXPIRE_TIME, expiryTime);
    }

    /**
     * Sets the token type.
     *
     * @param tokenType The token type.
     */
    protected void setTokenType(String tokenType) {
        setStringProperty(TOKEN_TYPE, tokenType);
    }

    /**
     * Sets the token name.
     *
     * @param tokenName The token name.
     */
    protected void setTokenName(String tokenName) {
        setStringProperty(TOKEN_NAME, tokenName);
    }

    /**
     * Sets the grant type.
     *
     * @param grantType The grant type.
     */
    protected void setGrantType(String grantType) {
        setStringProperty(GRANT_TYPE, grantType);
    }

    /**
     * Gets the scope.
     *
     * @return The scope.
     */
    public Set<String> getScope() {
        return getSetProperty(SCOPE);
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

    public String getTokenId() {
        return getStringProperty(ID);
    }

    public String getTokenName() {
        return getStringProperty(TOKEN_NAME);
    }

    public String getRealm() {
        return getStringProperty(REALM);
    }

    /**
     * Gets the requested claims associated w/ this access token.
     *
     * @return Requested claims (JSON as a String).
     */
    public String getClaims() {
        return getStringProperty(CLAIMS);
    }

    /**
     * Gets the token type.
     *
     * @return The token type.
     */
    public String getTokenType() {
        return getStringProperty(TOKEN_TYPE);
    }

    public String getAuditTrackingId() {
        return getStringProperty(AUDIT_TRACKING_ID);
    }

    /**
     * Gets the display String for the given String.
     *
     * @param string The String.
     * @return The display String.
     */
    protected String getResourceString(String string) {
        return string;
    }

    /**
     * Get a string property from the store.
     * @param key The property key.
     * @return The value.
     */
    protected String getStringProperty(String key) {
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

    public JsonValue toJsonValue() {
        return this;
    }

    protected Long getTimeLeft() {
        return (getExpiryTime() - currentTimeMillis()) / 1000;
    }

    public long getExpiryTime() {
        if (isDefined(EXPIRE_TIME)) {
            return get(EXPIRE_TIME).asLong();
        }
        return defaultExpireTime();
    }

    abstract protected long defaultExpireTime();

    /**
     * Determines if the Access Token is expired.
     *
     * @return {@code true} if current time is greater than the expiry time.
     */
    public boolean isExpired() {
        return currentTimeMillis() > getExpiryTime();
    }
}
