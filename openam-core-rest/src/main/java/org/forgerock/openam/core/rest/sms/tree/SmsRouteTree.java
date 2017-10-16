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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.sms.tree;

import static java.util.Arrays.asList;
import static org.forgerock.api.models.Action.action;
import static org.forgerock.api.models.ApiDescription.apiDescription;
import static org.forgerock.api.models.Definitions.definitions;
import static org.forgerock.api.models.Paths.paths;
import static org.forgerock.api.models.Resource.resource;
import static org.forgerock.api.models.VersionedPath.UNVERSIONED;
import static org.forgerock.api.models.VersionedPath.versionedPath;
import static org.forgerock.authz.filter.crest.AuthorizationFilters.createAuthorizationFilter;
import static org.forgerock.http.routing.RoutingMode.EQUALS;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Requests.newActionRequest;
import static org.forgerock.json.resource.Requests.newQueryRequest;
import static org.forgerock.json.resource.Requests.newReadRequest;
import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.NOT_FOUND;
import static org.forgerock.json.resource.ResourceException.NOT_SUPPORTED;
import static org.forgerock.json.resource.ResourcePath.empty;
import static org.forgerock.json.resource.ResourcePath.resourcePath;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Router.uriTemplate;
import static org.forgerock.openam.rest.RestConstants.FOR_UI;
import static org.forgerock.openam.rest.RestConstants.GET_ALL_TYPES;
import static org.forgerock.openam.rest.RestConstants.GET_CREATABLE_TYPES;
import static org.forgerock.openam.rest.RestConstants.GET_TYPE;
import static org.forgerock.openam.rest.RestConstants.NEXT_DESCENDENTS;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.api.models.Action;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.Definitions;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.forgerock.api.models.VersionedPath;
import org.forgerock.authz.filter.crest.api.CrestAuthorizationModule;
import com.google.common.base.Predicate;
import org.forgerock.http.ApiProducer;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.http.routing.Version;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.core.rest.sms.tree.SmsRouteTreeBuilder.SmsRouter;
import org.forgerock.openam.forgerockrest.utils.MatchingResourcePath;
import org.forgerock.openam.rest.DescriptorUtils;
import org.forgerock.openam.rest.RestConstants;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.services.routing.RouteMatcher;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;

/**
 * Represents a {@code Router} tree. Allows the structuring of Routers in a tree like manner whilst
 * offering a flat entry point to add and remove routes and services.
 *
 * @since 13.0.0
 */
public class SmsRouteTree implements RequestHandler, Describable<ApiDescription, Request> {

    private static final QueryFilter<JsonPointer> ALWAYS_TRUE = QueryFilter.alwaysTrue();
    private static final String SCRIPTING_SERVICE_NAME = "scripting";
    private static final ClassLoader CLASS_LOADER = SmsRouteTree.class.getClassLoader();
    private static final Schema TYPE_SCHEMA = schemaFromResource("restsms.type");

    final Map<MatchingResourcePath, CrestAuthorizationModule> authzModules;
    private final Predicate<String> handlesFunction;
    final CrestAuthorizationModule defaultAuthzModule;
    private final SmsRouter router;
    final ResourcePath path;
    private final boolean isRoot;
    private final Set<SmsRouteTree> subTrees;
    private final Filter filter;
    private final Map<String, RequestHandler> handlers = new LinkedHashMap<>();
    private final String uriTemplate;
    private final Set<String> hiddenFromUI = new HashSet<>();
    private final boolean supportGeneralActions;
    private static final List<Action> GENERAL_ACTIONS = asList(
            action()
                    .name(NEXT_DESCENDENTS)
                    .response(schemaFromResource("nextdescendents.response"))
                    .build(),
            action()
                    .name(GET_ALL_TYPES)
                    .response(schemaFromResource("types.response"))
                    .build(),
            action()
                    .name(GET_CREATABLE_TYPES)
                    .response(schemaFromResource("types.response"))
                    .build());

