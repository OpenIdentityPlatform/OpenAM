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
package org.forgerock.openam.cors;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CORSFilterTest {

    private CORSService mockService = mock(CORSService.class);
    private CORSFilter testFilter;

    @BeforeMethod
    public void setUp() {
        this.testFilter = new CORSFilter(mockService);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenNullConfig() throws ServletException {
        //given

        //when
        testFilter.init(null);

        //then
    }

    @Test(expectedExceptions = ServletException.class)
    public void shouldThrowExceptionWhenNoConfig() throws ServletException {
        //given
        FilterConfig config = mock(FilterConfig.class);

        //when
        testFilter.init(config);

        //then
    }

    @Test(expectedExceptions = ServletException.class)
    public void shoudlThrowExceptionWhenNoMethodsInConfig() throws ServletException {
        //given
        FilterConfig config = mock(FilterConfig.class);
        given(config.getInitParameter(CORSConstants.ORIGINS_KEY)).willReturn("www.google.com");

        //when
        testFilter.init(config);

        //then

    }

    @Test(expectedExceptions = ServletException.class)
    public void shouldThrowExceptionWhenNoOriginInConfig() throws ServletException {
        //given
        FilterConfig config = mock(FilterConfig.class);
        given(config.getInitParameter(CORSConstants.METHODS_KEY)).willReturn("GET,POST");

        //when
        testFilter.init(config);

        //then
    }

    @Test(expectedExceptions =  ServletException.class)
    public void shouldThrowExceptionWhenMaxAgeIsNaN() throws ServletException {
        //given
        FilterConfig config = mock(FilterConfig.class);
        given(config.getInitParameter(CORSConstants.ORIGINS_KEY)).willReturn("www.google.com");
        given(config.getInitParameter(CORSConstants.METHODS_KEY)).willReturn("GET,POST");
        given(config.getInitParameter(CORSConstants.MAX_AGE_KEY)).willReturn("words");

        //when
        testFilter.init(config);

        //then
    }

    @Test
    public void shouldConfigureWithBothMethodsAndOriginsNoErrors() {
        //given
        FilterConfig config = mock(FilterConfig.class);
        given(config.getInitParameter(CORSConstants.ORIGINS_KEY)).willReturn("www.google.com");
        given(config.getInitParameter(CORSConstants.METHODS_KEY)).willReturn("GET,POST");
        boolean success = true;

        //when
        try {
            testFilter.init(config);
        } catch (ServletException e) {
            success = false;
        }

        //then
        assertTrue(success);
    }

}
