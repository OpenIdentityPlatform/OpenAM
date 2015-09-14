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
 */

package org.forgerock.openam.authentication.modules.persistentcookie;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import javax.security.auth.message.MessageInfo;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.sm.SMSException;
import org.forgerock.caf.authentication.framework.AuthenticationFramework;
import org.forgerock.caf.http.Cookie;
import org.forgerock.jaspi.modules.session.jwt.JwtSessionModule;
import org.forgerock.jaspi.modules.session.jwt.ServletJwtSessionModule;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.jose.utils.KeystoreManager;
import org.forgerock.openam.authentication.modules.common.AMLoginModuleBinder;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.utils.AMKeyProvider;
import org.mockito.Matchers;
import org.mockito.MockSettings;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PersistentCookieAuthModuleTest {

    private PersistentCookieAuthModule persistentCookieAuthModule;

    private ServletJwtSessionModule jwtSessionModule;
    private AMKeyProvider amKeyProvider;
    private AMLoginModuleBinder amLoginModuleBinder;
    private CoreWrapper coreWrapper;

    @BeforeMethod
    public void setUp() {

        jwtSessionModule = mock(ServletJwtSessionModule.class);
        amKeyProvider = mock(AMKeyProvider.class);
        amLoginModuleBinder = mock(AMLoginModuleBinder.class);
        coreWrapper = mock(CoreWrapper.class);

        persistentCookieAuthModule = new PersistentCookieAuthModule(jwtSessionModule, amKeyProvider, coreWrapper) {
            @Override
            protected String getKeyAlias(String orgName) throws SSOException, SMSException {
                return "KEY_ALIAS";
            }
        };
        persistentCookieAuthModule.setAMLoginModule(amLoginModuleBinder);

        given(amKeyProvider.getPrivateKeyPass()).willReturn("PRIVATE_KEY_PASS");
        given(amKeyProvider.getKeystoreType()).willReturn("KEYSTORE_TYPE");
        given(amKeyProvider.getKeystoreFilePath()).willReturn("KEYSTORE_FILE_PATH");
        given(amKeyProvider.getKeystorePass()).willReturn("KEYSTORE_PASS".toCharArray());

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        given(amLoginModuleBinder.getHttpServletRequest()).willReturn(request);
        given(amLoginModuleBinder.getHttpServletResponse()).willReturn(response);
        given(amLoginModuleBinder.getRequestOrg()).willReturn("REQUEST_ORG");
    }

    @Test
    public void shouldInitialiseAuthModuleWithIdleTimeoutSetAsNull() {

        //Given
        Subject subject = new Subject();
        Map sharedState = new HashMap();
        Map options = new HashMap();

        options.put("openam-auth-persistent-cookie-idle-time", new HashSet<Object>());
        Set<Object> maxLifeSet = new HashSet<Object>();
        maxLifeSet.add("5");
        options.put("openam-auth-persistent-cookie-max-life", maxLifeSet);

        //When
        Map<String, Object> config = persistentCookieAuthModule.initialize(subject, sharedState, options);

        //Then
        assertEquals(config.get(JwtSessionModule.KEY_ALIAS_KEY), "KEY_ALIAS");
        assertEquals(config.get(JwtSessionModule.PRIVATE_KEY_PASSWORD_KEY), "PRIVATE_KEY_PASS");
        assertEquals(config.get(JwtSessionModule.KEYSTORE_TYPE_KEY), "KEYSTORE_TYPE");
        assertEquals(config.get(JwtSessionModule.KEYSTORE_FILE_KEY), "KEYSTORE_FILE_PATH");
        assertEquals(config.get(JwtSessionModule.KEYSTORE_PASSWORD_KEY), "KEYSTORE_PASS");
        assertEquals(config.get(JwtSessionModule.TOKEN_IDLE_TIME_IN_MINUTES_CLAIM_KEY), "0");
        assertEquals(config.get(JwtSessionModule.MAX_TOKEN_LIFE_IN_MINUTES_KEY), "300");
    }

    @Test
    public void shouldInitialiseAuthModuleWithMaxLifeSetAsNull() {

        //Given
        Subject subject = new Subject();
        Map sharedState = new HashMap();
        Map options = new HashMap();

        Set<Object> idleTimeoutSet = new HashSet<Object>();
        idleTimeoutSet.add("1");
        options.put("openam-auth-persistent-cookie-idle-time", idleTimeoutSet);
        options.put("openam-auth-persistent-cookie-max-life", new HashSet<Object>());

        //When
        Map<String, Object> config = persistentCookieAuthModule.initialize(subject, sharedState, options);

        //Then
        assertEquals(config.get(JwtSessionModule.KEY_ALIAS_KEY), "KEY_ALIAS");
        assertEquals(config.get(JwtSessionModule.PRIVATE_KEY_PASSWORD_KEY), "PRIVATE_KEY_PASS");
        assertEquals(config.get(JwtSessionModule.KEYSTORE_TYPE_KEY), "KEYSTORE_TYPE");
        assertEquals(config.get(JwtSessionModule.KEYSTORE_FILE_KEY), "KEYSTORE_FILE_PATH");
        assertEquals(config.get(JwtSessionModule.KEYSTORE_PASSWORD_KEY), "KEYSTORE_PASS");
        assertEquals(config.get(JwtSessionModule.TOKEN_IDLE_TIME_IN_MINUTES_CLAIM_KEY), "60");
        assertEquals(config.get(JwtSessionModule.MAX_TOKEN_LIFE_IN_MINUTES_KEY), "0");
    }

    @Test
    public void shouldInitialiseAuthModule() {

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
        Map<String, Object> config = persistentCookieAuthModule.initialize(subject, sharedState, options);

        //Then
        assertEquals(config.get(JwtSessionModule.KEY_ALIAS_KEY), "KEY_ALIAS");
        assertEquals(config.get(JwtSessionModule.PRIVATE_KEY_PASSWORD_KEY), "PRIVATE_KEY_PASS");
        assertEquals(config.get(JwtSessionModule.KEYSTORE_TYPE_KEY), "KEYSTORE_TYPE");
        assertEquals(config.get(JwtSessionModule.KEYSTORE_FILE_KEY), "KEYSTORE_FILE_PATH");
        assertEquals(config.get(JwtSessionModule.KEYSTORE_PASSWORD_KEY), "KEYSTORE_PASS");
        assertEquals(config.get(JwtSessionModule.TOKEN_IDLE_TIME_IN_MINUTES_CLAIM_KEY), "60");
        assertEquals(config.get(JwtSessionModule.MAX_TOKEN_LIFE_IN_MINUTES_KEY), "300");
    }

    @Test
    public void shouldProcessCallbacksAndThrowInvalidStateException() throws LoginException {

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
    public void shouldProcessCallbacksWhenJwtNotPresentOrValid() throws LoginException {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = ISAuthConstants.LOGIN_START;

        given(jwtSessionModule.validateJwtSessionCookie(Matchers.<MessageInfo>anyObject())).willReturn(null);
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
        verify(jwtSessionModule).validateJwtSessionCookie(Matchers.<MessageInfo>anyObject());
        assertTrue(exceptionCaught);
        assertEquals(exception.getErrorCode(), "cookieNotValid");
    }

    @Test
    public void shouldProcessCallbacksWhenJASPIContextNotFound() throws LoginException {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = ISAuthConstants.LOGIN_START;
        Jwt jwt = mock(Jwt.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);

        given(jwtSessionModule.validateJwtSessionCookie(Matchers.<MessageInfo>anyObject())).willReturn(jwt);

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
        verify(jwtSessionModule).validateJwtSessionCookie(Matchers.<MessageInfo>anyObject());
        assertTrue(exceptionCaught);
        assertEquals(exception.getErrorCode(), "jaspiContextNotFound");
    }


    @Test
    public void shouldProcessCallbacksWhenJwtRealmIsDifferent() throws LoginException {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = ISAuthConstants.LOGIN_START;
        Jwt jwt = mock(Jwt.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);
        Map<String, Object> internalMap = mock(HashMap.class);

        given(jwtSessionModule.validateJwtSessionCookie(Matchers.<MessageInfo>anyObject())).willReturn(jwt);

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
        verify(jwtSessionModule).validateJwtSessionCookie(Matchers.<MessageInfo>anyObject());
        assertTrue(exceptionCaught);
        assertEquals(exception.getErrorCode(), "authFailedDiffRealm");
    }

    @Test
    public void shouldProcessCallbacksWhenJwtValid() throws LoginException {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = ISAuthConstants.LOGIN_START;
        Jwt jwt = mock(Jwt.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);

        Map<String, Object> internalMap = mock(HashMap.class);

        given(jwtSessionModule.validateJwtSessionCookie(Matchers.<MessageInfo>anyObject())).willReturn(jwt);

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
        verify(jwtSessionModule).validateJwtSessionCookie(Matchers.<MessageInfo>anyObject());
        verify(amLoginModuleBinder).setUserSessionProperty("jwtValidated", "true");
        assertEquals(returnedState, ISAuthConstants.LOGIN_SUCCEED);
    }

    @Test
    public void shouldInitialisePostAuthProcess() throws SSOException, AuthenticationException {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        given(ssoToken.getProperty(JwtSessionModule.TOKEN_IDLE_TIME_IN_MINUTES_CLAIM_KEY)).willReturn("TOKEN_IDLE_TIME");
        given(ssoToken.getProperty(JwtSessionModule.MAX_TOKEN_LIFE_IN_MINUTES_KEY)).willReturn("TOKEN_MAX_LIFE");
        given(ssoToken.getProperty("openam-auth-persistent-cookie-domains")).willReturn("");

        //When
        Map<String, Object> config = persistentCookieAuthModule.initialize(requestParamsMap, request, response,
                ssoToken);

        //Then
        assertEquals(config.get(JwtSessionModule.KEY_ALIAS_KEY), "KEY_ALIAS");
        assertEquals(config.get(JwtSessionModule.PRIVATE_KEY_PASSWORD_KEY), "PRIVATE_KEY_PASS");
        assertEquals(config.get(JwtSessionModule.KEYSTORE_TYPE_KEY), "KEYSTORE_TYPE");
        assertEquals(config.get(JwtSessionModule.KEYSTORE_FILE_KEY), "KEYSTORE_FILE_PATH");
        assertEquals(config.get(JwtSessionModule.KEYSTORE_PASSWORD_KEY), "KEYSTORE_PASS");
        assertEquals(config.get(JwtSessionModule.TOKEN_IDLE_TIME_IN_MINUTES_CLAIM_KEY), "TOKEN_IDLE_TIME");
        assertEquals(config.get(JwtSessionModule.MAX_TOKEN_LIFE_IN_MINUTES_KEY), "TOKEN_MAX_LIFE");
    }

    @Test
    public void shouldCallOnLoginSuccessWhenJwtNotValidated() throws AuthenticationException, SSOException {

        //Given
        persistentCookieAuthModule = new PersistentCookieAuthModule(new ServletJwtSessionModule(), amKeyProvider, coreWrapper) {
            @Override
            protected String getKeyAlias(String orgName) throws SSOException, SMSException {
                return "KEY_ALIAS";
            }
        };

        MessageInfo messageInfo = mock(MessageInfo.class);
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        Map<String, Object> map = new HashMap<String, Object>();
        given(messageInfo.getMap()).willReturn(map);

        Principal principal = mock(Principal.class);
        given(principal.getName()).willReturn("PRINCIPAL_NAME");

        SSOTokenID ssoTokenId = mock(SSOTokenID.class);
        given(ssoTokenId.toString()).willReturn("SSO_TOKEN_ID");

        given(ssoToken.getPrincipal()).willReturn(principal);
        given(ssoToken.getAuthType()).willReturn("AUTH_TYPE");
        given(ssoToken.getTokenID()).willReturn(ssoTokenId);
        given(ssoToken.getProperty("Organization")).willReturn("ORGANISATION");

        //When
        persistentCookieAuthModule.onLoginSuccess(messageInfo, requestParamsMap, request, response, ssoToken);

        //Then
        assertEquals(map.size(), 1);
        Map<String, Object> contextMap = (Map<String, Object>) map.get("org.forgerock.authentication.context");
        assertEquals(contextMap.get("openam.usr"), "PRINCIPAL_NAME");
        assertEquals(contextMap.get("openam.aty"), "AUTH_TYPE");
        assertEquals(contextMap.get("openam.sid"), "SSO_TOKEN_ID");
        assertEquals(contextMap.get("openam.rlm"), "ORGANISATION");
        assertEquals(contextMap.get("openam.clientip"), null);
    }

    @Test
    public void shouldCallOnLoginSuccess() throws AuthenticationException, SSOException {

        //Given
        persistentCookieAuthModule = new PersistentCookieAuthModule(new ServletJwtSessionModule(), amKeyProvider, coreWrapper) {
            @Override
            protected String getKeyAlias(String orgName) throws SSOException, SMSException {
                return "KEY_ALIAS";
            }
        };

        MessageInfo messageInfo = mock(MessageInfo.class);
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        Map<String, Object> map = new HashMap<String, Object>();
        given(messageInfo.getMap()).willReturn(map);

        Principal principal = mock(Principal.class);
        given(principal.getName()).willReturn("PRINCIPAL_NAME");

        SSOTokenID ssoTokenId = mock(SSOTokenID.class);
        given(ssoTokenId.toString()).willReturn("SSO_TOKEN_ID");

        given(ssoToken.getPrincipal()).willReturn(principal);
        given(ssoToken.getAuthType()).willReturn("AUTH_TYPE");
        given(ssoToken.getTokenID()).willReturn(ssoTokenId);
        given(ssoToken.getProperty("Organization")).willReturn("ORGANISATION");
        given(ssoToken.getProperty("jwtValidated")).willReturn("true");

        //When
        persistentCookieAuthModule.onLoginSuccess(messageInfo, requestParamsMap, request, response, ssoToken);

        //Then
        assertEquals(map.size(), 2);
        assertEquals(map.get("jwtValidated"), true);
        Map<String, Object> contextMap = (Map<String, Object>) map.get("org.forgerock.authentication.context");
        assertEquals(contextMap.get("openam.usr"), "PRINCIPAL_NAME");
        assertEquals(contextMap.get("openam.aty"), "AUTH_TYPE");
        assertEquals(contextMap.get("openam.sid"), "SSO_TOKEN_ID");
        assertEquals(contextMap.get("openam.rlm"), "ORGANISATION");
        assertEquals(contextMap.get("openam.clientip"), null);
    }

    @Test
    public void shouldInitialiseAuthModuleWithClientIPEnforced() {

        //Given
        Subject subject = new Subject();
        Map sharedState = new HashMap();
        Map options = new HashMap();

        options.put("openam-auth-persistent-cookie-enforce-ip", Collections.singleton("true"));

        //When
        Map<String, Object> config = persistentCookieAuthModule.initialize(subject, sharedState, options);

        //Then
        assertEquals(config.get("openam-auth-persistent-cookie-enforce-ip"), true);
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
        persistentCookieAuthModule.initialize(null, null, options);

        given(jwtSessionModule.validateJwtSessionCookie(messageInfo)).willReturn(jwt);
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

    @Test (expectedExceptions = AuthLoginException.class)
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
        persistentCookieAuthModule.initialize(null, null, options);

        given(jwtSessionModule.validateJwtSessionCookie(messageInfo)).willReturn(jwt);
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

    @Test (expectedExceptions = AuthLoginException.class)
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
        persistentCookieAuthModule.initialize(null, null, options);

        given(jwtSessionModule.validateJwtSessionCookie(messageInfo)).willReturn(jwt);
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

    @Test (expectedExceptions = AuthLoginException.class)
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
        persistentCookieAuthModule.initialize(null, null, options);

        given(jwtSessionModule.validateJwtSessionCookie(messageInfo)).willReturn(jwt);
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

    @Test
    public void shouldInitialiseAuthModuleWithClientIPEnforcedForPAP() throws SSOException, AuthenticationException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        given(ssoToken.getProperty("openam-auth-persistent-cookie-enforce-ip")).willReturn("true");
        given(ssoToken.getProperty("openam-auth-persistent-cookie-domains")).willReturn("");

        //When
        Map<String, Object> config = persistentCookieAuthModule.initialize(Collections.emptyMap(), request, response,
                ssoToken);

        //Then
        assertEquals(config.get("openam-auth-persistent-cookie-enforce-ip"), true);
    }

    @Test
    public void shouldStoreClientIPOnLoginSuccess() throws AuthenticationException, SSOException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);
        Map<String, Object> messageInfoMap = new HashMap<String, Object>();
        Map<String, Object> contextMap = new HashMap<String, Object>();
        Principal principal = mock(Principal.class);
        SSOTokenID ssoTokenID = mock(SSOTokenID.class);

        given(messageInfo.getMap()).willReturn(messageInfoMap);
        messageInfoMap.put(AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT, contextMap);
        given(ssoToken.getPrincipal()).willReturn(principal);
        given(ssoToken.getTokenID()).willReturn(ssoTokenID);
        given(request.getRemoteAddr()).willReturn("CLIENT_IP");

        //When
        persistentCookieAuthModule.onLoginSuccess(messageInfo, Collections.emptyMap(), request, response, ssoToken);

        //Then
        assertEquals(contextMap.get("openam.clientip"), "CLIENT_IP");
    }
}
