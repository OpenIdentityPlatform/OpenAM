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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openam.entitlement.rest;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.utils.CollectionUtils.*;
import static org.forgerock.util.query.QueryFilter.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.times;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.security.auth.Subject;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.SortKey;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.entitlement.guice.EntitlementRestGuiceModule;
import org.forgerock.openam.entitlement.rest.query.AttributeType;
import org.forgerock.openam.entitlement.rest.query.QueryAttribute;
import org.forgerock.openam.entitlement.rest.wrappers.ApplicationManagerWrapper;
import org.forgerock.openam.entitlement.rest.wrappers.ApplicationTypeManagerWrapper;
import org.forgerock.openam.entitlement.rest.wrappers.ApplicationWrapper;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.query.QueryResponsePresentation;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.services.context.ClientContext;
import org.forgerock.services.context.Context;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.util.SearchAttribute;
import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.debug.Debug;

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
    private ApplicationWrapper applicationWrapper;
    private Map<String, QueryAttribute> queryAttributes;
    private EntitlementsExceptionMappingHandler resourceErrorHandler
            = new EntitlementsExceptionMappingHandler(EntitlementRestGuiceModule.getEntitlementsErrorHandlers());

    @BeforeMethod
    public void setUp() {

        debug = mock(Debug.class);
        applicationManagerWrapper = mock(ApplicationManagerWrapper.class);
        applicationTypeManagerWrapper = mock(ApplicationTypeManagerWrapper.class);
        applicationWrapper = mock(ApplicationWrapper.class);

        queryAttributes = new HashMap<String, QueryAttribute>();
        queryAttributes.put(STRING_ATTRIBUTE, new QueryAttribute(AttributeType.STRING, new SearchAttribute
                        (STRING_ATTRIBUTE, "ou")));
        queryAttributes.put(NUMERIC_ATTRIBUTE, new QueryAttribute(AttributeType.NUMBER, new SearchAttribute(NUMERIC_ATTRIBUTE, "ou")));
        queryAttributes.put(DATE_ATTRIBUTE, new QueryAttribute(AttributeType.TIMESTAMP, new SearchAttribute(DATE_ATTRIBUTE, "ou")));

        applicationsResource = new ApplicationsResource(
                debug, applicationManagerWrapper, applicationTypeManagerWrapper, queryAttributes, resourceErrorHandler) {

            @Override
            protected ApplicationWrapper createApplicationWrapper(JsonValue jsonValue, Subject mySubject)
                    throws EntitlementException {
                return applicationWrapper;
            }

            @Override
            protected ApplicationWrapper createApplicationWrapper(Application a, ApplicationTypeManagerWrapper atmw) {
                return applicationWrapper;
            }
        };
    }

    @Test (expectedExceptions = BadRequestException.class)
    public void shouldReturnNullIfSubjectNullOnCreate() throws ResourceException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.setSubRealm("REALM", "REALM");
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        CreateRequest mockCreateRequest = mock(CreateRequest.class);

        given(mockSSOTokenContext.getCallerSubject()).willReturn(null);

        //when
        Promise<ResourceResponse, ResourceException> result =
                applicationsResource.createInstance(mockServerContext, mockCreateRequest);

        //then
        result.getOrThrowUninterruptibly();
    }

    @Test (expectedExceptions = BadRequestException.class)
    public void shouldThrowBadRequestIfApplicationWrapperCannotBeCreated() throws ResourceException {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.setSubRealm("REALM", "REALM");
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
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
        Promise<ResourceResponse, ResourceException> result =
                applicationsResource.createInstance(mockServerContext, mockCreateRequest);

        //then
        result.getOrThrowUninterruptibly();
    }

    @Test (expectedExceptions = InternalServerErrorException.class)
    public void shouldThrowInternalIfApplicationClassCannotBeInstantiated() throws ResourceException {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.setSubRealm("/", "/");
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
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
        Promise<ResourceResponse, ResourceException> result =
                applicationsResource.createInstance(realmContext, mockCreateRequest);

        //then
        result.getOrThrowUninterruptibly();
    }

    @Test (expectedExceptions = InternalServerErrorException.class)
    public void shouldThrowInternalErrorIfResourceWillNotSave() throws EntitlementException, ResourceException {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.setSubRealm("/", "/");
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        Subject subject = new Subject();
        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(applicationWrapper.getName()).willReturn("newApplication");
        doThrow(new EntitlementException(1)).when(applicationManagerWrapper).saveApplication(any(Subject.class),
                anyString(), any(Application.class));

        //when
        Promise<ResourceResponse, ResourceException> result =
                applicationsResource.createInstance(realmContext, mockCreateRequest);

        //then
        result.getOrThrowUninterruptibly();
    }

    @Test (expectedExceptions = BadRequestException.class)
    public void shouldReturnBadRequestIfCannotReturnResource() throws EntitlementException, ResourceException {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.setSubRealm("/", "/");
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        Subject subject = null;

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        doThrow(new EntitlementException(1)).when(applicationWrapper).toJsonValue();

        //when
        Promise<ResourceResponse, ResourceException> result =
                applicationsResource.createInstance(realmContext, mockCreateRequest);

        //then
        result.getOrThrowUninterruptibly();
    }

    @Test
    public void shouldCreateApplication() throws ExecutionException, ResourceException, EntitlementException {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.setSubRealm("/", "/");
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        Subject mockSubject = new Subject();
        Application application = mock(Application.class);
        given(mockSSOTokenContext.getCallerSubject()).willReturn(mockSubject);
        given(applicationManagerWrapper.saveApplication(mockSubject, "/", application)).willReturn(application);
        given(applicationWrapper.getName()).willReturn("newApplication");
        given(applicationWrapper.getApplication()).willReturn(application);
        given(application.getName()).willReturn("newApplication");

        //when
        Promise<ResourceResponse, ResourceException> result =
                applicationsResource.createInstance(realmContext, mockCreateRequest);

        //then
        assertThat(result.getOrThrowUninterruptibly()).isNotNull();
    }

    @Test (expectedExceptions = BadRequestException.class)
    public void shouldNotCreateApplicationWithInvalidCharactersInName() throws ResourceException {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.setSubRealm("/", "/");
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        Subject mockSubject = new Subject();
        given(mockSSOTokenContext.getCallerSubject()).willReturn(mockSubject);
        given(applicationWrapper.getName()).willReturn("new+application");

        //when
        Promise<ResourceResponse, ResourceException> result =
                applicationsResource.createInstance(realmContext, mockCreateRequest);

        // Then
        result.getOrThrowUninterruptibly();
    }

    @Test (expectedExceptions = BadRequestException.class)
    public void shouldThrowBadRequestIfSubjectNotFoundOnRead() throws ResourceException {
        // Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.setSubRealm("REALM", "REALM");
        Context context = ClientContext.newInternalClientContext(realmContext);
        given(subjectContext.getCallerSubject()).willReturn(null);

        // When
        Promise<ResourceResponse, ResourceException> result = applicationsResource.readInstance(context, null, null);

        // Then
        result.getOrThrowUninterruptibly();
    }

    @Test
    public void shouldUseResourceIDForFetchingApplicationOnRead() throws EntitlementException {
        // Given
        String resourceID = "iPlanetAMWebAgentService";

        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.setSubRealm("badger", "badger");
        Context serverContext = ClientContext.newInternalClientContext(realmContext);

        Subject subject = new Subject();
        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);

        Application mockApplication = mock(Application.class);
        given(applicationManagerWrapper.getApplication(any(Subject.class), anyString(), anyString())).willReturn(mockApplication);

        // When
        applicationsResource.readInstance(serverContext, resourceID, null);

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
        realmContext.setSubRealm(realmID, realmID);
        Context serverContext = ClientContext.newInternalClientContext(realmContext);

        Subject subject = new Subject();
        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);

        Application mockApplication = mock(Application.class);
        given(applicationManagerWrapper.getApplication(any(Subject.class), anyString(), anyString())).willReturn(mockApplication);

        // When
        applicationsResource.readInstance(serverContext, resourceID, null);

        // Then
        verify(applicationManagerWrapper).getApplication(any(Subject.class), eq(realmID), anyString());
    }

    @Test
    public void shouldUseSubjectFromContextOnRead() throws EntitlementException {
        // Given
        String resourceID = "ferret";

        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.setSubRealm("badger", "badger");
        Context serverContext = ClientContext.newInternalClientContext(realmContext);

        Subject subject = new Subject();
        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);

        Application mockApplication = mock(Application.class);
        given(applicationManagerWrapper.getApplication(any(Subject.class), anyString(), anyString())).willReturn(mockApplication);

        // When
        applicationsResource.readInstance(serverContext, resourceID, null);

        // Then
        verify(applicationManagerWrapper).getApplication(eq(subject), anyString(), anyString());
    }

    @Test (expectedExceptions = NotFoundException.class)
    public void shouldReturnNotFoundDeleteInstance() throws EntitlementException, ResourceException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.setSubRealm("REALM", "REALM");
        Context context = ClientContext.newInternalClientContext(realmContext);
        String resourceId = "RESOURCE_ID";
        DeleteRequest request = mock(DeleteRequest.class);
        Subject subject = new Subject();

        given(subjectContext.getCallerSubject()).willReturn(subject);

        //When
        Promise<ResourceResponse, ResourceException> result =
                applicationsResource.deleteInstance(context, resourceId, request);

        //Then
        verify(applicationManagerWrapper, never()).deleteApplication(subject, "REALM", resourceId);
        result.getOrThrowUninterruptibly();
    }

    @Test
    public void shouldDeleteInstance() throws EntitlementException, ResourceException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.setSubRealm("REALM", "REALM");
        Context context = ClientContext.newInternalClientContext(realmContext);
        String resourceId = "iPlanetAMWebAgentService";
        DeleteRequest request = mock(DeleteRequest.class);
        Subject subject = new Subject();

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(applicationManagerWrapper.search(any(Subject.class), anyString(), any(Set.class)))
                .willReturn(singleton(resourceId));

        //When
        Promise<ResourceResponse, ResourceException> result =
                applicationsResource.deleteInstance(context, resourceId, request);

        //Then
        verify(applicationManagerWrapper).deleteApplication(subject, "/REALM", resourceId);
        ResourceResponse resource = result.getOrThrowUninterruptibly();
        assertEquals(resource.getId(), resourceId);
    }

    @Test (expectedExceptions = BadRequestException.class)
    public void shouldNotDeleteInstanceWhenSubjectIsNull() throws EntitlementException, ResourceException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        Context context = ClientContext.newInternalClientContext(subjectContext);
        String resourceId = "RESOURCE_ID";
        DeleteRequest request = mock(DeleteRequest.class);
        Subject subject = null;

        given(subjectContext.getCallerSubject()).willReturn(subject);

        //When
        Promise<ResourceResponse, ResourceException> result =
                applicationsResource.deleteInstance(context, resourceId, request);

        //Then
        verify(applicationManagerWrapper, never()).deleteApplication(subject, "REALM", resourceId);
        result.getOrThrowUninterruptibly();
    }

    @Test (expectedExceptions = NotFoundException.class)
    public void deleteInstanceShouldHandleFailedDeleteApplication()
            throws EntitlementException, ResourceException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.setSubRealm("REALM", "REALM");
        Context context = ClientContext.newInternalClientContext(realmContext);
        String resourceId = "RESOURCE_ID";
        DeleteRequest request = mock(DeleteRequest.class);
        Subject subject = new Subject();

        given(subjectContext.getCallerSubject()).willReturn(subject);
        doThrow(EntitlementException.class).when(applicationManagerWrapper)
                .deleteApplication(any(Subject.class), anyString(), eq(resourceId));

        //When
        Promise<ResourceResponse, ResourceException> result = applicationsResource.deleteInstance(context, resourceId, request);

        //Then
        result.getOrThrowUninterruptibly();
    }

    @Test
    public void shouldUpdateInstance() throws EntitlementException, ResourceException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.setSubRealm("REALM", "REALM");
        Context context = ClientContext.newInternalClientContext(realmContext);
        String resourceId = "iPlanetAMWebAgentService";
        UpdateRequest request = mock(UpdateRequest.class);
        Subject subject = new Subject();
        JsonValue content = mock(JsonValue.class);
        Application application = mock(Application.class);
        JsonValue response = mock(JsonValue.class);

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(request.getContent()).willReturn(content);
        given(applicationManagerWrapper.saveApplication(subject, "/REALM", application)).willReturn(application);
        given(applicationManagerWrapper.search(any(Subject.class), anyString(), any(Set.class)))
                .willReturn(singleton(resourceId));
        given(applicationWrapper.getName()).willReturn(resourceId);
        given(applicationWrapper.getDisplayName()).willReturn("APP_NAME");
        given(applicationWrapper.getApplication()).willReturn(application);
        given(application.getName()).willReturn(resourceId);
        given(application.getLastModifiedDate()).willReturn(1000L);
        given(applicationWrapper.toJsonValue()).willReturn(response);

        //When
        Promise<ResourceResponse, ResourceException> result =
                applicationsResource.updateInstance(context, resourceId, request);

        //Then
        verify(applicationManagerWrapper).saveApplication(subject, "/REALM", applicationWrapper.getApplication());
        ResourceResponse resource = result.getOrThrowUninterruptibly();
        assertEquals(resource.getId(), resourceId);
        assertEquals(resource.getRevision(), "1000");
        assertEquals(resource.getContent(), response);
    }

    @Test (expectedExceptions = BadRequestException.class)
    public void shouldNotUpdateInstanceWithNameChange() throws EntitlementException, ResourceException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.setSubRealm("REALM", "REALM");
        Context context = ClientContext.newInternalClientContext(realmContext);
        String resourceId = "iPlanetAMWebAgentService";
        UpdateRequest request = mock(UpdateRequest.class);
        Subject subject = new Subject();
        JsonValue content = mock(JsonValue.class);

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(request.getContent()).willReturn(content);
        given(applicationWrapper.getName()).willReturn("APP_NAME");

        //When
        Promise<ResourceResponse, ResourceException> result =
                applicationsResource.updateInstance(context, resourceId, request);

        //Then
        result.getOrThrowUninterruptibly();
    }

    @Test (expectedExceptions = ForbiddenException.class)
    public void updateInstanceShouldReturnForbiddenWhenUpdatingFailsDueToNotAuthorized() throws EntitlementException, ResourceException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.setSubRealm("REALM", "REALM");
        Context context = ClientContext.newInternalClientContext(realmContext);
        String resourceId = "iPlanetAMWebAgentService";
        UpdateRequest request = mock(UpdateRequest.class);
        Subject subject = new Subject();
        JsonValue content = mock(JsonValue.class);
        Application mockApplication = mock(Application.class);

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(request.getContent()).willReturn(content);
        given(applicationWrapper.getApplication()).willReturn(mockApplication);
        given(applicationManagerWrapper.search(any(Subject.class), anyString(), any(Set.class)))
                .willReturn(singleton(resourceId));
        doThrow(new EntitlementException(326)).when(applicationManagerWrapper)
                .saveApplication(any(Subject.class), anyString(), any(Application.class));

        //When
        Promise<ResourceResponse, ResourceException> result =
                applicationsResource.updateInstance(context, resourceId, request);

        //Then
        result.getOrThrowUninterruptibly();
    }

    @Test (expectedExceptions = ConflictException.class)
    public void updateInstanceShouldReturnConflictWhenUpdatingFailsDueToNeedToDeletePolicies()
            throws EntitlementException, ResourceException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.setSubRealm("REALM", "REALM");
        Context context = ClientContext.newInternalClientContext(realmContext);
        String resourceId = "iPlanetAMWebAgentService";
        UpdateRequest request = mock(UpdateRequest.class);
        Subject subject = new Subject();
        JsonValue content = mock(JsonValue.class);
        Application mockApplication = mock(Application.class);

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(request.getContent()).willReturn(content);
        given(applicationWrapper.getApplication()).willReturn(mockApplication);
        given(applicationManagerWrapper.search(any(Subject.class), anyString(), any(Set.class)))
                .willReturn(singleton(resourceId));
        doThrow(new EntitlementException(404)).when(applicationManagerWrapper)
                .saveApplication(any(Subject.class), anyString(), any(Application.class));

        //When
        Promise<ResourceResponse, ResourceException> result =
                applicationsResource.updateInstance(context, resourceId, request);

        //Then
        result.getOrThrowUninterruptibly();
    }

    @Test (expectedExceptions = NotFoundException.class)
    public void shouldNotUpdateInstanceIfApplicationNotFound() throws EntitlementException, ResourceException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.setSubRealm("REALM", "REALM");
        Context context = ClientContext.newInternalClientContext(realmContext);
        String resourceId = "RESOURCE_ID";
        UpdateRequest request = mock(UpdateRequest.class);
        Subject subject = new Subject();
        JsonValue content = mock(JsonValue.class);
        Application mockApplication = mock(Application.class);

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(request.getContent()).willReturn(content);
        given(applicationWrapper.getApplication()).willReturn(mockApplication);

        //When
        Promise<ResourceResponse, ResourceException> result =
                applicationsResource.updateInstance(context, resourceId, request);

        //Then
        result.getOrThrowUninterruptibly();
    }

    @Test (expectedExceptions = BadRequestException.class)
    public void shouldNotUpdateInstanceWhenSubjectIsNull() throws EntitlementException, ResourceException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.setSubRealm("REALM", "REALM");
        Context context = ClientContext.newInternalClientContext(realmContext);
        String resourceId = "RESOURCE_ID";
        UpdateRequest request = mock(UpdateRequest.class);
        Subject subject = null;

        given(subjectContext.getCallerSubject()).willReturn(subject);

        //When
        Promise<ResourceResponse, ResourceException> result =
                applicationsResource.updateInstance(context, resourceId, request);

        //Then
        result.getOrThrowUninterruptibly();
    }

    @Test
    public void shouldReturnThreeResultsOnQuery()
            throws EntitlementException, IllegalAccessException, InstantiationException, ResourceException {

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

        Subject subject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(subject);

        RealmContext realmContext = new RealmContext(mockSubjectContext);
        realmContext.setSubRealm("abc", "abc");
        Context serverContext = ClientContext.newInternalClientContext(realmContext);

        // Set the page size to be three starting from the second item.
        QueryRequest request = mock(QueryRequest.class);
        given(request.getAdditionalParameter(QueryResponsePresentation.REMAINING)).willReturn("true");
        given(request.getPageSize()).willReturn(3);
        given(request.getPagedResultsOffset()).willReturn(1);
        given(request.getSortKeys()).willReturn(Arrays.asList(SortKey.ascendingOrder("name")));

        QueryResourceHandler handler = mock(QueryResourceHandler.class);
        given(handler.handleResource(any(ResourceResponse.class))).willReturn(true);

        Set<String> appNames = asSet("app1", "app2", "app3", "app4", "app5", "iPlanetAMWebAgentService");
        given(applicationManagerWrapper.search(eq(subject), eq("/abc"), any(Set.class))).willReturn(appNames);

        for (String appName : appNames) {
            Application app = mock(Application.class);
            given(app.getName()).willReturn(appName);
            given(applicationManagerWrapper
                    .getApplication(eq(subject), eq("/abc"), eq(appName))).willReturn(app);
        }

        // When
        Promise<QueryResponse, ResourceException> result =
                applicationsResource.queryCollection(serverContext, request, handler);

        // Then
        verify(applicationManagerWrapper).search(eq(subject), eq("/abc"), any(Set.class));
        verify(applicationManagerWrapper, times(appNames.size())).getApplication(eq(subject), eq("/abc"), anyString());

        ArgumentCaptor<ResourceResponse> resourceCapture = ArgumentCaptor.forClass(ResourceResponse.class);
        verify(handler, times(3)).handleResource(resourceCapture.capture());

        List<String> selectedApps = transformList(resourceCapture.getAllValues(), new ResourceToIdMapper());
        assertThat(selectedApps).containsOnly("app2", "app3", "app4");

        QueryResponse response = result.getOrThrowUninterruptibly();
        assertThat(response.getRemainingPagedResults()).isEqualTo(2);
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
                    JsonValue jsonValue = JsonValueBuilder.jsonValue().put("name", "agentProtectedApplication").build();
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
        realmContext.setSubRealm("abc", "abc");
        Context serverContext = ClientContext.newInternalClientContext(realmContext);

        QueryRequest request = mock(QueryRequest.class);
        given(request.getSortKeys()).willReturn(Arrays.asList(SortKey.ascendingOrder("name")));

        Subject subject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(subject);

        Set<String> appNames = asSet("iPlanetAMWebAgentService");
        given(applicationManagerWrapper.search(eq(subject), eq("/abc"), any(Set.class))).willReturn(appNames);

        Application app = mock(Application.class);
        given(applicationManagerWrapper.getApplication(
                eq(subject), eq("/abc"), eq("iPlanetAMWebAgentService"))).willReturn(app);
        given(app.getName()).willReturn("agentProtectedApplication");

        QueryResourceHandler handler = mock(QueryResourceHandler.class);
        given(handler.handleResource(any(ResourceResponse.class))).willReturn(true);

        // When...
        applicationsResource.queryCollection(serverContext, request, handler);

        // Then...
        verify(applicationManagerWrapper).search(eq(subject), eq("/abc"), any(Set.class));
        verify(applicationManagerWrapper).getApplication(eq(subject), eq("/abc"), anyString());

        ArgumentCaptor<ResourceResponse> resourceCapture = ArgumentCaptor.forClass(ResourceResponse.class);
        verify(handler).handleResource(resourceCapture.capture());

        ResourceResponse resource = resourceCapture.getValue();
        assertThat(resource.getId()).isEqualTo("agentProtectedApplication");
    }

    @Test (expectedExceptions = NotFoundException.class)
    public void shouldHandleApplicationFindFailure() throws EntitlementException, ResourceException {
        // Given
        SSOTokenContext mockSubjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext);
        realmContext.setSubRealm("abc", "abc");
        Context serverContext = ClientContext.newInternalClientContext(realmContext);

        // Set the page size to be three starting from the second item.
        QueryRequest request = mock(QueryRequest.class);
        given(request.getPageSize()).willReturn(3);
        given(request.getPagedResultsOffset()).willReturn(1);

        QueryResourceHandler handler = mock(QueryResourceHandler.class);
        given(handler.handleResource(any(ResourceResponse.class))).willReturn(true);

        Subject subject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(subject);

        EntitlementException exception = new EntitlementException(EntitlementException.APP_RETRIEVAL_ERROR);
        given(applicationManagerWrapper.search(eq(subject), eq("/abc"), any(Set.class))).willThrow(exception);

        // When
        Promise<QueryResponse, ResourceException> result = applicationsResource.queryCollection(serverContext, request, handler);

        // Then
        result.getOrThrowUninterruptibly();
    }


    @Test (expectedExceptions = InternalServerErrorException.class)
    public void shouldHandleJsonParsingFailure() throws EntitlementException, ResourceException {
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
        realmContext.setSubRealm("abc", "abc");
        Context serverContext = ClientContext.newInternalClientContext(realmContext);

        // Set the page size to be three starting from the second item.
        QueryRequest request = mock(QueryRequest.class);
        given(request.getPageSize()).willReturn(3);
        given(request.getPagedResultsOffset()).willReturn(1);

        QueryResourceHandler handler = mock(QueryResourceHandler.class);
        given(handler.handleResource(any(ResourceResponse.class))).willReturn(true);

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
        Promise<QueryResponse, ResourceException> result = applicationsResource.queryCollection(serverContext, request, handler);

        // Then
        verify(applicationManagerWrapper).search(eq(subject), eq("/abc"), any(Set.class));
        result.getOrThrowUninterruptibly();
    }

    @Test
    public void shouldTranslateAlwaysTrueQueryFilterToEmptySearchFilters() throws Exception {
        // Given
        QueryRequest request = mockQueryRequest(QueryFilter.<JsonPointer>alwaysTrue());
        Subject subject = new Subject();

        // When
        applicationsResource.query(request, subject, "/abc");

        // Then
        verify(applicationManagerWrapper).search(eq(subject), eq("/abc"), eq(Collections.<SearchFilter>emptySet()));
    }

    @Test
    public void shouldSendAllMatchingPoliciesToQueryHandler() throws Exception {
        // Given
        QueryRequest request = mockQueryRequest(QueryFilter.<JsonPointer>alwaysTrue());
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
        QueryRequest request = mockQueryRequest(QueryFilter.<JsonPointer>alwaysFalse());
        Subject subject = new Subject();

        // When
        applicationsResource.query(request, subject, "/abc");

        // Then - exception
    }

    @Test
    public void shouldHandleStringEquality() throws Exception {
        // Given
        String value = "testValue";
        QueryRequest request = mockQueryRequest(equalTo(new JsonPointer(STRING_ATTRIBUTE), value));
        Subject subject = new Subject();

        // When
        applicationsResource.query(request, subject, "/abc");

        // Then
        SearchFilter searchFilter = new SearchFilter(new SearchAttribute(STRING_ATTRIBUTE, "ou"), value);
        verify(applicationManagerWrapper).search(eq(subject), eq("/abc"), eq(asSet(searchFilter)));
    }

    @DataProvider(name = "SupportedQueryOperators")
    public static Object[][] supportedQueryOperators() {
        return new Object[][] {
                { "eq", SearchFilter.Operator.EQUALS_OPERATOR},
                { "gt", SearchFilter.Operator.GREATER_THAN_OPERATOR },
                { "ge", SearchFilter.Operator.GREATER_THAN_OR_EQUAL_OPERATOR },
                { "lt", SearchFilter.Operator.LESS_THAN_OPERATOR},
                { "le", SearchFilter.Operator.LESS_THAN_OR_EQUAL_OPERATOR}
        };
    }

    @Test(dataProvider = "SupportedQueryOperators")
    public void shouldTranslateSupportedOperators(String queryOperator, SearchFilter.Operator expectedOperator)
            throws Exception {
        // Given
        long value = 123l;
        QueryRequest request = mockQueryRequest(comparisonFilter(new JsonPointer(NUMERIC_ATTRIBUTE), queryOperator, value));
        Subject subject = new Subject();

        // When
        applicationsResource.query(request, subject, "/abc");

        // Then
        SearchFilter searchFilter = new SearchFilter(new SearchAttribute(NUMERIC_ATTRIBUTE, "ou"), value, expectedOperator);
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
        QueryRequest request = mockQueryRequest(comparisonFilter(new JsonPointer(NUMERIC_ATTRIBUTE), queryOperator, 123l));
        Subject subject = new Subject();

        // When
        applicationsResource.query(request, subject, "/abc");

        // Then - exception
    }

    @Test(expectedExceptions = EntitlementException.class,
            expectedExceptionsMessageRegExp = ".*Unknown query field.*")
    public void shouldRejectUnknownAttributes() throws Exception {
        // Given
        QueryRequest request = mockQueryRequest(equalTo(new JsonPointer("unknown"), "a value"));
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
        QueryRequest request = mockQueryRequest(comparisonFilter(new JsonPointer(DATE_ATTRIBUTE), queryOperator,
                DateUtils.toUTCDateFormat(value)));
        Subject subject = new Subject();

        // When
        applicationsResource.query(request, subject, "/abc");

        // Then
        // Date should be converted into a time-stamp long value
        SearchFilter searchFilter = new SearchFilter(new SearchAttribute(DATE_ATTRIBUTE, "ou"), value.getTime(), expectedOperator);
        verify(applicationManagerWrapper).search(eq(subject), eq("/abc"), eq(asSet(searchFilter)));
    }

    @Test(expectedExceptions = EntitlementException.class,
            expectedExceptionsMessageRegExp = ".*not supported.*")
    public void shouldRejectPresenceQueries() throws Exception {
        // Given
        QueryRequest request = mockQueryRequest(present(new JsonPointer(STRING_ATTRIBUTE)));
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
                and(equalTo(new JsonPointer(STRING_ATTRIBUTE), value1), equalTo(new JsonPointer(STRING_ATTRIBUTE), value2)));
        Subject subject = new Subject();

        // When
        applicationsResource.query(request, subject, "/abc");

        // Then
        SearchFilter searchFilter1 = new SearchFilter(new SearchAttribute(STRING_ATTRIBUTE, "ou"), value1);
        SearchFilter searchFilter2 = new SearchFilter(new SearchAttribute(STRING_ATTRIBUTE, "ou"), value2);
        verify(applicationManagerWrapper).search(eq(subject), eq("/abc"), eq(asSet(searchFilter1, searchFilter2)));
    }

    @Test(expectedExceptions = EntitlementException.class, expectedExceptionsMessageRegExp = ".*'Or' not supported.*")
    public void shouldRejectOrQueries() throws Exception {
        // Given
        QueryRequest request = mockQueryRequest(or(QueryFilter.<JsonPointer>alwaysTrue(), QueryFilter.<JsonPointer>alwaysTrue()));
        Subject subject = new Subject();

        // When
        applicationsResource.query(request, subject, "/abc");

        // Then - exception
    }

    @Test(expectedExceptions = EntitlementException.class, expectedExceptionsMessageRegExp = ".*not supported.*")
    public void shouldRejectNotQueries() throws Exception {
        // Given
        QueryRequest request = mockQueryRequest(not(QueryFilter.<JsonPointer>alwaysTrue()));
        Subject subject = new Subject();

        // When
        applicationsResource.query(request, subject, "/abc");

        // Then - exception
    }

    private QueryRequest mockQueryRequest(QueryFilter<JsonPointer> filter) {
        QueryRequest request = mock(QueryRequest.class);
        given(request.getQueryFilter()).willReturn(filter);
        return request;
    }

    /**
     * Maps a resource object to its string Id.
     *
     * @since 12.0.0
     */
    private static class ResourceToIdMapper implements Function<ResourceResponse, String, NeverThrowsException> {

        @Override
        public String apply(ResourceResponse resource) throws NeverThrowsException {
            return resource.getId();
        }

    }
}