    private static Schema schemaFromResource(String schemaName) {
        return DescriptorUtils.fromResource("SmsRouteTree." + schemaName + ".schema.json", SmsRouteTree.class);
    }

    /**
     * Creates a {@code SmsRouteTree} instance.
     * @param authzModules Authz modules to use for specific matching resource paths.
     * @param defaultAuthzModule Auth module to use if no matching resouce path exists in {@code authzModules}.
     * @param isRoot {@code true} if this {@code SmsRouteTree} is the root of the tree.
     * @param router The {@code Router} instance.
     * @param filter The filter to wrap around all routes.
     * @param path The path of this tree element.
     * @param uriTemplate The tempate for the node.
     * @param supportGeneralActions Whether to support the general SMS actions of {@code nextdescendents},
     *                              {@code getAllTypes} and {@code getCreateableTypes}.
     */
    SmsRouteTree(Map<MatchingResourcePath, CrestAuthorizationModule> authzModules,
            CrestAuthorizationModule defaultAuthzModule, boolean isRoot, SmsRouter router, Filter filter,
            ResourcePath path, Predicate<String> handlesFunction, String uriTemplate, boolean supportGeneralActions) {
        this.authzModules = authzModules;
        this.defaultAuthzModule = defaultAuthzModule;
        this.isRoot = isRoot;
        this.router = router;
        this.path = path;
        this.subTrees = new HashSet<>();
        this.filter = filter;
        this.handlesFunction = handlesFunction;
        this.uriTemplate = uriTemplate;
        this.supportGeneralActions = supportGeneralActions;
    }

    final void addSubTree(SmsRouteTree subTree) {
        this.subTrees.add(subTree);
        router.addRoute(STARTS_WITH, uriTemplate(subTree.uriTemplate), subTree);
    }

    /**
     * Returns the {@code SmsRouteTree} instance that the route should be added to.
     *
     * @param serviceName The name of the service that the route is being added for.
     * @return The {@code SmsRouteTree} to handle the route.
     */
    public SmsRouteTree handles(String serviceName) {
        if (handlesFunction.apply(serviceName)) {
            return this;
        }
        for (SmsRouteTree subTree : subTrees) {
            SmsRouteTree tree = subTree.handles(serviceName);
            if (tree != null) {
                return tree;
            }
        }
        if (isRoot) {
            return this;
        } else {
            return null;
        }
    }

    public final Route addRoute(RoutingMode mode, String uriTemplate, RequestHandler handler, boolean hideFromUi) {
        if (hideFromUi) {
            this.hiddenFromUI.add(uriTemplate);
        }

        return addRoute(false, mode, uriTemplate, handler);
    }

    /**
     * Adds a new route to this router for the provided request handler. New routes may be added
     * while this router is processing requests.
     *
     * @param mode Indicates how the URI template should be matched against resource names.
     * @param uriTemplate The URI template which request resource names must match.
     * @param handler The request handler to which matching requests will be routed.
     * @return An opaque handle for the route which may be used for removing the route later, and the tree the route
     * leads to.
     */
    public final Route addRoute(RoutingMode mode, String uriTemplate, RequestHandler handler) {
        return addRoute(false, mode, uriTemplate, handler);
    }

    final Route addRoute(boolean internal, RoutingMode mode, String uriTemplate, RequestHandler handler) {
        CrestAuthorizationModule authzModule = authzModules.get(MatchingResourcePath.match(concat(path, uriTemplate)));
        if (authzModule != null || !internal) {
            handler = createAuthorizationFilter(handler, authzModule == null ? defaultAuthzModule : authzModule);
        }

        if (filter != null) {
            handler = new FilterChain(handler, filter);
        }

        if (!uriTemplate.isEmpty()) {
            SmsRouteTree subtree = new SmsRouteTreeBuilder(uriTemplate).supportGeneralActions(supportGeneralActions)
                    .build(this);
            handlers.put(uriTemplate, subtree);
            if (mode.equals(STARTS_WITH)) {
                subtree.router.setDefaultRoute(handler);
            } else {
                subtree.router.addRoute(EQUALS, uriTemplate(""), handler);
            }
            return new Route(router.addRoute(STARTS_WITH, uriTemplate(uriTemplate), subtree), subtree, handler);
        } else {
            return new Route(router.addRoute(mode, uriTemplate(uriTemplate), handler), this, handler);
        }
    }

