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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.rest.uma;

import static org.forgerock.openam.uma.UmaConstants.UMA_POLICY_SCHEME;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryFilterVisitor;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.openam.rest.oauth2.AggregateQuery;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.forgerock.openam.uma.PolicySearch;
import org.forgerock.openam.uma.UmaPolicy;
import org.forgerock.openam.uma.UmaPolicyQueryFilterVisitor;
import org.forgerock.openam.uma.UmaPolicyService;
import org.forgerock.openam.uma.audit.UmaAuditLogger;
import org.forgerock.openam.uma.audit.UmaAuditType;
import org.forgerock.openam.utils.Config;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.AsyncFunction;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.promise.SuccessHandler;

/**
 * Implementation of the {@code UmaPolicyService}.
 *
 * @since 13.0.0
 */
@Singleton
public class UmaPolicyServiceImpl implements UmaPolicyService {

    private final PolicyResourceDelegate policyResourceDelegate;
    private final ResourceSetStoreFactory resourceSetStoreFactory;
    private final Config<UmaAuditLogger> auditLogger;
    private final ContextHelper contextHelper;

    /**
     * Creates an instance of the {@code UmaPolicyServiceImpl}.
     *
     * @param policyResourceDelegate An instance of the {@code PolicyResourceDelegate}.
     * @param resourceSetStoreFactory An instance of the {@code ResourceSetStoreFactory}.
     * @param auditLogger An instance of the {@code UmaAuditLogger}.
     * @param contextHelper An instance of the {@code ContextHelper}.
     */
    @Inject
    public UmaPolicyServiceImpl(PolicyResourceDelegate policyResourceDelegate,
            ResourceSetStoreFactory resourceSetStoreFactory, Config<UmaAuditLogger> auditLogger,
            ContextHelper contextHelper) {
        this.policyResourceDelegate = policyResourceDelegate;
        this.resourceSetStoreFactory = resourceSetStoreFactory;
        this.auditLogger = auditLogger;
        this.contextHelper = contextHelper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<UmaPolicy, ResourceException> createPolicy(final ServerContext context, JsonValue policy) {
        UmaPolicy umaPolicy;
        final ResourceSetDescription resourceSet;
        String resourceOwnerId = getResourceOwnerId(context);
        try {
            resourceSet = getResourceSetDescription(UmaPolicy.idOf(policy), resourceOwnerId, context);
            umaPolicy = UmaPolicy.valueOf(resourceSet, policy);
            validateScopes(resourceSet, umaPolicy.getScopes());
            verifyPolicyDoesNotAlreadyExist(context, resourceSet);
        } catch (ResourceException e) {
            return Promises.newFailedPromise(e);
        }
        return policyResourceDelegate.createPolicies(context, umaPolicy.asUnderlyingPolicies())
                .thenAsync(new AsyncFunction<List<Resource>, UmaPolicy, ResourceException>() {
                    @Override
                    public Promise<UmaPolicy, ResourceException> apply(List<Resource> value) {
                        try {
                            UmaPolicy umaPolicy = UmaPolicy.fromUnderlyingPolicies(resourceSet, value);
                            String userId = getLoggedInUserId(context);
                            auditLogger.get().log(resourceSet.getId(), resourceSet.getName(), userId,
                                    UmaAuditType.POLICY_CREATED, userId);
                            return Promises.newSuccessfulPromise(umaPolicy);
                        } catch (ResourceException e) {
                            return Promises.newFailedPromise(e);
                        }
                    }
                }, new AsyncFunction<ResourceException, UmaPolicy, ResourceException>() {
                    @Override
                    public Promise<UmaPolicy, ResourceException> apply(ResourceException error)  {
                        return Promises.newFailedPromise(error);
                    }
                });
    }

    private void verifyPolicyDoesNotAlreadyExist(ServerContext context, ResourceSetDescription resourceSet)
            throws ResourceException {
        try {
            readPolicy(context, resourceSet.getId()).getOrThrowUninterruptibly();
            throw new ConflictException("Policy already exists for Resource Server, "
                            + resourceSet.getClientId() + ", Resource Set, " + resourceSet.getId());
        } catch (NotFoundException e) {
            //If caught then this simply means the policy does not already exist so we can create it
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<UmaPolicy, ResourceException> readPolicy(final ServerContext context, final String resourceSetId) {
        final String resourceOwnerId = getResourceOwnerId(context);
        String resourceOwnerUid = getResourceOwnerUid(context);
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.and(
                        QueryFilter.equalTo("/resourceTypeUuid", resourceSetId),
                        QueryFilter.equalTo("/createdBy", resourceOwnerUid)));
        return policyResourceDelegate.queryPolicies(context, request)
                .thenAsync(new AsyncFunction<Pair<QueryResult, List<Resource>>, UmaPolicy, ResourceException>() {
                    @Override
                    public Promise<UmaPolicy, ResourceException> apply(Pair<QueryResult, List<Resource>> value) {
                        try {
                            if (value.getSecond().isEmpty()) {
                                return Promises.newFailedPromise(
                                        (ResourceException) new NotFoundException("UMA Policy not found, "
                                                + resourceSetId));
                            } else {
                                ResourceSetDescription resourceSet = getResourceSetDescription(resourceSetId, resourceOwnerId, context);
                                return Promises.newSuccessfulPromise(UmaPolicy.fromUnderlyingPolicies(resourceSet, value.getSecond()));
                            }
                        } catch (ResourceException e) {
                            return Promises.newFailedPromise(e);
                        }
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<UmaPolicy, ResourceException> updatePolicy(final ServerContext context, final String resourceSetId, //TODO need to check if need to delete backend policies
            JsonValue policy) {
        final UmaPolicy updatedUmaPolicy;
        final ResourceSetDescription resourceSet;
        final String resourceOwnerId = getResourceOwnerId(context);
        final String resourceOwnerUid = getResourceOwnerUid(context);
        try {
            resourceSet = getResourceSetDescription(resourceSetId, resourceOwnerId, context);
            updatedUmaPolicy = UmaPolicy.valueOf(resourceSet, policy);
            validateScopes(resourceSet, updatedUmaPolicy.getScopes());
        } catch (ResourceException e) {
            return Promises.newFailedPromise(e);
        }
        return readPolicy(context, resourceSetId)
                .onSuccess(new SuccessHandler<UmaPolicy>() {
                    @Override
                    public void handleResult(UmaPolicy currentUmaPolicy) {
                        Set<String> modifiedScopes = new HashSet<String>(updatedUmaPolicy.getScopes());
                        modifiedScopes.retainAll(currentUmaPolicy.getScopes());
                        Set<String> removedScopes = new HashSet<String>(currentUmaPolicy.getScopes());
                        removedScopes.removeAll(modifiedScopes);
                        for (JsonValue policy : currentUmaPolicy.asUnderlyingPolicies()) {
                            for (String scope : removedScopes) {
                                if (policy.get("actionValues").isDefined(scope)) {
                                    policyResourceDelegate.queryPolicies(context, Requests.newQueryRequest("")
                                            .setQueryFilter(QueryFilter.and(
                                                    QueryFilter.equalTo("/createdBy", resourceOwnerUid),
                                                    QueryFilter.equalTo("/name", policy.get("name").asString()))))
                                            .thenAsync(new AsyncFunction<Pair<QueryResult, List<Resource>>, Void, ResourceException>() {
                                                @Override
                                                public Promise<Void, ResourceException> apply(Pair<QueryResult, List<Resource>> result) {
                                                    for (Resource resource : result.getSecond()) {
                                                        policyResourceDelegate.deletePolicies(context, Collections.singleton(resource.getId()));
                                                    }
                                                    return Promises.newSuccessfulPromise(null);
                                                }
                                            });
                                }
                            }
                        }
                    }
                }).onSuccess(new SuccessHandler<UmaPolicy>() {
                    @Override
                    public void handleResult(UmaPolicy currentUmaPolicy) {
                        Set<String> modifiedScopes = new HashSet<String>(currentUmaPolicy.getScopes());
                        modifiedScopes.retainAll(updatedUmaPolicy.getScopes());
                        Set<String> deletedScopes = new HashSet<String>(updatedUmaPolicy.getScopes());
                        deletedScopes.removeAll(modifiedScopes);
                        for (JsonValue policy : updatedUmaPolicy.asUnderlyingPolicies()) {
                            for (String scope : deletedScopes) {
                                if (policy.get("actionValues").isDefined(scope)) {
                                    policyResourceDelegate.createPolicies(context, Collections.singleton(policy));
                                }
                            }
                        }
                    }
                }).thenAsync(new AsyncFunction<UmaPolicy, UmaPolicy, ResourceException>() {
                    @Override
                    public Promise<UmaPolicy, ResourceException> apply(UmaPolicy umaPolicy) throws ResourceException {
                        return policyResourceDelegate.updatePolicies(context, updatedUmaPolicy.asUnderlyingPolicies())
                                .thenAsync(new AsyncFunction<List<Resource>, UmaPolicy, ResourceException>() {
                                    @Override
                                    public Promise<UmaPolicy, ResourceException> apply(List<Resource> value) throws ResourceException {
                                        String userId = getLoggedInUserId(context);
                                        auditLogger.get().log(resourceSetId, resourceSet.getName(), userId, UmaAuditType.POLICY_UPDATED, userId);
                                        return Promises.newSuccessfulPromise(updatedUmaPolicy);
                                    }
                                }, new AsyncFunction<ResourceException, UmaPolicy, ResourceException>() {
                                    @Override
                                    public Promise<UmaPolicy, ResourceException> apply(ResourceException error) {
                                        return Promises.newFailedPromise(error);
                                    }
                                });
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<Void, ResourceException> deletePolicy(final ServerContext context, final String resourceSetId) {
        return readPolicy(context, resourceSetId)
                .thenAsync(new AsyncFunction<UmaPolicy, List<Resource>, ResourceException>() {
                    @Override
                    public Promise<List<Resource>, ResourceException> apply(UmaPolicy value) {
                        return policyResourceDelegate.deletePolicies(context, value.getUnderlyingPolicyIds());
                    }
                })
                .thenAsync(new AsyncFunction<List<Resource>, Void, ResourceException>() {
                    @Override
                    public Promise<Void, ResourceException> apply(List<Resource> value) {
                        return Promises.newSuccessfulPromise(null);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<Pair<QueryResult, Collection<UmaPolicy>>, ResourceException> queryPolicies(final ServerContext context,
            final QueryRequest umaQueryRequest) {


        if (umaQueryRequest.getQueryExpression() != null) {
            return Promises.newFailedPromise((ResourceException) new BadRequestException("Query expressions not supported"));
        } else if (umaQueryRequest.getQueryId() != null) {
            return Promises.newFailedPromise((ResourceException) new BadRequestException("Query expressions not supported"));
        }

        final AggregateQuery<QueryFilter, QueryFilter> filter = umaQueryRequest.getQueryFilter()
                .accept(new AggregateUmaPolicyQueryFilter(), new AggregateQuery<QueryFilter, QueryFilter>());

        String resourceOwnerUid = getResourceOwnerUid(context);
        QueryRequest request = Requests.newQueryRequest("");
        if (filter.getFirstQuery() == null) {
            request.setQueryFilter(QueryFilter.equalTo("/createdBy", resourceOwnerUid));
        } else {
            request.setQueryFilter(QueryFilter.and(QueryFilter.equalTo("/createdBy", resourceOwnerUid), filter.getFirstQuery()));
        }
        return policyResourceDelegate.queryPolicies(context, request)
                .thenAsync(new AsyncFunction<Pair<QueryResult, List<Resource>>, Collection<UmaPolicy>, ResourceException>() {
                    @Override
                    public Promise<Collection<UmaPolicy>, ResourceException> apply(Pair<QueryResult, List<Resource>> value) {
                        Map<String, Set<Resource>> policyMapping = new HashMap<String, Set<Resource>>();
                        for (Resource policy : value.getSecond()) {

                            String resource = policy.getContent().get("resources").asList(String.class).get(0);
                            resource = resource.replaceFirst(UMA_POLICY_SCHEME, "");
                            if (resource.indexOf(":") > 0) {
                                resource = resource.substring(0, resource.indexOf(":"));
                            }

                            Set<Resource> mapping = policyMapping.get(resource);
                            if (mapping == null) {
                                mapping = new HashSet<Resource>();
                                policyMapping.put(resource, mapping);
                            }

                            mapping.add(policy);
                        }

                        try {
                            Collection<UmaPolicy> umaPolicies = new HashSet<UmaPolicy>();
                            String resourceOwnerId = getResourceOwnerId(context);
                            for (Map.Entry<String, Set<Resource>> entry : policyMapping.entrySet()) {
                                ResourceSetDescription resourceSet = getResourceSetDescription(entry.getKey(), resourceOwnerId, context);
                                umaPolicies.add(UmaPolicy.fromUnderlyingPolicies(resourceSet, entry.getValue()));
                            }
                            return Promises.newSuccessfulPromise(umaPolicies);
                        } catch (ResourceException e) {
                            return Promises.newFailedPromise(e);
                        }
                    }
                })
                .thenAsync(new AsyncFunction<Collection<UmaPolicy>, Pair<QueryResult, Collection<UmaPolicy>>, ResourceException>() {
                    @Override
                    public Promise<Pair<QueryResult, Collection<UmaPolicy>>, ResourceException> apply(Collection<UmaPolicy> policies) {
                        Collection<UmaPolicy> results = policies;
                        if (filter.getSecondQuery() != null) {
                            PolicySearch search = filter.getSecondQuery().accept(new UmaPolicyQueryFilterVisitor(), new PolicySearch(policies));
                            if (AggregateQuery.Operator.AND.equals(filter.getOperator())) {
                                results.retainAll(search.getPolicies());
                            }
                        }

                        int pageSize = umaQueryRequest.getPageSize();
                        String pagedResultsCookie = umaQueryRequest.getPagedResultsCookie();
                        int pagedResultsOffset = umaQueryRequest.getPagedResultsOffset();

                        Collection<UmaPolicy> pagedPolicies = new HashSet<UmaPolicy>();
                        int count = 0;
                        for (UmaPolicy policy : results) {
                            if (count >= pagedResultsOffset * pageSize) {
                                pagedPolicies.add(policy);
                            }
                            count++;
                        }
                        int remainingPagedResults = results.size() - pagedPolicies.size();
                        if (pageSize > 0) {
                            remainingPagedResults /= pageSize;
                        }

                        return Promises.newSuccessfulPromise(Pair.of(
                                new QueryResult(pagedResultsCookie, remainingPagedResults), pagedPolicies));
                    }
                });
    }

    private static final class AggregateUmaPolicyQueryFilter
            implements QueryFilterVisitor<AggregateQuery<QueryFilter, QueryFilter>, AggregateQuery<QueryFilter, QueryFilter>> {

        private int queryDepth = 0;

        private static final Map<JsonPointer, JsonPointer> queryableFields = new HashMap<JsonPointer, JsonPointer>();
        static {
            queryableFields.put(new JsonPointer("/resourceServer"), new JsonPointer("applicationName"));
        }

        private JsonPointer verifyFieldIsQueryable(JsonPointer field) {
            if (!queryableFields.containsKey(field)) {
                throw new UnsupportedOperationException("'" + field + "' not queryable");
            } else {
                return queryableFields.get(field);
            }
        }

        private void increaseQueryDepth() {
            queryDepth++;
        }

        private void decreaseQueryDepth() {
            queryDepth--;
        }

        @Override
        public AggregateQuery<QueryFilter, QueryFilter> visitAndFilter(AggregateQuery<QueryFilter, QueryFilter> query, List<QueryFilter> subFilters) {
            increaseQueryDepth();
            List<QueryFilter> childFilters = new ArrayList<QueryFilter>();
            for (QueryFilter filter : subFilters) {
                AggregateQuery<QueryFilter, QueryFilter> subAggregateQuery = filter.accept(this, query);
                if (subAggregateQuery.getSecondQuery() != null) {
                    subAggregateQuery.setOperator(AggregateQuery.Operator.AND);
                } else {
                    childFilters.add(subAggregateQuery.getFirstQuery());
                }
            }
            decreaseQueryDepth();
            query.setFirstQuery(QueryFilter.and(childFilters));
            return query;
        }

        @Override
        public AggregateQuery<QueryFilter, QueryFilter> visitEqualsFilter(AggregateQuery<QueryFilter, QueryFilter> query, JsonPointer field, Object valueAssertion) {
            if (new JsonPointer("/permissions/subject").equals(field)) {
                if (queryDepth > 1) {
                    throw new UnsupportedOperationException("Cannot nest queries on /permissions/subject field");
                }
                query.setSecondQuery(QueryFilter.equalTo("/permissions/subject", valueAssertion));
            } else {
                query.setFirstQuery(QueryFilter.equalTo(verifyFieldIsQueryable(field), valueAssertion));
            }
            return query;
        }

        @Override
        public AggregateQuery<QueryFilter, QueryFilter> visitStartsWithFilter(AggregateQuery<QueryFilter, QueryFilter> query, JsonPointer field, Object valueAssertion) {
            if (new JsonPointer("/permissions/subject").equals(field)) {
                if (queryDepth > 1) {
                    throw new UnsupportedOperationException("Cannot nest queries on /permissions/subject field");
                }
                query.setSecondQuery(QueryFilter.startsWith("/permissions/subject", valueAssertion));
            } else {
                query.setFirstQuery(QueryFilter.startsWith(verifyFieldIsQueryable(field), valueAssertion));
            }
            return query;
        }

        @Override
        public AggregateQuery<QueryFilter, QueryFilter> visitContainsFilter(AggregateQuery<QueryFilter, QueryFilter> query, JsonPointer field, Object valueAssertion) {
            if (new JsonPointer("/permissions/subject").equals(field)) {
                if (queryDepth > 1) {
                    throw new UnsupportedOperationException("Cannot nest queries on /permissions/subject field");
                }
                query.setSecondQuery(QueryFilter.contains("/permissions/subject", valueAssertion));
            } else {
                query.setFirstQuery(QueryFilter.contains(verifyFieldIsQueryable(field), valueAssertion));
            }
            return query;
        }

        @Override
        public AggregateQuery<QueryFilter, QueryFilter> visitBooleanLiteralFilter(AggregateQuery<QueryFilter, QueryFilter> query, boolean value) {
            if (value) {
                return query;
            }
            throw unsupportedFilterOperation("Boolean Literal, false,");
        }

        @Override
        public AggregateQuery<QueryFilter, QueryFilter> visitExtendedMatchFilter(AggregateQuery<QueryFilter, QueryFilter> query, JsonPointer field, String operator, Object valueAssertion) {
            throw unsupportedFilterOperation("Extended match");
        }

        @Override
        public AggregateQuery<QueryFilter, QueryFilter> visitGreaterThanFilter(AggregateQuery<QueryFilter, QueryFilter> query, JsonPointer field, Object valueAssertion) {
            throw unsupportedFilterOperation("Greater than");
        }

        @Override
        public AggregateQuery<QueryFilter, QueryFilter> visitGreaterThanOrEqualToFilter(AggregateQuery<QueryFilter, QueryFilter> query, JsonPointer field, Object valueAssertion) {
            throw unsupportedFilterOperation("Greater than or equal");
        }

        @Override
        public AggregateQuery<QueryFilter, QueryFilter> visitLessThanFilter(AggregateQuery<QueryFilter, QueryFilter> query, JsonPointer field, Object valueAssertion) {
            throw unsupportedFilterOperation("Less than");
        }

        @Override
        public AggregateQuery<QueryFilter, QueryFilter> visitLessThanOrEqualToFilter(AggregateQuery<QueryFilter, QueryFilter> query, JsonPointer field, Object valueAssertion) {
            throw unsupportedFilterOperation("Less than or equal");
        }

        @Override
        public AggregateQuery<QueryFilter, QueryFilter> visitNotFilter(AggregateQuery<QueryFilter, QueryFilter> query, QueryFilter subFilter) {
            throw unsupportedFilterOperation("Not");
        }

        @Override
        public AggregateQuery<QueryFilter, QueryFilter> visitOrFilter(AggregateQuery<QueryFilter, QueryFilter> query, List<QueryFilter> subFilters) {
            increaseQueryDepth();
            List<QueryFilter> childFilters = new ArrayList<QueryFilter>();
            for (QueryFilter filter : subFilters) {
                AggregateQuery<QueryFilter, QueryFilter> subAggregateQuery = filter.accept(this, query);
                if (subAggregateQuery.getSecondQuery() != null) {
                    subAggregateQuery.setOperator(AggregateQuery.Operator.OR);
                } else {
                    childFilters.add(subAggregateQuery.getFirstQuery());
                }
            }
            increaseQueryDepth();
            query.setFirstQuery(QueryFilter.or(childFilters));
            return query;
        }

        @Override
        public AggregateQuery<QueryFilter, QueryFilter> visitPresentFilter(AggregateQuery<QueryFilter, QueryFilter> query, JsonPointer field) {
            throw unsupportedFilterOperation("Present");
        }

        private UnsupportedOperationException unsupportedFilterOperation(String filterType) {
            return new UnsupportedOperationException("'" + filterType + "' not supported");
        }
    }

    private ResourceSetDescription getResourceSetDescription(String resourceSetId, String resourceOwnerId,
            ServerContext context) throws ResourceException {
        try {
            String realm = getRealm(context);
            return resourceSetStoreFactory.create(realm).read(resourceSetId, resourceOwnerId);
        } catch (org.forgerock.oauth2.core.exceptions.NotFoundException e) {
            throw new BadRequestException("Invalid ResourceSet UID");
        } catch (ServerException e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }

    private void validateScopes(ResourceSetDescription resourceSet, Set<String> requestedScopes)
            throws BadRequestException {
        Set<String> availableScopes = resourceSet.getDescription().get("scopes").asSet(String.class);
        if (!availableScopes.containsAll(requestedScopes)) {
            throw new BadRequestException("Defined Resource Set scopes, " + availableScopes.toString()
                    + ", do not contain requested scopes, " + requestedScopes.toString());
        }
    }

    private String getLoggedInUserId(ServerContext context) throws InternalServerErrorException {
        try {
            SubjectContext subjectContext = context.asContext(SubjectContext.class);
            SSOToken token = subjectContext.getCallerSSOToken();
            return token.getPrincipal().getName();
        } catch (SSOException e) {
            throw new InternalServerErrorException(e);
        }
    }

    private String getResourceOwnerUid(ServerContext context) {
        return contextHelper.getUserUid(context);
    }

    private String getResourceOwnerId(ServerContext context) {
        return contextHelper.getUserId(context);
    }

    private String getRealm(ServerContext context) {
        return contextHelper.getRealm(context);
    }
}
