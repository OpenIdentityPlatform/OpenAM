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
import org.forgerock.json.JsonPointer;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.http.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.RestUtils;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.CTSTokenPersistenceException;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.service.invocation.TokenGenerationServiceInvocationState;
import org.forgerock.openam.sts.tokengeneration.CTSTokenPersistence;
import org.forgerock.openam.sts.tokengeneration.oidc.OpenIdConnectTokenGeneration;
import org.forgerock.openam.sts.tokengeneration.state.RestSTSInstanceState;
import org.forgerock.openam.sts.tokengeneration.saml2.SAML2TokenGeneration;
import org.forgerock.openam.sts.tokengeneration.state.STSInstanceState;
import org.forgerock.openam.sts.tokengeneration.state.STSInstanceStateProvider;
import org.forgerock.openam.sts.tokengeneration.state.SoapSTSInstanceState;
import org.forgerock.openam.sts.user.invocation.STSIssuedTokenState;
import org.forgerock.openam.tokens.CoreTokenField;
import org.slf4j.Logger;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This service will be consumed by the REST/SOAP STS to issue tokens.
 *
 */
class TokenGenerationService implements CollectionResourceProvider {
    private static final String RESULT = "result";
    private static final String EMPTY_STRING = "";
    private final SAML2TokenGeneration saml2TokenGeneration;
    private final OpenIdConnectTokenGeneration openIdConnectTokenGeneration;
    private final STSInstanceStateProvider<RestSTSInstanceState> restSTSInstanceStateProvider;
    private final STSInstanceStateProvider<SoapSTSInstanceState> soapSTSInstanceStateProvider;
    private final CTSTokenPersistence ctsTokenPersistence;
    private final Logger logger;

    /*
    Ctor invoked by the TokenGenerationServiceConnectionFactory, using the SAML2TokenGeneration, STSInstanceStateProvider,
    and Logger bound by guice.
     */
    TokenGenerationService(SAML2TokenGeneration saml2TokenGeneration, OpenIdConnectTokenGeneration openIdConnectTokenGeneration,
                           STSInstanceStateProvider<RestSTSInstanceState> restSTSInstanceStateProvider,
                           STSInstanceStateProvider<SoapSTSInstanceState> soapSTSInstanceStateProvider,
                           CTSTokenPersistence ctsTokenPersistence,
                           Logger logger) {
        this.saml2TokenGeneration = saml2TokenGeneration;
        this.openIdConnectTokenGeneration = openIdConnectTokenGeneration;
        this.restSTSInstanceStateProvider = restSTSInstanceStateProvider;
        this.soapSTSInstanceStateProvider = soapSTSInstanceStateProvider;
        this.ctsTokenPersistence = ctsTokenPersistence;
        this.logger = logger;
    }

