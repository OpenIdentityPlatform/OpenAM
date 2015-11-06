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

package com.sun.identity.setup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.fail;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;

import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.shared.Constants;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AMSetupFilterTest {

    private AMSetupFilter setupFilter;
    private ServletContext context;

    private AMSetupFilter.AMSetupWrapper setupWrapper;

    @BeforeMethod
    public void setup() {
        setupWrapper = mock(AMSetupFilter.AMSetupWrapper.class);
        setupFilter = new AMSetupFilter(setupWrapper);

        context = mock(ServletContext.class);
    }

    @AfterMethod
    public void tearDown() {
        System.clearProperty(Constants.CONFIG_STORE_DOWN_REDIRECT_URL);
    }

    @Test
    public void initShouldSetEncryptionPasswordIfNotConfigured() throws Exception {

        //Given
        FilterConfig config = mock(FilterConfig.class);
        ServletContext context = mock(ServletContext.class);

        given(config.getServletContext()).willReturn(context);
        given(setupWrapper.getRandomString()).willReturn("RANDOM_STRING");

        systemIsNotConfigured();

        //When
        setupFilter.init(config);

        //Then
        verify(context).setAttribute("am.enc.pwd", "RANDOM_STRING");
    }

    @Test
    public void initShouldNotSetEncryptionPasswordIfConfigured() throws Exception {

        //Given
        FilterConfig config = mock(FilterConfig.class);

        given(config.getServletContext()).willReturn(context);

        systemIsConfigured();

        //When
        setupFilter.init(config);

        //Then
        verifyZeroInteractions(context);
    }

    @Test
    public void filterShouldAllowRequestsThroughIfConfigured() throws Exception {

        //Given
        HttpServletRequest request = mockRequest("REQUEST_URI");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        systemIsConfigured();

        //When
        setupFilter.doFilter(request, response, chain);

        //Then
        verify(chain).doFilter(request, response);
    }

    @DataProvider(name = "setupRequestUris")
    private Object[][] getSetupRequestUris() {
        return new Object[][]{
            {"/config/options.htm"},
            {"/setup/setSetupProgress"},
            {"/config/upgrade/upgrade.htm"},
            {"/upgrade/setUpgradeProgress"},
        };
    }

    @Test(dataProvider = "setupRequestUris")
    public void filterShouldRedirectSetupRequestsIfConfigured(String requestUri) throws Exception {

        //Given
        HttpServletRequest request = mockRequest(requestUri);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        systemIsConfigured();

        //When
        setupFilter.doFilter(request, response, chain);

        //Then
        verify(response).sendRedirect("SCHEME://SERVER_NAME:8080/CONTEXT_PATH");
        verifyZeroInteractions(chain);
    }

    @Test
    public void filterShouldRedirectRequestsIfUpgradeInProgressButConfigStoreIsDown() throws Exception {

        //Given
        HttpServletRequest request = mockRequest("REQUEST_URI");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        systemIsBeingUpgraded();
        configStoreIsDown("CONFIG_STORE_DOWN_REDIRECT_URI");

        //When
        setupFilter.doFilter(request, response, chain);

        //Then
        verify(response).sendRedirect("CONFIG_STORE_DOWN_REDIRECT_URI");
        verifyZeroInteractions(chain);
    }

    @Test
    public void filterShouldThrowConfigurationExceptionIfUpgradeInProgressAndConfigStoreIsDownButNoRedirectUriSet()
            throws Exception {

        //Given
        HttpServletRequest request = mockRequest("REQUEST_URI");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        systemIsBeingUpgraded();
        configStoreIsDown(null);

        //When
        try {
            setupFilter.doFilter(request, response, chain);
            fail("Expected ServletException with ConfigurationException is cause");
        } catch (ServletException e) {
            //Then
            assertThat(e.getCause()).isInstanceOf(ConfigurationException.class);
            verifyZeroInteractions(response, chain);
        }
    }

    @Test
    public void filterShouldAllowConfiguratorRequestsThroughIfNotConfigured() throws Exception {

        //Given
        HttpServletRequest request = mockRequest("/configurator");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        systemIsNotConfigured();

        //When
        setupFilter.doFilter(request, response, chain);

        //Then
        verify(chain).doFilter(request, response);
    }

    @Test
    public void filterShouldRedirectRequestsToSetupPageIfNotConfigured() throws Exception {

        //Given
        HttpServletRequest request = mockRequest("REQUEST_URI");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        systemIsNotConfigured();
        withWritePermissionsOnUserHomeDirectory();

        //When
        setupFilter.doFilter(request, response, chain);

        //Then
        verify(response).sendRedirect("SCHEME://SERVER_NAME:8080/CONTEXT_PATH/config/options.htm");
        verifyZeroInteractions(chain);
    }

    @Test
    public void filterShouldRedirectRequestsToNoWritePermissionPageIfNotConfiguredAndCannotWriteToUseHomeDirectory()
            throws Exception {

        //Given
        HttpServletRequest request = mockRequest("REQUEST_URI");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        systemIsNotConfigured();
        noWritePermissionsOnUserHomeDirectory();

        //When
        setupFilter.doFilter(request, response, chain);

        //Then
        verify(response).sendRedirect("SCHEME://SERVER_NAME:8080/CONTEXT_PATH/nowritewarning.jsp");
        verifyZeroInteractions(chain);
    }

    @DataProvider(name = "allowedRequests")
    private Object[][] getAllowedRequestsWhilstConfiguring() {
        return new Object[][]{
            {".ico"},
            {".htm"},
            {".css"},
            {".js"},
            {".jpg"},
            {".gif"},
            {".png"},
            {".JPG"},
            {"SMSObjectIF"},
            {"setSetupProgress"},
            {"setUpgradeProgress"},
            {"/legal-notices/"},
        };
    }

    @Test(dataProvider = "allowedRequests")
    public void filterShouldAllowCertainRequestsThroughIfNotConfiguredAndInConfigurationMode(String requestUriSuffix)
            throws Exception {

        //Previous request must have been redirected to setup page to set the pass-through flag
        filterShouldRedirectRequestsToSetupPageIfNotConfigured();

        //Given
        HttpServletRequest request = mockRequest("REQUEST_URI" + requestUriSuffix);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        systemIsNotConfigured();

        //When
        setupFilter.doFilter(request, response, chain);

        //Then
        verify(chain).doFilter(request, response);
    }

    @Test
    public void filterShouldRedirectRequestsToSetupPageIfNotConfiguredAndInConfigurationMode() throws Exception {

        //Given
        //Previous request must have been redirected to setup page to set the pass-through flag
        filterShouldRedirectRequestsToSetupPageIfNotConfigured();

        //When/Then
        filterShouldRedirectRequestsToSetupPageIfNotConfigured();
    }

    private void systemIsNotConfigured() {
        given(setupWrapper.checkInitState(context)).willReturn(false);
        given(setupWrapper.isCurrentConfigurationValid()).willReturn(false);
    }

    private void systemIsConfigured() {
        given(setupWrapper.checkInitState(context)).willReturn(true);
        given(setupWrapper.isCurrentConfigurationValid()).willReturn(true);
    }

    private void systemIsBeingUpgraded() {
        given(setupWrapper.isCurrentConfigurationValid()).willReturn(false);
    }

    private void configStoreIsDown(String redirectUri) {
        if (redirectUri != null) {
            System.setProperty(Constants.CONFIG_STORE_DOWN_REDIRECT_URL, redirectUri);
        }
        given(setupWrapper.getBootStrapFile()).willReturn("BOOTSTRAP_FILE_LOCATION");
        given(setupWrapper.isVersionNewer()).willReturn(false);
        given(setupWrapper.isUpgradeCompleted()).willReturn(false);
    }

    private HttpServletRequest mockRequest(String requestUri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        given(request.getScheme()).willReturn("SCHEME");
        given(request.getServerName()).willReturn("SERVER_NAME");
        given(request.getServerPort()).willReturn(8080);
        given(request.getContextPath()).willReturn("/CONTEXT_PATH");
        given(request.getRequestURI()).willReturn(requestUri);
        return request;
    }

    private void withWritePermissionsOnUserHomeDirectory() {
        File userHomeDirectory = mock(File.class);
        given(setupWrapper.getUserHomeDirectory()).willReturn(userHomeDirectory);
        given(userHomeDirectory.canWrite()).willReturn(true);
    }

    private void noWritePermissionsOnUserHomeDirectory() {
        File userHomeDirectory = mock(File.class);
        given(setupWrapper.getUserHomeDirectory()).willReturn(userHomeDirectory);
        given(userHomeDirectory.canWrite()).willReturn(false);
    }
}
