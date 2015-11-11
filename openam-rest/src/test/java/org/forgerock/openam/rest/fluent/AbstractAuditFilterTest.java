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
import static org.forgerock.audit.events.AuditEventBuilder.EVENT_NAME;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Requests.*;
import static org.forgerock.openam.audit.AuditConstants.ACCESS_TOPIC;
import static org.forgerock.openam.audit.AuditConstants.NO_REALM;
import static org.forgerock.openam.rest.fluent.JsonUtils.jsonFromFile;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.services.context.ClientContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RequestAuditContext;
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
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.audit.AMAccessAuditEventBuilder;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.rest.resource.AuditInfoContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import static org.forgerock.util.promise.Promises.newResultPromise;

import org.forgerock.util.promise.Promise;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.security.auth.Subject;

/**
 * @since 13.0.0
 */
public abstract class AbstractAuditFilterTest {

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
        auditEventPublisher = mock(AuditEventPublisher.class);
        auditEventFactory = mockAuditEventFactory();
        context = new RequestAuditContext(fakeContext());
        queryResourceHandler = mock(QueryResourceHandler.class);
        filterChain = mock(RequestHandler.class);
        createRequest = newCreateRequest("mockResource", json(object()));
        readRequest = newReadRequest("mockResource");
        updateRequest = newUpdateRequest("mockResource", json(object()));
        deleteRequest = newDeleteRequest("mockResource");
        patchRequest = newPatchRequest("mockResource");
        actionRequest = newActionRequest("mockResource", "actionId");
        queryRequest = newQueryRequest("mockResource");
    }

    @SuppressWarnings("unchecked")
    @DataProvider(name = "auditedCrudpaqOperations")
    public abstract Object[][] auditedCrudpaqOperations() throws IllegalAccessException, InstantiationException;

    @SuppressWarnings("unchecked")
    @DataProvider(name = "unauditedCrudpaqOperations")
    public abstract Object[][] unauditedCrudpaqOperations() throws IllegalAccessException, InstantiationException;

    @SuppressWarnings("unchecked")
    @Test(dataProvider = "auditedCrudpaqOperations")
    public void publishesAccessAndSuccessAuditEventsForSuccessfulAuditedRequests(Runnable filteredOp) throws Exception {
        // Given
        givenAccessAuditingEnabled();
        givenFilteredCrudpaqOperationSucceeds();

        // When
        filteredOp.run();

        // Then
        verifyPublishAccessEvent(AuditConstants.EventName.AM_ACCESS_ATTEMPT);
        verifyTryPublishAccessEvent(AuditConstants.EventName.AM_ACCESS_OUTCOME);
    }

    @Test(dataProvider = "auditedCrudpaqOperations")
    public void publishesAccessAndFailureAuditEventsForUnsuccessfulAuditedRequests(Runnable filteredOp) throws Exception {
        // Given
        givenAccessAuditingEnabled();
        givenFilteredCrudpaqOperationFails();

        // When
        filteredOp.run();

        // Then
        verifyPublishAccessEvent(AuditConstants.EventName.AM_ACCESS_ATTEMPT);
        verifyTryPublishAccessEvent(AuditConstants.EventName.AM_ACCESS_OUTCOME);
    }

    @Test(dataProvider = "unauditedCrudpaqOperations")
    public void publishesNoAuditsEventsForUnauditedRequests(Runnable filteredOp) throws Exception {
        // Given
        givenAccessAuditingEnabled();
        givenFilteredCrudpaqOperationSucceeds();

        // When
        filteredOp.run();

        // Then
        verifyNoEventsPublished();
    }

    @Test(dataProvider = "auditedCrudpaqOperations")
    public void publishesNoAuditsEventsIfAccessAuditingDisabled(Runnable filteredOp) throws Exception {
        // Given
        givenAccessAuditingDisabled();
        givenFilteredCrudpaqOperationSucceeds();

        // When
        filteredOp.run();

        // Then
        verifyNoEventsPublished();
    }

    @Test(dataProvider = "auditedCrudpaqOperations")
    public void cancelsOperationIfAccessAuditingGeneratesException(Runnable filteredOp) throws Exception {
        // Given
        givenAccessAuditingEnabled();
        givenAccessAuditingFails();

        // When
        filteredOp.run();

        // Then
        verifyPublishAccessEvent(AuditConstants.EventName.AM_ACCESS_ATTEMPT);
        verifyTryPublishAccessEventNotCalled();
        verifyNoMoreInteractions(filterChain);
    }

    private Context fakeContext() throws Exception {
        final Context httpContext = new HttpContext(
                jsonFromFile("/org/forgerock/openam/rest/fluent/httpContext.json"),
                AbstractAuditFilterTest.class.getClassLoader());
        final Subject callerSubject = new Subject();
        final Context securityContext = new SecurityContext(httpContext, null, null);
        final Context subjectContext = new SSOTokenContext(securityContext) {
            @Override
            public Subject getCallerSubject() {
                return callerSubject;
            }
        };
        final Context clientContext = ClientContext.newInternalClientContext(subjectContext);
        return new AuditInfoContext(clientContext, AuditConstants.Component.CREST);
    }

    private AuditEventFactory mockAuditEventFactory() {
        AuditEventFactory auditEventFactory = mock(AuditEventFactory.class);
        when(auditEventFactory.accessEvent(NO_REALM)).thenAnswer(new Answer<AMAccessAuditEventBuilder>() {
            @Override
            public AMAccessAuditEventBuilder answer(InvocationOnMock invocation) throws Throwable {
                return new AMAccessAuditEventBuilder();
            }
        });
        return auditEventFactory;
    }

    private void givenAccessAuditingEnabled() {
        given(auditEventPublisher.isAuditing(NO_REALM, ACCESS_TOPIC)).willReturn(true);
    }

    private void givenAccessAuditingDisabled() {
        given(auditEventPublisher.isAuditing(NO_REALM, ACCESS_TOPIC)).willReturn(false);
    }

    private void givenAccessAuditingFails() throws AuditException {
        doThrow(AuditException.class).when(auditEventPublisher).publish(eq(ACCESS_TOPIC), any(AuditEvent.class));
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

    private void verifyPublishAccessEvent(AuditConstants.EventName eventName) throws AuditException {
        ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventPublisher).publish(eq(ACCESS_TOPIC), auditEventCaptor.capture());
        assertThat(getEventName(auditEventCaptor)).isEqualTo(eventName.toString());
    }

    private void verifyTryPublishAccessEvent(AuditConstants.EventName eventName) {
        ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventPublisher).tryPublish(eq(ACCESS_TOPIC), auditEventCaptor.capture());
        assertThat(getEventName(auditEventCaptor)).isEqualTo(eventName.toString());
    }

    private String getEventName(ArgumentCaptor<AuditEvent> auditEventCaptor) {
        return auditEventCaptor.getValue().getValue().get(EVENT_NAME).asString();
    }

    private void verifyNoEventsPublished() throws AuditException {
        verifyPublishAccessEventNotCalled();
        verifyTryPublishAccessEventNotCalled();
    }

    private void verifyPublishAccessEventNotCalled() throws AuditException {
        verify(auditEventPublisher, never()).publish(any(String.class), any(AuditEvent.class));
    }

    private void verifyTryPublishAccessEventNotCalled() {
        verify(auditEventPublisher, never()).tryPublish(any(String.class), any(AuditEvent.class));
    }
}
