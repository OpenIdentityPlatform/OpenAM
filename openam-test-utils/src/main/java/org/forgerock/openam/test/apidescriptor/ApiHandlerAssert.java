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

import static java.util.Collections.singletonList;
import static org.forgerock.openam.test.apidescriptor.ApiAssertions.assertI18nDescription;
import static org.forgerock.openam.test.apidescriptor.ApiAssertions.assertI18nTitle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.AbstractAssert;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Schema;

/**
 * This class represents the {@link Handler} annotation.
 *
 * @since 14.0.0
 */
public final class ApiHandlerAssert extends AbstractAssert<ApiHandlerAssert, Handler> {

    private final Class<?> annotatedClass;

    ApiHandlerAssert(Class<?> annotatedClass, Handler actual) {
        super(actual, ApiHandlerAssert.class);
        this.annotatedClass = annotatedClass;
    }

    /**
     * Assert that all titles use i18n and that the keys have valid entries in the specifies resource bundle.
     *
     * @return An instance of {@link ApiHandlerAssert}.
     */
    public ApiHandlerAssert hasI18nTitle() {
        assertI18nTitle(actual.title(), annotatedClass);
        return this;
    }

    /**
     * Assert that all descriptions use i18n and that the keys have valid entries in the specifies resource bundle.
     *
     * @return An instance of {@link ApiHandlerAssert}.
     */
    public ApiHandlerAssert hasI18nDescription() {
        assertI18nDescription(actual.description(), annotatedClass);
        return this;
    }

    /**
     * Get the test representative of {@link Schema}s in the annotated handler.
     *
     * @return The {@link ApiSchemaAssert} containing the {@link Schema}s.
     */
    public ApiSchemaAssert schemas() {
        return new ApiSchemaAssert(annotatedClass, singletonList(actual.resourceSchema()));
    }

    /**
     * Get the test representative of {@link Parameter}s in the annotated handler.
     *
     * @return The {@link ApiParameterAssert} containing the {@link Parameter}s.
     */
    public ApiParameterAssert parameters() {
        List<Parameter> parameters = new ArrayList<>();
        parameters.addAll(Arrays.asList(actual.parameters()));
        return new ApiParameterAssert(annotatedClass, parameters);
    }
}
