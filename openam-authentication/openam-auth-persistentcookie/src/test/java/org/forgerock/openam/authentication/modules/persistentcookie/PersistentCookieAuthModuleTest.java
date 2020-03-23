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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.persistentcookie;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.AssertJUnit.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import javax.security.auth.message.MessageInfo;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.caf.authentication.framework.AuthenticationFramework;
import org.forgerock.jaspi.modules.session.jwt.JwtSessionModule;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.authentication.modules.common.AMLoginModuleBinder;
import org.forgerock.openam.core.CoreWrapper;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;

public class PersistentCookieAuthModuleTest {

    private PersistentCookieAuthModule persistentCookieAuthModule;

    private AMLoginModuleBinder amLoginModuleBinder;
    private CoreWrapper coreWrapper;
    private PersistentCookieModuleWrapper persistentCookieWrapper;
    private final Map<String, Object> GENERATED_CONFIG = mock(Map.class);

    @BeforeMethod
    public void setUp() throws Exception {

        amLoginModuleBinder = mock(AMLoginModuleBinder.class);
        coreWrapper = mock(CoreWrapper.class);

        persistentCookieWrapper = mock(PersistentCookieModuleWrapper.class);
        given(persistentCookieWrapper.generateConfig(anyString(), anyString(), anyBoolean(), anyString(), anyBoolean(),
                anyBoolean(), anyString(), anySetOf(String.class), anyString()))
                .willReturn(GENERATED_CONFIG);

        persistentCookieAuthModule = new PersistentCookieAuthModule(coreWrapper, persistentCookieWrapper);
        persistentCookieAuthModule.setAMLoginModule(amLoginModuleBinder);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        given(amLoginModuleBinder.getHttpServletRequest()).willReturn(request);
        given(amLoginModuleBinder.getHttpServletResponse()).willReturn(response);
        given(amLoginModuleBinder.getRequestOrg()).willReturn("REQUEST_ORG");
    }

    @Test
    public void shouldInitialiseAuthModuleWithIdleTimeoutSetAsNull() throws Exception {

        //Given
        Subject subject = new Subject();
        Map sharedState = new HashMap();
        Map options = new HashMap();

        options.put("openam-auth-persistent-cookie-idle-time", new HashSet<Object>());
        Set<Object> maxLifeSet = new HashSet<Object>();
        maxLifeSet.add("5");
        options.put("openam-auth-persistent-cookie-max-life", maxLifeSet);

        //When
        Map<String, Object> config = persistentCookieAuthModule.generateConfig(subject, sharedState, options);

        //Then
        assertSame(GENERATED_CONFIG, config);
        verify(persistentCookieWrapper).generateConfig(eq("0"), eq("300"), anyBoolean(), anyString(),
                anyBoolean(), anyBoolean(), anyString(), anySetOf(String.class), anyString());
    }

    @Test
    public void shouldInitialiseAuthModuleWithMaxLifeSetAsNull() throws Exception {

        //Given
        Subject subject = new Subject();
        Map sharedState = new HashMap();
        Map options = new HashMap();

        Set<Object> idleTimeoutSet = new HashSet<Object>();
        idleTimeoutSet.add("1");
        options.put("openam-auth-persistent-cookie-idle-time", idleTimeoutSet);
        options.put("openam-auth-persistent-cookie-max-life", new HashSet<>());

        //When
        Map<String, Object> config = persistentCookieAuthModule.generateConfig(subject, sharedState, options);

        //Then
        assertSame(GENERATED_CONFIG, config);
        verify(persistentCookieWrapper).generateConfig(eq("60"), eq("0"), anyBoolean(), anyString(),
                anyBoolean(), anyBoolean(), anyString(), anySetOf(String.class), anyString());
    }

    @Test
    public void shouldInitialiseAuthModule() throws Exception {

        //Given
        Subject subject = new Subject();
        Map sharedState = new HashMap();
        Map options = new HashMap();

        Set<Object> idleTimeoutSet = new HashSet<Object>();
        idleTimeoutSet.add("1");
        options.put("openam-auth-persistent-cookie-idle-time", idleTimeoutSet);
        Set<Object> maxLifeSet = new HashSet<Object>();
        maxLifeSet.add("5");
        options.put("openam-auth-persistent-cookie-max-life", maxLifeSet);

        //When
        Map<String, Object> config = persistentCookieAuthModule.generateConfig(subject, sharedState, options);

        //Then
        assertSame(GENERATED_CONFIG, config);
        verify(persistentCookieWrapper).generateConfig(eq("60"), eq("300"), anyBoolean(), anyString(),
                anyBoolean(), anyBoolean(), anyString(), anySetOf(String.class), anyString());
    }


