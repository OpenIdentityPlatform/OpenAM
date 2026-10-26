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
package org.forgerock.openam.rest.jakarta.servlet;

import static java.util.Collections.emptyEnumeration;
import static java.util.Collections.enumeration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.restlet.engine.adapter.HttpRequest;
import org.restlet.engine.adapter.HttpServerHelper;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Drives a Servlet request through the server adapter that {@link ServerServlet} configures, the way the Restlet
 * engine does when it handles a call.
 */
public class ServerServletTest {

    private ServerServlet servlet;
    private HttpServletRequest request;

    @BeforeMethod
    public void setUp() throws Exception {
        ServletContext servletContext = mock(ServletContext.class);
        given(servletContext.getInitParameterNames()).willReturn(emptyEnumeration());
        given(servletContext.getAttributeNames()).willReturn(emptyEnumeration());
        ServletConfig servletConfig = mock(ServletConfig.class);
        given(servletConfig.getServletName()).willReturn("restletServiceServlet");
        given(servletConfig.getServletContext()).willReturn(servletContext);
        given(servletConfig.getInitParameterNames()).willReturn(emptyEnumeration());

        servlet = new ServerServlet();
        servlet.init(servletConfig);

        request = mock(HttpServletRequest.class);
        given(request.getMethod()).willReturn("GET");
        given(request.getScheme()).willReturn("http");
        given(request.getProtocol()).willReturn("HTTP/1.1");
        given(request.getServerName()).willReturn("openam.example.com");
        given(request.getServerPort()).willReturn(8080);
        given(request.getLocalAddr()).willReturn("127.0.0.1");
        given(request.getLocalPort()).willReturn(8080);
        given(request.getContextPath()).willReturn("/openam");
        given(request.getServletPath()).willReturn("/oauth2");
        given(request.getHeaderNames()).willReturn(emptyEnumeration());
        given(request.getAttributeNames()).willReturn(emptyEnumeration());
    }

    @AfterMethod
    public void tearDown() {
        servlet.destroy();
    }

    @Test
    public void shouldRemoveJsessionidMatrixParameterFromLastSegment() throws Exception {
        given(request.getRequestURI()).willReturn("/openam/oauth2/authorize;jsessionid=0123ABCD");

        HttpRequest restletRequest = toRestletRequest();

        assertThat(restletRequest.getResourceRef().getLastSegment()).isEqualTo("authorize");
    }

    @Test
    public void shouldKeepOtherMatrixParametersWhenRemovingJsessionid() throws Exception {
        given(request.getRequestURI()).willReturn("/openam/oauth2/authorize;jsessionid=0123ABCD;mode=compact");

        HttpRequest restletRequest = toRestletRequest();

        assertThat(restletRequest.getResourceRef().getLastSegment()).isEqualTo("authorize;mode=compact");
    }

    @Test
    public void shouldCopyServletRequestAttributesIntoRestletRequestAttributes() throws Exception {
        given(request.getRequestURI()).willReturn("/openam/oauth2/authorize");
        Object authContext = Collections.singletonMap("tokenId", "AQIC5w");
        given(request.getAttributeNames()).willReturn(enumeration(Arrays.asList(
                "org.forgerock.authentication.context", "jakarta.servlet.request.cipher_suite")));
        given(request.getAttribute("org.forgerock.authentication.context")).willReturn(authContext);
        given(request.getAttribute("jakarta.servlet.request.cipher_suite")).willReturn("TLS_AES_128_GCM_SHA256");

        HttpRequest restletRequest = toRestletRequest();

        assertThat(restletRequest.getAttributes())
                .containsEntry("org.forgerock.authentication.context", authContext)
                .containsEntry("jakarta.servlet.request.cipher_suite", "TLS_AES_128_GCM_SHA256");
    }

    @Test
    public void shouldSkipServletRequestAttributesWithoutValue() throws Exception {
        given(request.getRequestURI()).willReturn("/openam/oauth2/authorize");
        given(request.getAttributeNames()).willReturn(enumeration(Arrays.asList("removedMeanwhile", "kept")));
        given(request.getAttribute("removedMeanwhile")).willReturn(null);
        given(request.getAttribute("kept")).willReturn("value");

        HttpRequest restletRequest = toRestletRequest();

        assertThat(restletRequest.getAttributes()).doesNotContainKey("removedMeanwhile").containsEntry("kept", "value");
    }

    private HttpRequest toRestletRequest() throws Exception {
        HttpServerHelper server = servlet.createServer(request);
        return server.getAdapter().toRequest(
                servlet.createCall(server.getHelped(), request, mock(HttpServletResponse.class)));
    }
}
