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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openam.sm;

import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttr;
import static java.lang.Double.parseDouble;
import static java.lang.Long.parseLong;
import static org.forgerock.openam.utils.StringUtils.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.util.query.BaseQueryFilterVisitor;
import org.forgerock.util.query.QueryFilter;

import com.sun.identity.sm.ServiceConfig;

/**
 * Filter visitor for {@code ServiceConfig}. It will filter against attribute data read from the Service Configuration.
 * Each visit will return true or false depending on whether or not the a match was found in the attributes.
 *
 * @since 13.0.0
 */
public class ServiceConfigQueryFilterVisitor extends BaseQueryFilterVisitor<Boolean, ServiceConfig, String> {

    /**
     * Get the configuration attribute data from the given {@link ServiceConfig}.
     *
     * @param serviceConfig The {@link ServiceConfig} instance.
     * @return A map containing the configuration attribute data.
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Set<String>> getConfigData(ServiceConfig serviceConfig) {
        return serviceConfig.getAttributesForRead();
    }

    @Override
    public Boolean visitAndFilter(ServiceConfig serviceConfig, List<QueryFilter<String>> subFilters) {
        for (QueryFilter<String> filter : subFilters) {
            if (!filter.accept(this, serviceConfig)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Boolean visitOrFilter(ServiceConfig serviceConfig, List<QueryFilter<String>> subFilters) {
        for (QueryFilter<String> filter : subFilters) {
            if (filter.accept(this, serviceConfig)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visitEqualsFilter(ServiceConfig serviceConfig, String field, Object valueAssertion) {
        if (!(valueAssertion instanceof String)) {
            return false;
        }
        return match(getMapAttr(getConfigData(serviceConfig), field), (String) valueAssertion);
    }

    @Override
    public Boolean visitContainsFilter(ServiceConfig serviceConfig, String field, Object valueAssertion) {
        if (!(valueAssertion instanceof String)) {
            return false;
        }
        return containsCaseInsensitive(getMapAttr(getConfigData(serviceConfig), field), (String) valueAssertion);
    }

    @Override
    public Boolean visitStartsWithFilter(ServiceConfig serviceConfig, String field, Object valueAssertion) {
        if (!(valueAssertion instanceof String)) {
            return false;
        }
        return startsWith(getMapAttr(getConfigData(serviceConfig), field), (String) valueAssertion);
    }

    @Override
    public Boolean visitBooleanLiteralFilter(ServiceConfig serviceConfig, boolean value) {
        if (value) {
            return true;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean visitGreaterThanFilter(ServiceConfig serviceConfig, String field, Object valueAssertion) {
        String value = getMapAttr(getConfigData(serviceConfig), field);
        if (isEmpty(value)) {
            return false;
        }
        try {
            if (valueAssertion instanceof Long) {
                return parseLong(value) > (Long) valueAssertion;
            }
            if (valueAssertion instanceof Double) {
                return parseDouble(value) > (Double) valueAssertion;
            }
        } catch (NumberFormatException e) {
            throw new UnsupportedOperationException("Value of field \"" + field + "\" is not a "
                    + valueAssertion.getClass().getSimpleName());
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean visitGreaterThanOrEqualToFilter(ServiceConfig serviceConfig, String field, Object valueAssertion) {
        String value = getMapAttr(getConfigData(serviceConfig), field);
        if (isEmpty(value)) {
            return false;
        }
        try {
            if (valueAssertion instanceof Long) {
                return parseLong(value) >= (Long) valueAssertion;
            }
            if (valueAssertion instanceof Double) {
                return parseDouble(value) >= (Double) valueAssertion;
            }
        } catch (NumberFormatException e) {
            throw new UnsupportedOperationException("Value of field \"" + field + "\" is not a "
                    + valueAssertion.getClass().getSimpleName());
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean visitLessThanFilter(ServiceConfig serviceConfig, String field, Object valueAssertion) {
        String value = getMapAttr(getConfigData(serviceConfig), field);
        if (isEmpty(value)) {
            return false;
        }
        try {
            if (valueAssertion instanceof Long) {
                return parseLong(value) < (Long) valueAssertion;
            }
            if (valueAssertion instanceof Double) {
                return parseDouble(value) < (Double) valueAssertion;
            }
        } catch (NumberFormatException e) {
            throw new UnsupportedOperationException("Value of field \"" + field + "\" is not a "
                    + valueAssertion.getClass().getSimpleName());
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean visitLessThanOrEqualToFilter(ServiceConfig serviceConfig, String field, Object valueAssertion) {
        String value = getMapAttr(getConfigData(serviceConfig), field);
        if (isEmpty(value)) {
            return false;
        }
        try {
            if (valueAssertion instanceof Long) {
                return parseLong(value) <= (Long) valueAssertion;
            }
            if (valueAssertion instanceof Double) {
                return parseDouble(value) <= (Double) valueAssertion;
            }
        } catch (NumberFormatException e) {
            throw new UnsupportedOperationException("Value of field \"" + field + "\" is not a "
                    + valueAssertion.getClass().getSimpleName());
        }
        throw new UnsupportedOperationException();
    }
}
