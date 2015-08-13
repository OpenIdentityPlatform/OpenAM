///*
// * The contents of this file are subject to the terms of the Common Development and
// * Distribution License (the License). You may not use this file except in compliance with the
// * License.
// *
// * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
// * specific language governing permission and limitations under the License.
// *
// * When distributing Covered Software, include this CDDL Header Notice in each file and include
// * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
// * Header, with the fields enclosed by brackets [] replaced by your own identifying
// * information: "Portions copyright [year] [name of copyright owner]".
// *
// * Copyright 2015 ForgeRock AS.
// */
//
//package org.forgerock.openam.rest.dashboard;
//
//import static org.fest.assertions.Assertions.*;
//import static org.forgerock.json.JsonValue.*;
//import static org.mockito.BDDMockito.anyObject;
//import static org.mockito.BDDMockito.anyString;
//import static org.mockito.BDDMockito.*;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//
//import com.iplanet.sso.SSOException;
//import com.iplanet.sso.SSOToken;
//import com.sun.identity.idm.AMIdentity;
//import com.sun.identity.idm.IdRepoException;
//import com.sun.identity.shared.debug.Debug;
//import com.sun.identity.sm.SMSException;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashSet;
//import java.util.List;
//import org.forgerock.json.JsonValue;
//import org.forgerock.json.resource.ActionRequest;
//import org.forgerock.json.resource.DeleteRequest;
//import org.forgerock.json.resource.InternalContext;
//import org.forgerock.json.resource.InternalServerErrorException;
//import org.forgerock.json.resource.QueryRequest;
//import org.forgerock.json.resource.QueryResultHandler;
//import org.forgerock.json.resource.Requests;
//import org.forgerock.json.resource.Resource;
//import org.forgerock.json.resource.ResourceException;
//import org.forgerock.json.resource.ResultHandler;
//import org.forgerock.http.Context;
//import org.forgerock.openam.rest.devices.OathDevicesDao;
//import org.forgerock.openam.rest.devices.OathDevicesResource;
//import org.forgerock.openam.rest.devices.services.OathService;
//import org.forgerock.openam.rest.devices.services.OathServiceFactory;
//import org.forgerock.openam.rest.resource.ContextHelper;
//import org.forgerock.openam.rest.RealmContext;
//import org.forgerock.openam.rest.resource.SSOTokenContext;
//import org.forgerock.openam.utils.JsonValueBuilder;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Matchers;
//import org.testng.annotations.BeforeMethod;
//import org.testng.annotations.Test;
//
//public class OathDevicesResourceTest {
//
//    private OathDevicesResource resource;
//
//    private OathDevicesDao dao;
//    private ContextHelper contextHelper;
//    private Debug debug;
//    private OathServiceFactory oathServiceFactory;
//    private OathService oathService;
//
//    @BeforeMethod
//    public void setUp() throws SMSException, SSOException {
//
//        dao = mock(OathDevicesDao.class);
//        contextHelper = mock(ContextHelper.class);
//        debug = mock(Debug.class);
//        oathServiceFactory = mock(OathServiceFactory.class);
//        oathService = mock(OathService.class);
//
//        resource = new OathDevicesResourceTestClass(dao, contextHelper, debug, oathServiceFactory);
//
//        given(contextHelper.getUserId((Context) anyObject())).willReturn("demo");
//        given(oathServiceFactory.create(anyString())).willReturn(oathService);
//    }
//
//    private Context ctx() throws SSOException {
//        SSOTokenContext mockSubjectContext = mock(SSOTokenContext.class);
//        given(mockSubjectContext.getCallerSSOToken()).willReturn(mock(SSOToken.class));
//        return new InternalContext(new RealmContext(mock(SSOTokenContext.class)));
//    }
//
//    @Test
//    public void shouldQueryTrustedDevices() throws ResourceException, SSOException {
//
//        //Given
//        QueryRequest request = Requests.newQueryRequest("");
//        QueryResultHandler handler = mock(QueryResultHandler.class);
//        List<JsonValue> devices = new ArrayList<JsonValue>();
//        devices.add(json(object(field("name", "NAME_1"), field("lastSelectedDate", new Date().getTime()))));
//        devices.add(json(object(field("name", "NAME_2"), field("lastSelectedDate", new Date().getTime() + 1000))));
//
//        given(dao.getDeviceProfiles(anyString(), anyString())).willReturn(devices);
//
//        //When
//        resource.queryCollection(ctx(), request, handler);
//
//        //Then
//        verify(handler, times(2)).handleResource(Matchers.<Resource>anyObject());
//    }
//
//    @Test
//    public void shouldDeleteTrustedDevice() throws ResourceException, SSOException {
//
//        //Given
//        DeleteRequest request = Requests.newDeleteRequest("UUID_2");
//        ResultHandler handler = mock(ResultHandler.class);
//
//        List<JsonValue> devices = new ArrayList<JsonValue>();
//        devices.add(json(object(field("uuid", "UUID_1"), field("name", "NAME_1"))));
//        devices.add(json(object(field("uuid", "UUID_2"), field("name", "NAME_2"))));
//
//        given(dao.getDeviceProfiles(anyString(), anyString())).willReturn(devices);
//
//        //When
//        resource.deleteInstance(ctx(), request.getResourceName(), request, handler);
//
//        //Then
//        ArgumentCaptor<List> devicesCaptor = ArgumentCaptor.forClass(List.class);
//        verify(dao).saveDeviceProfiles(anyString(), anyString(), devicesCaptor.capture());
//        assertThat(devicesCaptor.getValue()).hasSize(1);
//    }
//
//    @Test
//    public void shouldNotDeleteTrustedDeviceWhenNotFound() throws ResourceException, SSOException {
//
//        //Given
//        DeleteRequest request = Requests.newDeleteRequest("UUID_3");
//        ResultHandler handler = mock(ResultHandler.class);
//        List<JsonValue> devices = new ArrayList<JsonValue>();
//        devices.add(json(object(field("uuid", "UUID_1"), field("name", "NAME_1"))));
//        devices.add(json(object(field("uuid", "UUID_2"), field("name", "NAME_2"))));
//
//        given(dao.getDeviceProfiles(anyString(), anyString())).willReturn(devices);
//
//        //When
//        resource.deleteInstance(ctx(), request.getResourceName(), request, handler);
//
//        //Then
//        ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
//        verify(handler).handleError(exceptionCaptor.capture());
//        assertThat(exceptionCaptor.getValue().getCode() == ResourceException.NOT_FOUND);
//    }
//
//    @Test
//    public void shouldFailOnUnknownAction() throws ResourceException, SSOException {
//
//        //given
//        ActionRequest request = Requests.newActionRequest("instanceId", "fake");
//        ResultHandler handler = mock(ResultHandler.class);
//
//        //when
//        resource.actionCollection(ctx(), request, handler);
//
//        //then
//        ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
//        verify(handler).handleError(exceptionCaptor.capture());
//        assertThat(exceptionCaptor.getValue().getCode() == ResourceException.NOT_SUPPORTED);
//    }
//
//    @Test
//    public void shouldExecuteSkipAction() throws ResourceException, SSOException {
//
//        //given
//        JsonValue contents = JsonValueBuilder.toJsonValue("{ \"value\" : true }");
//        JsonValue successResult = JsonValueBuilder.jsonValue().build();
//        ActionRequest request = Requests.newActionRequest("instanceId", "skip");
//        request.setContent(contents);
//        ResultHandler handler = mock(ResultHandler.class);
//
//        //when
//        resource.actionCollection(ctx(), request, handler);
//
//        //then
//        ArgumentCaptor<JsonValue> jsonCaptor = ArgumentCaptor.forClass(JsonValue.class);
//        verify(handler, times(1)).handleResult(jsonCaptor.capture());
//        assertThat(successResult.toString()).isEqualTo(jsonCaptor.getValue().toString());
//    }
//
//    @Test
//    public void shouldExecuteTrueCheckAction() throws ResourceException, SSOException {
//
//        //given
//        JsonValue successResult = JsonValueBuilder.toJsonValue("{ \"result\" : true }");
//        ActionRequest request = Requests.newActionRequest("instanceId", "check");
//        ResultHandler handler = mock(ResultHandler.class);
//
//        //when
//        resource.actionCollection(ctx(), request, handler);
//
//        //then
//        ArgumentCaptor<JsonValue> jsonCaptor = ArgumentCaptor.forClass(JsonValue.class);
//        verify(handler, times(1)).handleResult(jsonCaptor.capture());
//        assertThat(successResult.toString()).isEqualTo(jsonCaptor.getValue().toString());
//    }
//
//    @Test
//    public void shouldFailOnUnknownActionInstance() throws ResourceException, SSOException {
//
//        //given
//        ResultHandler handler = mock(ResultHandler.class);
//        ActionRequest actionRequest = mock(ActionRequest.class);
//
//
//        //when
//        resource.actionInstance(ctx(), "", actionRequest, handler);
//
//        //then
//        ArgumentCaptor<ResourceException> exceptionCaptor = ArgumentCaptor.forClass(ResourceException.class);
//        verify(handler).handleError(exceptionCaptor.capture());
//        assertThat(exceptionCaptor.getValue().getCode() == ResourceException.NOT_SUPPORTED);
//    }
//
//    private static class OathDevicesResourceTestClass extends OathDevicesResource {
//
//
//        public OathDevicesResourceTestClass(OathDevicesDao dao, ContextHelper helper, Debug debug,
//                                            OathServiceFactory oathServiceFactory) {
//            super(dao, helper, debug, oathServiceFactory, helper);
//        }
//
//        protected AMIdentity getUserIdFromUri(Context context) throws InternalServerErrorException {
//
//            HashSet<String> attribute = new HashSet<>();
//            attribute.add(String.valueOf(OathService.SKIPPABLE));
//
//            AMIdentity mockId = mock(AMIdentity.class);
//            try {
//                given(mockId.getAttribute(anyString())).willReturn(attribute); //makes them
//            } catch (IdRepoException | SSOException e) {
//                e.printStackTrace();
//            }
//            return mockId;
//        }
//    }
//}
