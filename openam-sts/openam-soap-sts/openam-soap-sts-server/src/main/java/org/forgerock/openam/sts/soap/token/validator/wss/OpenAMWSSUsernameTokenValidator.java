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
 * Copyright 2013-2016 ForgeRock AS.
 * Portions Copyrighted 2025 3A-Systems LLC.
 */

package org.forgerock.openam.sts.soap.token.validator.wss;

import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.validator.AuthenticationHandler;
import org.forgerock.openam.sts.token.validator.ValidationInvocationContext;
import org.slf4j.Logger;

/**
 * The org.apache.cxf.sts.token.validator.UsernameTokenValidator class ultimately calls a org.apache.ws.security.validate.Validator
 * instance (set via a setter) to perform the actual token validation. This class implements the ws.security.validate.Validator
 * interface and will be set in the cxf.sts.token.validator.UsernameTokenValidator class. It will perform the actual
 * UsernameToken validation by calling the OpenAM REST interface via bound TokenAuthenticationRequestDispacher<UsernameToken>
 *
 */
public class OpenAMWSSUsernameTokenValidator extends org.apache.wss4j.dom.validate.UsernameTokenValidator {
    private final AuthenticationHandler<UsernameToken> authenticationHandler;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final ValidationInvocationContext validationInvocationContext;
    private final boolean invalidateOpenAMSession;
    private final Logger logger;

    public OpenAMWSSUsernameTokenValidator(AuthenticationHandler<UsernameToken> authenticationHandler,
                                           ThreadLocalAMTokenCache threadLocalAMTokenCache,
                                           ValidationInvocationContext validationInvocationContext,
                                           boolean invalidateOpenAMSession, Logger logger) {
        this.authenticationHandler = authenticationHandler;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.validationInvocationContext = validationInvocationContext;
        this.invalidateOpenAMSession = invalidateOpenAMSession;
        this.logger = logger;
    }

    @Override
    protected void verifyPlaintextPassword(UsernameToken usernameToken,
                                           RequestData data) throws WSSecurityException {
        try {
            final String sessionId = authenticationHandler.authenticate(usernameToken, TokenType.USERNAME);
            threadLocalAMTokenCache.cacheSessionIdForContext(validationInvocationContext, sessionId, invalidateOpenAMSession);
        } catch (TokenValidationException e) {
            String message = "Exception caught authenticating UsernameToken with OpenAM: " + e;
            logger.error(message, e);
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, e, message);
        }
    }


    /*
    TODO: have to verify the digest scheme - can only verify if digest scheme is same as that used to store AM passwords
     */
    @Override
    protected void verifyDigestPassword(UsernameToken usernameToken,
                                        RequestData data) throws WSSecurityException {
        logger.debug("!!!in verifyDigestPassword");
        printConfig(data);
    }

    /*
    TODO: what is the nature of 'custom' - does it pertain to the digest scheme, or ??
     */
    @Override
    protected void verifyCustomPassword(UsernameToken usernameToken,
                                        RequestData data) throws WSSecurityException {
        logger.debug("!!!in verifyCustomPassword");
        printConfig(data);
    }

    protected void printConfig(RequestData data) {
        WSSConfig config =  data.getWssConfig();
        if (config != null) {
            //logger.debug("Passwords are encoded: {}", data.getPasswordsAreEncoded());
            //logger.debug("Handle custom password types: {}", data.getHandleCustomPasswordTypes());
            logger.debug("Required Password Type: {}", data.getRequiredPasswordType());
        }
    }
}
