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

import static org.forgerock.openam.test.apidescriptor.ApiAssertions.assertI18nDescription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.AbstractListAssert;
import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.Operation;

/**
 * This class represents the {@link Operation} annotation.
 *
 * @since 14.0.0
 */
public final class ApiOperationsAssert extends AbstractListAssert<ApiOperationsAssert, List<Operation>, Operation> {

    private final Class<?> annotatedClass;

    ApiOperationsAssert(Class<?> annotatedClass, List<Operation> actual) {
        super(actual, ApiOperationsAssert.class);
        this.annotatedClass = annotatedClass;
    }

    /**
     * Get the test representative of {@link ApiError}s in the annotated operation.
     *
     * @return The {@link ApiErrorAssert} containing the {@link ApiError}s.
     */
    public ApiErrorAssert errors() {
        List<ApiError> errors = new ArrayList<>();
        for (Operation operation : actual) {
            errors.addAll(Arrays.asList(operation.errors()));
        }

        return new ApiErrorAssert(annotatedClass, errors);
    }

    /**
     * Assert that all descriptions use i18n and that the keys have valid entries in the specifies resource bundle.
     *
     * @return An instance of {@link ApiOperationsAssert}.
     */
    public ApiOperationsAssert hasI18nDescriptions() {
        for (Operation operation : actual) {
            assertI18nDescription(operation.description(), annotatedClass);
        }
        return this;
    }
}
