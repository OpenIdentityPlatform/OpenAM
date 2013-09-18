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

package org.forgerock.openam.jaspi.filter;

import org.forgerock.json.resource.servlet.SecurityContextFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;

public class RestSecurityContextMapperTest {

    private RestSecurityContextMapper restSecurityContextMapper;

    @BeforeMethod
    public void setUp() {
        restSecurityContextMapper = new RestSecurityContextMapper();
    }

    @Test
    public void shouldMapAuthAttributesToCRESTAttributesWithNulls() throws IOException, ServletException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        given(request.getHeader(AMAuthNFilter.ATTRIBUTE_AUTH_PRINCIPAL)).willReturn(null);
        given(request.getAttribute(AMAuthNFilter.ATTRIBUTE_AUTH_CONTEXT)).willReturn(null);

        //When
        restSecurityContextMapper.doFilter(request, response, filterChain);

        //Then
        verify(request).setAttribute(SecurityContextFactory.ATTRIBUTE_AUTHCID, null);
        verify(request).setAttribute(SecurityContextFactory.ATTRIBUTE_AUTHZID, null);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void shouldMapAuthAttributesToCRESTAttributes() throws IOException, ServletException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        Map<String, Object> authenticationContext = new HashMap<String, Object>();
        authenticationContext.put("ssoTokenId", "asdweq ED23RFCD");

        given(request.getHeader(AMAuthNFilter.ATTRIBUTE_AUTH_PRINCIPAL)).willReturn("AUTHC_ID");
        given(request.getAttribute(AMAuthNFilter.ATTRIBUTE_AUTH_CONTEXT)).willReturn(authenticationContext);

        //When
        restSecurityContextMapper.doFilter(request, response, filterChain);

        //Then
        verify(request).setAttribute(SecurityContextFactory.ATTRIBUTE_AUTHCID, "AUTHC_ID");
        verify(request).setAttribute(SecurityContextFactory.ATTRIBUTE_AUTHZID, authenticationContext);
        verify(filterChain).doFilter(request, response);
    }
}
