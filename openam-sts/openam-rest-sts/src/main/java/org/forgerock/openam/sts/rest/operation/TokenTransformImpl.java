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

package org.forgerock.openam.sts.rest.operation;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenTypeId;
import org.forgerock.openam.sts.TokenValidationException;

import org.forgerock.openam.sts.rest.token.provider.RestTokenProvider;
import org.forgerock.openam.sts.rest.token.provider.RestTokenProviderParameters;
import org.forgerock.openam.sts.rest.token.validator.RestTokenValidator;
import org.forgerock.openam.sts.rest.token.validator.RestTokenValidatorParameters;

/**
 * The TokenTranslateOperationImpl maintains a Set of these instances, one for each supported token transformation. These
 * instances will be created by the TokenTransformFactory, and thus their dependencies are not injected.
 * See {@link org.forgerock.openam.sts.rest.operation.TokenTransform}
 *
 * Note that this implementation does not specify the generic types defined in the TokenTransform interface. The bottom
 * line is that the TokenTranslateOperationImpl, the implementation of the top-level TokenTranslateOperation, encapsulates
 * a set of TokenTransform instances, one for each of the supported token translations. I want to handle these instances
 * generically, and not have a distinct reference for each supported token type, but rather choose the TokenTransform
 * instance corresponding to the rest-sts invocation via TokenTransform#isTransformSupported. Thus I do not have a different
 * implementation of the TokenTransform for each supported transform type, but rather this single implementation, which
 * does not specify the generic types.
 */
public class TokenTransformImpl implements TokenTransform {
    private final RestTokenValidator<?> tokenValidator;
    private final RestTokenProvider<? extends TokenTypeId> tokenProvider;
    private final TokenTypeId inputTokenType;
    private final TokenTypeId outputTokenType;
    private final String key;

    /*
    No @Inject, as ctor called by the TokenTransformFactoryImpl
     */
    TokenTransformImpl(
            RestTokenValidator<?> tokenValidator,
            RestTokenProvider<? extends TokenTypeId> tokenProvider,
            TokenType inputTokenType, TokenType outputTokenType) {
        this.tokenValidator = tokenValidator;
        this.tokenProvider = tokenProvider;
        this.inputTokenType = inputTokenType;
        this.outputTokenType = outputTokenType;
        key = this.inputTokenType.getId() + this.outputTokenType.getId();
    }

    @Override
    public boolean isTransformSupported(TokenTypeId inputTokenType, TokenTypeId outputTokenType) {
        return this.inputTokenType.equals(inputTokenType) && this.outputTokenType.equals(outputTokenType);
    }

    @Override
    public JsonValue transformToken(RestTokenValidatorParameters validatorParameters, RestTokenProviderParameters providerParameters)
            throws TokenValidationException, TokenCreationException {
        tokenValidator.validateToken(validatorParameters);
        //TODO: pending decision on supporting custom token operations (AME-6554), I may want to allow the RestTokenValidatorResult
        //to encapsulate an 'additionalState' JsonValue, which is copied into the RestTokenProviderParameters. It may even make
        //sense to add an 'additionalState' JsonValue to the RestSTSServiceInvocationState, which could be passed into the
        //RestTokenValidatorParameters. This would allow rest-sts invocations to encapsulate additional state possibly required
        //by custom token operations.
        return tokenProvider.createToken(providerParameters);
    }

    @Override
    public boolean equals(Object other)  {
        if (this == other) {
            return true;
        }
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
