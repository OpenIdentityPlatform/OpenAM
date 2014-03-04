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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openam.authz.filter.session;

import com.iplanet.dpro.session.service.SessionService;
import org.forgerock.authz.AuthorizationContext;
import org.forgerock.openam.auth.shared.AuthUtilsWrapper;
import org.forgerock.openam.auth.shared.AuthnRequestUtils;
import org.forgerock.openam.auth.shared.SSOTokenFactory;
import org.forgerock.openam.utils.Config;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SessionResourceAuthzFilterTest {

    private SessionResourceAuthorizationModule sessionResourceAuthzModule;

    private AuthnRequestUtils requestUtils;
    private SessionService sessionService;
    private SSOTokenFactory ssoTokenFactory;
    private AuthUtilsWrapper authUtilsWrapper;

    @BeforeMethod
    public void setUp() throws ExecutionException, InterruptedException {
        requestUtils = mock(AuthnRequestUtils.class);
        sessionService = mock(SessionService.class);
        ssoTokenFactory = mock(SSOTokenFactory.class);
        authUtilsWrapper = mock(AuthUtilsWrapper.class);

        Config<SessionService> sessionServiceConfig = mock(Config.class);
        given(sessionServiceConfig.isReady()).willReturn(true);
        given(sessionServiceConfig.get()).willReturn(sessionService);

        sessionResourceAuthzModule = new SessionResourceAuthorizationModule(ssoTokenFactory, requestUtils,
                sessionServiceConfig, authUtilsWrapper);
    }

    @Test
    public void shouldReturnTrueWhenRequestQueryParamsContainsActionLogout() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        Map<String, String[]> parameterMap = new HashMap<String, String[]>();

        given(request.getParameterMap()).willReturn(parameterMap);
        parameterMap.put("_action", new String[]{"logOUT"});

        //When
        boolean authorized = sessionResourceAuthzModule.authorize(request, mock(AuthorizationContext.class));

        //Then
        assertTrue(authorized);
    }

    @Test
    public void shouldFallbackToAdminOnlyAuthzAndReturnFalse() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        Map<String, String> parameterMap = new HashMap<String, String>();

        given(request.getParameterMap()).willReturn(parameterMap);

        //When
        boolean authorized = sessionResourceAuthzModule.authorize(request, mock(AuthorizationContext.class));

        //Then
        assertFalse(authorized);
    }
}
