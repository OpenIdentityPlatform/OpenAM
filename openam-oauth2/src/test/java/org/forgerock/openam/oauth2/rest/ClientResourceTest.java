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
 * Copyright 2012-2016 ForgeRock AS.
 */
package org.forgerock.openam.oauth2.rest;


import static org.forgerock.json.resource.test.assertj.AssertJResourceResponseAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.oauth2.OAuth2AuditLogger;
import org.forgerock.util.promise.Promise;
import org.mockito.Mockito;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;


public class ClientResourceTest {

    private ClientResource resource = null;
    private ClientResourceManager mockManager = null;
    private ServiceSchema mockSubSchema = null;

    @BeforeTest
    public void setUp() throws SMSException {
        mockManager = mock(ClientResourceManager.class);
        ServiceSchemaManager mockSchemaManager = mock(ServiceSchemaManager.class);

        ServiceSchema mockSchema = mock(ServiceSchema.class);
        mockSubSchema = mock(ServiceSchema.class);

        Mockito.doReturn(mockSchema).when(mockSchemaManager).getOrganizationSchema();
        Mockito.doReturn(mockSubSchema).when(mockSchema).getSubSchema(anyString());

        resource = new ClientResource(
                mockManager,
                mock(CTSPersistentStore.class),
                mockSchemaManager,
                mock(OAuth2AuditLogger.class),
                mock(Debug.class));
    }

    @Test
    public void shouldCreateIdentity() throws SSOException, IdRepoException, ResourceException {
        // Given
        Map<String, ArrayList<String>> client = new HashMap<>();

        //must contain userpassword, realm, client_id, "com.forgerock.openam.oauth2provider.clientType"
        client.put("client_id", new ArrayList(Arrays.asList("client")));
        client.put("userpassword", new ArrayList(Arrays.asList("password")));
        client.put("realm", new ArrayList(Arrays.asList("/")));
        client.put("com.forgerock.openam.oauth2provider.clientType", new ArrayList(Arrays.asList("Public")));
        when(mockSubSchema.getAttributeSchema(eq("client"))).thenReturn(mock(AttributeSchema.class));
        when(mockSubSchema.getAttributeSchema(eq("userpassword"))).thenReturn(mock(AttributeSchema.class));
        when(mockSubSchema.getAttributeSchema(eq("realm"))).thenReturn(mock(AttributeSchema.class));
        when(mockSubSchema.getAttributeSchema(eq("com.forgerock.openam.oauth2provider.clientType")))
                .thenReturn(mock(AttributeSchema.class));

        CreateRequest request = mock(CreateRequest.class);
        JsonValue val = mock(JsonValue.class);
        when(request.getContent()).thenReturn(val);
        when(val.getObject()).thenReturn(client);

        // When
        Promise<ResourceResponse, ResourceException> createInstancePromise
                = resource.createInstance(null, request);

        // Then
        assertThat(createInstancePromise).succeeded().withId().isEqualTo("results");
        assertThat(createInstancePromise).succeeded().withContent().stringAt("success").isEqualTo("true");
    }

    @Test
    public void shouldDeleteIdentity()
            throws SSOException, IdRepoException, ResourceException {
        //Given
        String resourceId = "client";
        DeleteRequest request = mock(DeleteRequest.class);

        // When
        Promise<ResourceResponse, ResourceException> deletePromise = resource.deleteInstance(null, resourceId, request);

        // Then
        assertThat(deletePromise).succeeded().withId().isEqualTo("results");
        assertThat(deletePromise).succeeded().withRevision().isEqualTo("1");
        assertThat(deletePromise).succeeded().withContent().stringAt("success").isEqualTo("true");
    }
}
