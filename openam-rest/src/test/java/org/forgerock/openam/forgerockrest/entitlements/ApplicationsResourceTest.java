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
import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.debug.Debug;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import static org.fest.assertions.Assertions.assertThat;
import org.forgerock.json.fluent.JsonValue;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.QueryFilter;
import static org.forgerock.json.resource.QueryFilter.alwaysFalse;
import static org.forgerock.json.resource.QueryFilter.alwaysTrue;
import static org.forgerock.json.resource.QueryFilter.and;
import static org.forgerock.json.resource.QueryFilter.comparisonFilter;
import static org.forgerock.json.resource.QueryFilter.equalTo;
import static org.forgerock.json.resource.QueryFilter.present;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SortKey;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.entitlements.query.AttributeType;
import org.forgerock.openam.forgerockrest.entitlements.query.QueryAttribute;
import org.forgerock.openam.forgerockrest.entitlements.wrappers.ApplicationManagerWrapper;
import org.forgerock.openam.forgerockrest.entitlements.wrappers.ApplicationTypeManagerWrapper;
import org.forgerock.openam.forgerockrest.entitlements.wrappers.ApplicationWrapper;
import org.forgerock.openam.forgerockrest.guice.ForgerockRestGuiceModule;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import static org.forgerock.openam.utils.CollectionUtils.asOrderedSet;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.forgerock.openam.utils.CollectionUtils.transformList;
import org.forgerock.util.promise.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.mockito.ArgumentCaptor;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import org.testng.Assert;
import static org.testng.Assert.fail;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @since 12.0.0
 */
public class ApplicationsResourceTest {

    private static final String STRING_ATTRIBUTE = "stringAttribute";
    private static final String DATE_ATTRIBUTE = "dateAttribute";
    private static final String NUMERIC_ATTRIBUTE = "numberAttribute";

    private ApplicationsResource applicationsResource;

    private Debug debug;
    private ApplicationManagerWrapper applicationManagerWrapper;
    private ApplicationTypeManagerWrapper applicationTypeManagerWrapper;
    private ResultHandler<Resource> mockResultHandler;
    private ApplicationWrapper applicationWrapper;
    private Map<String, QueryAttribute> queryAttributes;
    private EntitlementsResourceErrorHandler resourceErrorHandler
            = new EntitlementsResourceErrorHandler(ForgerockRestGuiceModule.getEntitlementsErrorHandlers());

    @BeforeMethod
    public void setUp() {

        debug = mock(Debug.class);
        applicationManagerWrapper = mock(ApplicationManagerWrapper.class);
        applicationTypeManagerWrapper = mock(ApplicationTypeManagerWrapper.class);
        applicationWrapper = mock(ApplicationWrapper.class);

        queryAttributes = new HashMap<String, QueryAttribute>();
        queryAttributes.put(STRING_ATTRIBUTE, new QueryAttribute(AttributeType.STRING, STRING_ATTRIBUTE));
        queryAttributes.put(NUMERIC_ATTRIBUTE, new QueryAttribute(AttributeType.NUMBER, NUMERIC_ATTRIBUTE));
        queryAttributes.put(DATE_ATTRIBUTE, new QueryAttribute(AttributeType.TIMESTAMP, DATE_ATTRIBUTE));

        applicationsResource = new ApplicationsResource(
                debug, applicationManagerWrapper, applicationTypeManagerWrapper, queryAttributes, resourceErrorHandler) {

            @Override
            protected ApplicationWrapper createApplicationWrapper(JsonValue jsonValue, Subject mySubject)
                    throws EntitlementException {
                return applicationWrapper;
            }
        };

        mockResultHandler = mock(ResultHandler.class);
    }

    @Test
    public void shouldReturnNullIfSubjectNullOnCreate() {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.addSubRealm("REALM", "REALM");
        ServerContext mockServerContext = new ServerContext(realmContext);
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);

        given(mockSSOTokenContext.getCallerSubject()).willReturn(null);

