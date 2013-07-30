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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.forgerockrest.authn.core;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.service.AuthException;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.IdRepoException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.forgerockrest.authn.core.wrappers.AuthContextLocalWrapper;
import org.forgerock.openam.forgerockrest.authn.core.wrappers.CoreServicesWrapper;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

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
            SSOException {

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
        verifyNoMoreInteractions(authContextLocalWrapper);
        verify(coreServicesWrapper).getAuthContext(eq(request), eq((HttpServletResponse) null), (SessionID) anyObject(),
                eq(false), eq(false));
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexType() throws AuthException, AuthLoginException,
            SSOException {

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
        verifyNoMoreInteractions(authContextLocalWrapper);
        verify(coreServicesWrapper).getAuthContext(eq(request), eq((HttpServletResponse) null), (SessionID) anyObject(),
                eq(false), eq(false));
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithResourceAuthIndexType() throws AuthException,
            AuthLoginException, SSOException {

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
        verify(authContextLocalWrapper).login(AuthContext.IndexType.RESOURCE, "INDEX_VALUE", false, envMap, null);
        verifyNoMoreInteractions(authContextLocalWrapper);
        verify(coreServicesWrapper).getAuthContext(eq(request), eq((HttpServletResponse) null), (SessionID) anyObject(),
                eq(false), eq(false));
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForSubsequentRequest() throws AuthException, AuthLoginException, SSOException,
            IdRepoException {

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

        given(coreServicesWrapper.getDomainNameByRequest(request)).willReturn("ORG_DN");
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
        verify(coreServicesWrapper).isOrganizationActive("ORG_DN");
        verifyNoMoreInteractions(coreServicesWrapper);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexTypeNoneWithSessionUpgradeButNotRequired()
            throws AuthException, AuthLoginException, SSOException {

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
        verify(authContextLocalWrapper).login();
        verifyNoMoreInteractions(authContextLocalWrapper);
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexTypeUserWithSessionUpgradeButNotRequired()
            throws AuthException, AuthLoginException, SSOException {

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
        verify(authContextLocalWrapper).login(AuthContext.IndexType.USER, "INDEX_VALUE");
        verifyNoMoreInteractions(authContextLocalWrapper);
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexTypeRoleWithSessionUpgradeButNotRequired()
            throws AuthException, AuthLoginException, SSOException {

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
        verify(authContextLocalWrapper).login(AuthContext.IndexType.ROLE, "INDEX_VALUE");
        verifyNoMoreInteractions(authContextLocalWrapper);
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexTypeServiceWithSessionUpgradeButNotRequired()
            throws AuthException, AuthLoginException, SSOException {

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
        verify(authContextLocalWrapper).login(AuthContext.IndexType.SERVICE, "INDEX_VALUE");
        verifyNoMoreInteractions(authContextLocalWrapper);
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexTypeModuleWithSessionUpgradeButNotRequired()
            throws AuthException, AuthLoginException, SSOException {

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
        verify(authContextLocalWrapper).login(AuthContext.IndexType.MODULE_INSTANCE, "INDEX_VALUE");
        verifyNoMoreInteractions(authContextLocalWrapper);
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexTypeLevelWithSessionUpgradeButNotRequired()
            throws AuthException, AuthLoginException, SSOException {

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
        verify(authContextLocalWrapper).login(AuthContext.IndexType.LEVEL, "5");
        verifyNoMoreInteractions(authContextLocalWrapper);
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexTypeUserWithSessionUpgrade()
            throws AuthException, AuthLoginException, SSOException {

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

        //When
        LoginProcess loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

        //Then
        verify(authContextLocalWrapper).login(AuthContext.IndexType.USER, "INDEX_VALUE_NEW");
        verifyNoMoreInteractions(authContextLocalWrapper);
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexTypeRoleWithSessionUpgrade()
            throws AuthException, AuthLoginException, SSOException {

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

        //When
        LoginProcess loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

        //Then
        verify(authContextLocalWrapper).login(AuthContext.IndexType.ROLE, "INDEX_VALUE_NEW");
        verifyNoMoreInteractions(authContextLocalWrapper);
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexTypeServiceWithSessionUpgrade()
            throws AuthException, AuthLoginException, SSOException {

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

        //When
        LoginProcess loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

        //Then
        verify(authContextLocalWrapper).login(AuthContext.IndexType.SERVICE, "INDEX_VALUE_NEW");
        verifyNoMoreInteractions(authContextLocalWrapper);
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexTypeModuleWithSessionUpgrade()
            throws AuthException, AuthLoginException, SSOException {

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

        //When
        LoginProcess loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

        //Then
        verify(authContextLocalWrapper).login(AuthContext.IndexType.MODULE_INSTANCE, "INDEX_VALUE_NEW");
        verifyNoMoreInteractions(authContextLocalWrapper);
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexTypeLevelWithSessionUpgrade()
            throws AuthException, AuthLoginException, SSOException {

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

        //When
        LoginProcess loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

        //Then
        verify(authContextLocalWrapper).login(AuthContext.IndexType.LEVEL, "15");
        verifyNoMoreInteractions(authContextLocalWrapper);
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForInitialRequestWithAuthIndexTypeCompositeWithSessionUpgrade()
            throws AuthException, AuthLoginException, SSOException {

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
        given(coreServicesWrapper.getExistingValidSSOToken(eq(new SessionID("SSO_TOKEN_ID")))).willReturn(ssoToken);
        given(coreServicesWrapper.isNewRequest(authContextLocalWrapper)).willReturn(true);

        //When
        LoginProcess loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

        //Then
        verify(authContextLocalWrapper).login(AuthContext.IndexType.COMPOSITE_ADVICE, "INDEX_VALUE");
        verifyNoMoreInteractions(authContextLocalWrapper);
        assertNotNull(loginProcess);
    }

    @Test
    public void shouldGetLoginProcessForSubsequentRequestWithAuthIndexTypeCompositeAndSessionUpgradeSet()
            throws AuthException, AuthLoginException, SSOException {

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

        //When
        LoginProcess loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

        //Then
        verify(authContextLocalWrapper).login(AuthContext.IndexType.COMPOSITE_ADVICE, "INDEX_VALUE");
        verifyNoMoreInteractions(authContextLocalWrapper);
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
        Response response = exception.getResponse();
        assertEquals(response.getStatus(), 400);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonValue jsonValue = new JsonValue(objectMapper.readValue((String) response.getEntity(), Map.class));
        assertEquals(jsonValue.size(), 1);
        assertTrue(jsonValue.isDefined("errorMessage"));
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
        Response response = exception.getResponse();
        assertEquals(response.getStatus(), 400);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonValue jsonValue = new JsonValue(objectMapper.readValue((String) response.getEntity(), Map.class));
        assertEquals(jsonValue.size(), 1);
        assertTrue(jsonValue.isDefined("errorMessage"));
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
        Response response = exception.getResponse();
        assertEquals(response.getStatus(), 400);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonValue jsonValue = new JsonValue(objectMapper.readValue((String) response.getEntity(), Map.class));
        assertEquals(jsonValue.size(), 1);
        assertTrue(jsonValue.isDefined("errorMessage"));
    }
}
