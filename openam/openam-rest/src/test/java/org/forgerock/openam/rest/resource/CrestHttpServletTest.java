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

package org.forgerock.openam.rest.resource;

import org.forgerock.json.resource.ConnectionFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServlet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CrestHttpServletTest {

    private CrestHttpServlet crestHttpServlet;

    private HttpServlet httpServlet;

    @BeforeMethod
    public void setUp() {

        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        httpServlet = mock(HttpServlet.class);

        crestHttpServlet = new CrestHttpServlet(httpServlet, connectionFactory);
    }

    @Test
    public void shouldGetInitParameterFromHttpServlet() {

        //Given
        String name = "NAME";

        //When
        crestHttpServlet.getInitParameter(name);

        //Then
        verify(httpServlet).getInitParameter(name);
    }

    @Test
    public void shouldGetInitParameterNamesFromHttpServlet() {

        //Given

        //When
        crestHttpServlet.getInitParameterNames();

        //Then
        verify(httpServlet).getInitParameterNames();
    }

    @Test
    public void shouldGetServletConfigFromHttpServlet() {

        //Given

        //When
        crestHttpServlet.getServletConfig();

        //Then
        verify(httpServlet).getServletConfig();
    }

    @Test
    public void shouldGetServletContextFromHttpServlet() {

        //Given

        //When
        crestHttpServlet.getServletContext();

        //Then
        verify(httpServlet).getServletContext();
    }

    @Test
    public void shouldGetServletInfoFromHttpServlet() {

        //Given

        //When
        crestHttpServlet.getServletInfo();

        //Then
        verify(httpServlet).getServletInfo();
    }

    @Test
    public void shouldGetServletNameFromHttpServlet() {

        //Given

        //When
        crestHttpServlet.getServletName();

        //Then
        verify(httpServlet).getServletName();
    }

    @Test
    public void shouldLogUsingHttpServlet() {

        //Given
        String msg = "NAME";

        //When
        crestHttpServlet.log(msg);

        //Then
        verify(httpServlet).log(msg);
    }

    @Test
    public void shouldLogWithThrowableUsingHttpServlet() {

        //Given
        String msg = "NAME";
        Throwable t = mock(Throwable.class);

        //When
        crestHttpServlet.log(msg, t);

        //Then
        verify(httpServlet).log(msg, t);
    }
}
