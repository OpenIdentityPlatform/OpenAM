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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.rest.resource;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.RouterContext;
import org.forgerock.json.resource.RoutingMode;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.router.RestRealmValidator;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 * Contains the CREST Connection Factory for all of OpenAMs CREST resource endpoints.
 *
 * @since 12.0.0
 */
@Singleton
public class RealmRouterConnectionFactory {

    public static final String CONNECTION_FACTORY_NAME = "CrestRealmRouter";

    private final Map<String, CollectionResourceProvider> collectionResourceEndpoints;
    private final Map<String, SingletonResourceProvider> singletonResourceEndpoints;
    private final RestRealmValidator realmValidator;

    /**
     * Constructs a new instance of the RealmRouteConnectionFactory.
     *
     * @param collectionResourceEndpoints The Map of CollectionResourceProvider endpoints.
     * @param singletonResourceEndpoints The Map of SingletonResourceProvider endpoints.
     * @param realmValidator An instance of the RestRealmValidator.
     */
    @Inject
    public RealmRouterConnectionFactory(final Map<String, CollectionResourceProvider> collectionResourceEndpoints,
            final Map<String, SingletonResourceProvider> singletonResourceEndpoints,
            final RestRealmValidator realmValidator) {
        this.collectionResourceEndpoints = collectionResourceEndpoints;
        this.singletonResourceEndpoints = singletonResourceEndpoints;
        this.realmValidator = realmValidator;
    }

    /**
     * Creates a dynamic Router for the given realm.
     *
     * @param realm The realm.
     * @return A RequestHandler.
     */
    public RequestHandler realmRouter(final String realm) {
        Router router = new Router();
        for (final Map.Entry<String, CollectionResourceProvider> entry : collectionResourceEndpoints.entrySet()) {
            router.addRoute(entry.getKey(), realmCollection(entry.getValue(), realm));
        }
        for (final Map.Entry<String, SingletonResourceProvider> entry : singletonResourceEndpoints.entrySet()) {
            router.addRoute(entry.getKey(), realmSingleton(entry.getValue(), realm));
        }
        router.addRoute(RoutingMode.STARTS_WITH, "/{realm}", subrealm(realm));
        return router;
    }

    /**
     * Wraps the given CollectionResourceProvider so that a RealmContext can be created containing the given realm.
     *
     * @param provider The CollectionResourceProvider that will actually handle the request.
     * @param realm The realm.
     * @return A CollectionResourceProvider instance.
     */
    private CollectionResourceProvider realmCollection(final CollectionResourceProvider provider, final String realm) {
        return new CollectionResourceProvider() {

            /**
             * {@inheritDoc}
             */
            public void actionCollection(ServerContext context, ActionRequest request,
                    ResultHandler<JsonValue> handler) {
                provider.actionCollection(realmContext(context, realm), request, handler);
            }

            /**
             * {@inheritDoc}
             */
            public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
                    ResultHandler<JsonValue> handler) {
                provider.actionInstance(realmContext(context, realm), resourceId, request, handler);
            }

            /**
             * {@inheritDoc}
             */
            public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
                provider.createInstance(realmContext(context, realm), request, handler);
            }

