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

package org.forgerock.openam.rest.oauth2;

import org.forgerock.json.resource.QueryFilter;

/**
 * Aggregation of queries against Resource Sets and UMA policies.
 *
 * @since 13.0.0
 */
final class ResourceSetWithPolicyQuery {

    private org.forgerock.util.query.QueryFilter<String> resourceSetQuery;
    private QueryFilter policyQuery;
    private String operator = "OR";

    org.forgerock.util.query.QueryFilter<String> getResourceSetQuery() {
        return resourceSetQuery;
    }

    void setResourceSetQuery(org.forgerock.util.query.QueryFilter<String> query) {
        this.resourceSetQuery = query;
    }

    QueryFilter getPolicyQuery() {
        return policyQuery;
    }

    void setPolicyQuery(QueryFilter query) {
        this.policyQuery = query;
    }

    String getOperator() {
        return operator;
    }

    void setOperator(String operator) {
        this.operator = operator;
    }
}
