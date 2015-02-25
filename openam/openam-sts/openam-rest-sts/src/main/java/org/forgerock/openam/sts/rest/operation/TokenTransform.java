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

import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenValidationException;

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
public interface TokenTransform {
    boolean isTransformSupported(TokenType inputTokenType, TokenType outputTokenType);
    TokenProviderResponse transformToken(TokenValidatorParameters validatorParameters, TokenProviderParameters providerParameters)
            throws TokenValidationException, TokenCreationException;
}
