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

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.encode.Hash;
import com.sun.identity.sm.DNMapper;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.CTSTokenPersistenceException;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.service.invocation.TokenGenerationServiceInvocationState;
import org.forgerock.openam.sts.tokengeneration.CTSTokenPersistence;
import org.forgerock.openam.sts.tokengeneration.oidc.OpenIdConnectTokenGeneration;
import org.forgerock.openam.sts.tokengeneration.saml2.SAML2TokenGeneration;
import org.forgerock.openam.sts.tokengeneration.state.RestSTSInstanceState;
import org.forgerock.openam.sts.tokengeneration.state.STSInstanceState;
import org.forgerock.openam.sts.tokengeneration.state.STSInstanceStateProvider;
import org.forgerock.openam.sts.tokengeneration.state.SoapSTSInstanceState;
import org.forgerock.openam.sts.user.invocation.STSIssuedTokenState;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;
import org.slf4j.Logger;

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
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request) {
        TokenGenerationServiceInvocationState invocationState;
        try {
            invocationState = TokenGenerationServiceInvocationState.fromJson(request.getContent());
        } catch (Exception e) {
            logger.error("Exception caught marshalling json into TokenGenerationServiceInvocationState instance: " + e);
            return new BadRequestException(e.getMessage(), e).asPromise();
        }
        SSOToken subjectToken;
        try {
            subjectToken = validateAssertionSubjectSession(invocationState);
        } catch (ForbiddenException e) {
            return e.asPromise();
        }

        STSInstanceState stsInstanceState;
        try {
            stsInstanceState = getSTSInstanceState(invocationState);
        } catch (ResourceException e) {
            return e.asPromise();
        }

        if (TokenType.SAML2.equals(invocationState.getTokenType())) {
            try {
                final String assertion = saml2TokenGeneration.generate(
                        subjectToken,
                        stsInstanceState,
                        invocationState);
                return newResultPromise(issuedTokenResource(assertion));
            } catch (TokenCreationException e) {
                logger.error("Exception caught generating saml2 token: " + e, e);
                return e.asPromise();
            } catch (Exception e) {
                logger.error("Exception caught generating saml2 token: " + e, e);
                return new InternalServerErrorException(e.toString(), e).asPromise();
            }
        } else if (TokenType.OPENIDCONNECT.equals(invocationState.getTokenType())) {
            try {
                final String assertion = openIdConnectTokenGeneration.generate(
                        subjectToken,
                        stsInstanceState,
                        invocationState);
                return newResultPromise(issuedTokenResource(assertion));
            } catch (TokenCreationException e) {
                logger.error("Exception caught generating OpenIdConnect token: " + e, e);
                return e.asPromise();
            } catch (Exception e) {
                logger.error("Exception caught generating OpenIdConnect token: " + e, e);
                return new InternalServerErrorException(e.toString(), e).asPromise();
            }
        } else {
            String message = "Bad request: unexpected token type:" + invocationState.getTokenType();
            logger.error(message);
            return new BadRequestException(message).asPromise();
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
    private ResourceResponse issuedTokenResource(String assertion) {
        return newResourceResponse(UUID.randomUUID().toString(), Hash.hash(assertion),
                json(object(field(AMSTSConstants.ISSUED_TOKEN, assertion))));
    }

    private SSOToken validateAssertionSubjectSession(TokenGenerationServiceInvocationState invocationState)
            throws ForbiddenException {
        SSOToken subjectToken;
        SSOTokenManager tokenManager;
        try {
            tokenManager = SSOTokenManager.getInstance();
            subjectToken = tokenManager.createSSOToken(invocationState.getSsoTokenString());
        } catch (SSOException e) {
            logger.debug("Exception caught creating the SSO token from the token string, almost certainly "
                    + "because token string does not correspond to a valid session: " + e);
            throw new ForbiddenException(e.toString(), e);
        }
        if (!tokenManager.isValidToken(subjectToken)) {
            throw new ForbiddenException("SSO token string does not correspond to a valid SSOToken");
        }
        try {
            AMIdentity subjectIdentity = IdUtils.getIdentity(subjectToken);
            String invocationRealm = invocationState.getRealm();
            String subjectSessionRealm = DNMapper.orgNameToRealmName(subjectIdentity.getRealm());
            logger.debug("TokenGenerationService:validateAssertionSubjectSession subjectRealm " + subjectSessionRealm
                    + " invocation realm: " + invocationRealm);
            if (!invocationRealm.equalsIgnoreCase(subjectSessionRealm)) {
                logger.error("TokenGenerationService:validateAssertionSubjectSession realms do not match: Subject realm : "
                        + subjectSessionRealm + " invocation realm: " + invocationRealm);
                throw new ForbiddenException("SSO token subject realm does not match invocation realm");
            }
        } catch (SSOException | IdRepoException e) {
            logger.error("TokenGenerationService:validateAssertionSubjectSession error while validating identity : " + e);
            throw new ForbiddenException(e.toString(), e);
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
    public Promise<ResourceResponse, ResourceException> readInstance(final Context serverContext,
            final String resourceId, final ReadRequest readRequest) {
        try {
            String token = ctsTokenPersistence.getToken(resourceId);
            if (token != null) {
                return newResultPromise(issuedTokenResource(token));
            } else {
                return new NotFoundException("STS-issued token with id " + resourceId + " not found.").asPromise();
            }
        } catch (CTSTokenPersistenceException e) {
            logger.error("Exception caught reading token with id " + resourceId + ": " + e, e);
            return new InternalServerErrorException(e.toString(), e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(final Context serverContext,
            final String resourceId, final DeleteRequest deleteRequest) {
        try {
            ctsTokenPersistence.deleteToken(resourceId);
            logger.debug("Deleted token with id: " + resourceId);
            return newResultPromise(newResourceResponse(resourceId, resourceId, json(object(field
                    (RESULT, "token with id " + resourceId + " successfully removed.")))));
        } catch (CTSTokenPersistenceException e) {
            logger.error("Exception caught deleting token with id " + resourceId + ": " + e, e);
            return new InternalServerErrorException(e.toString(), e).asPromise();
        }
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(final Context serverContext,
            final QueryRequest queryRequest, final QueryResourceHandler queryResultHandler) {
        QueryFilter<JsonPointer> queryFilter = queryRequest.getQueryFilter();
        if (queryFilter == null) {
            return new BadRequestException(getUsageString()).asPromise();
        }
        try {
            final QueryFilter<CoreTokenField> coreTokenFieldQueryFilter =
                    convertToCoreTokenFieldQueryFilter(queryFilter);
            final List<STSIssuedTokenState> issuedTokens = ctsTokenPersistence.listTokens(coreTokenFieldQueryFilter);
            for (STSIssuedTokenState tokenState : issuedTokens) {
                queryResultHandler.handleResource(newResourceResponse(tokenState.getTokenId(), EMPTY_STRING, tokenState.toJson()));
            }
            return newResultPromise(newQueryResponse());
        } catch (CTSTokenPersistenceException e) {
            logger.error("Exception caught obtaining list of sts-issued tokens: " + e, e);
            return e.asPromise();
        }
    }

    private QueryFilter<CoreTokenField> convertToCoreTokenFieldQueryFilter(QueryFilter<JsonPointer> queryFilter)
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
            public QueryFilter<CoreTokenField> visitAndFilter(Void aVoid, List<QueryFilter<JsonPointer>> subFilters) {
                List<QueryFilter<CoreTokenField>> subCoreTokenFieldFilters = new ArrayList<>(subFilters.size());
                for (QueryFilter<JsonPointer> filter : subFilters) {
                    subCoreTokenFieldFilters.add(filter.accept(this, aVoid));
                }
                return QueryFilter.and(subCoreTokenFieldFilters);
            }

            @Override
            public QueryFilter<CoreTokenField> visitEqualsFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
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
            public QueryFilter<CoreTokenField> visitBooleanLiteralFilter(Void aVoid, boolean value) {
                throw new IllegalArgumentException("Querying STS issued tokens via boolean literal unsupported. Query format: "
                        + getUsageString());
            }

            @Override
            public QueryFilter<CoreTokenField> visitContainsFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
                throw new IllegalArgumentException("Querying STS issued token via contains relationship unsupported. Query format: "
                        + getUsageString());
            }

            @Override
            public QueryFilter<CoreTokenField> visitExtendedMatchFilter(Void aVoid, JsonPointer field, String operator, Object valueAssertion) {
                throw new IllegalArgumentException("Querying STS issued token via extended match filter unsupported. Query format: "
                        + getUsageString());
            }

            @Override
            public QueryFilter<CoreTokenField> visitGreaterThanFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
                throw new IllegalArgumentException("Querying STS issued token via greater-than filter unsupported. Query format: "
                        + getUsageString());

            }

            @Override
            public QueryFilter<CoreTokenField> visitGreaterThanOrEqualToFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
                throw new IllegalArgumentException("Querying STS issued token via greater-than-or-equal-to filter unsupported. Query format:"
                        + getUsageString());
            }

            @Override
            public QueryFilter<CoreTokenField> visitLessThanFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
                throw new IllegalArgumentException("Querying STS issued token via less-than filter unsupported. Query format: "
                        + getUsageString());
            }

            @Override
            public QueryFilter<CoreTokenField> visitLessThanOrEqualToFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
                throw new IllegalArgumentException("Querying STS issued token via less-than-or-equal-to filter unsupported. Query format: "
                    + getUsageString());

            }

            @Override
            public QueryFilter<CoreTokenField> visitNotFilter(Void aVoid, QueryFilter<JsonPointer> subFilter) {
                throw new IllegalArgumentException("Querying STS issued token via not filter unsupported. Query format: "
                    + getUsageString());

            }

            @Override
            public QueryFilter<CoreTokenField> visitOrFilter(Void aVoid, List<QueryFilter<JsonPointer>> subFilters) {
                throw new IllegalArgumentException("Querying STS issued token via or filter unsupported. Query format: "
                    + getUsageString());

            }

            @Override
            public QueryFilter<CoreTokenField> visitPresentFilter(Void aVoid, JsonPointer field) {
                throw new IllegalArgumentException("Querying STS issued token via present filter unsupported. Query format: "
                    + getUsageString());

            }

            @Override
            public QueryFilter<CoreTokenField> visitStartsWithFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
                throw new IllegalArgumentException("Querying STS issued token via starts-with filter unsupported. Query format: "
                    + getUsageString());

            }
       };

    private static String getUsageString() {
        return "Url must have a url-encoded query param of format: _queryFilter=/" + STSIssuedTokenState.STS_ID_QUERY_ATTRIBUTE +
                " eq \"sts_instance_id\" or _queryFilter=/" + STSIssuedTokenState.STS_ID_QUERY_ATTRIBUTE +
                " eq \"sts_instance_id\" and /" + STSIssuedTokenState.STS_TOKEN_PRINCIPAL_QUERY_ATTRIBUTE + " eq \"token_principal_id\"";
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(final Context serverContext,
            final ActionRequest actionRequest) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(final Context serverContext, final String s,
            final ActionRequest actionRequest) {
        return RestUtils.generateUnsupportedOperation();
    }


    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(final Context serverContext, final String s,
            final PatchRequest patchRequest) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(final Context serverContext, final String s,
            final UpdateRequest updateRequest) {
        return RestUtils.generateUnsupportedOperation();
    }
}