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

package org.forgerock.openam.http.annotations;

import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Key;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.services.context.Context;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * Convenience class for creating {@code Handler}s from classes that contain annotated methods
 * that handle requests.
 * @since 13.0.0
 */
public final class Endpoints {

    private static final String HEADER_X_HTTP_METHOD_OVERRIDE = "X-HTTP-Method-Override";

    /**
     * Produce a {@code Handler} from the annotated methods on the provided object.
     * <p>
     * This method currently only distinguishes requests by their method type. In future this
     * should be extended to support selection by request and response media types, and request
     * path.
     * @param obj The object containing annotated methods.
     * @return A new {@code Handler}.
     */
    public static Handler from(final Object obj) {
        final Map<String, AnnotatedMethod> methods = new HashMap<>();
        methods.put("DELETE", AnnotatedMethod.findMethod(obj, Delete.class));
        methods.put("GET", AnnotatedMethod.findMethod(obj, Get.class));
        methods.put("POST", AnnotatedMethod.findMethod(obj, Post.class));
        methods.put("PUT", AnnotatedMethod.findMethod(obj, Put.class));
        return new Handler() {
            @Override
            public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
                AnnotatedMethod method = methods.get(getMethod(request));
                if (method == null) {
                    Response response = new Response(Status.METHOD_NOT_ALLOWED);
                    response.setEntity(new NotSupportedException().toJsonValue().getObject());
                    return newResultPromise(response);
                }
                return method.invoke(context, request);
            }
        };
    }

    /**
     * Convenience method that produces a {@code Handler} using the {@link #from(Object)} method
     * and an object obtained from Guice.
     * @param cls The class to use.
     * @return A new {@code Handler}.
     */
    public static Handler from(Class<?> cls) {
        return from(Key.get(cls));
    }

    /**
     * Convenience method that produces a {@code Handler} using the {@link #from(Object)} method
     * and an object obtained from Guice.
     * @param key The Guice key to use.
     * @return A new {@code Handler}.
     */
    public static Handler from(Key key) {
        return from(InjectorHolder.getInstance(key));
    }

    /**
     * Returns the effective method name for an HTTP request taking into account
     * the "X-HTTP-Method-Override" header.
     *
     * @param req
     *            The HTTP request.
     * @return The effective method name.
     */
    private static String getMethod(org.forgerock.http.protocol.Request req) {
        String method = req.getMethod();
        if ("POST".equals(method)
                && req.getHeaders().getFirst(HEADER_X_HTTP_METHOD_OVERRIDE) != null) {
            method = req.getHeaders().getFirst(HEADER_X_HTTP_METHOD_OVERRIDE);
        }
        return method;
    }

    private Endpoints() {
    }
}
