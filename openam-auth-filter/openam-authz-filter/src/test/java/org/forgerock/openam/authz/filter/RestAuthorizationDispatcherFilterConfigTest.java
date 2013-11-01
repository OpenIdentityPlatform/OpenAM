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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.openam.authz.filter;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.FilterConfig;

import java.util.Collections;
import java.util.Enumeration;

import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;
import static org.testng.Assert.assertEquals;

public class RestAuthorizationDispatcherFilterConfigTest {

    private RestAuthorizationDispatcherFilterConfig filterConfig;

    private FilterConfig wrappedFilterConfig;
    private String authzConfiguratorClassName;

    @BeforeMethod
    public void setUp() {

        wrappedFilterConfig = mock(FilterConfig.class);
        authzConfiguratorClassName = "AUTHZ_CONFIGURATOR_CLASS_NAME";

        filterConfig = new RestAuthorizationDispatcherFilterConfig(wrappedFilterConfig, authzConfiguratorClassName);
    }

    @Test
    public void shouldGetFilterName() {

        //Given

        //When
        filterConfig.getFilterName();

        //Then
        verify(wrappedFilterConfig).getFilterName();
    }

    @Test
    public void shouldGetServletContext() {

        //Given

        //When
        filterConfig.getServletContext();

        //Then
        verify(wrappedFilterConfig).getServletContext();
    }

    @Test
    public void shouldGetConfiguratorInitParameter() {

        //Given
        given(wrappedFilterConfig.getInitParameter("other")).willReturn("OTHER");

        //When
        String initParam = filterConfig.getInitParameter("configurator");

        //Then
        assertEquals(initParam, authzConfiguratorClassName);
    }

    @Test
    public void shouldGetOtherInitParameter() {

        //Given
        given(wrappedFilterConfig.getInitParameter("other")).willReturn("OTHER");

        //When
        String initParam = filterConfig.getInitParameter("other");

        //Then
        assertEquals(initParam, "OTHER");
    }

    @Test
    public void shouldGetInitParameterNames() {

        //Given
        given(wrappedFilterConfig.getInitParameterNames()).willReturn(Collections.enumeration(Collections.emptyList()));

        //When
        Enumeration<String> initParameterNames = filterConfig.getInitParameterNames();

        //Then
        verify(wrappedFilterConfig).getInitParameterNames();
        assertEquals(initParameterNames.nextElement(), "configurator");
    }
}
