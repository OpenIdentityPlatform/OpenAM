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

package org.forgerock.openam.rest.dashboard;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.RootContext;
import org.forgerock.json.resource.ServerContext;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.*;
import static org.forgerock.json.resource.Resources.newCollection;
import static org.forgerock.json.resource.Resources.newInternalConnection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class TrustedDevicesResourceTest {

    private TrustedDevicesResource resource;

    private TrustedDevicesDao dao;

    @BeforeMethod
    public void setUp() {

        dao = mock(TrustedDevicesDao.class);

        resource = new TrustedDevicesResource(dao);
    }

    private Context ctx() {
        return new ServerContext(new RootContext());
    }

    @Test
    public void shouldQueryTrustedDevices() throws ResourceException {

        //Given
        QueryRequest request = Requests.newQueryRequest("");
        Connection connection = newInternalConnection(newCollection(resource));
        QueryResultHandler handler = mock(QueryResultHandler.class);
        List<JsonValue> devices = new ArrayList<JsonValue>();
        devices.add(json(object(field("name", "NAME_1"), field("lastSelectedDate", new Date().getTime()))));
        devices.add(json(object(field("name", "NAME_2"), field("lastSelectedDate", new Date().getTime() + 1000))));

        given(dao.getDeviceProfiles(Matchers.<Context>anyObject())).willReturn(devices);

        //When
        connection.query(ctx(), request, handler);

        //Then
        verify(handler, times(2)).handleResource(Matchers.<Resource>anyObject());
    }

    @Test
    public void shouldDeleteTrustedDevice() throws ResourceException {

        //Given
        DeleteRequest request = Requests.newDeleteRequest("UUID_1");
        Connection connection = newInternalConnection(newCollection(resource));
        List<JsonValue> devices = new ArrayList<JsonValue>();
        devices.add(json(object(field("uuid", "UUID_1"), field("name", "NAME_1"))));
        devices.add(json(object(field("uuid", "UUID_2"), field("name", "NAME_2"))));

        given(dao.getDeviceProfiles(Matchers.<Context>anyObject())).willReturn(devices);

        //When
        connection.delete(ctx(), request);

        //Then
        ArgumentCaptor<List> devicesCaptor = ArgumentCaptor.forClass(List.class);
        verify(dao).saveDeviceProfiles(Matchers.<Context>anyObject(), devicesCaptor.capture());
        assertThat(devicesCaptor.getValue()).hasSize(1);
    }

    @Test (expectedExceptions = NotFoundException.class)
    public void shouldNotDeleteTrustedDeviceWhenNotFound() throws ResourceException {

        //Given
        DeleteRequest request = Requests.newDeleteRequest("UUID_3");
        Connection connection = newInternalConnection(newCollection(resource));
        List<JsonValue> devices = new ArrayList<JsonValue>();
        devices.add(json(object(field("uuid", "UUID_1"), field("name", "NAME_1"))));
        devices.add(json(object(field("uuid", "UUID_2"), field("name", "NAME_2"))));

        given(dao.getDeviceProfiles(Matchers.<Context>anyObject())).willReturn(devices);

        //When
        connection.delete(ctx(), request);

        //Then
        //Expected NotFoundException
    }
}
