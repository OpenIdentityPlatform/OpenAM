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

package org.forgerock.openam.sts.rest.operation;

import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSRuntimeException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenValidationException;

import org.slf4j.Logger;

/**
 * The TokenTranslateOperationImpl maintains a Set of these instances, one for each supported token transformation. These
 * instances will be created by the TokenTransformFactory, and thus their dependencies are not injected.
 * See {@link org.forgerock.openam.sts.rest.operation.TokenTransform}
 */
public class TokenTransformImpl implements TokenTransform {
    private final TokenValidator tokenValidator;
    private final TokenProvider tokenProvider;
    private final TokenType inputTokenType;
    private final TokenType outputTokenType;
    private final Logger logger;
    private final String key;

    TokenTransformImpl(
            TokenValidator tokenValidator,
            TokenProvider tokenProvider,
            TokenType inputTokenType, TokenType outputTokenType,
            Logger logger) {
        this.tokenValidator = tokenValidator;
        this.tokenProvider = tokenProvider;
        this.inputTokenType = inputTokenType;
        this.outputTokenType = outputTokenType;
        this.logger = logger;
        key = this.inputTokenType.name() + this.outputTokenType.name();
    }

    @Override
    public boolean isTransformSupported(TokenType inputTokenType, TokenType outputTokenType) {
        return this.inputTokenType.equals(inputTokenType) && this.outputTokenType.equals(outputTokenType);
    }

    @Override
    public TokenProviderResponse transformToken(TokenValidatorParameters validatorParameters, TokenProviderParameters providerParameters)
            throws TokenValidationException, TokenCreationException {
        TokenValidatorResponse validatorResponse = null;
        validatorResponse = tokenValidator.validateToken(validatorParameters);
        if (ReceivedToken.STATE.VALID.equals(validatorResponse.getToken().getState())) {
            providerParameters.setPrincipal(validatorResponse.getPrincipal());
            try {
                return tokenProvider.createToken(providerParameters);
            } catch (AMSTSRuntimeException e) {
                /*
                The TokenProvider interface does not allow for checked exceptions. I throw an AMSTSRuntimeException for
                exceptional conditions. Convert to TokenCreationException.
                 */
                throw new TokenCreationException(e.getCode(), e.getMessage(), e);
            }
        } else {
            String message = "Validation of token of type " + inputTokenType + " failed.";
            logger.error(message);
            throw new TokenValidationException(ResourceException.BAD_REQUEST, message);
        }
    }

    @Override
    public boolean equals(Object other)  {
        if (other instanceof TokenTransformImpl) {
            TokenTransformImpl otherTransform = (TokenTransformImpl)other;
            return key.equals(otherTransform.key);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
