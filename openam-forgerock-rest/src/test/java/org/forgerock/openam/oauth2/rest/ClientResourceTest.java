/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions copyright [year] [name of copyright owner]"
 */

package org.forgerock.openam.oauth2.rest;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.ldap.CTSPersistentStore;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

public class ClientResourceTest {

    @Test
    public void shouldCreateIdentity() throws SSOException, IdRepoException, InternalServerErrorException, SMSException {
        // Given
        Map<String, ArrayList<String>> client = new HashMap<String, ArrayList<String>>();

        //must contain userpassword, realm, client_id, "com.forgerock.openam.oauth2provider.clientType"
        ArrayList<String> temp = new ArrayList<String>();
        temp.add("client");
        client.put("client_id",temp);
        temp = new ArrayList<String>();
        temp.add("password");
        client.put("userpassword",temp);
        temp = new ArrayList<String>();
        temp.add("/");
        client.put("realm",temp);
        temp = new ArrayList<String>();
        temp.add("Public");
        client.put("com.forgerock.openam.oauth2provider.clientType",temp);

        ClientResourceManager mockManager = mock(ClientResourceManager.class);

        //setup mockManager
        when(mockManager.usersEqual(any(ServerContext.class))).thenReturn(true);
        doNothing().when(mockManager).deleteIdentity(anyString());
        doNothing().when(mockManager).createIdentity(anyString(), anyString(),anyMap());

        ResultHandler mockHandler = mock(ResultHandler.class);
        CreateRequest request = mock(CreateRequest.class);
        JsonValue val = mock(JsonValue.class);
        when(request.getContent()).thenReturn(val);
        when(val.getObject()).thenReturn(client);

        ServiceSchemaManager mockServiceSchemaManager = mock(ServiceSchemaManager.class);
        ServiceSchema mockServiceSchema = mock(ServiceSchema.class);
        AttributeSchema mockAttributeSchema = mock(AttributeSchema.class);
        when(mockServiceSchemaManager.getSchema(anyString())).thenReturn(mockServiceSchema);
        when(mockServiceSchema.getAttributeSchema(anyString())).thenReturn(mockAttributeSchema);
        when(mockAttributeSchema.getUIType()).thenReturn(AttributeSchema.UIType.LINK);


        Map<String, String> responseVal = new HashMap<String, String>();
        JsonValue response = null;
        responseVal.put("success", "true");
        response = new JsonValue(responseVal);

        Resource expectedResource = new Resource("results", "1", response);

        ClientResource resource = spy(new ClientResource(mockManager, mock(CTSPersistentStore.class), mockServiceSchemaManager));

        // When
        resource.createInstance(null, request, mockHandler);

        // Then
        verify(mockHandler, times(1)).handleResult(expectedResource);
        verify(mockHandler, times(0)).handleError(any(ResourceException.class));
    }

    @Test
    public void shouldDeleteIdentity() throws SSOException, IdRepoException, InternalServerErrorException {
        //Given
        ClientResourceManager mockManager = mock(ClientResourceManager.class);

        //setup mockManager
        when(mockManager.usersEqual(any(ServerContext.class))).thenReturn(true);
        doNothing().when(mockManager).deleteIdentity(anyString());
        doNothing().when(mockManager).createIdentity(anyString(), anyString(),anyMap());

        ResultHandler mockHandler = mock(ResultHandler.class);
        DeleteRequest request = mock(DeleteRequest.class);


        Map<String, String> responseVal = new HashMap<String, String>();
        JsonValue response = null;
        responseVal.put("success", "true");
        response = new JsonValue(responseVal);

        Resource expectedResource = new Resource("results", "1", response);

        ClientResource resource = spy(new ClientResource(mockManager, mock(CTSPersistentStore.class),  mock(ServiceSchemaManager.class)));

        // When
        resource.deleteInstance(null, "client", request, mockHandler);

        // Then
        verify(mockHandler, times(1)).handleResult(expectedResource);
        verify(mockHandler, times(0)).handleError(any(ResourceException.class));
    }
}