    /**
     * Removes a route from this router. Routes may be removed while this router is
     * processing requests.
     *
     * @param route The route to be removed.
     * @return {@code true} if at the route was found and removed.
     */
    public final boolean removeRoute(RouteMatcher<Request> route) {
        return router.removeRoute(route);
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        String remainingUri = context.asContext(UriRouterContext.class).getRemainingUri();
        String action = request.getAction();
        boolean forUI = Boolean.parseBoolean(request.getAdditionalParameter(FOR_UI));

        if (supportGeneralActions && NEXT_DESCENDENTS.equals(action) && remainingUri.isEmpty()) {
            JsonValue result = json(object(field("result", array())));
            for (Map.Entry<String, RequestHandler> subRoute : handlers.entrySet()) {
                if (!subRoute.getKey().equals("")) {
                    final ResourcePath subPath = resourcePath(subRoute.getKey());
                    try {
                        readInstances(context, result.get("result"), subPath, subRoute.getValue(), forUI);
                    } catch (ResourceException e) {
                        return e.asPromise();
                    }
                }
            }
            return newActionResponse(result).asPromise();
        } else if (NEXT_DESCENDENTS.equals(action) && remainingUri.isEmpty()) {
            return new NotSupportedException().asPromise();
        } else if (supportGeneralActions && GET_ALL_TYPES.equals(action) && remainingUri.isEmpty()) {
            try {
                return readTypes(context, ALL_CHILD_TYPES, false);
            } catch (ResourceException e) {
                return e.asPromise();
            }
        } else if (GET_ALL_TYPES.equals(action) && remainingUri.isEmpty()) {
            return new NotSupportedException().asPromise();
        } else if (supportGeneralActions && GET_CREATABLE_TYPES.equals(action) && remainingUri.isEmpty()) {
            try {
                if (SCRIPTING_SERVICE_NAME.equals(context.asContext(UriRouterContext.class).getMatchedUri())) {
                    // Default script types cannot be created
                    return newActionResponse(json(array())).asPromise();
                } else {
                    return readTypes(context, NOT_CREATED_SINGLETONS, forUI);
                }
            } catch (ResourceException e) {
                return e.asPromise();
            }
        } else if (GET_CREATABLE_TYPES.equals(action) && remainingUri.isEmpty()) {
            return new NotSupportedException().asPromise();
        } else {
            return router.handleAction(context, request);
        }
    }

    private Promise<ActionResponse, ResourceException> readTypes(Context context, ChildTypePredicate includeType,
                                                                 boolean forUI)
            throws ResourceException {
        JsonValue result = json(array());
        for (Map.Entry<String, RequestHandler> subRoute : handlers.entrySet()) {
            if (!subRoute.getKey().equals("")) {
                try {
                    ActionResponse response = subRoute.getValue()
                            .handleAction(context, newActionRequest(empty(), GET_TYPE))
                            .getOrThrowUninterruptibly();

                    JsonValue jsonContent = response.getJsonContent();
                    if (includeType.apply(jsonContent, context, subRoute.getValue()) && (!hiddenFromUI
                            .contains(subRoute.getKey()) || !forUI)) {
                        result.add(jsonContent.getObject());
                    }
                } catch (ResourceException e) {
                    if (e.getCode() != NOT_SUPPORTED && e.getCode() != BAD_REQUEST && e.getCode() != NOT_FOUND) {
                        throw e;
                    }
                }
            }
        }
        return newActionResponse(json(object(field("result", result.getObject())))).asPromise();
    }

