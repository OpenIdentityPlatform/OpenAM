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


import static org.forgerock.json.resource.test.assertj.AssertJActionResponseAssert.assertThat;
import static org.forgerock.json.resource.test.assertj.AssertJResourceResponseAssert.assertThat;
import static org.forgerock.openam.core.rest.session.SessionResourceUtil.*;
import static org.forgerock.openam.core.rest.session.SessionResourceV2.REFRESH_ACTION_ID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.authentication.service.AuthUtilsWrapper;
import org.forgerock.openam.core.rest.session.query.SessionQueryManager;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.test.apidescriptor.ApiAnnotationAssert;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SessionResourceV2Test {


    private SSOTokenContext mockContext = mock(SSOTokenContext.class);

    private SSOToken ssoToken = mock(SSOToken.class);

    private AMIdentity amIdentity;
    private AuthUtilsWrapper authUtilsWrapper;
    private SSOTokenManager ssoTokenManager;
    private SessionResourceUtil sessionResourceUtil;

    private SessionResourceV2 sessionResource;

    @BeforeMethod
    public void setUp() throws Exception {

        SessionQueryManager sessionQueryManager = mock(SessionQueryManager.class);
        ssoTokenManager = mock(SSOTokenManager.class);
        authUtilsWrapper = mock(AuthUtilsWrapper.class);

        amIdentity = new AMIdentity(DN.valueOf("id=demo,dc=example,dc=com"), null);


        sessionResourceUtil = new SessionResourceUtil(ssoTokenManager, sessionQueryManager, null) {
            @Override
            public AMIdentity getIdentity(SSOToken ssoToken) throws IdRepoException, SSOException {
                return amIdentity;
            }

            @Override
            public String convertDNToRealm(String dn) {
                return "/example/com";
            }
        };
        sessionResource = new SessionResourceV2(ssoTokenManager, authUtilsWrapper, sessionResourceUtil);

    }


    @Test
    public void testActionCollectionIsUnsupported() {
        //given
        ActionRequest request = mock(ActionRequest.class);

        //when
        Promise<ActionResponse, ResourceException> result = sessionResource.actionCollection(mockContext, request);


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

        //When
        Promise<ActionResponse, ResourceException> promise =
                sessionResource.actionInstance(mockContext, "unknown", request);

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

        //When
        Promise<ActionResponse, ResourceException> promise =
                sessionResource.actionInstance(mockContext, "tokenId", request);

        //Then
        assertThat(promise).succeeded().withContent().longAt(IDLE_TIME).isEqualTo(0);
    }


    @Test
    public void readShouldReturnSessionInfoForValidToken() throws SSOException {
        //Given
        ReadRequest request = mock(ReadRequest.class);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("tokenId")).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(ssoToken.getMaxIdleTime()).willReturn(1000l);
        given(ssoToken.getMaxSessionTime()).willReturn(600l);
        given(ssoToken.getTimeLeft()).willReturn(575l);
        given(ssoToken.getIdleTime()).willReturn(25l);

        //When
        Promise<ResourceResponse, ResourceException> promise =
                sessionResource.readInstance(mockContext, "tokenId", request);

        //Then
        assertThat(promise).succeeded().withContent().stringAt(UID).isEqualTo("demo");
        assertThat(promise).succeeded().withContent().stringAt(REALM).isEqualTo("/example/com");
        assertThat(promise).succeeded().withContent().longAt(MAX_IDLE_TIME).isEqualTo(1000);
        assertThat(promise).succeeded().withContent().longAt(MAX_SESSION_TIME).isEqualTo(600);
        assertThat(promise).succeeded().withContent().longAt(MAX_TIME).isEqualTo(575);
        assertThat(promise).succeeded().withContent().longAt(IDLE_TIME).isEqualTo(25);
    }

    @Test
    public void readShouldReturnFalseWhenTokenUnknown() throws SSOException {
        //Given
        ReadRequest request = mock(ReadRequest.class);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("unknown")).willThrow(SSOException.class);

        //When
        Promise<ResourceResponse, ResourceException> promise =
                sessionResource.readInstance(mockContext, "unknown", request);

        //Then
        assertThat(promise).succeeded().withContent().booleanAt("valid").isFalse();
    }

    @Test
    public void shouldFailIfAnnotationsAreNotValid() {
        ApiAnnotationAssert.assertThat(SessionResourceV2.class).hasValidAnnotations();
    }
}
