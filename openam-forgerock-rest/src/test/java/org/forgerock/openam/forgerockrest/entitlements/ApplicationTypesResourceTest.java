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

import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.ApplicationTypeManager;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.debug.Debug;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.security.auth.Subject;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.resource.SubjectContext;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ApplicationTypeManager.class, Subject.class, ApplicationType.class})
@SuppressStaticInitializationFor("org.forgerock.openam.forgerockrest.RestUtils")
public class ApplicationTypesResourceTest {

    private ApplicationTypesResource testResource;

    //necessary to use the .asContext methods
    private  SubjectContext mockSubjectContext;
    private ServerContext mockContext;
    private ResultHandler mockHandler;

    @Before
    public void setUp() {

        mockSubjectContext = mock(SubjectContext.class);
        mockContext = new ServerContext(mockSubjectContext);

        mockHandler = mock(ResultHandler.class);
        Debug mockDebug = mock(Debug.class);

        testResource = new ApplicationTypesResource(mockDebug);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldHandleUnsupportedActionCollection() {

        //given
        ActionRequest mockRequest = mock(ActionRequest.class);

        //when
        testResource.actionCollection(mockContext, mockRequest, mockHandler);

        //then
        verify(mockHandler, times(1)).handleError(any(NotSupportedException.class));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldHandleUnsupportedActionInstance() {

        //given
        ActionRequest mockRequest = mock(ActionRequest.class);

        //when
        testResource.actionInstance(mockContext, "test", mockRequest, mockHandler);

        //then
        verify(mockHandler, times(1)).handleError(any(NotSupportedException.class));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldHandleUnsupportedCreateInstance() {

        //given
        CreateRequest mockRequest = mock(CreateRequest.class);

        //when
        testResource.createInstance(mockContext, mockRequest, mockHandler);

        //then
        verify(mockHandler, times(1)).handleError(any(NotSupportedException.class));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldHandleUnsupportedDeleteInstance() {

        //given
        DeleteRequest mockRequest = mock(DeleteRequest.class);

        //when
        testResource.deleteInstance(mockContext, "test", mockRequest, mockHandler);

        //then
        verify(mockHandler, times(1)).handleError(any(NotSupportedException.class));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldHandleUnsupportedPatchInstance() {

        //given
        PatchRequest mockRequest = mock(PatchRequest.class);

        //when
        testResource.patchInstance(mockContext, "test", mockRequest, mockHandler);

        //then
        verify(mockHandler, times(1)).handleError(any(NotSupportedException.class));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldHandleUnsupportedUpdateInstance() {

        //given
        UpdateRequest mockRequest = mock(UpdateRequest.class);

        //when
        testResource.updateInstance(mockContext, "test", mockRequest, mockHandler);

        //then
        verify(mockHandler, times(1)).handleError(any(NotSupportedException.class));

    }

    @Test
    public void shouldReturnNonPagableResponseFromSuccessfulQueryCollection() throws EntitlementException {
        //given
        Set<String> appNames = new LinkedHashSet<String>();
        appNames.add("testApp");
        appNames.add("testApp2");

        QueryRequest mockRequest = mock(QueryRequest.class);
        QueryResultHandler mockResultHandler = mock(QueryResultHandler.class);
        Subject mockSubject = mock(Subject.class);
        mockStatic(ApplicationTypeManager.class);

        when(mockSubjectContext.getCallerSubject()).thenReturn(mockSubject);
        when(ApplicationTypeManager.getApplicationTypeNames(mockSubject)).thenReturn(appNames);

        ArgumentCaptor<Resource> resourceArgument = ArgumentCaptor.forClass(Resource.class);

        //when
        testResource.queryCollection(mockContext, mockRequest, mockResultHandler);

        //then
        verify(mockResultHandler, times(1)).handleResource(resourceArgument.capture());
        verify(mockResultHandler, times(1)).handleResult(any(QueryResult.class));

        LinkedHashSet argToTest = (LinkedHashSet) resourceArgument.getValue().getContent().asMap().get("applicationTypeNames");

        assertEquals(appNames, argToTest);
    }

    @Test
    public void shouldHandleErrorIfNotAdminForQueryCollection() throws EntitlementException {
        //given
        QueryRequest mockRequest = mock(QueryRequest.class);
        QueryResultHandler mockResultHandler = mock(QueryResultHandler.class);
        mockStatic(ApplicationTypeManager.class);

        //when
        testResource.queryCollection(mockContext, mockRequest, mockResultHandler);

        //then
        verify(mockResultHandler, times(1)).handleError(any(ResourceException.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldHandleErrorIfNotAdminForReadInstance() throws EntitlementException {
        //given
        ReadRequest mockRequest = mock(ReadRequest.class);
        mockStatic(ApplicationTypeManager.class);

        //when
        testResource.readInstance(mockContext, "test", mockRequest, mockHandler);

        //then
        verify(mockHandler, times(1)).handleError(any(ResourceException.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReturnResponseFromSuccessfulReadInstance() throws EntitlementException {
        //given
        ApplicationType mockApplicationType = mock(ApplicationType.class);
        JsonValue jsonValueExpected = new JsonValue("{}");

        given(mockApplicationType.toJsonValue()).willReturn(jsonValueExpected);

        ReadRequest mockRequest = mock(ReadRequest.class);
        Subject mockSubject = mock(Subject.class);
        mockStatic(ApplicationTypeManager.class);
        when(ApplicationTypeManager.getAppplicationType(mockSubject, "test")).thenReturn(mockApplicationType);
        when(mockSubjectContext.getCallerSubject()).thenReturn(mockSubject);

        ArgumentCaptor<Resource> resourceArgument = ArgumentCaptor.forClass(Resource.class);

        //when
        testResource.readInstance(mockContext, "test", mockRequest, mockHandler);

        //then
        verify(mockHandler, times(1)).handleResult(resourceArgument.capture());

        assertEquals(jsonValueExpected.toString(), resourceArgument.getValue().getContent().toString());
        assertEquals("test", resourceArgument.getValue().getId());
        assertEquals(String.valueOf(mockApplicationType.hashCode()), resourceArgument.getValue().getRevision());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldErrorOnInvalidApplicationTypeWhenReadInstance() throws EntitlementException {
        //given
        ReadRequest mockRequest = mock(ReadRequest.class);
        Subject mockSubject = mock(Subject.class);
        mockStatic(ApplicationTypeManager.class);

        given(ApplicationTypeManager.getAppplicationType(mockSubject, "test")).willReturn(null);

        //when
        testResource.readInstance(mockContext, "test", mockRequest, mockHandler);

        //then
        verify(mockHandler, times(1)).handleError(any(ResourceException.class));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldErrorOnInvalidClassForApplicationType() throws EntitlementException {

        //given
        ApplicationType mockApplicationType = mock(ApplicationType.class);

        ReadRequest mockRequest = mock(ReadRequest.class);
        Subject mockSubject = mock(Subject.class);
        mockStatic(ApplicationTypeManager.class);
        when(ApplicationTypeManager.getAppplicationType(mockSubject, "test")).thenReturn(mockApplicationType);
        given(mockApplicationType.toJsonValue()).willThrow(new EntitlementException(6));

        //when
        testResource.readInstance(mockContext, "test", mockRequest, mockHandler);

        //then
        verify(mockHandler, times(1)).handleError(any(ResourceException.class));

    }

}
