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

import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.model.OpenAMSessionToken;
import org.forgerock.openam.sts.token.validator.PrincipalFromSession;
import org.forgerock.openam.sts.token.validator.ValidationInvocationContext;

import java.security.Principal;

/**
 * Validates OpenAM session tokens in the rest context. Validates the token by attempting to obtain the principal
 * for the particular session id.
 */
public class RestAMTokenValidator implements RestTokenValidator<OpenAMSessionToken>  {
    private final PrincipalFromSession principalFromSession;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final ValidationInvocationContext validationInvocationContext;
    private final boolean invalidateAMSession;

    public RestAMTokenValidator(PrincipalFromSession principalFromSession, ThreadLocalAMTokenCache threadLocalAMTokenCache,
                                ValidationInvocationContext validationInvocationContext, boolean invalidateAMSession) {
        this.principalFromSession = principalFromSession;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.validationInvocationContext = validationInvocationContext;
        this.invalidateAMSession = invalidateAMSession;
    }

    @Override
    public RestTokenValidatorResult validateToken(RestTokenValidatorParameters<OpenAMSessionToken> restTokenValidatorParameters) throws TokenValidationException {
        final String sessionId = restTokenValidatorParameters.getInputToken().getSessionId();
        final Principal principal = principalFromSession.getPrincipalFromSession(sessionId);
        threadLocalAMTokenCache.cacheSessionIdForContext(validationInvocationContext, sessionId, invalidateAMSession);
        return new RestTokenValidatorResult(principal, sessionId);
    }
}
