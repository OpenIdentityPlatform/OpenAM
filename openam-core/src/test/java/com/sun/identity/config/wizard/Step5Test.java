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
package com.sun.identity.config.wizard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.sun.identity.config.SessionAttributeNames;
import org.openidentityplatform.openam.config.servlet.ConfiguratorContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Byte-exact response and session side-effect coverage for the migrated Step5 page, matching
 * the old Click AjaxPage/ProtectedPage/Step5 behavior these handlers were ported from.
 */
public class Step5Test {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter responseBody;
    private Map<String, Object> sessionAttributes;
    private Step5 step5;

    @BeforeMethod
    public void setup() throws Exception {
        sessionAttributes = new HashMap<>();

        HttpSession session = mock(HttpSession.class);
        doAnswer(inv -> sessionAttributes.put(inv.getArgument(0), inv.getArgument(1)))
                .when(session).setAttribute(anyString(), any());
        when(session.getAttribute(anyString())).thenAnswer(inv -> sessionAttributes.get(inv.getArgument(0)));
        doAnswer(inv -> sessionAttributes.remove(inv.getArgument(0)))
                .when(session).removeAttribute(anyString());

        request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(request.getParameter("locale")).thenReturn("en");

        responseBody = new StringWriter();
        response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        step5 = new Step5();
        step5.setContext(new ConfiguratorContext(request, response));
    }

    private void param(String name, String value) {
        when(request.getParameter(name)).thenReturn(value);
    }

    @Test
    public void clearRemovesSiteAttributesAndWritesNoResponseBody() {
        sessionAttributes.put(SessionAttributeNames.LB_SITE_NAME, "site1");
        sessionAttributes.put(SessionAttributeNames.LB_PRIMARY_URL, "http://site1.example.com/openam");

        step5.clear();

        assertThat(responseBody.toString()).isEmpty();
        assertThat(sessionAttributes).doesNotContainKeys(
                SessionAttributeNames.LB_SITE_NAME, SessionAttributeNames.LB_PRIMARY_URL);
    }

    @Test
    public void validateSiteRejectsMissingName() {
        boolean invalid = step5.validateSite();

        assertThat(invalid).isTrue();
        assertThat(responseBody.toString()).isEqualTo("{\"valid\":\"false\", \"body\":\"Site Name Missing\"}");
        assertThat(sessionAttributes).isEmpty();
    }

    @Test
    public void validateSiteAcceptsName() {
        param("host", "site1");

        boolean invalid = step5.validateSite();

        assertThat(invalid).isFalse();
        assertThat(responseBody.toString()).isEqualTo("{\"valid\":\"true\", \"body\":\"ok.label\"}");
        assertThat(sessionAttributes.get(SessionAttributeNames.LB_SITE_NAME)).isEqualTo("site1");
    }

    @Test
    public void validateURLRejectsMissingValue() {
        boolean invalid = step5.validateURL();

        assertThat(invalid).isTrue();
        assertThat(responseBody.toString()).isEqualTo("Missing Primary URL");
        assertThat(sessionAttributes).isEmpty();
    }

    @Test
    public void validateURLRejectsMalformedURL() {
        param("port", "not-a-url");

        boolean invalid = step5.validateURL();

        assertThat(invalid).isTrue();
        assertThat(responseBody.toString()).isEqualTo("Primary URL is not valid");
        assertThat(sessionAttributes).isEmpty();
    }

    @Test
    public void validateURLRejectsMissingPath() {
        param("port", "http://site1.example.com");

        boolean invalid = step5.validateURL();

        assertThat(invalid).isTrue();
        assertThat(responseBody.toString()).isEqualTo("primary.url.no.uri");
        assertThat(sessionAttributes).isEmpty();
    }

    @Test
    public void validateURLAcceptsWellFormedURL() {
        param("port", "http://site1.example.com/openam");

        boolean invalid = step5.validateURL();

        assertThat(invalid).isFalse();
        assertThat(responseBody.toString()).isEqualTo("ok");
        assertThat(sessionAttributes.get(SessionAttributeNames.LB_PRIMARY_URL))
                .isEqualTo("http://site1.example.com/openam");
    }
}
