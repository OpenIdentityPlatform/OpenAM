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
package org.forgerock.restlet.ext.oauth2.consumer;

import java.util.Set;

import org.forgerock.openam.oauth2.model.Token;
import org.restlet.security.User;

/**
 * Object that represents an OAuth2User.
 * Used in the demo.
 */
public class OAuth2User extends User {

    /**
     * The access token.
     */
    private final String accessToken;

    /**
     * The validity delay of the authentication.
     */
    private final long expiresIn;

    /**
     * The refresh token.
     */
    private final String refreshToken;

    /**
     * The authorized scope.
     */
    private final Set<String> scope;

    /**
     * The current state.
     */
    private volatile String state;

    /**
     * Constructor.
     * 
     * @param identifier
     *            The identifier (login).
     */
    public OAuth2User(String identifier, String accessToken, long expiresIn, String refreshToken,
            Set<String> scope, String state) {
        super(identifier);
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
        this.scope = scope;
        this.state = state;
    }

    public OAuth2User(Token token) {
        super(token.getUserID());
        this.accessToken = token.getTokenID();
        this.expiresIn = token.getExpireTime();
        this.scope = token.getScope();

        // TODO Do we need these?
        this.refreshToken = null;
        this.state = null;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public Set<String> getScope() {
        return scope;
    }

    public String getState() {
        return state;
    }
}
