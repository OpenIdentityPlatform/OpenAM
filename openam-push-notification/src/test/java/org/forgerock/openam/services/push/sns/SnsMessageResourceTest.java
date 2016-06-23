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
import static org.forgerock.openam.services.push.PushNotificationConstants.*;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sun.identity.shared.debug.Debug;
import java.util.concurrent.ExecutionException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.utils.JSONSerialisation;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.services.push.PushNotificationService;
import org.forgerock.openam.services.push.dispatch.MessageDispatcher;
import org.forgerock.openam.services.push.dispatch.PredicateNotMetException;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SnsMessageResourceTest {

    SnsMessageResource messageResource;
    PushNotificationService mockService;
    MessageDispatcher mockDispatcher;
    CTSPersistentStore mockCTS;
    JSONSerialisation mockSerialisation;
    JwtReconstruction mockReconstructor;

    @BeforeMethod
    public void theSetUp() { //you need this

        mockService = mock(PushNotificationService.class);
        Debug mockDebug = mock(Debug.class);
        mockCTS = mock(CTSPersistentStore.class);
        mockDispatcher = mock(MessageDispatcher.class);
        mockSerialisation = mock(JSONSerialisation.class);
        mockReconstructor = mock(JwtReconstruction.class);

        try {
            given(mockService.getMessageDispatcher(anyString())).willReturn(mockDispatcher);
        } catch (NotFoundException e) {
            //does not happen
        }

        messageResource = new SnsMessageResource(mockCTS, mockService, mockSerialisation, mockDebug, mockReconstructor);
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
        given(request.getAction()).willReturn("authenticate");

        //when
        Promise<ActionResponse, ResourceException> result = messageResource.authenticate(realmContext, request);

        //then
        verify(mockDispatcher, times(1)).handle("asdf", request.getContent());
        assertThat(result.get()).isNotNull();
    }

    @Test
    public void regShouldHandleByCTS() throws NotFoundException, PredicateNotMetException,
            ExecutionException, InterruptedException, CoreTokenException {
        //given
        Token mockToken = mock(Token.class);
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.setSubRealm("realm", "realm");

        JsonValue content = JsonValue.json(object(field("messageId", "asdf"), field("jwt", "")));

        ActionRequest request = mock(ActionRequest.class);
        given(request.getContent()).willReturn(content);
        given(request.getAction()).willReturn("register");
        doThrow(new NotFoundException()).when(mockDispatcher).handle(anyString(), (JsonValue) anyObject());
        given(mockCTS.read("asdf")).willReturn(mockToken);
        given(mockToken.getBlob()).willReturn("{ }".getBytes());
        given(mockSerialisation.serialise(any())).willReturn("");

        //when
        Promise<ActionResponse, ResourceException> result = messageResource.authenticate(realmContext, request);

        //then
        assertThat(result.get()).isNotNull();
        verify(mockToken, times(1)).setAttribute(CoreTokenField.INTEGER_ONE, ACCEPT_VALUE);
        verify(mockToken, times(1)).setBlob((byte[]) any());
        verify(mockCTS, times(1)).update(mockToken);
    }

    @Test
    public void authShouldHandleByCTS() throws NotFoundException, PredicateNotMetException,
            ExecutionException, InterruptedException, CoreTokenException {
        //given
        Jwt mockJwt = mock(Jwt.class);
        JwtClaimsSet mockClaimSet = mock(JwtClaimsSet.class);
        Token mockToken = mock(Token.class);
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        realmContext.setSubRealm("realm", "realm");

        JsonValue content = JsonValue.json(object(field("messageId", "asdf"), field("jwt", "")));

        ActionRequest request = mock(ActionRequest.class);
        given(request.getContent()).willReturn(content);
        given(request.getAction()).willReturn("authenticate");
        doThrow(new NotFoundException()).when(mockDispatcher).handle(anyString(), (JsonValue) anyObject());
        given(mockCTS.read("asdf")).willReturn(mockToken);
        given(mockToken.getBlob()).willReturn("{ }".getBytes());
        given(mockSerialisation.serialise(any())).willReturn("");
        given(mockReconstructor.reconstructJwt(anyString(), (Class<Jwt>) any())).willReturn(mockJwt);
        given(mockJwt.getClaimsSet()).willReturn(mockClaimSet);
        given(mockClaimSet.getClaim(anyString())).willReturn(null);

        //when
        Promise<ActionResponse, ResourceException> result = messageResource.authenticate(realmContext, request);

        //then
        assertThat(result.get()).isNotNull();
        verify(mockToken, times(1)).setAttribute(CoreTokenField.INTEGER_ONE, ACCEPT_VALUE);
        verify(mockCTS, times(1)).update(mockToken);
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
        given(request.getAction()).willReturn("authenticate");

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
        given(request.getAction()).willReturn("authenticate");

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
        given(request.getAction()).willReturn("authenticate");

        doThrow(new NotFoundException()).when(mockDispatcher).handle(anyString(), (JsonValue) anyObject());

        //when
        Promise<ActionResponse, ResourceException> result = messageResource.authenticate(realmContext, request);

        //then
        verify(mockDispatcher, times(1)).handle("asdf", request.getContent());
        result.getOrThrow();
    }

}
