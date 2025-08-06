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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.core.rest.authn.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.service.AuthException;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import org.forgerock.openam.core.rest.authn.core.wrappers.AuthContextLocalWrapper;
import org.forgerock.openam.core.rest.authn.core.wrappers.CoreServicesWrapper;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class LoginAuthenticatorTest {

    private LoginAuthenticator loginAuthenticator;

    private CoreServicesWrapper coreServicesWrapper;

    @BeforeMethod
    public void setUp() {
        coreServicesWrapper = mock(CoreServicesWrapper.class);
        loginAuthenticator = new LoginAuthenticator(coreServicesWrapper);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithNoAuthIndexType() throws AuthException, AuthLoginException,
            SSOException, RestAuthException {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String sessionId = null;
        AuthIndexType authIndexType = AuthIndexType.NONE;
        String authIndexValue = "INDEX_VALUE";
        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);

        loginConfiguration.httpRequest(request)
                .sessionId(sessionId)
                .indexType(authIndexType)
                .indexValue(authIndexValue);

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("ORG_DN");
        given(coreServicesWrapper.getAuthContext(eq(request), eq((HttpServletResponse) null), (SessionID) anyObject(),
                eq(false), eq(false))).willReturn(authContextLocalWrapper);
        given(coreServicesWrapper.isNewRequest(authContextLocalWrapper)).willReturn(true);

        //When
        LoginProcess loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

        //Then
        verify(authContextLocalWrapper).login();
        verify(coreServicesWrapper).getAuthContext(eq(request), eq((HttpServletResponse) null), (SessionID) anyObject(),
                eq(false), eq(false));
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexType() throws AuthException, AuthLoginException,
            SSOException, RestAuthException {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String sessionId = null;
        AuthIndexType authIndexType = AuthIndexType.MODULE;
        String authIndexValue = "INDEX_VALUE";
        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);

        loginConfiguration.httpRequest(request)
                .sessionId(sessionId)
                .indexType(authIndexType)
                .indexValue(authIndexValue);

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("ORG_DN");
        given(coreServicesWrapper.getAuthContext(eq(request), eq((HttpServletResponse) null), (SessionID) anyObject(),
                eq(false), eq(false))).willReturn(authContextLocalWrapper);
        given(coreServicesWrapper.isNewRequest(authContextLocalWrapper)).willReturn(true);

        //When
        LoginProcess loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

        //Then
        verify(authContextLocalWrapper).login(AuthContext.IndexType.MODULE_INSTANCE, "INDEX_VALUE");
        verify(coreServicesWrapper).getAuthContext(eq(request), eq((HttpServletResponse) null), (SessionID) anyObject(),
                eq(false), eq(false));
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithResourceAuthIndexType() throws AuthException,
            AuthLoginException, SSOException, RestAuthException {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String sessionId = null;
        AuthIndexType authIndexType = AuthIndexType.RESOURCE;
        String authIndexValue = "INDEX_VALUE";
        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);
        Map<String, Set<String>> envMap = new HashMap<String, Set<String>>();

        loginConfiguration.httpRequest(request)
                .sessionId(sessionId)
                .indexType(authIndexType)
                .indexValue(authIndexValue);

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("ORG_DN");
        given(coreServicesWrapper.getAuthContext(eq(request), eq((HttpServletResponse) null), (SessionID) anyObject(),
                eq(false), eq(false))).willReturn(authContextLocalWrapper);
        given(coreServicesWrapper.isNewRequest(authContextLocalWrapper)).willReturn(true);
        given(coreServicesWrapper.getEnvMap(request)).willReturn(envMap);

        //When
        LoginProcess loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

        //Then
        verify(authContextLocalWrapper).login(AuthContext.IndexType.RESOURCE, "INDEX_VALUE", envMap, null);
        verify(coreServicesWrapper).getAuthContext(eq(request), eq((HttpServletResponse) null), (SessionID) anyObject(),
                eq(false), eq(false));
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForSubsequentRequest() throws Exception {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String sessionId = "SESSION_ID";
        AuthIndexType authIndexType = AuthIndexType.NONE;
        String authIndexValue = "INDEX_VALUE";
        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);

        loginConfiguration.httpRequest(request)
                .sessionId(sessionId)
                .indexType(authIndexType)
                .indexValue(authIndexValue);

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("/ORG_DN");
        given(coreServicesWrapper.getAuthContext(eq(request), eq((HttpServletResponse) null), (SessionID) anyObject(),
                eq(false), eq(false))).willReturn(authContextLocalWrapper);
        given(coreServicesWrapper.isNewRequest(authContextLocalWrapper)).willReturn(false);

        //When
        LoginProcess loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

        //Then
        verifyZeroInteractions(authContextLocalWrapper);
        assertNotNull(loginProcess);
        verify(coreServicesWrapper).getAuthContext(eq(request), eq((HttpServletResponse) null), (SessionID) anyObject(),
                eq(false), eq(false));
        verify(coreServicesWrapper).isNewRequest(authContextLocalWrapper);
        verify(coreServicesWrapper).getDomainNameByRequest(request);
        verify(coreServicesWrapper).isOrganizationActive("/ORG_DN");
        verify(coreServicesWrapper).getExistingValidSSOToken(Matchers.<SessionID>anyObject());
        verifyNoMoreInteractions(coreServicesWrapper);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexTypeNoneWithSessionUpgradeButNotRequired()
            throws AuthException, AuthLoginException, SSOException, RestAuthException {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String sessionId = null;
        AuthIndexType authIndexType = AuthIndexType.NONE;
        String authIndexValue = "INDEX_VALUE";
        String ssoTokenId = "SSO_TOKEN_ID";
        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);
        SSOToken ssoToken = mock(SSOToken.class);

        loginConfiguration.httpRequest(request)
                .sessionId(sessionId)
                .indexType(authIndexType)
                .indexValue(authIndexValue)
                .sessionUpgrade(ssoTokenId);

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("ORG_DN");
        given(coreServicesWrapper.getAuthContext(eq(request), eq((HttpServletResponse) null), (SessionID) anyObject(),
                eq(false), eq(false))).willReturn(authContextLocalWrapper);
        given(coreServicesWrapper.getExistingValidSSOToken(eq(new SessionID("SSO_TOKEN_ID")))).willReturn(ssoToken);
        given(coreServicesWrapper.isNewRequest(authContextLocalWrapper)).willReturn(true);

        //When
        LoginProcess loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

        //Then
        assertThat(loginProcess.isSuccessful()).isTrue();
        verify(authContextLocalWrapper, never()).login();
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexTypeUserWithSessionUpgradeButNotRequired()
            throws AuthException, AuthLoginException, SSOException, RestAuthException {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String sessionId = null;
        AuthIndexType authIndexType = AuthIndexType.USER;
        String authIndexValue = "INDEX_VALUE";
        String ssoTokenId = "SSO_TOKEN_ID";
        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);
        SSOToken ssoToken = mock(SSOToken.class);

        loginConfiguration.httpRequest(request)
                .sessionId(sessionId)
                .indexType(authIndexType)
                .indexValue(authIndexValue)
                .sessionUpgrade(ssoTokenId);
        given(ssoToken.getProperty("UserToken")).willReturn("INDEX_VALUE");

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("ORG_DN");
        given(coreServicesWrapper.getAuthContext(eq(request), eq((HttpServletResponse) null), (SessionID) anyObject(),
                eq(false), eq(false))).willReturn(authContextLocalWrapper);
        given(coreServicesWrapper.getExistingValidSSOToken(eq(new SessionID("SSO_TOKEN_ID")))).willReturn(ssoToken);
        given(coreServicesWrapper.isNewRequest(authContextLocalWrapper)).willReturn(true);

        //When
        LoginProcess loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

        //Then
        assertThat(loginProcess.isSuccessful()).isTrue();
        verify(authContextLocalWrapper, never()).login();
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexTypeRoleWithSessionUpgradeButNotRequired()
            throws AuthException, AuthLoginException, SSOException, RestAuthException {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String sessionId = null;
        AuthIndexType authIndexType = AuthIndexType.ROLE;
        String authIndexValue = "INDEX_VALUE";
        String ssoTokenId = "SSO_TOKEN_ID";
        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);
        SSOToken ssoToken = mock(SSOToken.class);

        loginConfiguration.httpRequest(request)
                .sessionId(sessionId)
                .indexType(authIndexType)
                .indexValue(authIndexValue)
                .sessionUpgrade(ssoTokenId);
        given(ssoToken.getProperty("Role")).willReturn("INDEX_VALUE");

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("ORG_DN");
        given(coreServicesWrapper.getAuthContext(eq(request), eq((HttpServletResponse) null), (SessionID) anyObject(),
                eq(false), eq(false))).willReturn(authContextLocalWrapper);
        given(coreServicesWrapper.getExistingValidSSOToken(eq(new SessionID("SSO_TOKEN_ID")))).willReturn(ssoToken);
        given(coreServicesWrapper.isNewRequest(authContextLocalWrapper)).willReturn(true);
        given(coreServicesWrapper.doesValueContainKey(anyString(), anyString())).willReturn(false);
        given(coreServicesWrapper.doesValueContainKey("INDEX_VALUE", "INDEX_VALUE")).willReturn(true);

        //When
        LoginProcess loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

        //Then
        assertThat(loginProcess.isSuccessful()).isTrue();
        verify(authContextLocalWrapper, never()).login();
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexTypeServiceWithSessionUpgradeButNotRequired()
            throws AuthException, AuthLoginException, SSOException, RestAuthException {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String sessionId = null;
        AuthIndexType authIndexType = AuthIndexType.SERVICE;
        String authIndexValue = "INDEX_VALUE";
        String ssoTokenId = "SSO_TOKEN_ID";
        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);
        SSOToken ssoToken = mock(SSOToken.class);

        loginConfiguration.httpRequest(request)
                .sessionId(sessionId)
                .indexType(authIndexType)
                .indexValue(authIndexValue)
                .sessionUpgrade(ssoTokenId);
        given(ssoToken.getProperty("Service")).willReturn("INDEX_VALUE");

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("ORG_DN");
        given(coreServicesWrapper.getAuthContext(eq(request), eq((HttpServletResponse) null), (SessionID) anyObject(),
                eq(false), eq(false))).willReturn(authContextLocalWrapper);
        given(coreServicesWrapper.getExistingValidSSOToken(eq(new SessionID("SSO_TOKEN_ID")))).willReturn(ssoToken);
        given(coreServicesWrapper.isNewRequest(authContextLocalWrapper)).willReturn(true);
        given(coreServicesWrapper.doesValueContainKey(anyString(), anyString())).willReturn(false);
        given(coreServicesWrapper.doesValueContainKey("INDEX_VALUE", "INDEX_VALUE")).willReturn(true);

        //When
        LoginProcess loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

        //Then
        assertThat(loginProcess.isSuccessful()).isTrue();
        verify(authContextLocalWrapper, never()).login();
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexTypeModuleWithSessionUpgradeButNotRequired()
            throws AuthException, AuthLoginException, SSOException, RestAuthException {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String sessionId = null;
        AuthIndexType authIndexType = AuthIndexType.MODULE;
        String authIndexValue = "INDEX_VALUE";
        String ssoTokenId = "SSO_TOKEN_ID";
        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);
        SSOToken ssoToken = mock(SSOToken.class);

        loginConfiguration.httpRequest(request)
                .sessionId(sessionId)
                .indexType(authIndexType)
                .indexValue(authIndexValue)
                .sessionUpgrade(ssoTokenId);
        given(ssoToken.getProperty("AuthType")).willReturn("INDEX_VALUE");

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("ORG_DN");
        given(coreServicesWrapper.getAuthContext(eq(request), eq((HttpServletResponse) null), (SessionID) anyObject(),
                eq(false), eq(false))).willReturn(authContextLocalWrapper);
        given(coreServicesWrapper.getExistingValidSSOToken(eq(new SessionID("SSO_TOKEN_ID")))).willReturn(ssoToken);
        given(coreServicesWrapper.isNewRequest(authContextLocalWrapper)).willReturn(true);
        given(coreServicesWrapper.doesValueContainKey(anyString(), anyString())).willReturn(false);
        given(coreServicesWrapper.doesValueContainKey("INDEX_VALUE", "INDEX_VALUE")).willReturn(true);

        //When
        LoginProcess loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

        //Then
        assertThat(loginProcess.isSuccessful()).isTrue();
        verify(authContextLocalWrapper, never()).login();
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexTypeLevelWithSessionUpgradeButNotRequired()
            throws AuthException, AuthLoginException, SSOException, RestAuthException {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String sessionId = null;
        AuthIndexType authIndexType = AuthIndexType.LEVEL;
        String authIndexValue = "5";
        String ssoTokenId = "SSO_TOKEN_ID";
        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);
        SSOToken ssoToken = mock(SSOToken.class);

        loginConfiguration.httpRequest(request)
                .sessionId(sessionId)
                .indexType(authIndexType)
                .indexValue(authIndexValue)
                .sessionUpgrade(ssoTokenId);
        given(ssoToken.getProperty("AuthLevel")).willReturn("10");

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("ORG_DN");
        given(coreServicesWrapper.getAuthContext(eq(request), eq((HttpServletResponse) null), (SessionID) anyObject(),
                eq(false), eq(false))).willReturn(authContextLocalWrapper);
        given(coreServicesWrapper.getExistingValidSSOToken(eq(new SessionID("SSO_TOKEN_ID")))).willReturn(ssoToken);
        given(coreServicesWrapper.isNewRequest(authContextLocalWrapper)).willReturn(true);
        given(coreServicesWrapper.doesValueContainKey(anyString(), anyString())).willReturn(false);
        given(coreServicesWrapper.doesValueContainKey("INDEX_VALUE", "INDEX_VALUE")).willReturn(true);

        //When
        LoginProcess loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

        //Then
        assertThat(loginProcess.isSuccessful()).isTrue();
        verify(authContextLocalWrapper, never()).login();
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexTypeUserWithSessionUpgrade()
            throws AuthException, AuthLoginException, SSOException, RestAuthException {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String sessionId = null;
        AuthIndexType authIndexType = AuthIndexType.USER;
        String authIndexValue = "INDEX_VALUE_NEW";
        String ssoTokenId = "SSO_TOKEN_ID";
        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);
        SSOToken ssoToken = mock(SSOToken.class);

        loginConfiguration.httpRequest(request)
                .sessionId(sessionId)
                .indexType(authIndexType)
                .indexValue(authIndexValue)
                .sessionUpgrade(ssoTokenId);
        given(ssoToken.getProperty("UserToken")).willReturn("INDEX_VALUE");

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("ORG_DN");
        given(coreServicesWrapper.getAuthContext(eq(request), eq((HttpServletResponse) null), (SessionID) anyObject(),
                eq(true), eq(false))).willReturn(authContextLocalWrapper);
        given(coreServicesWrapper.getExistingValidSSOToken(eq(new SessionID("SSO_TOKEN_ID")))).willReturn(ssoToken);
        given(coreServicesWrapper.isNewRequest(authContextLocalWrapper)).willReturn(true);
        given(authContextLocalWrapper.isSessionUpgrade()).willReturn(true);

        //When
        LoginProcess loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

        //Then
        verify(authContextLocalWrapper).login(AuthContext.IndexType.USER, "INDEX_VALUE_NEW");
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexTypeRoleWithSessionUpgrade()
            throws AuthException, AuthLoginException, SSOException, RestAuthException {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String sessionId = null;
        AuthIndexType authIndexType = AuthIndexType.ROLE;
        String authIndexValue = "INDEX_VALUE_NEW";
        String ssoTokenId = "SSO_TOKEN_ID";
        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);
        SSOToken ssoToken = mock(SSOToken.class);

        loginConfiguration.httpRequest(request)
                .sessionId(sessionId)
                .indexType(authIndexType)
                .indexValue(authIndexValue)
                .sessionUpgrade(ssoTokenId);
        given(ssoToken.getProperty("Role")).willReturn("INDEX_VALUE");

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("ORG_DN");
        given(coreServicesWrapper.getAuthContext(eq(request), eq((HttpServletResponse) null), (SessionID) anyObject(),
                eq(true), eq(false))).willReturn(authContextLocalWrapper);
        given(coreServicesWrapper.getExistingValidSSOToken(eq(new SessionID("SSO_TOKEN_ID")))).willReturn(ssoToken);
        given(coreServicesWrapper.isNewRequest(authContextLocalWrapper)).willReturn(true);
        given(coreServicesWrapper.doesValueContainKey(anyString(), anyString())).willReturn(false);
        given(coreServicesWrapper.doesValueContainKey("INDEX_VALUE", "INDEX_VALUE")).willReturn(true);
        given(authContextLocalWrapper.isSessionUpgrade()).willReturn(true);

        //When
        LoginProcess loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

        //Then
        verify(authContextLocalWrapper).login(AuthContext.IndexType.ROLE, "INDEX_VALUE_NEW");
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexTypeServiceWithSessionUpgrade()
            throws AuthException, AuthLoginException, SSOException, RestAuthException {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String sessionId = null;
        AuthIndexType authIndexType = AuthIndexType.SERVICE;
        String authIndexValue = "INDEX_VALUE_NEW";
        String ssoTokenId = "SSO_TOKEN_ID";
        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);
        SSOToken ssoToken = mock(SSOToken.class);

        loginConfiguration.httpRequest(request)
                .sessionId(sessionId)
                .indexType(authIndexType)
                .indexValue(authIndexValue)
                .sessionUpgrade(ssoTokenId);
        given(ssoToken.getProperty("Service")).willReturn("INDEX_VALUE");

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("ORG_DN");
        given(coreServicesWrapper.getAuthContext(eq(request), eq((HttpServletResponse) null), (SessionID) anyObject(),
                eq(true), eq(false))).willReturn(authContextLocalWrapper);
        given(coreServicesWrapper.getExistingValidSSOToken(eq(new SessionID("SSO_TOKEN_ID")))).willReturn(ssoToken);
        given(coreServicesWrapper.isNewRequest(authContextLocalWrapper)).willReturn(true);
        given(coreServicesWrapper.doesValueContainKey(anyString(), anyString())).willReturn(false);
        given(coreServicesWrapper.doesValueContainKey("INDEX_VALUE", "INDEX_VALUE")).willReturn(true);
        given(authContextLocalWrapper.isSessionUpgrade()).willReturn(true);

        //When
        LoginProcess loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

        //Then
        verify(authContextLocalWrapper).login(AuthContext.IndexType.SERVICE, "INDEX_VALUE_NEW");
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexTypeModuleWithSessionUpgrade()
            throws AuthException, AuthLoginException, SSOException, RestAuthException {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String sessionId = null;
        AuthIndexType authIndexType = AuthIndexType.MODULE;
        String authIndexValue = "INDEX_VALUE_NEW";
        String ssoTokenId = "SSO_TOKEN_ID";
        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);
        SSOToken ssoToken = mock(SSOToken.class);

        loginConfiguration.httpRequest(request)
                .sessionId(sessionId)
                .indexType(authIndexType)
                .indexValue(authIndexValue)
                .sessionUpgrade(ssoTokenId);
        given(ssoToken.getProperty("AuthType")).willReturn("INDEX_VALUE");

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("ORG_DN");
        given(coreServicesWrapper.getAuthContext(eq(request), eq((HttpServletResponse) null), (SessionID) anyObject(),
                eq(true), eq(false))).willReturn(authContextLocalWrapper);
        given(coreServicesWrapper.getExistingValidSSOToken(eq(new SessionID("SSO_TOKEN_ID")))).willReturn(ssoToken);
        given(coreServicesWrapper.isNewRequest(authContextLocalWrapper)).willReturn(true);
        given(coreServicesWrapper.doesValueContainKey(anyString(), anyString())).willReturn(false);
        given(coreServicesWrapper.doesValueContainKey("INDEX_VALUE", "INDEX_VALUE")).willReturn(true);
        given(authContextLocalWrapper.isSessionUpgrade()).willReturn(true);

        //When
        LoginProcess loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

        //Then
        verify(authContextLocalWrapper).login(AuthContext.IndexType.MODULE_INSTANCE, "INDEX_VALUE_NEW");
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexTypeLevelWithSessionUpgrade()
            throws AuthException, AuthLoginException, SSOException, RestAuthException {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String sessionId = null;
        AuthIndexType authIndexType = AuthIndexType.LEVEL;
        String authIndexValue = "15";
        String ssoTokenId = "SSO_TOKEN_ID";
        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);
        SSOToken ssoToken = mock(SSOToken.class);

        loginConfiguration.httpRequest(request)
                .sessionId(sessionId)
                .indexType(authIndexType)
                .indexValue(authIndexValue)
                .sessionUpgrade(ssoTokenId);
        given(ssoToken.getProperty("AuthLevel")).willReturn("10");

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("ORG_DN");
        given(coreServicesWrapper.getAuthContext(eq(request), eq((HttpServletResponse) null), (SessionID) anyObject(),
                eq(true), eq(false))).willReturn(authContextLocalWrapper);
        given(coreServicesWrapper.getExistingValidSSOToken(eq(new SessionID("SSO_TOKEN_ID")))).willReturn(ssoToken);
        given(coreServicesWrapper.isNewRequest(authContextLocalWrapper)).willReturn(true);
        given(coreServicesWrapper.doesValueContainKey(anyString(), anyString())).willReturn(false);
        given(coreServicesWrapper.doesValueContainKey("INDEX_VALUE", "INDEX_VALUE")).willReturn(true);
        given(authContextLocalWrapper.isSessionUpgrade()).willReturn(true);

        //When
        LoginProcess loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

        //Then
        verify(authContextLocalWrapper).login(AuthContext.IndexType.LEVEL, "15");
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexTypeCompositeWithSessionUpgrade()
            throws AuthException, AuthLoginException, SSOException, RestAuthException {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String sessionId = null;
        AuthIndexType authIndexType = AuthIndexType.COMPOSITE;
        String authIndexValue = "INDEX_VALUE";
        String ssoTokenId = "SSO_TOKEN_ID";
        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);
        SSOToken ssoToken = mock(SSOToken.class);

        loginConfiguration.httpRequest(request)
                .sessionId(sessionId)
                .indexType(authIndexType)
                .indexValue(authIndexValue)
                .sessionUpgrade(ssoTokenId);

        given(coreServicesWrapper.getDomainNameByRequest(Matchers.<HttpServletRequest>anyObject()))
                .willReturn("ORG_DN");
        given(coreServicesWrapper.getAuthContext((HttpServletRequest) anyObject(), eq((HttpServletResponse) null),
                (SessionID) anyObject(), eq(true), eq(false))).willReturn(authContextLocalWrapper);
        given(authContextLocalWrapper.isSessionUpgrade()).willReturn(true);
        given(coreServicesWrapper.getExistingValidSSOToken(eq(new SessionID("SSO_TOKEN_ID")))).willReturn(ssoToken);
        given(coreServicesWrapper.isNewRequest(authContextLocalWrapper)).willReturn(true);

        //When
        LoginProcess loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

        //Then
        verify(authContextLocalWrapper).login(AuthContext.IndexType.COMPOSITE_ADVICE, "INDEX_VALUE");
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForSubsequentRequestWithAuthIndexTypeCompositeAndSessionUpgradeSet()
            throws AuthException, AuthLoginException, SSOException, RestAuthException {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String sessionId = "SESSION_ID";
        AuthIndexType authIndexType = AuthIndexType.COMPOSITE;
        String authIndexValue = "INDEX_VALUE";
        String ssoTokenId = "SSO_TOKEN_ID";
        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);
        SSOToken ssoToken = mock(SSOToken.class);

        loginConfiguration.httpRequest(request)
                .sessionId(sessionId)
                .indexType(authIndexType)
                .indexValue(authIndexValue)
                .sessionUpgrade(ssoTokenId);

        given(coreServicesWrapper.getDomainNameByRequest(Matchers.<HttpServletRequest>anyObject()))
                .willReturn("ORG_DN");
        given(coreServicesWrapper.getAuthContext((HttpServletRequest) anyObject(), eq((HttpServletResponse) null),
                (SessionID) anyObject(), eq(false), eq(false))).willReturn(authContextLocalWrapper);
        given(coreServicesWrapper.getExistingValidSSOToken(eq(new SessionID("SSO_TOKEN_ID")))).willReturn(ssoToken);
        given(coreServicesWrapper.isNewRequest(authContextLocalWrapper)).willReturn(true);
        given(authContextLocalWrapper.isSessionUpgrade()).willReturn(true);

        //When
        LoginProcess loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

        //Then
        verify(authContextLocalWrapper).login(AuthContext.IndexType.COMPOSITE_ADVICE, "INDEX_VALUE");
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldThrow400ExceptionWithOrgDNNotValidReturningEmptyString() throws SSOException, AuthException, AuthLoginException, IOException {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String sessionId = "SESSION_ID";
        AuthIndexType authIndexType = AuthIndexType.COMPOSITE;
        String authIndexValue = "INDEX_VALUE";
        String ssoTokenId = "SSO_TOKEN_ID";

        loginConfiguration.httpRequest(request)
                .sessionId(sessionId)
                .indexType(authIndexType)
                .indexValue(authIndexValue)
                .sessionUpgrade(ssoTokenId);

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("");

        //When
        boolean exceptionCaught = false;
        RestAuthException exception = null;
        try {
            loginAuthenticator.getLoginProcess(loginConfiguration);
        } catch (RestAuthException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        assertTrue(exceptionCaught);
        assertEquals(exception.getStatusCode(), 400);
    }

    @Test
    public void shouldThrow400ExceptionWithOrgDNNotValid() throws SSOException, AuthException, AuthLoginException, IOException {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String sessionId = "SESSION_ID";
        AuthIndexType authIndexType = AuthIndexType.COMPOSITE;
        String authIndexValue = "INDEX_VALUE";
        String ssoTokenId = "SSO_TOKEN_ID";

        loginConfiguration.httpRequest(request)
                .sessionId(sessionId)
                .indexType(authIndexType)
                .indexValue(authIndexValue)
                .sessionUpgrade(ssoTokenId);

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("");

        //When
        boolean exceptionCaught = false;
        RestAuthException exception = null;
        try {
            loginAuthenticator.getLoginProcess(loginConfiguration);
        } catch (RestAuthException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        assertTrue(exceptionCaught);
        assertEquals(exception.getStatusCode(), 400);
    }

    @Test
    public void shouldThrow400ExceptionWithOrgDNNotValidReturningNull() throws SSOException,
            AuthException, AuthLoginException, IOException {

        //Given
        LoginConfiguration loginConfiguration = new LoginConfiguration();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String sessionId = "SESSION_ID";
        AuthIndexType authIndexType = AuthIndexType.COMPOSITE;
        String authIndexValue = "INDEX_VALUE";
        String ssoTokenId = "SSO_TOKEN_ID";

        loginConfiguration.httpRequest(request)
                .sessionId(sessionId)
                .indexType(authIndexType)
                .indexValue(authIndexValue)
                .sessionUpgrade(ssoTokenId);

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn(null);

        //When
        boolean exceptionCaught = false;
        RestAuthException exception = null;
        try {
            loginAuthenticator.getLoginProcess(loginConfiguration);
        } catch (RestAuthException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        assertTrue(exceptionCaught);
        assertEquals(exception.getStatusCode(), 400);
    }

    @Test(dataProvider = "resourceBasedAuthenticationParams")
    public void shouldUseAuthIndexValueOrGotoUrlForResourceBasedAuth(
            String authIndexValue, String resourceUrlParam, String expectedResource) throws AuthLoginException {
        // Given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        LoginConfiguration config = new LoginConfiguration().indexType(AuthIndexType.RESOURCE)
                                                            .indexValue(authIndexValue)
                                                            .httpRequest(mockRequest);

        Map<String, Set<String>> envMap = Collections.singletonMap("a", Collections.singleton("b"));
        given(coreServicesWrapper.getEnvMap(mockRequest)).willReturn(envMap);
        given(coreServicesWrapper.getResourceURL(mockRequest)).willReturn(resourceUrlParam);

        LoginProcess mockProcess = mock(LoginProcess.class);
        AuthenticationContext mockAuthContext = mock(AuthenticationContext.class);
        given(mockProcess.getLoginConfiguration()).willReturn(config);
        given(mockProcess.getAuthContext()).willReturn(mockAuthContext);

        // When
        loginAuthenticator.startLoginProcess(mockProcess);

        // Then
        verify(mockAuthContext).login(AuthContext.IndexType.RESOURCE, expectedResource, envMap, null);
    }

    @DataProvider
    public Object[][] resourceBasedAuthenticationParams() {
        final String authIndex = "http://authIndex.com";
        final String resourceParam = "http://resourceURL.com";
        // AuthIndexValue, ResourceURL/goto param, expected resource used
        return new Object[][]{
                {authIndex, resourceParam, authIndex},
                {authIndex, null, authIndex},
                {"true", resourceParam, resourceParam},
                {"true", null, null},
                {null, resourceParam, resourceParam},
                {null, null, null},
                {"", resourceParam, resourceParam}
        };
    }

    @Test(dataProvider = "realmQualifiedModules")
    public void shouldRecogniseRealmQualifiedModuleNamesForSessionUpgrade(String desiredModule,
                                                                          String existingModules,
                                                                          boolean upgradeExpected) throws Exception {
        // Given
        SSOToken mockSSOToken = mock(SSOToken.class);
        given(mockSSOToken.getProperty(ISAuthConstants.AUTH_TYPE)).willReturn(existingModules);


        // When
        boolean result = loginAuthenticator.checkSessionUpgrade(mockSSOToken, AuthIndexType.MODULE, desiredModule);

        // Then
        assertThat(result).as("With desiredModule=%s and existingModules=%s, expected checkSessionUpgrade to be %s",
                desiredModule, existingModules, upgradeExpected).isEqualTo(upgradeExpected);
    }

    @Test(dataProvider = "realmQualifiedModules") // Re-use module names as service names
    public void shouldRecogniseRealmQualifiedServiceNamesForSessionUpgrade(String desiredService,
                                                                           String existingServices,
                                                                           boolean upgradeExpected) throws Exception {
        // Given
        SSOToken mockSSOToken = mock(SSOToken.class);
        given(mockSSOToken.getProperty(ISAuthConstants.SERVICE)).willReturn(existingServices);


        // When
        boolean result = loginAuthenticator.checkSessionUpgrade(mockSSOToken, AuthIndexType.SERVICE, desiredService);

        // Then
        assertThat(result).as("With desiredService=%s and existingServices=%s, expected checkSessionUpgrade to be %s",
                desiredService, existingServices, upgradeExpected).isEqualTo(upgradeExpected);

    }

    @Test(dataProvider = "realmQualifiedModules") // and as roles too, why not?
    public void shouldRecogniseRealmQualifiedRoleNamesForSessionUpgrade(String desiredRole,
                                                                        String existingRoles,
                                                                        boolean upgradeExpected) throws Exception {
        // Given
        SSOToken mockSSOToken = mock(SSOToken.class);
        given(mockSSOToken.getProperty(ISAuthConstants.ROLE)).willReturn(existingRoles);


        // When
        boolean result = loginAuthenticator.checkSessionUpgrade(mockSSOToken, AuthIndexType.ROLE, desiredRole);

        // Then
        assertThat(result).as("With desiredRole=%s and existingRoles=%s, expected checkSessionUpgrade to be %s",
                desiredRole, existingRoles, upgradeExpected).isEqualTo(upgradeExpected);
    }

    @DataProvider
    public Object[][] realmQualifiedModules() {
        return new Object[][] {
                { "LDAP", "/test:LDAP|DataStore", false },
                { "LDAP", "DataStore|/test:LDAP", false },
                { "LDAP", "LDAP|DataStore", false },
                { "LDAP", "DataStore|LDAP", false },
                { "LDAP", "LDAP", false },
                { "LDAP", "/test:LDAP", false },
                { "LDAP", "DataStore", true },
                { "LDAP", "LDAP|/test:DataStore", false },
                { "LDAP", "/test:DataStore", true },
                { "LDAP", "", true },
                { "LDAP", null, true }
        };
    }
}