    private void readInstances(Context context, JsonValue response, ResourcePath subPath, RequestHandler handler,
            boolean forUI) throws ResourceException {
        try {
            if (!(hiddenFromUI.contains(subPath.toString()) && forUI)) {
                QueryRequest subRequest = newQueryRequest(empty()).setQueryFilter(ALWAYS_TRUE);
                handler.handleQuery(context, subRequest, new ChildQueryResourceHandler(subPath, response))
                        .getOrThrowUninterruptibly();
            }
        } catch (ResourceException e) {
            if (e.getCode() == NOT_SUPPORTED || e.getCode() == BAD_REQUEST) {
                getSingletonInstance(context, response, subPath, handler);
            } else if (e.getCode() != NOT_FOUND){
                throw e;
            }
        }
    }

    private void getSingletonInstance(Context context, JsonValue response, ResourcePath subPath,
            RequestHandler handler) throws ResourceException {
        try {
            ResourceResponse instance = handler.handleRead(context, newReadRequest(empty()))
                    .getOrThrowUninterruptibly();
            response.add(instance.getContent().put("_id", subPath.toString()).getObject());
        } catch (ResourceException e) {
            if (e.getCode() == NOT_SUPPORTED || e.getCode() == BAD_REQUEST) {
                findFurtherDescendents(context, response, subPath, handler);
            } else if (e.getCode() != NOT_FOUND) {
                throw e;
            }
        }
    }

