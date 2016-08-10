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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.AbstractListAssert;
import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Schema;

/**
 * This class represents the {@link org.forgerock.api.annotations.Create}, {@link org.forgerock.api.annotations.Update},
 * {@link org.forgerock.api.annotations.Delete}, {@link org.forgerock.api.annotations.Patch},
 * {@link org.forgerock.api.annotations.Action} and {@link org.forgerock.api.annotations.Query} annotations
 * as generic {@link Annotation}s.
 *
 * @since 14.0.0
 */
public final class ApiActionAssert extends AbstractListAssert<ApiActionAssert, List<Annotation>, Annotation> {

    private final Class<?> annotatedClass;

    ApiActionAssert(Class<?> annotatedClass, List<Annotation> actual) {
        super(actual, ApiActionAssert.class);
        this.annotatedClass = annotatedClass;
    }

    /**
     * Get the test representative of {@link Operation}s in the annotated method.
     *
     * @return The {@link ApiOperationsAssert} containing the {@link Operation}s.
     */
    public ApiOperationsAssert operations() {
        List<Operation> operations = new ArrayList<>();
        for (Annotation annotation : actual) {
            try {
                Method method = annotation.getClass().getMethod("operationDescription");
                operations.add((Operation) method.invoke(annotation));
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        return new ApiOperationsAssert(annotatedClass, operations);
    }

    /**
     * Get the test representative of {@link Schema}s in the annotated method.
     *
     * @return The {@link ApiSchemaAssert} containing the {@link Schema}s.
     */
    public ApiSchemaAssert schemas() {
        List<Schema> schemas = new ArrayList<>();
        for (Annotation annotation : actual) {
            if (annotation instanceof Action) {
                Action action = (Action) annotation;
                schemas.add(action.request());
                schemas.add(action.response());
            }
        }
        return new ApiSchemaAssert(annotatedClass, schemas);
    }
}
