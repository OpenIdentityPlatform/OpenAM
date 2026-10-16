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
 * Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.openam.session.service.access;

import static org.forgerock.openam.session.SessionConstants.JSON_SESSION_REALM;

import java.util.List;

import org.forgerock.json.JsonPointer;
import org.forgerock.openam.utils.CrestQuery;
import org.forgerock.openam.utils.RealmUtils;
import org.forgerock.util.query.BaseQueryFilterVisitor;
import org.forgerock.util.query.QueryFilter;

/**
 * Reads the realm a session CREST query filter asks for, so that the realm can be authorized against the
 * caller before the query is run against the CTS.
 *
 * <p>The supported filter shapes are the ones supported by
 * {@code org.forgerock.openam.session.service.access.persistence.SessionQueryFilterVisitor}, i.e. {@literal and}
 * of {@literal equals} comparisons. Any other filter type is rejected with an
 * {@link UnsupportedOperationException} by the visitor this class is based on, exactly as it would be rejected
 * when the filter is later translated into a CTS query.</p>
 */
public final class SessionQueryFilterRealm {

    private SessionQueryFilterRealm() {
        // Utility class.
    }

    /**
     * Returns the realm named in the query filter of the provided CREST query.
     *
     * @param crestQuery The CREST query to inspect. May be {@code null}.
     * @return The realm named in the query filter, or {@code null} if the query does not carry a filter, or the
     *         filter does not name a realm.
     * @throws IllegalArgumentException If the filter names more than one realm, since in that case the realm the
     *         query would run against cannot be authorized, or if the realm contains a {@literal ..} path
     *         segment, since such a realm is authorized as the path it is written as, while it may name another
     *         realm once it is resolved.
     */
    public static String extract(CrestQuery crestQuery) {
        if (crestQuery == null || crestQuery.getQueryFilter() == null) {
            return null;
        }
        RealmVisitor visitor = new RealmVisitor();
        crestQuery.getQueryFilter().accept(visitor, null);
        if (RealmUtils.containsParentPathSegment(visitor.realm)) {
            throw new IllegalArgumentException("The realm of the query filter must not contain a '..' path segment");
        }
        return visitor.realm;
    }

    /**
     * Collects the realm out of the {@literal realm eq "..."} comparison of a session query filter, ignoring the
     * comparisons of every other field.
     */
    private static final class RealmVisitor extends BaseQueryFilterVisitor<Void, Void, JsonPointer> {

        private String realm;

        @Override
        public Void visitAndFilter(Void parameter, List<QueryFilter<JsonPointer>> subFilters) {
            for (QueryFilter<JsonPointer> subFilter : subFilters) {
                subFilter.accept(this, parameter);
            }
            return null;
        }

        @Override
        public Void visitEqualsFilter(Void parameter, JsonPointer field, Object valueAssertion) {
            if (JSON_SESSION_REALM.equals(field.leaf()) && valueAssertion instanceof String) {
                String value = (String) valueAssertion;
                if (realm != null && !realm.equals(value)) {
                    throw new IllegalArgumentException("The query filter must not specify more than one realm");
                }
                realm = value;
            }
            return null;
        }
    }
}
