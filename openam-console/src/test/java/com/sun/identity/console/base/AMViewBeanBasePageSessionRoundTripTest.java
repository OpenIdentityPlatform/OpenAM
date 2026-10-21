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
package com.sun.identity.console.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManagerTestSupport;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.shared.Constants;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.forgerock.openam.utils.IOUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Round-trips the admin-console page session through both places that read it back from a request, so that
 * neither can fall back to the default {@code maxcollectiondepth} of {@link IOUtils#deserialise} unnoticed.
 * The console page session nests page-session map -&gt; attribute-value map -&gt; value set, one level past
 * the default limit; a read with the default depth rejects it and the page silently loses its state.
 * <ul>
 *     <li>{@code AMViewBeanBase.handlePageSessionThruURL}: the {@code dsame.pgSession} URL parameter written
 *     by {@code appendPgSession}, read on {@link AMViewBeanBase#setRequestContext}.</li>
 *     <li>{@code ConsoleViewBeanBase.deserializePageAttributes}: the {@code jato.pageSession} form field written
 *     from {@code getPageSessionAttributeString()}, read on the first page-session access.</li>
 * </ul>
 * Each value is produced by a console view bean through the production encoder, so the test sends back
 * exactly what the browser would.
 */
public class AMViewBeanBasePageSessionRoundTripTest {

    private static final String PAGE_SESSION_KEY = "propertyAttributes";
    private static final String URL_PAGE_SESSION_PARAMETER = "dsame.pgSession";
    private static final String FORM_PAGE_SESSION_PARAMETER = "jato.pageSession";

    @BeforeClass
    public void provideServerAddress() {
        // AMViewBeanBase's static initialiser copies the server address into system properties and fails
        // without one; outside a deployment nothing supplies it, so give it a placeholder.
        SystemProperties.initializeProperties(Constants.AM_SERVER_PROTOCOL, "http");
        SystemProperties.initializeProperties(Constants.AM_SERVER_HOST, "localhost");
        SystemProperties.initializeProperties(Constants.AM_SERVER_PORT, "8080");
    }

    @AfterMethod(alwaysRun = true)
    public void clearRequestContext() {
        RequestManagerTestSupport.install(null);
    }

    @Test
    public void shouldRestorePageSessionPassedThroughUrl() {
        // Given: a console page that links to another page, carrying its page session in the URL.
        HashMap<String, Set<String>> attributeValues = consoleAttributeValues();
        MinimalConsoleViewBean source = beanInRequest(new HashMap<String, String>());
        source.setPageSessionAttribute(PAGE_SESSION_KEY, attributeValues);
        String url = source.appendPgSession("Target");
        String prefix = URL_PAGE_SESSION_PARAMETER + "=";
        String pageSession = url.substring(url.indexOf(prefix) + prefix.length());

        // When: the browser follows the link.
        Map<String, String> parameters = new HashMap<>();
        parameters.put(URL_PAGE_SESSION_PARAMETER, pageSession);
        MinimalConsoleViewBean target = beanInRequest(parameters);

        // Then: with the default depth the value would be rejected and missing here.
        assertThat(target.getPageSessionAttribute(PAGE_SESSION_KEY)).isEqualTo(attributeValues);
    }

    @Test
    public void shouldRestorePageSessionPostedBackInForm() {
        // Given: a console page rendered with its page session in the jato.pageSession form field.
        HashMap<String, Set<String>> attributeValues = consoleAttributeValues();
        MinimalConsoleViewBean source = beanInRequest(new HashMap<String, String>());
        source.setPageSessionAttribute(PAGE_SESSION_KEY, attributeValues);
        String pageSession = source.getPageSessionAttributeString();

        // When: the browser submits the form.
        Map<String, String> parameters = new HashMap<>();
        parameters.put(FORM_PAGE_SESSION_PARAMETER, pageSession);
        MinimalConsoleViewBean target = beanInRequest(parameters);

        // Then: with the default depth the value would be rejected and missing here.
        assertThat(target.getPageSessionAttribute(PAGE_SESSION_KEY)).isEqualTo(attributeValues);
    }

    /** The attribute-value map a console property sheet keeps in the page session. */
    private static HashMap<String, Set<String>> consoleAttributeValues() {
        Set<String> values = new HashSet<>();
        values.add("value");
        HashMap<String, Set<String>> attributeValues = new HashMap<>();
        attributeValues.put("attribute", values);
        return attributeValues;
    }

    /**
     * Starts a new request carrying {@code parameters}, as JATO does: the context is installed for the thread
     * and handed to a fresh view bean.
     */
    private static MinimalConsoleViewBean beanInRequest(Map<String, String> parameters) {
        RequestContext context = requestContext(parameters);
        RequestManagerTestSupport.install(context);
        MinimalConsoleViewBean bean = new MinimalConsoleViewBean();
        bean.setRequestContext(context);
        return bean;
    }

    private static RequestContext requestContext(Map<String, String> parameters) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenAnswer(invocation -> parameters.get(invocation.getArgument(0)));
        // JATO records "page session already read" as a request attribute, so the mock has to keep attributes.
        Map<String, Object> attributes = new HashMap<>();
        when(request.getAttribute(anyString())).thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));
        doAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(request).setAttribute(anyString(), any());

        RequestContext context = mock(RequestContext.class);
        when(context.getRequest()).thenReturn(request);
        when(context.getServletContext()).thenReturn(mock(ServletContext.class));
        return context;
    }

    /**
     * The smallest concrete console view bean. It overrides nothing on the page-session path, so what is
     * observed is what every real console page inherits from {@link AMViewBeanBase}.
     */
    private static final class MinimalConsoleViewBean extends AMViewBeanBase {

        MinimalConsoleViewBean() {
            super("MinimalConsoleViewBean");
        }

        @Override
        protected AMModel getModelInternal() {
            return null;
        }
    }
}
