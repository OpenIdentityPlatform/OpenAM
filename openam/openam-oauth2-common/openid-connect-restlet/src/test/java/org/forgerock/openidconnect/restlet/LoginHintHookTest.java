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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openidconnect.restlet;

import org.forgerock.oauth2.core.OAuth2Request;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Cookie;
import org.restlet.data.CookieSetting;
import org.restlet.util.Series;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.*;
import static org.forgerock.oauth2.core.OAuth2Constants.Custom.*;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.*;
import static org.mockito.Mockito.*;

public class LoginHintHookTest {

    private LoginHintHook hook = new LoginHintHook();
    private OAuth2Request o2request;
    private Request request;
    private Response response;

    @BeforeMethod
    public void setup() {
        o2request = mock(OAuth2Request.class);
        request = mock(Request.class);
        response = mock(Response.class);
    }

    @Test
    public void testBeforeAuthorizeHandlingNoLoginHint() throws Exception {
        //given
        Series<CookieSetting> cookieSettings = new Series<CookieSetting>(CookieSetting.class);
        when(response.getCookieSettings()).thenReturn(cookieSettings);
        when(o2request.getParameter(anyString())).thenReturn(null);

        //when
        hook.beforeAuthorizeHandling(o2request, request, response);

        //then
        verify(o2request).getParameter(LOGIN_HINT);
        assertThat(cookieSettings.size()).isEqualTo(0);
    }

    @Test
    public void testBeforeAuthorizeHandling() throws Exception {
        //given
        Series<CookieSetting> cookieSettings = new Series<CookieSetting>(CookieSetting.class);
        when(response.getCookieSettings()).thenReturn(cookieSettings);
        when(o2request.getParameter(LOGIN_HINT)).thenReturn("msisdn=07898989898");
        Series<Cookie> cookies = new Series<Cookie>(Cookie.class);
        when(request.getCookies()).thenReturn(cookies);

        //when
        hook.beforeAuthorizeHandling(o2request, request, response);

        //then
        verify(o2request).getParameter(LOGIN_HINT);
        assertThat(cookieSettings.size()).isEqualTo(1);
        assertThat(cookieSettings.getFirst(LOGIN_HINT_COOKIE).getValue()).isEqualTo("msisdn=07898989898");
        assertThat(cookieSettings.getFirst(LOGIN_HINT_COOKIE).isAccessRestricted()).isTrue();
    }

    @Test
    public void testAfterAuthorizeSuccessWithCookieSetting() throws Exception {
        //given
        Series<CookieSetting> cookieSettings = new Series<CookieSetting>(CookieSetting.class);
        when(response.getCookieSettings()).thenReturn(cookieSettings);
        when(o2request.getParameter(LOGIN_HINT)).thenReturn("msisdn=07898989898");
        Series<Cookie> cookies = new Series<Cookie>(Cookie.class);
        when(request.getCookies()).thenReturn(cookies);

        hook.beforeAuthorizeHandling(o2request, request, response);

        //when
        hook.afterAuthorizeSuccess(o2request, request, response);

        //then
        verify(request, times(2)).getCookies();
        verify(response, times(2)).getCookieSettings();
        assertThat(cookieSettings.size()).isEqualTo(0);
    }

    @Test
    public void testAfterAuthorizeSuccessWithCookie() throws Exception {
        //given
        Series<Cookie> cookies = new Series<Cookie>(Cookie.class);
        cookies.add(LOGIN_HINT_COOKIE, "fred");
        when(request.getCookies()).thenReturn(cookies);
        Series<CookieSetting> cookieSettings = new Series<CookieSetting>(CookieSetting.class);
        when(response.getCookieSettings()).thenReturn(cookieSettings);

        //when
        hook.afterAuthorizeSuccess(o2request, request, response);

        //then
        verify(request).getCookies();
        verify(response, times(2)).getCookieSettings();
        assertThat(cookieSettings.size()).isEqualTo(1);
        assertThat(cookieSettings.getFirst(LOGIN_HINT_COOKIE).getMaxAge()).isEqualTo(0);
    }
    @Test
    public void testAfterTokenHandlingNoCookie() throws Exception {
        //given
        Series<Cookie> cookies = new Series<Cookie>(Cookie.class);
        when(request.getCookies()).thenReturn(cookies);

        //when
        hook.afterTokenHandling(o2request, request, response);

        //then
        verify(request).getCookies();
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testAfterTokenHandling() throws Exception {
        //given
        Series<Cookie> cookies = new Series<Cookie>(Cookie.class);
        cookies.add(LOGIN_HINT_COOKIE, "fred");
        when(request.getCookies()).thenReturn(cookies);
        Series<CookieSetting> cookieSettings = new Series<CookieSetting>(CookieSetting.class);
        when(response.getCookieSettings()).thenReturn(cookieSettings);

        //when
        hook.afterTokenHandling(o2request, request, response);

        //then
        verify(request).getCookies();
        verify(response).getCookieSettings();
        assertThat(cookieSettings.size()).isEqualTo(1);
        assertThat(cookieSettings.getFirst(LOGIN_HINT_COOKIE).getMaxAge()).isEqualTo(0);
    }
}