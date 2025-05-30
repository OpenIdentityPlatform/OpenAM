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
 * Portions Copyrighted 2025 3A-Systems LLC.
 */

package org.forgerock.openam.sts.soap.token.validator.wss;

import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.wss4j.dom.validate.Validator;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.validator.ValidationInvocationContext;
import org.forgerock.openam.sts.token.validator.AuthenticationHandler;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.security.cert.X509Certificate;

/**
 * @see org.forgerock.openam.sts.soap.token.validator.wss.WSSValidatorFactory
 */
public class WSSValidatorFactoryImpl implements WSSValidatorFactory {
    private final AuthenticationHandler<UsernameToken> usernameTokenAuthenticationHandler;
    private final AuthenticationHandler<X509Certificate[]> certificateAuthenticationHandler;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final Logger logger;

    @Inject
    WSSValidatorFactoryImpl(AuthenticationHandler<UsernameToken> usernameTokenAuthenticationHandler,
                            AuthenticationHandler<X509Certificate[]> certificateAuthenticationHandler,
                            ThreadLocalAMTokenCache threadLocalAMTokenCache,
                            Logger logger) {
        this.usernameTokenAuthenticationHandler = usernameTokenAuthenticationHandler;
        this.certificateAuthenticationHandler = certificateAuthenticationHandler;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.logger = logger;
    }

    @Override
    public Validator getValidator(TokenType tokenType, ValidationInvocationContext validationInvocationContext,
                                  boolean invalidateInterimOpenAMSession) {
        switch (tokenType) {
            case USERNAME:
                return new OpenAMWSSUsernameTokenValidator(usernameTokenAuthenticationHandler, threadLocalAMTokenCache,
                        validationInvocationContext, invalidateInterimOpenAMSession, logger);
            case X509:
                return new SoapCertificateTokenValidator(certificateAuthenticationHandler, threadLocalAMTokenCache,
                        validationInvocationContext, invalidateInterimOpenAMSession, logger);
            default:
                throw new IllegalArgumentException("SecurityPolicy validation for specified token type, "
                        + tokenType + " not supported.");

        }
    }
}
