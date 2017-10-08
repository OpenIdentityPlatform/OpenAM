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

package org.forgerock.openam.cts.api.filter;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.json.JsonPointer;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.util.query.BaseQueryFilterVisitor;
import org.forgerock.util.query.QueryFilter;

/**
 * A {@link org.forgerock.util.query.QueryFilterVisitor} implementation that transforms CREST Query filters to CTS
 * attribute query filters.
 *
 * This implementation is currently non-exhaustive, but can easily be extended.
 */
public class CTSQueryFilterVisitor extends BaseQueryFilterVisitor<QueryFilter<CoreTokenField>, Void, JsonPointer>  {

    @Override
    public QueryFilter<CoreTokenField> visitAndFilter(Void ignore, List<QueryFilter<JsonPointer>> subFilters) {
        List<QueryFilter<CoreTokenField>> convertedSubFilters = new ArrayList<>();
        for (QueryFilter<JsonPointer> subFilter : subFilters) {
            convertedSubFilters.add(subFilter.accept(this, null));
        }
        return QueryFilter.and(convertedSubFilters);
    }

    @Override
    public QueryFilter<CoreTokenField> visitEqualsFilter(Void ignore, JsonPointer field, Object valueAssertion) {
        return QueryFilter.equalTo(convertJsonPointer(field), valueAssertion);
    }

    @Override
    public QueryFilter<CoreTokenField> visitContainsFilter(Void ignore, JsonPointer field, Object valueAssertion) {
        return QueryFilter.contains(convertJsonPointer(field), valueAssertion);
    }

    private CoreTokenField convertJsonPointer(JsonPointer field) throws IllegalArgumentException {
        return CoreTokenField.fromLDAPAttribute(field.leaf());
    }
}
