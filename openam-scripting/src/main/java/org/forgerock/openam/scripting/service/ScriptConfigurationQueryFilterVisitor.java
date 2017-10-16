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

package org.forgerock.openam.scripting.service;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;

/**
 * A query filter visitor that filters a {@code Set<ScriptConfiguration>} for values that match using the bean
 * properties of the configuration objects.
 */
class ScriptConfigurationQueryFilterVisitor
        implements QueryFilterVisitor<Set<ScriptConfiguration>, Set<ScriptConfiguration>, String> {

    private static final BeanInfo BEAN_INFO;

    static {
        try {
            BEAN_INFO = Introspector.getBeanInfo(ScriptConfiguration.class);
        } catch (IntrospectionException e) {
            throw new IllegalStateException("Cannot get bean info for ScriptConfiguration", e);
        }
    }

    @Override
    public Set<ScriptConfiguration> visitAndFilter(Set<ScriptConfiguration> scriptConfigurations,
            List<QueryFilter<String>> subFilters) {
        Set<ScriptConfiguration> result = new HashSet<>(scriptConfigurations);
        for (QueryFilter<String> filter : subFilters) {
            result.retainAll(filter.accept(this, scriptConfigurations));
        }
        return result;
    }

    @Override
    public Set<ScriptConfiguration> visitOrFilter(Set<ScriptConfiguration> scriptConfigurations,
            List<QueryFilter<String>> subFilters) {
        Set<ScriptConfiguration> result = new HashSet<>();
        for (QueryFilter<String> filter : subFilters) {
            result.addAll(filter.accept(this, scriptConfigurations));
        }
        return result;
    }

    @Override
    public Set<ScriptConfiguration> visitBooleanLiteralFilter(Set<ScriptConfiguration> scriptConfigurations,
            boolean value) {
        return value ? scriptConfigurations : Collections.<ScriptConfiguration>emptySet();
    }

    @Override
    public Set<ScriptConfiguration> visitContainsFilter(Set<ScriptConfiguration> scriptConfigurations, String field,
            final Object valueAssertion) {
        final PropertyDescriptor descriptor = getField(field);
        return findMatching(scriptConfigurations, new Predicate<ScriptConfiguration>() {
            @Override
            public boolean apply(@Nullable ScriptConfiguration configuration) {
                return getStringValue(descriptor, configuration).contains(valueAssertion.toString());
            }
        });
    }

    @Override
    public Set<ScriptConfiguration> visitEqualsFilter(Set<ScriptConfiguration> scriptConfigurations, String field,
            final Object valueAssertion) {
        final PropertyDescriptor descriptor = getField(field);
        return findMatching(scriptConfigurations, new Predicate<ScriptConfiguration>() {
            @Override
            public boolean apply(@Nullable ScriptConfiguration configuration) {
                return getValue(descriptor, configuration).equals(valueAssertion);
            }
        });
    }

    @Override
    public Set<ScriptConfiguration> visitGreaterThanFilter(Set<ScriptConfiguration> scriptConfigurations, String field,
            final Object valueAssertion) {
        final PropertyDescriptor descriptor = getField(field);
        return findMatching(scriptConfigurations, new Predicate<ScriptConfiguration>() {
            @Override
            @SuppressWarnings("unchecked")
            public boolean apply(@Nullable ScriptConfiguration configuration) {
                Comparable comparableValue = getComparableValue(descriptor, configuration);
                return comparableValue.getClass().equals(valueAssertion.getClass())
                        && comparableValue.compareTo(valueAssertion) > 0;
            }
        });
    }

    @Override
    public Set<ScriptConfiguration> visitGreaterThanOrEqualToFilter(Set<ScriptConfiguration> scriptConfigurations,
            String field, final Object valueAssertion) {
        final PropertyDescriptor descriptor = getField(field);
        return findMatching(scriptConfigurations, new Predicate<ScriptConfiguration>() {
            @Override
            @SuppressWarnings("unchecked")
            public boolean apply(@Nullable ScriptConfiguration configuration) {
                Comparable comparableValue = getComparableValue(descriptor, configuration);
                return comparableValue.getClass().equals(valueAssertion.getClass())
                        && comparableValue.compareTo(valueAssertion) >= 0;
            }
        });
    }

    @Override
    public Set<ScriptConfiguration> visitLessThanFilter(Set<ScriptConfiguration> scriptConfigurations, String field,
            final Object valueAssertion) {
        final PropertyDescriptor descriptor = getField(field);
        return findMatching(scriptConfigurations, new Predicate<ScriptConfiguration>() {
            @Override
            @SuppressWarnings("unchecked")
            public boolean apply(@Nullable ScriptConfiguration configuration) {
                Comparable comparableValue = getComparableValue(descriptor, configuration);
                return comparableValue.getClass().equals(valueAssertion.getClass())
                        && comparableValue.compareTo(valueAssertion) < 0;
            }
        });
    }

    @Override
    public Set<ScriptConfiguration> visitLessThanOrEqualToFilter(Set<ScriptConfiguration> scriptConfigurations,
            String field, final Object valueAssertion) {
        final PropertyDescriptor descriptor = getField(field);
        return findMatching(scriptConfigurations, new Predicate<ScriptConfiguration>() {
            @Override
            @SuppressWarnings("unchecked")
            public boolean apply(@Nullable ScriptConfiguration configuration) {
                Comparable comparableValue = getComparableValue(descriptor, configuration);
                return comparableValue.getClass().equals(valueAssertion.getClass())
                        && comparableValue.compareTo(valueAssertion) <= 0;
            }
        });
    }

    @Override
    public Set<ScriptConfiguration> visitNotFilter(Set<ScriptConfiguration> scriptConfigurations, QueryFilter<String>
            subFilter) {
        Set<ScriptConfiguration> configurations = new HashSet<>(scriptConfigurations);
        configurations.removeAll(subFilter.accept(this, scriptConfigurations));
        return null;
    }

    @Override
    public Set<ScriptConfiguration> visitPresentFilter(Set<ScriptConfiguration> scriptConfigurations, String field) {
        final PropertyDescriptor descriptor = getField(field);
        return findMatching(scriptConfigurations, new Predicate<ScriptConfiguration>() {
            @Override
            public boolean apply(@Nullable ScriptConfiguration configuration) {
                return getValue(descriptor, configuration) != null;
            }
        });
    }

    @Override
    public Set<ScriptConfiguration> visitStartsWithFilter(Set<ScriptConfiguration> scriptConfigurations, String field,
            final Object valueAssertion) {
        final PropertyDescriptor descriptor = getField(field);
        return findMatching(scriptConfigurations, new Predicate<ScriptConfiguration>() {
            @Override
            public boolean apply(@Nullable ScriptConfiguration configuration) {
                return getStringValue(descriptor, configuration).startsWith(valueAssertion.toString());
            }
        });
    }

    private PropertyDescriptor getField(String field) {
        for (PropertyDescriptor pd : BEAN_INFO.getPropertyDescriptors()) {
            if (pd.getName().equals(field)) {
                return pd;
            }
        }
        throw new IllegalArgumentException("Unknown field: " + field);
    }

    private Object getValue(PropertyDescriptor pd, ScriptConfiguration configuration) {
        try {
            Object result = pd.getReadMethod().invoke(configuration);
            return result instanceof Enum ? ((Enum) result).name() : result;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Cannot get field: " + pd.getName(), e);
        }
    }

    private String getStringValue(PropertyDescriptor pd, ScriptConfiguration configuration) {
        Object result = getValue(pd, configuration);
        if (result instanceof Number) {
            throw new IllegalArgumentException("Field is not a string: " + pd.getName());
        }
        if (result instanceof Enum) {
            return ((Enum<?>) result).name();
        }
        return result.toString();
    }

    private Comparable getComparableValue(PropertyDescriptor pd, ScriptConfiguration configuration) {
        Object result = getValue(pd, configuration);
        if (result instanceof Enum) {
            result = ((Enum<?>) result).name();
        }
        if (!(result instanceof Comparable)) {
            throw new IllegalArgumentException("Field is not a comparable: " + pd.getName());
        }
        return (Comparable) result;
    }

    private Set<ScriptConfiguration> findMatching(Set<ScriptConfiguration> configurations,
            Predicate<ScriptConfiguration> test) {
        Set<ScriptConfiguration> result = new HashSet<>();
        for (ScriptConfiguration sc : configurations) {
            if (test.apply(sc)) {
                result.add(sc);
            }
        }
        return result;
    }

    @Override
    public Set<ScriptConfiguration> visitExtendedMatchFilter(Set<ScriptConfiguration> scriptConfigurations, String
            field, String operator, Object valueAssertion) {
        throw new UnsupportedOperationException();
    }
}
