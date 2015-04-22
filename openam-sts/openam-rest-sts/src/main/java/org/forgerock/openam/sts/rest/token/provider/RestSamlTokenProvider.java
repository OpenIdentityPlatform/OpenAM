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

package org.forgerock.openam.sts.rest.token.provider;

import com.sun.identity.security.AdminTokenAction;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.service.invocation.ProofTokenState;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;

import org.forgerock.openam.sts.token.provider.AMSessionInvalidator;
import org.forgerock.openam.sts.token.provider.TokenGenerationServiceConsumer;
import org.forgerock.openam.sts.token.validator.ValidationInvocationContext;
import org.slf4j.Logger;

import java.security.AccessController;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * This encapsulates logic to both create a SAML2 token, and to invalidate the interim OpenAM session object
 * generated from the preceding TokenValidation operation if the TokenTransform has been configured to invalidate
 * the interim OpenAM sessions generated from token validation.
 *
 */
public class RestSamlTokenProvider implements RestTokenProvider<Saml2TokenCreationState> {
    private final TokenGenerationServiceConsumer tokenGenerationServiceConsumer;
    private final AMSessionInvalidator amSessionInvalidator;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final String stsInstanceId;
    private final String realm;
    private final JsonTokenAuthnContextMapper authnContextMapper;
    private final ValidationInvocationContext validationInvocationContext;
    private final Logger logger;

    /*
    ctor not injected as this class created by TokenTransformFactoryImpl
     */
    public RestSamlTokenProvider(TokenGenerationServiceConsumer tokenGenerationServiceConsumer,
                               AMSessionInvalidator amSessionInvalidator,
                               ThreadLocalAMTokenCache threadLocalAMTokenCache,
                               String stsInstanceId,
                               String realm,
                               JsonTokenAuthnContextMapper authnContextMapper,
                               ValidationInvocationContext validationInvocationContext,
                               Logger logger) {
        this.tokenGenerationServiceConsumer = tokenGenerationServiceConsumer;
        this.amSessionInvalidator = amSessionInvalidator;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.stsInstanceId = stsInstanceId;
        this.realm = realm;
        this.authnContextMapper = authnContextMapper;
        this.validationInvocationContext = validationInvocationContext;
        this.logger = logger;
    }

    @Override
    public JsonValue createToken(RestTokenProviderParameters<Saml2TokenCreationState> tokenParameters) throws TokenCreationException {
        try {
            Saml2TokenCreationState saml2TokenCreationState = tokenParameters.getTokenCreationState();

            final String authNContextClassRef = authnContextMapper.getAuthnContext(tokenParameters.getInputTokenType(),
                    tokenParameters.getInputToken());
            final String assertion = getAssertion(authNContextClassRef, saml2TokenCreationState.getSubjectConfirmation(),
                        saml2TokenCreationState.getProofTokenState());
            return json(object(field(AMSTSConstants.ISSUED_TOKEN, assertion)));
        } finally {
            try {
                amSessionInvalidator.invalidateAMSessions(threadLocalAMTokenCache.getToBeInvalidatedAMSessionIds());
            } catch (Exception e) {
                String message = "Exception caught invalidating interim AMSession: " + e;
                logger.warn(message, e);
                /*
                The fact that the interim OpenAM session was not invalidated should not prevent a token from being issued, so
                I will not throw a AMSTSRuntimeException
                */
            }
        }
    }

    /*
    Throw TokenCreationException as threadLocalAMTokenCache.getAMToken throws a TokenCreationException. Let caller above
    map that to an AMSTSRuntimeException.
     */
    private String getAssertion(String authnContextClassRef, SAML2SubjectConfirmation subjectConfirmation,
                                ProofTokenState proofTokenState) throws TokenCreationException {
        switch (subjectConfirmation) {
            case BEARER:
                return tokenGenerationServiceConsumer.getSAML2BearerAssertion(
                        threadLocalAMTokenCache.getSessionIdForContext(validationInvocationContext),
                        stsInstanceId, realm, authnContextClassRef, getAdminToken());
            case SENDER_VOUCHES:
                /*
                Note that for the rest-sts, there is no delegated token relationship, as there is in ws-trust, so I just
                pull the standard, non-delegated AMSessionId from the ThreadLocalAMTokenCache.
                 */
                return tokenGenerationServiceConsumer.getSAML2SenderVouchesAssertion(
                        threadLocalAMTokenCache.getSessionIdForContext(validationInvocationContext),
                        stsInstanceId, realm, authnContextClassRef, getAdminToken());
            case HOLDER_OF_KEY:
                return tokenGenerationServiceConsumer.getSAML2HolderOfKeyAssertion(
                        threadLocalAMTokenCache.getSessionIdForContext(validationInvocationContext),
                        stsInstanceId, realm, authnContextClassRef, proofTokenState, getAdminToken());
        }
        throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                "Unexpected SAML2SubjectConfirmation in AMSAMLTokenProvider: " + subjectConfirmation);
    }

    private String getAdminToken() {
        return AccessController.doPrivileged(AdminTokenAction.getInstance()).getTokenID().toString();
    }
}
