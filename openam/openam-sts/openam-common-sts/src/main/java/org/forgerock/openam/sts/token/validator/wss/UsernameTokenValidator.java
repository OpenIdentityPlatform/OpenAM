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

import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.message.token.UsernameToken;
import org.slf4j.Logger;

/**
 * The org.apache.cxf.sts.token.validator.UsernameTokenValidator class ultimately calls a org.apache.ws.security.validate.Validator
 * instance (set via a setter) to perform the actual token validation. This class implements the ws.security.validate.Validator
 * interface and will be set in the cxf.sts.token.validator.UsernameTokenValidator class. It will perform the actual
 * UsernameToken validation by calling the OpenAM REST interface via bound TokenAuthenticationRequestDispacher<UsernameToken>
 *
 */
public class UsernameTokenValidator extends org.apache.ws.security.validate.UsernameTokenValidator {
    private final Logger logger;
    private final AuthenticationHandler<UsernameToken> authenticationHandler;

    public UsernameTokenValidator(Logger logger, AuthenticationHandler<UsernameToken> authenticationHandler) {
        this.logger = logger;
        this.authenticationHandler = authenticationHandler;
    }

    @Override
    protected void verifyPlaintextPassword(UsernameToken usernameToken,
                                           RequestData data) throws WSSecurityException {
        try {
            authenticationHandler.authenticate(data, usernameToken);
        } catch (Exception e) {
            String message = "Exception caught authenticating UsernameToken with OpenAM: " + e;
            logger.error(message, e);
            throw new WSSecurityException(message, e);
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
            logger.debug("Passwords are encoded: %b", config.getPasswordsAreEncoded());
            logger.debug("Handle custom password types: %b", config.getHandleCustomPasswordTypes());
            logger.debug("Required Password Type: %s", config.getRequiredPasswordType());
        }
    }
}
