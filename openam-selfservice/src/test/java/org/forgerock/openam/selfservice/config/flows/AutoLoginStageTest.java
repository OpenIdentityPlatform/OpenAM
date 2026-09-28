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
 * Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openam.selfservice.config.flows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.services.context.Context;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.shared.encode.CookieUtils;

public class AutoLoginStageTest {

    private static final String SSO_TOKEN = "AQIC5wM2LY4Sfczn-registered-user";
    private static final String GOTO_URL = "/openam/console";

    private boolean savedHttpOnly;
    private boolean savedAllowTokenInBody;
    private AutoLoginStage.SessionCookieSetter cookieSetter;
    private Context requestContext;
    private ProcessContext processContext;
    private AutoLoginStage stage;

    @BeforeMethod
    public void setUp() throws Exception {
        savedHttpOnly = CookieUtils.isCookieHttpOnly();
        savedAllowTokenInBody = CookieUtils.isHttpOnlyAllowTokenInBody();
        cookieSetter = mock(AutoLoginStage.SessionCookieSetter.class);
        requestContext = mock(Context.class);
        processContext = mock(ProcessContext.class);
        when(processContext.getRequestContext()).thenReturn(requestContext);
        stage = new AutoLoginStage(cookieSetter);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        setCookieUtilsFlag("cookieHttpOnly", savedHttpOnly);
        setCookieUtilsFlag("httpOnlyAllowTokenInBody", savedAllowTokenInBody);
    }

    @Test
    public void httpOnlySetsTheSessionCookieAndKeepsTheTokenOutOfTheResponse() throws Exception {
        setCookieUtilsFlag("cookieHttpOnly", true);
        setCookieUtilsFlag("httpOnlyAllowTokenInBody", false);

        stage.putSessionInSuccessAdditions(processContext, SSO_TOKEN, GOTO_URL);

        verify(cookieSetter).setSessionCookie(requestContext, SSO_TOKEN);
        verify(processContext, never()).putSuccessAddition(eq("tokenId"), any());
        verify(processContext).putSuccessAddition("successUrl", GOTO_URL);
    }

    @Test
    public void httpOnlyWithAllowTokenInBodyAlsoReturnsTheToken() throws Exception {
        setCookieUtilsFlag("cookieHttpOnly", true);
        setCookieUtilsFlag("httpOnlyAllowTokenInBody", true);

        stage.putSessionInSuccessAdditions(processContext, SSO_TOKEN, GOTO_URL);

        verify(cookieSetter).setSessionCookie(requestContext, SSO_TOKEN);
        verify(processContext).putSuccessAddition("tokenId", SSO_TOKEN);
        verify(processContext).putSuccessAddition("successUrl", GOTO_URL);
    }

    @Test
    public void withoutHttpOnlyTheBrowserSetsTheCookieFromTheToken() throws Exception {
        setCookieUtilsFlag("cookieHttpOnly", false);

        stage.putSessionInSuccessAdditions(processContext, SSO_TOKEN, GOTO_URL);

        verify(cookieSetter, never()).setSessionCookie(any(Context.class), anyString());
        verify(processContext).putSuccessAddition("tokenId", SSO_TOKEN);
        verify(processContext).putSuccessAddition("successUrl", GOTO_URL);
    }

    private static void setCookieUtilsFlag(String name, boolean value) throws Exception {
        Field field = CookieUtils.class.getDeclaredField(name);
        field.setAccessible(true);
        field.setBoolean(null, value);
    }
}
