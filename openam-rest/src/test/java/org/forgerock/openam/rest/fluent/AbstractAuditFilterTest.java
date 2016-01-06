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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openam.rest.fluent;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Requests.*;
import static org.forgerock.openam.rest.fluent.AuditTestUtils.*;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.BDDMockito.*;

import org.forgerock.audit.AuditException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @since 13.0.0
 */
public abstract class AbstractAuditFilterTest {

    protected CrestAuditor auditor;
    protected CrestAuditorFactory auditorFactory;
    protected AuditEventFactory auditEventFactory;
    protected AuditEventPublisher auditEventPublisher;
    protected Context context;
    protected ReadRequest readRequest;
    protected QueryResourceHandler queryResourceHandler;
    protected RequestHandler filterChain;
    protected CreateRequest createRequest;
    protected UpdateRequest updateRequest;
    protected DeleteRequest deleteRequest;
    protected PatchRequest patchRequest;
    protected ActionRequest actionRequest;
    protected QueryRequest queryRequest;

    @BeforeMethod
    protected void setUp() throws Exception {
        auditor = mock(CrestAuditor.class);
        auditorFactory = mock(CrestAuditorFactory.class);
        auditEventPublisher = mock(AuditEventPublisher.class);
        auditEventFactory = mockAuditEventFactory();
        context = mockAuditContext();
        queryResourceHandler = mock(QueryResourceHandler.class);
        filterChain = mock(RequestHandler.class);
        createRequest = newCreateRequest("mockResource", json(object()));
        readRequest = newReadRequest("mockResource");
        updateRequest = newUpdateRequest("mockResource", json(object()));
        deleteRequest = newDeleteRequest("mockResource");
        patchRequest = newPatchRequest("mockResource");
        actionRequest = newActionRequest("mockResource", "actionId");
        queryRequest = newQueryRequest("mockResource");
        given(auditorFactory.create(any(Context.class), any(Request.class))).willReturn(auditor);
    }

    @SuppressWarnings("unchecked")
    @DataProvider(name = "auditedCrudpaqOperations")
    public abstract Object[][] auditedCrudpaqOperations() throws IllegalAccessException, InstantiationException;

    @SuppressWarnings("unchecked")
    @DataProvider(name = "unauditedCrudpaqOperations")
    public abstract Object[][] unauditedCrudpaqOperations() throws IllegalAccessException, InstantiationException;

    @SuppressWarnings("unchecked")
    @Test(dataProvider = "auditedCrudpaqOperations")
    public void shouldAuditAttemptAndSuccessEventsForSuccessfulAuditedRequests(Runnable filteredOp) throws Exception {
        // Given
        givenFilteredCrudpaqOperationSucceeds();

        // When
        filteredOp.run();

        // Then
        verify(auditor).auditAccessAttempt();
        verify(auditor).auditAccessSuccess(any(JsonValue.class));
    }

    @Test(dataProvider = "auditedCrudpaqOperations")
    public void shouldAuditAttemptAndFailureEventsForUnsuccessfulAuditedRequests(Runnable filteredOp) throws Exception {
        // Given
        givenFilteredCrudpaqOperationFails();

        // When
        filteredOp.run();

        // Then
        verify(auditor).auditAccessAttempt();
        verify(auditor).auditAccessFailure(anyInt(), anyString());
    }

    @Test(dataProvider = "unauditedCrudpaqOperations")
    public void publishesNoAuditsEventsForUnauditedRequests(Runnable filteredOp) throws Exception {
        // Given
        givenFilteredCrudpaqOperationSucceeds();

        // When
        filteredOp.run();

        // Then
        verifyNoEventsPublished();
    }

    @SuppressWarnings("unchecked")
    private void givenFilteredCrudpaqOperationSucceeds() {
        when(filterChain.handleCreate(eq(context), eq(createRequest)))
                .thenReturn(mockResourceResponseResultPromise());
        when(filterChain.handleRead(eq(context), eq(readRequest)))
                .thenReturn(mockResourceResponseResultPromise());
        when(filterChain.handleUpdate(eq(context), eq(updateRequest)))
                .thenReturn(mockResourceResponseResultPromise());
        when(filterChain.handleDelete(eq(context), eq(deleteRequest)))
                .thenReturn(mockResourceResponseResultPromise());
        when(filterChain.handlePatch(eq(context), eq(patchRequest)))
                .thenReturn(mockResourceResponseResultPromise());
        when(filterChain.handleAction(eq(context), eq(actionRequest)))
                .thenReturn(mockActionResponseResultPromise());
        when(filterChain.handleQuery(eq(context), eq(queryRequest), any(QueryResourceHandler.class)))
                .thenReturn(mockQueryResponseResultPromise());
    }

    @SuppressWarnings("unchecked")
    private void givenFilteredCrudpaqOperationFails() {
        when(filterChain.handleCreate(eq(context), eq(createRequest)))
                .thenReturn(mockResourceResponseExceptionPromise());
        when(filterChain.handleRead(eq(context), eq(readRequest)))
                .thenReturn(mockResourceResponseExceptionPromise());
        when(filterChain.handleUpdate(eq(context), eq(updateRequest)))
                .thenReturn(mockResourceResponseExceptionPromise());
        when(filterChain.handleDelete(eq(context), eq(deleteRequest)))
                .thenReturn(mockResourceResponseExceptionPromise());
        when(filterChain.handlePatch(eq(context), eq(patchRequest)))
                .thenReturn(mockResourceResponseExceptionPromise());
        when(filterChain.handleAction(eq(context), eq(actionRequest)))
                .thenReturn(mockActionResponseExceptionPromise());
        when(filterChain.handleQuery(eq(context), eq(queryRequest), any(QueryResourceHandler.class)))
                .thenReturn(mockQueryResponseExceptionPromise());
    }

    private static Promise<ResourceResponse, ResourceException> mockResourceResponseResultPromise() {
        return newResultPromise(mock(ResourceResponse.class));
    }

    private static Promise<ActionResponse, ResourceException> mockActionResponseResultPromise() {
        return newResultPromise(mock(ActionResponse.class));
    }

    private static Promise<QueryResponse, ResourceException> mockQueryResponseResultPromise() {
        return newResultPromise(mock(QueryResponse.class));
    }

    private static Promise<ResourceResponse, ResourceException> mockResourceResponseExceptionPromise() {
        return new InternalServerErrorException("expected exception").asPromise();
    }

    private static Promise<ActionResponse, ResourceException> mockActionResponseExceptionPromise() {
        return new InternalServerErrorException("expected exception").asPromise();
    }

    private static Promise<QueryResponse, ResourceException> mockQueryResponseExceptionPromise() {
        return new InternalServerErrorException("expected exception").asPromise();
    }

    private void verifyNoEventsPublished() throws AuditException {
        verify(auditor, never()).auditAccessAttempt();
        verifyNoOutcomeEventsPublished();
    }

    private void verifyNoOutcomeEventsPublished() {
        verify(auditor, never()).auditAccessSuccess(any(JsonValue.class));
        verify(auditor, never()).auditAccessFailure(anyInt(), anyString());
    }
}
