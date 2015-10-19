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

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.oauth2.extensions.ExtensionFilterManager;
import org.forgerock.openam.uma.extensions.ResourceDelegationFilter;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.oauth2.restlet.resources.ResourceSetDescriptionValidator;
import org.forgerock.openam.cts.api.fields.ResourceSetTokenField;
import org.forgerock.openam.oauth2.resources.labels.LabelType;
import org.forgerock.openam.oauth2.resources.labels.ResourceSetLabel;
import org.forgerock.openam.oauth2.resources.labels.UmaLabelsStore;
import org.forgerock.openam.oauth2.rest.AggregateQuery;
import org.forgerock.openam.rest.query.QueryResponsePresentation;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;

/**
 * <p>Resource Set resource to expose registered Resource Sets for a given user.</p>
 *
 * <p>Only non-modifiable operations allowed. To alter a Resource Set use the OAuth2 Resource Set
 * Registration endpoint.</p>
 *
 * @since 13.0.0
 */
public class ResourceSetResource implements CollectionResourceProvider {

    private static final String ID = "_id";

    static final String SUBJECT_FIELD = "/permissions/subject";
    private final ResourceSetService resourceSetService;
    private final ContextHelper contextHelper;
    private final UmaLabelsStore umaLabelsStore;
    private final ResourceSetDescriptionValidator validator;
    private final ExtensionFilterManager extensionFilterManager;

    /**
     * Constructs a new ResourceSetResource instance.
     *
     * @param resourceSetService An instance of the ResourceSetService.
     * @param contextHelper An instance of the ContextHelper.
     * @param extensionFilterManager An instance of the ExtensionFilterManager.
     */
    @Inject
    public ResourceSetResource(ResourceSetService resourceSetService, ContextHelper contextHelper,
            UmaLabelsStore umaLabelsStore, ResourceSetDescriptionValidator validator,
            ExtensionFilterManager extensionFilterManager) {
        this.resourceSetService = resourceSetService;
        this.contextHelper = contextHelper;
        this.umaLabelsStore = umaLabelsStore;
        this.validator = validator;
        this.extensionFilterManager = extensionFilterManager;
    }

