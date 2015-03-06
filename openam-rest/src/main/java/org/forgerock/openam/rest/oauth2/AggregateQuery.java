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

/**
 * Aggregation of queries against Resource Sets and UMA policies.
 *
 * @since 13.0.0
 */
public class AggregateQuery<Q1, Q2> {

    public enum Operator {
        OR,
        AND
    }

    private Q1 firstQuery;
    private Q2 secondQuery;
    private Operator operator = Operator.OR;

    public Q1 getFirstQuery() {
        return firstQuery;
    }

    public void setFirstQuery(Q1 query) {
        this.firstQuery = query;
    }

    public Q2 getSecondQuery() {
        return secondQuery;
    }

    public void setSecondQuery(Q2 query) {
        this.secondQuery = query;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }
}
