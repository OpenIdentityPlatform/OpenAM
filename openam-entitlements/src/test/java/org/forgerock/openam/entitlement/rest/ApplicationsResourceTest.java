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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.security.auth.Subject;

import org.forgerock.openam.test.apidescriptor.ApiAnnotationAssert;
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
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.entitlement.guice.EntitlementRestGuiceModule;
import org.forgerock.openam.entitlement.rest.wrappers.ApplicationTypeManagerWrapper;
import org.forgerock.openam.entitlement.rest.wrappers.ApplicationWrapper;
import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.debug.Debug;

/**
 * @since 12.0.0
 */
public class ApplicationsResourceTest {

    private ApplicationsResource applicationsResource;
    private Debug debug;
    private ApplicationServiceFactory applicationServiceFactory;
    private ApplicationService applicationService;
    private ApplicationTypeManagerWrapper applicationTypeManagerWrapper;
    private ApplicationWrapper applicationWrapper;
    private EntitlementsExceptionMappingHandler resourceErrorHandler
            = new EntitlementsExceptionMappingHandler(EntitlementRestGuiceModule.getEntitlementsErrorHandlers());
    private RealmTestHelper realmTestHelper;

    @BeforeMethod
    public void setUp() throws Exception {

        debug = mock(Debug.class);
        applicationServiceFactory = mock(ApplicationServiceFactory.class);
        applicationService = mock(ApplicationService.class);
        when(applicationServiceFactory.create(any(), anyString())).thenReturn(applicationService);
        applicationTypeManagerWrapper = mock(ApplicationTypeManagerWrapper.class);
        applicationWrapper = mock(ApplicationWrapper.class);
        realmTestHelper = new RealmTestHelper();
        realmTestHelper.setupRealmClass();
        applicationsResource = new ApplicationsResource(debug, applicationServiceFactory, applicationTypeManagerWrapper,
                resourceErrorHandler) {

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

    @AfterMethod
    public void tearDown() {
        realmTestHelper.tearDownRealmClass();
    }

    @Test (expectedExceptions = BadRequestException.class)
    public void shouldReturnNullIfSubjectNullOnCreate() throws ResourceException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        Realm realm = realmTestHelper.mockRealm("REALM");
        RealmContext realmContext = new RealmContext(mockSSOTokenContext, realm);
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
        Realm realm = realmTestHelper.mockRealm("REALM");
        RealmContext realmContext = new RealmContext(mockSSOTokenContext, realm);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        Subject subject = new Subject();

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);

        applicationsResource = new ApplicationsResource(
                debug, applicationServiceFactory, applicationTypeManagerWrapper, resourceErrorHandler) {

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
        RealmContext realmContext = new RealmContext(mockSSOTokenContext, Realm.root());
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        Subject subject = new Subject();

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);

        applicationsResource = new ApplicationsResource(
                debug, applicationServiceFactory, applicationTypeManagerWrapper, resourceErrorHandler) {

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
        RealmContext realmContext = new RealmContext(mockSSOTokenContext, Realm.root());
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        Subject subject = new Subject();
        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(applicationWrapper.getName()).willReturn("newApplication");
        doThrow(new EntitlementException(1)).when(applicationService).saveApplication(any());

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
        RealmContext realmContext = new RealmContext(mockSSOTokenContext, Realm.root());
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
        RealmContext realmContext = new RealmContext(mockSSOTokenContext, Realm.root());
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        Subject mockSubject = new Subject();
        Application application = mock(Application.class);
        given(mockSSOTokenContext.getCallerSubject()).willReturn(mockSubject);
        given(applicationService.saveApplication(application)).willReturn(application);
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
        RealmContext realmContext = new RealmContext(mockSSOTokenContext, Realm.root());
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
        Realm realm = realmTestHelper.mockRealm("REALM");
        RealmContext realmContext = new RealmContext(subjectContext, realm);
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
        Realm realm = realmTestHelper.mockRealm("badger");
        RealmContext realmContext = new RealmContext(mockSSOTokenContext, realm);
        Context serverContext = ClientContext.newInternalClientContext(realmContext);

        Subject subject = new Subject();
        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);

        Application mockApplication = mock(Application.class);
        given(applicationService.getApplication(anyString())).willReturn(mockApplication);

        // When
        applicationsResource.readInstance(serverContext, resourceID, null);

        // Then
        verify(applicationService).getApplication(eq(resourceID));
    }

