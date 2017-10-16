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

package org.forgerock.openam.authentication.modules.persistentcookie;

import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.*;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.security.auth.message.MessageInfo;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.caf.authentication.framework.AuthenticationFramework;
import com.google.common.collect.Sets;
import org.forgerock.jaspi.modules.session.jwt.JwtSessionModule;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

public class PersistentCookiePAPTest {

    private PersistentCookieAuthModulePostAuthenticationPlugin persistentCookieAuthPAP;

    @BeforeMethod
    public void setUp() {

        PersistentCookieModuleWrapper persistentCookieWrapper = new PersistentCookieModuleWrapper() {
            @Override
            protected ServiceConfigManager getServiceConfigManager() throws SSOException, SMSException {
                ServiceConfigManager serviceConfigManager = mock(ServiceConfigManager.class);
                ServiceConfig serviceConfig = mock(ServiceConfig.class);
                given(serviceConfig.getAttributes()).willReturn(Collections.singletonMap("iplanet-am-auth-key-alias",
                        (Set<String>) Sets.newHashSet("str")));
                given(serviceConfigManager.getOrganizationConfig(anyString(), anyString())).willReturn(serviceConfig);
                return serviceConfigManager;
            }
        };

        persistentCookieAuthPAP = new PersistentCookieAuthModulePostAuthenticationPlugin(persistentCookieWrapper);
    }

    @Test
    public void shouldInitialisePostAuthProcess() throws Exception {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        given(ssoToken.getProperty(JwtSessionModule.TOKEN_IDLE_TIME_IN_MINUTES_CLAIM_KEY)).willReturn("TOKEN_IDLE_TIME");
        given(ssoToken.getProperty(JwtSessionModule.MAX_TOKEN_LIFE_IN_MINUTES_KEY)).willReturn("TOKEN_MAX_LIFE");
        given(ssoToken.getProperty("openam-auth-persistent-cookie-domains")).willReturn("");

        //When
        Map<String, Object> config = persistentCookieAuthPAP.generateConfig(request, response, ssoToken);

        //Then
        assertEquals(config.get(JwtSessionModule.TOKEN_IDLE_TIME_IN_MINUTES_CLAIM_KEY), "TOKEN_IDLE_TIME");
        assertEquals(config.get(JwtSessionModule.MAX_TOKEN_LIFE_IN_MINUTES_KEY), "TOKEN_MAX_LIFE");
    }

    @Test
    public void shouldInitialiseAuthModuleWithClientIPEnforcedForPAP() throws Exception {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        given(ssoToken.getProperty("openam-auth-persistent-cookie-enforce-ip")).willReturn("true");
        given(ssoToken.getProperty("openam-auth-persistent-cookie-domains")).willReturn("");

        //When
        Map<String, Object> config = persistentCookieAuthPAP.generateConfig(request, response, ssoToken);

        //Then
        assertEquals(config.get("openam-auth-persistent-cookie-enforce-ip"), true);
    }

    @Test
    public void shouldCallOnLoginSuccessWhenJwtNotValidated() throws Exception {

        //Given

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
        persistentCookieAuthPAP.onLoginSuccess(messageInfo, requestParamsMap, request, response, ssoToken);

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
    public void shouldCallOnLoginSuccess() throws Exception {

        //Given
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
        persistentCookieAuthPAP.onLoginSuccess(messageInfo, requestParamsMap, request, response, ssoToken);

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
    public void shouldStoreClientIPOnLoginSuccess() throws Exception {

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
        persistentCookieAuthPAP.onLoginSuccess(messageInfo, Collections.emptyMap(), request, response, ssoToken);

        //Then
        assertEquals(contextMap.get("openam.clientip"), "CLIENT_IP");
    }
}
