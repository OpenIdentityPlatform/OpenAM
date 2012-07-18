/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openam.oauth2.model.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.restlet.ext.oauth2.OAuth2;
import org.forgerock.restlet.ext.oauth2.model.SessionClient;
import org.forgerock.restlet.ext.oauth2.model.Token;

/**
 * TODO Description.
 */
public abstract class TokenImpl extends JsonValue implements Token {

    private String id;

    /**
     * Constructor that sets common values in the token object.
     * 
     * @param id
     *            the ID of the token, kept out of the JsonValue
     * @param userID
     *            the userID
     * @param client
     *            the client object (id and redirect URI)
     * @param realm
     *            the realm that governs this token
     * @param scope
     *            the set of scopes
     * @param expiresIn
     *            the number of seconds from message generation time that this
     *            token is valid
     */
    protected TokenImpl(String id, String userID, SessionClient client, String realm,
            Set<String> scope, long expiresIn) {
        super(new HashMap<String, Object>());

        this.id = id;

        setUserID(userID);
        setClient(client);
        setRealm(realm);
        setScope(scope);

        // The expiresIn value is a count of the number of seconds that this
        // token should be valid
        // for.
        // For storage purposes, the token should store the time when the token
        // will expire, so that
        // later retrieval
        // will allow the expires_in value to be easily and accurately
        // calculated. The expiresIn
        // value should therefore
        // be translated to a more absolute value.
        setAbsoluteExpiryTime(calculateAbsoluteExpiry(expiresIn));
    }

    /**
     * Converts a countdown-style lifetime in seconds to a more absolute expiry
     * time suitable for storage.
     * 
     * @param expiresIn
     *            lifetime of token in seconds from time of generation
     * @return expiry time in milliseconds relative to the last epoch
     */
    private long calculateAbsoluteExpiry(long expiresIn) {
        return System.currentTimeMillis() + expiresIn * 1000; // Seconds to
                                                              // milliseconds
    }

    /**
     * Constructs a TokenImpl object using the values in a JsonValue object and
     * an associated ID.
     * 
     * @param id
     *            the ID of the token as used when storing/modifying/retrieving
     *            the object
     * @param value
     *            the JSON object containing the values for this object
     */
    protected TokenImpl(String id, JsonValue value) {
        super(value);
        this.id = id;
    }

    private Set<String> convertScope(List<Object> scopeList) {
        Set<String> scopeSet = new HashSet<String>();
        for (Object o : scopeList) {
            scopeSet.add(o.toString());
        }
        return scopeSet;
    }

    /**
     * TODO Description.
     * 
     * @param userID
     *            TODO Description
     */
    public void setUserID(String userID) {
        this.put(OAuth2.Params.USERNAME, userID);
    }

    /**
     * TODO Description.
     * 
     * @param realm
     *            TODO Description
     */
    public void setRealm(String realm) {
        this.put(OAuth2.Params.REALM, realm == null ? "/" : new String(realm));
    }

    /**
     * TODO Description.
     * 
     * @param client
     *            TODO Description
     */
    public void setClient(SessionClient client) {
        if (client != null) {
            this.put(OAuth2.Params.CLIENT_ID, client.getClientId());
            this.put(OAuth2.Params.REDIRECT_URI, client.getRedirectUri());
        }
    }

    /**
     * TODO Description.
     * 
     * @param scope
     *            TODO Description
     */
    public void setScope(Set<String> scope) {
        if (scope == null) {
            scope = Collections.emptySet();
        }
        this.put(OAuth2.Params.SCOPE, scope);
    }

    /**
     * TODO Description.
     * 
     * @param expiryTime
     *            TODO Description
     */
    public void setAbsoluteExpiryTime(long expiryTime) {
        this.put(OAuth2.StoredToken.EXPIRY_TIME, expiryTime);
    }

    @Override
    public String getToken() {
        return id;
    }

    @Override
    public String getUserID() {
        return this.get(OAuth2.Params.USERNAME).asString();
    }

    @Override
    public String getRealm() {
        return this.get(OAuth2.Params.REALM).asString();
    }

    @Override
    public SessionClient getClient() {
        return new SessionClientImpl(this.get(OAuth2.Params.CLIENT_ID).asString(), this.get(
                OAuth2.Params.REDIRECT_URI).asString());
    }

    @Override
    public Set<String> getScope() {
        // return convertScope(this.get(OAuth2.Params.SCOPE).asList());
        return (Set<String>) this.get(OAuth2.Params.SCOPE).getObject();
    }

    @Override
    public long getExpireTime() {
        return (getAbsoluteExpiryTime() - System.currentTimeMillis()) / 1000;
    }

    /**
     * Returns the expiry time as stored.
     * 
     * @return time of expiry expressed as milliseconds since the epoch.
     */
    public long getAbsoluteExpiryTime() {
        return get(OAuth2.StoredToken.EXPIRY_TIME).required().asLong();
    }

    @Override
    public boolean isExpired() {
        return (System.currentTimeMillis() > getAbsoluteExpiryTime());
    }

    /**
     * Presents the "type" parameter of the token.
     * 
     * @return the OAuth2 token type.
     */
    public String getType() {
        return this.get(OAuth2.Params.TOKEN_TYPE).asString();
    }

}