    private void findFurtherDescendents(Context context, JsonValue response, ResourcePath subPath,
            RequestHandler handler) throws ResourceException {
        ActionRequest subRequest = newActionRequest(empty(), NEXT_DESCENDENTS);
        try {
            JsonValue result = handler.handleAction(context, subRequest).getOrThrowUninterruptibly().getJsonContent();
            for (JsonValue item : result.get(RestConstants.RESULT)) {
                response.add(item.getObject());
            }
        } catch (ResourceException e) {
            if (e.getCode() != NOT_FOUND) {
                throw e;
            }
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        return router.handleCreate(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        return router.handleDelete(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        return router.handlePatch(context, request);
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        return router.handleQuery(context, request, handler);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        return router.handleRead(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        return router.handleUpdate(context, request);
    }

    static ResourcePath concat(ResourcePath parent, String child) {
        if (StringUtils.isEmpty(child)) {
            return parent;
        }
        ResourcePath childPath = resourcePath(child);
        return parent.concat(childPath);
    }

    @Override
    public ApiDescription api(ApiProducer<ApiDescription> apiProducer) {
        ApiDescription api = router.api(apiProducer);
        if (api == null || !supportGeneralActions) {
            return api;
        }
        VersionedPath basePath = api.getPaths().get("");
        if (basePath != null) {
            VersionedPath.Builder pathBuilder = versionedPath();
            for (Version v : basePath.getVersions()) {
                Resource resource = basePath.get(v);
                pathBuilder.put(v, resource()
                        .mvccSupported(resource.isMvccSupported())
                        .description(resource.getDescription())
                        .title(resource.getTitle())
                        .actions(asList(resource.getActions()))
                        .actions(GENERAL_ACTIONS)
                        .create(resource.getCreate())
                        .read(resource.getRead())
                        .update(resource.getUpdate())
                        .delete(resource.getDelete())
                        .patch(resource.getPatch())
                        .queries(asList(resource.getQueries()))
                        .items(resource.getItems())
                        .resourceSchema(resource.getResourceSchema())
                        .subresources(resource.getSubresources())
                        .parameters(asList(resource.getParameters()))
                        .build());
            }
            basePath = pathBuilder.build();
        } else {
            basePath = versionedPath().put(UNVERSIONED, resource()
                    .mvccSupported(false)
                    .title(localizableString(uriTemplate, ".title"))
                    .description(localizableString(uriTemplate, ".description"))
                    .actions(GENERAL_ACTIONS).build()).build();
        }
        Paths.Builder paths = paths().put("", basePath);
        for (String path : api.getPaths().getNames()) {
            if (!path.equals("")) {
                paths.put(path, api.getPaths().get(path));
            }
        }
        Definitions definitions = api.getDefinitions();
        if (definitions == null || !definitions.getNames().contains("restsms.type")) {
            Definitions.Builder builder = definitions().put("restsms.type", TYPE_SCHEMA);
            if (definitions != null) {
                for (String name : definitions.getNames()) {
                    builder.put(name, definitions.get(name));
                }
            }
            definitions = builder.build();
        }
        return apiDescription()
                .definitions(definitions)
                .description(api.getDescription())
                .errors(api.getErrors())
                .id(api.getId())
                .version(api.getVersion())
                .services(api.getServices())
                .paths(paths.build())
                .build();
    }

    private LocalizableString localizableString(String uriTemplate, String suffix) {
        String key = uriTemplate.replace("/", ".").replaceAll("[{}]", "");
        if (!key.startsWith(".")) {
            key = "." + key;
        }
        if (key.endsWith(".")) {
            key = key.substring(0, key.length() - 1);
        }
        return new LocalizableString("i18n:api-descriptor/SmsRequestHandler#paths" + key + suffix, CLASS_LOADER);
    }

    @Override
    public ApiDescription handleApiRequest(Context context, Request request) {
        return router.handleApiRequest(context, request);
    }

    @Override
    public void addDescriptorListener(Listener listener) {
        router.addDescriptorListener(listener);
    }

    @Override
    public void removeDescriptorListener(Listener listener) {
        router.removeDescriptorListener(listener);
    }

    private static class ChildQueryResourceHandler implements QueryResourceHandler {
        private final ResourcePath subPath;
        private final JsonValue response;

        private ChildQueryResourceHandler(ResourcePath subPath, JsonValue response) {
            this.subPath = subPath;
            this.response = response;
        }

        public boolean handleResource(ResourceResponse resource) {
            response.add(resource.getContent().getObject());
            return true;
        }
    }

    /**
     * Returns the resource path of this SMS RouteTree
     *
     * @return The resource path
     */
    public ResourcePath getPath() {
        return  path;
    }

    /**
     * A matching pair for an {@code SmsRouteTree} route that consists of the tree routed to, and the route matcher
     * that leads there.
     */
    public static class Route {
        /** The matcher that leads to the tree. */
        public final RouteMatcher<Request> matcher;
        /** The tree that contains this route. */
        public final SmsRouteTree tree;
        /** The target handler for the route. */
        public final RequestHandler target;

        private Route(RouteMatcher<Request> matcher, SmsRouteTree tree, RequestHandler target) {
            this.matcher = matcher;
            this.tree = tree;
            this.target = target;
        }
    }

    private interface ChildTypePredicate {
        boolean apply(JsonValue type, Context context, RequestHandler handler) throws ResourceException;
    }

    private static final ChildTypePredicate ALL_CHILD_TYPES = new ChildTypePredicate() {
        public boolean apply(JsonValue type, Context context, RequestHandler handler) {
            return true;
        }
    };

    private static final ChildTypePredicate NOT_CREATED_SINGLETONS =
            new ChildTypePredicate() {
                @Override
                public boolean apply(JsonValue type, Context context, RequestHandler handler) throws ResourceException {
                    if (!type.get("collection").asBoolean()) {
                        try {
                            final ResourceResponse response = handler.handleRead(context,
                                    newReadRequest(empty())).getOrThrowUninterruptibly();

                            final JsonValue dynamicAttribute = response.getContent().get("dynamic");
                            if (dynamicAttribute.isNotNull()) {
                                return dynamicAttribute.asMap().isEmpty();
                            }
                            return false;
                        } catch (ResourceException e) {
                            if (e.getCode() == NOT_FOUND) {
                                return true;
                            }
                            throw e;
                        }
                    }
                    return true;
                }
            };
}
