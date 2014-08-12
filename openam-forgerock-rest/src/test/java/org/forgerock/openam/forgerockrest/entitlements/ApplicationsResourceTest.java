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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.entitlements.wrappers.ApplicationManagerWrapper;
import org.forgerock.openam.forgerockrest.entitlements.wrappers.ApplicationTypeManagerWrapper;
import org.forgerock.openam.forgerockrest.entitlements.wrappers.ApplicationWrapper;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.util.promise.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * @since 12.0.0
 */
public class ApplicationsResourceTest {

    private ApplicationsResource applicationsResource;

    private Debug debug;
    private ApplicationManagerWrapper applicationManagerWrapper;
    private ApplicationTypeManagerWrapper applicationTypeManagerWrapper;
    private ResultHandler<Resource> mockResultHandler;
    private ApplicationWrapper applicationWrapper;

    @BeforeMethod
    public void setUp() {

        debug = mock(Debug.class);
        applicationManagerWrapper = mock(ApplicationManagerWrapper.class);
        applicationTypeManagerWrapper = mock(ApplicationTypeManagerWrapper.class);
        applicationWrapper = mock(ApplicationWrapper.class);

        applicationsResource = new ApplicationsResource(debug, applicationManagerWrapper,
                applicationTypeManagerWrapper) {
            @Override
            protected ApplicationWrapper createApplicationWrapper(JsonValue jsonValue, Subject mySubject, String realm)
                    throws IOException, EntitlementException {
                return applicationWrapper;
            }
        };

        mockResultHandler = mock(ResultHandler.class);
    }

