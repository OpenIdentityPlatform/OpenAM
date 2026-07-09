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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.sun.identity.config.SessionAttributeNames;
import com.sun.identity.setup.EmbeddedOpenDS;
import com.sun.identity.setup.SetupConstants;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Per-page render smoke tests for {@link ConfiguratorServlet}: exercises the full
 * {@code service()} &rarr; {@code onSecurityCheck()} &rarr; {@code onInit()} &rarr;
 * {@code onGet()} &rarr; {@code render()} &rarr; {@code template.process()} chain against
 * the <em>real</em> FreeMarker templates in {@code openam-server-only}, with representative
 * session attributes.
 *
 * <p>FreeMarker's default undefined-variable semantics throw a {@code TemplateException} (which
 * wraps into a {@code ServletException}) for any bare {@code ${x}} where {@code x} is absent
 * from the data model. This catches regressions where a future {@code onInit()} change or
 * template edit silently drops a required model key.
 */
@PrepareForTest(EmbeddedOpenDS.class)
@PowerMockIgnore("jdk.internal.reflect.*")
public class ConfiguratorServletRenderSmokeTest extends PowerMockTestCase {

    private static final String TEMPLATE_BASE =
            "../openam-server-only/src/main/webapp/WEB-INF/templates/config";
    private static final String RESOURCE_PREFIX = "/WEB-INF/templates/config";

    private static final String STEP7_PATH = "/config/wizard/step7.htm";
    private static final String CONFIG_PORT = "50389";
    private static final String ADMIN_PORT = "4444";
    private static final String JMX_PORT = "1689";

    private ConfiguratorServlet servlet;
    private ServletContext servletContext;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter responseBody;
    private Map<String, Object> sessionAttributes;

    @BeforeMethod
    public void setUp() throws Exception {
        PowerMockito.mockStatic(EmbeddedOpenDS.class);
        when(EmbeddedOpenDS.isOpenDSVer1Installed()).thenReturn(false);

        servlet = new ConfiguratorServlet();

        servletContext = mock(ServletContext.class);
        mockAllTemplateResources();

        ServletConfig servletConfig = mock(ServletConfig.class);
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletConfig.getServletName()).thenReturn("configurator-servlet");
        servlet.init(servletConfig);

        sessionAttributes = new HashMap<>();

        HttpSession session = mock(HttpSession.class);
        doAnswer(inv -> sessionAttributes.put(inv.getArgument(0), inv.getArgument(1)))
                .when(session).setAttribute(anyString(), org.mockito.ArgumentMatchers.any());
        when(session.getAttribute(anyString())).thenAnswer(inv -> sessionAttributes.get(inv.getArgument(0)));
        doAnswer(inv -> sessionAttributes.remove(inv.getArgument(0)))
                .when(session).removeAttribute(anyString());