    @Test
    public void shouldUseRealmFromContextSubjectOnRead() throws EntitlementException {
        // Given
        String resourceID = "ferret";
        String realmID = "badger";

        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        Realm realm = realmTestHelper.mockRealm(realmID);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext, realm);
        Context serverContext = ClientContext.newInternalClientContext(realmContext);

        Subject subject = new Subject();
        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);

        Application mockApplication = mock(Application.class);
        given(applicationService.getApplication(anyString())).willReturn(mockApplication);

        // When
        applicationsResource.readInstance(serverContext, resourceID, null);

        // Then
        verify(applicationService).getApplication(anyString());
    }

    @Test
    public void shouldUseSubjectFromContextOnRead() throws EntitlementException {
        // Given
        String resourceID = "ferret";

        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        Realm realm = realmTestHelper.mockRealm("badger");
        RealmContext realmContext = new RealmContext(mockSSOTokenContext, realm);
        Context serverContext = ClientContext.newInternalClientContext(realmContext);

        Subject subject = new Subject();
        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);

        Application mockApplication = mock(Application.class);
        given(applicationService.getApplication(anyString())).willReturn(mockApplication);

        // When
        applicationsResource.readInstance(serverContext, resourceID, null);

        // Then
        verify(applicationService).getApplication(anyString());
    }

    @Test (expectedExceptions = NotFoundException.class)
    public void shouldReturnNotFoundDeleteInstance() throws EntitlementException, ResourceException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        Realm realm = realmTestHelper.mockRealm("REALM");
        RealmContext realmContext = new RealmContext(subjectContext, realm);
        Context context = ClientContext.newInternalClientContext(realmContext);
        String resourceId = "RESOURCE_ID";
        DeleteRequest request = mock(DeleteRequest.class);
        Subject subject = new Subject();

        given(subjectContext.getCallerSubject()).willReturn(subject);

        //When
        Promise<ResourceResponse, ResourceException> result =
                applicationsResource.deleteInstance(context, resourceId, request);

        //Then
        verify(applicationService, never()).deleteApplication(resourceId);
        result.getOrThrowUninterruptibly();
    }

    @Test
    public void shouldDeleteInstance() throws EntitlementException, ResourceException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        Realm realm = realmTestHelper.mockRealm("REALM");
        RealmContext realmContext = new RealmContext(subjectContext, realm);
        Context context = ClientContext.newInternalClientContext(realmContext);
        String resourceId = "iPlanetAMWebAgentService";
        Application application = mock(Application.class);
        when(application.getName()).thenReturn(resourceId);
        DeleteRequest request = mock(DeleteRequest.class);
        Subject subject = new Subject();

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(applicationService.search(any(QueryFilter.class)))
                .willReturn(singleton(application));

        //When
        Promise<ResourceResponse, ResourceException> result =
                applicationsResource.deleteInstance(context, resourceId, request);

        //Then
        verify(applicationService).deleteApplication(resourceId);
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
        verify(applicationService, never()).deleteApplication(resourceId);
        result.getOrThrowUninterruptibly();
    }

    @Test (expectedExceptions = NotFoundException.class)
    public void deleteInstanceShouldHandleFailedDeleteApplication()
            throws EntitlementException, ResourceException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        Realm realm = realmTestHelper.mockRealm("REALM");
        RealmContext realmContext = new RealmContext(subjectContext, realm);
        Context context = ClientContext.newInternalClientContext(realmContext);
        String resourceId = "RESOURCE_ID";
        DeleteRequest request = mock(DeleteRequest.class);
        Subject subject = new Subject();

        given(subjectContext.getCallerSubject()).willReturn(subject);
        doThrow(EntitlementException.class).when(applicationService).deleteApplication(eq(resourceId));

        //When
        Promise<ResourceResponse, ResourceException> result = applicationsResource.deleteInstance(context, resourceId, request);

        //Then
        result.getOrThrowUninterruptibly();
    }

    @Test
    public void shouldUpdateInstance() throws EntitlementException, ResourceException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        Realm realm = realmTestHelper.mockRealm("REALM");
        RealmContext realmContext = new RealmContext(subjectContext, realm);
        Context context = ClientContext.newInternalClientContext(realmContext);
        String resourceId = "iPlanetAMWebAgentService";
        UpdateRequest request = mock(UpdateRequest.class);
        Subject subject = new Subject();
        JsonValue content = mock(JsonValue.class);
        Application application = mock(Application.class);
        JsonValue response = mock(JsonValue.class);

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(request.getContent()).willReturn(content);
        given(applicationService.saveApplication(application)).willReturn(application);
        given(applicationService.search(any(QueryFilter.class))).willReturn(singleton(application));
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
        verify(applicationService).saveApplication(applicationWrapper.getApplication());
        ResourceResponse resource = result.getOrThrowUninterruptibly();
        assertEquals(resource.getId(), resourceId);
        assertEquals(resource.getRevision(), "1000");
        assertEquals(resource.getContent(), response);
    }

    @Test (expectedExceptions = BadRequestException.class)
    public void shouldNotUpdateInstanceWithNameChange() throws EntitlementException, ResourceException {

        //Given
        SSOTokenContext subjectContext = mock(SSOTokenContext.class);
        Realm realm = realmTestHelper.mockRealm("REALM");
        RealmContext realmContext = new RealmContext(subjectContext, realm);
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
        Realm realm = realmTestHelper.mockRealm("REALM");
        RealmContext realmContext = new RealmContext(subjectContext, realm);
        Context context = ClientContext.newInternalClientContext(realmContext);
        String resourceId = "iPlanetAMWebAgentService";
        UpdateRequest request = mock(UpdateRequest.class);
        Subject subject = new Subject();
        JsonValue content = mock(JsonValue.class);
        Application mockApplication = mock(Application.class);

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(request.getContent()).willReturn(content);
        given(applicationWrapper.getApplication()).willReturn(mockApplication);
        given(applicationService.search(any(QueryFilter.class)))
                .willReturn(singleton(mockApplication));
        doThrow(new EntitlementException(326)).when(applicationService).saveApplication(any(Application.class));

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
        Realm realm = realmTestHelper.mockRealm("REALM");
        RealmContext realmContext = new RealmContext(subjectContext, realm);
        Context context = ClientContext.newInternalClientContext(realmContext);
        String resourceId = "iPlanetAMWebAgentService";
        UpdateRequest request = mock(UpdateRequest.class);
        Subject subject = new Subject();
        JsonValue content = mock(JsonValue.class);
        Application mockApplication = mock(Application.class);

        given(subjectContext.getCallerSubject()).willReturn(subject);
        given(request.getContent()).willReturn(content);
        given(applicationWrapper.getApplication()).willReturn(mockApplication);
        given(applicationService.search(any(QueryFilter.class))).willReturn(singleton(mockApplication));
        doThrow(new EntitlementException(404)).when(applicationService).saveApplication(any(Application.class));

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
        Realm realm = realmTestHelper.mockRealm("REALM");
        RealmContext realmContext = new RealmContext(subjectContext, realm);
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
        Realm realm = realmTestHelper.mockRealm("REALM");
        RealmContext realmContext = new RealmContext(subjectContext, realm);
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
                debug, applicationServiceFactory, applicationTypeManagerWrapper, resourceErrorHandler) {

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

        Realm realm = realmTestHelper.mockRealm("abc");
        RealmContext realmContext = new RealmContext(mockSubjectContext, realm);
        Context serverContext = ClientContext.newInternalClientContext(realmContext);

        // Set the page size to be three starting from the second item.
        QueryRequest request = mock(QueryRequest.class);
        given(request.getAdditionalParameter(QueryResponsePresentation.REMAINING)).willReturn("true");
        given(request.getPageSize()).willReturn(3);
        given(request.getPagedResultsOffset()).willReturn(1);
        given(request.getSortKeys()).willReturn(Arrays.asList(SortKey.ascendingOrder("name")));

        QueryResourceHandler handler = mock(QueryResourceHandler.class);
        given(handler.handleResource(any(ResourceResponse.class))).willReturn(true);

        Set<String> appNames = asOrderedSet("app1", "app2", "app3", "app4", "app5", "iPlanetAMWebAgentService");
        Set<Application> apps = new HashSet<>();
        for (String appName : appNames) {
            Application app = mock(Application.class);
            given(app.getName()).willReturn(appName);
            apps.add(app);
        }
        given(applicationService.search(any(QueryFilter.class))).willReturn(apps);

        // When
        Promise<QueryResponse, ResourceException> result =
                applicationsResource.queryCollection(serverContext, request, handler);

        // Then
        verify(applicationService).search(any(QueryFilter.class));

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
                debug, applicationServiceFactory, applicationTypeManagerWrapper, resourceErrorHandler) {

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
        Realm realm = realmTestHelper.mockRealm("abc");
        RealmContext realmContext = new RealmContext(mockSubjectContext, realm);
        Context serverContext = ClientContext.newInternalClientContext(realmContext);

        QueryRequest request = mock(QueryRequest.class);
        given(request.getSortKeys()).willReturn(Arrays.asList(SortKey.ascendingOrder("name")));

        Subject subject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(subject);

        Application app = mock(Application.class);
        given(applicationService.search(any(QueryFilter.class)))
                .willReturn(singleton(app));
        given(app.getName()).willReturn("agentProtectedApplication");

        QueryResourceHandler handler = mock(QueryResourceHandler.class);
        given(handler.handleResource(any(ResourceResponse.class))).willReturn(true);

        // When...
        applicationsResource.queryCollection(serverContext, request, handler);

        // Then...
        verify(applicationService).search(any(QueryFilter.class));

        ArgumentCaptor<ResourceResponse> resourceCapture = ArgumentCaptor.forClass(ResourceResponse.class);
        verify(handler).handleResource(resourceCapture.capture());

        ResourceResponse resource = resourceCapture.getValue();
        assertThat(resource.getId()).isEqualTo("agentProtectedApplication");
    }

    @Test (expectedExceptions = NotFoundException.class)
    public void shouldHandleApplicationFindFailure() throws EntitlementException, ResourceException {
        // Given
        SSOTokenContext mockSubjectContext = mock(SSOTokenContext.class);
        Realm realm = realmTestHelper.mockRealm("abc");
        RealmContext realmContext = new RealmContext(mockSubjectContext, realm);
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
        given(applicationService.search(any(QueryFilter.class))).willThrow(exception);

        // When
        Promise<QueryResponse, ResourceException> result = applicationsResource.queryCollection(serverContext, request,
                handler);

        // Then
        result.getOrThrowUninterruptibly();
    }


    @Test (expectedExceptions = InternalServerErrorException.class)
    public void shouldHandleJsonParsingFailure() throws EntitlementException, ResourceException {
        // Override the creation of the application wrapper so to return a mocked version.
        applicationsResource = new ApplicationsResource(
                debug, applicationServiceFactory, applicationTypeManagerWrapper, resourceErrorHandler) {

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
        Realm realm = realmTestHelper.mockRealm("abc");
        RealmContext realmContext = new RealmContext(mockSubjectContext, realm);
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
        Set<Application> apps = new HashSet<>();
        for (String appName : appNames) {
            Application app = mock(Application.class);
            given(app.getName()).willReturn(appName);
            apps.add(app);
        }
        given(applicationService.search(any(QueryFilter.class))).willReturn(apps);

        // When
        Promise<QueryResponse, ResourceException> result = applicationsResource.queryCollection(serverContext, request,
                handler);

        // Then
        verify(applicationService).search(any(QueryFilter.class));
        result.getOrThrowUninterruptibly();
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

    @Test
    public void shouldFailIfAnnotationsAreNotValid() {
        ApiAnnotationAssert.assertThat(ApplicationsResource.class).hasValidAnnotations();
    }
}
