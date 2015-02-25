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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.token.validator.wss;

import org.apache.ws.security.handler.RequestData;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.token.AMTokenParser;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.validator.wss.disp.TokenAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.token.validator.wss.url.AuthenticationUrlProvider;

import javax.inject.Inject;
import java.net.URL;

import org.slf4j.Logger;

/**
 * @see org.forgerock.openam.sts.token.validator.wss.AuthenticationHandler
 */
public class AuthenticationHandlerImpl<T> implements AuthenticationHandler<T> {
    private final AuthenticationUrlProvider authenticationUrlProvider;
    private final TokenAuthenticationRequestDispatcher<T> requestDispatcher;
//    private final AMTokenCache tokenCache;
    private final ThreadLocalAMTokenCache tokenCache;
    private final AMTokenParser tokenParser;
    private final AuthTargetMapping authTargetMapping;
    private final Logger logger;

    @Inject
    public AuthenticationHandlerImpl(
            AuthenticationUrlProvider urlProvider,
            TokenAuthenticationRequestDispatcher<T> requestDispatcher,
            //AMTokenCache tokenCache,
            ThreadLocalAMTokenCache tokenCache,
            AMTokenParser tokenParser,
            AuthTargetMapping authTargetMapping,
            Logger logger)  {
        this.requestDispatcher = requestDispatcher;
        this.authenticationUrlProvider = urlProvider;
        this.tokenCache = tokenCache;
        this.tokenParser = tokenParser;
        this.authTargetMapping = authTargetMapping;
        this.logger = logger;
    }

    public void authenticate(RequestData requestData, T token) throws TokenValidationException {
        URL authUrl = authenticationUrlProvider.authenticationUrl(token);
        logger.debug("STSAuthenticationHandler: The authUri: " + authUrl.toString());
        String response = requestDispatcher.dispatch(authUrl, authTargetMapping.getAuthTargetMapping(token.getClass()), token);
        tokenCache.cacheAMToken(tokenParser.getSessionFromAuthNResponse(response));
        //tokenCache.cacheAMSessionId(requestData, tokenParser.getSessionFromAuthNResponse(representation));
    }
}
