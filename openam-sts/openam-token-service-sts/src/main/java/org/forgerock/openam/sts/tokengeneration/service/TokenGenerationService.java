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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.tokengeneration.service;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.encode.Hash;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.RestUtils;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.service.invocation.TokenGenerationServiceInvocationState;
import org.forgerock.openam.sts.tokengeneration.oidc.OpenIdConnectTokenGeneration;
import org.forgerock.openam.sts.tokengeneration.state.RestSTSInstanceState;
import org.forgerock.openam.sts.tokengeneration.saml2.SAML2TokenGeneration;
import org.forgerock.openam.sts.tokengeneration.state.STSInstanceState;
import org.forgerock.openam.sts.tokengeneration.state.STSInstanceStateProvider;
import org.forgerock.openam.sts.tokengeneration.state.SoapSTSInstanceState;
import org.slf4j.Logger;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

import java.util.UUID;

/**
 * This service will be consumed by the REST/SOAP STS to issue tokens.
 *
 */
class TokenGenerationService implements CollectionResourceProvider {
    private final SAML2TokenGeneration saml2TokenGeneration;
    private final OpenIdConnectTokenGeneration openIdConnectTokenGeneration;
    private final STSInstanceStateProvider<RestSTSInstanceState> restSTSInstanceStateProvider;
    private final STSInstanceStateProvider<SoapSTSInstanceState> soapSTSInstanceStateProvider;
    private final Logger logger;

    /*
    Ctor invoked by the TokenGenerationServiceConnectionFactory, using the SAML2TokenGeneration, STSInstanceStateProvider,
    and Logger bound by guice.
     */
    TokenGenerationService(SAML2TokenGeneration saml2TokenGeneration, OpenIdConnectTokenGeneration openIdConnectTokenGeneration,
                           STSInstanceStateProvider<RestSTSInstanceState> restSTSInstanceStateProvider,
                           STSInstanceStateProvider<SoapSTSInstanceState> soapSTSInstanceStateProvider, Logger logger) {
        this.saml2TokenGeneration = saml2TokenGeneration;
        this.openIdConnectTokenGeneration = openIdConnectTokenGeneration;
        this.restSTSInstanceStateProvider = restSTSInstanceStateProvider;
        this.soapSTSInstanceStateProvider = soapSTSInstanceStateProvider;
        this.logger = logger;
    }

    @Override
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        TokenGenerationServiceInvocationState invocationState;
        try {
            invocationState = TokenGenerationServiceInvocationState.fromJson(request.getContent());
        } catch (Exception e) {
            logger.error("Exception caught marshalling json into TokenGenerationServiceInvocationState instance: " + e);
            handler.handleError(new BadRequestException(e));
            return;
        }
        SSOToken subjectToken;
        try {
            subjectToken = validateAssertionSubjectSession(invocationState.getSsoTokenString());
        } catch (ForbiddenException e) {
            handler.handleError(e);
            return;
        }

        STSInstanceState stsInstanceState;
        try {
            stsInstanceState = getSTSInstanceState(invocationState);
        } catch (ResourceException e) {
            handler.handleError(e);
            return;
        }

        if (TokenType.SAML2.equals(invocationState.getTokenType())) {
            try {
                final String assertion = saml2TokenGeneration.generate(
                        subjectToken,
                        stsInstanceState,
                        invocationState);
                handler.handleResult(issuedTokenResource(assertion));
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
        } else if (TokenType.OPENIDCONNECT.equals(invocationState.getTokenType())) {
            try {
                final String assertion = openIdConnectTokenGeneration.generate(
                        subjectToken,
                        stsInstanceState,
                        invocationState);
                handler.handleResult(issuedTokenResource(assertion));
                return;
            } catch (TokenCreationException e) {
                logger.error("Exception caught generating OpenIdConnect token: " + e, e);
                handler.handleError(e);
                return;
            } catch (Exception e) {
                logger.error("Exception caught generating OpenIdConnect token: " + e, e);
                handler.handleError(new InternalServerErrorException(e.toString(), e));
                return;
            }
        } else {
            String message = "Bad request: unexpected token type:" + invocationState.getTokenType();
            logger.error(message);
            handler.handleError(new BadRequestException(message));
            return;
        }
    }

