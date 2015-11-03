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
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.json.resource.UpdateRequest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @since 13.0.0
 */
public class AuditFilterTest extends AbstractAuditFilterTest {

    private AuditFilter auditFilter;

    @BeforeMethod
    protected void setUp() throws Exception {
        super.setUp();
        auditFilter = new AuditFilter(mock(Debug.class), auditEventPublisher, auditEventFactory);
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
    public void shouldReturnNullForCreateSuccess() {
        CreateRequest createRequest = mock(CreateRequest.class);
        ResourceResponse resourceResponse = mock(ResourceResponse.class);

        JsonValue filterResponse = auditFilter.getCreateSuccessDetail(createRequest, resourceResponse);

        assertThat(filterResponse).isEqualTo(null);
    }

    @Test
    public void shouldReturnNullForReadSuccess() {
        ReadRequest readRequest = mock(ReadRequest.class);
        ResourceResponse resourceResponse = mock(ResourceResponse.class);

        JsonValue filterResponse = auditFilter.getReadSuccessDetail(readRequest, resourceResponse);

        assertThat(filterResponse).isEqualTo(null);
    }

    @Test
    public void shouldReturnNullForUpdateSuccess() {
        UpdateRequest updateRequest = mock(UpdateRequest.class);
        ResourceResponse resourceResponse = mock(ResourceResponse.class);

        JsonValue filterResponse = auditFilter.getUpdateSuccessDetail(updateRequest, resourceResponse);

        assertThat(filterResponse).isEqualTo(null);
    }

    @Test
    public void shouldReturnNullForDeleteSuccess() {
        DeleteRequest deleteRequest = mock(DeleteRequest.class);
        ResourceResponse resourceResponse = mock(ResourceResponse.class);

        JsonValue filterResponse = auditFilter.getDeleteSuccessDetail(deleteRequest, resourceResponse);

        assertThat(filterResponse).isEqualTo(null);
    }

    @Test
    public void shouldReturnNullForPatchSuccess() {
        PatchRequest patchRequest = mock(PatchRequest.class);
        ResourceResponse resourceResponse = mock(ResourceResponse.class);

        JsonValue filterResponse = auditFilter.getPatchSuccessDetail(patchRequest, resourceResponse);

        assertThat(filterResponse).isEqualTo(null);
    }

    @Test
    public void shouldReturnNullForActionSuccess() {
        ActionRequest actionRequest = mock(ActionRequest.class);
        ActionResponse actionResponse = mock(ActionResponse.class);

        JsonValue filterResponse = auditFilter.getActionSuccessDetail(actionRequest, actionResponse);

        assertThat(filterResponse).isEqualTo(null);
    }

    @Test
    public void shouldReturnNullForQuerySuccess() {
        QueryRequest queryRequest = mock(QueryRequest.class);
        QueryResponse queryResponse = mock(QueryResponse.class);

        JsonValue filterResponse = auditFilter.getQuerySuccessDetail(queryRequest, queryResponse);

        assertThat(filterResponse).isEqualTo(null);
    }
}
