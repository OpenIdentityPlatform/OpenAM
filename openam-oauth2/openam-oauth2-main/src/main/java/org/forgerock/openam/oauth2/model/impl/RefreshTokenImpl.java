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
 * "Portions Copyrighted [2012] [ForgeRock Inc]"
 */

package org.forgerock.openam.oauth2.model.impl;

import java.util.HashSet;
import java.util.Set;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.model.RefreshToken;
import org.forgerock.openam.oauth2.model.SessionClient;
import org.forgerock.openam.oauth2.model.Token;

/**
 * Implements a {@link RefreshToken} Token
 */
public class RefreshTokenImpl extends TokenImpl implements RefreshToken {

    private String parent;

    /**
     * Creates a refresh token
     * 
     * @param id
     *            ID of the token
     * @param parent
     *            Parent ID of the refresh token
     * @param userID
     *            UserID of the user creating the token
     * @param client
     *            SessionClient of the client creating the token
     * @param realm
     *            Realm the token is created in
     * @param scope
     *            Scope of the token
     * @param expireTime
     *            Time in seconds until the token expires
     */
    public RefreshTokenImpl(String id, String parent, String userID, SessionClient client,
            String realm, Set<String> scope, long expireTime) {
        super(id, userID, client, realm, scope, expireTime);
        setType();
        setParentToken(parent);
    }

    /**
     * Creates a refresh token
     * 
     * @param id
     *            ID of the token
     * @param scope
     *            Scope of the token
     * @param expireTime
     *            Time in seconds until the token expires
     * @param token
     *            Parent Token of the refresh token
     */
    public RefreshTokenImpl(String id, Set<String> scope, long expireTime, Token token) {
        super(id, token.getUserID(), token.getClient(), token.getRealm(), scope, expireTime);
        setType();
    }

    /**
     * Creates a refresh token.
     * 
     * @param id
     *            ID of the token
     * @param value
     *            A JsonValue map to populate this token with.
     */
    public RefreshTokenImpl(String id, JsonValue value) {
        super(id, value);
        setType();
    }

    /**
     * Set the parent token
     * 
     * @param parent
     *            ID of the parent token
     */
    public void setParentToken(String parent) {
        Set<String> s = new HashSet<String>();
        s.add(parent);
        this.put(OAuth2Constants.StoredToken.PARENT, s);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getParentToken() {
        String parent = null;
        Set parentSet = (Set) get(OAuth2Constants.StoredToken.PARENT).getObject();
        if (parentSet != null && !parentSet.isEmpty()){
            parent = parentSet.iterator().next().toString();
        }
        return parent;
    }

    /**
     * Set the token type
     */
    protected void setType() {
        Set<String> s = new HashSet<String>();
        s.add(OAuth2Constants.Params.REFRESH_TOKEN);
        this.put(OAuth2Constants.StoredToken.TYPE, s);
    }

}