        request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(request.getServletContext()).thenReturn(servletContext);
        when(request.getContextPath()).thenReturn("/openam");
        when(request.getServletPath()).thenReturn("/config/wizard/step1.htm");
        when(request.getRequestURI()).thenReturn("/openam/config/wizard/step1.htm");
        when(request.getRequestURL())
                .thenReturn(new StringBuffer("http://localhost:8080/openam/config/wizard/step1.htm"));
        when(request.getParameter("locale")).thenReturn("en");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getScheme()).thenReturn("http");

        responseBody = new StringWriter();
        response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));
    }

    private void mockAllTemplateResources() throws IOException {
        String[] templatePaths = {
                "wizard/step1.ftl", "wizard/step2.ftl", "wizard/step3.ftl",
                "wizard/step4.ftl", "wizard/step5.ftl", "wizard/step6.ftl",
                "wizard/step7.ftl", "wizard/wizard.ftl",
                "options.ftl", "defaultSummary.ftl"
        };
        for (String tpl : templatePaths) {
            File file = new File(TEMPLATE_BASE, tpl);
            if (file.exists()) {
                URL url = file.toURI().toURL();
                String resourcePath = RESOURCE_PREFIX + "/" + tpl;
                when(servletContext.getResource(resourcePath)).thenReturn(url);
                when(servletContext.getResourceAsStream(resourcePath))
                        .thenAnswer(inv -> new FileInputStream(file));
            }
        }
    }

    @DataProvider(name = "pages")
    public Object[][] pages() {
        return new Object[][]{
                {"/config/wizard/step1.htm"},
                {"/config/wizard/step2.htm"},
                {"/config/wizard/step3.htm"},
                {"/config/wizard/step4.htm"},
                {"/config/wizard/step5.htm"},
                {"/config/wizard/step6.htm"},
                {"/config/wizard/step7.htm"},
                {"/config/wizard/wizard.htm"},
                {"/config/options.htm"},
                {"/config/defaultSummary.htm"},
        };
    }

    @Test(dataProvider = "pages")
    public void rendersPageWithoutException(String servletPath) throws Exception {
        populateSessionForPage(servletPath);

        when(request.getServletPath()).thenReturn(servletPath);

        servlet.service(request, response);

        assertThat(responseBody.toString())
                .as("Non-empty response for %s", servletPath)
                .isNotEmpty();
    }

    /**
     * {@code Step7.onInit()} adds {@code isEmbedded} only for an embedded config store, and
     * {@code step7.ftl} guards the admin/JMX port rows on it. The template originally tested
     * {@code embedded} - a name nothing ever adds - so these rows never rendered.
     */
    @Test
    public void step7SummaryShowsAdminAndJmxPortsWhenConfigStoreIsEmbedded() throws Exception {
        populateSessionForPage(STEP7_PATH);
        sessionAttributes.put(SetupConstants.CONFIG_VAR_DATA_STORE, SetupConstants.SMS_EMBED_DATASTORE);
        pinConfigStorePorts();

        when(request.getServletPath()).thenReturn(STEP7_PATH);
        servlet.service(request, response);

        assertThat(responseBody.toString())
                .contains("Admin Port")
                .contains("JMX Port")
                .contains(ADMIN_PORT)
                .contains(JMX_PORT);
    }

    /**
     * The ports stay in the session, so their absence proves the {@code isEmbedded} guard suppressed
     * them rather than the model simply lacking the keys.
     */
    @Test
    public void step7SummaryHidesAdminAndJmxPortsWhenConfigStoreIsExternal() throws Exception {
        populateSessionForPage(STEP7_PATH);
        sessionAttributes.put(SetupConstants.CONFIG_VAR_DATA_STORE, SetupConstants.SMS_DS_DATASTORE);
        pinConfigStorePorts();

        when(request.getServletPath()).thenReturn(STEP7_PATH);
        servlet.service(request, response);

        assertThat(responseBody.toString())
                .doesNotContain("Admin Port")
                .doesNotContain("JMX Port")
                .doesNotContain(ADMIN_PORT)
                .doesNotContain(JMX_PORT);
    }

    /** Pins otherwise free-port-derived values so the assertions above can match exact strings. */
    private void pinConfigStorePorts() {
        sessionAttributes.put("configStorePort", CONFIG_PORT);
        sessionAttributes.put("configStoreAdminPort", ADMIN_PORT);
        sessionAttributes.put("configStoreJmxPort", JMX_PORT);
    }

    private void populateSessionForPage(String servletPath) {
        switch (servletPath) {

            case "/config/wizard/step2.htm":
                sessionAttributes.put(SessionAttributeNames.SERVER_URL,
                        "https://openam.example.com:8443");
                sessionAttributes.put(SessionAttributeNames.COOKIE_DOMAIN,
                        ".example.com");
                sessionAttributes.put(SessionAttributeNames.PLATFORM_LOCALE, "en");
                sessionAttributes.put(SessionAttributeNames.CONFIG_DIR,
                        "/home/openam/config");
                break;

            case "/config/wizard/step3.htm":
                sessionAttributes.put(SetupConstants.CONFIG_VAR_DATA_STORE,
                        SetupConstants.SMS_EMBED_DATASTORE);
                sessionAttributes.put("configStoreHost", "localhost");
                sessionAttributes.put("configStoreSSL", "SIMPLE");
                break;

            case "/config/wizard/step4.htm":
                sessionAttributes.put(SetupConstants.CONFIG_VAR_DATA_STORE,
                        SetupConstants.SMS_EMBED_DATASTORE);
                sessionAttributes.put(SessionAttributeNames.EXT_DATA_STORE, "false");
                break;

            case "/config/wizard/step7.htm":
                sessionAttributes.put(SessionAttributeNames.EXT_DATA_STORE, "false");
                sessionAttributes.put("configStoreHost", "localhost");
                sessionAttributes.put("configStoreSSL", "SIMPLE");
                sessionAttributes.put(SessionAttributeNames.CONFIG_DIR,
                        "/home/openam/config");
                break;

            default:
                break;
        }
    }
}
