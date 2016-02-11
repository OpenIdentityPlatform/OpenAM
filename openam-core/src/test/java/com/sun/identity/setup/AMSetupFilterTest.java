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
import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.fail;

import java.io.File;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mockito.Mock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.inject.Injector;
import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.shared.Constants;

public class AMSetupFilterTest {

    private AMSetupFilter setupFilter;

    @Mock
    private AMSetupManager setupManager;
    private Injector injector;

    @BeforeClass
    public void setupClass() {
        injector = mock(Injector.class);
        SystemStartupInjectorHolder.initialise(injector);
    }

    @BeforeMethod
    public void setup() {
        initMocks(this);
        given(injector.getInstance(AMSetupManager.class)).willReturn(setupManager);

        setupFilter = new AMSetupFilter();
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

        systemIsNotConfigured();

        //When
        setupFilter.init(config);

        //Then
        verify(context).setAttribute(eq("am.enc.pwd"), anyString());
    }

    @Test
    public void initShouldNotSetEncryptionPasswordIfConfigured() throws Exception {

        //Given
        FilterConfig config = mock(FilterConfig.class);
        ServletContext context = mock(ServletContext.class);
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
        initializeFilter();
        HttpServletRequest request = mockRequest("REQUEST_URI");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        systemIsConfigured();

        //When
        setupFilter.doFilter(request, response, chain);

        //Then
        verify(chain).doFilter(request, response);
    }

    @DataProvider
    private Object[][] setupRequestUris() {
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
        initializeFilter();
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
        initializeFilter();
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
        initializeFilter();
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
        initializeFilter();
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
        initializeFilter();
        HttpServletRequest request = mockRequest("REQUEST_URI");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        systemIsNotConfigured();
        withWritePermissionsOnBootstrapRootDirectory();

        //When
        setupFilter.doFilter(request, response, chain);

        //Then
        verify(response).sendRedirect("SCHEME://SERVER_NAME:8080/CONTEXT_PATH/config/options.htm");
        verifyZeroInteractions(chain);
    }

    @Test
    public void filterShouldThrowExceptionIfNotConfiguredAndCannotWriteToUserHomeDirectory()
            throws Exception {

        //Given
        initializeFilter();
        HttpServletRequest request = mockRequest("REQUEST_URI");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        systemIsNotConfigured();
        noWritePermissionsOnBootstrapRootDirectory();

        //When
        try {
            setupFilter.doFilter(request, response, chain);
            fail("Expected ServletException with ConfigurationException as cause");
        } catch (ServletException e) {
            //Then
            assertThat(e.getCause()).isInstanceOf(ConfigurationException.class);
            verifyZeroInteractions(response, chain);
        }
    }

    @DataProvider
    private Object[][] allowedRequestsWhilstConfiguring() {
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

    @Test(dataProvider = "allowedRequestsWhilstConfiguring")
    public void filterShouldAllowCertainRequestsThroughIfNotConfiguredAndInConfigurationMode(String suffix)
            throws Exception {

        //Previous request must have been redirected to setup page to set the pass-through flag
        filterShouldRedirectRequestsToSetupPageIfNotConfigured();

        //Given
        HttpServletRequest request = mockRequest(suffix);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

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

    private void initializeFilter() throws ServletException {
        FilterConfig config = mock(FilterConfig.class);
        ServletContext context = mock(ServletContext.class);
        given(config.getServletContext()).willReturn(context);
        setupFilter.init(config);
    }

    private void systemIsNotConfigured() {
        given(setupManager.isConfigured()).willReturn(false);
        given(setupManager.isCurrentConfigurationValid()).willReturn(false);
    }

    private void systemIsConfigured() {
        given(setupManager.isConfigured()).willReturn(true);
        given(setupManager.isCurrentConfigurationValid()).willReturn(true);
    }

    private void systemIsBeingUpgraded() {
        given(setupManager.isCurrentConfigurationValid()).willReturn(false);
    }

    private void configStoreIsDown(String redirectUri) {
        if (redirectUri != null) {
            System.setProperty(Constants.CONFIG_STORE_DOWN_REDIRECT_URL, redirectUri);
        }
        given(setupManager.getBootStrapFileLocation()).willReturn("BOOTSTRAP_FILE_LOCATION");
        given(setupManager.isVersionNewer()).willReturn(false);
        given(setupManager.isUpgradeCompleted()).willReturn(false);
    }

    private HttpServletRequest mockRequest(String suffix) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        given(request.getScheme()).willReturn("SCHEME");
        given(request.getServerName()).willReturn("SERVER_NAME");
        given(request.getServerPort()).willReturn(8080);
        given(request.getContextPath()).willReturn("/CONTEXT_PATH");
        given(request.getRequestURI()).willReturn("REQUEST_URI" + suffix);
        given(request.getServletPath()).willReturn("SERVLET_PATH" + suffix);
        given(request.getPathInfo()).willReturn("PATH_INFO" + suffix);
        return request;
    }

    private void withWritePermissionsOnBootstrapRootDirectory() {
        File bootstrapRootDirectory = mock(File.class);
        given(setupManager.getUserHomeDirectory()).willReturn(bootstrapRootDirectory);
        given(bootstrapRootDirectory.canWrite()).willReturn(true);
    }

    private void noWritePermissionsOnBootstrapRootDirectory() {
        File bootstrapRootDirectory = mock(File.class);
        given(setupManager.getUserHomeDirectory()).willReturn(bootstrapRootDirectory);
        given(bootstrapRootDirectory.canWrite()).willReturn(false);
    }
}