    @Test
    public void shouldInitialiseAuthModuleWithClientIPEnforced() throws Exception {

        //Given
        Subject subject = new Subject();
        Map sharedState = new HashMap();
        Map options = new HashMap();

        options.put("openam-auth-persistent-cookie-enforce-ip", Collections.singleton("true"));

        //When
        Map<String, Object> config = persistentCookieAuthModule.generateConfig(subject, sharedState, options);

        //Then
        assertSame(GENERATED_CONFIG, config);
        verify(persistentCookieWrapper).generateConfig(anyString(), anyString(), eq(true), anyString(),
                anyBoolean(), anyBoolean(), anyString(), anySetOf(String.class), anyString());
    }

    @Test
    public void shouldProcessCallbacksAndThrowInvalidStateException() throws Exception {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = 0;

        //When
        boolean exceptionCaught = false;
        AuthLoginException exception = null;
        try {
            persistentCookieAuthModule.process(callbacks, state);
        } catch (AuthLoginException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        assertTrue(exceptionCaught);
        assertEquals(exception.getErrorCode(), "incorrectState");
    }

    @Test
    public void shouldProcessCallbacksWhenJwtNotPresentOrValid() throws Exception {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = ISAuthConstants.LOGIN_START;

        given(persistentCookieWrapper.validateJwtSessionCookie(Matchers.<MessageInfo>anyObject())).willReturn(null);
        shouldInitialiseAuthModule();

        //When
        boolean exceptionCaught = false;
        AuthLoginException exception = null;
        try {
            persistentCookieAuthModule.process(callbacks, state);
        } catch (AuthLoginException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        verify(amLoginModuleBinder).setUserSessionProperty(JwtSessionModule.TOKEN_IDLE_TIME_IN_MINUTES_CLAIM_KEY, "60");
        verify(amLoginModuleBinder).setUserSessionProperty(JwtSessionModule.MAX_TOKEN_LIFE_IN_MINUTES_KEY, "300");
        verify(persistentCookieWrapper).validateJwtSessionCookie(Matchers.<MessageInfo>anyObject());
        assertTrue(exceptionCaught);
        assertEquals(exception.getErrorCode(), "cookieNotValid");
    }

    @Test
    public void shouldProcessCallbacksWhenJASPIContextNotFound() throws Exception {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = ISAuthConstants.LOGIN_START;
        Jwt jwt = mock(Jwt.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);

        given(persistentCookieWrapper.validateJwtSessionCookie(Matchers.<MessageInfo>anyObject())).willReturn(jwt);

        given(jwt.getClaimsSet()).willReturn(claimsSet);
        given(claimsSet.getClaim("org.forgerock.authentication.context", Map.class)).willReturn(null);
        shouldInitialiseAuthModule();

        //When
        boolean exceptionCaught = false;
        AuthLoginException exception = null;
        try {
            persistentCookieAuthModule.process(callbacks, state);
        } catch (AuthLoginException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        verify(amLoginModuleBinder).setUserSessionProperty(JwtSessionModule.TOKEN_IDLE_TIME_IN_MINUTES_CLAIM_KEY, "60");
        verify(amLoginModuleBinder).setUserSessionProperty(JwtSessionModule.MAX_TOKEN_LIFE_IN_MINUTES_KEY, "300");
        verify(persistentCookieWrapper).validateJwtSessionCookie(Matchers.<MessageInfo>anyObject());
        assertTrue(exceptionCaught);
        assertEquals(exception.getErrorCode(), "jaspiContextNotFound");
    }

    @Test
    public void shouldProcessCallbacksWhenJwtRealmIsDifferent() throws Exception {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = ISAuthConstants.LOGIN_START;
        Jwt jwt = mock(Jwt.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);
        Map<String, Object> internalMap = mock(HashMap.class);

        given(persistentCookieWrapper.validateJwtSessionCookie(Matchers.<MessageInfo>anyObject())).willReturn(jwt);

        given(jwt.getClaimsSet()).willReturn(claimsSet);
        given(claimsSet.getClaim("org.forgerock.authentication.context", Map.class)).willReturn(internalMap);
        given(internalMap.get("openam.rlm")).willReturn("REALM");
        given(amLoginModuleBinder.getRequestOrg()).willReturn("OTHER_REALM");
        shouldInitialiseAuthModule();

        //When
        boolean exceptionCaught = false;
        AuthLoginException exception = null;
        try {
            persistentCookieAuthModule.process(callbacks, state);
        } catch (AuthLoginException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        verify(amLoginModuleBinder).setUserSessionProperty(JwtSessionModule.TOKEN_IDLE_TIME_IN_MINUTES_CLAIM_KEY, "60");
        verify(amLoginModuleBinder).setUserSessionProperty(JwtSessionModule.MAX_TOKEN_LIFE_IN_MINUTES_KEY, "300");
        verify(persistentCookieWrapper).validateJwtSessionCookie(Matchers.<MessageInfo>anyObject());
        assertTrue(exceptionCaught);
        assertEquals(exception.getErrorCode(), "authFailedDiffRealm");
    }

    @Test
    public void shouldProcessCallbacksWhenJwtValid() throws Exception {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = ISAuthConstants.LOGIN_START;
        Jwt jwt = mock(Jwt.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);

        Map<String, Object> internalMap = mock(HashMap.class);

        given(persistentCookieWrapper.validateJwtSessionCookie(Matchers.<MessageInfo>anyObject())).willReturn(jwt);

        given(jwt.getClaimsSet()).willReturn(claimsSet);
        given(claimsSet.getClaim("org.forgerock.authentication.context", Map.class)).willReturn(internalMap);
        given(amLoginModuleBinder.getRequestOrg()).willReturn("REALM");
        given(internalMap.get("openam.rlm")).willReturn("REALM");
        given(internalMap.get("openam.usr")).willReturn("USER");
        shouldInitialiseAuthModule();

        //When
        int returnedState = persistentCookieAuthModule.process(callbacks, state);

        //Then
        verify(amLoginModuleBinder).setUserSessionProperty(JwtSessionModule.TOKEN_IDLE_TIME_IN_MINUTES_CLAIM_KEY, "60");
        verify(amLoginModuleBinder).setUserSessionProperty(JwtSessionModule.MAX_TOKEN_LIFE_IN_MINUTES_KEY, "300");
        verify(persistentCookieWrapper).validateJwtSessionCookie(Matchers.<MessageInfo>anyObject());
        verify(amLoginModuleBinder).setUserSessionProperty("jwtValidated", "true");
        assertEquals(returnedState, ISAuthConstants.LOGIN_SUCCEED);
    }

    @Test
    public void shouldSetUsernameInSharedState() throws Exception {
        shouldProcessCallbacksWhenJwtValid();
        verify(amLoginModuleBinder).setAuthenticatingUserName("USER");
    }

    @Test
    public void shouldEnforceClientIPOnLoginWhenClientIPIsSame() throws LoginException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Callback[] callbacks = new Callback[0];
        Jwt jwt = mock(Jwt.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);
        Map<String, Object> claimsSetContext = new HashMap<String, Object>();
        HttpServletRequest request = mock(HttpServletRequest.class);

        Map options = new HashMap();
        options.put("openam-auth-persistent-cookie-enforce-ip", Collections.singleton("true"));
        persistentCookieAuthModule.generateConfig(null, null, options);

        given(persistentCookieWrapper.validateJwtSessionCookie(messageInfo)).willReturn(jwt);
        given(jwt.getClaimsSet()).willReturn(claimsSet);
        given(claimsSet.getClaim(AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT, Map.class)).willReturn(claimsSetContext);
        claimsSetContext.put("openam.rlm", "REALM");
        given(amLoginModuleBinder.getRequestOrg()).willReturn("REALM");
        claimsSetContext.put("openam.clientip", "CLIENT_IP");
        given(amLoginModuleBinder.getHttpServletRequest()).willReturn(request);
        given(request.getRemoteAddr()).willReturn("CLIENT_IP");

        //When
        boolean result = persistentCookieAuthModule.process(messageInfo, clientSubject, callbacks);

        //Then
        assertTrue(result);
    }

    @Test(expectedExceptions = AuthLoginException.class)
    public void shouldEnforceClientIPOnLoginWhenClientIPIsDifferent() throws LoginException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Callback[] callbacks = new Callback[0];
        Jwt jwt = mock(Jwt.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);
        Map<String, Object> claimsSetContext = new HashMap<String, Object>();
        HttpServletRequest request = mock(HttpServletRequest.class);

        Map options = new HashMap();
        options.put("openam-auth-persistent-cookie-enforce-ip", Collections.singleton("true"));
        persistentCookieAuthModule.generateConfig(null, null, options);

        given(persistentCookieWrapper.validateJwtSessionCookie(messageInfo)).willReturn(jwt);
        given(jwt.getClaimsSet()).willReturn(claimsSet);
        given(claimsSet.getClaim(AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT, Map.class)).willReturn(claimsSetContext);
        claimsSetContext.put("openam.rlm", "REALM");
        given(amLoginModuleBinder.getRequestOrg()).willReturn("REALM");
        claimsSetContext.put("openam-auth-persistent-cookie-enforce-ip", "CLIENT_IP");
        given(amLoginModuleBinder.getHttpServletRequest()).willReturn(request);
        given(request.getRemoteAddr()).willReturn("CLIENT_IP_2");

        //When
        persistentCookieAuthModule.process(messageInfo, clientSubject, callbacks);

        //Then
        fail();
    }

    @Test(expectedExceptions = AuthLoginException.class)
    public void shouldEnforceClientIPOnLoginWhenClientIPIsNotStoredInPCookie() throws LoginException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Callback[] callbacks = new Callback[0];
        Jwt jwt = mock(Jwt.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);
        Map<String, Object> claimsSetContext = new HashMap<String, Object>();
        HttpServletRequest request = mock(HttpServletRequest.class);

        Map options = new HashMap();
        options.put("openam-auth-persistent-cookie-enforce-ip", Collections.singleton("true"));
        persistentCookieAuthModule.generateConfig(null, null, options);

        given(persistentCookieWrapper.validateJwtSessionCookie(messageInfo)).willReturn(jwt);
        given(jwt.getClaimsSet()).willReturn(claimsSet);
        given(claimsSet.getClaim(AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT, Map.class)).willReturn(claimsSetContext);
        claimsSetContext.put("openam.rlm", "REALM");
        given(amLoginModuleBinder.getRequestOrg()).willReturn("REALM");
        given(amLoginModuleBinder.getHttpServletRequest()).willReturn(request);
        given(request.getRemoteAddr()).willReturn("CLIENT_IP");

        //When
        persistentCookieAuthModule.process(messageInfo, clientSubject, callbacks);

        //Then
        fail();
    }

    @Test(expectedExceptions = AuthLoginException.class)
    public void shouldEnforceClientIPOnLoginWhenClientIPIsNotOnRequest() throws LoginException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Callback[] callbacks = new Callback[0];
        Jwt jwt = mock(Jwt.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);
        Map<String, Object> claimsSetContext = new HashMap<String, Object>();
        HttpServletRequest request = mock(HttpServletRequest.class);

        Map options = new HashMap();
        options.put("openam-auth-persistent-cookie-enforce-ip", Collections.singleton("true"));
        persistentCookieAuthModule.generateConfig(null, null, options);

        given(persistentCookieWrapper.validateJwtSessionCookie(messageInfo)).willReturn(jwt);
        given(jwt.getClaimsSet()).willReturn(claimsSet);
        given(claimsSet.getClaim(AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT, Map.class)).willReturn(claimsSetContext);
        claimsSetContext.put("openam.rlm", "REALM");
        given(amLoginModuleBinder.getRequestOrg()).willReturn("REALM");
        claimsSetContext.put("openam-auth-persistent-cookie-enforce-ip", "CLIENT_IP");
        given(amLoginModuleBinder.getHttpServletRequest()).willReturn(request);

        //When
        persistentCookieAuthModule.process(messageInfo, clientSubject, callbacks);

        //Then
        fail();
    }
}
