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
 * "Portions Copyrighted [2012] [Forgerock Inc]"
 */

package org.forgerock.openam.oauth2.model.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.fluent.JsonValue;
import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.oauth2.model.AccessToken;
import org.forgerock.openam.oauth2.model.Token;
import org.forgerock.openam.oauth2.model.SessionClient;

/**
 * Implementation of AccessToken
 */
public class AccessTokenImpl extends TokenImpl implements AccessToken {

    /**
     * Constructor. Creates an Access Token
     * 
     * @param id
     *            Id of the access token
     * @param parent
     *            Id of the parent token
     * @param userID
     *            UserID of the user creating the token
     * @param client
     *            The sessionClient of the client creating the token
     * @param realm
     *            The realm this token is created in
     * @param scope
     *            The scope of this token
     * @param expireTime
     *            The amount of time in seconds this token will expire in
     */
    public AccessTokenImpl(String id, String parent, String userID, SessionClient client,
            String realm, Set<String> scope, long expireTime) {
        super(id, userID, client, realm, scope, expireTime);
        setType();
        setParentToken(parent);
    }

    /**
     * Constructor. Creates an Access Token
     * 
     * @param id
     *            Id of the access token
     * @param scope
     *            The scope of this token
     * @param expireTime
     *            The amount of time in seconds this token will expire in
     * @param token
     *            The parent token of this token
     */
    public AccessTokenImpl(String id, Set<String> scope, long expireTime, Token token) {
        super(id, token.getUserID(), token.getClient(), token.getRealm(), scope, expireTime);
        setType();
        setParentToken(token.getToken());
    }

    /**
     * Creates an Access Token
     * 
     * @param id
     *            Id of the access Token
     * @param value
     *            A JsonValue map to populate this token with.
     */
    public AccessTokenImpl(String id, JsonValue value) {
        super(id, value);
        setType();
    }

    /**
     * Sets the parent token
     * 
     * @param parent
     *            The parent token
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
     * {@inheritDoc}
     */
    @Override
    public String getRefreshToken() {
        //refresh tokens are stored as parent tokens
        return getParentToken();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> convertToMap() {
        Map<String, Object> tokenMap = new HashMap<String, Object>();
        tokenMap.put(OAuth2Constants.Params.ACCESS_TOKEN, getToken());
        tokenMap.put(OAuth2Constants.Params.TOKEN_TYPE, OAuth2Constants.Bearer.BEARER);
        tokenMap.put(OAuth2Constants.Params.EXPIRES_IN, getExpireTime());
        tokenMap.put(OAuth2Constants.Params.USERNAME, getUserID());
        tokenMap.put(OAuth2Constants.Params.REALM, getRealm());
        tokenMap.put(OAuth2Constants.Params.REDIRECT_URI, getClient().getRedirectUri());
        tokenMap.put(OAuth2Constants.Params.CLIENT_ID, getClient().getClientId());
        tokenMap.put(OAuth2Constants.Params.SCOPE, getScope());
        return tokenMap;
    }

    /**
     * Sets the type of the token
     */
    protected void setType() {
        Set<String> s = new HashSet<String>();
        s.add(OAuth2Constants.Params.ACCESS_TOKEN);
        this.put(OAuth2Constants.StoredToken.TYPE, s);
    }

}