        //when
        applicationsResource.createInstance(mockServerContext, mockCreateRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler, times(1)).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.BAD_REQUEST);
    }

    @Test
    public void shouldThrowBadRequestIfApplicationWrapperCannotBeCreated() {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.addSubRealm("REALM", "REALM");
        ServerContext mockServerContext = new ServerContext(realmContext);
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);

        applicationsResource = new ApplicationsResource(
                debug, applicationManagerWrapper, applicationTypeManagerWrapper, queryAttributes, resourceErrorHandler) {

            @Override
            protected ApplicationWrapper createApplicationWrapper(JsonValue jsonValue, Subject mySubject)
                    throws EntitlementException {
                throw new EntitlementException(317);
            }
        };

        //when
        applicationsResource.createInstance(mockServerContext, mockCreateRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.BAD_REQUEST);
    }

    @Test //potential to be BAD_REQUEST?
    public void shouldThrowInternalIfApplicationClassCannotBeInstantiated() {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.addSubRealm("/", "/");
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);

        applicationsResource = new ApplicationsResource(
                debug, applicationManagerWrapper, applicationTypeManagerWrapper, queryAttributes, resourceErrorHandler) {

            @Override
            protected ApplicationWrapper createApplicationWrapper(JsonValue jsonValue, Subject mySubject)
                    throws EntitlementException {
                throw new EntitlementException(6);
            }
        };

        //when
        applicationsResource.createInstance(realmContext, mockCreateRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.INTERNAL_ERROR);
    }


    @Test
    public void shouldThrowBadRequestIfRealmOfRequestAndResourceNotEqualCreate() throws EntitlementException {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.addSubRealm("/", "/");
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();
        Application mockApplication = mock(Application.class);

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(applicationWrapper.getApplication()).willReturn(mockApplication);
        given(mockApplication.getRealm()).willReturn("NOT SLASH");

        //when
        applicationsResource.createInstance(realmContext, mockCreateRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.BAD_REQUEST);
    }

    @Test
    public void shouldThrowBadRequestIfRealmOfRequestAndResourceNotEqualUpdate() throws EntitlementException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.addSubRealm("REALM", "REALM");
        ServerContext context = new ServerContext(realmContext);
        String resourceId = "iPlanetAMWebAgentService";
        UpdateRequest request = mock(UpdateRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = new Subject();
        JsonValue content = mock(JsonValue.class);
        Application application = mock(Application.class);
        Application newApplication = mock(Application.class);

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(request.getContent()).willReturn(content);
        given(applicationManagerWrapper.getApplication(subject, "/REALM", resourceId)).willReturn(application);
        given(applicationWrapper.getName()).willReturn("APP_NAME");
        given(applicationWrapper.getApplication()).willReturn(newApplication);
        given(newApplication.getRealm()).willReturn("NOT REALM");

        //When
        applicationsResource.updateInstance(context, resourceId, request, handler);

        //Then
        ArgumentCaptor<ResourceException> resourceExceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(resourceExceptionCaptor.capture());
        ResourceException exception = resourceExceptionCaptor.getValue();
        Assert.assertEquals(exception.getCode(), 400);
        Assert.assertEquals(exception.getReason(), "Bad Request");
    }

    @Test
    public void shouldThrowInternalErrorIfResourceWillNotSave() throws EntitlementException {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.addSubRealm("/", "/");
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();
        Application mockApplication = mock(Application.class);

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(applicationWrapper.getApplication()).willReturn(mockApplication);
        given(mockApplication.getRealm()).willReturn("/");
        given(mockApplication.getName()).willReturn("newApplication");
        doThrow(new EntitlementException(1)).when
                (applicationManagerWrapper).saveApplication(any(Subject.class), any(Application.class));

        //when
        applicationsResource.createInstance(realmContext, mockCreateRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.INTERNAL_ERROR);
    }

    @Test
    public void shouldReturnBadRequestIfCannotReturnResource() throws EntitlementException {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.addSubRealm("/", "/");
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = null;

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        doThrow(new EntitlementException(1)).when(applicationWrapper).toJsonValue();

        //when
        applicationsResource.createInstance(realmContext, mockCreateRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.BAD_REQUEST);
    }

    @Test
    public void shouldCreateApplication() {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.addSubRealm("/", "/");
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject mockSubject = new Subject();
        Application mockApplication = mock(Application.class);

        applicationsResource = new ApplicationsResource(
                debug, applicationManagerWrapper, applicationTypeManagerWrapper, queryAttributes, resourceErrorHandler) {

            @Override
            protected ApplicationWrapper createApplicationWrapper(JsonValue jsonValue, Subject mySubject)
                    throws EntitlementException {
                return applicationWrapper;
            }

            @Override
            protected ApplicationWrapper createApplicationWrapper(Application application,
                                                                  ApplicationTypeManagerWrapper type) {
                return applicationWrapper;
            }
        };

        given(mockSSOTokenContext.getCallerSubject()).willReturn(mockSubject);
        given(applicationWrapper.getApplication()).willReturn(mockApplication);
        given(mockApplication.getName()).willReturn("newApplication");
        given(mockApplication.getRealm()).willReturn("/");

        //when
        applicationsResource.createInstance(realmContext, mockCreateRequest, mockResultHandler);

        //then
        verify(mockResultHandler, times(1)).handleResult(any(Resource.class));
    }


    @Test
    public void shouldNotCreateApplicationWithInvalidCharactersInName() {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.addSubRealm("/", "/");
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject mockSubject = new Subject();
        Application mockApplication = mock(Application.class);

        applicationsResource = new ApplicationsResource(
                debug, applicationManagerWrapper, applicationTypeManagerWrapper, queryAttributes, resourceErrorHandler) {

            @Override
            protected ApplicationWrapper createApplicationWrapper(JsonValue jsonValue, Subject mySubject)
                    throws EntitlementException {
                return applicationWrapper;
            }

            @Override
            protected ApplicationWrapper createApplicationWrapper(Application application,
                                                                  ApplicationTypeManagerWrapper type) {
                return applicationWrapper;
            }
        };

        given(mockSSOTokenContext.getCallerSubject()).willReturn(mockSubject);
        given(applicationWrapper.getApplication()).willReturn(mockApplication);
        given(mockApplication.getName()).willReturn("new+application");
        given(mockApplication.getRealm()).willReturn("/");

        //when
        applicationsResource.createInstance(realmContext, mockCreateRequest, mockResultHandler);

        // Then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.BAD_REQUEST);
    }

    @Test
    public void shouldThrowBadRequestIfSubjectNotFoundOnRead() {
        // Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.addSubRealm("REALM", "REALM");
        ServerContext context = new ServerContext(realmContext);
        given(subjectContext.getCallerSubject()).willReturn(null);

        // When
        applicationsResource.readInstance(context, null, null, mockResultHandler);

        // Then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.BAD_REQUEST);
    }

    @Test
    public void shouldUseResourceIDForFetchingApplicationOnRead() throws EntitlementException {
        // Given
        String resourceID = "iPlanetAMWebAgentService";

        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.addSubRealm("badger", "badger");
        ServerContext serverContext = new ServerContext(realmContext);

        Subject subject = new Subject();
        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);

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
        String realmID = "/badger";

        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.addSubRealm(realmID, realmID);
        ServerContext serverContext = new ServerContext(realmContext);

        Subject subject = new Subject();
        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);

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

        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.addSubRealm("badger", "badger");
        ServerContext serverContext = new ServerContext(realmContext);

        Subject subject = new Subject();
        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);

        Application mockApplication = mock(Application.class);
        given(applicationManagerWrapper.getApplication(any(Subject.class), anyString(), anyString())).willReturn(mockApplication);

        // When
        applicationsResource.readInstance(serverContext, resourceID, null, mockResultHandler);

        // Then
        verify(applicationManagerWrapper).getApplication(eq(subject), anyString(), anyString());
    }

    @Test
    public void shouldReturnNotFoundDeleteInstance() throws EntitlementException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.addSubRealm("REALM", "REALM");
        ServerContext context = new ServerContext(realmContext);
        String resourceId = "RESOURCE_ID";
        DeleteRequest request = mock(DeleteRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = new Subject();

        given(subjectContext.getCallerSubject()).willReturn(subject);

        //When
        applicationsResource.deleteInstance(context, resourceId, request, handler);

        //Then
        verify(applicationManagerWrapper, never()).deleteApplication(subject, "REALM", resourceId);
        ArgumentCaptor<ResourceException> resourceExceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(resourceExceptionCaptor.capture());
        ResourceException exception = resourceExceptionCaptor.getValue();
        Assert.assertEquals(exception.getCode(), 404);
        Assert.assertEquals(exception.getReason(), "Not Found");
    }

    @Test
    public void shouldDeleteInstance() throws EntitlementException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.addSubRealm("REALM", "REALM");
        ServerContext context = new ServerContext(realmContext);
        String resourceId = "iPlanetAMWebAgentService";
        DeleteRequest request = mock(DeleteRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = new Subject();
        Application mockApplication = mock(Application.class);

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(applicationManagerWrapper
                .getApplication(any(Subject.class), anyString(), anyString())).willReturn(mockApplication);

        //When
        applicationsResource.deleteInstance(context, resourceId, request, handler);

        //Then
        verify(applicationManagerWrapper).deleteApplication(subject, "/REALM", resourceId);
        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(handler).handleResult(resourceCaptor.capture());
        Resource resource = resourceCaptor.getValue();
        Assert.assertEquals(resource.getId(), resourceId);
        Assert.assertEquals(resource.getRevision(), "0");
        Assert.assertEquals(resource.getContent(), json(object()));
    }

    @Test
    public void shouldNotDeleteInstanceWhenSubjectIsNull() throws EntitlementException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
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
        Assert.assertEquals(exception.getCode(), 400);
        Assert.assertEquals(exception.getReason(), "Bad Request");
    }

    @Test
    public void deleteInstanceShouldHandleFailedDeleteApplication() throws EntitlementException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.addSubRealm("REALM", "REALM");
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
        verify(applicationManagerWrapper, never()).deleteApplication(subject, "REALM", resourceId);
        ArgumentCaptor<ResourceException> resourceExceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(resourceExceptionCaptor.capture());
        ResourceException exception = resourceExceptionCaptor.getValue();
        Assert.assertEquals(exception.getCode(), 404);
        Assert.assertEquals(exception.getReason(), "Not Found");
    }

    @Test
    public void shouldUpdateInstance() throws EntitlementException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.addSubRealm("REALM", "REALM");
        ServerContext context = new ServerContext(realmContext);
        String resourceId = "iPlanetAMWebAgentService";
        UpdateRequest request = mock(UpdateRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = new Subject();
        JsonValue content = mock(JsonValue.class);
        Application application = mock(Application.class);
        Application newApplication = mock(Application.class);
        JsonValue response = mock(JsonValue.class);

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(request.getContent()).willReturn(content);
        given(applicationManagerWrapper.getApplication(subject, "/REALM", resourceId)).willReturn(application);
        given(applicationWrapper.getName()).willReturn("APP_NAME");
        given(applicationWrapper.getApplication()).willReturn(newApplication);
        given(newApplication.getLastModifiedDate()).willReturn(1000L);
        given(applicationWrapper.toJsonValue()).willReturn(response);
        given(newApplication.getRealm()).willReturn("/REALM");

        //When
        applicationsResource.updateInstance(context, resourceId, request, handler);

        //Then
        verify(applicationManagerWrapper)
                .updateApplication(application, applicationWrapper.getApplication(), subject);
        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(handler).handleResult(resourceCaptor.capture());
        Resource resource = resourceCaptor.getValue();
        Assert.assertEquals(resource.getId(), "APP_NAME");
        Assert.assertEquals(resource.getRevision(), "1000");
        Assert.assertEquals(resource.getContent(), response);
    }

    @Test
    public void updateInstanceShouldReturnConflictExceptionWhenApplicationNameAlreadyExists() throws
            EntitlementException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.addSubRealm("REALM", "REALM");
        ServerContext context = new ServerContext(realmContext);
        String resourceId = "iPlanetAMWebAgentService";
        UpdateRequest request = mock(UpdateRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = new Subject();
        JsonValue content = mock(JsonValue.class);
        Application application = mock(Application.class);
        Application newApplication = mock(Application.class);

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(request.getContent()).willReturn(content);
        given(applicationManagerWrapper.getApplication(subject, "/REALM", resourceId))
                .willReturn(application);
        given(applicationManagerWrapper.getApplication(subject, "/REALM", "APP_NAME"))
                .willReturn(application);
        given(applicationWrapper.getName()).willReturn("APP_NAME");
        given(applicationWrapper.getApplication()).willReturn(newApplication);
        given(newApplication.getRealm()).willReturn("/REALM");
        given(newApplication.getLastModifiedDate()).willReturn(1000L);
        doThrow(EntitlementException.class).when(applicationWrapper).toJsonValue();

        //When
        applicationsResource.updateInstance(context, resourceId, request, handler);

        //Then
        ArgumentCaptor<ResourceException> resourceExceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(resourceExceptionCaptor.capture());
        ResourceException exception = resourceExceptionCaptor.getValue();
        Assert.assertEquals(exception.getCode(), 409);
        Assert.assertEquals(exception.getReason(), "Conflict");
    }

    @Test
    public void updateInstanceShouldReturnServerInternalExceptionWhenApplicationToJson() throws EntitlementException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.addSubRealm("REALM", "REALM");
        ServerContext context = new ServerContext(realmContext);
        String resourceId = "iPlanetAMWebAgentService";
        UpdateRequest request = mock(UpdateRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = new Subject();
        JsonValue content = mock(JsonValue.class);
        Application application = mock(Application.class);
        Application newApplication = mock(Application.class);

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(request.getContent()).willReturn(content);
        given(applicationManagerWrapper.getApplication(subject, "/REALM", resourceId)).willReturn(application);
        given(applicationWrapper.getName()).willReturn("APP_NAME");
        given(applicationWrapper.getApplication()).willReturn(newApplication);
        given(newApplication.getRealm()).willReturn("/REALM");
        given(newApplication.getLastModifiedDate()).willReturn(1000L);
        doThrow(new EntitlementException(1)).when(applicationWrapper).toJsonValue();

        //When
        applicationsResource.updateInstance(context, resourceId, request, handler);

        //Then
        verify(applicationManagerWrapper)
                .updateApplication(application, applicationWrapper.getApplication(), subject);
        ArgumentCaptor<ResourceException> resourceExceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(resourceExceptionCaptor.capture());
        ResourceException exception = resourceExceptionCaptor.getValue();
        Assert.assertEquals(exception.getCode(), 500);
        Assert.assertEquals(exception.getReason(), "Internal Server Error");
    }

    @Test
    public void updateInstanceShouldReturnForbiddenWhenUpdatingFailsDueToNotAuthorized() throws EntitlementException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.addSubRealm("REALM", "REALM");
        ServerContext context = new ServerContext(realmContext);
        String resourceId = "iPlanetAMWebAgentService";
        UpdateRequest request = mock(UpdateRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = new Subject();
        JsonValue content = mock(JsonValue.class);
        Application mockApplication = mock(Application.class);

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(request.getContent()).willReturn(content);
        given(applicationWrapper.getApplication()).willReturn(mockApplication);
        given(mockApplication.getRealm()).willReturn("/REALM");
        given(applicationManagerWrapper.getApplication(subject, "/REALM", resourceId)).willReturn(mockApplication);
        doThrow(new EntitlementException(326)).when(applicationManagerWrapper)
                .updateApplication(any(Application.class), any(Application.class), any(Subject.class));

        //When
        applicationsResource.updateInstance(context, resourceId, request, handler);

        //Then
        ArgumentCaptor<ResourceException> resourceExceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(resourceExceptionCaptor.capture());
        ResourceException exception = resourceExceptionCaptor.getValue();
        Assert.assertEquals(exception.getCode(), 403);
        Assert.assertEquals(exception.getReason(), "Forbidden");
    }

    @Test
    public void updateInstanceShouldReturnConflictWhenUpdatingFailsDueToNeedToDeletePolicies()
            throws EntitlementException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.addSubRealm("REALM", "REALM");
        ServerContext context = new ServerContext(realmContext);
        String resourceId = "iPlanetAMWebAgentService";
        UpdateRequest request = mock(UpdateRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = new Subject();
        JsonValue content = mock(JsonValue.class);
        Application mockApplication = mock(Application.class);

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(request.getContent()).willReturn(content);
        given(applicationWrapper.getApplication()).willReturn(mockApplication);
        given(mockApplication.getRealm()).willReturn("/REALM");
        given(applicationManagerWrapper.getApplication(subject, "/REALM", resourceId)).willReturn(mockApplication);
        doThrow(new EntitlementException(404)).when(applicationManagerWrapper)
                .updateApplication(any(Application.class), any(Application.class), any(Subject.class));

        //When
        applicationsResource.updateInstance(context, resourceId, request, handler);

        //Then
        ArgumentCaptor<ResourceException> resourceExceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(resourceExceptionCaptor.capture());
        ResourceException exception = resourceExceptionCaptor.getValue();
        Assert.assertEquals(exception.getCode(), 409);
        Assert.assertEquals(exception.getReason(), "Conflict");
    }

    @Test
    public void shouldNotUpdateInstanceIfApplicationNotFound() throws EntitlementException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.addSubRealm("REALM", "REALM");
        ServerContext context = new ServerContext(realmContext);
        String resourceId = "RESOURCE_ID";
        UpdateRequest request = mock(UpdateRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        Subject subject = new Subject();
        JsonValue content = mock(JsonValue.class);
        Application mockApplication = mock(Application.class);

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(request.getContent()).willReturn(content);
        given(applicationManagerWrapper.getApplication(subject, "REALM", resourceId)).willReturn(null);
        given(applicationWrapper.getApplication()).willReturn(mockApplication);
        given(mockApplication.getRealm()).willReturn("REALM");

        //When
        applicationsResource.updateInstance(context, resourceId, request, handler);

        //Then
        ArgumentCaptor<ResourceException> resourceExceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(resourceExceptionCaptor.capture());
        ResourceException exception = resourceExceptionCaptor.getValue();
        Assert.assertEquals(exception.getCode(), 404);
        Assert.assertEquals(exception.getReason(), "Not Found");
    }

    @Test
    public void shouldNotUpdateInstanceWhenSubjectIsNull() throws EntitlementException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.addSubRealm("REALM", "REALM");
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
        Assert.assertEquals(exception.getCode(), 400);
        Assert.assertEquals(exception.getReason(), "Bad Request");
    }

    @Test
    public void shouldReturnThreeResultsOnQuery()
            throws EntitlementException, IllegalAccessException, InstantiationException {

        // Override the creation of the application wrapper so to return a mocked version.
        applicationsResource = new ApplicationsResource(
                debug, applicationManagerWrapper, applicationTypeManagerWrapper, queryAttributes, resourceErrorHandler) {

            @Override
            protected ApplicationWrapper createApplicationWrapper(
                    Application application, ApplicationTypeManagerWrapper type) {

                ApplicationWrapper wrapper = mock(ApplicationWrapper.class);
                String appName = application.getName();
                given(wrapper.getName()).willReturn(appName);

                try {
                    JsonValue jsonValue = json(object(field("name", appName)));
                    given(wrapper.toJsonValue()).willReturn(jsonValue);
                } catch (EntitlementException e) {
                    fail();
                }

                return wrapper;
            }
        };

        // Given
        SSOTokenContext mockSubjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext);
        realmContext.addSubRealm("abc", "abc");
        ServerContext serverContext = new ServerContext(realmContext);

        // Set the page size to be three starting from the second item.
        QueryRequest request = mock(QueryRequest.class);
        given(request.getPageSize()).willReturn(3);
        given(request.getPagedResultsOffset()).willReturn(1);
        given(request.getSortKeys()).willReturn(Arrays.asList(SortKey.ascendingOrder("name")));

        QueryResultHandler handler = mock(QueryResultHandler.class);
        given(handler.handleResource(any(Resource.class))).willReturn(true);

        Subject subject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(subject);

        Set<String> appNames = asSet("app1", "app2", "app3", "app4", "app5", "iPlanetAMWebAgentService");
        given(applicationManagerWrapper.search(eq(subject), eq("/abc"), any(Set.class))).willReturn(appNames);

        for (String appName : appNames) {
            Application app = mock(Application.class);
            given(app.getName()).willReturn(appName);
            given(applicationManagerWrapper
                    .getApplication(eq(subject), eq("/abc"), eq(appName))).willReturn(app);
        }

        // When
        applicationsResource.queryCollection(serverContext, request, handler);

        // Then
        verify(applicationManagerWrapper).search(eq(subject), eq("/abc"), any(Set.class));
        verify(applicationManagerWrapper, times(appNames.size())).getApplication(eq(subject), eq("/abc"), anyString());

        ArgumentCaptor<Resource> resourceCapture = ArgumentCaptor.forClass(Resource.class);
        verify(handler, times(3)).handleResource(resourceCapture.capture());

        List<String> selectedApps = transformList(resourceCapture.getAllValues(), new ResourceToIdMapper());
        assertThat(selectedApps).containsOnly("app2", "app3", "app4");

        ArgumentCaptor<QueryResult> resultCapture = ArgumentCaptor.forClass(QueryResult.class);
        verify(handler).handleResult(resultCapture.capture());

        QueryResult result = resultCapture.getValue();
        assertThat(result.getRemainingPagedResults()).isEqualTo(2);
    }

    @Test
    public void reservedInternalAppIsMappedDuringQuery()
            throws EntitlementException, IllegalAccessException, InstantiationException {

        // Override the creation of the application wrapper so to return a mocked version.
        applicationsResource = new ApplicationsResource(
                debug, applicationManagerWrapper, applicationTypeManagerWrapper, queryAttributes, resourceErrorHandler) {

            @Override
            protected ApplicationWrapper createApplicationWrapper(
                    Application application, ApplicationTypeManagerWrapper type) {

                ApplicationWrapper wrapper = mock(ApplicationWrapper.class);
                String appName = application.getName();
                given(wrapper.getName()).willReturn(appName);

                try {
                    JsonValue jsonValue = mock(JsonValue.class);
                    given(wrapper.toJsonValue()).willReturn(jsonValue);
                } catch (EntitlementException e) {
                    fail();
                }

                return wrapper;
            }
        };


        // Given...
        SSOTokenContext mockSubjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext);
        realmContext.addSubRealm("abc", "abc");
        ServerContext serverContext = new ServerContext(realmContext);

        QueryRequest request = mock(QueryRequest.class);

        Subject subject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(subject);

        Set<String> appNames = asSet("iPlanetAMWebAgentService");
        given(applicationManagerWrapper.search(eq(subject), eq("/abc"), any(Set.class))).willReturn(appNames);

        Application app = mock(Application.class);
        given(applicationManagerWrapper.getApplication(
                eq(subject), eq("/abc"), eq("iPlanetAMWebAgentService"))).willReturn(app);
        given(app.getName()).willReturn("iPlanetAMWebAgentService");

        given(app.getName()).willReturn("agentProtectedApplication");
        QueryResultHandler handler = mock(QueryResultHandler.class);
        given(handler.handleResource(any(Resource.class))).willReturn(true);

        // When...
        applicationsResource.queryCollection(serverContext, request, handler);

        // Then...
        verify(applicationManagerWrapper).search(eq(subject), eq("/abc"), any(Set.class));
        verify(applicationManagerWrapper).getApplication(eq(subject), eq("/abc"), anyString());

        ArgumentCaptor<Resource> resourceCapture = ArgumentCaptor.forClass(Resource.class);
        verify(handler).handleResource(resourceCapture.capture());

        Resource resource = resourceCapture.getValue();
        assertThat(resource.getId()).isEqualTo("agentProtectedApplication");
    }

    @Test
    public void shouldHandleApplicationFindFailure() throws EntitlementException {
        // Given
        SSOTokenContext mockSubjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext);
        realmContext.addSubRealm("abc", "abc");
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
        given(applicationManagerWrapper.search(eq(subject), eq("/abc"), any(Set.class))).willThrow(exception);

        // When
        applicationsResource.queryCollection(serverContext, request, handler);

        // Then
        ArgumentCaptor<ResourceException> exceptionCapture = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(exceptionCapture.capture());

        ResourceException resourceException = exceptionCapture.getValue();
        assertThat(resourceException.getCode()).isEqualTo(ResourceException.NOT_FOUND);
    }


    @Test
    public void shouldHandleJsonParsingFailure() throws EntitlementException {
        // Override the creation of the application wrapper so to return a mocked version.
        applicationsResource = new ApplicationsResource(
                debug, applicationManagerWrapper, applicationTypeManagerWrapper, queryAttributes, resourceErrorHandler) {

            @Override
            protected ApplicationWrapper createApplicationWrapper(
                    Application application, ApplicationTypeManagerWrapper type) {

                ApplicationWrapper wrapper = mock(ApplicationWrapper.class);
                String appName = application.getName();
                given(wrapper.getName()).willReturn(appName);

                try {
                    // Throws an EntitlementException when attempting to parse the json.
                    EntitlementException entitlementException = new EntitlementException(1);
                    given(wrapper.toJsonValue()).willThrow(entitlementException);
                } catch (EntitlementException e) {
                    fail();
                }

                return wrapper;
            }
        };


        // Given
        SSOTokenContext mockSubjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext);
        realmContext.addSubRealm("abc", "abc");
        ServerContext serverContext = new ServerContext(realmContext);

        // Set the page size to be three starting from the second item.
        QueryRequest request = mock(QueryRequest.class);
        given(request.getPageSize()).willReturn(3);
        given(request.getPagedResultsOffset()).willReturn(1);

        QueryResultHandler handler = mock(QueryResultHandler.class);
        given(handler.handleResource(any(Resource.class))).willReturn(true);

        Subject subject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(subject);

        Set<String> appNames = asOrderedSet("app1", "app2", "app3", "app4", "app5", "iPlanetAMWebAgentService");
        given(applicationManagerWrapper.search(eq(subject), eq("/abc"), any(Set.class))).willReturn(appNames);

        for (String appName : appNames) {
            Application app = mock(Application.class);
            given(app.getName()).willReturn(appName);
            given(applicationManagerWrapper
                    .getApplication(eq(subject), eq("/abc"), eq(appName))).willReturn(app);
        }

        // When
        applicationsResource.queryCollection(serverContext, request, handler);

        // Then
        verify(applicationManagerWrapper).search(eq(subject), eq("/abc"), any(Set.class));
        verify(applicationManagerWrapper, times(appNames.size())).getApplication(eq(subject), eq("/abc"), anyString());

        ArgumentCaptor<ResourceException> exceptionCapture = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(exceptionCapture.capture());

        ResourceException resourceException = exceptionCapture.getValue();
        assertThat(resourceException.getCode()).isEqualTo(ResourceException.INTERNAL_ERROR);
    }

    @Test
    public void shouldTranslateAlwaysTrueQueryFilterToEmptySearchFilters() throws Exception {
        // Given
        QueryRequest request = mockQueryRequest(alwaysTrue());
        Subject subject = new Subject();

        // When
        applicationsResource.query(request, subject, "/abc");

        // Then
        verify(applicationManagerWrapper).search(eq(subject), eq("/abc"), eq(Collections.<SearchFilter>emptySet()));
    }

    @Test
    public void shouldSendAllMatchingPoliciesToQueryHandler() throws Exception {
        // Given
        QueryRequest request = mockQueryRequest(alwaysTrue());
        Subject subject = new Subject();

        Set<String> applications = asSet("one", "two", "three");
        given(applicationManagerWrapper.search(eq(subject), eq("/abc"), any(Set.class))).willReturn(applications);

        // When
        Set<String> result = applicationsResource.query(request, subject, "/abc");

        // Then
        assertThat(result).isEqualTo(applications);
    }

    @Test(expectedExceptions = EntitlementException.class,
            expectedExceptionsMessageRegExp = ".*'false' not supported.*")
    public void shouldRejectAlwaysFalseQueryFilters() throws Exception {
        // Given
        QueryRequest request = mockQueryRequest(alwaysFalse());
        Subject subject = new Subject();

        // When
        applicationsResource.query(request, subject, "/abc");

        // Then - exception
    }

    @Test
    public void shouldHandleStringEquality() throws Exception {
        // Given
        String value = "testValue";
        QueryRequest request = mockQueryRequest(equalTo(STRING_ATTRIBUTE, value));
        Subject subject = new Subject();

        // When
        applicationsResource.query(request, subject, "/abc");

        // Then
        SearchFilter searchFilter = new SearchFilter(STRING_ATTRIBUTE, value);
        verify(applicationManagerWrapper).search(eq(subject), eq("/abc"), eq(asSet(searchFilter)));
    }

    @DataProvider(name = "SupportedQueryOperators")
    public static Object[][] supportedQueryOperators() {
        return new Object[][] {
                { "eq", SearchFilter.Operator.EQUAL_OPERATOR },
                // Treat >= and > as both greater-than-or-equals as that is all the search filters support
                { "gt", SearchFilter.Operator.GREATER_THAN_OPERATOR },
                { "ge", SearchFilter.Operator.GREATER_THAN_OPERATOR },
                // Same for <= and <.
                { "lt", SearchFilter.Operator.LESSER_THAN_OPERATOR },
                { "le", SearchFilter.Operator.LESSER_THAN_OPERATOR }
        };
    }

    @Test(dataProvider = "SupportedQueryOperators")
    public void shouldTranslateSupportedOperators(String queryOperator, SearchFilter.Operator expectedOperator)
            throws Exception {
        // Given
        long value = 123l;
        QueryRequest request = mockQueryRequest(comparisonFilter(NUMERIC_ATTRIBUTE, queryOperator, value));
        Subject subject = new Subject();

        // When
        applicationsResource.query(request, subject, "/abc");

        // Then
        SearchFilter searchFilter = new SearchFilter(NUMERIC_ATTRIBUTE, value, expectedOperator);
        verify(applicationManagerWrapper).search(eq(subject), eq("/abc"), eq(asSet(searchFilter)));
    }

    @DataProvider(name = "UnsupportedOperators")
    public static Object[][] unsupportedQueryOperators() {
        // We do not support starts-with, contains or any extended operators
        return new Object[][] {{ "sw" }, { "co" }, { "someExtendedOperator" }};
    }

    @Test(dataProvider = "UnsupportedOperators",
            expectedExceptions = EntitlementException.class,
            expectedExceptionsMessageRegExp = ".*not supported.*")
    public void shouldRejectUnsupportedQueryOperators(String queryOperator) throws Exception {
        // Given
        QueryRequest request = mockQueryRequest(comparisonFilter(NUMERIC_ATTRIBUTE, queryOperator, 123l));
        Subject subject = new Subject();

        // When
        applicationsResource.query(request, subject, "/abc");

        // Then - exception
    }

    @Test(expectedExceptions = EntitlementException.class,
            expectedExceptionsMessageRegExp = ".*Unknown query field.*")
    public void shouldRejectUnknownAttributes() throws Exception {
        // Given
        QueryRequest request = mockQueryRequest(equalTo("unknown", "a value"));
        Subject subject = new Subject();

        // When
        applicationsResource.query(request, subject, "/abc");

        // Then - exception
    }

    @Test(dataProvider = "SupportedQueryOperators")
    public void shouldSupportDateQueries(String queryOperator, SearchFilter.Operator expectedOperator)
            throws Exception {
        // Given
        Date value = new Date(123456789000l); // Note: only second accuracy supported in timestamp format
        QueryRequest request = mockQueryRequest(comparisonFilter(DATE_ATTRIBUTE, queryOperator,
                DateUtils.toUTCDateFormat(value)));
        Subject subject = new Subject();

        // When
        applicationsResource.query(request, subject, "/abc");

        // Then
        // Date should be converted into a time-stamp long value
        SearchFilter searchFilter = new SearchFilter(DATE_ATTRIBUTE, value.getTime(), expectedOperator);
        verify(applicationManagerWrapper).search(eq(subject), eq("/abc"), eq(asSet(searchFilter)));
    }

    @Test(expectedExceptions = EntitlementException.class,
            expectedExceptionsMessageRegExp = ".*not supported.*")
    public void shouldRejectPresenceQueries() throws Exception {
        // Given
        QueryRequest request = mockQueryRequest(present(STRING_ATTRIBUTE));
        Subject subject = new Subject();

        // When
        applicationsResource.query(request, subject, "/abc");

        // Then - exception
    }

    @Test
    public void shouldHandleAndQueries() throws Exception {
        // Given
        String value1 = "value1";
        String value2 = "value2";
        QueryRequest request = mockQueryRequest(
                and(equalTo(STRING_ATTRIBUTE, value1), equalTo(STRING_ATTRIBUTE, value2)));
        Subject subject = new Subject();

        // When
        applicationsResource.query(request, subject, "/abc");

        // Then
        SearchFilter searchFilter1 = new SearchFilter(STRING_ATTRIBUTE, value1);
        SearchFilter searchFilter2 = new SearchFilter(STRING_ATTRIBUTE, value2);
        verify(applicationManagerWrapper).search(eq(subject), eq("/abc"), eq(asSet(searchFilter1, searchFilter2)));
    }

    @Test(expectedExceptions = EntitlementException.class,
            expectedExceptionsMessageRegExp = ".*'Or' not supported.*")
    public void shouldRejectOrQueries() throws Exception {
        // Given
        QueryRequest request = mockQueryRequest(QueryFilter.or(QueryFilter.alwaysTrue(), QueryFilter.alwaysTrue()));
        Subject subject = new Subject();

        // When
        applicationsResource.query(request, subject, "/abc");

        // Then - exception
    }

    @Test(expectedExceptions = EntitlementException.class,
            expectedExceptionsMessageRegExp = ".*not supported.*")
    public void shouldRejectNotQueries() throws Exception {
        // Given
        QueryRequest request = mockQueryRequest(QueryFilter.not(QueryFilter.alwaysTrue()));
        Subject subject = new Subject();

        // When
        applicationsResource.query(request, subject, "/abc");

        // Then - exception
    }

    private QueryRequest mockQueryRequest(QueryFilter filter) {
        QueryRequest request = mock(QueryRequest.class);
        given(request.getQueryFilter()).willReturn(filter);
        return request;
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
