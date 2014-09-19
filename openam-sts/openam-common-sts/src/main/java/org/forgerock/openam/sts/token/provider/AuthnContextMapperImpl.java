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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.token.provider;

import org.apache.ws.security.saml.ext.builder.SAML2Constants;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.TokenType;
import org.slf4j.Logger;

import javax.inject.Inject;

/**
 * @see org.forgerock.openam.sts.token.provider.AuthnContextMapper
 */
public class AuthnContextMapperImpl implements AuthnContextMapper {
    private final Logger logger;

    @Inject
    public AuthnContextMapperImpl(Logger logger) {
        this.logger = logger;
    }

    public String getAuthnContext(TokenType inputTokenType, JsonValue inputToken) {
        switch (inputTokenType) {
            case OPENAM:
                return SAML2Constants.AUTH_CONTEXT_CLASS_REF_PREVIOUS_SESSION;
            case USERNAME:
                return SAML2Constants.AUTH_CONTEXT_CLASS_REF_PASSWORD_PROTECTED_TRANSPORT;
            case OPENIDCONNECT:
                return SAML2Constants.AUTH_CONTEXT_CLASS_REF_PASSWORD_PROTECTED_TRANSPORT;
            case X509:
                return SAML2Constants.AUTH_CONTEXT_CLASS_REF_X509;
            default:
                logger.error("Unexpected TokenType passed to AuthnContextMapperImpl: " + inputTokenType + "; returning " +
                        SAML2Constants.AUTH_CONTEXT_CLASS_REF_UNSPECIFIED);
                return SAML2Constants.AUTH_CONTEXT_CLASS_REF_UNSPECIFIED;
        }
    }
}
