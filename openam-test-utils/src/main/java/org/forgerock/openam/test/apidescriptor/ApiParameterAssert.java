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

import java.util.List;

import org.assertj.core.api.AbstractListAssert;
import org.forgerock.api.annotations.Parameter;

/**
 * This class represents the {@link Parameter} annotation.
 *
 * @since 14.0.0
 */
public final class ApiParameterAssert extends AbstractListAssert<ApiParameterAssert, List<Parameter>, Parameter> {

    private final Class<?> annotatedClass;

    ApiParameterAssert(Class<?> annotatedClass, List<Parameter> actual) {
        super(actual, ApiParameterAssert.class);
        this.annotatedClass = annotatedClass;
    }

    /**
     * Assert that all descriptions use i18n and that the keys have valid entries in the specifies resource bundle.
     *
     * @return An instance of {@link ApiParameterAssert}.
     */
    public ApiParameterAssert hasI18nDescriptions() {
        for (Parameter parameter : actual) {
            assertI18nDescription(parameter.description(), annotatedClass);
        }
        return this;
    }
}
