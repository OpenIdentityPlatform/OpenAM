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
 * ""Portions Copyrighted [2012] [ForgeRock Inc]""
 */

package org.forgerock.restlet.ext.oauth2.consumer;

import org.restlet.security.User;

/**
 * A BearerTokenVerifier verifies a {@link BearerToken}
 * 
 * @author Laszlo Hordos
 */
public class BearerTokenVerifier extends TokenVerifier<BearerAuthenticatorHelper, BearerToken> {

    private final AccessTokenValidator<BearerToken> validator;
    private BearerAuthenticatorHelper helper;

    public BearerTokenVerifier(AccessTokenValidator<BearerToken> validator) {
        this.validator = validator;
        this.helper = new BearerAuthenticatorHelper();
    }

    public BearerTokenVerifier(AccessTokenValidator<BearerToken> validator,
            BearerAuthenticatorHelper helper) {
        this.validator = validator;
        this.helper = helper;
    }

    @Override
    public User createUser(BearerToken token) {
        return new OAuth2User(token.getUsername(), token.getAccessToken(), token.getExpiresIn()
                .longValue(), token.getRefreshToken(), token.getScope(), null);
    }

    @Override
    protected AccessTokenValidator<BearerToken> getTokenValidator() {
        return validator;
    }

    @Override
    protected BearerAuthenticatorHelper getTokenExtractor() {
        return helper;
    }
}
