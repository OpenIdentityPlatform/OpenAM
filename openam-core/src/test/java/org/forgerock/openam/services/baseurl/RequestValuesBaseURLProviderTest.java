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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.services.baseurl;

import static org.fest.assertions.Assertions.*;
import static org.mockito.Mockito.*;

import javax.servlet.http.HttpServletRequest;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RequestValuesBaseURLProviderTest {

    private RequestValuesBaseURLProvider provider;

    @BeforeMethod
    public void setup() {
        provider = new RequestValuesBaseURLProvider();
    }

    @Test
    public void testGetBaseURL() throws Exception {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("fred");
        when(request.getServerPort()).thenReturn(8080);

        provider.setContextPath("");

        // When
        String url = provider.getURL(request);

        // Then
        assertThat(url).isEqualTo("http://fred:8080");
    }

    @Test
    public void testGetBaseURLWithContext() throws Exception {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("/fred");
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("fred");
        when(request.getServerPort()).thenReturn(8080);

        provider.setContextPath("/openam");

        // When
        String url = provider.getURL(request);

        // Then
        assertThat(url).isEqualTo("http://fred:8080/openam");
    }

}