    @Override
    public void createInstance(Context context, CreateRequest request, ResultHandler<Resource> handler) {
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
        } catch (TokenCreationException | STSPublishException e) {
            logger.error("Exception caught obtaining the sts instance state necessary to generate a saml2 assertion: " + e, e);
            throw e;
        } catch (Exception e) {
            logger.error("Exception caught obtaining the sts instance state necessary to generate a saml2 assertion: " + e, e);
            throw new InternalServerErrorException(e);
        }
        return stsInstanceState;
    }

    @Override
    public void readInstance(final Context serverContext, final String resourceId, final ReadRequest readRequest,
                             final ResultHandler<Resource> resultHandler) {
        try {
            String token = ctsTokenPersistence.getToken(resourceId);
            if (token != null) {
                resultHandler.handleResult(issuedTokenResource(token));
                return;
            } else {
                resultHandler.handleError(new NotFoundException("STS-issued token with id " + resourceId + " not found."));
                return;
            }
        } catch (CTSTokenPersistenceException e) {
            logger.error("Exception caught reading token with id " + resourceId + ": " + e, e);
            resultHandler.handleError(new InternalServerErrorException(e.toString(), e));
        }
    }

    @Override
    public void deleteInstance(final Context serverContext, final String resourceId, final DeleteRequest deleteRequest,
                               final ResultHandler<Resource> resultHandler) {
        try {
            ctsTokenPersistence.deleteToken(resourceId);
            resultHandler.handleResult(new Resource(resourceId, resourceId, json(object(field
                    (RESULT, "token with id " + resourceId + " successfully removed.")))));
            logger.debug("Deleted token with id: " + resourceId);
        } catch (CTSTokenPersistenceException e) {
            logger.error("Exception caught deleting token with id " + resourceId + ": " + e, e);
            resultHandler.handleError(new InternalServerErrorException(e.toString(), e));
        }
    }

    @Override
    public void queryCollection(final Context serverContext, final QueryRequest queryRequest,
                                final QueryResultHandler queryResultHandler) {
        QueryFilter queryFilter = queryRequest.getQueryFilter();
        if (queryFilter == null) {
            queryResultHandler.handleError(new BadRequestException(getUsageString()));
            return;
        }
        try {
            final org.forgerock.util.query.QueryFilter<CoreTokenField> coreTokenFieldQueryFilter =
                    convertToCoreTokenFieldQueryFilter(queryFilter);
            final List<STSIssuedTokenState> issuedTokens = ctsTokenPersistence.listTokens(coreTokenFieldQueryFilter);
            for (STSIssuedTokenState tokenState : issuedTokens) {
                queryResultHandler.handleResource(new Resource(tokenState.getTokenId(), EMPTY_STRING, tokenState.toJson()));
            }
            queryResultHandler.handleResult(new QueryResult());
        } catch (CTSTokenPersistenceException e) {
            logger.error("Exception caught obtaining list of sts-issued tokens: " + e, e);
            queryResultHandler.handleError(e);
        }
    }

    private org.forgerock.util.query.QueryFilter<CoreTokenField> convertToCoreTokenFieldQueryFilter(QueryFilter queryFilter)
                                                                throws CTSTokenPersistenceException {
        try {
            return queryFilter.accept(CORE_TOKEN_FIELD_QUERY_FILTER_VISITOR, null);
        } catch (IllegalArgumentException e) {
            throw new CTSTokenPersistenceException(ResourceException.BAD_REQUEST, e.getMessage(), e);
        }
    }

    private static final QueryFilterVisitor<QueryFilter<CoreTokenField>, Void, JsonPointer> CORE_TOKEN_FIELD_QUERY_FILTER_VISITOR =
        new QueryFilterVisitor<QueryFilter<CoreTokenField>, Void, JsonPointer>() {
            @Override
            public org.forgerock.util.query.QueryFilter<CoreTokenField> visitAndFilter(Void aVoid, List<QueryFilter<JsonPointer>> subFilters) {
                List<QueryFilter<CoreTokenField>> subCoreTokenFieldFilters = new ArrayList<>(subFilters.size());
                for (QueryFilter<JsonPointer> filter : subFilters) {
                    subCoreTokenFieldFilters.add(filter.accept(this, aVoid));
                }
                return QueryFilter.and(subCoreTokenFieldFilters);
            }

            @Override
            public org.forgerock.util.query.QueryFilter<CoreTokenField> visitEqualsFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
                final String fieldString = field.toString().substring(1);
                if (STSIssuedTokenState.STS_ID_QUERY_ATTRIBUTE.equals(fieldString)) {
                    return QueryFilter.equalTo(CTSTokenPersistence.CTS_TOKEN_FIELD_STS_ID, valueAssertion);
                } else if (STSIssuedTokenState.STS_TOKEN_PRINCIPAL_QUERY_ATTRIBUTE.equals(fieldString)) {
                    return QueryFilter.equalTo(CoreTokenField.USER_ID, valueAssertion);
                } else {
                    throw new IllegalArgumentException("Querying TokenService on field " + fieldString +
                            " not supported. Query format: " + getUsageString());
                }
            }

            @Override
            public org.forgerock.util.query.QueryFilter<CoreTokenField> visitBooleanLiteralFilter(Void aVoid, boolean value) {
                throw new IllegalArgumentException("Querying STS issued tokens via boolean literal unsupported. Query format: "
                        + getUsageString());
            }

            @Override
            public org.forgerock.util.query.QueryFilter<CoreTokenField> visitContainsFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
                throw new IllegalArgumentException("Querying STS issued token via contains relationship unsupported. Query format: "
                        + getUsageString());
            }

            @Override
            public org.forgerock.util.query.QueryFilter<CoreTokenField> visitExtendedMatchFilter(Void aVoid, JsonPointer field, String operator, Object valueAssertion) {
                throw new IllegalArgumentException("Querying STS issued token via extended match filter unsupported. Query format: "
                        + getUsageString());
            }

            @Override
            public org.forgerock.util.query.QueryFilter<CoreTokenField> visitGreaterThanFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
                throw new IllegalArgumentException("Querying STS issued token via greater-than filter unsupported. Query format: "
                        + getUsageString());

            }

            @Override
            public org.forgerock.util.query.QueryFilter<CoreTokenField> visitGreaterThanOrEqualToFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
                throw new IllegalArgumentException("Querying STS issued token via greater-than-or-equal-to filter unsupported. Query format:"
                        + getUsageString());
            }

            @Override
            public org.forgerock.util.query.QueryFilter<CoreTokenField> visitLessThanFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
                throw new IllegalArgumentException("Querying STS issued token via less-than filter unsupported. Query format: "
                        + getUsageString());
            }

            @Override
            public org.forgerock.util.query.QueryFilter<CoreTokenField> visitLessThanOrEqualToFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
                throw new IllegalArgumentException("Querying STS issued token via less-than-or-equal-to filter unsupported. Query format: "
                    + getUsageString());

            }

            @Override
            public org.forgerock.util.query.QueryFilter<CoreTokenField> visitNotFilter(Void aVoid, QueryFilter<JsonPointer> subFilter) {
                throw new IllegalArgumentException("Querying STS issued token via not filter unsupported. Query format: "
                    + getUsageString());

            }

            @Override
            public org.forgerock.util.query.QueryFilter<CoreTokenField> visitOrFilter(Void aVoid, List<QueryFilter<JsonPointer>> subFilters) {
                throw new IllegalArgumentException("Querying STS issued token via or filter unsupported. Query format: "
                    + getUsageString());

            }

            @Override
            public org.forgerock.util.query.QueryFilter<CoreTokenField> visitPresentFilter(Void aVoid, JsonPointer field) {
                throw new IllegalArgumentException("Querying STS issued token via present filter unsupported. Query format: "
                    + getUsageString());

            }

            @Override
            public org.forgerock.util.query.QueryFilter<CoreTokenField> visitStartsWithFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
                throw new IllegalArgumentException("Querying STS issued token via starts-with filter unsupported. Query format: "
                    + getUsageString());

            }
       };

    private static String getUsageString() {
        return "Url must have a query param of format: _queryFilter=/" + STSIssuedTokenState.STS_ID_QUERY_ATTRIBUTE +
                "+eq+\"sts_instance_id\" or _queryFilter=/" + STSIssuedTokenState.STS_ID_QUERY_ATTRIBUTE +
                "+eq+\"sts_instance_id\"+and+/" + STSIssuedTokenState.STS_TOKEN_PRINCIPAL_QUERY_ATTRIBUTE + "+eq+\"token_principal_id\"";
    }

    @Override
    public void actionCollection(final Context serverContext, final ActionRequest actionRequest,
                                 final ResultHandler<JsonValue> resultHandler) {
        RestUtils.generateUnsupportedOperation(resultHandler);
    }

    @Override
    public void actionInstance(final Context serverContext, final String s, final ActionRequest actionRequest,
                               final ResultHandler<JsonValue> resultHandler) {
        RestUtils.generateUnsupportedOperation(resultHandler);
    }


    @Override
    public void patchInstance(final Context serverContext, final String s, final PatchRequest patchRequest,
                              final ResultHandler<Resource> resultHandler) {
        RestUtils.generateUnsupportedOperation(resultHandler);
    }

    @Override
    public void updateInstance(final Context serverContext, final String s, final UpdateRequest updateRequest,
                               final ResultHandler<Resource> resultHandler) {
        RestUtils.generateUnsupportedOperation(resultHandler);
    }

}