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

package org.forgerock.openam.sts.rest.token.provider.oidc;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.rest.token.provider.RestTokenProviderBase;
import org.forgerock.openam.sts.rest.token.provider.RestTokenProviderParameters;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.provider.AMSessionInvalidator;
import org.forgerock.openam.sts.token.provider.TokenGenerationServiceConsumer;
import org.forgerock.openam.sts.token.validator.ValidationInvocationContext;
import org.slf4j.Logger;

import java.util.Set;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 *
 */
public class RestOpenIdConnectTokenProvider extends RestTokenProviderBase<OpenIdConnectTokenCreationState> {
    private final OpenIdConnectTokenAuthnContextMapper authnContextMapper;
    private final OpenIdConnectTokenAuthMethodReferencesMapper authModeReferencesMapper;

    /*
    ctor not injected as this class created by TokenTransformFactoryImpl
     */
    public RestOpenIdConnectTokenProvider(TokenGenerationServiceConsumer tokenGenerationServiceConsumer,
                                 AMSessionInvalidator amSessionInvalidator,
                                 ThreadLocalAMTokenCache threadLocalAMTokenCache,
                                 String stsInstanceId,
                                 String realm,
                                 OpenIdConnectTokenAuthnContextMapper authnContextMapper,
                                 OpenIdConnectTokenAuthMethodReferencesMapper authModeReferencesMapper,
                                 ValidationInvocationContext validationInvocationContext,
                                 Logger logger) {
        super(tokenGenerationServiceConsumer, amSessionInvalidator, threadLocalAMTokenCache, stsInstanceId, realm,
                validationInvocationContext, logger);
        this.authnContextMapper = authnContextMapper;
        this.authModeReferencesMapper = authModeReferencesMapper;
    }

    @Override
    public JsonValue createToken(RestTokenProviderParameters<OpenIdConnectTokenCreationState> restTokenProviderParameters) throws TokenCreationException {
        try {
            OpenIdConnectTokenCreationState tokenCreationState = restTokenProviderParameters.getTokenCreationState();

            final String authNContextClassRef = authnContextMapper.getAuthnContextClassReference(restTokenProviderParameters.getInputTokenType(),
                    restTokenProviderParameters.getInputToken());
            final Set<String> authenticationMethodReferences = authModeReferencesMapper.getAuthnMethodsReferences(restTokenProviderParameters.getInputTokenType(),
                    restTokenProviderParameters.getInputToken());
            final String assertion = getAssertion(authNContextClassRef, authenticationMethodReferences,
                    tokenCreationState.getAuthenticationTimeInSeconds(), tokenCreationState.getNonce());
            return json(object(field(AMSTSConstants.ISSUED_TOKEN, assertion)));
        } finally {
            try {
                amSessionInvalidator.invalidateAMSessions(threadLocalAMTokenCache.getToBeInvalidatedAMSessionIds());
            } catch (Exception e) {
                String message = "Exception caught invalidating interim AMSession following OpenIdConnect token creation: " + e;
                logger.warn(message, e);
                /*
                The fact that the interim OpenAM session was not invalidated should not prevent a token from being issued, so
                I will not throw an exception.
                */
            }
        }
    }

    private String getAssertion(String authNContextClassRef, Set<String> authenticationMethodReferences,
                                long authTimeInSeconds, String nonce) throws TokenCreationException {
        return tokenGenerationServiceConsumer.getOpenIdConnectToken(
                threadLocalAMTokenCache.getSessionIdForContext(validationInvocationContext),
                stsInstanceId, realm, authNContextClassRef, authenticationMethodReferences,
                authTimeInSeconds, nonce, getAdminToken());
    }
}
