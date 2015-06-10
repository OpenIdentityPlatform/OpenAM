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
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenTypeId;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.config.user.TokenTransformConfig;
import org.forgerock.openam.sts.rest.marshal.TokenRequestMarshaller;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.openam.sts.rest.service.RestSTSServiceHttpServletContext;
import org.forgerock.openam.sts.rest.token.validator.RestTokenValidatorParameters;
import org.forgerock.openam.sts.user.invocation.RestSTSServiceInvocationState;
import org.forgerock.openam.sts.rest.token.provider.RestTokenProviderParameters;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class defines TokenTranslateOperation implementation, the top-level operation invoked by the REST-STS
 * resource. It is responsible determining which TokenTransform instance should be targeted, taking the HttpServletRequest
 * and the invocation parameters and creating RestTokenValidatorParameters and RestTokenProviderParameters necessary
 * to invoke the RestTokenValidator/RestTokenProvider instances encapsulated in the chosen TokenTransform, and invoking this
 * instance, returning the result.
 */
public class TokenTranslateOperationImpl implements TokenTranslateOperation {
    private final TokenRequestMarshaller tokenRequestMarshaller;
    private final Set<TokenTransform> tokenTransforms;

    @Inject
    TokenTranslateOperationImpl(
                        TokenRequestMarshaller tokenRequestMarshaller,
                        @Named(AMSTSConstants.REST_SUPPORTED_TOKEN_TRANSLATIONS)
                        Set<TokenTransformConfig> supportedTranslations,
                        TokenTransformFactory tokenTransformFactory) throws Exception {
        this.tokenRequestMarshaller = tokenRequestMarshaller;

        if (supportedTranslations.isEmpty()) {
            throw new IllegalArgumentException("No token transform operations specified.");
        }

        Set<TokenTransform> interimTransforms = new HashSet<TokenTransform>();
        for (TokenTransformConfig tokenTransformConfig : supportedTranslations) {
            interimTransforms.add(tokenTransformFactory.buildTokenTransform(tokenTransformConfig));
        }
        tokenTransforms = Collections.unmodifiableSet(interimTransforms);
    }

    public JsonValue translateToken(RestSTSServiceInvocationState invocationState, HttpContext httpContext,
                                    RestSTSServiceHttpServletContext restSTSServiceHttpServletContext)
            throws TokenMarshalException, TokenValidationException, TokenCreationException {
        TokenTypeId inputTokenType = tokenRequestMarshaller.getTokenType(invocationState.getInputTokenState());
        TokenTypeId outputTokenType = tokenRequestMarshaller.getTokenType(invocationState.getOutputTokenState());

        TokenTransform targetedTransform = null;
        for (TokenTransform transform : tokenTransforms) {
            if (transform.isTransformSupported(inputTokenType, outputTokenType)) {
                targetedTransform = transform;
                break;
            }
        }
        if (targetedTransform == null) {
            String message = "The desired transformation, from " + inputTokenType + " to " + outputTokenType +
                    ", is not a supported token translation.";
            throw new TokenValidationException(ResourceException.BAD_REQUEST, message);
        }
        RestTokenValidatorParameters<?> validatorParameters = tokenRequestMarshaller.buildTokenValidatorParameters(
                invocationState.getInputTokenState(), httpContext, restSTSServiceHttpServletContext);
        RestTokenProviderParameters<? extends TokenTypeId> providerParameters =
                tokenRequestMarshaller.buildTokenProviderParameters(inputTokenType,
                    invocationState.getInputTokenState(), outputTokenType, invocationState.getOutputTokenState());

        return targetedTransform.transformToken(validatorParameters, providerParameters);
    }
}
