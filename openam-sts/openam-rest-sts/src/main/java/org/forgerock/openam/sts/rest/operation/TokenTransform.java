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
 * Copyright 2013-2015 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest.operation;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenTypeId;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.rest.token.provider.RestTokenProviderParameters;
import org.forgerock.openam.sts.rest.token.validator.RestTokenValidatorParameters;

/**
 * This interface defines a specific token transformation. A set of TokenTransform instance will be injected into each
 * REST-STS instance, one for each supported token translation.
 *
 * Token transformation and token translation can be considered synonyms - different names were chosen to distinguish
 * the top-level operation(TokenTransformOperation), and the set of specific TokenTransform instances, each of which
 * validates a specific input token type and generates a specific output token type.
 *
 * Instances of this interface will be maintained in a Set in the TokenTranslateOperation, so their equals and
 * hashCode methods must be overridden correctly.
 */
public interface TokenTransform<I, O extends TokenTypeId> {
    /**
     * The TokenTranslateOperationImpl maintains a set of these instances, one corresponding to each of the supported
     * token transforms. When a token translation invocation arrives, this method is used on the {@code Set<TokenTransform>}
     * to determine which instance to invoke to realize the token transformation.
     * @param inputTokenType the input token type in the token transformation
     * @param outputTokenType the output token type in the token transformation
     * @return whether or not this particular TokenTransform instance can support the specified translation
     */
    boolean isTransformSupported(TokenTypeId inputTokenType, TokenTypeId outputTokenType);

    /**
     * transforms the input token type specified in the RestTokenValidatorParameters into the output token specified by
     * the RestTokenProviderParameters
     * @param validatorParameters the state necessary to perform token validation
     * @param providerParameters the state necessary to generate the output token
     * @return the json representation of the generated token
     * @throws TokenValidationException if the input token could not be validated
     * @throws TokenCreationException if the output token could not be generated
     */
    JsonValue transformToken(RestTokenValidatorParameters<I> validatorParameters,
                             RestTokenProviderParameters<O> providerParameters) throws TokenValidationException, TokenCreationException;
}
