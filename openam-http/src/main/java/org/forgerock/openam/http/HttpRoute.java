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

package org.forgerock.openam.http;

import javax.inject.Provider;
import java.lang.annotation.Annotation;

import com.google.inject.Key;
import org.forgerock.services.context.Context;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * Encapsulates a HTTP route that will handle incoming HTTP requests which can
 * be bound in the Guice injector so it can be registered on the HTTP router.
 *
 * @since 13.0.0
 */
public final class HttpRoute {

    private final RoutingMode mode;
    private final String uriTemplate;
    private final Provider<Handler> handler;

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
        return newHttpRoute(mode, uriTemplate, new Provider<Handler>() {
            @Override
            public Handler get() {
                return handler;
            }
        });
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
        return newHttpRoute(mode, uriTemplate, new Provider<Handler>() {
            @Override
            public Handler get() {
                return new HandlerProvider(key);
            }
        });
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
        return new HttpRoute(mode, uriTemplate, provider);
    }

    private HttpRoute(RoutingMode mode, String uriTemplate, Provider<Handler> handler) {
        this.mode = mode;
        this.uriTemplate = uriTemplate;
        this.handler = handler;
    }

    RoutingMode getMode() {
        return mode;
    }

    String getUriTemplate() {
        return uriTemplate;
    }

    Handler getHandler() {
        return new Handler() {
            @Override
            public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
                return handler.get().handle(context, request);
            }
        };
    }
}
