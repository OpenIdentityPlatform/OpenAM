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
package com.sun.identity.federation.common;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Drives {@link FSUtils#forwardRequest} with a same-host target, the branch
 * that ends in a {@code RequestDispatcher} rather than a redirect.
 */
public class FSUtilsForwardPathTest {

    private static final String BASE = "http://sp.example.com:8080/openam";

    private String savedDeploymentURI;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeMethod
    public void setUp() {
        // forwardRequest reads the static deployment URI; pinned per test and
        // restored whatever the outcome.
        savedDeploymentURI = FSUtils.deploymentURI;
        FSUtils.deploymentURI = "/openam";
        request = mock(HttpServletRequest.class);
        when(request.getServerName()).thenReturn("sp.example.com");
        when(request.getServerPort()).thenReturn(8080);
        response = mock(HttpServletResponse.class);
    }

    @AfterMethod(alwaysRun = true)
    public void restoreDeploymentUri() {
        FSUtils.deploymentURI = savedDeploymentURI;
    }

    @Test
    public void forwardsAnInAppTargetOnTheSameHost() throws Exception {
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("/UI/Login?goto=%2Fopenam%2Fconsole")).thenReturn(dispatcher);

        FSUtils.forwardRequest(request, response, BASE + "/UI/Login?goto=%2Fopenam%2Fconsole");

        verify(dispatcher).forward(request, response);
        verify(response, never()).sendError(anyInt());
    }

    @DataProvider
    public Object[][] traversingTargets() {
        return new Object[][] {
            {BASE + "/x/../WEB-INF/web.xml"},
            {BASE + "/WEB-INF/web.xml"},
            // The container decodes the dispatcher path before it normalises it.
            {BASE + "/x/%2e%2e/WEB-INF/web.xml"},
            {BASE + "/x/%252e%252e/WEB-INF/web.xml"},
            {BASE + "/x/..%2fWEB-INF/web.xml"},
            {BASE + "/%57EB-INF/web.xml"},
            {BASE + "/x/%zz"},
            // Forms the container collapses to /WEB-INF/web.xml before mapping.
            {BASE + "//WEB-INF/web.xml"},
            {BASE + "/./WEB-INF/web.xml"},
            {BASE + "/WEB-INF;x/web.xml"},
            {BASE + "/;x/WEB-INF/web.xml"},
            {BASE + "/.;x/WEB-INF/web.xml"},
            {BASE + "/%2e/WEB-INF/web.xml"},
            {BASE + "/WEB-INF#/x"},
            // ";param" is stripped on the raw string, encoded slash included.
            {BASE + "/;%2Fjunk/WEB-INF/web.xml"},
            {BASE + "/;jsessionid=1%2Fa/WEB-INF/web.xml"},
            // request.getRequestDispatcher() drops the fragment: /x/.. -> /
            {BASE + "/x/..#"},
        };
    }

    @Test(dataProvider = "traversingTargets")
    public void answers400WithoutDispatchingForATraversingTarget(String url) throws Exception {
        FSUtils.forwardRequest(request, response, url);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        verify(request, never()).getRequestDispatcher(anyString());
        verify(response, never()).sendRedirect(anyString());
    }
}
