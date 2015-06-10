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

package org.forgerock.openam.sts.soap.token.provider.saml2;

import org.apache.ws.security.saml.ext.builder.SAML2Constants;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenTypeId;
import org.slf4j.Logger;

import javax.inject.Inject;

/**
 * @see Saml2XmlTokenAuthnContextMapper
 */
public class Saml2XmlTokenAuthnContextMapperImpl implements Saml2XmlTokenAuthnContextMapper {
    private final Logger logger;

    @Inject
    public Saml2XmlTokenAuthnContextMapperImpl(Logger logger) {
        this.logger = logger;
    }
    public String getAuthnContext(TokenTypeId inputTokenType, Object inputToken) {
        logger.debug("In Saml2XmlTokenAuthnContextMapperImpl, the type of the inputToken: "
                + (inputToken != null ? inputToken.getClass().getCanonicalName() : null));
        if(TokenType.OPENAM.getId().equals(inputTokenType.getId())) {
            return SAML2Constants.AUTH_CONTEXT_CLASS_REF_PREVIOUS_SESSION;
        } else if (TokenType.USERNAME.getId().equals(inputTokenType.getId())) {
            return SAML2Constants.AUTH_CONTEXT_CLASS_REF_PASSWORD_PROTECTED_TRANSPORT;
        } else if (TokenType.X509.getId().equals(inputTokenType.getId())) {
            return SAML2Constants.AUTH_CONTEXT_CLASS_REF_X509;
        } else {
            logger.error("Unexpected TokenType passed to Saml2XmlTokenAuthnContextMapperImpl: " + inputTokenType + "; returning " +
                    SAML2Constants.AUTH_CONTEXT_CLASS_REF_UNSPECIFIED);
            return SAML2Constants.AUTH_CONTEXT_CLASS_REF_UNSPECIFIED;
        }
    }
}
