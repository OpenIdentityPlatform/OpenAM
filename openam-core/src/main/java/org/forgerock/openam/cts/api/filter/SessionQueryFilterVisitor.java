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

import static org.forgerock.openam.session.SessionConstants.JSON_SESSION_REALM;
import static org.forgerock.openam.session.SessionConstants.JSON_SESSION_USERNAME;

import java.util.List;

import org.forgerock.json.JsonPointer;
import org.forgerock.openam.cts.api.fields.SessionTokenField;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder.FilterAttributeBuilder;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.query.BaseQueryFilterVisitor;
import org.forgerock.util.query.QueryFilter;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;

/**
 * A {@link org.forgerock.util.query.QueryFilterVisitor} implementation that transforms CREST Query filters to CTS
 * attribute query filters.
 *
 * The current implementation only supports username and realm based lookups, however in order to construct the username
 * part of the filter, the realm MUST be also present in the filter.
 */
public class SessionQueryFilterVisitor extends BaseQueryFilterVisitor<Void, FilterAttributeBuilder, JsonPointer> {

    private final IdentityUtils identityUtils;
    private String username;
    private String realm;

    public SessionQueryFilterVisitor(IdentityUtils identityUtils) {
        this.identityUtils = identityUtils;
    }

    @Override
    public Void visitAndFilter(FilterAttributeBuilder filterAttributeBuilder,
            List<QueryFilter<JsonPointer>> subFilters) {
        for (QueryFilter<JsonPointer> subFilter : subFilters) {
            subFilter.accept(this, filterAttributeBuilder);
        }
        return null;
    }

    @Override
    public Void visitEqualsFilter(FilterAttributeBuilder filterAttributeBuilder, JsonPointer field,
            Object valueAssertion) {
        switch (field.leaf()) {
            case JSON_SESSION_USERNAME:
                username = (String) valueAssertion;
                break;
            case JSON_SESSION_REALM:
                realm = (String) valueAssertion;
                filterAttributeBuilder.withAttribute(SessionTokenField.REALM.getField(), valueAssertion);
                break;
            default:
                throw new IllegalArgumentException(field.leaf() + " is not supported in query filter");
        }

        if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(realm)) {
            filterAttributeBuilder.withPriorityAttribute(CoreTokenField.USER_ID,
                    identityUtils.getUniversalId(username, IdType.USER, realm));
        }

        return null;
    }
}
