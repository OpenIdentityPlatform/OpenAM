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

import static java.util.Collections.singleton;
import static org.forgerock.openam.uma.UmaConstants.UMA_POLICY_SCHEME;
import static org.forgerock.util.promise.Promises.*;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.security.auth.Subject;
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
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Evaluator;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.http.context.ServerContext;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.openam.cts.api.fields.ResourceSetTokenField;
import org.forgerock.openam.forgerockrest.authn.core.wrappers.CoreServicesWrapper;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.openam.rest.oauth2.AggregateQuery;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.forgerock.openam.uma.ResharingMode;
import org.forgerock.openam.uma.PolicySearch;
import org.forgerock.openam.uma.UmaConstants;
import org.forgerock.openam.uma.UmaPolicy;
import org.forgerock.openam.uma.UmaPolicyQueryFilterVisitor;
import org.forgerock.openam.uma.UmaPolicyService;
import org.forgerock.openam.uma.UmaSettingsFactory;
import org.forgerock.openam.uma.UmaUtils;
import org.forgerock.openam.uma.audit.UmaAuditLogger;
import org.forgerock.openam.uma.audit.UmaAuditType;
import org.forgerock.openam.utils.Config;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.promise.ResultHandler;

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
    private final UmaPolicyEvaluatorFactory policyEvaluatorFactory;
    private final CoreServicesWrapper coreServicesWrapper;
    private final Debug debug;
    private final UmaSettingsFactory umaSettingsFactory;

    /**
     * Creates an instance of the {@code UmaPolicyServiceImpl}.
     *
     * @param policyResourceDelegate An instance of the {@code PolicyResourceDelegate}.
     * @param resourceSetStoreFactory An instance of the {@code ResourceSetStoreFactory}.
     * @param auditLogger An instance of the {@code UmaAuditLogger}.
     * @param contextHelper An instance of the {@code ContextHelper}.
     * @param policyEvaluatorFactory An instance of the {@code UmaPolicyEvaluatorFactory}.
     * @param coreServicesWrapper An instance of the {@code CoreServicesWrapper}.
     * @param debug An instance of the REST {@code Debug}.
     * @param umaSettingsFactory An instance of the {@code UmaSettingsFactory}.
     */
    @Inject
    public UmaPolicyServiceImpl(PolicyResourceDelegate policyResourceDelegate,
            ResourceSetStoreFactory resourceSetStoreFactory, Config<UmaAuditLogger> auditLogger,
            ContextHelper contextHelper, UmaPolicyEvaluatorFactory policyEvaluatorFactory,
            CoreServicesWrapper coreServicesWrapper, @Named("frRest") Debug debug,
            UmaSettingsFactory umaSettingsFactory) {
        this.policyResourceDelegate = policyResourceDelegate;
        this.resourceSetStoreFactory = resourceSetStoreFactory;
        this.auditLogger = auditLogger;
        this.contextHelper = contextHelper;
        this.policyEvaluatorFactory = policyEvaluatorFactory;
        this.coreServicesWrapper = coreServicesWrapper;
        this.debug = debug;
        this.umaSettingsFactory = umaSettingsFactory;
    }

    private JsonValue resolveUsernameToUID(final ServerContext context, JsonValue policy) {
        final String resourceOwnerName = contextHelper.getUserId(context);
        final String resourceOwnerUserUid = contextHelper.getUserUid(context);

        for (JsonValue permission : policy.get("permissions")) {
            final String userName = permission.get("subject").asString();
            String userUid = contextHelper.getUserUid(context, userName);

            if (userUid != null) {
                permission.put("subject", userUid);
            } else if (resourceOwnerUserUid.contains(resourceOwnerName)) {
                final String derivedUserUid = resourceOwnerUserUid.replace(resourceOwnerName, userName);
                permission.put("subject", derivedUserUid);
            }
        }
        return policy;
    }

    private JsonValue resolveUIDToUsername(JsonValue policy) {
        for (JsonValue permission : policy.get("permissions")) {
            try {
                String username = new AMIdentity(null, permission.get("subject").asString()).getName();
                permission.put("subject", username);
            } catch (IdRepoException e) {
                //Cannot happen in this use case
            }
        }
        return policy;
    }

    private ResourceSetDescription getResourceSet(String realm, String policyId) throws ResourceException {
        try {
            Set<ResourceSetDescription> results = resourceSetStoreFactory.create(realm).query(
                            QueryFilter.equalTo(ResourceSetTokenField.RESOURCE_SET_ID, policyId));
            if (results.isEmpty()) {
                throw new BadRequestException("Invalid ResourceSet UID" + policyId);
            } else if (results.size() > 1) {
                throw new InternalServerErrorException("Multiple Resource Sets found with id: " + policyId);
            } else {
                return results.iterator().next();
            }
        } catch (ServerException e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }

    private boolean isDelegationOn(String realm) {
        try {
            return ResharingMode.IMPLICIT.equals(umaSettingsFactory.create(realm).getResharingMode());
        } catch (ServerException e) {
            debug.error("Could not read UMA Delegation mode for realm: " + realm, e);
            return false;
        }
    }

    private boolean canUserShareResourceSet(String resourceOwnerId, String username, String clientId, String realm,
            String resourceSetId, Set<String> requestedScopes) {
        Subject resourceOwner = UmaUtils.createSubject(coreServicesWrapper.getIdentity(resourceOwnerId, realm));
        Subject user = UmaUtils.createSubject(coreServicesWrapper.getIdentity(username, realm));
        if (resourceOwner.equals(user)) {
            return true;
        }
        if (!isDelegationOn(realm)) {
            return false;
        }

        try {
            Evaluator evaluator = policyEvaluatorFactory.getEvaluator(user, clientId.toLowerCase());

            List<Entitlement> entitlements = evaluator.evaluate(realm, user,
                    UmaConstants.UMA_POLICY_SCHEME + resourceSetId, null, false);

            Set<String> requiredScopes = new HashSet<>(requestedScopes);
            for (Entitlement entitlement : entitlements) {
                for (String requestedScope : requestedScopes) {
                    final Boolean actionValue = entitlement.getActionValue(requestedScope);
                    if (actionValue != null && actionValue) {
                        requiredScopes.remove(requestedScope);
                    }
                }
            }
            return requiredScopes.isEmpty();
        } catch (EntitlementException e) {
            debug.error("Failed to evaluate UAM policies", e);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<UmaPolicy, ResourceException> createPolicy(final ServerContext context, JsonValue policy) {
        UmaPolicy umaPolicy;
        final ResourceSetDescription resourceSet;
        String userId = contextHelper.getUserId(context);
        String realm = getRealm(context);
        try {
            String policyId = UmaPolicy.idOf(policy);
            resourceSet = getResourceSet(realm, policyId);
            umaPolicy = UmaPolicy.valueOf(resourceSet, resolveUsernameToUID(context, policy));
            boolean canShare = canUserShareResourceSet(resourceSet.getResourceOwnerId(),
                    userId, resourceSet.getClientId(), realm, resourceSet.getId(),
                    umaPolicy.getScopes());
            if (!canShare) {
                return newExceptionPromise(ResourceException.getException(403));
            }
            validateScopes(resourceSet, umaPolicy.getScopes());
            verifyPolicyDoesNotAlreadyExist(context, resourceSet);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
        return policyResourceDelegate.createPolicies(context, umaPolicy.asUnderlyingPolicies(userId))
                .thenAsync(new UpdatePolicyGraphStatesFunction<List<Resource>>(resourceSet, context))
                .thenAsync(new AuditAndProduceUmaPolicyFunction(resourceSet, context));
    }

    private class UpdatePolicyGraphStatesFunction<T> implements AsyncFunction<T, T, ResourceException> {
        private final ResourceSetDescription resourceSet;
        private final ServerContext context;

        public UpdatePolicyGraphStatesFunction(ResourceSetDescription resourceSet, ServerContext context) {
            this.resourceSet = resourceSet;
            this.context = context;
        }

        @Override
        public Promise<T, ResourceException> apply(final T result) {
            final QueryRequest queryRequest = Requests.newQueryRequest("")
                    .setQueryFilter(QueryFilter.equalTo(new JsonPointer("resourceTypeUuid"), resourceSet.getId()));
            final PolicyGraph policyGraph = new PolicyGraph(resourceSet);
            return policyResourceDelegate.queryPolicies(context, queryRequest, policyGraph)
                    .thenAsync(new AsyncFunction<QueryResult, T, ResourceException>() {
                        @Override
                        public Promise<T, ResourceException> apply(QueryResult queryResult) {
                            // All policies are now loaded, so compute the graph and update if necessary
                            policyGraph.computeGraph();
                            if (!policyGraph.isValid()) {
                                // Graph needs updating, so return a promise that completes once the update is done.
                                return policyGraph.update(context, policyResourceDelegate).then(
                                        new Function<List<List<Resource>>, T, ResourceException>() {
                                            @Override
                                            public T apply(List<List<Resource>> lists) throws ResourceException {
                                                return result;
                                            }
                                        });
                            }
                            // No update required, complete straight away.
                            return Promises.newResultPromise(result);
                        }
                    });
        }
    }

    private class AuditAndProduceUmaPolicyFunction implements AsyncFunction<List<Resource>, UmaPolicy, ResourceException> {
        private final ResourceSetDescription resourceSet;
        private final ServerContext context;

        public AuditAndProduceUmaPolicyFunction(ResourceSetDescription resourceSet, ServerContext context) {
            this.resourceSet = resourceSet;
            this.context = context;
        }

        @Override
        public Promise<UmaPolicy, ResourceException> apply(List<Resource> value) {
            try {
                UmaPolicy umaPolicy = UmaPolicy.fromUnderlyingPolicies(resourceSet, value);
                String userId = getLoggedInUserId(context);
                auditLogger.get().log(resourceSet.getId(), resourceSet.getName(), userId,
                        UmaAuditType.POLICY_CREATED, userId);
                return Promises.newResultPromise(umaPolicy);
            } catch (ResourceException e) {
                return newExceptionPromise(e);
            }
        }
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
        return internalReadPolicy(context, resourceSetId)
                .then(new Function<UmaPolicy, UmaPolicy, ResourceException>() {
                    @Override
                    public UmaPolicy apply(UmaPolicy umaPolicy) throws ResourceException {
                        resolveUIDToUsername(umaPolicy.asJson());
                        return umaPolicy;
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    private Promise<UmaPolicy, ResourceException> internalReadPolicy(final ServerContext context, final String resourceSetId) {
        String resourceOwnerUid = getResourceOwnerUid(context);
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.and(
                        QueryFilter.equalTo(new JsonPointer("resourceTypeUuid"), resourceSetId),
                        QueryFilter.equalTo(new JsonPointer("createdBy"), resourceOwnerUid)));
        return policyResourceDelegate.queryPolicies(context, request)
                .thenAsync(new AsyncFunction<Pair<QueryResult, List<Resource>>, UmaPolicy, ResourceException>() {
                    @Override
                    public Promise<UmaPolicy, ResourceException> apply(Pair<QueryResult, List<Resource>> value) {
                        try {
                            if (value.getSecond().isEmpty()) {
                                return newExceptionPromise(
                                        (ResourceException) new NotFoundException("UMA Policy not found, "
                                                + resourceSetId));
                            } else {
                                ResourceSetDescription resourceSet = getResourceSet(getRealm(context), resourceSetId);
                                UmaPolicy umaPolicy = UmaPolicy.fromUnderlyingPolicies(resourceSet, value.getSecond());
                                return Promises.newResultPromise(umaPolicy);
                            }
                        } catch (ResourceException e) {
                            return newExceptionPromise(e);
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
        try {
            resourceSet = getResourceSet(getRealm(context), resourceSetId);
            updatedUmaPolicy = UmaPolicy.valueOf(resourceSet, resolveUsernameToUID(context, policy));
            boolean canShare = canUserShareResourceSet(resourceSet.getResourceOwnerId(),
                    contextHelper.getUserId(context), resourceSet.getClientId(), getRealm(context), resourceSet.getId(),
                    updatedUmaPolicy.getScopes());
            if (!canShare) {
                return newExceptionPromise(ResourceException.getException(403));
            }
            validateScopes(resourceSet, updatedUmaPolicy.getScopes());
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
        return internalReadPolicy(context, resourceSetId)
                .thenOnResult(new ResultHandler<UmaPolicy>() {
                    @Override
                    public void handleResult(UmaPolicy currentUmaPolicy) {
                        Set<String> modifiedScopes = new HashSet<>(updatedUmaPolicy.getScopes());
                        modifiedScopes.retainAll(currentUmaPolicy.getScopes());
                        Set<String> removedScopes = new HashSet<>(currentUmaPolicy.getScopes());
                        removedScopes.removeAll(modifiedScopes);
                        for (JsonValue policy : currentUmaPolicy.asUnderlyingPolicies(contextHelper.getUserId(context))) {
                            for (String scope : removedScopes) {
                                if (policy.get("actionValues").isDefined(scope)) {
                                    policyResourceDelegate.queryPolicies(context, Requests.newQueryRequest("")
                                            .setQueryFilter(QueryFilter.and(
                                                    QueryFilter.equalTo(new JsonPointer("createdBy"), contextHelper.getUserUid(context)),
                                                    QueryFilter.equalTo(new JsonPointer("name"), policy.get("name").asString()))))
                                            .thenAsync(new DeleteOldPolicyFunction(context));
                                }
                            }
                        }
                    }
                }).thenOnResult(new ResultHandler<UmaPolicy>() {
                    @Override
                    public void handleResult(UmaPolicy currentUmaPolicy) {
                        Set<String> modifiedScopes = new HashSet<>(currentUmaPolicy.getScopes());
                        modifiedScopes.retainAll(updatedUmaPolicy.getScopes());
                        Set<String> deletedScopes = new HashSet<>(updatedUmaPolicy.getScopes());
                        deletedScopes.removeAll(modifiedScopes);
                        for (JsonValue policy : updatedUmaPolicy.asUnderlyingPolicies(contextHelper.getUserId(context))) {
                            for (String scope : deletedScopes) {
                                if (policy.get("actionValues").isDefined(scope)) {
                                    policyResourceDelegate.createPolicies(context, singleton(policy));
                                }
                            }
                        }
                    }
                })
                .thenAsync(new UpdatePolicyGraphStatesFunction<UmaPolicy>(resourceSet, context))
                .thenAsync(new UpdateUmaPolicyFunction(context, updatedUmaPolicy, resourceSetId, resourceSet));
    }

    private class UpdateUmaPolicyFunction implements AsyncFunction<UmaPolicy, UmaPolicy, ResourceException> {
        private final ServerContext context;
        private final UmaPolicy updatedUmaPolicy;
        private final String resourceSetId;
        private final ResourceSetDescription resourceSet;

        public UpdateUmaPolicyFunction(ServerContext context, UmaPolicy updatedUmaPolicy, String resourceSetId, ResourceSetDescription resourceSet) {
            this.context = context;
            this.updatedUmaPolicy = updatedUmaPolicy;
            this.resourceSetId = resourceSetId;
            this.resourceSet = resourceSet;
        }

        @Override
        public Promise<UmaPolicy, ResourceException> apply(UmaPolicy umaPolicy) throws ResourceException {
            List<Promise<Resource, ResourceException>> promises = new ArrayList<>();
            final String userId = contextHelper.getUserId(context);
            final Set<JsonValue> policies = updatedUmaPolicy.asUnderlyingPolicies(userId);
            for (final JsonValue policy : policies) {
                promises.add(policyResourceDelegate.updatePolicies(context, singleton(policy))
                        .thenAsync(SINGLETON_RESOURCE_FUNCTION,
                                new AsyncFunction<ResourceException, Resource, ResourceException>() {
                            @Override
                            public Promise<Resource, ResourceException> apply(ResourceException e)
                                    throws ResourceException {
                                if (e instanceof NotFoundException) {
                                    return policyResourceDelegate.createPolicies(context, singleton(policy))
                                            .thenAsync(SINGLETON_RESOURCE_FUNCTION);
                                }
                                return newExceptionPromise(e);
                            }
                        })
                );
            }
            return when(promises).thenAsync(new AsyncFunction<List<Resource>, UmaPolicy, ResourceException>() {
                @Override
                public Promise<UmaPolicy, ResourceException> apply(List<Resource> value) throws ResourceException {
                    String userId = getLoggedInUserId(context);
                    auditLogger.get().log(resourceSetId, resourceSet.getName(), userId, UmaAuditType.POLICY_UPDATED, userId);
                    resolveUIDToUsername(updatedUmaPolicy.asJson());
                    return Promises.newResultPromise(updatedUmaPolicy);
                }
            }, new AsyncFunction<ResourceException, UmaPolicy, ResourceException>() {
                @Override
                public Promise<UmaPolicy, ResourceException> apply(ResourceException error) {
                    return newExceptionPromise(error);
                }
            });
        }

    }

    private static AsyncFunction<List<Resource>, Resource, ResourceException> SINGLETON_RESOURCE_FUNCTION =
            new AsyncFunction<List<Resource>, Resource, ResourceException>() {
                @Override
                public Promise<Resource, ResourceException> apply(List<Resource> resources)
                        throws ResourceException {
                    return newResultPromise(resources.get(0));
                }
            };

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<Void, ResourceException> deletePolicy(final ServerContext context, final String resourceSetId) {
        ResourceSetDescription resourceSet;
        try {
            resourceSet = getResourceSet(getRealm(context), resourceSetId);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
        return internalReadPolicy(context, resourceSetId)
                .thenAsync(new AsyncFunction<UmaPolicy, List<Resource>, ResourceException>() {
                    @Override
                    public Promise<List<Resource>, ResourceException> apply(UmaPolicy value) {
                        return policyResourceDelegate.deletePolicies(context, value.getUnderlyingPolicyIds());
                    }
                })
                .thenAsync(new UpdatePolicyGraphStatesFunction<List<Resource>>(resourceSet, context))
                .thenAsync(new AsyncFunction<List<Resource>, Void, ResourceException>() {
                    @Override
                    public Promise<Void, ResourceException> apply(List<Resource> value) {
                        return Promises.newResultPromise(null);
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
            return newExceptionPromise((ResourceException) new BadRequestException("Query expressions not supported"));
        }

        QueryRequest request = Requests.newQueryRequest("");
        final AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> filter = umaQueryRequest.getQueryFilter()
                .accept(new AggregateUmaPolicyQueryFilter(), new AggregateQuery<QueryFilter, QueryFilter>());

        String queryId = umaQueryRequest.getQueryId();
        if (queryId != null && queryId.equals("searchAll")) {
            request.setQueryFilter(QueryFilter.<JsonPointer>alwaysTrue());
        } else {
            String resourceOwnerUid = getResourceOwnerUid(context);
            if (filter.getFirstQuery() == null) {
                request.setQueryFilter(QueryFilter.equalTo(new JsonPointer("createdBy"), resourceOwnerUid));
            } else {
                request.setQueryFilter(QueryFilter.and(QueryFilter.equalTo(new JsonPointer("createdBy"), resourceOwnerUid), filter.getFirstQuery()));
            }
        }
        return policyResourceDelegate.queryPolicies(context, request)
                .thenAsync(new AsyncFunction<Pair<QueryResult, List<Resource>>, Collection<UmaPolicy>, ResourceException>() {
                    @Override
                    public Promise<Collection<UmaPolicy>, ResourceException> apply(Pair<QueryResult, List<Resource>> value) {
                        Map<String, Set<Resource>> policyMapping = new HashMap<>();
                        for (Resource policy : value.getSecond()) {

                            String resource = policy.getContent().get("resources").asList(String.class).get(0);

                            if (!resource.startsWith(UMA_POLICY_SCHEME)) {
                                continue;
                            }
                            resource = resource.replaceFirst(UMA_POLICY_SCHEME, "");
                            if (resource.indexOf(":") > 0) {
                                resource = resource.substring(0, resource.indexOf(":"));
                            }

                            Set<Resource> mapping = policyMapping.get(resource);
                            if (mapping == null) {
                                mapping = new HashSet<>();
                                policyMapping.put(resource, mapping);
                            }

                            mapping.add(policy);
                        }

                        try {
                            Collection<UmaPolicy> umaPolicies = new HashSet<>();
                            String resourceOwnerId = getResourceOwnerId(context);
                            for (Map.Entry<String, Set<Resource>> entry : policyMapping.entrySet()) {
                                ResourceSetDescription resourceSet = getResourceSetDescription(entry.getKey(), resourceOwnerId, context);
                                UmaPolicy umaPolicy = UmaPolicy.fromUnderlyingPolicies(resourceSet, entry.getValue());
                                resolveUIDToUsername(umaPolicy.asJson());
                                umaPolicies.add(umaPolicy);
                            }
                            return Promises.newResultPromise(umaPolicies);
                        } catch (ResourceException e) {
                            return newExceptionPromise(e);
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

                        return Promises.newResultPromise(Pair.of(
                                new QueryResult(pagedResultsCookie, remainingPagedResults), pagedPolicies));
                    }
                });
    }

    private static final class AggregateUmaPolicyQueryFilter
            implements QueryFilterVisitor<AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>>,
            AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>>,
            JsonPointer> {

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
        public AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> visitAndFilter(
                AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> query,
                List<QueryFilter<JsonPointer>> subFilters) {
            increaseQueryDepth();
            List<QueryFilter<JsonPointer>> childFilters = new ArrayList<>();
            for (QueryFilter<JsonPointer> filter : subFilters) {
                AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> subAggregateQuery =
                        filter.accept(this, query);
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
        public AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> visitEqualsFilter(
                AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> query, JsonPointer field,
                Object valueAssertion) {
            if (new JsonPointer("/permissions/subject").equals(field)) {
                if (queryDepth > 1) {
                    throw new UnsupportedOperationException("Cannot nest queries on /permissions/subject field");
                }
                query.setSecondQuery(QueryFilter.equalTo(new JsonPointer("permissions/subject"), valueAssertion));
            } else {
                query.setFirstQuery(QueryFilter.equalTo(verifyFieldIsQueryable(field), valueAssertion));
            }
            return query;
        }

        @Override
        public AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> visitStartsWithFilter(
                AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> query, JsonPointer field,
                Object valueAssertion) {
            if (new JsonPointer("/permissions/subject").equals(field)) {
                if (queryDepth > 1) {
                    throw new UnsupportedOperationException("Cannot nest queries on /permissions/subject field");
                }
                query.setSecondQuery(QueryFilter.startsWith(new JsonPointer("permissions/subject"), valueAssertion));
            } else {
                query.setFirstQuery(QueryFilter.startsWith(verifyFieldIsQueryable(field), valueAssertion));
            }
            return query;
        }

        @Override
        public AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> visitContainsFilter(
                AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> query, JsonPointer field,
                Object valueAssertion) {
            if (new JsonPointer("/permissions/subject").equals(field)) {
                if (queryDepth > 1) {
                    throw new UnsupportedOperationException("Cannot nest queries on /permissions/subject field");
                }
                query.setSecondQuery(QueryFilter.contains(new JsonPointer("permissions/subject"), valueAssertion));
            } else {
                query.setFirstQuery(QueryFilter.contains(verifyFieldIsQueryable(field), valueAssertion));
            }
            return query;
        }

        @Override
        public AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> visitBooleanLiteralFilter(
                AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> query, boolean value) {
            if (value) {
                return query;
            }
            throw unsupportedFilterOperation("Boolean Literal, false,");
        }

        @Override
        public AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> visitExtendedMatchFilter(
                AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> query, JsonPointer field,
                String operator, Object valueAssertion) {
            throw unsupportedFilterOperation("Extended match");
        }

        @Override
        public AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> visitGreaterThanFilter(
                AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> query, JsonPointer field,
                Object valueAssertion) {
            throw unsupportedFilterOperation("Greater than");
        }

        @Override
        public AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> visitGreaterThanOrEqualToFilter(
                AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> query, JsonPointer field,
                Object valueAssertion) {
            throw unsupportedFilterOperation("Greater than or equal");
        }

        @Override
        public AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> visitLessThanFilter(
                AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> query, JsonPointer field,
                Object valueAssertion) {
            throw unsupportedFilterOperation("Less than");
        }

        @Override
        public AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> visitLessThanOrEqualToFilter(
                AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> query, JsonPointer field, Object valueAssertion) {
            throw unsupportedFilterOperation("Less than or equal");
        }

        @Override
        public AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> visitNotFilter(
                AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> query,
                QueryFilter<JsonPointer> subFilter) {
            throw unsupportedFilterOperation("Not");
        }

        @Override
        public AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> visitOrFilter(
                AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> query,
                List<QueryFilter<JsonPointer>> subFilters) {
            increaseQueryDepth();
            List<QueryFilter<JsonPointer>> childFilters = new ArrayList<>();
            for (QueryFilter<JsonPointer> filter : subFilters) {
                AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> subAggregateQuery = filter.accept(this, query);
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
        public AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> visitPresentFilter(
                AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> query, JsonPointer field) {
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
            return resourceSetStoreFactory.create(realm).read(resourceSetId);
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

    private class DeleteOldPolicyFunction implements AsyncFunction<Pair<QueryResult, List<Resource>>, Void, ResourceException> {
        private final ServerContext context;

        public DeleteOldPolicyFunction(ServerContext context) {
            this.context = context;
        }

        @Override
        public Promise<Void, ResourceException> apply(Pair<QueryResult, List<Resource>> result) {
            List<Promise<List<Resource>, ResourceException>> results = new ArrayList<>();
            for (Resource resource : result.getSecond()) {
                results.add(policyResourceDelegate.deletePolicies(context, singleton(resource.getId())));
            }
            return Promises.when(results).then(new Function<List<List<Resource>>, Void, ResourceException>() {
                @Override
                public Void apply(List<List<Resource>> lists) throws ResourceException {
                    return null;
                }
            });
        }
    }

}
