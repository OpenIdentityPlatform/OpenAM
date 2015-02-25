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

import static org.forgerock.openam.uma.UmaConstants.UMA_POLICY_APPLICATION_TYPE;
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
import org.forgerock.guava.common.cache.Cache;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SortKey;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.forgerock.openam.uma.PolicySearch;
import org.forgerock.openam.uma.UmaPolicy;
import org.forgerock.openam.uma.UmaPolicyComparator;
import org.forgerock.openam.uma.UmaPolicyQueryFilterVisitor;
import org.forgerock.openam.uma.UmaPolicyService;
import org.forgerock.openam.uma.UmaPolicyStore;
import org.forgerock.openam.uma.audit.UmaAuditLogger;
import org.forgerock.openam.uma.audit.UmaAuditType;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.AsyncFunction;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * Implementation of the {@code UmaPolicyService}.
 *
 * @since 13.0.0
 */
@Singleton
public class UmaPolicyServiceImpl implements UmaPolicyService {

    private final UmaPolicyStore umaPolicyStore;
    private final PolicyResourceDelegate policyResourceDelegate;
    private final ResourceSetStoreFactory resourceSetStoreFactory;
    private final UmaAuditLogger auditLogger;

    /**
     * Creates an instance of the {@code UmaPolicyServiceImpl}.
     *
     * @param umaPolicyStore An instance of the {@code UmaPolicyStore}.
     * @param policyResourceDelegate An instance of the {@code PolicyResourceDelegate}.
     * @param resourceSetStoreFactory An instance of the {@code ResourceSetStoreFactory}.
     */
    @Inject
    public UmaPolicyServiceImpl(UmaPolicyStore umaPolicyStore, PolicyResourceDelegate policyResourceDelegate,
            ResourceSetStoreFactory resourceSetStoreFactory, UmaAuditLogger auditLogger) {
        this.umaPolicyStore = umaPolicyStore;
        this.policyResourceDelegate = policyResourceDelegate;
        this.resourceSetStoreFactory = resourceSetStoreFactory;
        this.auditLogger = auditLogger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<UmaPolicy, ResourceException> createPolicy(final ServerContext context, JsonValue policy) {
        UmaPolicy umaPolicy;
        final String userId;
        final ResourceSetDescription resourceSet;
        try {
            resourceSet = getResourceSetDescription(UmaPolicy.idOf(policy), context);
            userId = getLoggedInUserId(context);
            umaPolicy = UmaPolicy.valueOf(resourceSet, policy);
            validateScopes(resourceSet, umaPolicy.getScopes());
            verifyPolicyDoesNotAlreadyExist(resourceSet, userId, getRealm(context));
        } catch (ResourceException e) {
            return Promises.newFailedPromise(e);
        }
        return policyResourceDelegate.createPolicies(context, umaPolicy.asUnderlyingPolicies())
                .thenAsync(new AsyncFunction<List<Resource>, UmaPolicy, ResourceException>() {
                    @Override
                    public Promise<UmaPolicy, ResourceException> apply(List<Resource> value) {
                        try {
                            UmaPolicy umaPolicy = UmaPolicy.fromUnderlyingPolicies(resourceSet, value);
                            umaPolicyStore.addToUserCache(userId, getRealm(context), resourceSet.getId(), umaPolicy);
                            auditLogger.log(resourceSet.getName(), userId, UmaAuditType.POLICY_CREATED,
                                    userId);
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

    private void verifyPolicyDoesNotAlreadyExist(ResourceSetDescription resourceSet, String userId, String realm)
            throws ConflictException {
        Cache<String, UmaPolicy> userCache = umaPolicyStore.getUserCache(userId, realm);
        if (userCache != null && userCache.getIfPresent(resourceSet.getId()) != null) {
            throw new ConflictException("Policy already exists for Resource Server, "
                            + resourceSet.getClientId() + ", Resource Set, " + resourceSet.getId());
        }
    }

    private String getRealm(ServerContext context) {
        RealmContext realmContext = context.asContext(RealmContext.class);
        return realmContext.getResolvedRealm();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<UmaPolicy, ResourceException> readPolicy(final ServerContext context, final String resourceSetUid) {
        final String userId;
        try {
            userId = getLoggedInUserId(context);
        } catch (ResourceException e) {
            return Promises.newFailedPromise(e);
        }
        UmaPolicy umaPolicy = localRead(resourceSetUid, userId, getRealm(context));
        if (umaPolicy != null) {
            return Promises.newSuccessfulPromise(umaPolicy);
        }
        return loadUserUmaPolicies(context)
                .thenAsync(new AsyncFunction<Void, UmaPolicy, ResourceException>() {
                    @Override
                    public Promise<UmaPolicy, ResourceException> apply(Void value) {
                        UmaPolicy umaPolicy = localRead(resourceSetUid, userId, getRealm(context));
                        if (umaPolicy != null) {
                            return Promises.newSuccessfulPromise(umaPolicy);
                        } else {
                            return Promises.newFailedPromise((ResourceException) new NotFoundException());
                        }
                    }
                });
    }

    private UmaPolicy localRead(String resourceSetUid, String userId, String realm) {
        Cache<String, UmaPolicy> userCache = umaPolicyStore.getUserCache(userId, realm);
        if (userCache != null) {
            return userCache.getIfPresent(resourceSetUid);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<UmaPolicy, ResourceException> updatePolicy(final ServerContext context, final String resourceSetUid,
            JsonValue policy) {
        final UmaPolicy umaPolicy;
        final String userId;
        final ResourceSetDescription resourceSet;
        try {
            userId = getLoggedInUserId(context);
            resourceSet = getResourceSetDescription(resourceSetUid, context);
            umaPolicy = UmaPolicy.valueOf(resourceSet, policy);
            validateScopes(resourceSet, umaPolicy.getScopes());
        } catch (ResourceException e) {
            return Promises.newFailedPromise(e);
        }
        return policyResourceDelegate.updatePolicies(context, umaPolicy.asUnderlyingPolicies())
                .thenAsync(new AsyncFunction<List<Resource>, UmaPolicy, ResourceException>() {
                    @Override
                    public Promise<UmaPolicy, ResourceException> apply(List<Resource> value) throws ResourceException {
                        umaPolicyStore.addToUserCache(userId, getRealm(context), resourceSet.getId(),
                                UmaPolicy.fromUnderlyingPolicies(resourceSet, value));
                        auditLogger.log(resourceSet.getName(), userId, UmaAuditType.POLICY_UPDATED, userId);
                        return Promises.newSuccessfulPromise(umaPolicy);
                    }
                }, new AsyncFunction<ResourceException, UmaPolicy, ResourceException>() {
                    @Override
                    public Promise<UmaPolicy, ResourceException> apply(ResourceException error)  {
                        return Promises.newFailedPromise(error);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<Void, ResourceException> deletePolicy(final ServerContext context, final String resourceSetUid) {
        final String userId;
        try {
            userId = getLoggedInUserId(context);
        } catch (ResourceException e) {
            return Promises.newFailedPromise(e);
        }
        return readPolicy(context, resourceSetUid)
                .thenAsync(new AsyncFunction<UmaPolicy, List<Resource>, ResourceException>() {
                    @Override
                    public Promise<List<Resource>, ResourceException> apply(UmaPolicy value) {
                        return policyResourceDelegate.deletePolicies(context, value.getUnderlyingPolicyIds());
                    }
                })
                .thenAsync(new AsyncFunction<List<Resource>, Void, ResourceException>() {
                    @Override
                    public Promise<Void, ResourceException> apply(List<Resource> value) {
                        Cache<String, UmaPolicy> userCache = umaPolicyStore.getUserCache(userId, getRealm(context));
                        if (userCache != null) {
                            userCache.invalidate(resourceSetUid);
                        }
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

        final String userId;
        try {
            userId = getLoggedInUserId(context);
        } catch (ResourceException e) {
            return Promises.newFailedPromise(e);
        }
        return loadUserUmaPolicies(context)
                .thenAsync(new AsyncFunction<Void, Pair<QueryResult, Collection<UmaPolicy>>, ResourceException>() {
                    @Override
                    public Promise<Pair<QueryResult, Collection<UmaPolicy>>, ResourceException> apply(Void value) {
                        Collection<UmaPolicy> umaPolicies;
                        Cache<String, UmaPolicy> userCache = umaPolicyStore.getUserCache(userId, getRealm(context));
                        if (userCache != null) {
                            umaPolicies = userCache.asMap().values();
                        } else {
                            umaPolicies = new HashSet<UmaPolicy>();
                        }

                        try {
                            QueryFilter queryFilter = umaQueryRequest.getQueryFilter();
                            if (queryFilter == null) {
                                queryFilter = QueryFilter.alwaysTrue();
                            }
                            PolicySearch policySearch = queryFilter.accept(
                                    new UmaPolicyQueryFilterVisitor(), new PolicySearch(umaPolicies));
                            List<UmaPolicy> sortedPolicies = new ArrayList<UmaPolicy>(policySearch.getPolicies());
                            for (SortKey key : umaQueryRequest.getSortKeys()) {
                                Collections.sort(sortedPolicies, new UmaPolicyComparator(key));
                            }
                            return Promises.newSuccessfulPromise(
                                    Pair.of(new QueryResult(), (Collection<UmaPolicy>) sortedPolicies));
                        } catch (UnsupportedOperationException e) {
                            return Promises.newFailedPromise(
                                    (ResourceException) new BadRequestException(e.getMessage(), e));
                        }
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearCache() {
        umaPolicyStore.clearCache();
    }

    private Promise<Void, ResourceException> loadUserUmaPolicies(final ServerContext context) {
        QueryRequest request;
        final String userId;
        try {
            userId = getLoggedInUserId(context);
            request = createQueryRequest(userId);
        } catch (ResourceException e) {
            return Promises.newFailedPromise(e);
        }
        return policyResourceDelegate.queryPolicies(context, request)
                .thenAsync(new AsyncFunction<Pair<QueryResult, List<Resource>>, Void, ResourceException>() {
                    @Override
                    public Promise<Void, ResourceException> apply(Pair<QueryResult, List<Resource>> value) {

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
                            for (Map.Entry<String, Set<Resource>> entry : policyMapping.entrySet()) {
                                ResourceSetDescription resourceSet = getResourceSetDescription(entry.getKey(), context);
                                umaPolicyStore.addToUserCache(userId, getRealm(context), resourceSet.getId(),
                                        UmaPolicy.fromUnderlyingPolicies(resourceSet, entry.getValue()));
                            }
                            return Promises.newSuccessfulPromise(null);
                        } catch (ResourceException e) {
                            return Promises.newFailedPromise(e);
                        }
                    }
                });
    }

    private ResourceSetDescription getResourceSetDescription(String resourceSetUid, ServerContext context)
            throws ResourceException {
        try {
            RealmContext realmContext = context.asContext(RealmContext.class);
            return resourceSetStoreFactory.create(realmContext.getResolvedRealm()).read(resourceSetUid);
        } catch (org.forgerock.oauth2.core.exceptions.NotFoundException e) {
            throw new BadRequestException("Invalid ResourceSet UID", e);
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

    private QueryRequest createQueryRequest(String userId) {
        return Requests.newQueryRequest("")
                .setQueryFilter(QueryFilter.and(QueryFilter.equalTo("/createdBy", userId),
                        QueryFilter.equalTo("/applicationName", UMA_POLICY_APPLICATION_TYPE)));
    }
}
