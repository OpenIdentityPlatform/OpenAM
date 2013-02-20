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
import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.AuthorizationCode;
import org.forgerock.openam.oauth2.model.SessionClient;
import org.restlet.Request;

/**
 * Implements an {@link AuthorizationCode} Token
 */
public class AuthorizationCodeImpl extends TokenImpl implements AuthorizationCode {

    /**
     * Creates an Authorization Code Token
     * 
     * @param id
     *            ID of the token
     * @param userID
     *            UserID of the user creating the token
     * @param client
     *            SessionClient of the client creating the token
     * @param realm
     *            Realm the token is created in
     * @param scope
     *            Scope of the token
     * @param issued
     *            Whether or not the access code has already been used to create an access token
     * @param expireTime
     *            Time in seconds until the token expires
     */
    public AuthorizationCodeImpl(String id, String userID, SessionClient client, String realm,
            Set<String> scope, boolean issued, long expireTime) {
        super(id, userID, client, realm, scope, expireTime);
        setIssued(issued);
        setType();
    }

    /**
     * Creates an Authorization Code Token
     * 
     * @param id
     *            ID of the token
     * @param value
     *            A JsonValue map to populate this token with
     */
    public AuthorizationCodeImpl(String id, JsonValue value) {
        super(id, value);
        setType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIssued(boolean issued) {
        Set<String> s = new HashSet<String>();
        s.add(String.valueOf(issued));
        this.put(OAuth2Constants.StoredToken.ISSUED, s);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTokenIssued() {
        Set issuedSet = (Set) get(OAuth2Constants.StoredToken.ISSUED).getObject();
        if (issuedSet != null && !issuedSet.isEmpty()){
            return Boolean.parseBoolean(issuedSet.iterator().next().toString());
        }
        throw OAuthProblemException.OAuthError.INVALID_TOKEN.handle(Request.getCurrent(),
                "Access Code has no issued state. Invalid Token");
    }

    /**
     * Set the type of the token
     */
    protected void setType() {
        Set<String> s = new HashSet<String>();
        s.add(String.valueOf(OAuth2Constants.Params.CODE));
        this.put(OAuth2Constants.StoredToken.TYPE, s);
    }

}
