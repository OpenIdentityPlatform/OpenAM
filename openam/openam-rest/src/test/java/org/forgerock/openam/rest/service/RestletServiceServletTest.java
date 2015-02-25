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

package org.forgerock.openam.rest.service;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServlet;
import java.util.Collections;
import java.util.Enumeration;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RestletServiceServletTest {

    private RestletServiceServlet restletServiceServlet;

    private HttpServlet servlet;

    @BeforeMethod
    public void setUp() {

        servlet = mock(HttpServlet.class);

        restletServiceServlet = new RestletServiceServlet(servlet, JSONServiceEndpointApplication.class, "json");
    }

    @Test
    public void shouldGetApplicationInitParameter() {

        //Given

        //When
        String initParameter = restletServiceServlet.getInitParameter("org.restlet.application", "ANY");

        //Then
        assertEquals(initParameter, JSONServiceEndpointApplication.class.getName());
    }

    @Test
    public void shouldGetInitParameterWithoutDefault() {

        //Given
        given(servlet.getInitParameter("INIT_PARAM_NAME")).willReturn("VALUE");

        //When
        String initParameter = restletServiceServlet.getInitParameter("INIT_PARAM_NAME", "ANY");

        //Then
        assertEquals(initParameter, "VALUE");
    }

    @Test
    public void shouldGetInitParameterWithDefault() {

        //Given
        given(servlet.getInitParameter("INIT_PARAM_NAME")).willReturn(null);

        //When
        String initParameter = restletServiceServlet.getInitParameter("INIT_PARAM_NAME", "ANY");

        //Then
        assertEquals(initParameter, "ANY");
    }

    @Test
    public void shouldGetServletName() {

        //Given

        //When
        assertEquals(restletServiceServlet.getServletName(), "json");

        //Then
        verifyZeroInteractions(servlet);
    }

    @Test
    public void shouldGetInitParameter() {

        //Given

        //When
        restletServiceServlet.getInitParameter("NAME");

        //Then
        verify(servlet).getInitParameter("NAME");
    }

    @Test
    public void shouldGetInitParameterNames() {

        //Given
        given(servlet.getInitParameterNames()).willReturn(Collections.enumeration(Collections.EMPTY_SET));

        //When
        Enumeration initParameterNames = restletServiceServlet.getInitParameterNames();

        //Then
        boolean found = false;
        while (initParameterNames.hasMoreElements()) {
            if ("org.restlet.application".equals(initParameterNames.nextElement())) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    public void shouldGetServletConfig() {

        //Given

        //When
        restletServiceServlet.getServletConfig();

        //Then
        verify(servlet).getServletConfig();
    }

    @Test
    public void shouldGetServletContext() {

        //Given

        //When
        restletServiceServlet.getServletContext();

        //Then
        verify(servlet).getServletContext();
    }

    @Test
    public void shouldGetServletInfo() {

        //Given

        //When
        restletServiceServlet.getServletInfo();

        //Then
        verify(servlet).getServletInfo();
    }

    @Test
    public void shouldLog() {

        //Given

        //When
        restletServiceServlet.log("MSG");

        //Then
        verify(servlet).log("MSG");
    }

    @Test
    public void shouldLogWithThrowable() {

        //Given
        Throwable t = mock(Throwable.class);

        //When
        restletServiceServlet.log("MSG", t);

        //Then
        verify(servlet).log("MSG", t);
    }
}
