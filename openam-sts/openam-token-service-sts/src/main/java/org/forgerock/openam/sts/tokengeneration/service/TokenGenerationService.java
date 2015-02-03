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
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.service.invocation.TokenGenerationServiceInvocationState;
import org.forgerock.openam.sts.tokengeneration.saml2.RestSTSInstanceState;
import org.forgerock.openam.sts.tokengeneration.saml2.SAML2TokenGeneration;
import org.forgerock.openam.sts.tokengeneration.saml2.STSInstanceState;
import org.forgerock.openam.sts.tokengeneration.saml2.STSInstanceStateProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.SoapSTSInstanceState;
import org.slf4j.Logger;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * This service will be consumed by the REST/SOAP STS to issue tokens, initially focused on SAML assertions.
 *
 */
class TokenGenerationService implements SingletonResourceProvider {
    private final SAML2TokenGeneration saml2TokenGeneration;
    private final STSInstanceStateProvider<RestSTSInstanceState> restSTSInstanceStateProvider;
    private final STSInstanceStateProvider<SoapSTSInstanceState> soapSTSInstanceStateProvider;
    private final Logger logger;

    /*
    Ctor invoked by the TokenGenerationServiceConnectionFactory, using the SAML2TokenGeneration, STSInstanceStateProvider,
    and Logger bound by guice.

    Once this service starts being consumed by SOAP, I need to bind a STSInstanceStateProvider<SoapSTSInstanceState>
     */
    TokenGenerationService(SAML2TokenGeneration saml2TokenGeneration, STSInstanceStateProvider<RestSTSInstanceState> restSTSInstanceStateProvider,
                           STSInstanceStateProvider<SoapSTSInstanceState> soapSTSInstanceStateProvider, Logger logger) {
        this.saml2TokenGeneration = saml2TokenGeneration;
        this.restSTSInstanceStateProvider = restSTSInstanceStateProvider;
        this.soapSTSInstanceStateProvider = soapSTSInstanceStateProvider;
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
                STSInstanceState stsInstanceState = null;
                try {
                    if (AMSTSConstants.STSType.REST.equals(invocationState.getStsType())) {
                        stsInstanceState = restSTSInstanceStateProvider.getSTSInstanceState(invocationState.getStsInstanceId(), invocationState.getRealm());
                    } else if (AMSTSConstants.STSType.SOAP.equals(invocationState.getStsType())) {
                        stsInstanceState = soapSTSInstanceStateProvider.getSTSInstanceState(invocationState.getStsInstanceId(), invocationState.getRealm());
                    } else {
                        String message = "Illegal STSType specified in TokenGenerationService invocation: " + invocationState.getStsType();
                        logger.error(message);
                        handler.handleError(new BadRequestException(message));
                    }
                } catch (TokenCreationException e) {
                    logger.error("Exception caught obtaining the sts instance state necessary to generate a saml2 assertion: " + e, e);
                    handler.handleError(e);
                    return;
                } catch (STSPublishException e) {
                    logger.error("Exception caught obtaining the sts instance state necessary to generate a saml2 assertion: " + e, e);
                    handler.handleError(e);
                    return;
                } catch (Exception e) {
                    logger.error("Exception caught obtaining the sts instance state necessary to generate a saml2 assertion: " + e, e);
                    handler.handleError(new InternalServerErrorException(e));
                    return;
                }
                try {
                    final String assertion = saml2TokenGeneration.generate(
                            subjectToken,
                            stsInstanceState,
                            invocationState);
                    handler.handleResult(json(object(field(AMSTSConstants.ISSUED_TOKEN, assertion))));
                    return;
                } catch (TokenCreationException e) {
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