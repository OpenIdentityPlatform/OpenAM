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

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenTypeId;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.rest.operation.TokenRequestMarshaller;
import org.forgerock.openam.sts.rest.token.validator.RestIssuedTokenValidator;
import org.forgerock.openam.sts.rest.token.validator.RestIssuedTokenValidatorParameters;
import org.forgerock.openam.sts.user.invocation.RestSTSTokenValidationInvocationState;

import javax.inject.Named;
import java.util.HashSet;
import java.util.Set;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

/**
 * @see IssuedTokenValidateOperation
 */
public class IssuedTokenValidateOperationImpl implements IssuedTokenValidateOperation {
    private final TokenRequestMarshaller tokenRequestMarshaller;
    private final Set<RestIssuedTokenValidator> tokenValidators;

    /*
    No @Inject as instance created by @Provider - see RestSTSInstanceModule#getIssuedTokenValidateOperation
     */
    public IssuedTokenValidateOperationImpl(IssuedTokenValidatorFactory issuedTokenValidatorFactory,
                                            TokenRequestMarshaller tokenRequestMarshaller,
                                            @Named(AMSTSConstants.ISSUED_TOKEN_TYPES) Set<TokenTypeId> validatedTokenTypes)
                                            throws STSInitializationException {
        this.tokenRequestMarshaller = tokenRequestMarshaller;

        if (validatedTokenTypes.isEmpty()) {
            throw new IllegalArgumentException("No supported validated token types specified.");
        }
        tokenValidators = new HashSet<>();
        for (TokenTypeId tokenType : validatedTokenTypes) {
            tokenValidators.add(issuedTokenValidatorFactory.getTokenValidator(tokenType));
        }
    }

    public JsonValue validateToken(RestSTSTokenValidationInvocationState invocationState) throws TokenValidationException, TokenMarshalException {
        TokenTypeId tokenTypeId = tokenRequestMarshaller.getTokenType(invocationState.getValidatedTokenState());
        RestIssuedTokenValidatorParameters<?> validatorParameters =
                tokenRequestMarshaller.buildIssuedTokenValidatorParameters(invocationState.getValidatedTokenState());

        for (RestIssuedTokenValidator tokenValidator : tokenValidators) {
            if (tokenValidator.canValidateToken(tokenTypeId)) {
                @SuppressWarnings("unchecked")
                boolean tokenValid = tokenValidator.validateToken(validatorParameters);
                return json(object(field(AMSTSConstants.TOKEN_VALID, tokenValid)));
            }
        }
        throw new TokenValidationException(ResourceException.BAD_REQUEST, "No IssuedTokenValidators available for " +
                "token type: " + tokenTypeId.getId() + ". Does this sts issue tokens of the specified type?");
    }
}
