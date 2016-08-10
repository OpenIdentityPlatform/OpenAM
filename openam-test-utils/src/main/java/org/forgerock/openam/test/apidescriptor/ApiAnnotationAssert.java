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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.AbstractAssert;
import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.Actions;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Create;
import org.forgerock.api.annotations.Delete;
import org.forgerock.api.annotations.Patch;
import org.forgerock.api.annotations.Query;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.RequestHandler;
import org.forgerock.api.annotations.SingletonProvider;
import org.forgerock.api.annotations.Update;

/**
 * This is the entry test class for API annotated classes. It will assert that a class was correctly annotated and that
 * it conforms to the OpenAM conventions. The convenience method {@link ApiAnnotationAssert#hasValidAnnotations()} will
 * do all assertions necessary to validate an annotated class.
 *
 * @since 14.0.0
 */
public final class ApiAnnotationAssert extends AbstractAssert<ApiAnnotationAssert, Class> {

    private ApiAnnotationAssert(Class actual) {
        super(actual, ApiAnnotationAssert.class);
    }

    /**
     * Get an instance of the assertion class for the given API annotated class.
     *
     * @param value The API annotated class.
     * @return The assertion instance.
     */
    public static ApiAnnotationAssert assertThat(Class value) {
        return new ApiAnnotationAssert(value);
    }

    /**
     * Get the test representative of {@link SingletonProvider}s, {@link CollectionProvider}s and
     * {@link RequestHandler}s in the annotated class.
     *
     * @return The {@link ApiProviderAssert} containing the provider annotation.
     */
    public ApiProviderAssert classAnnotation() {
        Annotation[] annotations = actual.getAnnotations();
        Annotation classAnnotation = null;
        for (Annotation annotation : annotations) {
            if (annotation instanceof SingletonProvider
                    || annotation instanceof CollectionProvider
                    || annotation instanceof RequestHandler) {
                classAnnotation = annotation;
                break;
            }
        }
        if (classAnnotation == null) {
            failWithMessage(
                    "Expected %s to be annotated with one of SingletonProvider, CollectionProvider or RequestHandler",
                    actual.getSimpleName());
        }
        return new ApiProviderAssert(actual, classAnnotation);
    }

    /**
     * Get the test representative of {@link Create}, {@link Update}, {@link Delete}, {@link Patch}, {@link Action},
     * {@link Query}) as a generic {@link Annotation} in the annotated class.
     *
     * @return The {@link ApiActionAssert} containing the method annotations.
     */
    public ApiActionAssert methodAnnotations() {
        List<Annotation> apiAnnotations = new ArrayList<>();
        Method[] methods = actual.getMethods();
        for (Method method : methods) {
            Annotation[] annotations = method.getAnnotations();
            for (Annotation annotation : annotations) {
                apiAnnotations.addAll(getApiAnnotation(annotation));
            }
        }
        return new ApiActionAssert(actual, apiAnnotations);
    }

    private List<Annotation> getApiAnnotation(Annotation annotation) {
        if (annotation instanceof Create
                || annotation instanceof Read
                || annotation instanceof Update
                || annotation instanceof Delete
                || annotation instanceof Patch
                || annotation instanceof Action
                || annotation instanceof Query) {
            return singletonList(annotation);
        } else if (annotation instanceof Actions) {
            Action[] actions = ((Actions) annotation).value();
            return Arrays.<Annotation>asList(actions);
        }
        return emptyList();
    }

    /**
     * This convenience method will do all assertions necessary to validate that an annotated class was correctly
     * annotated and that it conforms to the OpenAM conventions.
     */
    public void hasValidAnnotations() {
        assertThatAnnotatedClassHasValidI18n();
        assertThatAnnotatedClassHasAnnotatedMethods();
        assertThatAllSchemasAreValid();
    }

    private void assertThatAnnotatedClassHasValidI18n() {
        assertThatClassAnnotationI18nIsValid();
        assertThatMethodAnnotationI18nIsValid();
    }

    private void assertThatClassAnnotationI18nIsValid() {
        ApiProviderAssert classAnnotation = classAnnotation();

        classAnnotation.handler()
                .hasI18nTitle()
                .hasI18nDescription();

        classAnnotation.parameters()
                .hasI18nDescriptions();

        classAnnotation.handler().schemas()
                .hasI18nTitles()
                .hasI18nDescriptions();

        classAnnotation.handler().parameters()
                .hasI18nDescriptions();
    }

    private void assertThatMethodAnnotationI18nIsValid() {
        ApiActionAssert methodAnnotations = methodAnnotations();

        methodAnnotations.operations()
                .hasI18nDescriptions();

        methodAnnotations.operations().errors()
                .hasI18nDescriptions();

        methodAnnotations.operations().errors().schemas()
                .hasI18nTitles()
                .hasI18nDescriptions();

        methodAnnotations.schemas()
                .hasI18nTitles()
                .hasI18nDescriptions();
    }

    private void assertThatAnnotatedClassHasAnnotatedMethods() {
        methodAnnotations()
                .isNotEmpty()
                .operations()
                .isNotEmpty();
    }

    private void assertThatAllSchemasAreValid() {
        classAnnotation().handler().schemas()
                .hasValidSchema();

        ApiActionAssert apiActionAssert = methodAnnotations();

        apiActionAssert.operations().errors().schemas()
                .hasValidSchema();

        apiActionAssert.schemas()
                .hasValidSchema();
    }

}