    @Test
    public void shouldReturnNullIfSubjectNullOnCreate() {
        //given
        SubjectContext mockSubjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext, "REALM");
        ServerContext mockServerContext = new ServerContext(realmContext);
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);

        given(mockSubjectContext.getCallerSubject()).willReturn(null);

        //when
        applicationsResource.createInstance(mockServerContext, mockCreateRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler, times(1)).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.INTERNAL_ERROR);
    }

    @Test
    public void shouldThrowInternalErrorIfApplicationWrapperCannotBeCreated() {
        //given
        SubjectContext mockSubjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext, "REALM");
        ServerContext mockServerContext = new ServerContext(realmContext);
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();

        given(mockSubjectContext.getCallerSubject()).willReturn(subject);

        applicationsResource = new ApplicationsResource(debug, applicationManagerWrapper,
                applicationTypeManagerWrapper) {
            @Override
            protected ApplicationWrapper createApplicationWrapper(JsonValue jsonValue, Subject mySubject, String realm)
                    throws IOException, EntitlementException {
                throw new IOException("");
            }
        };

        //when
        applicationsResource.createInstance(mockServerContext, mockCreateRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.INTERNAL_ERROR);
    }

    @Test
    public void shouldThrowBadRequestIfNoApplicationTypeSpecifiedInRequest() {
        //given
        SubjectContext mockSubjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext, "/");
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();

        given(mockSubjectContext.getCallerSubject()).willReturn(subject);

        applicationsResource = new ApplicationsResource(debug, applicationManagerWrapper,
                applicationTypeManagerWrapper) {
            @Override
            protected ApplicationWrapper createApplicationWrapper(JsonValue jsonValue, Subject mySubject, String realm)
                    throws IOException, EntitlementException {
                throw new EntitlementException(1);
            }
        };

        //when
        applicationsResource.createInstance(realmContext, mockCreateRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.BAD_REQUEST);
    }

    @Test
    public void shouldThrowInternalErrorIfResourceWillNotSave() throws EntitlementException {
        //given
        SubjectContext mockSubjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext, "/");
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();

        given(mockSubjectContext.getCallerSubject()).willReturn(subject);
        doThrow(EntitlementException.class).when
                (applicationManagerWrapper).saveApplication(any(Subject.class), anyString(), any(Application.class));

        //when
        applicationsResource.createInstance(realmContext, mockCreateRequest, mockResultHandler);


        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.INTERNAL_ERROR);
    }

    @Test
    public void shouldThrowIOExceptionIfCannotReturnResource() throws IOException {
        //given
        SubjectContext mockSubjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext, "/");
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = null;

        given(mockSubjectContext.getCallerSubject()).willReturn(subject);
        doThrow(IOException.class).when(applicationWrapper).toJsonValue();

        //when
        applicationsResource.createInstance(realmContext, mockCreateRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.INTERNAL_ERROR);
    }

    @Test
    public void shouldCreateApplication() {
        //given
        SubjectContext mockSubjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext, "/");
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject mockSubject = new Subject();
        Application mockApplication = mock(Application.class);

        given(mockSubjectContext.getCallerSubject()).willReturn(mockSubject);
        given(applicationWrapper.getApplication()).willReturn(mockApplication);

        //when
        applicationsResource.createInstance(realmContext, mockCreateRequest, mockResultHandler);

        //then
        verify(mockResultHandler, times(1)).handleResult(any(Resource.class));
    }

    @Test
    public void shouldThrowInternalErrorIfSubjectNotFoundOnRead() {
        // Given
        SubjectContext mockSubjectContext = mock(SubjectContext.class);
        ServerContext context = new ServerContext(mockSubjectContext);
        given(mockSubjectContext.getCallerSubject()).willReturn(null);

        // When
        applicationsResource.readInstance(context, null, null, mockResultHandler);

        // Then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.INTERNAL_ERROR);
    }

    @Test
    public void shouldUseResourceIDForFetchingApplicationOnRead() throws EntitlementException {
        // Given
        String resourceID = "ferret";

        SubjectContext mockSubjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext, "badger");
        ServerContext serverContext = new ServerContext(realmContext);

        Subject subject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(subject);

        Application mockApplication = mock(Application.class);
        given(applicationManagerWrapper.getApplication(any(Subject.class), anyString(), anyString())).willReturn(mockApplication);

        // When
        applicationsResource.readInstance(serverContext, resourceID, null, mockResultHandler);

        // Then
        verify(applicationManagerWrapper).getApplication(any(Subject.class), anyString(), eq(resourceID));
    }

    @Test
    public void shouldUseRealmFromContextSubjectOnRead() throws EntitlementException {
        // Given
        String resourceID = "ferret";
        String realmID = "badger";

        SubjectContext mockSubjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext, realmID);
        ServerContext serverContext = new ServerContext(realmContext);

        Subject subject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(subject);

        Application mockApplication = mock(Application.class);
        given(applicationManagerWrapper.getApplication(any(Subject.class), anyString(), anyString())).willReturn(mockApplication);

        // When
        applicationsResource.readInstance(serverContext, resourceID, null, mockResultHandler);

        // Then
        verify(applicationManagerWrapper).getApplication(any(Subject.class), eq(realmID), anyString());
    }

    @Test
    public void shouldUseSubjectFromContextOnRead() throws EntitlementException {
        // Given
        String resourceID = "ferret";

        SubjectContext mockSubjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext, "badger");
        ServerContext serverContext = new ServerContext(realmContext);

        Subject subject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(subject);

        Application mockApplication = mock(Application.class);
        given(applicationManagerWrapper.getApplication(any(Subject.class), anyString(), anyString())).willReturn(mockApplication);

        // When
        applicationsResource.readInstance(serverContext, resourceID, null, mockResultHandler);

        // Then
        verify(applicationManagerWrapper).getApplication(eq(subject), anyString(), anyString());
    }

    @Test
    public void shouldDeleteInstance() throws EntitlementException {

        //Given
        SubjectContext subjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(subjectContext, "REALM");
        ServerContext context = new ServerContext(realmContext);
        String resourceId = "RESOURCE_ID";
        DeleteRequest request = mock(DeleteRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = new Subject();

        given(subjectContext.getCallerSubject()).willReturn(subject);

        //When
        applicationsResource.deleteInstance(context, resourceId, request, handler);

        //Then
        verify(applicationManagerWrapper).deleteApplication(subject, "REALM", resourceId);
        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(handler).handleResult(resourceCaptor.capture());
        Resource resource = resourceCaptor.getValue();
        assertEquals(resource.getId(), resourceId);
        assertEquals(resource.getRevision(), "0");
        assertEquals(resource.getContent(), json(object()));
    }

    @Test
    public void shouldNotDeleteInstanceWhenSubjectIsNull() throws EntitlementException {

        //Given
        SubjectContext subjectContext = mock(SubjectContext.class);
        ServerContext context = new ServerContext(subjectContext);
        String resourceId = "RESOURCE_ID";
        DeleteRequest request = mock(DeleteRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = null;

        given(subjectContext.getCallerSubject()).willReturn(subject);

        //When
        applicationsResource.deleteInstance(context, resourceId, request, handler);

        //Then
        verify(applicationManagerWrapper, never()).deleteApplication(subject, "REALM", resourceId);
        ArgumentCaptor<ResourceException> resourceExceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(resourceExceptionCaptor.capture());
        ResourceException exception = resourceExceptionCaptor.getValue();
        assertEquals(exception.getCode(), 500);
        assertEquals(exception.getReason(), "Internal Server Error");
    }

    @Test
    public void deleteInstanceShouldHandleFailedDeleteApplication() throws EntitlementException {

        //Given
        SubjectContext subjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(subjectContext, "REALM");
        ServerContext context = new ServerContext(realmContext);
        String resourceId = "RESOURCE_ID";
        DeleteRequest request = mock(DeleteRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = new Subject();

        given(subjectContext.getCallerSubject()).willReturn(subject);
        doThrow(EntitlementException.class).when(applicationManagerWrapper)
                .deleteApplication(subject, "REALM", resourceId);

        //When
        applicationsResource.deleteInstance(context, resourceId, request, handler);

        //Then
        verify(applicationManagerWrapper).deleteApplication(subject, "REALM", resourceId);
        ArgumentCaptor<ResourceException> resourceExceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(resourceExceptionCaptor.capture());
        ResourceException exception = resourceExceptionCaptor.getValue();
        assertEquals(exception.getCode(), 500);
        assertEquals(exception.getReason(), "Internal Server Error");
    }

    @Test
    public void shouldUpdateInstance() throws EntitlementException, IOException {

        //Given
        SubjectContext subjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(subjectContext, "REALM");
        ServerContext context = new ServerContext(realmContext);
        String resourceId = "RESOURCE_ID";
        UpdateRequest request = mock(UpdateRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = new Subject();
        JsonValue content = mock(JsonValue.class);
        Application application = mock(Application.class);
        Application newApplication = mock(Application.class);
        JsonValue response = mock(JsonValue.class);

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(request.getContent()).willReturn(content);
        given(applicationManagerWrapper.getApplication(subject, "REALM", resourceId)).willReturn(application);
        given(applicationWrapper.getName()).willReturn("APP_NAME");
        given(applicationWrapper.getApplication()).willReturn(newApplication);
        given(newApplication.getLastModifiedDate()).willReturn(1000L);
        given(applicationWrapper.toJsonValue()).willReturn(response);

        //When
        applicationsResource.updateInstance(context, resourceId, request, handler);

        //Then
        verify(applicationManagerWrapper)
                .updateApplication(application, applicationWrapper.getApplication(), subject, "REALM");
        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(handler).handleResult(resourceCaptor.capture());
        Resource resource = resourceCaptor.getValue();
        assertEquals(resource.getId(), "APP_NAME");
        assertEquals(resource.getRevision(), "1000");
        assertEquals(resource.getContent(), response);
    }

    @Test
    public void updateInstanceShouldReturnServerInternalExceptionWhenApplicationToJson() throws EntitlementException,
            IOException {

        //Given
        SubjectContext subjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(subjectContext, "REALM");
        ServerContext context = new ServerContext(realmContext);
        String resourceId = "RESOURCE_ID";
        UpdateRequest request = mock(UpdateRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = new Subject();
        JsonValue content = mock(JsonValue.class);
        Application application = mock(Application.class);
        Application newApplication = mock(Application.class);

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(request.getContent()).willReturn(content);
        given(applicationManagerWrapper.getApplication(subject, "REALM", resourceId)).willReturn(application);
        given(applicationWrapper.getName()).willReturn("APP_NAME");
        given(applicationWrapper.getApplication()).willReturn(newApplication);
        given(newApplication.getLastModifiedDate()).willReturn(1000L);
        doThrow(IOException.class).when(applicationWrapper).toJsonValue();

        //When
        applicationsResource.updateInstance(context, resourceId, request, handler);

        //Then
        verify(applicationManagerWrapper)
                .updateApplication(application, applicationWrapper.getApplication(), subject, "REALM");
        ArgumentCaptor<ResourceException> resourceExceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(resourceExceptionCaptor.capture());
        ResourceException exception = resourceExceptionCaptor.getValue();
        assertEquals(exception.getCode(), 500);
        assertEquals(exception.getReason(), "Internal Server Error");
    }

    @Test
    public void updateInstanceShouldReturnInternalServerErrorWhenUpdatingFails() throws EntitlementException {

        //Given
        SubjectContext subjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(subjectContext, "REALM");
        ServerContext context = new ServerContext(realmContext);
        String resourceId = "RESOURCE_ID";
        UpdateRequest request = mock(UpdateRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = new Subject();
        JsonValue content = mock(JsonValue.class);
        Application application = mock(Application.class);

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(request.getContent()).willReturn(content);
        given(applicationManagerWrapper.getApplication(subject, "REALM", resourceId)).willReturn(application);
        doThrow(EntitlementException.class).when(applicationManagerWrapper)
                .updateApplication(any(Application.class),any(Application.class), any(Subject.class), anyString());

        //When
        applicationsResource.updateInstance(context, resourceId, request, handler);

        //Then
        ArgumentCaptor<ResourceException> resourceExceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(resourceExceptionCaptor.capture());
        ResourceException exception = resourceExceptionCaptor.getValue();
        assertEquals(exception.getCode(), 500);
        assertEquals(exception.getReason(), "Internal Server Error");
    }

    @Test
    public void shouldNotUpdateInstanceIfApplicationNotFound() throws EntitlementException {

        //Given
        SubjectContext subjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(subjectContext, "REALM");
        ServerContext context = new ServerContext(realmContext);
        String resourceId = "RESOURCE_ID";
        UpdateRequest request = mock(UpdateRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = new Subject();
        JsonValue content = mock(JsonValue.class);
        Application application = null;

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(request.getContent()).willReturn(content);
        given(applicationManagerWrapper.getApplication(subject, "REALM", resourceId)).willReturn(application);

        //When
        applicationsResource.updateInstance(context, resourceId, request, handler);

        //Then
        ArgumentCaptor<ResourceException> resourceExceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(resourceExceptionCaptor.capture());
        ResourceException exception = resourceExceptionCaptor.getValue();
        assertEquals(exception.getCode(), 404);
        assertEquals(exception.getReason(), "Not Found");
    }

    @Test
    public void shouldNotUpdateInstanceWhenSubjectIsNull() throws EntitlementException {

        //Given
        SubjectContext subjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(subjectContext, "REALM");
        ServerContext context = new ServerContext(realmContext);
        String resourceId = "RESOURCE_ID";
        UpdateRequest request = mock(UpdateRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = null;

        given(subjectContext.getCallerSubject()).willReturn(subject);

        //When
        applicationsResource.updateInstance(context, resourceId, request, handler);

        //Then
        verifyZeroInteractions(applicationManagerWrapper);
        ArgumentCaptor<ResourceException> resourceExceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(resourceExceptionCaptor.capture());
        ResourceException exception = resourceExceptionCaptor.getValue();
        assertEquals(exception.getCode(), 500);
        assertEquals(exception.getReason(), "Internal Server Error");
    }

    @Test
    public void shouldReturnThreeResultsOnQuery()
            throws EntitlementException, IllegalAccessException, InstantiationException {

        // Override the creation of the application wrapper so to return a mocked version.
        applicationsResource = new ApplicationsResource(
                debug, applicationManagerWrapper, applicationTypeManagerWrapper) {

            @Override
            protected ApplicationWrapper createApplicationWrapper(
                    Application application, ApplicationTypeManagerWrapper type) {

                ApplicationWrapper wrapper = mock(ApplicationWrapper.class);
                String appName = application.getName();
                given(wrapper.getName()).willReturn(appName);

                try {
                    JsonValue jsonValue = mock(JsonValue.class);
                    given(wrapper.toJsonValue()).willReturn(jsonValue);
                } catch (IOException e) {
                    fail();
                }

                return wrapper;
            }
        };


        // Given
        SubjectContext mockSubjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext, "/abc");
        ServerContext serverContext = new ServerContext(realmContext);

        // Set the page size to be three starting from the second item.
        QueryRequest request = mock(QueryRequest.class);
        given(request.getPageSize()).willReturn(3);
        given(request.getPagedResultsOffset()).willReturn(1);

        QueryResultHandler handler = mock(QueryResultHandler.class);
        given(handler.handleResource(any(Resource.class))).willReturn(true);

        Subject subject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(subject);

        Set<String> appNames = CollectionUtils.asOrderedSet("app1", "app2", "app3", "app4", "app5");
        given(applicationManagerWrapper.getApplicationNames(eq(subject), eq("/abc"))).willReturn(appNames);

        for (String appName : appNames) {
            Application app = mock(Application.class);
            given(app.getName()).willReturn(appName);
            given(applicationManagerWrapper
                    .getApplication(eq(subject), eq("/abc"), eq(appName))).willReturn(app);
        }

        // When
        applicationsResource.queryCollection(serverContext, request, handler);

        // Then
        verify(applicationManagerWrapper).getApplicationNames(eq(subject), eq("/abc"));
        verify(applicationManagerWrapper, times(5)).getApplication(eq(subject), eq("/abc"), anyString());

        ArgumentCaptor<Resource> resourceCapture = ArgumentCaptor.forClass(Resource.class);
        verify(handler, times(3)).handleResource(resourceCapture.capture());

        List<String> selectedApps = CollectionUtils
                .transformList(resourceCapture.getAllValues(), new ResourceToIdMapper());
        assertThat(selectedApps).containsOnly("app2", "app3", "app4");

        ArgumentCaptor<QueryResult> resultCapture = ArgumentCaptor.forClass(QueryResult.class);
        verify(handler).handleResult(resultCapture.capture());

        QueryResult result = resultCapture.getValue();
        assertThat(result.getRemainingPagedResults()).isEqualTo(1);
    }

    @Test
    public void shouldHandleApplicationFindFailure() throws EntitlementException {
        // Given
        SubjectContext mockSubjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext, "/abc");
        ServerContext serverContext = new ServerContext(realmContext);

        // Set the page size to be three starting from the second item.
        QueryRequest request = mock(QueryRequest.class);
        given(request.getPageSize()).willReturn(3);
        given(request.getPagedResultsOffset()).willReturn(1);

        QueryResultHandler handler = mock(QueryResultHandler.class);
        given(handler.handleResource(any(Resource.class))).willReturn(true);

        Subject subject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(subject);

        EntitlementException exception = new EntitlementException(EntitlementException.APP_RETRIEVAL_ERROR);
        given(applicationManagerWrapper.getApplicationNames(eq(subject), eq("/abc"))).willThrow(exception);

        // When
        applicationsResource.queryCollection(serverContext, request, handler);

        // Then
        ArgumentCaptor<ResourceException> exceptionCapture = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(exceptionCapture.capture());

        ResourceException resourceException = exceptionCapture.getValue();
        assertThat(resourceException.getCode()).isEqualTo(ResourceException.INTERNAL_ERROR);
    }


    @Test
    public void shouldHandleJsonParsingFailure() throws EntitlementException {
        // Override the creation of the application wrapper so to return a mocked version.
        applicationsResource = new ApplicationsResource(
                debug, applicationManagerWrapper, applicationTypeManagerWrapper) {

            @Override
            protected ApplicationWrapper createApplicationWrapper(
                    Application application, ApplicationTypeManagerWrapper type) {

                ApplicationWrapper wrapper = mock(ApplicationWrapper.class);
                String appName = application.getName();
                given(wrapper.getName()).willReturn(appName);

                try {
                    // Throws an IOException when attempting to parse the json.
                    IOException ioException = new IOException();
                    given(wrapper.toJsonValue()).willThrow(ioException);
                } catch (IOException e) {
                    fail();
                }

                return wrapper;
            }
        };


        // Given
        SubjectContext mockSubjectContext = mock(SubjectContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext, "/abc");
        ServerContext serverContext = new ServerContext(realmContext);

        // Set the page size to be three starting from the second item.
        QueryRequest request = mock(QueryRequest.class);
        given(request.getPageSize()).willReturn(3);
        given(request.getPagedResultsOffset()).willReturn(1);

        QueryResultHandler handler = mock(QueryResultHandler.class);
        given(handler.handleResource(any(Resource.class))).willReturn(true);

        Subject subject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(subject);

        Set<String> appNames = CollectionUtils.asOrderedSet("app1", "app2", "app3", "app4", "app5");
        given(applicationManagerWrapper.getApplicationNames(eq(subject), eq("/abc"))).willReturn(appNames);

        for (String appName : appNames) {
            Application app = mock(Application.class);
            given(app.getName()).willReturn(appName);
            given(applicationManagerWrapper
                    .getApplication(eq(subject), eq("/abc"), eq(appName))).willReturn(app);
        }

        // When
        applicationsResource.queryCollection(serverContext, request, handler);

        // Then
        verify(applicationManagerWrapper).getApplicationNames(eq(subject), eq("/abc"));
        verify(applicationManagerWrapper, times(5)).getApplication(eq(subject), eq("/abc"), anyString());

        ArgumentCaptor<ResourceException> exceptionCapture = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(exceptionCapture.capture());

        ResourceException resourceException = exceptionCapture.getValue();
        assertThat(resourceException.getCode()).isEqualTo(ResourceException.INTERNAL_ERROR);
    }

    /**
     * Maps a resource object to its string Id.
     *
     * @since 12.0.0
     */
    private static class ResourceToIdMapper implements Function<Resource, String, NeverThrowsException> {

        @Override
        public String apply(Resource resource) throws NeverThrowsException {
            return resource.getId();
        }

    }

}
