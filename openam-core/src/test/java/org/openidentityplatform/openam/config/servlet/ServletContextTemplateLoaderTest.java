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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletContext;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.testng.annotations.Test;

/**
 * Covers the FreeMarker rendering mechanism {@link ConfiguratorServlet} builds on: loading a
 * {@code .ftl} through a jakarta {@code ServletContext} (which FreeMarker's own
 * {@code WebappTemplateLoader} cannot do - see docs/migration/click-to-freemarker), then merging
 * it with a page-like model (localized strings via method call, {@code context}/{@code path}
 * variables).
 */
public class ServletContextTemplateLoaderTest {

    private static final String RESOURCE_PATH = "/WEB-INF/templates/config/greeting.ftl";
    private static final String CLASSPATH_FIXTURE = "/test-templates/greeting.ftl";

    /** Duck-typed page stand-in: FreeMarker's default object wrapper resolves ${page.foo()} via
     *  reflection, so this does not need to be a real SetupPage. */
    public static class StubPage {
        public String getLocalizedString(String key) {
            return "greeting".equals(key) ? "Hello" : key;
        }
    }

    @Test
    public void loadsAndRendersTemplateThroughServletContext() throws Exception {
        URL resourceUrl = getClass().getResource(CLASSPATH_FIXTURE);
        assertThat(resourceUrl).isNotNull();

        ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getResource(RESOURCE_PATH)).thenReturn(resourceUrl);
        when(servletContext.getResourceAsStream(RESOURCE_PATH))
                .thenAnswer(inv -> getClass().getResourceAsStream(CLASSPATH_FIXTURE));

        Configuration config = new Configuration(Configuration.VERSION_2_3_31);
        config.setTemplateLoader(new ServletContextTemplateLoader(servletContext, "/WEB-INF/templates/config"));
        config.setDefaultEncoding("UTF-8");

        Template template = config.getTemplate("greeting.ftl");

        Map<String, Object> model = new HashMap<>();
        model.put("page", new StubPage());
        model.put("name", "World");
        model.put("context", "/openam");
        model.put("path", "/config/wizard/step1.htm");

        StringWriter out = new StringWriter();
        template.process(model, out);

        assertThat(out.toString().trim()).isEqualTo("Hello World, from /openam/config/wizard/step1.htm");
    }

    @Test
    public void findTemplateSourceReturnsNullWhenResourceIsMissing() throws Exception {
        ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getResource("/WEB-INF/templates/config/missing.ftl")).thenReturn(null);

        ServletContextTemplateLoader loader =
                new ServletContextTemplateLoader(servletContext, "/WEB-INF/templates/config");

        assertThat(loader.findTemplateSource("missing.ftl")).isNull();
    }
}
