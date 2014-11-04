/**
 * Copyright 2013 ForgeRock, Inc.
 *
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
 */
package org.forgerock.openam.forgerockrest.cts;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.utils.JSONSerialisation;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

public class CoreTokenResourceTest {

    private Debug mockDebug;
    private Token mockToken;
    private CTSPersistentStore mockStore;
    private JSONSerialisation mockSerialisation;
    private CoreTokenResource resource;
    private ResultHandler mockHandler;

    @BeforeMethod
    public void setup() {
        mockToken = mock(Token.class);
        mockDebug = mock(Debug.class);
        mockStore = mock(CTSPersistentStore.class);
        mockHandler = mock(ResultHandler.class);
        mockSerialisation = mock(JSONSerialisation.class);

        resource = new CoreTokenResource(mockSerialisation, mockStore, mockDebug);
    }

    @Test
    public void shouldCreateTokenInCTS() throws CoreTokenException {
        // Given
        CreateRequest request = mock(CreateRequest.class);
        given(request.getContent()).willReturn(new JsonValue(""));
        given(mockSerialisation.deserialise(anyString(), Matchers.<Class<Object>>any())).willReturn(mockToken);

        // When
        resource.createInstance(null, request, mockHandler);

        // Then
        verify(mockStore).create(mockToken);
    }

    @Test
    public void shouldDeleteTokenBasedOnTokenId() throws CoreTokenException {
        // Given
        String one = "one";

        // When
        resource.deleteInstance(null, one, mock(DeleteRequest.class), mockHandler);

        // Then
        verify(mockStore).delete(one);
    }

    @Test
    public void shouldReadTokenFromStore() throws CoreTokenException {
        // Given
        String one = "badger";
        given(mockStore.read(anyString())).willReturn(mockToken);
        given(mockSerialisation.serialise(any())).willReturn("{ \"value\": \"some JSON\" }");

        // When
        resource.readInstance(null, one, mock(ReadRequest.class), mockHandler);

        // Then
        verify(mockStore).read(one);
    }

    @Test
    public void shouldReadAndReturnTokenInSerialisedForm() throws CoreTokenException {
        // Given
        String serialisedToken = "{ \"value\": \"some JSON\" }";
        given(mockStore.read(anyString())).willReturn(mockToken);
        given(mockSerialisation.serialise(any(Token.class))).willReturn(serialisedToken);

        // When
        resource.readInstance(null, "", mock(ReadRequest.class), mockHandler);

        // Then
        ArgumentCaptor<Resource> captor = ArgumentCaptor.forClass(Resource.class);
        verify(mockHandler).handleResult(captor.capture());
        Resource r = captor.getValue();
        assertThat(r.getContent().toString()).isEqualTo(serialisedToken);
    }

    @Test
    public void shouldIndicateWhenNoTokenCanBeRead() throws CoreTokenException {
        // Given
        given(mockStore.read(anyString())).willReturn(null);

        // When
        resource.readInstance(null, "badger", mock(ReadRequest.class), mockHandler);

        // Then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockHandler).handleError(captor.capture());
        ResourceException exception = captor.getValue();
        Assert.assertEquals(exception.getCode(), ResourceException.NOT_FOUND);
    }

    @Test
    public void shouldUpdateUsingTokenInUpdateRequest() throws CoreTokenException {
        // Given
        UpdateRequest updateRequest = mock(UpdateRequest.class);
        JsonValue value = mock(JsonValue.class);
        given(value.toString()).willReturn("{ \"value\": \"test\" }");
        given(updateRequest.getContent()).willReturn(value);
        given(mockSerialisation.deserialise(anyString(), Matchers.<Class<Object>>any())).willReturn(mockToken);

        // When
        resource.updateInstance(null, "badger", updateRequest, mockHandler);

        // Then
        verify(mockStore).update(any(Token.class));
    }
}
