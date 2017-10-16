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

package org.forgerock.openam.http;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

import com.google.inject.Key;

import com.google.common.reflect.TypeToken;
import org.forgerock.http.ApiProducer;
import org.forgerock.http.handler.DescribableHandler;
import org.forgerock.services.context.Context;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

import io.swagger.models.Swagger;

/**
 * Encapsulates a HTTP route that will handle incoming HTTP requests which can
 * be bound in the Guice injector so it can be registered on the HTTP router.
 *
 * @since 13.0.0
 */
public final class HttpRoute {

    /**
     * Creates a new {@code HttpRoute} for a route with the given {@code mode} and
     * {@code template}. The {@code Handler} for this route will be the provided
     * {@code handler} class.
     *
     * @param mode Indicates how the URI template should be matched against resource names.
     * @param uriTemplate The URI template which request resource names must match.
     * @param handler The handler to which matching requests will be routed.
     * @return The {@code HttpRoute}.
     */
    public static HttpRoute newHttpRoute(RoutingMode mode, String uriTemplate, final Handler handler) {
        Provider<Describable<Swagger, Request>> describableProvider = null;
        if (handler instanceof Describable) {
            describableProvider = new Provider<Describable<Swagger, Request>>() {
                @Override
                public Describable<Swagger, Request> get() {
                    return (Describable<Swagger, Request>) handler;
                }
            };
        }
        return new HttpRoute(mode, uriTemplate, new Provider<Handler>() {
            @Override
            public Handler get() {
                return handler;
            }
        }, describableProvider);
    }

    /**
     * Creates a new {@code HttpRoute} for a route with the given {@code mode} and
     * {@code template}. The {@code Handler} for this route will be retrieved from the Guice
     * injector using the base {@link Handler} class and the provided {@code annotation}.
     *
     * @param mode Indicates how the URI template should be matched against resource names.
     * @param uriTemplate The URI template which request resource names must match.
     * @param annotation The Guice binding annotation.
     * @return The {@code HttpRoute}.
     */
    public static HttpRoute newHttpRoute(RoutingMode mode, String uriTemplate, Annotation annotation) {
        return newHttpRoute(mode, uriTemplate, Key.get(Handler.class, annotation));
    }

    /**
     * Creates a new {@code HttpRoute} for a route with the given {@code mode} and
     * {@code template}. The {@code Handler} for this route will be retrieved from the Guice
     * injector using the provided {@code handler} class.
     *
     * @param mode Indicates how the URI template should be matched against resource names.
     * @param uriTemplate The URI template which request resource names must match.
     * @param handler The handler to which matching requests will be routed.
     * @return The {@code HttpRoute}.
     */
    public static HttpRoute newHttpRoute(RoutingMode mode, String uriTemplate, Class<? extends Handler> handler) {
        return newHttpRoute(mode, uriTemplate, Key.get(handler));
    }

    /**
     * Creates a new {@code HttpRoute} for a route with the given {@code mode} and
     * {@code template}. The {@code Handler} for this route will be retrieved from the Guice
     * injector using the provided {@code handler} class and {@code annotation}.
     *
     * @param mode Indicates how the URI template should be matched against resource names.
     * @param uriTemplate The URI template which request resource names must match.
     * @param key The Guice binding {@link Key}.
     * @return The {@code HttpRoute}.
     */
    public static HttpRoute newHttpRoute(RoutingMode mode, String uriTemplate, final Key<? extends Handler> key) {
        Provider<? extends Describable<Swagger, Request>> describableProvider = null;
        Provider<? extends Handler> handler;
        if (DescribableHandler.class.isAssignableFrom(key.getTypeLiteral().getRawType())) {
            Provider<DescribableHandler> provider = new Provider<DescribableHandler>() {
                @Override
                public DescribableHandler get() {
                    return new GuiceHandler(key);
                }
            };
            describableProvider = provider;
            handler = provider;
        } else {
            handler = new Provider<Handler>() {
                @Override
                public Handler get() {
                    return new GuiceHandler(key);
                }
            };
        }
        return new HttpRoute(mode, uriTemplate, handler, describableProvider);
    }

    /**
     * Creates a new {@code HttpRoute} for a route with the given {@code mode} and
     * {@code template}. The {@code Handler} for this route will be the provided
     * {@code handler} class.
     *
     * @param mode Indicates how the URI template should be matched against resource names.
     * @param uriTemplate The URI template which request resource names must match.
     * @param provider A {@link Provider} that returns the handler to which
     *                        matching requests will be routed.
     * @return The {@code HttpRoute}.
     */
    public static HttpRoute newHttpRoute(RoutingMode mode, String uriTemplate, final Provider<Handler> provider) {
        TypeToken type = TypeToken.of(provider.getClass());
        try {
            Class handlerType = type.resolveType(Provider.class.getMethod("get").getGenericReturnType()).getRawType();
            Provider<Describable<Swagger, Request>> describableProvider = null;
            if (Describable.class.isAssignableFrom(handlerType)) {
                describableProvider = new Provider<Describable<Swagger, Request>>() {
                    @Override
                    public Describable<Swagger, Request> get() {
                        return (Describable<Swagger, Request>) provider.get();
                    }
                };
            }
            return new HttpRoute(mode, uriTemplate, provider, describableProvider);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Provider must have a get method", e);
        }
    }

    private final RoutingMode mode;
    private final String uriTemplate;
    private final Provider<? extends Handler> handler;
    private final Provider<? extends Describable<Swagger, Request>> describable;

    private HttpRoute(RoutingMode mode, String uriTemplate, Provider<? extends Handler> handler,
            Provider<? extends Describable<Swagger, Request>> describable) {
        this.mode = mode;
        this.uriTemplate = uriTemplate;
        this.handler = handler;
        this.describable = describable;
    }

    RoutingMode getMode() {
        return mode;
    }

    String getUriTemplate() {
        return uriTemplate;
    }

    Handler getHandler() {
        return new DescribableHandler() {
            @Override
            public Swagger api(ApiProducer<Swagger> producer) {
                return describable != null ? describable.get().api(producer) : null;
            }

            @Override
            public Swagger handleApiRequest(Context context, Request request) {
                return describable != null ? describable.get().handleApiRequest(context, request) : null;
            }

            @Override
            public void addDescriptorListener(Listener listener) {
                if (describable != null) {
                    describable.get().addDescriptorListener(listener);
                }
            }

            @Override
            public void removeDescriptorListener(Listener listener) {
                if (describable != null) {
                    describable.get().removeDescriptorListener(listener);
                }
            }

            @Override
            public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
                return handler.get().handle(context, request);
            }
        };
    }
}