    /**
     * Not supported.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request) {
        return new NotSupportedException().asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId,
            ReadRequest request) {
        boolean augmentWithPolicies = augmentWithPolicies(request);
        String realm = getRealm(context);
        final String resourceOwnerId = getUserId(context);
        return resourceSetService.getResourceSet(context, realm, resourceId, resourceOwnerId, augmentWithPolicies)
                .thenAsync(new AsyncFunction<ResourceSetDescription, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(ResourceSetDescription result) {
                        try {
                            JsonValue content = null;
                            content = getResourceSetJson(result, resourceOwnerId);
                            return newResultPromise(newResource(result.getId(), content));
                        } catch (ResourceException e) {
                            return e.asPromise();
                        }
                    }
                });
    }

    /**
     * "revokeAll" action supported, which will delete all a user's resource set UMA policies.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {

        if ("revokeAll".equalsIgnoreCase(request.getAction())) {
            String realm = getRealm(context);
            String resourceOwnerId = getUserId(context);
            return resourceSetService.revokeAllPolicies(context, realm, resourceOwnerId)
                    .thenAsync(new AsyncFunction<Void, ActionResponse, ResourceException>() {
                        @Override
                        public Promise<ActionResponse, ResourceException> apply(Void value) throws ResourceException {
                            return newResultPromise(newActionResponse(json(object())));
                        }
                    });
        } else {
            return new NotSupportedException("Action " + request.getAction() + " not supported").asPromise();
        }
    }

    /**
     * Support for querying by equals for /name and /resourceServer with AND only.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(final Context context, final QueryRequest request,
            final QueryResourceHandler handler) {

        final String userId = getUserId(context);
        final ResourceSetWithPolicyQuery query;
        try {
            if (request.getQueryId() != null && request.getQueryId().equals("*")) {
                query = new ResourceSetWithPolicyQuery();
                query.setResourceSetQuery(QueryFilter.<String>alwaysTrue());
            } else if (request.getQueryFilter() != null) {
                query = beforeQueryResourceSets(userId, request)
                        .accept(new ResourceSetQueryFilter(context), new ResourceSetWithPolicyQuery());
            } else {
                return new BadRequestException("Invalid query").asPromise();
            }
        } catch (UnsupportedOperationException e) {
            return new NotSupportedException(e.getMessage()).asPromise();
        }

        boolean augmentWithPolicies = augmentWithPolicies(request);
        String realm = getRealm(context);
        return resourceSetService.getResourceSets(context, realm, query, userId, augmentWithPolicies)
                .thenAsync(new AsyncFunction<Collection<ResourceSetDescription>, QueryResponse, ResourceException>() {
                    @Override
                    public Promise<QueryResponse, ResourceException> apply(Collection<ResourceSetDescription> resourceSets) {
                        try {
                            List<ResourceResponse> resources = new ArrayList<>();
                            for (ResourceSetDescription resourceSet : resourceSets) {
                                resources.add(newResource(resourceSet.getId(),
                                        getResourceSetJson(resourceSet, userId)));
                            }
                            QueryResponsePresentation.enableDeprecatedRemainingQueryResponse(request);
                            return QueryResponsePresentation.perform(handler, request, resources);
                        } catch (ResourceException e) {
                            return e.asPromise();
                        }
                    }
                });
    }

    private QueryFilter<JsonPointer> beforeQueryResourceSets(String userId, QueryRequest request) {
        QueryFilter<JsonPointer> queryFilter = request.getQueryFilter();
        for (ResourceDelegationFilter filter :
                extensionFilterManager.getFilters(ResourceDelegationFilter.class)) {
            QueryFilter<JsonPointer> extensionQueryFilter = filter.beforeQueryResourceSets(userId, queryFilter);
            if (extensionQueryFilter != null) {
                queryFilter = extensionQueryFilter;
            }
        }
        return queryFilter;
    }

    private String getUserId(Context context) {
        return contextHelper.getUserId(context);
    }

    private String getRealm(Context context) {
        return contextHelper.getRealm(context);
    }

    private boolean augmentWithPolicies(Request request) {
        return request.getFields().isEmpty() || request.getFields().contains(new JsonPointer("/policy"));
    }

    private ResourceResponse newResource(String id, JsonValue content) {
        return newResourceResponse(id, Long.toString(content.hashCode()), content);
    }

    private JsonValue getResourceSetJson(ResourceSetDescription resourceSet, String userId) throws ResourceException {
        HashMap<String, Object> content = new HashMap<String, Object>(resourceSet.asMap());
        content.put(ID, resourceSet.getId());
        content.put("resourceServer", resourceSet.getClientId());
        content.put("resourceOwnerId", resourceSet.getResourceOwnerId());


        Set<ResourceSetLabel> labels = umaLabelsStore.forResourceSet(resourceSet.getRealm(), resourceSet.getResourceOwnerId(), resourceSet.getId(), false);
        Set<String> labelIds = new HashSet<>();

        boolean filterOutSystemLabels = userId.equals(resourceSet.getResourceOwnerId());

        for (ResourceSetLabel label : labels) {
            if (filterOutSystemLabels && label.getType().equals(LabelType.SYSTEM)) {
                continue;
            } else {
                labelIds.add(label.getId());
            }
        }
        content.put("labels", labelIds);

        if (resourceSet.getPolicy() != null) {
            JsonValue policy = new JsonValue(new HashMap<String, Object>(resourceSet.getPolicy().asMap()));
            policy.remove("policyId");
            policy.remove("name");
            content.put("policy", policy.getObject());
        }
        return new JsonValue(content);
    }

    private final class ResourceSetQueryFilter
            implements QueryFilterVisitor<ResourceSetWithPolicyQuery, ResourceSetWithPolicyQuery, JsonPointer> {

        private final Map<JsonPointer, String> queryableFields = new HashMap<JsonPointer, String>();
        private final Context context;
        private int queryDepth = 0;

        private ResourceSetQueryFilter(Context context) {
            this.context = context;
            queryableFields.put(new JsonPointer("/name"), ResourceSetTokenField.NAME);
            queryableFields.put(new JsonPointer("/resourceServer"), ResourceSetTokenField.CLIENT_ID);
            queryableFields.put(new JsonPointer("/resourceOwnerId"), ResourceSetTokenField.RESOURCE_OWNER_ID);
        }

        private void increaseQueryDepth() {
            queryDepth++;
        }

        private void decreaseQueryDepth() {
            queryDepth--;
        }

        @Override
        public ResourceSetWithPolicyQuery visitAndFilter(ResourceSetWithPolicyQuery query,
                List<QueryFilter<JsonPointer>> subFilters) {
            increaseQueryDepth();
            List<QueryFilter<String>> childFilters =
                    new ArrayList<QueryFilter<String>>();
            for (QueryFilter<JsonPointer> filter : subFilters) {
                ResourceSetWithPolicyQuery subResourceSetWithPolicyQuery = filter.accept(this, query);
                if (subResourceSetWithPolicyQuery.getPolicyQuery() != null) {
                    subResourceSetWithPolicyQuery.setOperator(AggregateQuery.Operator.AND);
                } else {
                    childFilters.add(subResourceSetWithPolicyQuery.getResourceSetQuery());
                }
            }
            decreaseQueryDepth();
            query.setResourceSetQuery(QueryFilter.and(childFilters));
            return query;
        }

        @Override
        public ResourceSetWithPolicyQuery visitOrFilter(ResourceSetWithPolicyQuery query,
                List<QueryFilter<JsonPointer>> subFilters) {
            increaseQueryDepth();
            List<QueryFilter<String>> childFilters =
                    new ArrayList<QueryFilter<String>>();
            for (QueryFilter<JsonPointer> filter : subFilters) {
                ResourceSetWithPolicyQuery subResourceSetWithPolicyQuery = filter.accept(this, query);
                if (subResourceSetWithPolicyQuery.getPolicyQuery() != null) {
                    subResourceSetWithPolicyQuery.setOperator(AggregateQuery.Operator.OR);
                } else {
                    childFilters.add(subResourceSetWithPolicyQuery.getResourceSetQuery());
                }
            }
            decreaseQueryDepth();
            query.setResourceSetQuery(QueryFilter.or(childFilters));
            return query;
        }

        @Override
        public ResourceSetWithPolicyQuery visitEqualsFilter(ResourceSetWithPolicyQuery query, JsonPointer field,
                Object valueAssertion) {
            if (new JsonPointer("/policy/permissions/subject").equals(field)) {
                if (queryDepth > 1) {
                    throw new UnsupportedOperationException("Cannot nest queries on /policy/permissions/subject field");
                }
                query.setPolicyQuery(QueryFilter.equalTo(new JsonPointer(SUBJECT_FIELD), valueAssertion));
            } else if (new JsonPointer("/labels").equals(field)) {
                ResourceSetLabel label = null;
                try {
                    label = umaLabelsStore.read(getRealm(context), getUserId(context), (String) valueAssertion);
                } catch (ResourceException e) {
                    throw new IllegalArgumentException("Unknown Label ID.");
                }
                List<QueryFilter<String>> labelFilters = new ArrayList<>();
                for (String resourceSetId : label.getResourceSetIds()) {
                    labelFilters.add(QueryFilter.equalTo( ResourceSetTokenField.RESOURCE_SET_ID, resourceSetId));
                }
                query.setResourceSetQuery(QueryFilter.or(labelFilters));
            } else {
                query.setResourceSetQuery(
                        QueryFilter.equalTo(verifyFieldIsQueryable(field), valueAssertion));
            }
            return query;
        }

        @Override
        public ResourceSetWithPolicyQuery visitStartsWithFilter(ResourceSetWithPolicyQuery query, JsonPointer field,
                Object valueAssertion) {
            if (new JsonPointer("/policy/permissions/subject").equals(field)) {
                if (queryDepth > 1) {
                    throw new UnsupportedOperationException("Cannot nest queries on /policy/permissions/subject field");
                }
                query.setPolicyQuery(QueryFilter.startsWith(new JsonPointer(SUBJECT_FIELD), valueAssertion));
            } else {
                query.setResourceSetQuery(
                        QueryFilter.startsWith(verifyFieldIsQueryable(field), valueAssertion));
            }
            return query;
        }

        @Override
        public ResourceSetWithPolicyQuery visitContainsFilter(ResourceSetWithPolicyQuery query, JsonPointer field,
                Object valueAssertion) {
            if (new JsonPointer("/policy/permissions/subject").equals(field)) {
                if (queryDepth > 1) {
                    throw new UnsupportedOperationException("Cannot nest queries on /policy/permissions/subject field");
                }
                query.setPolicyQuery(QueryFilter.contains(new JsonPointer(SUBJECT_FIELD), valueAssertion));
            } else {
                query.setResourceSetQuery(
                        QueryFilter.contains(verifyFieldIsQueryable(field), valueAssertion));
            }
            return query;
        }

        private String verifyFieldIsQueryable(JsonPointer field) {
            if (!queryableFields.containsKey(field)) {
                throw new UnsupportedOperationException("'" + field + "' not queryable");
            } else {
                return queryableFields.get(field);
            }
        }

        @Override
        public ResourceSetWithPolicyQuery visitBooleanLiteralFilter(ResourceSetWithPolicyQuery query, boolean value) {
            throw unsupportedFilterOperation("Boolean Literal");
        }

        @Override
        public ResourceSetWithPolicyQuery visitExtendedMatchFilter(ResourceSetWithPolicyQuery query, JsonPointer field,
                String operator, Object valueAssertion) {
            throw unsupportedFilterOperation("Extended match");
        }

        @Override
        public ResourceSetWithPolicyQuery visitGreaterThanFilter(ResourceSetWithPolicyQuery query, JsonPointer field,
                Object valueAssertion) {
            throw unsupportedFilterOperation("Greater than");
        }

        @Override
        public ResourceSetWithPolicyQuery visitGreaterThanOrEqualToFilter(ResourceSetWithPolicyQuery query,
                JsonPointer field, Object valueAssertion) {
            throw unsupportedFilterOperation("Greater than or equal");
        }

        @Override
        public ResourceSetWithPolicyQuery visitLessThanFilter(ResourceSetWithPolicyQuery query, JsonPointer field,
                Object valueAssertion) {
            throw unsupportedFilterOperation("Less than");
        }

        @Override
        public ResourceSetWithPolicyQuery visitLessThanOrEqualToFilter(ResourceSetWithPolicyQuery query,
                JsonPointer field, Object valueAssertion) {
            throw unsupportedFilterOperation("Less than or equal");
        }

        @Override
        public ResourceSetWithPolicyQuery visitNotFilter(final ResourceSetWithPolicyQuery query,
                QueryFilter<JsonPointer> subFilter) {
            query.setResourceSetQuery(QueryFilter.not(
                    subFilter.accept(new QueryFilterVisitor<QueryFilter<String>, QueryFilter<JsonPointer>, JsonPointer>() {

                        @Override
                        public QueryFilter<String> visitAndFilter(QueryFilter<JsonPointer> queryFilter, List<QueryFilter<JsonPointer>> list) {
                            throw unsupportedFilterOperation("And");
                        }

                        @Override
                        public QueryFilter<String> visitBooleanLiteralFilter(QueryFilter<JsonPointer> queryFilter, boolean b) {
                            throw unsupportedFilterOperation("Boolean");
                        }

                        @Override
                        public QueryFilter<String> visitContainsFilter(QueryFilter<JsonPointer> queryFilter, JsonPointer jsonPointer, Object o) {
                            throw unsupportedFilterOperation("Contains");
                        }

                        @Override
                        public QueryFilter<String> visitEqualsFilter(QueryFilter<JsonPointer> queryFilter, JsonPointer jsonPointer, Object o) {
                            return QueryFilter.equalTo(verifyFieldIsQueryable(jsonPointer), o);
                        }

                        @Override
                        public QueryFilter<String> visitExtendedMatchFilter(QueryFilter<JsonPointer> queryFilter, JsonPointer jsonPointer, String s, Object o) {
                            throw unsupportedFilterOperation("Extended Match");
                        }

                        @Override
                        public QueryFilter<String> visitGreaterThanFilter(QueryFilter<JsonPointer> queryFilter, JsonPointer jsonPointer, Object o) {
                            throw unsupportedFilterOperation("Greater Than");
                        }

                        @Override
                        public QueryFilter<String> visitGreaterThanOrEqualToFilter(QueryFilter<JsonPointer> queryFilter, JsonPointer jsonPointer, Object o) {
                            throw unsupportedFilterOperation("Greater Than Equal To");
                        }

                        @Override
                        public QueryFilter<String> visitLessThanFilter(QueryFilter<JsonPointer> queryFilter, JsonPointer jsonPointer, Object o) {
                            throw unsupportedFilterOperation("Less Than");
                        }

                        @Override
                        public QueryFilter<String> visitLessThanOrEqualToFilter(QueryFilter<JsonPointer> queryFilter, JsonPointer jsonPointer, Object o) {
                            throw unsupportedFilterOperation("Less Than Equal To");
                        }

                        @Override
                        public QueryFilter<String> visitNotFilter(QueryFilter<JsonPointer> queryFilter, QueryFilter<JsonPointer> queryFilter2) {
                            throw unsupportedFilterOperation("Not");
                        }

                        @Override
                        public QueryFilter<String> visitOrFilter(QueryFilter<JsonPointer> queryFilter, List<QueryFilter<JsonPointer>> list) {
                            throw unsupportedFilterOperation("Or");
                        }

                        @Override
                        public QueryFilter<String> visitPresentFilter(QueryFilter<JsonPointer> queryFilter, JsonPointer jsonPointer) {
                            throw unsupportedFilterOperation("Present");
                        }

                        @Override
                        public QueryFilter<String> visitStartsWithFilter(QueryFilter<JsonPointer> queryFilter, JsonPointer jsonPointer, Object o) {
                            throw unsupportedFilterOperation("Starts with");
                        }
                    }, subFilter)));
            return query;
        }

        @Override
        public ResourceSetWithPolicyQuery visitPresentFilter(ResourceSetWithPolicyQuery query, JsonPointer field) {
            throw unsupportedFilterOperation("Present");
        }

        private UnsupportedOperationException unsupportedFilterOperation(String filterType) {
            return new UnsupportedOperationException("'" + filterType + "' not supported");
        }
    }

    /**
     * Update the none system labels on a resource set only
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId,
            UpdateRequest request) {

        final Map<String, Object> resourceSetDescriptionAttributes;
        try {
            resourceSetDescriptionAttributes = validator.validate(request.getContent().asMap());

            final String realm = getRealm(context);
            final String userId = getUserId(context);

            //remove this resource set id from all labels
            Set<ResourceSetLabel> labels = umaLabelsStore.forResourceSet(realm, userId, resourceId, true);
            for (ResourceSetLabel label : labels) {
                if (!isSystemLabel(label)) {
                    label.removeResourceSetId(resourceId);
                    umaLabelsStore.update(realm, userId, label);
                }
            }

            //add resource set id to new labels
            for (String labelId : (List<String>) resourceSetDescriptionAttributes.get("labels")) {
                ResourceSetLabel label = umaLabelsStore.read(realm, userId, labelId);
                label.addResourceSetId(resourceId);
                umaLabelsStore.update(realm, userId, label);
            }

            return resourceSetService.getResourceSet(context, realm, resourceId, userId, augmentWithPolicies(request))
                    .thenAsync(new AsyncFunction<ResourceSetDescription, ResourceResponse, ResourceException>() {
                        @Override
                        public Promise<ResourceResponse, ResourceException> apply(ResourceSetDescription result) {
                            try {
                                JsonValue content = null;
                                content = getResourceSetJson(result, userId);
                                return newResultPromise(newResource(result.getId(), content));
                            } catch (ResourceException e) {
                                return e.asPromise();
                            }
                        }
                    });
        } catch (ResourceException e) {
            return e.asPromise();
        } catch (org.forgerock.oauth2.core.exceptions.BadRequestException e) {
            return new BadRequestException("Error retrieving labels.", e).asPromise();
        }
    }

    private boolean isSystemLabel(ResourceSetLabel label) {
        return label.getId().contains("/");
    }

    /**
     * Not supported.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String resourceId,
            DeleteRequest request) {
        return new NotSupportedException().asPromise();
    }

    /**
     * Not supported.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId,
            PatchRequest request) {
        return new NotSupportedException().asPromise();
    }

    /**
     * Not supported.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId,
            ActionRequest request) {
        return new NotSupportedException().asPromise();
    }

    /**
     * Used for containing all values required during a query to be converted to a ResourceResponse.
     */
    private static class QueryPayload {
        ResourceSetDescription description;
        String userId;
        public QueryPayload(ResourceSetDescription description, String userId) {
            this.description = description;
            this.userId = userId;
        }
    }
}
