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
package org.forgerock.openam.rest.query;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.openam.rest.query.QueryException.QueryErrorCode.*;

import org.testng.annotations.Test;

public class QueryExceptionTest {

    @Test
    public void shouldMapScriptErrorCodeToMessage() {
        // given
        QueryException qe;

        // when
        qe = new QueryException(FILTER_BOOLEAN_LITERAL_FALSE);
        //then
        assertThat(qe.getMessage()).isEqualTo("The 'boolean literal' filter with value of 'false' is not supported");

        // when
        qe = new QueryException(FILTER_EXTENDED_MATCH);
        //then
        assertThat(qe.getMessage()).isEqualTo("The 'extended match' filter is not supported");

        // when
        qe = new QueryException(FILTER_GREATER_THAN);
        //then
        assertThat(qe.getMessage()).isEqualTo("The 'greater than' filter is not supported");

        // when
        qe = new QueryException(FILTER_GREATER_THAN_OR_EQUAL);
        //then
        assertThat(qe.getMessage()).isEqualTo("The 'greater than or equal' filter is not supported");

        // when
        qe = new QueryException(FILTER_LESS_THAN);
        //then
        assertThat(qe.getMessage()).isEqualTo("The 'less than' filter is not supported");

        // when
        qe = new QueryException(FILTER_LESS_THAN_OR_EQUAL);
        //then
        assertThat(qe.getMessage()).isEqualTo("The 'less than or equal' filter is not supported");

        // when
        qe = new QueryException(FILTER_NOT);
        //then
        assertThat(qe.getMessage()).isEqualTo("The 'not' filter is not supported");

        // when
        qe = new QueryException(FILTER_PRESENT);
        //then
        assertThat(qe.getMessage()).isEqualTo("The 'present' filter is not supported");

        // when
        qe = new QueryException(FILTER_DEPTH_SUPPORTED);
        //then
        assertThat(qe.getMessage()).isEqualTo("Filter path is too long, a depth of 1 is supported");
    }
}
