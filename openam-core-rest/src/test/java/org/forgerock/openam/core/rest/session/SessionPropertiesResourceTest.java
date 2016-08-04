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

package org.forgerock.openam.core.rest.session;


import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.resource.test.assertj.AssertJResourceResponseAssert.assertThat;
import static org.forgerock.openam.core.rest.session.SessionPropertiesResource.TOKEN_ID_PARAM_NAME;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.session.SessionPropertyWhitelist;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.junit.runner.RunWith;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SessionPropertiesResourceTest {


    private SSOTokenContext ssoTokenContext = mock(SSOTokenContext.class);
    private RealmContext realmContext = mock(RealmContext.class);
    private Realm realm = mock(Realm.class);
    private Map<String, String> uriVariables = mock(Map.class);
    private Context context = mock(Context.class);
    private SessionUtilsWrapper sessionUtilsWrapper;
    private UriRouterContext uriRouterContext;
    private SSOToken ssoToken;
    private SSOTokenManager ssoTokenManager;
    private SessionPropertyWhitelist whiteList;
    private SessionResourceUtil sessionResourceUtil;
    private SessionPropertiesResource resource;
    private String tokenId = "tokenId";
    private String realmpath = "realm";
    private AMIdentity amIdentity;
    private Map<String, String> properties;
    private Map<String, String> templateVariables = new HashMap<>();

    @BeforeMethod
    public void setup() throws SSOException, IdRepoException {

        properties = new HashMap<>();
        ssoToken = mock(SSOToken.class);
        templateVariables.put(TOKEN_ID_PARAM_NAME, tokenId);
        whiteList = mock(SessionPropertyWhitelist.class);
        ssoTokenManager = mock(SSOTokenManager.class);
        uriRouterContext = new UriRouterContext(context, "/" + tokenId, "", Collections.<String, String>emptyMap());
        given(uriVariables.get(TOKEN_ID_PARAM_NAME)).willReturn(tokenId);
        given(context.containsContext(RealmContext.class)).willReturn(true);
        given(context.asContext(RealmContext.class)).willReturn(realmContext);
        given(realmContext.getRealm()).willReturn(realm);
        given(realm.asPath()).willReturn(realmpath);
        UriRouterContext urc = new UriRouterContext(context, "/uri", "", templateVariables);
        given(context.asContext(UriRouterContext.class)).willReturn(urc);
        given(context.asContext(SSOTokenContext.class)).willReturn(ssoTokenContext);
        given(ssoTokenContext.getCallerSSOToken()).willReturn(ssoToken);
        sessionUtilsWrapper = spy(new SessionUtilsWrapper());
        sessionResourceUtil = spy(new SessionResourceUtil(ssoTokenManager, null, null));
        amIdentity = new AMIdentity(DN.valueOf("id=demo,dc=example,dc=com"), null);

        properties.put("foo", "bar");
        properties.put("ping", "pong");
        properties.put("woo", null);

        for (String key : properties.keySet()) {
            given(ssoToken.getProperty(key)).willReturn(properties.get(key));
        }

        resource = new SessionPropertiesResource(whiteList, sessionUtilsWrapper, sessionResourceUtil);
        doReturn(amIdentity).when(sessionResourceUtil).getIdentity (ssoToken);
        doReturn(realmpath).when(sessionResourceUtil).convertDNToRealm (amIdentity.getRealm());
    }

    @Test
    public void shouldReturnSessionProperties() throws SSOException, IdRepoException {
        //given
        ReadRequest request = mock(ReadRequest.class);
        given(whiteList.getAllListedProperties(realmpath)).willReturn(properties.keySet());
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime(tokenId)).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);

        //when
        Promise<ResourceResponse, ResourceException> promise = resource.readInstance(context, request);

        //then
        assertThat(promise).succeeded().withContent().stringAt("foo").isEqualTo("bar");
        assertThat(promise).succeeded().withContent().stringAt("ping").isEqualTo("pong");
        assertThat(promise).succeeded().withContent().stringAt("woo").isEqualTo("");
    }

    @Test
    public void whenSSOExceptionShouldReturnBadRequest() throws SSOException {
        //given
        ReadRequest request = mock(ReadRequest.class);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime(tokenId)).willThrow(SSOException.class);

        //when
        Promise<ResourceResponse, ResourceException> promise = resource.readInstance(context, request);

        //then
        assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
    }


    @Test
    public void whenOperationNotSupportedShouldReturnBadRequest() {
        //given
        PatchRequest request = mock(PatchRequest.class);
        given(request.getPatchOperations()).
                willReturn(Arrays.asList(new PatchOperation[]{PatchOperation.copy("x", "y")}));

        //when
        Promise<ResourceResponse, ResourceException> promise = resource.patchInstance(context, request);

        //then
        assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
    }

    @Test
    public void whenOperationNotPermittedShouldReturnForbidden() throws SessionException {
        //given
        PatchRequest request = mock(PatchRequest.class);
        given(request.getPatchOperations()).
                willReturn(Arrays.asList(new PatchOperation[]{PatchOperation.replace("x", "y")}));
        doThrow(SessionException.class).when(sessionUtilsWrapper).checkPermissionToSetProperty(ssoToken, "x", "y");

        //when
        Promise<ResourceResponse, ResourceException> promise = resource.patchInstance(context, request);

        //then
        assertThat(promise).failedWithException().isInstanceOf(ForbiddenException.class);
    }

    @Test
    public void operationRemovePropertyShouldSetValueToEmpty() throws SSOException {

        //given
        PatchRequest request = mock(PatchRequest.class);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime(tokenId)).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(request.getPatchOperations()).willReturn(Arrays.asList(new PatchOperation[]{PatchOperation.remove("foo")}));

        //when
        Promise<ResourceResponse, ResourceException> promise = resource.patchInstance(context, request);

        //then
        verify(ssoToken).setProperty("foo", "");
    }

    @Test
    public void operationReplacePropertyShouldReplaceProperty() throws SSOException {

        //given
        PatchRequest request = mock(PatchRequest.class);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime(tokenId)).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(request.getPatchOperations())
                .willReturn(Arrays.asList(new PatchOperation[]{PatchOperation.replace("foo", "baar")}));

        //when
        Promise<ResourceResponse, ResourceException> promise = resource.patchInstance(context, request);

        //then
        verify(ssoToken).setProperty("foo", "baar");
    }

    @Test
    public void whenNullContentShouldReturnBadRequest() throws SSOException {
        //given
        UpdateRequest request = mock(UpdateRequest.class);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime(tokenId)).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);

        //when
        Promise<ResourceResponse, ResourceException> promise = resource.updateInstance(context, request);

        //then
        assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
    }

    @Test
    public void whenPropertyNotListedShouldReturnForbidden() throws SSOException, DelegationException {
        //given
        UpdateRequest request = mock(UpdateRequest.class);
        given(request.getContent()).willReturn(json(properties));
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime(tokenId)).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(whiteList.isPropertyListed(ssoToken, realmpath, properties.keySet())).willReturn(false);

        //when
        Promise<ResourceResponse, ResourceException> promise = resource.updateInstance(context, request);

        //then
        assertThat(promise).failedWithException().isInstanceOf(ForbiddenException.class);
    }

    @Test
    public void whenPropertyNotSettableShouldReturnForbidden() throws SSOException, DelegationException {
        //given
        UpdateRequest request = mock(UpdateRequest.class);
        given(request.getContent()).willReturn(json(properties));
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime(tokenId)).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(whiteList.isPropertyListed(ssoToken, realmpath, properties.keySet())).willReturn(true);
        given(whiteList.isPropertyMapSettable(ssoToken, properties)).willReturn(false);

        //when
        Promise<ResourceResponse, ResourceException> promise = resource.updateInstance(context, request);

        //then
        assertThat(promise).failedWithException().isInstanceOf(ForbiddenException.class);
    }


    @Test
    public void whenUpdatedPermittedPropertiesShouldGetUpdated() throws SSOException, DelegationException {
        //given
        UpdateRequest request = mock(UpdateRequest.class);
        properties.put("foo", "baar");
        properties.put("ping", "poong");
        properties.put("woo", "hoo");
        given(request.getContent()).willReturn(json(properties));
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime(tokenId)).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(whiteList.getAllListedProperties(realmpath)).willReturn(properties.keySet());
        given(whiteList.isPropertyMapSettable(ssoToken, properties)).willReturn(true);

        //when
        Promise<ResourceResponse, ResourceException> promise = resource.updateInstance(context, request);

        //then
        verify(ssoToken).setProperty("foo", "baar");
        verify(ssoToken).setProperty("ping", "poong");
        verify(ssoToken).setProperty("woo", "hoo");
    }
}
