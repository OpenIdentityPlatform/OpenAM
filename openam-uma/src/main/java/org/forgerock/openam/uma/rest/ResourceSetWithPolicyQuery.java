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

package org.forgerock.openam.uma.rest;

import org.forgerock.json.JsonPointer;
import org.forgerock.openam.oauth2.rest.AggregateQuery;
import org.forgerock.util.query.QueryFilter;

/**
 * Aggregation of queries against Resource Sets and UMA policies.
 *
 * @since 13.0.0
 */
final class ResourceSetWithPolicyQuery
        extends AggregateQuery<QueryFilter<String>, QueryFilter<JsonPointer>> {

    /**
     * Get the resource set query.
     * @return The query.
     */
    public org.forgerock.util.query.QueryFilter<String> getResourceSetQuery() {
        return getFirstQuery();
    }

    /**
     * Set the resource set query.
     * @param query The query.
     */
    public void setResourceSetQuery(org.forgerock.util.query.QueryFilter<String> query) {
        setFirstQuery(query);
    }

    /**
     * Get the policy query.
     * @return The query.
     */
    public QueryFilter<JsonPointer> getPolicyQuery() {
        return getSecondQuery();
    }

    /**
     * Set the policy query.
     * @param query The query.
     */
    public void setPolicyQuery(QueryFilter<JsonPointer> query) {
        setSecondQuery(query);
    }
}
