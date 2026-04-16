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
import static org.forgerock.json.resource.test.assertj.AssertJActionResponseAssert.assertThat;
import static org.forgerock.json.resource.test.assertj.AssertJResourceResponseAssert.assertThat;
import static org.forgerock.openam.core.rest.session.SessionResourceUtil.*;
import static org.forgerock.openam.core.rest.session.SessionResourceV2.REFRESH_ACTION_ID;
import static org.forgerock.openam.session.SessionConstants.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.authentication.service.AuthUtilsWrapper;
import org.forgerock.openam.core.rest.session.query.SessionQueryManager;
import org.forgerock.openam.dpro.session.PartialSession.Builder;
import org.forgerock.openam.dpro.session.PartialSessionFactory;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.session.SessionPropertyWhitelist;
import org.forgerock.openam.test.apidescriptor.ApiAnnotationAssert;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SessionResourceV2Test {

    private static final String REALM_PATH = "/example/com";

    private SSOTokenContext mockContext = mock(SSOTokenContext.class);

    private SSOToken ssoToken = mock(SSOToken.class);

    private AMIdentity amIdentity;
    private AuthUtilsWrapper authUtilsWrapper;
    private SSOTokenManager ssoTokenManager;
    private SessionResourceUtil sessionResourceUtil;
    private SessionPropertyWhitelist sessionPropertyWhitelist;
    private SessionService sessionService;
    private PartialSessionFactory partialSessionFactory;

    private SessionResourceV2 sessionResource;

    @BeforeMethod
    public void setUp() throws Exception {

        SessionQueryManager sessionQueryManager = mock(SessionQueryManager.class);
        ssoTokenManager = mock(SSOTokenManager.class);
        authUtilsWrapper = mock(AuthUtilsWrapper.class);
        sessionPropertyWhitelist = mock(SessionPropertyWhitelist.class);

        amIdentity = new AMIdentity(DN.valueOf("id=demo,dc=example,dc=com"), null);

        sessionService = mock(SessionService.class);
        partialSessionFactory = mock(PartialSessionFactory.class);

        sessionResourceUtil = new SessionResourceUtil(ssoTokenManager, sessionQueryManager, null) {
            @Override
            public AMIdentity getIdentity(SSOToken ssoToken) throws IdRepoException, SSOException {
                return amIdentity;
            }

            @Override
            public String convertDNToRealm(String dn) {
                return REALM_PATH;
            }
        };
        sessionResource = new SessionResourceV2(ssoTokenManager, authUtilsWrapper,
                sessionResourceUtil, sessionPropertyWhitelist, sessionService, partialSessionFactory);
        given(mockContext.getCallerSSOToken()).willReturn(ssoToken);
    }

    @Test
    public void testActionInstanceIsUnsupported() {
        //given
        ActionRequest request = mock(ActionRequest.class);

        //when
        Promise<ActionResponse, ResourceException> result = sessionResource.actionInstance(mockContext, "resource",
                request);

        //then
        assertThat(result).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testCreateInstanceIsUnsupported() {
        //given
        CreateRequest request = mock(CreateRequest.class);

        //when
        Promise<ResourceResponse, ResourceException> result = sessionResource.createInstance(mockContext, request);


        //then
        assertThat(result).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testDeleteInstanceIsUnsupported() {
        //given
        DeleteRequest request = mock(DeleteRequest.class);

        //when
        Promise<ResourceResponse, ResourceException> result =
                sessionResource.deleteInstance(mockContext, "resId", request);

        //then
        assertThat(result).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testPatchInstanceIsUnsupported() {
        //given
        PatchRequest request = mock(PatchRequest.class);

        //when
        Promise<ResourceResponse, ResourceException> result =
                sessionResource.patchInstance(mockContext, "resId", request);

        //then
        assertThat(result).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testUpdateInstanceIsUnsupported() {
        //given
        UpdateRequest request = mock(UpdateRequest.class);

        //when
        Promise<ResourceResponse, ResourceException> result =
                sessionResource.updateInstance(mockContext, "resId", request);

        //then
        assertThat(result).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void actionInstanceShouldReturnFalseWhenTokenUnknown() throws SSOException {
        //Given
        ActionRequest request = mock(ActionRequest.class);
        given(ssoTokenManager.createSSOToken("unknown")).willThrow(SSOException.class);
        given(request.getAction()).willReturn(REFRESH_ACTION_ID);
        given(request.getAdditionalParameter("tokenId")).willReturn("unknown");


        //When
        Promise<ActionResponse, ResourceException> promise =
                sessionResource.actionCollection(mockContext, request);

        //Then
        assertThat(promise).succeeded().withContent().booleanAt("valid").isFalse();
    }

    @Test
    public void refreshActionShouldReturnIdleTimeToZero() throws SSOException {
        //Given
        ActionRequest request = mock(ActionRequest.class);
        given(ssoTokenManager.createSSOToken("tokenId")).willReturn(ssoToken);
        given(ssoToken.getIdleTime()).willReturn(0L);
        given(request.getAction()).willReturn(REFRESH_ACTION_ID);
        given(request.getAdditionalParameter("tokenId")).willReturn("tokenId");

        //When
        Promise<ActionResponse, ResourceException> promise =
                sessionResource.actionCollection(mockContext, request);

        //Then
        assertThat(promise).succeeded().withContent().longAt(IDLE_TIME).isEqualTo(0);
    }

    @Test
    public void readShouldReturnSessionInfoForValidToken() throws SSOException {
        //Given
        ActionRequest request = mock(ActionRequest.class);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("tokenId")).willReturn(ssoToken);
        given(request.getAction()).willReturn("getSessionInfo");
        given(request.getAdditionalParameter("tokenId")).willReturn("tokenId");
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(partialSessionFactory.fromSSOToken(eq(ssoToken))).willReturn(
                new Builder()
                        .username("demo")
                        .universalId("universalId")
                        .realm(REALM_PATH)
                        .sessionHandle("shandle:badger")
                        .latestAccessTime("JUST_NOW")
                        .maxIdleExpirationTime("CLOSE")
                        .maxSessionExpirationTime("FAR")
                        .build());

        //When
        Promise<ActionResponse, ResourceException> promise =
                sessionResource.actionCollection(mockContext, request);

        //Then
        assertThat(promise).succeeded().withContent().stringAt(JSON_SESSION_USERNAME).isEqualTo("demo");
        assertThat(promise).succeeded().withContent().stringAt(JSON_SESSION_UNIVERSAL_ID).isEqualTo("universalId");
        assertThat(promise).succeeded().withContent().stringAt(JSON_SESSION_REALM).isEqualTo(REALM_PATH);
        assertThat(promise).succeeded().withContent().stringAt(JSON_SESSION_HANDLE).isEqualTo("shandle:badger");
        assertThat(promise).succeeded().withContent().stringAt(JSON_SESSION_LATEST_ACCESS_TIME).isEqualTo("JUST_NOW");
        assertThat(promise).succeeded().withContent().stringAt(JSON_SESSION_MAX_IDLE_EXPIRATION_TIME)
                .isEqualTo("CLOSE");
        assertThat(promise).succeeded().withContent().stringAt(JSON_SESSION_MAX_SESSION_EXPIRATION_TIME)
                .isEqualTo("FAR");
    }

    @Test
    public void getSessionPropertiesActionShouldReturnSessionProperties() throws Exception {
        //Given
        ActionRequest request = mock(ActionRequest.class);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("tokenId")).willReturn(ssoToken);
        given(request.getAction()).willReturn("getSessionProperties");
        given(request.getAdditionalParameter("tokenId")).willReturn("tokenId");
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        setUpSessionProperties();

        //When
        Promise<ActionResponse, ResourceException> promise =
                sessionResource.actionCollection(mockContext, request);

        //Then
        assertThat(promise).succeeded().withContent().stringAt("foo").isEqualTo("bar");
        assertThat(promise).succeeded().withContent().stringAt("ping").isEqualTo("pong");
        assertThat(promise).succeeded().withContent().stringAt("woo").isEqualTo("");
    }

    @Test
    public void whenNullContentShouldReturnBadRequest() throws Exception {
        //given
        ActionRequest request = mock(ActionRequest.class);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("tokenId")).willReturn(ssoToken);
        given(request.getAction()).willReturn("updateSessionProperties");
        given(request.getAdditionalParameter("tokenId")).willReturn("tokenId");
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        setUpSessionProperties();

        //when
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionCollection(mockContext, request);

        //then
        assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
    }

    @Test
    public void whenPropertyNotListedShouldReturnForbidden() throws Exception {
        //given
        ActionRequest request = mock(ActionRequest.class);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("tokenId")).willReturn(ssoToken);
        given(request.getAction()).willReturn("updateSessionProperties");
        given(request.getAdditionalParameter("tokenId")).willReturn("tokenId");
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        Map<String, String> properties = setUpSessionProperties();
        given(request.getContent()).willReturn(json(properties));
        given(sessionPropertyWhitelist.isPropertyListed(ssoToken, REALM_PATH, properties.keySet())).willReturn(false);

        //when
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionCollection(mockContext, request);

        //then
        assertThat(promise).failedWithException().isInstanceOf(ForbiddenException.class);
    }

    @Test
    public void whenPropertyNotSettableShouldReturnForbidden() throws Exception {
        //given
        ActionRequest request = mock(ActionRequest.class);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("tokenId")).willReturn(ssoToken);
        given(request.getAction()).willReturn("updateSessionProperties");
        given(request.getAdditionalParameter("tokenId")).willReturn("tokenId");
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        Map<String, String> properties = setUpSessionProperties();
        given(request.getContent()).willReturn(json(properties));
        given(sessionPropertyWhitelist.isPropertyListed(ssoToken, REALM_PATH, properties.keySet())).willReturn(true);
        given(sessionPropertyWhitelist.isPropertyMapSettable(ssoToken, properties)).willReturn(false);

        //when
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionCollection(mockContext, request);

        //then
        assertThat(promise).failedWithException().isInstanceOf(ForbiddenException.class);
    }

    @Test
    public void whenUpdatedPermittedPropertiesShouldGetUpdated() throws Exception {
        //given
        ActionRequest request = mock(ActionRequest.class);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("tokenId")).willReturn(ssoToken);
        given(request.getAction()).willReturn("updateSessionProperties");
        given(request.getAdditionalParameter("tokenId")).willReturn("tokenId");
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        Map<String, String> properties = setUpSessionProperties();
        Map<String, String> updatedProperties = new HashMap<>();
        updatedProperties.put("foo", "baar");
        updatedProperties.put("ping", "poong");
        updatedProperties.put("woo", "hoo");
        given(request.getContent()).willReturn(json(updatedProperties));
        given(sessionPropertyWhitelist.isPropertyMapSettable(ssoToken, updatedProperties)).willReturn(true);

        //when
        sessionResource.actionCollection(mockContext, request);

        //then
        verify(ssoToken).setProperty("foo", "baar");
        verify(ssoToken).setProperty("ping", "poong");
        verify(ssoToken).setProperty("woo", "hoo");
    }

    @Test
    public void readShouldReturnFalseWhenTokenUnknown() throws SSOException {
        //Given
        ActionRequest request = mock(ActionRequest.class);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("unknown")).willThrow(SSOException.class);
        given(request.getAction()).willReturn("getSessionInfo");
        given(request.getAdditionalParameter("tokenId")).willReturn("unknown");

        //When
        Promise<ActionResponse, ResourceException> promise =
                sessionResource.actionCollection(mockContext, request);

        //Then
        assertThat(promise).succeeded().withContent().booleanAt("valid").isFalse();
    }

    @Test
    public void shouldFailIfAnnotationsAreNotValid() {
        ApiAnnotationAssert.assertThat(SessionResourceV2.class).hasValidAnnotations();
    }

    private Map<String, String> setUpSessionProperties() throws SSOException {
        Map<String, String> properties = new HashMap<>();
        properties.put("foo", "bar");
        properties.put("ping", "pong");
        properties.put("woo", null);
        for (String key : properties.keySet()) {
            given(ssoToken.getProperty(key)).willReturn(properties.get(key));
        }
        given(sessionPropertyWhitelist.getAllListedProperties(REALM_PATH)).willReturn(properties.keySet());
        return properties;
    }
}
