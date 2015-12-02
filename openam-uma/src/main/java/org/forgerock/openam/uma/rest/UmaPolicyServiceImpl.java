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

package org.forgerock.openam.uma.rest;

import static java.util.Collections.singleton;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.openam.uma.UmaConstants.UMA_POLICY_SCHEME;
import static org.forgerock.util.promise.Promises.*;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.security.auth.Subject;
import java.util.ArrayList;
import java.util.Collection;
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
import org.forgerock.json.resource.CountPolicy;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.openam.core.CoreServicesWrapper;
import org.forgerock.openam.cts.api.fields.ResourceSetTokenField;
import org.forgerock.openam.oauth2.extensions.ExtensionFilterManager;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.openam.oauth2.rest.AggregateQuery;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.forgerock.openam.uma.PolicySearch;
import org.forgerock.openam.uma.ResharingMode;
import org.forgerock.openam.uma.ResourceSetAcceptAllFilter;
import org.forgerock.openam.uma.ResourceSetSharedFilter;
import org.forgerock.openam.uma.UmaConstants;
import org.forgerock.openam.uma.UmaPolicy;
import org.forgerock.openam.uma.UmaPolicyQueryFilterVisitor;
import org.forgerock.openam.uma.UmaPolicyService;
import org.forgerock.openam.uma.UmaSettingsFactory;
import org.forgerock.openam.uma.UmaUtils;
import org.forgerock.openam.uma.audit.UmaAuditLogger;
import org.forgerock.openam.uma.audit.UmaAuditType;
import org.forgerock.openam.uma.extensions.ResourceDelegationFilter;
import org.forgerock.openam.utils.Config;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.services.context.Context;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.promise.ResultHandler;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;

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
    private final ExtensionFilterManager extensionFilterManager;

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
     * @param extensionFilterManager An instance of the {@code ExtensionFilterManager}.
     */
    @Inject
    public UmaPolicyServiceImpl(PolicyResourceDelegate policyResourceDelegate,
            ResourceSetStoreFactory resourceSetStoreFactory, Config<UmaAuditLogger> auditLogger,
            ContextHelper contextHelper, UmaPolicyEvaluatorFactory policyEvaluatorFactory,
            CoreServicesWrapper coreServicesWrapper, @Named("frRest") Debug debug,
            UmaSettingsFactory umaSettingsFactory, ExtensionFilterManager extensionFilterManager) {
        this.policyResourceDelegate = policyResourceDelegate;
        this.resourceSetStoreFactory = resourceSetStoreFactory;
        this.auditLogger = auditLogger;
        this.contextHelper = contextHelper;
        this.policyEvaluatorFactory = policyEvaluatorFactory;
        this.coreServicesWrapper = coreServicesWrapper;
        this.debug = debug;
        this.umaSettingsFactory = umaSettingsFactory;
        this.extensionFilterManager = extensionFilterManager;
    }

    private JsonValue resolveUsernameToUID(final Context context, JsonValue policy) throws BadRequestException {
        final String resourceOwnerName = contextHelper.getUserId(context);
        final String resourceOwnerUserUid = contextHelper.getUserUid(context);

        for (JsonValue permission : policy.get("permissions")) {
            final String userName = permission.get("subject").asString();
            if (StringUtils.isBlank(userName)) {
                throw new BadRequestException("Subject cannot be a blank string");
            }
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
    public Promise<UmaPolicy, ResourceException> createPolicy(final Context context, JsonValue policy) {
        final UmaPolicy umaPolicy;
        final ResourceSetDescription resourceSet;
        final String userId = contextHelper.getUserId(context);
        String realm = getRealm(context);
        try {
            String policyId = UmaPolicy.idOf(policy);
            resourceSet = getResourceSet(realm, policyId);
            umaPolicy = UmaPolicy.valueOf(resourceSet, resolveUsernameToUID(context, policy));
            boolean canShare = canUserShareResourceSet(resourceSet.getResourceOwnerId(),
                    userId, resourceSet.getClientId(), realm, resourceSet.getId(),
                    umaPolicy.getScopes());
            if (!canShare) {
                return new ForbiddenException().asPromise();
            }
            validateScopes(resourceSet, umaPolicy.getScopes());
            verifyPolicyDoesNotAlreadyExist(context, resourceSet);
        } catch (ResourceException e) {
            return e.asPromise();
        }
        return beforeResourceShared(umaPolicy)
                .thenAsync(new AsyncFunction<UmaPolicy, List<ResourceResponse>, ResourceException>() {
                    @Override
                    public Promise<List<ResourceResponse>, ResourceException> apply(UmaPolicy umaPolicy) {
                        return policyResourceDelegate.createPolicies(context, umaPolicy.asUnderlyingPolicies(userId));
                    }
                })
                .thenAlways(afterResourceShared(umaPolicy))
                .thenAsync(new UpdatePolicyGraphStatesFunction<List<ResourceResponse>>(resourceSet, context))
                .thenAsync(new AuditAndProduceUmaPolicyFunction(resourceSet, context));
    }

    private Promise<UmaPolicy, ResourceException> beforeResourceShared(UmaPolicy umaPolicy) {
        try {
            for (ResourceDelegationFilter filter : extensionFilterManager.getFilters(ResourceDelegationFilter.class)) {
                filter.beforeResourceShared(umaPolicy);
            }
            return newResultPromise(umaPolicy);
        } catch (ResourceException e) {
            return newExceptionPromise(e);
        }
    }

    private AsyncFunction<UmaPolicy, UmaPolicy, ResourceException> beforeResourceSharedModified(
            final UmaPolicy updatedUmaPolicy) {
        return new AsyncFunction<UmaPolicy, UmaPolicy, ResourceException>() {
            @Override
            public Promise<UmaPolicy, ResourceException> apply(UmaPolicy currentUmaPolicy) throws ResourceException {
                for (ResourceDelegationFilter filter :
                        extensionFilterManager.getFilters(ResourceDelegationFilter.class)) {
                    filter.beforeResourceSharedModification(currentUmaPolicy, updatedUmaPolicy);
                }
                return newResultPromise(currentUmaPolicy);
            }
        };
    }

    private AsyncFunction<UmaPolicy, UmaPolicy, ResourceException> onResourceSharedDeletion() {
        return new AsyncFunction<UmaPolicy, UmaPolicy, ResourceException>() {
            @Override
            public Promise<UmaPolicy, ResourceException> apply(UmaPolicy umaPolicy) throws ResourceException {
                for (ResourceDelegationFilter filter
                        :extensionFilterManager.getFilters(ResourceDelegationFilter.class)) {
                    filter.onResourceSharedDeletion(umaPolicy);
                }
                return newResultPromise(umaPolicy);
            }
        };
    }

    private Runnable afterResourceShared(final UmaPolicy umaPolicy) {
        return new Runnable() {
            @Override
            public void run() {
                for (ResourceDelegationFilter filter
                        : extensionFilterManager.getFilters(ResourceDelegationFilter.class)) {
                    filter.afterResourceShared(umaPolicy);
                }
            }
        };
    }

    private class UpdatePolicyGraphStatesFunction<T> implements AsyncFunction<T, T, ResourceException> {
        private final ResourceSetDescription resourceSet;
        private final Context context;

        public UpdatePolicyGraphStatesFunction(ResourceSetDescription resourceSet, Context context) {
            this.resourceSet = resourceSet;
            this.context = context;
        }

        @Override
        public Promise<T, ResourceException> apply(final T result) {
            final QueryRequest queryRequest = Requests.newQueryRequest("")
                    .setQueryFilter(QueryFilter.equalTo(new JsonPointer("resourceTypeUuid"), resourceSet.getId()));
            final PolicyGraph policyGraph = new PolicyGraph(resourceSet);
            return policyResourceDelegate.queryPolicies(context, queryRequest, policyGraph)
                    .thenOnException(policyGraph).thenOnResult(policyGraph)
                    .thenAsync(new AsyncFunction<QueryResponse, T, ResourceException>() {
                        @Override
                        public Promise<T, ResourceException> apply(QueryResponse queryResult) {
                            // All policies are now loaded, so compute the graph and update if necessary
                            policyGraph.computeGraph();
                            if (!policyGraph.isValid()) {
                                // Graph needs updating, so return a promise that completes once the update is done.
                                return policyGraph.update(context, policyResourceDelegate).then(
                                        new Function<List<List<ResourceResponse>>, T, ResourceException>() {
                                            @Override
                                            public T apply(List<List<ResourceResponse>> lists) {
                                                return result;
                                            }
                                        });
                            }
                            // No update required, complete straight away.
                            return newResultPromise(result);
                        }
                    });
        }
    }

    private class AuditAndProduceUmaPolicyFunction implements AsyncFunction<List<ResourceResponse>, UmaPolicy, ResourceException> {
        private final ResourceSetDescription resourceSet;
        private final Context context;

        public AuditAndProduceUmaPolicyFunction(ResourceSetDescription resourceSet, Context context) {
            this.resourceSet = resourceSet;
            this.context = context;
        }

        @Override
        public Promise<UmaPolicy, ResourceException> apply(List<ResourceResponse> value) {
            try {
                UmaPolicy umaPolicy = UmaPolicy.fromUnderlyingPolicies(resourceSet, value);
                AMIdentity resourceOwner = getLoggedInUser(context);
                auditLogger.get().log(resourceSet.getId(), resourceSet.getName(), resourceOwner,
                        UmaAuditType.POLICY_CREATED, resourceOwner.getUniversalId());
                return newResultPromise(umaPolicy);
            } catch (ResourceException e) {
                return e.asPromise();
            }
        }
    }

    private void verifyPolicyDoesNotAlreadyExist(Context context, ResourceSetDescription resourceSet)
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
    public Promise<UmaPolicy, ResourceException> readPolicy(final Context context, final String resourceSetId) {
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
    private Promise<UmaPolicy, ResourceException> internalReadPolicy(final Context context, final String resourceSetId) {
        String resourceOwnerUid = getResourceOwnerUid(context);
        QueryRequest request = Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.and(
                        QueryFilter.equalTo(new JsonPointer("resourceTypeUuid"), resourceSetId),
                        QueryFilter.equalTo(new JsonPointer("createdBy"), resourceOwnerUid)));
        return policyResourceDelegate.queryPolicies(context, request)
                .thenAsync(new AsyncFunction<Pair<QueryResponse, List<ResourceResponse>>, UmaPolicy, ResourceException>() {
                    @Override
                    public Promise<UmaPolicy, ResourceException> apply(Pair<QueryResponse, List<ResourceResponse>> value) {
                        try {
                            if (value.getSecond().isEmpty()) {
                                return new NotFoundException("UMA Policy not found, " + resourceSetId).asPromise();
                            } else {
                                ResourceSetDescription resourceSet = getResourceSet(getRealm(context), resourceSetId);
                                UmaPolicy umaPolicy = UmaPolicy.fromUnderlyingPolicies(resourceSet, value.getSecond());
                                return newResultPromise(umaPolicy);
                            }
                        } catch (ResourceException e) {
                            return e.asPromise();
                        }
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<UmaPolicy, ResourceException> updatePolicy(final Context context, final String resourceSetId, //TODO need to check if need to delete backend policies
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
                return new ForbiddenException().asPromise();
            }
            validateScopes(resourceSet, updatedUmaPolicy.getScopes());
        } catch (ResourceException e) {
            return e.asPromise();
        }
        return internalReadPolicy(context, resourceSetId)
                .thenAsync(beforeResourceSharedModified(updatedUmaPolicy))
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
                }).thenOnResult(new ResultHandler<UmaPolicy>() {
                    @Override
                    public void handleResult(UmaPolicy currentUmaPolicy) {
                        String uid = contextHelper.getUserId(context);
                        Set<String> underlyingPolicyIds = new HashSet<>(currentUmaPolicy.getUnderlyingPolicyIds());
                        Set<JsonValue> newUnderlyingPolicies = updatedUmaPolicy.asUnderlyingPolicies(uid);
                        for (JsonValue value : newUnderlyingPolicies) {
                            underlyingPolicyIds.remove(value.get("name").asString());
                        }
                        policyResourceDelegate.deletePolicies(context, underlyingPolicyIds);
                    }
                })
                .thenAsync(new UpdatePolicyGraphStatesFunction<UmaPolicy>(resourceSet, context))
                .thenAsync(new UpdateUmaPolicyFunction(context, updatedUmaPolicy, resourceSetId, resourceSet));
    }

    private class UpdateUmaPolicyFunction implements AsyncFunction<UmaPolicy, UmaPolicy, ResourceException> {
        private final Context context;
        private final UmaPolicy updatedUmaPolicy;
        private final String resourceSetId;
        private final ResourceSetDescription resourceSet;

        public UpdateUmaPolicyFunction(Context context, UmaPolicy updatedUmaPolicy, String resourceSetId, ResourceSetDescription resourceSet) {
            this.context = context;
            this.updatedUmaPolicy = updatedUmaPolicy;
            this.resourceSetId = resourceSetId;
            this.resourceSet = resourceSet;
        }

        @Override
        public Promise<UmaPolicy, ResourceException> apply(UmaPolicy umaPolicy) throws ResourceException {
            List<Promise<ResourceResponse, ResourceException>> promises = new ArrayList<>();
            final String userId = contextHelper.getUserId(context);
            final Set<JsonValue> policies = updatedUmaPolicy.asUnderlyingPolicies(userId);
            for (final JsonValue policy : policies) {
                promises.add(policyResourceDelegate.updatePolicies(context, singleton(policy))
                        .thenAsync(SINGLETON_RESOURCE_FUNCTION,
                                new AsyncFunction<ResourceException, ResourceResponse, ResourceException>() {
                            @Override
                            public Promise<ResourceResponse, ResourceException> apply(ResourceException e)
                                    throws ResourceException {
                                if (e instanceof NotFoundException) {
                                    return policyResourceDelegate.createPolicies(context, singleton(policy))
                                            .thenAsync(SINGLETON_RESOURCE_FUNCTION);
                                }
                                return e.asPromise();
                            }
                        })
                );
            }
            return when(promises).thenAsync(new AsyncFunction<List<ResourceResponse>, UmaPolicy, ResourceException>() {
                @Override
                public Promise<UmaPolicy, ResourceException> apply(List<ResourceResponse> value) throws ResourceException {
                    AMIdentity resourceOwner = getLoggedInUser(context);
                    auditLogger.get().log(resourceSetId, resourceSet.getName(), resourceOwner, UmaAuditType.POLICY_UPDATED, userId);
                    resolveUIDToUsername(updatedUmaPolicy.asJson());
                    return newResultPromise(updatedUmaPolicy);
                }
            }, new AsyncFunction<ResourceException, UmaPolicy, ResourceException>() {
                @Override
                public Promise<UmaPolicy, ResourceException> apply(ResourceException error) {
                    return error.asPromise();
                }
            });
        }

    }

    private static AsyncFunction<List<ResourceResponse>, ResourceResponse, ResourceException> SINGLETON_RESOURCE_FUNCTION =
            new AsyncFunction<List<ResourceResponse>, ResourceResponse, ResourceException>() {
                @Override
                public Promise<ResourceResponse, ResourceException> apply(List<ResourceResponse> resources)
                        throws ResourceException {
                    return newResultPromise(resources.get(0));
                }
            };

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<Void, ResourceException> deletePolicy(final Context context, final String resourceSetId) {
        ResourceSetDescription resourceSet;
        try {
            resourceSet = getResourceSet(getRealm(context), resourceSetId);
        } catch (ResourceException e) {
            return e.asPromise();
        }
        return internalReadPolicy(context, resourceSetId)
                .thenAsync(onResourceSharedDeletion())
                .thenAsync(new AsyncFunction<UmaPolicy, List<ResourceResponse>, ResourceException>() {
                    @Override
                    public Promise<List<ResourceResponse>, ResourceException> apply(UmaPolicy value) {
                        return policyResourceDelegate.deletePolicies(context, value.getUnderlyingPolicyIds());
                    }
                })
                .thenAsync(new UpdatePolicyGraphStatesFunction<List<ResourceResponse>>(resourceSet, context))
                .thenAsync(new AsyncFunction<List<ResourceResponse>, Void, ResourceException>() {
                    @Override
                    public Promise<Void, ResourceException> apply(List<ResourceResponse> value) {
                        return newResultPromise(null);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<Pair<QueryResponse, Collection<UmaPolicy>>, ResourceException> queryPolicies(final Context context,
            final QueryRequest umaQueryRequest) {

        if (umaQueryRequest.getQueryExpression() != null) {
            return new BadRequestException("Query expressions not supported").asPromise();
        }

        QueryRequest request = Requests.newQueryRequest("");
        final AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>> filter = umaQueryRequest.getQueryFilter()
                .accept(new AggregateUmaPolicyQueryFilter(), new AggregateQuery<QueryFilter<JsonPointer>, QueryFilter<JsonPointer>>());

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
                .thenAsync(new AsyncFunction<Pair<QueryResponse, List<ResourceResponse>>, Collection<UmaPolicy>, ResourceException>() {
                    @Override
                    public Promise<Collection<UmaPolicy>, ResourceException> apply(Pair<QueryResponse, List<ResourceResponse>> value) {
                        Map<String, Set<ResourceResponse>> policyMapping = new HashMap<>();
                        for (ResourceResponse policy : value.getSecond()) {

                            String resource = policy.getContent().get("resources").asList(String.class).get(0);

                            if (!resource.startsWith(UMA_POLICY_SCHEME)) {
                                continue;
                            }
                            resource = resource.replaceFirst(UMA_POLICY_SCHEME, "");
                            if (resource.indexOf(":") > 0) {
                                resource = resource.substring(0, resource.indexOf(":"));
                            }

                            Set<ResourceResponse> mapping = policyMapping.get(resource);
                            if (mapping == null) {
                                mapping = new HashSet<>();
                                policyMapping.put(resource, mapping);
                            }

                            mapping.add(policy);
                        }

                        try {
                            Collection<UmaPolicy> umaPolicies = new HashSet<>();
                            for (Map.Entry<String, Set<ResourceResponse>> entry : policyMapping.entrySet()) {
                                ResourceSetDescription resourceSet = getResourceSetDescription(entry.getKey(), context);
                                UmaPolicy umaPolicy = UmaPolicy.fromUnderlyingPolicies(resourceSet, entry.getValue());
                                resolveUIDToUsername(umaPolicy.asJson());
                                umaPolicies.add(umaPolicy);
                            }
                            return newResultPromise(umaPolicies);
                        } catch (ResourceException e) {
                            return e.asPromise();
                        }
                    }
                })
                .thenAsync(new AsyncFunction<Collection<UmaPolicy>, Pair<QueryResponse, Collection<UmaPolicy>>, ResourceException>() {
                    @Override
                    public Promise<Pair<QueryResponse, Collection<UmaPolicy>>, ResourceException> apply(Collection<UmaPolicy> policies) {
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

                        return newResultPromise(Pair.of(
                                newQueryResponse(pagedResultsCookie, CountPolicy.EXACT, remainingPagedResults), pagedPolicies));
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

    private ResourceSetDescription getResourceSetDescription(String resourceSetId, Context context)
            throws ResourceException {
        try {
            String realm = getRealm(context);
            return resourceSetStoreFactory.create(realm).read(resourceSetId, ResourceSetAcceptAllFilter.INSTANCE);
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

    private String getLoggedInUserId(Context context) throws InternalServerErrorException {
        try {
            SubjectContext subjectContext = context.asContext(SubjectContext.class);
            SSOToken token = subjectContext.getCallerSSOToken();
            return token.getPrincipal().getName();
        } catch (SSOException e) {
            throw new InternalServerErrorException(e);
        }
    }

    private AMIdentity getLoggedInUser(Context context) throws InternalServerErrorException {
        try {
            SubjectContext subjectContext = context.asContext(SubjectContext.class);
            return new AMIdentity(subjectContext.getCallerSSOToken());
        } catch (SSOException | IdRepoException e) {
            throw new InternalServerErrorException(e);
        }
    }

    private String getResourceOwnerUid(Context context) {
        return contextHelper.getUserUid(context);
    }

    private String getResourceOwnerId(Context context) {
        return contextHelper.getUserId(context);
    }

    private String getRealm(Context context) {
        return contextHelper.getRealm(context);
    }

    private class DeleteOldPolicyFunction implements AsyncFunction<Pair<QueryResponse, List<ResourceResponse>>, Void, ResourceException> {
        private final Context context;

        public DeleteOldPolicyFunction(Context context) {
            this.context = context;
        }

        @Override
        public Promise<Void, ResourceException> apply(Pair<QueryResponse, List<ResourceResponse>> result) {
            List<Promise<List<ResourceResponse>, ResourceException>> results = new ArrayList<>();
            for (ResourceResponse resource : result.getSecond()) {
                results.add(policyResourceDelegate.deletePolicies(context, singleton(resource.getId())));
            }
            return Promises.when(results).then(new Function<List<List<ResourceResponse>>, Void, ResourceException>() {
                @Override
                public Void apply(List<List<ResourceResponse>> lists) {
                    return null;
                }
            });
        }
    }

}
