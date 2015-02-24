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

package org.forgerock.openam.rest.oauth2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryFilterVisitor;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.openam.cts.api.fields.ResourceSetTokenField;
import org.forgerock.openam.forgerockrest.entitlements.query.QueryResultHandlerBuilder;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.uma.UmaPolicy;
import org.forgerock.openam.uma.UmaPolicyService;
import org.forgerock.util.promise.AsyncFunction;
import org.forgerock.util.promise.FailureHandler;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.promise.SuccessHandler;

/**
 * <p>Resource Set resource to expose registered Resource Sets for a given user.</p>
 *
 * <p>Only non-modifiable operations allowed. To alter a Resource Set use the OAuth2 Resource Set Registration
 * endpoint.</p>
 *
 * @since 13.0.0
 */
public class ResourceSetResource implements CollectionResourceProvider {

    private final ResourceSetStoreFactory resourceSetStoreFactory;
    private final UmaPolicyService policyService;

    /**
     * Constructs a new ResourceSetResource instance.
     *
     * @param resourceSetStoreFactory An instance of the ResourceSetStoreFactory.
     * @param policyService An instance of the UmaPolicyService.
     */
    @Inject
    public ResourceSetResource(ResourceSetStoreFactory resourceSetStoreFactory, UmaPolicyService policyService) {
        this.resourceSetStoreFactory = resourceSetStoreFactory;
        this.policyService = policyService;
    }

