/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock Inc. All rights reserved.
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
 * "Portions Copyrighted [year] [name of company]"
 */

package org.forgerock.openam.oauth2.model;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;

/**
 * Implementation of BearerToken
 */
public class BearerToken extends CoreToken {

    private static ResourceBundle rb = ResourceBundle.getBundle("OAuth2CoreToken");

    /**
     * Constructor. Creates an Bearer Access Token
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
    public BearerToken(String id, String parent, String userID, SessionClient client,
                       String realm, Set<String> scope, long expireTime) {
        super(id, userID, realm, expireTime, OAuth2Constants.Bearer.BEARER, OAuth2Constants.Token.OAUTH_ACCESS_TOKEN);
        super.put(OAuth2Constants.CoreTokenParams.SCOPE, scope);
        super.put(OAuth2Constants.CoreTokenParams.CLIENT_ID, OAuth2Utils.stringToSet(client.getClientId()));
        super.put(OAuth2Constants.CoreTokenParams.REDIRECT_URI, OAuth2Utils.stringToSet(client.getRedirectUri()));
        super.put(OAuth2Constants.CoreTokenParams.PARENT, OAuth2Utils.stringToSet(parent));
    }

    /**
     * Constructor. Creates an Bearer Access Code Token
     *
     * @param id
     *            Id of the access token
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
    public BearerToken(String id, String userID, SessionClient client,
                       String realm, Set<String> scope, long expireTime, String issued) {
        super(id, userID, realm, expireTime, OAuth2Constants.Bearer.BEARER, OAuth2Constants.Token.OAUTH_CODE_TYPE);
        super.put(OAuth2Constants.CoreTokenParams.SCOPE, scope);
        super.put(OAuth2Constants.CoreTokenParams.CLIENT_ID, OAuth2Utils.stringToSet(client.getClientId()));
        super.put(OAuth2Constants.CoreTokenParams.REDIRECT_URI, OAuth2Utils.stringToSet(client.getRedirectUri()));
        super.put(OAuth2Constants.CoreTokenParams.ISSUED, OAuth2Utils.stringToSet(issued));
    }

    /**
     * Creates a Bearer Token
     * @param id
     *          id of the token
     * @param parent
     *          id of the parent token
     * @param userID
     *          UserID of the user creating the token
     * @param client
     *          The sessionClient of the client creating the token
     * @param realm
     *          The realm this token is created in
     * @param scope
     *          The scope of this token
     * @param expireTime
     *          The amount of time in seconds this token will expire in
     * @param tokenType
     *          The type of this token. Refresh, Access, Code
     */
    public BearerToken(String id, String parent, String userID, SessionClient client,
                       String realm, Set<String> scope, long expireTime, String tokenType) {
        super(id, userID, realm, expireTime, OAuth2Constants.Bearer.BEARER, tokenType);
        super.put(OAuth2Constants.CoreTokenParams.SCOPE, scope);
        super.put(OAuth2Constants.CoreTokenParams.CLIENT_ID, OAuth2Utils.stringToSet(client.getClientId()));
        super.put(OAuth2Constants.CoreTokenParams.REDIRECT_URI, OAuth2Utils.stringToSet(client.getRedirectUri()));
        super.put(OAuth2Constants.CoreTokenParams.PARENT, OAuth2Utils.stringToSet(parent));
    }

    /**
     * Creates a Bearer Token
     * @param id
     *          id of the token
     * @param parent
     *          id of the parent token
     * @param userID
     *          UserID of the user creating the token
     * @param client
     *          The sessionClient of the client creating the token
     * @param realm
     *          The realm this token is created in
     * @param scope
     *          The scope of this token
     * @param expireTime
     *          The amount of time in seconds this token will expire in
     * @param tokenType
     *          The type of this token. Refresh, Access, Code
     * @param refreshToken
     *          The id of the refresh token
     */
    public BearerToken(String id, String parent, String userID, SessionClient client,
                       String realm, Set<String> scope, long expireTime, String refreshToken, String tokenType) {
        super(id, userID, realm, expireTime, OAuth2Constants.Bearer.BEARER, tokenType);
        super.put(OAuth2Constants.CoreTokenParams.SCOPE, scope);
        super.put(OAuth2Constants.CoreTokenParams.CLIENT_ID, OAuth2Utils.stringToSet(client.getClientId()));
        super.put(OAuth2Constants.CoreTokenParams.REDIRECT_URI, OAuth2Utils.stringToSet(client.getRedirectUri()));
        super.put(OAuth2Constants.CoreTokenParams.PARENT, OAuth2Utils.stringToSet(parent));
        super.put(OAuth2Constants.CoreTokenParams.REFRESH_TOKEN, OAuth2Utils.stringToSet(refreshToken));
    }

    /**
     * Creates an Bearer Access Token
     * 
     * @param id
     *            Id of the access Token
     * @param value
     *            A JsonValue map to populate this token with.
     */
    public BearerToken(String id, JsonValue value) {
        super(id, value);
    }

    @Override
    /**
     * @{inheritDoc}
     */
    public Map<String, Object> convertToMap(){
        Map<String, Object> tokenMap = new HashMap<String, Object>();
        tokenMap.put(rb.getString(OAuth2Constants.Params.ACCESS_TOKEN), getTokenID());
        tokenMap.put(rb.getString(OAuth2Constants.CoreTokenParams.TOKEN_TYPE), getTokenType());
        tokenMap.put(rb.getString(OAuth2Constants.CoreTokenParams.EXPIRE_TIME), (getExpireTime() - System.currentTimeMillis())/1000);
        return tokenMap;
    }

    @Override
    /**
     * @{inheritDoc}
     */
    public Map<String, Object> getTokenInfo() {
        Map<String, Object> tokenMap = new HashMap<String, Object>();
        tokenMap.put(rb.getString(OAuth2Constants.CoreTokenParams.ID), getTokenID());
        tokenMap.put(rb.getString(OAuth2Constants.CoreTokenParams.TOKEN_TYPE), getTokenType());
        tokenMap.put(rb.getString(OAuth2Constants.CoreTokenParams.EXPIRE_TIME), (getExpireTime() - System.currentTimeMillis())/1000);
        tokenMap.put(rb.getString(OAuth2Constants.CoreTokenParams.REALM), getRealm());
        tokenMap.put(rb.getString(OAuth2Constants.CoreTokenParams.SCOPE), getScope());
        return tokenMap;
    }

}
