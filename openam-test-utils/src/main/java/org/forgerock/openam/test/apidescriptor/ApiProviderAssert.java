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
package org.forgerock.openam.test.apidescriptor;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.AbstractAssert;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.RequestHandler;
import org.forgerock.api.annotations.SingletonProvider;
import org.forgerock.openam.utils.StringUtils;

/**
 * This class represents the {@link SingletonProvider}, {@link CollectionProvider} and {@link RequestHandler}
 * annotations as generic {@link Annotation}s.
 *
 * @since 14.0.0
 */
public final class ApiProviderAssert extends AbstractAssert<ApiProviderAssert, Annotation> {

    private final Class<?> annotatedClass;

    ApiProviderAssert(Class<?> annotatedClass, Annotation actual) {
        super(actual, ApiProviderAssert.class);
        this.annotatedClass = annotatedClass;
    }

    /**
     * Get the test representative of {@link SingletonProvider}, {@link CollectionProvider} or {@link RequestHandler}
     * in the annotated class.
     *
     * @return The {@link ApiHandlerAssert} containing the provider.
     */
    public ApiHandlerAssert handler() {
        Handler handler = null;
        if (actual instanceof SingletonProvider) {
            handler = ((SingletonProvider) actual).value();
        } else if (actual instanceof CollectionProvider) {
            handler = ((CollectionProvider) actual).details();
        } else if (actual instanceof RequestHandler) {
            handler = ((RequestHandler) actual).value();
        }
        if (handler == null) {
            failWithMessage("Handler should not be null");
        }
        return new ApiHandlerAssert(annotatedClass, handler);
    }

    /**
     * Get the test representative of {@link Parameter}s in the annotated provider.
     *
     * @return The {@link ApiParameterAssert} containing the {@link Parameter}s.
     */
    public ApiParameterAssert parameters() {
        List<Parameter> parameters = new ArrayList<>();
        if (actual instanceof CollectionProvider) {
            Parameter parameter = ((CollectionProvider) actual).pathParam();
            if (StringUtils.isNotEmpty(parameter.description())) {
                parameters.add(parameter);
            }
        }
        return new ApiParameterAssert(annotatedClass, parameters);
    }
}
