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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2026 3A Systems LLC.
 */
package org.openidentityplatform.openam.config.servlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Routing behavior of {@link ConfiguratorServlet} against the real migrated-page registry
 * (steps 1-7, the wizard shell, {@code Options}, {@code DefaultSummary} and {@code Upgrade} -
 * every real configurator page there is): a registered path dispatches {@code ?actionLink=}
 * requests to the matching {@code @ConfiguratorAction} method, and an unregistered path 404s.
 * Apache Click (and the named-dispatch fallback to it) has been removed.
 *
 * <p>{@code upgrade.htm} is used below to prove the {@code ServiceLoader}-registered page path
 * still resolves - within {@code openam-core}'s own test classpath (which never has
 * {@code openam-upgrade} on it, so {@code ConfiguratorPageProvider} discovery finds zero
 * providers here), it exercises the genuinely-unregistered/404 branch instead. The real
 * end-to-end routing through the actual {@code Upgrade} page is covered by a dedicated test in
 * {@code openam-upgrade}, which - unlike this module - can see both classes on its classpath.
 */
public class ConfiguratorServletTest {

    /** Any page would do - validateInput is inherited by all of them - and Step1's onInit() is cheap. */
    private static final String ANY_PAGE = "/config/wizard/step1.htm";
    private static final String MISSING_FIELD = "Missing Required Field";

    private ConfiguratorServlet servlet;
    private ServletContext servletContext;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter responseBody;
    private Map<String, Object> sessionAttributes;

    @BeforeMethod
    public void setup() throws Exception {
        servlet = new ConfiguratorServlet();

        servletContext = mock(ServletContext.class);
        ServletConfig servletConfig = mock(ServletConfig.class);
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletConfig.getServletName()).thenReturn("configurator-servlet");
        servlet.init(servletConfig);

        sessionAttributes = new HashMap<>();
        HttpSession session = mock(HttpSession.class);
        doAnswer(inv -> sessionAttributes.put(inv.getArgument(0), inv.getArgument(1)))
                .when(session).setAttribute(anyString(), any());

        request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(request.getParameter("locale")).thenReturn("en");

