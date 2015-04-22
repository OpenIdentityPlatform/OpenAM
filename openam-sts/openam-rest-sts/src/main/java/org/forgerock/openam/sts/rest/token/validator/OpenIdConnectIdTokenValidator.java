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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.rest.token.validator;

import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdToken;
import org.forgerock.openam.sts.token.validator.AuthenticationHandler;
import org.forgerock.openam.sts.token.validator.PrincipalFromSession;
import org.forgerock.openam.sts.token.validator.ValidationInvocationContext;

/**
 * The RestTokenValidator implementation responsible for dispatching OpenID Connect ID Tokens to the OpenAM Rest authN
 * context.
 */
public class OpenIdConnectIdTokenValidator implements RestTokenValidator<OpenIdConnectIdToken> {
    private final AuthenticationHandler<OpenIdConnectIdToken> authenticationHandler;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final PrincipalFromSession principalFromSession;
    private final ValidationInvocationContext validationInvocationContext;
    private final boolean invalidateAMSession;

    public OpenIdConnectIdTokenValidator(AuthenticationHandler<OpenIdConnectIdToken> authenticationHandler,
                                         ThreadLocalAMTokenCache threadLocalAMTokenCache,
                                         PrincipalFromSession principalFromSession,
                                         ValidationInvocationContext validationInvocationContext,
                                         boolean invalidateAMSession) {
        this.authenticationHandler = authenticationHandler;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.principalFromSession = principalFromSession;
        this.validationInvocationContext = validationInvocationContext;
        this.invalidateAMSession = invalidateAMSession;
    }

    @Override
    public RestTokenValidatorResult validateToken(RestTokenValidatorParameters<OpenIdConnectIdToken> tokenParameters) throws TokenValidationException {
        final String sessionId = authenticationHandler.authenticate(tokenParameters.getInputToken(), TokenType.OPENIDCONNECT);
        threadLocalAMTokenCache.cacheSessionIdForContext(validationInvocationContext, sessionId, invalidateAMSession);
        return new RestTokenValidatorResult(principalFromSession.getPrincipalFromSession(sessionId), sessionId);
    }
}
