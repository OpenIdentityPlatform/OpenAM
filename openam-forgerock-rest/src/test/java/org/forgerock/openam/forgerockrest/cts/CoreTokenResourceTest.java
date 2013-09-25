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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.exceptions.DeleteFailedException;
import org.forgerock.openam.cts.utils.JSONSerialisation;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;

/**
 * @author robert.wapshott@forgerock.com
 */
public class CoreTokenResourceTest {
    @Test
    public void shouldUseDeserialisedTokenToCreate() throws CoreTokenException {
        // Given
        Token token = mock(Token.class);
        CTSPersistentStore store = mock(CTSPersistentStore.class);

        JSONSerialisation serialisation = mock(JSONSerialisation.class);
        given(serialisation.deserialise(anyString(), Matchers.<Class<Object>>any())).willReturn(token);

        CoreTokenResource resource = new CoreTokenResource(serialisation, store);

        CreateRequest request = mock(CreateRequest.class);
        given(request.getContent()).willReturn(new JsonValue(""));

        // When
        resource.createInstance(null, request, mock(ResultHandler.class));
        // Then
        verify(store).create(token);
    }

    @Test
    public void shouldDeleteTokenBasedOnTokenId() throws DeleteFailedException {
        // Given
        String one = "one";

        CTSPersistentStore store = mock(CTSPersistentStore.class);
        CoreTokenResource resource = new CoreTokenResource(mock(JSONSerialisation.class), store);

        // When
        resource.deleteInstance(null, one, mock(DeleteRequest.class), mock(ResultHandler.class));

        // Then
        verify(store).delete(one);
    }

    @Test
    public void shouldReadTokenFromStore() throws CoreTokenException {
        // Given
        String one = "badger";
        Token token = mock(Token.class);

        CTSPersistentStore store = mock(CTSPersistentStore.class);
        given(store.read(anyString())).willReturn(token);
        CoreTokenResource resource = new CoreTokenResource(mock(JSONSerialisation.class), store);

        // When
        resource.readInstance(null, one, mock(ReadRequest.class), mock(ResultHandler.class));

        // Then
        verify(store).read(one);
    }

    @Test
    public void shouldReadAndReturnTokenInSerialisedForm() throws CoreTokenException {
        // Given
        String serialisedToken = "badger";

        CTSPersistentStore store = mock(CTSPersistentStore.class);
        given(store.read(anyString())).willReturn(mock(Token.class));

        JSONSerialisation serialisation = mock(JSONSerialisation.class);
        given(serialisation.serialise(any(Token.class))).willReturn(serialisedToken);

        CoreTokenResource resource = new CoreTokenResource(serialisation, store);

        ResultHandler handler = mock(ResultHandler.class);

        // When
        resource.readInstance(null, "", mock(ReadRequest.class), handler);

        // Then
        ArgumentCaptor<Resource> captor = ArgumentCaptor.forClass(Resource.class);
        verify(handler).handleResult(captor.capture());
        Resource r = captor.getValue();
        assertEquals(r.getContent().toString(), new JsonValue(serialisedToken).toString());
    }

    @Test
    public void shouldIndicateWhenNoTokenCanBeRead() throws CoreTokenException {
        // Given
        CTSPersistentStore store = mock(CTSPersistentStore.class);
        given(store.read(anyString())).willReturn(null);

        CoreTokenResource resource = new CoreTokenResource(mock(JSONSerialisation.class), store);

        ResultHandler<Resource> handler = mock(ResultHandler.class);

        // When
        resource.readInstance(null, "badger", mock(ReadRequest.class), handler);

        // Then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(captor.capture());
        ResourceException exception = captor.getValue();
        assertEquals(exception.getCode(), ResourceException.NOT_FOUND);
    }

    @Test
    public void shouldUpdateUsingTokenInUpdateRequest() throws CoreTokenException {
        // Given
        JSONSerialisation serialisation = new JSONSerialisation();

        CTSPersistentStore store = mock(CTSPersistentStore.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);
        UpdateRequest updateRequest = mock(UpdateRequest.class);
        CoreTokenResource resource = new CoreTokenResource(serialisation, store);

        //Ensure the Token is included in the UpdateRequest
        Token token = new Token("badger", TokenType.OAUTH);
        String tokenJson = serialisation.serialise(token);
        given(updateRequest.getNewContent()).willReturn(new JsonValue(tokenJson));

        // When
        resource.updateInstance(null, "badger", updateRequest, handler);

        // Then
        verify(store).update(any(Token.class));
    }
}
