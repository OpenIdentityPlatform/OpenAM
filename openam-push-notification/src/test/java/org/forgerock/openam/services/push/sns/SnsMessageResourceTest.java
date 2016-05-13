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
* Copyright 2016 ForgeRock AS.
*/
package org.forgerock.openam.services.push.sns;

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.json.JsonValue.*;
import static org.mockito.BDDMockito.anyObject;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.mock;

import com.sun.identity.shared.debug.Debug;
import java.util.concurrent.ExecutionException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.utils.JSONSerialisation;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.services.push.dispatch.MessageDispatcher;
import org.forgerock.openam.services.push.dispatch.PredicateNotMetException;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SnsMessageResourceTest {


    SnsMessageResource messageResource;
    MessageDispatcher mockDispatcher;
    CTSPersistentStore mockCTS;

    @BeforeMethod
    public void theSetUp() { //you need this

        mockDispatcher = mock(MessageDispatcher.class);
        Debug mockDebug = mock(Debug.class);
        mockCTS = mock(CTSPersistentStore.class);
        JSONSerialisation mockSerialisation = mock(JSONSerialisation.class);
        messageResource = new SnsMessageResource(mockCTS, mockDispatcher, mockSerialisation, mockDebug);
    }

    @Test
    public void shouldHandle() throws NotFoundException, PredicateNotMetException,
            ExecutionException, InterruptedException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.setSubRealm("realm", "realm");

        JsonValue content = JsonValue.json(object(field("messageId", "asdf"), field("jwt", "")));

        ActionRequest request = mock(ActionRequest.class);
        given(request.getContent()).willReturn(content);

        //when
        Promise<ActionResponse, ResourceException> result = messageResource.authenticate(realmContext, request);

        //then
        verify(mockDispatcher, times(1)).handle("asdf", request.getContent());
        assertThat(result.get()).isNotNull();
    }

    @Test (expectedExceptions = BadRequestException.class)
    public void shouldFailWhenNoMessageId() throws ResourceException, InterruptedException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.setSubRealm("realm", "realm");

        JsonValue content = JsonValue.json(object(field("test", "test")));

        ActionRequest request = mock(ActionRequest.class);
        given(request.getContent()).willReturn(content);

        //when
        Promise<ActionResponse, ResourceException> result = messageResource.authenticate(realmContext, request);

        //then
        result.getOrThrow();
    }

    @Test (expectedExceptions = BadRequestException.class)
    public void shouldFailWhenPredicateNotMet() throws ResourceException, InterruptedException,
            PredicateNotMetException {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.setSubRealm("realm", "realm");

        JsonValue content = JsonValue.json(object(field("messageId", "asdf"), field("jwt", "")));

        ActionRequest request = mock(ActionRequest.class);
        given(request.getContent()).willReturn(content);

        doThrow(new PredicateNotMetException("")).when(mockDispatcher).handle(anyString(), (JsonValue) anyObject());

        //when
        Promise<ActionResponse, ResourceException> result = messageResource.authenticate(realmContext, request);

        //then
        result.getOrThrow();
    }

    @Test (expectedExceptions = BadRequestException.class)
    public void shouldFailWhenLocalAndCTSReadsFail() throws ResourceException, InterruptedException,
            PredicateNotMetException, CoreTokenException {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.setSubRealm("realm", "realm");

        JsonValue content = JsonValue.json(object(field("messageId", "asdf"), field("jwt", "")));

        ActionRequest request = mock(ActionRequest.class);
        given(request.getContent()).willReturn(content);
        given(mockCTS.read("asdf")).willReturn(null);

        doThrow(new NotFoundException()).when(mockDispatcher).handle(anyString(), (JsonValue) anyObject());

        //when
        Promise<ActionResponse, ResourceException> result = messageResource.authenticate(realmContext, request);

        //then
        verify(mockDispatcher, times(1)).handle("asdf", request.getContent());
        result.getOrThrow();
    }

}
