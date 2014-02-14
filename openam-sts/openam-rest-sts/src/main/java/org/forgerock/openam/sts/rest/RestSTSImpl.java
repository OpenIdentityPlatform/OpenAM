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

package org.forgerock.openam.sts.rest;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.rest.operation.TokenTranslateOperation;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;

import org.slf4j.Logger;

/**
 * See {@link org.forgerock.openam.sts.rest.RestSTS}
 */
public class RestSTSImpl implements RestSTS {
    private final TokenTranslateOperation translateOperation;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final Logger slf4jLogger;

    @Inject
    public RestSTSImpl(TokenTranslateOperation translateOperation, ThreadLocalAMTokenCache threadLocalAMTokenCache, Logger slf4jLogger) {
        this.translateOperation = translateOperation;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.slf4jLogger = slf4jLogger;
    }

    public String translateToken(String inputToken, String desiredTokenType, HttpServletRequest request)
            throws TokenValidationException, TokenCreationException {
        try {
            return translateOperation.translateToken(inputToken, desiredTokenType, request);
        } finally {
            threadLocalAMTokenCache.clearAMToken();
        }
    }
}
