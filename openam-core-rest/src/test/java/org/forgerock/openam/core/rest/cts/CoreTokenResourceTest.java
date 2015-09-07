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
 * Copyright 2013-2015 ForgeRock AS.
 */
package org.forgerock.openam.core.rest.cts;

import static org.forgerock.json.resource.test.assertj.AssertJResourceResponseAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.fail;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.utils.JSONSerialisation;
import org.forgerock.util.promise.Promise;
import org.mockito.Matchers;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CoreTokenResourceTest {

    private Debug mockDebug;
    private Token mockToken;
    private CTSPersistentStore mockStore;
    private JSONSerialisation mockSerialisation;
    private CoreTokenResource resource;

    @BeforeMethod
    public void setup() {
        mockToken = mock(Token.class);
        mockDebug = mock(Debug.class);
        mockStore = mock(CTSPersistentStore.class);
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
        resource.createInstance(null, request);

        // Then
        verify(mockStore).createAsync(mockToken);
    }

    @Test
    public void shouldGetBadRequestForMissingTokenId() throws CoreTokenException {
        // Given
        CreateRequest request = mock(CreateRequest.class);
        given(request.getContent()).willReturn(new JsonValue(""));
        doThrow(IllegalArgumentException.class).when(mockStore).createAsync(any(Token.class));

        // When
        Promise<ResourceResponse, ResourceException> promise = resource.createInstance(null, request);

        // Then
        assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
    }

    @Test
    public void shouldDeleteTokenBasedOnTokenId() throws CoreTokenException {
        // Given
        String one = "one";

        // When
        resource.deleteInstance(null, one, mock(DeleteRequest.class));

        // Then
        verify(mockStore).deleteAsync(one);
    }

    @Test
    public void shouldReadTokenFromStore() throws CoreTokenException {
        // Given
        String one = "badger";
        given(mockStore.read(anyString())).willReturn(mockToken);
        given(mockSerialisation.serialise(any())).willReturn("{ \"value\": \"some JSON\" }");

        // When
        resource.readInstance(null, one, mock(ReadRequest.class));

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
        Promise<ResourceResponse, ResourceException> promise = resource.readInstance(null, "", mock(ReadRequest.class));

        // Then
        assertThat(promise).succeeded().withContent().stringAt("value").isEqualTo("some JSON");
    }

    @Test
    public void shouldIndicateWhenNoTokenCanBeRead() throws CoreTokenException {
        // Given
        given(mockStore.read(anyString())).willReturn(null);

        // When
        Promise<ResourceResponse, ResourceException> promise = resource.readInstance(null, "badger", mock(ReadRequest.class));

        // Then
        assertThat(promise).failedWithException();
        try {
            promise.getOrThrowUninterruptibly();
            fail();
        } catch (ResourceException e) {
            Assert.assertEquals(e.getCode(), ResourceException.NOT_FOUND);
        }
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
        resource.updateInstance(null, "badger", updateRequest);

        // Then
        verify(mockStore).updateAsync(any(Token.class));
    }
}
