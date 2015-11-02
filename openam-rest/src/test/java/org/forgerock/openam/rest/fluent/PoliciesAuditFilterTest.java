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
package org.forgerock.openam.rest.fluent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.Responses;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @since 13.0.0
 */
public class PoliciesAuditFilterTest extends AbstractAuditFilterTest {

    private PoliciesAuditFilter auditFilter;

    @BeforeMethod
    protected void setUp() throws Exception {
        super.setUp();
        auditFilter = new PoliciesAuditFilter(mock(Debug.class), auditEventPublisher, auditEventFactory);
    }

    @SuppressWarnings("unchecked")
    @DataProvider(name = "auditedCrudpaqOperations")
    public Object[][] auditedCrudpaqOperations() throws IllegalAccessException, InstantiationException {
        return new Object[][] {
                {new Runnable() {
                    @Override
                    public void run() {
                        auditFilter.filterCreate(context, createRequest, filterChain);
                    }
                }},
                {new Runnable() {
                    @Override
                    public void run() {
                        auditFilter.filterRead(context, readRequest, filterChain);
                    }
                }},
                {new Runnable() {
                    @Override
                    public void run() {
                        auditFilter.filterUpdate(context, updateRequest, filterChain);
                    }
                }},
                {new Runnable() {
                    @Override
                    public void run() {
                        auditFilter.filterDelete(context, deleteRequest, filterChain);
                    }
                }},
                {new Runnable() {
                    @Override
                    public void run() {
                        auditFilter.filterPatch(context, patchRequest, filterChain);
                    }
                }},
                {new Runnable() {
                    @Override
                    public void run() {
                        auditFilter.filterAction(context, actionRequest, filterChain);
                    }
                }},
                {new Runnable() {
                    @Override
                    public void run() {
                        auditFilter.filterQuery(context, queryRequest, queryResourceHandler, filterChain);
                    }
                }}
        };
    }

    @SuppressWarnings("unchecked")
    @DataProvider(name = "unauditedCrudpaqOperations")
    public Object[][] unauditedCrudpaqOperations() throws IllegalAccessException, InstantiationException {
        return new Object[][] { };
    }

    @Test
    public void shouldNotReturnJsonDetailGivenNoActionResponseContent() {
        QueryResponse queryResponse = Responses.newQueryResponse();

        JsonValue filterResponse = auditFilter.getDetail(queryResponse);

        assertThat(filterResponse).isEqualTo(null);
    }

    @Test
    public void shouldReturnJsonDetailGivenActionResponseContent() {
        JsonValue jsonValue = new JsonValue("Test string");
        ActionResponse actionResponse = Responses.newActionResponse(jsonValue);

        JsonValue filterResponse = auditFilter.getDetail(actionResponse);

        assertThat(filterResponse).isEqualTo(jsonValue);
    }
}
