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

package org.forgerock.openam.sts.rest.operation.validate;

import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenTypeId;
import org.forgerock.openam.sts.rest.token.validator.RestIssuedTokenValidator;
import org.forgerock.openam.sts.rest.token.validator.RestOpenIdConnectIssuedTokenValidator;
import org.forgerock.openam.sts.rest.token.validator.RestSAML2IssuedTokenValidator;
import org.forgerock.openam.sts.token.CTSTokenIdGenerator;
import org.forgerock.openam.sts.token.provider.TokenServiceConsumer;

import javax.inject.Inject;

/**
 * @see IssuedTokenValidatorFactory
 */
public class IssuedTokenValidatorFactoryImpl implements IssuedTokenValidatorFactory {
    private final CTSTokenIdGenerator ctsTokenIdGenerator;
    private final TokenServiceConsumer tokenServiceConsumer;

    @Inject
    IssuedTokenValidatorFactoryImpl(CTSTokenIdGenerator ctsTokenIdGenerator, TokenServiceConsumer tokenServiceConsumer) {
        this.ctsTokenIdGenerator = ctsTokenIdGenerator;
        this.tokenServiceConsumer = tokenServiceConsumer;
    }

    @Override
    public RestIssuedTokenValidator getTokenValidator(TokenTypeId tokenType) throws STSInitializationException {
        if (TokenType.SAML2.getId().equals(tokenType.getId())) {
            return new RestSAML2IssuedTokenValidator(ctsTokenIdGenerator, tokenServiceConsumer);
        } else if (TokenType.OPENIDCONNECT.getId().equals(tokenType.getId())) {
            return new RestOpenIdConnectIssuedTokenValidator(ctsTokenIdGenerator, tokenServiceConsumer);
        } else {
            throw new STSInitializationException(ResourceException.BAD_REQUEST, "Only tokens issued by the rest-sts are " +
                    "available for validation. Illegal validation token type: " + tokenType.getId());
        }
    }
}
