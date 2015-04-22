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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.rest.token.validator;

import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.model.RestUsernameToken;
import org.forgerock.openam.sts.token.validator.AuthenticationHandler;
import org.forgerock.openam.sts.token.validator.PrincipalFromSession;
import org.forgerock.openam.sts.token.validator.ValidationInvocationContext;

/**
 * Responsible for validating RestUsernameToken instances, which is simply a <username,password> combination.
 */
public class RestUsernameTokenValidator implements RestTokenValidator<RestUsernameToken> {
    private final AuthenticationHandler<RestUsernameToken> authenticationHandler;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final PrincipalFromSession principalFromSession;
    private final ValidationInvocationContext validationInvocationContext;
    private final boolean invalidateAMSession;

    /*
    No @Inject as instances are created by the TokenTransformFactory
     */
    public RestUsernameTokenValidator(AuthenticationHandler<RestUsernameToken> authenticationHandler,
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
    public RestTokenValidatorResult validateToken(RestTokenValidatorParameters<RestUsernameToken> restTokenValidatorParameters) throws TokenValidationException {
        final String sessionId = authenticationHandler.authenticate(restTokenValidatorParameters.getInputToken(), TokenType.USERNAME);
        threadLocalAMTokenCache.cacheSessionIdForContext(validationInvocationContext, sessionId, invalidateAMSession);
        return new RestTokenValidatorResult(principalFromSession.getPrincipalFromSession(sessionId), sessionId);
    }
}