            /**
             * {@inheritDoc}
             */
            public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
                    ResultHandler<Resource> handler) {
                provider.deleteInstance(realmContext(context, realm), resourceId, request, handler);
            }

            /**
             * {@inheritDoc}
             */
            public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
                    ResultHandler<Resource> handler) {
                provider.patchInstance(realmContext(context, realm), resourceId, request, handler);
            }

            /**
             * {@inheritDoc}
             */
            public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
                provider.queryCollection(realmContext(context, realm), request, handler);
            }

            /**
             * {@inheritDoc}
             */
            public void readInstance(ServerContext context, String resourceId, ReadRequest request,
                    ResultHandler<Resource> handler) {
                provider.readInstance(realmContext(context, realm), resourceId, request, handler);
            }

            /**
             * {@inheritDoc}
             */
            public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
                    ResultHandler<Resource> handler) {
                provider.updateInstance(realmContext(context, realm), resourceId, request, handler);
            }
        };
    }

    /**
     * Creates a {@link RealmContext} wrapper around the given parent context and realm name. Also wraps the parent
     * context in an {@link SSOTokenContext} to provide convenient access to the caller subject if required.
     *
     * @param parent the parent realm.
     * @param realm the realm name.
     * @return an appropriate realm context.
     */
    private RealmContext realmContext(ServerContext parent, String realm) {
        return new RealmContext(new SSOTokenContext(parent), realm);
    }

    /**
     * Wraps the given SingletonResourceProvider so that a RealmContext can be created containing the given realm.
     *
     * @param provider The SingletonResourceProvider that will actually handle the request.
     * @param realm The realm.
     * @return A SingletonResourceProvider instance.
     */
    private SingletonResourceProvider realmSingleton(final SingletonResourceProvider provider, final String realm) {
        return new SingletonResourceProvider() {

            /**
             * {@inheritDoc}
             */
            public void actionInstance(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
                provider.actionInstance(realmContext(context, realm), request, handler);
            }

            /**
             * {@inheritDoc}
             */
            public void patchInstance(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
                provider.patchInstance(realmContext(context, realm), request, handler);
            }

            /**
             * {@inheritDoc}
             */
            public void readInstance(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
                provider.readInstance(realmContext(context, realm), request, handler);
            }

            /**
             * {@inheritDoc}
             */
            public void updateInstance(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
                provider.updateInstance(realmContext(context, realm), request, handler);
            }
        };
    }

    /**
     * Creates a RequestHandler for the sub-realms of the given parent realm.
     *
     * @param parentRealm The parent realm.
     * @return A RequestHandler instance.
     */
    private RequestHandler subrealm(final String parentRealm) {
        return new RequestHandler() {

            /**
             * {@inheritDoc}
             */
            public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
                try {
                    subrealm(parentRealm, context).handleAction(context, request, handler);
                } catch (ResourceException e) {
                    handler.handleError(e);
                }
            }
            /**
             * {@inheritDoc}
             */
            public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
                try {
                    subrealm(parentRealm, context).handleCreate(context, request, handler);
                } catch (ResourceException e) {
                    handler.handleError(e);
                }
            }

            /**
             * {@inheritDoc}
             */
            public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
                try {
                    subrealm(parentRealm, context).handleDelete(context, request, handler);
                } catch (ResourceException e) {
                    handler.handleError(e);
                }
            }

            /**
             * {@inheritDoc}
             */
            public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
                try {
                    subrealm(parentRealm, context).handlePatch(context, request, handler);
                } catch (ResourceException e) {
                    handler.handleError(e);
                }
            }

            /**
             * {@inheritDoc}
             */
            public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
                try {
                    subrealm(parentRealm, context).handleQuery(context, request, handler);
                } catch (ResourceException e) {
                    handler.handleError(e);
                }
            }

            /**
             * {@inheritDoc}
             */
            public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
                try {
                    subrealm(parentRealm, context).handleRead(context, request, handler);
                } catch (ResourceException e) {
                    handler.handleError(e);
                }
            }

            /**
             * {@inheritDoc}
             */
            public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
                try {
                    subrealm(parentRealm, context).handleUpdate(context, request, handler);
                } catch (ResourceException e) {
                    handler.handleError(e);
                }
            }

            /**
             * Gets the realm part that was matched whilst routing and concatenates in to the current realm, verifies
             * that the realm exists and is valid and then creates a Router for the realm.
             *
             * @param parentRealm The parent realm.
             * @param context The ServerContext.
             * @return A RequestHandler instance.
             * @throws ResourceException If the realm to which the request is being routed does not exist or is not
             *                           valid.
             */
            private RequestHandler subrealm(final String parentRealm, final ServerContext context)
                    throws ResourceException {
                final String matchedRealm = context.asContext(RouterContext.class).getUriTemplateVariables()
                        .get("realm");
                final String realm = parentRealm + "/" + matchedRealm;

                // Check that the path references an existing realm
                if (!realmValidator.isRealm(realm)) {
                    throw ResourceException.getException(ResourceException.BAD_REQUEST, "Invalid realm, " + realm);
                }

                return realmRouter(realm);
            }
        };
    }
}
