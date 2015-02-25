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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.tokengeneration.service;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.service.invocation.TokenGenerationServiceInvocationState;
import org.forgerock.openam.sts.tokengeneration.saml2.RestSTSInstanceState;
import org.forgerock.openam.sts.tokengeneration.saml2.SAML2TokenGeneration;
import org.forgerock.openam.sts.tokengeneration.saml2.STSInstanceStateProvider;
import org.slf4j.Logger;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * This service will be consumed by the REST/SOAP STS to issue tokens, initially focused on SAML assertions.
 *
 * The REST/SOAP STS will have credentials similar to agents, and a SSOToken corresponding to these credentials
 * will be required to consume this service, enforced by a auth filter. TODO: enforcement must be added
 *
 */
class TokenGenerationService implements SingletonResourceProvider {
    private final SAML2TokenGeneration saml2TokenGeneration;
    private final STSInstanceStateProvider<RestSTSInstanceState> restStsInstanceStateProvider;
    private final Logger logger;

    /*
    Ctor invoked by the TokenGenerationServiceConnectionFactory, using the SAML2TokenGeneration, STSInstanceStateProvider,
    and Logger bound by guice.

    Once this service starts being consumed by SOAP, I need to bind a STSInstanceStateProvider<SoapSTSInstanceState>
     */
    TokenGenerationService(SAML2TokenGeneration saml2TokenGeneration, STSInstanceStateProvider<RestSTSInstanceState> restStsInstanceStateProvider, Logger logger) {
        this.saml2TokenGeneration = saml2TokenGeneration;
        this.restStsInstanceStateProvider = restStsInstanceStateProvider;
        this.logger = logger;
    }
    public static final String ISSUE = "issue";

    public void actionInstance(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        if (ISSUE.equals(request.getAction())) {
            TokenGenerationServiceInvocationState invocationState;
            try {
                invocationState = TokenGenerationServiceInvocationState.fromJson(request.getContent());
            } catch (Exception e) {
                logger.error("Exception caught marshalling json into TokenGenerationServiceInvocationState instance: " + e);
                handler.handleError(new BadRequestException(e));
                return;
            }
            if (!AMSTSConstants.STSType.REST.equals(invocationState.getStsType())) {
                handler.handleError(new BadRequestException("Only the REST STS can currently call the token generation service."));
                return;
            }
            if (TokenType.SAML2.equals(invocationState.getTokenType())) {
                SSOToken subjectToken;
                SSOTokenManager tokenManager;
                try {
                    tokenManager = SSOTokenManager.getInstance();
                    subjectToken = tokenManager.createSSOToken(invocationState.getSsoTokenString());
                } catch (SSOException e) {
                    logger.debug("Exception caught creating the SSO token from the token string, almost certainly " +
                            "because token string does not correspond to a valid session: " + e);
                    handler.handleError(new ForbiddenException(e.toString(), e));
                    return;
                }
                if (!tokenManager.isValidToken(subjectToken)) {
                    handler.handleError(new ForbiddenException("SSO token string does not correspond to a valid SSOToken"));
                    return;
                }

                try {
                    final String assertion = saml2TokenGeneration.generate(
                            subjectToken,
                            restStsInstanceStateProvider.getSTSInstanceState(invocationState.getStsInstanceId(), invocationState.getRealm()),
                            invocationState);
                    handler.handleResult(json(object(field(AMSTSConstants.ISSUED_TOKEN, assertion))));
                    return;
                } catch (TokenCreationException e) {
                    logger.error("Exception caught generating saml2 token: " + e, e);
                    handler.handleError(e);
                    return;
                } catch (STSPublishException e) {
                    logger.error("Exception caught generating saml2 token: " + e, e);
                    handler.handleError(e);
                    return;
                } catch (Exception e) {
                    logger.error("Exception caught generating saml2 token: " + e, e);
                    handler.handleError(new InternalServerErrorException(e.toString(), e));
                    return;
                }
            } else {
                String message = "Bad request: unexpected token type:" + invocationState.getTokenType();
                logger.error(message);
                handler.handleError(new BadRequestException(message));
                return;
            }
        } else {
            handler.handleError(new BadRequestException("The specified _action parameter is not supported."));
        }
    }

    public void patchInstance(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }

    public void readInstance(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }

    public void updateInstance(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }
}