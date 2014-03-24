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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

/**
 * This class is the entry point to request/gain a OAuth2 Access Token, it delegates to a GrantTypeHandler depending
 * on the grant type specified in the Access Token Request.
 *
 * @since 12.0.0
 */
public class AccessTokenService {

    public static final String GRANT_TYPE_HANDLERS_INJECT_KEY = "GRANT_TYPE_HANDLERS";

    private final Map<? extends GrantType, ? extends GrantTypeHandler> grantTypeHandlers; //TODO make into a factory class?

    @Inject
    public AccessTokenService(final @Named(GRANT_TYPE_HANDLERS_INJECT_KEY)
    Map<? extends GrantType, ? extends GrantTypeHandler> grantTypeHandlers) {
        this.grantTypeHandlers = grantTypeHandlers;
    }

    public AccessToken requestAccessToken(final AccessTokenRequest accessTokenRequest) throws InvalidClientException, UnauthorizedClientException, InvalidGrantException {
        return grantTypeHandlers.get(accessTokenRequest.getGrantType())
                .handle(accessTokenRequest);
    }
}