    /**
     * Not supported.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request,
            final ResultHandler<Resource> handler) {

        try {
            RealmContext realmContext = context.asContext(RealmContext.class);
            ResourceSetDescription resourceSet = resourceSetStoreFactory.create(realmContext.getResolvedRealm())
                    .read(resourceId);

            if (!request.getFields().isEmpty() && !request.getFields().contains(new JsonPointer("/policy"))) {
                handler.handleResult(newResource(resourceId, getResourceSetContent(resourceSet)));
            } else {
                augmentResourceSetWithPolicy(context, resourceId, resourceSet)
                        .onSuccess(new SuccessHandler<Resource>() {
                            @Override
                            public void handleResult(Resource result) {
                                handler.handleResult(result);
                            }
                        })
                        .onFailure(new FailureHandler<ResourceException>() {
                            @Override
                            public void handleError(ResourceException error) {
                                handler.handleError(error);
                            }
                        });
            }

        } catch (org.forgerock.oauth2.core.exceptions.NotFoundException e) {
            handler.handleError(new NotFoundException("No resource set with uid, " + resourceId + ", found."));
        } catch (org.forgerock.oauth2.core.exceptions.ServerException e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    private JsonValue getResourceSetContent(ResourceSetDescription resourceSet) {
        HashMap<String, Object> content = new HashMap<String, Object>(resourceSet.asMap());
        content.put("resourceServer", resourceSet.getClientId());
        return new JsonValue(content);
    }

    private Promise<Resource, ResourceException> augmentResourceSetWithPolicy(ServerContext context,
            final String resourceId, final ResourceSetDescription resourceSet) {
        return policyService.readPolicy(context, resourceId)
                .thenAsync(new AsyncFunction<UmaPolicy, Resource, ResourceException>() {
                    @Override
                    public Promise<Resource, ResourceException> apply(UmaPolicy result) throws ResourceException {
                        JsonValue content = getResourceSetContent(resourceSet);
                        JsonValue policy = result.asJson();
                        policy.remove("policyId");
                        policy.remove("name");
                        content.add("policy", policy.getObject());
                        return Promises.newSuccessfulPromise(newResource(resourceId, content));
                    }
                }, new AsyncFunction<ResourceException, Resource, ResourceException>() {
                    @Override
                    public Promise<Resource, ResourceException> apply(ResourceException e) throws ResourceException {
                        JsonValue content = getResourceSetContent(resourceSet);
                        content.add("policy", null);
                        return Promises.newSuccessfulPromise(newResource(resourceId, content));
                    }
                });
    }

    /**
     * Not supported.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
            ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }

    /**
     * Not supported.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
            ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }

    /**
     * Not supported.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
            ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }

    /**
     * Not supported.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException());
    }

    /**
     * Not supported.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException());
    }

    /**
     * Support for querying by equals for /name and /resourceServer with AND only.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {

        final QueryResultHandler queryHandler = QueryResultHandlerBuilder.withPagingAndSorting(handler, request);

        org.forgerock.util.query.QueryFilter<String> query;
        try {
            query = request.getQueryFilter().accept(new ResourceSetQueryFilter(), null);
        } catch (UnsupportedOperationException e) {
            handler.handleError(new NotSupportedException(e.getMessage()));
            return;
        }

        RealmContext realmContext = context.asContext(RealmContext.class);
        try {
            Set<ResourceSetDescription> resourceSets = resourceSetStoreFactory.create(realmContext.getResolvedRealm())
                    .query(query);

            for (ResourceSetDescription resourceSet : resourceSets) {
                if (!request.getFields().isEmpty() && !request.getFields().contains(new JsonPointer("/policy"))) {
                    queryHandler.handleResource(newResource(resourceSet.getId(), getResourceSetContent(resourceSet)));
                } else {
                    augmentResourceSetWithPolicy(context, resourceSet.getId(), resourceSet)
                            .onSuccess(new SuccessHandler<Resource>() {
                                @Override
                                public void handleResult(Resource result) {
                                    queryHandler.handleResource(result);
                                }
                            })
                            .onFailure(new FailureHandler<ResourceException>() {
                                @Override
                                public void handleError(ResourceException error) {
                                    queryHandler.handleError(error);
                                }
                            });
                }
            }
            queryHandler.handleResult(new QueryResult());
        } catch (org.forgerock.oauth2.core.exceptions.ServerException e) {
            queryHandler.handleError(new InternalServerErrorException(e));
        }
    }

    private Resource newResource(String id, JsonValue content) {
        return new Resource(id, Long.toString(content.hashCode()), content);
    }

    private static final class ResourceSetQueryFilter
            implements QueryFilterVisitor<org.forgerock.util.query.QueryFilter<String>, Void> {

        private final Map<JsonPointer, String> queryableFields = new HashMap<JsonPointer, String>();

        private ResourceSetQueryFilter() {
            queryableFields.put(new JsonPointer("/name"), ResourceSetTokenField.NAME);
            queryableFields.put(new JsonPointer("/resourceServer"), ResourceSetTokenField.CLIENT_ID);
        }

        @Override
        public org.forgerock.util.query.QueryFilter<String> visitAndFilter(Void aVoid, List<QueryFilter> subFilters) {
            List<org.forgerock.util.query.QueryFilter<String>> childFilters =
                    new ArrayList<org.forgerock.util.query.QueryFilter<String>>();
            for (QueryFilter filter : subFilters) {
                childFilters.add(filter.accept(this, null));
            }
            return org.forgerock.util.query.QueryFilter.and(childFilters);
        }

        @Override
        public org.forgerock.util.query.QueryFilter<String> visitEqualsFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
            return org.forgerock.util.query.QueryFilter.equalTo(verifyFieldIsQueryable(field), valueAssertion);
        }

        @Override
        public org.forgerock.util.query.QueryFilter<String> visitStartsWithFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
            return org.forgerock.util.query.QueryFilter.startsWith(verifyFieldIsQueryable(field), valueAssertion);
        }

        @Override
        public org.forgerock.util.query.QueryFilter<String> visitContainsFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
            return org.forgerock.util.query.QueryFilter.contains(verifyFieldIsQueryable(field), valueAssertion);
        }

        private String verifyFieldIsQueryable(JsonPointer field) {
            if (!queryableFields.containsKey(field)) {
                throw new UnsupportedOperationException("'" + field + "' not queryable");
            } else {
                return queryableFields.get(field);
            }
        }

        @Override
        public org.forgerock.util.query.QueryFilter<String> visitBooleanLiteralFilter(Void aVoid, boolean value) {
            throw unsupportedFilterOperation("Boolean Literal");
        }

        @Override
        public org.forgerock.util.query.QueryFilter<String> visitExtendedMatchFilter(Void aVoid, JsonPointer field, String operator, Object valueAssertion) {
            throw unsupportedFilterOperation("Extended match");
        }

        @Override
        public org.forgerock.util.query.QueryFilter<String> visitGreaterThanFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
            throw unsupportedFilterOperation("Greater than");
        }

        @Override
        public org.forgerock.util.query.QueryFilter<String> visitGreaterThanOrEqualToFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
            throw unsupportedFilterOperation("Greater than or equal");
        }

        @Override
        public org.forgerock.util.query.QueryFilter<String> visitLessThanFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
            throw unsupportedFilterOperation("Less than");
        }

        @Override
        public org.forgerock.util.query.QueryFilter<String> visitLessThanOrEqualToFilter(Void aVoid, JsonPointer field, Object valueAssertion) {
            throw unsupportedFilterOperation("Less than or equal");
        }

        @Override
        public org.forgerock.util.query.QueryFilter<String> visitNotFilter(Void aVoid, QueryFilter subFilter) {
            throw unsupportedFilterOperation("Not");
        }

        @Override
        public org.forgerock.util.query.QueryFilter<String> visitOrFilter(Void aVoid, List<QueryFilter> subFilters) {
            throw unsupportedFilterOperation("Or");
        }

        @Override
        public org.forgerock.util.query.QueryFilter<String> visitPresentFilter(Void aVoid, JsonPointer field) {
            throw unsupportedFilterOperation("Present");
        }

        private UnsupportedOperationException unsupportedFilterOperation(String filterType) {
            return new UnsupportedOperationException("'" + filterType + "' not supported");
        }

    }
}