        responseBody = new StringWriter();
        response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));
    }

    private void callValidateInput(String key, String value) throws Exception {
        when(request.getServletPath()).thenReturn(ANY_PAGE);
        when(request.getParameter("actionLink")).thenReturn("validateInput");
        when(request.getParameter("key")).thenReturn(key);
        when(request.getParameter("value")).thenReturn(value);

        servlet.service(request, response);
    }

    @Test
    public void validateInputStoresAKeyTheWizardActuallySends() throws Exception {
        callValidateInput("serverURL", "https://openam.example.com:8443/openam");

        assertThat(responseBody.toString()).isEqualTo("true");
        assertThat(sessionAttributes).containsEntry("serverURL", "https://openam.example.com:8443/openam");
    }

    /**
     * {@code checkPasswords} writes ADMIN_PWD only after a length and confirmation check. Before the
     * key was constrained, {@code ?actionLink=validateInput&key=ADMIN_PWD&value=x} set a one-character
     * admin password directly.
     */
    @Test
    public void validateInputRefusesToSetTheAdminPassword() throws Exception {
        callValidateInput("ADMIN_PWD", "x");

        assertThat(responseBody.toString()).isEqualTo(MISSING_FIELD);
        assertThat(sessionAttributes).isEmpty();
    }

    /** rootSuffix has its own validating action (validateRootSuffix); validateInput must not bypass it. */
    @Test
    public void validateInputRefusesAKeyThatHasItsOwnValidatingAction() throws Exception {
        callValidateInput("rootSuffix", "dc=evil,dc=com");

        assertThat(responseBody.toString()).isEqualTo(MISSING_FIELD);
        assertThat(sessionAttributes).isEmpty();
    }

    /** An absent key must be refused, not passed to the allow-list (Set.of rejects null with an NPE). */
    @Test
    public void validateInputWithoutAKeyStoresNothing() throws Exception {
        callValidateInput(null, "somevalue");

        assertThat(responseBody.toString()).isEqualTo(MISSING_FIELD);
        assertThat(sessionAttributes).isEmpty();
    }

    @Test
    public void validateInputWithoutAValueStoresNothing() throws Exception {
        callValidateInput("serverURL", null);

        assertThat(responseBody.toString()).isEqualTo(MISSING_FIELD);
        assertThat(sessionAttributes).isEmpty();
    }

    @Test
    public void dispatchesRegisteredActionLinkToAnnotatedHandler() throws Exception {
        when(request.getServletPath()).thenReturn("/config/wizard/step1.htm");
        when(request.getParameter("actionLink")).thenReturn("checkAdminPassword");
        when(request.getParameter("admin")).thenReturn("longenough1");
        when(request.getParameter("adminConfirm")).thenReturn("longenough1");

        servlet.service(request, response);

        assertThat(responseBody.toString()).isEqualTo("{\"valid\":\"true\",\"body\":\"OK\"}");
    }

    @Test
    public void unknownActionLinkOnRegisteredPageIsRejected() throws Exception {
        when(request.getServletPath()).thenReturn("/config/wizard/step1.htm");
        when(request.getParameter("actionLink")).thenReturn("notARealAction");

        servlet.service(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void unregisteredPathIsNotFound() throws Exception {
        // See the class Javadoc: this path is only "unregistered" from openam-core's own test
        // classpath, which never has the openam-upgrade ServiceLoader provider on it.
        when(request.getServletPath()).thenReturn("/config/upgrade/upgrade.htm");

        servlet.service(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    // --- HTTP-method guard ---------------------------------------------------------------------
    // Overriding service() bypasses HttpServlet's per-method dispatch (the old ClickServlet overrode
    // doGet/doPost, so anything else 405'd); the guard must reject other verbs before action dispatch.

    @Test
    public void rejectsPutBeforeDispatchingAnyAction() throws Exception {
        when(request.getMethod()).thenReturn("PUT");
        when(request.getServletPath()).thenReturn(ANY_PAGE);
        when(request.getParameter("actionLink")).thenReturn("validateInput");
        when(request.getParameter("key")).thenReturn("serverURL");
        when(request.getParameter("value")).thenReturn("https://openam.example.com/openam");

        servlet.service(request, response);

        verify(response).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        assertThat(sessionAttributes).isEmpty();
        assertThat(responseBody.toString()).isEmpty();
    }

    @Test
    public void rejectsDelete() throws Exception {
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getServletPath()).thenReturn(ANY_PAGE);

        servlet.service(request, response);

        verify(response).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void rejectsTrace() throws Exception {
        when(request.getMethod()).thenReturn("TRACE");
        when(request.getServletPath()).thenReturn(ANY_PAGE);

        servlet.service(request, response);

        verify(response).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void allowsPostForActionDispatch() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        callValidateInput("serverURL", "https://openam.example.com/openam");

        assertThat(sessionAttributes).containsEntry("serverURL", "https://openam.example.com/openam");
    }

    @Test
    public void allowsHeadForActionDispatch() throws Exception {
        when(request.getMethod()).thenReturn("HEAD");
        callValidateInput("serverURL", "https://openam.example.com/openam");

        assertThat(sessionAttributes).containsEntry("serverURL", "https://openam.example.com/openam");
    }

    // --- checkPasswords type handling ----------------------------------------------------------
    // checkPasswords is the only writer of ADMIN_PWD/AMLDAPUSERPASSWD (validateInput's allow-list
    // excludes them). An unrecognized/absent type must be refused, not defaulted to ADMIN_PWD.

    private void callCheckPasswords(String type) throws Exception {
        when(request.getServletPath()).thenReturn(ANY_PAGE);
        when(request.getParameter("actionLink")).thenReturn("checkPasswords");
        when(request.getParameter("password")).thenReturn("longenough1");
        when(request.getParameter("confirm")).thenReturn("longenough1");
        when(request.getParameter("type")).thenReturn(type);

        servlet.service(request, response);
    }

    @Test
    public void checkPasswordsStoresAdminPasswordForTypeAdmin() throws Exception {
        callCheckPasswords("admin");

        assertThat(sessionAttributes).containsEntry("ADMIN_PWD", "longenough1");
    }

    @Test
    public void checkPasswordsStoresAgentPasswordForTypeAgent() throws Exception {
        callCheckPasswords("agent");

        assertThat(sessionAttributes).containsEntry("AMLDAPUSERPASSWD", "longenough1");
        assertThat(sessionAttributes).doesNotContainKey("ADMIN_PWD");
    }

    @Test
    public void checkPasswordsRefusesAbsentType() throws Exception {
        callCheckPasswords(null);

        assertThat(sessionAttributes).isEmpty();
        assertThat(responseBody.toString()).isEqualTo(MISSING_FIELD);
    }

    @Test
    public void checkPasswordsRefusesUnknownType() throws Exception {
        callCheckPasswords("bogus");

        assertThat(sessionAttributes).isEmpty();
        assertThat(responseBody.toString()).isEqualTo(MISSING_FIELD);
    }
}
