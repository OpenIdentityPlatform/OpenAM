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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.forgerock.services.context.Context;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * A method annotated with one of {@link Get}, {@link Post}, {@link Put} or {@link Delete}. This class
 * works out what parameters are going to need to be passed to the annotated method, and creates
 * necessary functions for translating at reflection time so that reflection is not done at request
 * time.
 * <p>
 * This class is expected to only be used from the {@link Endpoints} class.
 * @since 13.0.0
 */
public class AnnotatedMethod {
    private final Object requestHandler;
    private final Method method;
    private final int requestParameter;
    private final int numberOfParameters;
    private final String operation;
    private final Function<Object, Promise<Response, NeverThrowsException>, NeverThrowsException> responseAdapter;
    private final List<ContextParameter> contextParameters;

    AnnotatedMethod(String operation, Object requestHandler, Method method, List<ContextParameter> contextParameters,
            int requestParameter, int numberOfParameters,
            Function<Object, Promise<Response, NeverThrowsException>, NeverThrowsException> responseAdapter) {
        this.operation = operation;
        this.requestHandler = requestHandler;
        this.method = method;
        this.contextParameters = contextParameters;
        this.requestParameter = requestParameter;
        this.numberOfParameters = numberOfParameters;
        this.responseAdapter = responseAdapter;
    }

    Promise<Response, NeverThrowsException> invoke(Context context, Request request) {
        if (method == null) {
            Object entity = ResourceException.getException(Status.METHOD_NOT_ALLOWED.getCode(),
                    Status.METHOD_NOT_ALLOWED.getReasonPhrase()).toJsonValue().getObject();
            return newResultPromise(createErrorResponse(Status.METHOD_NOT_ALLOWED, entity));
        }
        Object[] args = new Object[numberOfParameters];
        if (requestParameter > -1) {
            args[requestParameter] = request;
        }
        for (ContextParameter parameter : contextParameters) {
            args[parameter.index] = parameter.getContext(context);
        }
        try {
            Object result = method.invoke(requestHandler, args);
            return responseAdapter.apply(result);
        } catch (IllegalAccessException e) {
            return newResultPromise(new Response().setStatus(Status.INTERNAL_SERVER_ERROR)
                    .setCause(new IllegalStateException("Cannot access the annotated method: " + method.getName(), e)));
        } catch (InvocationTargetException e) {
            return newResultPromise(new Response().setStatus(Status.INTERNAL_SERVER_ERROR)
                    .setCause(new IllegalStateException("Exception from invocation should be handled by promise", e)));
        }
    }

    private Response createErrorResponse(Status status, Object entity) {
        return new Response().setStatus(status).setEntity(entity);
    }

    static AnnotatedMethod findMethod(Object requestHandler, Class<? extends Annotation> annotation) {
        for (Method method : requestHandler.getClass().getMethods()) {
            if (method.getAnnotation(annotation) != null) {
                AnnotatedMethod checked = checkMethod(annotation, requestHandler, method);
                if (checked != null) {
                    return checked;
                }
            }
        }
        for (Method method : requestHandler.getClass().getMethods()) {
            String crestVerb = annotation.getSimpleName().toLowerCase();
            String methodName = method.getName();
            if (methodName.equals(crestVerb)) {
                AnnotatedMethod checked = checkMethod(annotation, requestHandler, method);
                if (checked != null) {
                    return checked;
                }
            }
        }
        return new AnnotatedMethod(annotation.getSimpleName(), null, null, null, -1, -1, null);
    }

    static AnnotatedMethod checkMethod(Class<?> annotation, Object requestHandler, Method method) {
        List<ContextParameter> contextParams = new ArrayList<>();
        int requestParam = -1;
        for (int i = 0; i < method.getParameterTypes().length; i++) {
            Class<?> type = method.getParameterTypes()[i];
            for (Annotation paramAnnotation : method.getParameterAnnotations()[i]) {
                if (paramAnnotation instanceof Contextual) {
                    if (Context.class.isAssignableFrom(type)) {
                        contextParams.add(new ContextParameter(i, (Class<? extends Context>) type));
                    } else if (Request.class.isAssignableFrom(type)) {
                        requestParam = i;
                    }
                }
            }
        }
        Function<Object, Promise<Response, NeverThrowsException>, NeverThrowsException> resourceCreator;
        if (Promise.class.equals(method.getReturnType())) {
            resourceCreator = new PromisedResponseCreator();
        } else if (Response.class.equals(method.getReturnType())) {
            resourceCreator = new Function<Object, Promise<Response, NeverThrowsException>, NeverThrowsException>() {
                @Override
                public Promise<Response, NeverThrowsException> apply(Object o) {
                    return newResultPromise((Response) o);
                }
            };
        } else {
            resourceCreator = ResponseCreator.forType(method.getReturnType());
        }
        return new AnnotatedMethod(annotation.getSimpleName(), requestHandler, method, contextParams,
                requestParam, method.getParameterTypes().length, resourceCreator);
    }

    /**
     * A function to create a {@link Response} from the generic type of a method that produces response
     * content.
     */
    private final static class ResponseCreator
            implements Function<Object, Promise<Response, NeverThrowsException>, NeverThrowsException> {

        private final Function<Object, Object, NeverThrowsException> entityConverter;

        private ResponseCreator(Function<Object, Object, NeverThrowsException> entityConverter) {
            this.entityConverter = entityConverter;
        }

        @Override
        public Promise<Response, NeverThrowsException> apply(Object o) {
            Object content = entityConverter.apply(o);
            return newResultPromise(
                    new Response()
                            .setEntity(content)
                            .setStatus(content == null ? Status.NO_CONTENT : Status.OK));
        }

        /**
         * Uses reflection to deduce the need for a {@code ResourceCreator}, and return an appropriately created one
         * if needed.
         * @param type The type for the {@code Response} entity.
         * @return A new function.
         */
        private static Function<Object, Promise<Response, NeverThrowsException>, NeverThrowsException> forType(
                Class<?> type) {
            if (String.class.equals(type) || Void.class.equals(type) || byte[].class.equals(type)) {
                return new ResponseCreator(IDENTITY_FUNCTION);
            } else if (JsonValue.class.equals(type)) {
                return new ResponseCreator(new Function<Object, Object, NeverThrowsException>() {
                    @Override
                    public Object apply(Object o) {
                        return ((JsonValue) o).getObject();
                    }
                });
            }
            throw new IllegalArgumentException("Unsupported response type: " + type);
        }

    }

    private static class PromisedResponseCreator
            implements Function<Object, Promise<Response, NeverThrowsException>, NeverThrowsException> {

        @Override
        public Promise<Response, NeverThrowsException> apply(Object o) {
            throw new UnsupportedOperationException("to be implemented");
        }
    }

    //@Checkstyle:off
    private static final Function<Object, Object, NeverThrowsException> IDENTITY_FUNCTION =
            new Function<Object, Object, NeverThrowsException>() {
        @Override
        public Object apply(Object o) {
            return o;
        }
    };
    //@Checkstyle:on

    private final static class ContextParameter {
        private final int index;
        private final Class<? extends Context> type;

        private ContextParameter(int index, Class<? extends Context> type) {
            this.index = index;
            this.type = type.equals(Context.class) ? null : type;
        }

        private Context getContext(Context context) {
            return type == null ? context : context.asContext(type);
        }
    }
}
