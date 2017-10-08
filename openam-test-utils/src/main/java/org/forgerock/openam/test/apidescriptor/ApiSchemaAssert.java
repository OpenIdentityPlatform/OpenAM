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

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.test.apidescriptor.ApiAssertions.assertI18nDescription;
import static org.forgerock.openam.test.apidescriptor.ApiAssertions.assertI18nTitle;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.assertj.core.api.AbstractListAssert;
import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.annotations.Title;
import org.forgerock.api.jackson.JacksonUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;

/**
 * This class represents the {@link Schema} annotation.
 *
 * @since 14.0.0
 */
public final class ApiSchemaAssert extends AbstractListAssert<ApiSchemaAssert, List<Schema>, Schema> {

    private final TitleAssertor titleAssertor = new TitleAssertor();
    private final DescriptionAssertor descriptionAssertor = new DescriptionAssertor();
    private final List<Annotation> schemaAnnotations;
    private final List<String> schemaResources;
    private final Class<?> annotatedClass;

    ApiSchemaAssert(Class<?> annotatedClass, List<Schema> actual) {
        super(actual, ApiSchemaAssert.class);
        this.annotatedClass = annotatedClass;
        schemaAnnotations = new ArrayList<>();
        schemaResources = new ArrayList<>();

        for (Schema schema : actual) {
            Class<?> resourceClass = schema.fromType();
            if (!resourceClass.isAssignableFrom(Void.class)) {
                schemaAnnotations.addAll(getSchemaAnnotations(resourceClass));
            }
            String schemaResource = schema.schemaResource();
            if (!schemaResource.isEmpty()) {
                schemaResources.add(schemaResource);
            }
        }
    }

    /**
     * Assert that all titles use i18n and that the keys have valid entries in the specifies resource bundle.
     *
     * @return An instance of {@link ApiSchemaAssert}.
     */
    public ApiSchemaAssert hasI18nTitles() {
        for (Annotation annotation : schemaAnnotations) {
            if (annotation instanceof Title) {
                assertI18nTitle(((Title) annotation).value(), annotatedClass);
            }
        }
        for (String schemaResource : schemaResources) {
            assertSchemaResource(schemaResource, titleAssertor);
        }
        return this;
    }

    /**
     * Assert that all descriptions use i18n and that the keys have valid entries in the specifies resource bundle.
     *
     * @return An instance of {@link ApiSchemaAssert}.
     */
    public ApiSchemaAssert hasI18nDescriptions() {
        for (Annotation annotation : schemaAnnotations) {
            if (annotation instanceof Description) {
                assertI18nDescription(((Description) annotation).value(), annotatedClass);
            }
        }
        for (String schemaResource : schemaResources) {
            assertSchemaResource(schemaResource, descriptionAssertor);
        }
        return this;
    }

    /**
     * This convenience method will do all assertions necessary to validate that a resource schema is valid and that
     * it conforms to the OpenAM conventions.
     */
    public ApiSchemaAssert hasValidSchema() {
        for (String schemaResource : schemaResources) {
            assertSchemaResource(schemaResource, new PropertiesAssertor(schemaResource));
        }
        return this;
    }

    private void assertSchemaResource(String schemaResource, SchemaResourceAssertor assertor) {
        InputStream resource = annotatedClass.getResourceAsStream(schemaResource);
        try {
            json(JacksonUtils.OBJECT_MAPPER.readValue(resource, Object.class)).as(assertor);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read declared resource " + schemaResource, e);
        }
    }

    private List<Annotation> getSchemaAnnotations(Class<?> resourceClass) {
        List<Annotation> schemaAnnotations = new ArrayList<>();
        Annotation[] annotations = resourceClass.getAnnotations();
        schemaAnnotations.addAll(Arrays.asList(annotations));

        Field[] fields = resourceClass.getDeclaredFields();
        for (Field field : fields) {
            annotations = field.getAnnotations();
            schemaAnnotations.addAll(Arrays.asList(annotations));
        }

        Method[] methods = resourceClass.getMethods();
        for (Method method : methods) {
            annotations = method.getAnnotations();
            schemaAnnotations.addAll(Arrays.asList(annotations));
        }
        return schemaAnnotations;
    }

    private abstract class SchemaResourceAssertor implements Function<JsonValue, JsonValue, NeverThrowsException> {

        abstract String getName();

        Stack<Map<String, Object>> stack = new Stack<>();

        void assertValue(String value) {
            // implement in subclass
        }

        void assertValue(JsonValue value) {
            // implement in subclass
        }

        @Override
        public JsonValue apply(JsonValue value) {
            String name = value.getPointer().leaf();
            if (value.isCollection()) {
                for (JsonValue item : value) {
                    item.as(this);
                }
            } else if (value.isMap()) {
                if (stack.size() > 0) {
                    Map<String, Object> top = stack.peek();
                    if (name != null && name.equals(getName()) && top.containsKey("type") && top.get("type").equals("object")) {
                        assertValue(value);
                    }
                }
                stack.push(value.asMap());
                for (String key : value.keys()) {
                    value.get(key).as(this);
                }
                stack.pop();
            } else if (value.isString() && name.equals(getName())) {
                assertValue(value.asString());
            }
            return value;
        }
    }

    private class TitleAssertor extends SchemaResourceAssertor {

        @Override
        String getName() {
            return "title";
        }

        @Override
        void assertValue(String value) {
            assertI18nTitle(value, annotatedClass);
        }
    }

    private class DescriptionAssertor extends SchemaResourceAssertor {

        @Override
        String getName() {
            return "description";
        }

        @Override
        void assertValue(String value) {
            assertI18nDescription(value, annotatedClass);
        }
    }

    private class PropertiesAssertor extends SchemaResourceAssertor {

        private final String schemaResourceName;

        PropertiesAssertor(String schemaResourceName) {
            this.schemaResourceName = schemaResourceName;
        }

        @Override
        String getName() {
            return "properties";
        }

        @Override
        void assertValue(JsonValue value) {
            Set<String> propertyKeys = value.keys();
            for (String key : propertyKeys) {
                JsonValue property = value.get(key);
                if (!property.isDefined("type")) {
                    failWithMessage("Property \"%s\" in \"%s\" must specify a type", property.getPointer(),
                            schemaResourceName);
                }
            }
        }
    }

}
