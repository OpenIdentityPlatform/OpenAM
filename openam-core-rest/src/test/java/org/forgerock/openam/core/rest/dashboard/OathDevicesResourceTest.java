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

package org.forgerock.openam.core.rest.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.test.assertj.AssertJActionResponseAssert.assertThat;
import static org.forgerock.json.resource.test.assertj.AssertJQueryResponseAssert.assertThat;
import static org.forgerock.json.resource.test.assertj.AssertJResourceResponseAssert.assertThat;
import static org.forgerock.openam.utils.Time.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.ClientContext;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.core.rest.devices.OathDevicesDao;
import org.forgerock.openam.core.rest.devices.OathDevicesResource;
import org.forgerock.openam.core.rest.devices.services.AuthenticatorOathService;
import org.forgerock.openam.core.rest.devices.services.AuthenticatorOathServiceFactory;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.util.promise.Promise;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OathDevicesResourceTest {

    private OathDevicesResource resource;

    private static final String USER_ID = "demo";
    @Mock
    private OathDevicesDao dao;
    @Mock
    private ContextHelper contextHelper;
    @Mock
    private ContextHelper context;
    @Mock
    private Debug debug;
    @Mock
    private AuthenticatorOathServiceFactory oathServiceFactory;
    @Mock
    private AuthenticatorOathService oathService;

    @BeforeMethod
    public void setUp() throws SMSException, SSOException {

        MockitoAnnotations.initMocks(this);
        resource = new OathDevicesResourceTestClass(dao, contextHelper, debug, oathServiceFactory);

        given(contextHelper.getUserId((Context) anyObject())).willReturn(USER_ID);
        given(oathServiceFactory.create(anyString())).willReturn(oathService);
    }

    private Context ctx() throws SSOException {
        SSOTokenContext mockSubjectContext = mock(SSOTokenContext.class);
        given(mockSubjectContext.getCallerSSOToken()).willReturn(mock(SSOToken.class));
        return ClientContext.newInternalClientContext(new RealmContext(mock(SSOTokenContext.class)));
    }

    @Test
    public void shouldQueryTrustedDevices() throws ResourceException, SSOException {
        // Given
        QueryRequest request = Requests.newQueryRequest("");
        QueryResourceHandler handler = mock(QueryResourceHandler.class);
        List<JsonValue> devices = new ArrayList<JsonValue>();
        devices.add(json(object(field("name", "NAME_1"), field("lastSelectedDate", newDate().getTime()))));
        devices.add(json(object(field("name", "NAME_2"), field("lastSelectedDate", newDate().getTime() + 1000))));

        given(dao.getDeviceProfiles(anyString(), anyString())).willReturn(devices);

        // When
        Promise<QueryResponse, ResourceException> actual = resource.queryCollection(ctx(), request, handler);
        QueryResponse response = actual.getOrThrowUninterruptibly();

        // Then
        assertThat(response).isNotNull();
        verify(dao, times(1)).getDeviceProfiles(USER_ID, "/");
        verify(handler, times(2)).handleResource(any(ResourceResponse.class));
    }

    /**
     * Tests the OathDeveResources deleteInstance() method.
     * 
     * @throws ResourceException
     * @throws SSOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void shouldDeleteTrustedDevice()
            throws ResourceException, SSOException, InterruptedException, ExecutionException {

        // GIVEN
        DeleteRequest request = Requests.newDeleteRequest("UUID_2");

        List<JsonValue> devices = new ArrayList<JsonValue>();
        devices.add(json(object(field("uuid", "UUID_1"), field("name", "NAME_1"))));
        devices.add(json(object(field("uuid", "UUID_2"), field("name", "NAME_2"))));

        given(dao.getDeviceProfiles(anyString(), anyString())).willReturn(devices);

        // WHEN
        Promise<ResourceResponse, ResourceException> actual = resource.deleteInstance(ctx(), request.getResourcePath(),
                request);

        // THEN
        assertThat(actual).succeeded();
        ArgumentCaptor<List> devicesCaptor = ArgumentCaptor.forClass(List.class);
        verify(dao).saveDeviceProfiles(anyString(), anyString(), devicesCaptor.capture());
        assertThat(devicesCaptor.getValue()).hasSize(1);
    }

    @Test
    public void shouldNotDeleteTrustedDeviceWhenNotFound() throws ResourceException, SSOException {

        // Given
        DeleteRequest request = Requests.newDeleteRequest("UUID_3");
        List<JsonValue> devices = new ArrayList<JsonValue>();
        devices.add(json(object(field("uuid", "UUID_1"), field("name", "NAME_1"))));
        devices.add(json(object(field("uuid", "UUID_2"), field("name", "NAME_2"))));

        given(dao.getDeviceProfiles(anyString(), anyString())).willReturn(devices);

        // When
        Promise<ResourceResponse, ResourceException> promise = resource.deleteInstance(ctx(), request.getResourcePath(),
                request);

        // Then
        assertThat(promise).failedWithResourceException().withCode(ResourceException.NOT_FOUND);
    }

    @Test
    public void shouldFailOnUnknownAction() throws ResourceException, SSOException {
        // given
        ActionRequest request = Requests.newActionRequest("instanceId", "fake");

        // when
        Promise<ActionResponse, ResourceException> promise = resource.actionCollection(ctx(), request);

        // then
        assertThat(promise).failedWithResourceException().withCode(ResourceException.NOT_SUPPORTED);
    }

    @Test
    public void shouldExecuteSkipAction()
            throws ResourceException, SSOException, InterruptedException, ExecutionException {
        // given
        JsonValue contents = JsonValueBuilder.toJsonValue("{ \"value\" : true }");
        ActionRequest request = Requests.newActionRequest("instanceId", "skip");
        request.setContent(contents);

        // when
        Promise<ActionResponse, ResourceException> promise = resource.actionCollection(ctx(), request);

        // then
        assertThat(promise).succeeded().withContent().isObject().containsOnly();
    }

    @Test
    public void shouldExecuteTrueCheckAction()
            throws ResourceException, SSOException, InterruptedException, ExecutionException {
        // give
        ActionRequest request = Requests.newActionRequest("instanceId", "check");

        // when
        Promise<ActionResponse, ResourceException> promise = resource.actionCollection(ctx(), request);

        // then
        assertThat(promise).succeeded().withContent().booleanAt("result").isTrue();
    }

    @Test
    public void shouldFailOnUnknownActionInstance() throws ResourceException, SSOException {
        // given
        ActionRequest actionRequest = mock(ActionRequest.class);

        // when
        Promise<ActionResponse, ResourceException> promise = resource.actionInstance(ctx(), "", actionRequest);

        // then
        assertThat(promise).failedWithResourceException().withCode(ResourceException.NOT_SUPPORTED);
    }

    private static class OathDevicesResourceTestClass extends OathDevicesResource {

        public OathDevicesResourceTestClass(OathDevicesDao dao, ContextHelper helper, Debug debug,
                AuthenticatorOathServiceFactory oathServiceFactory) {
            super(dao, helper, debug, oathServiceFactory, helper);
        }

        protected AMIdentity getUserIdFromUri(Context context) throws InternalServerErrorException {

            HashSet<String> attribute = new HashSet<>();
            attribute.add(String.valueOf(AuthenticatorOathService.SKIPPABLE));

            AMIdentity mockId = mock(AMIdentity.class);
            try {
                given(mockId.getAttribute(anyString())).willReturn(attribute); // makes them
            } catch (IdRepoException | SSOException e) {
                e.printStackTrace();
            }
            return mockId;
        }
    }
}
