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

package org.forgerock.openam.jaspi.modules.session;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static junit.framework.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class LocalSSOTokenSessionModuleTest {

    private static final String SSO_TOKEN_ID_COOKIE_NAME = "TOKEN_COOKIE_NAME";

    private LocalSSOTokenSessionModule localSSOTokenSessionModule;
    private SSOTokenManager ssoTokenManager;

    @BeforeMethod
    public void setUpMethod() {

        ssoTokenManager = mock(SSOTokenManager.class);

        localSSOTokenSessionModule = new LocalSSOTokenSessionModule() {
            @Override
            protected String getSSOTokenCookieName() {
                return SSO_TOKEN_ID_COOKIE_NAME;
            }

            @Override
            protected SSOTokenManager getSSOTokenManager() throws SSOException {
                return ssoTokenManager;
            }
        };
    }

    @Test
    public void shouldGetSupportedMessageTypes() {

        //Given

        //When
        Class[] supportedMessageTypes = localSSOTokenSessionModule.getSupportedMessageTypes();

        //Then
        assertEquals(supportedMessageTypes.length, 2);
        assertEquals(supportedMessageTypes[0], HttpServletRequest.class);
        assertEquals(supportedMessageTypes[1], HttpServletResponse.class);
    }

    @Test
    public void shouldValidateRequestWithCookiesNull() {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(messageInfo.getRequestMessage()).willReturn(request);

        //When
        AuthStatus authStatus = localSSOTokenSessionModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldValidateRequestWithCookiesEmpty() {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getCookies()).willReturn(new Cookie[0]);

        //When
        AuthStatus authStatus = localSSOTokenSessionModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldValidateRequestWithCookiesNoSSOToken() {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getCookies()).willReturn(new Cookie[]{new Cookie("2", "2"), new Cookie("1", "1")});

        //When
        AuthStatus authStatus = localSSOTokenSessionModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldValidateRequestWithCookiesIncludingInvalidSSOToken() throws SSOException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getCookies()).willReturn(new Cookie[]{new Cookie("2", "2"),
                new Cookie(SSO_TOKEN_ID_COOKIE_NAME, "SSO_TOKEN_ID"), new Cookie("1", "1")});
        given(ssoTokenManager.createSSOToken("SSO_TOKEN_ID")).willThrow(SSOException.class);

        //When
        AuthStatus authStatus = localSSOTokenSessionModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_FAILURE);
    }

    @Test
    public void shouldValidateRequestWithCookiesIncludingValidSSOToken() throws SSOException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject clientSubject = new Subject();
        Subject serviceSubject = new Subject();
        HttpServletRequest request = mock(HttpServletRequest.class);
        SSOToken ssoToken = mock(SSOToken.class);

        given(messageInfo.getRequestMessage()).willReturn(request);
        given(request.getCookies()).willReturn(new Cookie[]{new Cookie("2", "2"),
                new Cookie(SSO_TOKEN_ID_COOKIE_NAME, "SSO_TOKEN_ID"), new Cookie("1", "1")});
        given(ssoTokenManager.createSSOToken("SSO_TOKEN_ID")).willReturn(ssoToken);

        //When
        AuthStatus authStatus = localSSOTokenSessionModule.validateRequest(messageInfo, clientSubject, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SUCCESS);
    }

    @Test
    public void shouldSecureResponse() {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Subject serviceSubject = new Subject();

        //When
        AuthStatus authStatus = localSSOTokenSessionModule.secureResponse(messageInfo, serviceSubject);

        //Then
        assertEquals(authStatus, AuthStatus.SEND_SUCCESS);
    }
}
