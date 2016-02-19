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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.oauth2;

import static org.fest.assertions.Assertions.*;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.forgerock.openam.utils.Time.*;
import static org.mockito.Mockito.*;
import static org.forgerock.json.JsonValue.*;

import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.InsufficientScopeException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidTokenException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.mockito.ArgumentCaptor;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Status;
import org.restlet.routing.Filter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AccessTokenProtectionFilterTest {

    private static final String REQUIRED_SCOPE = "myscope";

    private AccessTokenProtectionFilter filter;
    private TokenStore tokenStore;
    private OAuth2RequestFactory<?, Request> requestFactory;

    @BeforeMethod
    public void setUp() throws Exception {
        tokenStore = mock(TokenStore.class);
        requestFactory = mock(OAuth2RequestFactory.class);
        filter = new AccessTokenProtectionFilter(REQUIRED_SCOPE, tokenStore, requestFactory, null);
    }

    @Test
    public void testBeforeHandle() throws Exception {
        //Given
        Request req = mock(Request.class);
        OAuth2Request oAuth2Request = mock(OAuth2Request.class);

        when(requestFactory.create(req)).thenReturn(oAuth2Request);
        ChallengeResponse challengeResponse = new ChallengeResponse(ChallengeScheme.HTTP_BASIC);
        challengeResponse.setRawValue("tokenId");
        when(req.getChallengeResponse()).thenReturn(challengeResponse);

        AccessToken accessToken = new AccessToken(json(object(field("id", "tokenId"),
                field("tokenName", "access_token"), field("scope", asSet("a", REQUIRED_SCOPE)),
                field("expireTime", currentTimeMillis() + 5000))));
        when(tokenStore.readAccessToken(oAuth2Request, "tokenId")).thenReturn(accessToken);

        //When
        int result = filter.beforeHandle(req, null);

        //Then
        assertThat(result).isEqualTo(Filter.CONTINUE);
    }

    @Test
    public void testBeforeHandleWithoutScope() throws Exception {
        //Given
        Request req = mock(Request.class);
        Response resp = mock(Response.class);
        OAuth2Request oAuth2Request = mock(OAuth2Request.class);

        when(requestFactory.create(req)).thenReturn(oAuth2Request);
        ChallengeResponse challengeResponse = new ChallengeResponse(ChallengeScheme.HTTP_BASIC);
        challengeResponse.setRawValue("tokenId");
        when(req.getChallengeResponse()).thenReturn(challengeResponse);

        AccessToken accessToken = new AccessToken(json(object(field("id", "tokenId"),
                field("tokenName", "access_token"), field("scope", asSet("a")),
                field("expireTime", currentTimeMillis() + 5000))));
        when(tokenStore.readAccessToken(oAuth2Request, "tokenId")).thenReturn(accessToken);

        //When
        int result = filter.beforeHandle(req, resp);

        //Then
        assertThat(result).isEqualTo(Filter.STOP);
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(resp).setStatus(statusCaptor.capture());
        Status status = statusCaptor.getValue();
        assertThat(status.getThrowable()).isInstanceOf(InsufficientScopeException.class);
    }

    @Test
    public void testBeforeHandleWithoutNeedingScope() throws Exception {
        //Given
        filter = new AccessTokenProtectionFilter(null, tokenStore, requestFactory, null);
        Request req = mock(Request.class);
        Response resp = mock(Response.class);
        OAuth2Request oAuth2Request = mock(OAuth2Request.class);

        when(requestFactory.create(req)).thenReturn(oAuth2Request);
        ChallengeResponse challengeResponse = new ChallengeResponse(ChallengeScheme.HTTP_BASIC);
        challengeResponse.setRawValue("tokenId");
        when(req.getChallengeResponse()).thenReturn(challengeResponse);

        AccessToken accessToken = new AccessToken(json(object(field("id", "tokenId"),
                field("tokenName", "access_token"), field("scope", asSet("a")),
                field("expireTime", currentTimeMillis() + 5000))));
        when(tokenStore.readAccessToken(oAuth2Request, "tokenId")).thenReturn(accessToken);

        //When
        int result = filter.beforeHandle(req, resp);

        //Then
        assertThat(result).isEqualTo(Filter.CONTINUE);
    }

    @Test
    public void testBeforeHandleWithoutToken() throws Exception {
        //Given
        Request req = mock(Request.class);
        Response resp = mock(Response.class);
        OAuth2Request oAuth2Request = mock(OAuth2Request.class);

        when(requestFactory.create(req)).thenReturn(oAuth2Request);
        ChallengeResponse challengeResponse = new ChallengeResponse(ChallengeScheme.HTTP_BASIC);
        challengeResponse.setRawValue("tokenId");
        when(req.getChallengeResponse()).thenReturn(challengeResponse);

        when(tokenStore.readAccessToken(oAuth2Request, "tokenId")).thenReturn(null);

        //When
        int result = filter.beforeHandle(req, resp);

        //Then
        assertThat(result).isEqualTo(Filter.STOP);
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(resp).setStatus(statusCaptor.capture());
        Status status = statusCaptor.getValue();
        assertThat(status.getThrowable()).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    public void testBeforeHandleWithInvalidGrant() throws Exception {
        //Given
        Request req = mock(Request.class);
        Response resp = mock(Response.class);
        OAuth2Request oAuth2Request = mock(OAuth2Request.class);

        when(requestFactory.create(req)).thenReturn(oAuth2Request);
        ChallengeResponse challengeResponse = new ChallengeResponse(ChallengeScheme.HTTP_BASIC);
        challengeResponse.setRawValue("tokenId");
        when(req.getChallengeResponse()).thenReturn(challengeResponse);

        when(tokenStore.readAccessToken(oAuth2Request, "tokenId")).thenThrow(InvalidGrantException.class);

        //When
        int result = filter.beforeHandle(req, resp);

        //Then
        assertThat(result).isEqualTo(Filter.STOP);
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(resp).setStatus(statusCaptor.capture());
        Status status = statusCaptor.getValue();
        assertThat(status.getThrowable()).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    public void testBeforeHandleWithServerException() throws Exception {
        //Given
        Request req = mock(Request.class);
        Response resp = mock(Response.class);
        OAuth2Request oAuth2Request = mock(OAuth2Request.class);

        when(requestFactory.create(req)).thenReturn(oAuth2Request);
        ChallengeResponse challengeResponse = new ChallengeResponse(ChallengeScheme.HTTP_BASIC);
        challengeResponse.setRawValue("tokenId");
        when(req.getChallengeResponse()).thenReturn(challengeResponse);

        when(tokenStore.readAccessToken(oAuth2Request, "tokenId")).thenThrow(ServerException.class);

        //When
        int result = filter.beforeHandle(req, resp);

        //Then
        assertThat(result).isEqualTo(Filter.STOP);
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(resp).setStatus(statusCaptor.capture());
        Status status = statusCaptor.getValue();
        assertThat(status.getThrowable()).isInstanceOf(ServerException.class);
    }

    @Test
    public void testBeforeHandleWithNoBearerToken() throws Exception {
        //Given
        Request req = mock(Request.class);
        Response resp = mock(Response.class);
        OAuth2Request oAuth2Request = mock(OAuth2Request.class);

        when(requestFactory.create(req)).thenReturn(oAuth2Request);
        when(req.getChallengeResponse()).thenReturn(null);

        //When
        int result = filter.beforeHandle(req, resp);

        //Then
        assertThat(result).isEqualTo(Filter.STOP);
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(resp).setStatus(statusCaptor.capture());
        Status status = statusCaptor.getValue();
        assertThat(status.getThrowable()).isInstanceOf(InvalidTokenException.class);
    }
}