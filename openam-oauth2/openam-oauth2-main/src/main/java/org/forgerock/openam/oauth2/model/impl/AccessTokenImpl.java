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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.restlet.ext.oauth2.OAuth2;
import org.forgerock.restlet.ext.oauth2.model.AccessToken;
import org.forgerock.restlet.ext.oauth2.model.SessionClient;
import org.forgerock.restlet.ext.oauth2.model.Token;

/**
 * Created by IntelliJ IDEA. User: jonathan Date: 26/3/12 Time: 10:37 AM To
 * change this template use File | Settings | File Templates.
 */
public class AccessTokenImpl extends TokenImpl implements AccessToken {

    /**
     * Constructor. TODO Description
     * 
     * @param id
     *            TODO Description
     * @param parent
     *            TODO Description
     * @param userID
     *            TODO Description
     * @param client
     *            TODO Description
     * @param realm
     *            TODO Description
     * @param scope
     *            TODO Description
     * @param expireTime
     *            TODO Description
     */
    public AccessTokenImpl(String id, String parent, String userID, SessionClient client,
            String realm, Set<String> scope, long expireTime) {
        super(id, userID, client, realm, scope, expireTime);
        setType();
        setParentToken(parent);
    }

    /**
     * Constructor. TODO javadoc
     * 
     * @param id
     *            TODO Description
     * @param scope
     *            TODO Description
     * @param expireTime
     *            TODO Description
     * @param token
     *            TODO Description
     */
    public AccessTokenImpl(String id, Set<String> scope, long expireTime, Token token) {
        super(id, token.getUserID(), token.getClient(), token.getRealm(), scope, expireTime);
        setType();
    }

    /**
     * TODO Description.
     * 
     * @param id
     *            TODO Description
     * @param value
     *            TODO Description
     */
    public AccessTokenImpl(String id, JsonValue value) {
        super(id, value);
        setType();
    }

    /**
     * TODO Description.
     * 
     * @param parent
     *            TODO Description
     */
    public void setParentToken(String parent) {
        this.put(OAuth2.StoredToken.PARENT, parent);
    }

    @Override
    public String getParentToken() {
        return this.get(OAuth2.StoredToken.PARENT).asString();
    }

    @Override
    public RefreshTokenImpl getRefreshToken() {
        // TODO implement or change interface
        return null; // To change body of implemented methods use File |
                     // Settings | File Templates.
    }

    @Override
    public Map<String, Object> convertToMap() {
        Map<String, Object> tokenMap = new HashMap<String, Object>();
        tokenMap.put(OAuth2.Params.ACCESS_TOKEN, getToken());
        tokenMap.put(OAuth2.Params.TOKEN_TYPE, OAuth2.Bearer.BEARER);
        tokenMap.put(OAuth2.Params.EXPIRES_IN, getExpireTime() - System.currentTimeMillis());
        // TODO implement or change interface
        return tokenMap;
    }

    /**
     * TODO Description.
     */
    protected void setType() {
        this.put(OAuth2.StoredToken.TYPE, OAuth2.Params.ACCESS_TOKEN);
    }

}
