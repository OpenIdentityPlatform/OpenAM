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

package org.forgerock.openam.sts.rest;

import javax.inject.Inject;

import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.sts.TokenCancellationException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.rest.operation.cancel.IssuedTokenCancelOperation;
import org.forgerock.openam.sts.rest.operation.validate.IssuedTokenValidateOperation;
import org.forgerock.openam.sts.rest.operation.translate.TokenTranslateOperation;
import org.forgerock.openam.sts.user.invocation.RestSTSTokenCancellationInvocationState;
import org.forgerock.openam.sts.user.invocation.RestSTSTokenTranslationInvocationState;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.user.invocation.RestSTSTokenValidationInvocationState;

/**
 * See {@link org.forgerock.openam.sts.rest.RestSTS}
 */
public class RestSTSImpl implements RestSTS {
    private final TokenTranslateOperation translateOperation;
    private final IssuedTokenValidateOperation issuedTokenValidateOperation;
    private final IssuedTokenCancelOperation issuedTokenCancelOperation;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;

    @Inject
    public RestSTSImpl(TokenTranslateOperation translateOperation,
                       IssuedTokenValidateOperation issuedTokenValidateOperation,
                       IssuedTokenCancelOperation issuedTokenCancelOperation,
                       ThreadLocalAMTokenCache threadLocalAMTokenCache) {
        this.translateOperation = translateOperation;
        this.issuedTokenValidateOperation = issuedTokenValidateOperation;
        this.issuedTokenCancelOperation = issuedTokenCancelOperation;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
    }

    @Override
    public JsonValue translateToken(RestSTSTokenTranslationInvocationState invocationState, Context context)
            throws TokenMarshalException, TokenValidationException, TokenCreationException {
        try {
            return translateOperation.translateToken(invocationState, context);
        } finally {
            threadLocalAMTokenCache.clearCachedSessions();
        }
    }

    @Override
    public JsonValue validateToken(RestSTSTokenValidationInvocationState invocationState)
            throws TokenMarshalException, TokenValidationException {
        return issuedTokenValidateOperation.validateToken(invocationState);
    }

    @Override
    public JsonValue cancelToken(RestSTSTokenCancellationInvocationState invocationState) throws TokenMarshalException, TokenCancellationException {
        return issuedTokenCancelOperation.cancelToken(invocationState);
    }
}
