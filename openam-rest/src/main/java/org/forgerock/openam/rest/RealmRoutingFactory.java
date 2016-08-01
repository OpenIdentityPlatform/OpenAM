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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.rest;

import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;

import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.RouteMatchers;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.core.realms.NoRealmFoundException;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * Factory for providing realm routers for CHF and CREST.
 *
 * @since 14.0.0
 */
public class RealmRoutingFactory {

    public static final String REALM_ROUTE = "realms/{realm}";

    /**
     * Creates a CHF {@link Filter} for resolving the hostname to a realm.
     *
     * @return A {@code Filter}.
     */
    public Filter createHostnameFilter() {
        return new HostnameFilter();
    }

    /**
     * Creates a CHF {@link Handler} for recursively matching the {@literal /realms/{realm}} route.
     *
     * <p>This handler MUST be registered at the route {@literal /realms/{realm}}.</p>
     *
     * @param next The next {@code Handler} that the realm handler should route to after parsing
     * the realm from the URI.
     * @return A {@code Handler}.
     */
    public Handler createRouter(Handler next) {
        return new ChfRealmRouter(next);
    }

    /**
     * Creates a CREST {@link RequestHandler} for recursivelt matching the
     * {@literal /realms/{realm}} route.
     *
     * <p>This request handler MUST be registered at the route {@literal /realms/{realm}}.</p>
     *
     * @param next The next {@code RequestHandler} that the realm handler should route to after
     * parsing the realm from the URI.
     * @return A {@code RequestHandler}.
     */
    public RequestHandler createRouter(RequestHandler next) {
        return new CrestRealmRouter(next);
    }

    private static final class HostnameFilter implements Filter {

        @Override
        public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler handler) {
            try {
                return handler.handle(new RealmContext(context, Realm.of(request.getUri().getHost())), request);
            } catch (RealmLookupException e) {
                return Promises.newResultPromise(new Response(Status.BAD_REQUEST)
                        .setEntity(new BadRequestException("Realm \"" + e.getRealm() + "\" not found", e)
                                .toJsonValue().getObject()));
            }
        }
    }

    private static final class ChfRealmRouter implements Handler {

        private final org.forgerock.http.routing.Router router;

        private ChfRealmRouter(Handler next) {
            this.router = new org.forgerock.http.routing.Router();
            router.addRoute(org.forgerock.http.routing.RouteMatchers.requestUriMatcher(STARTS_WITH, REALM_ROUTE),
                    this);
            router.setDefaultRoute(next);
        }

        @Override
        public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
            try {
                return router.handle(new RealmContext(context, getRealm(context)), request);
            } catch (RealmLookupException e) {
                return Promises.newResultPromise(new Response(Status.NOT_FOUND)
                        .setEntity(new NotFoundException("Realm \"" + e.getRealm() + "\" not found", e)
                                .toJsonValue().getObject()));
            }
        }
    }

    private static final class CrestRealmRouter implements RequestHandler {

        private final Router router;

        public CrestRealmRouter(RequestHandler next) {
            this.router = new Router();
            router.addRoute(RouteMatchers.requestUriMatcher(STARTS_WITH, REALM_ROUTE), this);
            router.setDefaultRoute(next);
        }

        @Override
        public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
            try {
                return router.handleAction(new RealmContext(context, getRealm(context)), request);
            } catch (RealmLookupException e) {
                return new NotFoundException("Realm \"" + e.getRealm() + "\" not found").asPromise();
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
            try {
                return router.handleCreate(new RealmContext(context, getRealm(context)), request);
            } catch (RealmLookupException e) {
                return new NotFoundException("Realm \"" + e.getRealm() + "\" not found").asPromise();
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
            try {
                return router.handleDelete(new RealmContext(context, getRealm(context)), request);
            } catch (RealmLookupException e) {
                return new NotFoundException("Realm \"" + e.getRealm() + "\" not found").asPromise();
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
            try {
                return router.handlePatch(new RealmContext(context, getRealm(context)), request);
            } catch (RealmLookupException e) {
                return new NotFoundException("Realm \"" + e.getRealm() + "\" not found").asPromise();
            }
        }

        @Override
        public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
                QueryResourceHandler resourceHandler) {
            try {
                return router.handleQuery(new RealmContext(context, getRealm(context)), request, resourceHandler);
            } catch (RealmLookupException e) {
                return new NotFoundException("Realm \"" + e.getRealm() + "\" not found").asPromise();
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
            try {
                return router.handleRead(new RealmContext(context, getRealm(context)), request);
            } catch (RealmLookupException e) {
                return new NotFoundException("Realm \"" + e.getRealm() + "\" not found").asPromise();
            }
        }

        @Override
        public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
            try {
                return router.handleUpdate(new RealmContext(context, getRealm(context)), request);
            } catch (RealmLookupException e) {
                return new NotFoundException("Realm \"" + e.getRealm() + "\" not found").asPromise();
            }
        }
    }

    private static Realm getRealm(Context context) throws RealmLookupException {
        String realmPathElement = context.asContext(UriRouterContext.class).getUriTemplateVariables().get("realm");
        if ("root".equalsIgnoreCase(realmPathElement)) {
            return Realm.root();
        } else if (context.containsContext(RealmContext.class)) {
            Realm currentRealm = context.asContext(RealmContext.class).getRealm();
            return Realm.of(currentRealm, realmPathElement);
        } else {
            throw new NoRealmFoundException(realmPathElement);
        }
    }
}
