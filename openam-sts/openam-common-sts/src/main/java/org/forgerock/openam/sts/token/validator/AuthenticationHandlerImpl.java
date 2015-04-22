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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.token.validator;

import org.forgerock.openam.sts.TokenTypeId;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.token.AMTokenParser;
import org.forgerock.openam.sts.token.validator.disp.TokenAuthenticationRequestDispatcher;
import org.forgerock.openam.sts.token.validator.url.AuthenticationUrlProvider;

import javax.inject.Inject;
import java.net.URL;

import org.slf4j.Logger;

/**
 * @see org.forgerock.openam.sts.token.validator.AuthenticationHandler
 */
public class AuthenticationHandlerImpl<T> implements AuthenticationHandler<T> {
    private final AuthenticationUrlProvider authenticationUrlProvider;
    private final TokenAuthenticationRequestDispatcher<T> requestDispatcher;
    private final AMTokenParser tokenParser;
    private final AuthTargetMapping authTargetMapping;
    private final Logger logger;

    @Inject
    public AuthenticationHandlerImpl(
            AuthenticationUrlProvider urlProvider,
            TokenAuthenticationRequestDispatcher<T> requestDispatcher,
            AMTokenParser tokenParser,
            AuthTargetMapping authTargetMapping,
            Logger logger)  {
        this.requestDispatcher = requestDispatcher;
        this.authenticationUrlProvider = urlProvider;
        this.tokenParser = tokenParser;
        this.authTargetMapping = authTargetMapping;
        this.logger = logger;
    }

    @Override
    public String authenticate(T token, TokenTypeId tokenTypeId) throws TokenValidationException {
        final URL authUrl = authenticationUrlProvider.authenticationUrl(tokenTypeId);
        logger.debug("STSAuthenticationHandler: The authUri: " + authUrl.toString());
        final String response = requestDispatcher.dispatch(authUrl, authTargetMapping.getAuthTargetMapping(tokenTypeId), token);
        return tokenParser.getSessionFromAuthNResponse(response);
    }
}
