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

package org.forgerock.openam.core.rest.dashboard;

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Resources.*;
import static org.forgerock.openam.utils.Time.*;
import static org.mockito.BDDMockito.anyObject;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTest;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.core.rest.devices.deviceprint.TrustedDevicesDao;
import org.forgerock.openam.core.rest.devices.deviceprint.TrustedDevicesResource;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.services.context.ClientContext;
import org.forgerock.services.context.Context;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TrustedDevicesResourceTest {

    private TrustedDevicesResource resource;

    private TrustedDevicesDao dao;
    private ContextHelper contextHelper;
    private RealmTestHelper realmTestHelper;

    @BeforeMethod
    public void setUp() throws Exception {

        dao = mock(TrustedDevicesDao.class);
        contextHelper = mock(ContextHelper.class);

        resource = new TrustedDevicesResource(dao, contextHelper);

        given(contextHelper.getUserId((Context) anyObject())).willReturn("demo");

        realmTestHelper = new RealmTestHelper();
        realmTestHelper.setupRealmClass();
    }

    @AfterMethod
    public void tearDown() {
        realmTestHelper.tearDownRealmClass();;
    }

    private Context ctx() {
        SSOTokenContext ssoTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(ssoTokenContext, Realm.root());
        return ClientContext.newInternalClientContext(realmContext);
    }

    @Test
    public void shouldQueryDevices() throws ResourceException {

        //Given
        QueryRequest request = Requests.newQueryRequest("");
        Connection connection = newInternalConnection(newCollection(resource));
        QueryResourceHandler handler = mock(QueryResourceHandler.class);
        List<JsonValue> devices = new ArrayList<>();
        devices.add(json(object(field("name", "NAME_1"), field("lastSelectedDate", newDate().getTime()))));
        devices.add(json(object(field("name", "NAME_2"), field("lastSelectedDate", newDate().getTime() + 1000))));

        given(dao.getDeviceProfiles(anyString(), anyString())).willReturn(devices);

        //When
        connection.query(ctx(), request, handler);

        //Then
        verify(handler, times(2)).handleResource(Matchers.<ResourceResponse>anyObject());
    }

    @Test
    public void shouldDeleteDevice() throws ResourceException {

        //Given
        DeleteRequest request = Requests.newDeleteRequest("UUID_1");
        Connection connection = newInternalConnection(newCollection(resource));
        List<JsonValue> devices = new ArrayList<JsonValue>();
        devices.add(json(object(field("uuid", "UUID_1"), field("name", "NAME_1"))));
        devices.add(json(object(field("uuid", "UUID_2"), field("name", "NAME_2"))));

        given(dao.getDeviceProfiles(anyString(), anyString())).willReturn(devices);

        //When
        connection.delete(ctx(), request);

        //Then
        ArgumentCaptor<List> devicesCaptor = ArgumentCaptor.forClass(List.class);
        verify(dao).saveDeviceProfiles(anyString(), anyString(), devicesCaptor.capture());
        assertThat(devicesCaptor.getValue()).hasSize(1);
    }

    @Test (expectedExceptions = NotFoundException.class)
    public void shouldNotDeleteDeviceWhenNotFound() throws ResourceException {

        //Given
        DeleteRequest request = Requests.newDeleteRequest("UUID_3");
        Connection connection = newInternalConnection(newCollection(resource));
        List<JsonValue> devices = new ArrayList<JsonValue>();
        devices.add(json(object(field("uuid", "UUID_1"), field("name", "NAME_1"))));
        devices.add(json(object(field("uuid", "UUID_2"), field("name", "NAME_2"))));

        given(dao.getDeviceProfiles(anyString(), anyString())).willReturn(devices);

        //When
        connection.delete(ctx(), request);

        //Then
        //Expected NotFoundException
    }
}
