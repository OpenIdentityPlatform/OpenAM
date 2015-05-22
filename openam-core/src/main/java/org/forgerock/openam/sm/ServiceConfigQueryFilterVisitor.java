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
package org.forgerock.openam.sm;

import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttr;

import com.sun.identity.sm.ServiceConfig;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.query.BaseQueryFilterVisitor;
import org.forgerock.util.query.QueryFilter;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Filter visitor for {@code ServiceConfig}. It will filter against attribute data read from the Service Configuration.
 * Each visit will return true or false depending on whether or not the a match was found in the attributes.
 *
 * @since 13.0.0
 */
public class ServiceConfigQueryFilterVisitor extends BaseQueryFilterVisitor<Boolean, ServiceConfig, String> {

    @SuppressWarnings("unchecked")
    private Map<String, Set<String>> getConfigData(ServiceConfig serviceConfig) {
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
        return StringUtils.match(getMapAttr(getConfigData(serviceConfig), field), (String)valueAssertion);
    }

    @Override
    public Boolean visitContainsFilter(ServiceConfig serviceConfig, String field, Object valueAssertion) {
        if (!(valueAssertion instanceof String)) {
            return false;
        }
        return StringUtils.contains(getMapAttr(getConfigData(serviceConfig), field), (String)valueAssertion);
    }

    @Override
    public Boolean visitStartsWithFilter(ServiceConfig serviceConfig, String field, Object valueAssertion) {
        if (!(valueAssertion instanceof String)) {
            return false;
        }
        return StringUtils.startsWith(getMapAttr(getConfigData(serviceConfig), field), (String)valueAssertion);
    }

    @Override
    public Boolean visitBooleanLiteralFilter(ServiceConfig serviceConfig, boolean value) {
        if (value) {
            return true;
        }
        throw new UnsupportedOperationException();
    }
}
