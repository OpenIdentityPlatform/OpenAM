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
 * Copyright 2013 ForgeRock AS
 */

package org.forgerock.openam.authz.filter.session;

import com.iplanet.dpro.session.service.SessionService;
import org.forgerock.openam.auth.shared.AuthnRequestUtils;
import org.forgerock.openam.auth.shared.SSOTokenFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.BDDMockito.*;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SessionResourceAuthzFilterTest {

    private SessionResourceAuthzFilter sessionResourceAuthzFilter;

    private AuthnRequestUtils requestUtils;
    private SessionService sessionService;
    private SSOTokenFactory ssoTokenFactory;

    @BeforeMethod
    public void setUp() {
        requestUtils = mock(AuthnRequestUtils.class);
        sessionService = mock(SessionService.class);
        ssoTokenFactory = mock(SSOTokenFactory.class);

        sessionResourceAuthzFilter = new SessionResourceAuthzFilter(ssoTokenFactory, requestUtils, sessionService);
    }

    @Test
    public void shouldReturnTrueWhenRequestQueryParamsContainsActionLogout() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        Map<String, String[]> parameterMap = new HashMap<String, String[]>();

        given(request.getParameterMap()).willReturn(parameterMap);
        parameterMap.put("_action", new String[]{"logOUT"});

        //When
        boolean authorized = sessionResourceAuthzFilter.authorize(request, null);

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
        boolean authorized = sessionResourceAuthzFilter.authorize(request, null);

        //Then
        assertFalse(authorized);
    }
}
