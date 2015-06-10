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

package org.forgerock.openam.sts.rest.marshal;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenTypeId;
import org.forgerock.openam.sts.rest.service.RestSTSServiceHttpServletContext;
import org.forgerock.openam.sts.rest.token.provider.RestTokenProviderParameters;
import org.forgerock.openam.sts.rest.token.validator.RestTokenValidatorParameters;

/**
 * Defines an interface encapsulating the concerns of taking the json posted at the rest-sts, and marshalling it into
 * the constructs necessary to implement rest-sts operations, currently limited to token transformation.
 */
public interface TokenRequestMarshaller {
    /**
     * Marshals state from a token translate invocation into the RestTokenValidatorParameters necessary to validate this token.
     * @param token the json representation of a token
     * @param httpContext The HttpContext, which is necessary to obtain the client's x509 cert
     *                    presented via two-way-tls for token transformations with x509 certs as input token types.
     * @param restSTSServiceHttpServletContext Provides direct access to the HttpServletRequest so that
     *                                                            client certificate state, presented via two-way-tls, can
     *                                                            be obtained
     * @return a RestTokenValidatorParameters instance for a particular token type
     * @throws org.forgerock.openam.sts.TokenMarshalException if the json string cannot be marshalled into a recognized token.
     */
    RestTokenValidatorParameters<?> buildTokenValidatorParameters(JsonValue token, HttpContext httpContext, RestSTSServiceHttpServletContext
            restSTSServiceHttpServletContext) throws TokenMarshalException;


    /**
     * Marshals state from a token translate invocation into the RestTokenProviderParameters necessary to create a token
     * of the specified type
     * @param inputTokenType the type of input token
     * @param inputToken the json specifying the input token
     * @param desiredTokenType the type of the output token
     * @param desiredTokenState the json state corresponding this output token type
     * @return the RestTokenProviderParameters necessary to create a token of the specified type.
     * @throws TokenMarshalException
     */
    RestTokenProviderParameters<? extends TokenTypeId> buildTokenProviderParameters(TokenTypeId inputTokenType, JsonValue inputToken,
                                                                TokenTypeId desiredTokenType, JsonValue desiredTokenState) throws TokenMarshalException;
    /**
     * Returns the TokenType corresponding to the JsonValue. The JsonValue will be pulled from the RestSTSServiceInvocationState.
     * @param token The token definition
     * @return The TokenType represented by the json string.
     * @throws org.forgerock.openam.sts.TokenMarshalException if the TOKEN_TYPE_KEY is missing or unrecognized.
     */
    TokenTypeId getTokenType(JsonValue token) throws TokenMarshalException;
}