    /**
     * Generates a resource response for an issued token. The ID of the resource will be a random UUID, and the
     * revision is the base-64 encoded SHA-1 hash of the assertion. The content of the resource is a JSON object with
     * a single field "issued_token" whose content is the issued token assertion.
     *
     * @param assertion the assertion to return.
     * @return the assertion as a resource.
     */
    private Resource issuedTokenResource(String assertion) {
        return new Resource(UUID.randomUUID().toString(), Hash.hash(assertion),
                json(object(field(AMSTSConstants.ISSUED_TOKEN, assertion))));
    }

    private SSOToken validateAssertionSubjectSession(String sessionId) throws ForbiddenException {
        SSOToken subjectToken;
        SSOTokenManager tokenManager;
        try {
            tokenManager = SSOTokenManager.getInstance();
            subjectToken = tokenManager.createSSOToken(sessionId);
        } catch (SSOException e) {
            logger.debug("Exception caught creating the SSO token from the token string, almost certainly " +
                    "because token string does not correspond to a valid session: " + e);
            throw new ForbiddenException(e.toString(), e);
        }
        if (!tokenManager.isValidToken(subjectToken)) {
            throw new ForbiddenException("SSO token string does not correspond to a valid SSOToken");
        }
        return subjectToken;
    }

    private STSInstanceState getSTSInstanceState(TokenGenerationServiceInvocationState invocationState) throws ResourceException {
        STSInstanceState stsInstanceState;
        try {
            if (AMSTSConstants.STSType.REST.equals(invocationState.getStsType())) {
                stsInstanceState = restSTSInstanceStateProvider.getSTSInstanceState(invocationState.getStsInstanceId(), invocationState.getRealm());
            } else if (AMSTSConstants.STSType.SOAP.equals(invocationState.getStsType())) {
                stsInstanceState = soapSTSInstanceStateProvider.getSTSInstanceState(invocationState.getStsInstanceId(), invocationState.getRealm());
            } else {
                String message = "Illegal STSType specified in TokenGenerationService invocation: " + invocationState.getStsType();
                logger.error(message);
                throw new BadRequestException(message);
            }
        } catch (TokenCreationException e) {
            logger.error("Exception caught obtaining the sts instance state necessary to generate a saml2 assertion: " + e, e);
            throw e;
        } catch (STSPublishException e) {
            logger.error("Exception caught obtaining the sts instance state necessary to generate a saml2 assertion: " + e, e);
            throw e;
        } catch (Exception e) {
            logger.error("Exception caught obtaining the sts instance state necessary to generate a saml2 assertion: " + e, e);
            throw new InternalServerErrorException(e);
        }
        return stsInstanceState;
    }

    @Override
    public void actionCollection(final ServerContext serverContext, final ActionRequest actionRequest,
                                 final ResultHandler<JsonValue> resultHandler) {
        RestUtils.generateUnsupportedOperation(resultHandler);
    }

    @Override
    public void actionInstance(final ServerContext serverContext, final String s, final ActionRequest actionRequest,
                               final ResultHandler<JsonValue> resultHandler) {
        RestUtils.generateUnsupportedOperation(resultHandler);
    }

    @Override
    public void deleteInstance(final ServerContext serverContext, final String s, final DeleteRequest deleteRequest,
                               final ResultHandler<Resource> resultHandler) {
        RestUtils.generateUnsupportedOperation(resultHandler);
    }

    @Override
    public void patchInstance(final ServerContext serverContext, final String s, final PatchRequest patchRequest,
                              final ResultHandler<Resource> resultHandler) {
        RestUtils.generateUnsupportedOperation(resultHandler);
    }

    @Override
    public void queryCollection(final ServerContext serverContext, final QueryRequest queryRequest,
                                final QueryResultHandler queryResultHandler) {
        RestUtils.generateUnsupportedOperation(queryResultHandler);
    }

    @Override
    public void readInstance(final ServerContext serverContext, final String s, final ReadRequest readRequest,
                             final ResultHandler<Resource> resultHandler) {
        RestUtils.generateUnsupportedOperation(resultHandler);
    }

    @Override
    public void updateInstance(final ServerContext serverContext, final String s, final UpdateRequest updateRequest,
                               final ResultHandler<Resource> resultHandler) {
        RestUtils.generateUnsupportedOperation(resultHandler);
    }
}