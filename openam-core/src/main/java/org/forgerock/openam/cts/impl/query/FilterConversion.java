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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.impl.query;

import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.opendj.ldap.Filter;

import javax.inject.Inject;
import java.util.Map;

/**
 * Responsible for converting the CTS api TokenFilter into an LDAP filter.
 */
public class FilterConversion {
    private final QueryFactory factory;

    /**
     * @param factory Required.
     */
    @Inject
    public FilterConversion(QueryFactory factory) {
        this.factory = factory;
    }

    /**
     * Convert a TokenFilter into an LDAP SDK Filter.
     *
     * This conversion will perform conversion based on the type of
     * TokenFilter and the attributes that are to be filtered.
     *
     * @param filter Non null.
     * @return Non null LDAP Filter.
     */
    public Filter convert(TokenFilter filter) {
        QueryFilter.QueryFilterBuilder builder;

        if (filter.getType() == TokenFilter.Type.AND) {
            builder = factory.createFilter().and();
        } else {
            builder = factory.createFilter().or();
        }

        for (Map.Entry<CoreTokenField, Object> entry : filter.getFilters().entrySet()) {
            builder.attribute(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }
}
