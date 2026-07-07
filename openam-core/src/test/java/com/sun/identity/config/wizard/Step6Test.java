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
 * Byte-exact response and session side-effect coverage for the migrated Step6 page, matching
 * the old Click AjaxPage/ProtectedPage/Step6 behavior these handlers were ported from.
 *
 * <p>{@code checkAgentPassword} is not the live handler step6.htm/step6.ftl actually posts to
 * (that's the base {@code checkPasswords}, exercised via {@code Step1Test}/{@code
 * ConfiguratorServletTest} - same base method, same behavior) - see
 * docs/migration/click-to-freemarker/04-implementation-notes.md. It is still covered here since
 * it stays a live, annotated, web-invokable endpoint for URL-preservation parity with the old
 * Click page.
 */
public class Step6Test {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter responseBody;
    private Map<String, Object> sessionAttributes;
    private Step6 step6;

    @BeforeMethod
    public void setup() throws Exception {
        sessionAttributes = new HashMap<>();

        HttpSession session = mock(HttpSession.class);
        doAnswer(inv -> sessionAttributes.put(inv.getArgument(0), inv.getArgument(1)))
                .when(session).setAttribute(anyString(), any());
        when(session.getAttribute(anyString())).thenAnswer(inv -> sessionAttributes.get(inv.getArgument(0)));

        request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(request.getParameter("locale")).thenReturn("en");

        responseBody = new StringWriter();
        response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        step6 = new Step6();
        step6.setContext(new ConfiguratorContext(request, response));
    }

    private void param(String name, String value) {
        when(request.getParameter(name)).thenReturn(value);
    }

    @Test
    public void checkAgentPasswordRejectsMissingFields() {
        step6.checkAgentPassword();

        assertThat(responseBody.toString())
                .isEqualTo("{\"valid\":\"false\", \"body\":\"Missing Required Field\"}");
        assertThat(sessionAttributes).isEmpty();
    }

    @Test
    public void checkAgentPasswordRejectsSameAsAdminPassword() {
        sessionAttributes.put(SessionAttributeNames.CONFIG_VAR_ADMIN_PWD, "sharedpassword1");
        param("agent", "sharedpassword1");
        param("agentConfirm", "sharedpassword1");

        step6.checkAgentPassword();

        assertThat(responseBody.toString())
                .isEqualTo("{\"valid\":\"false\", \"body\":\"Agent password is same as Admin Password\"}");
        assertThat(sessionAttributes.get(SessionAttributeNames.CONFIG_VAR_AMLDAPUSERPASSWD)).isNull();
    }

    @Test
    public void checkAgentPasswordRejectsShortPassword() {
        param("agent", "short");
        param("agentConfirm", "short");

        step6.checkAgentPassword();

        assertThat(responseBody.toString())
                .isEqualTo("{\"valid\":\"false\", \"body\":\"Password must be at least 8 characters\"}");
        assertThat(sessionAttributes).isEmpty();
    }

    @Test
    public void checkAgentPasswordRejectsMismatch() {
        param("agent", "longenough1");
        param("agentConfirm", "different1");

        step6.checkAgentPassword();

        assertThat(responseBody.toString())
                .isEqualTo("{\"valid\":\"false\", \"body\":\"Passwords do not match\"}");
        assertThat(sessionAttributes).isEmpty();
    }

    @Test
    public void checkAgentPasswordAcceptsDistinctPassword() {
        sessionAttributes.put(SessionAttributeNames.CONFIG_VAR_ADMIN_PWD, "adminpassword1");
        param("agent", "agentpassword1");
        param("agentConfirm", "agentpassword1");

        step6.checkAgentPassword();

        assertThat(responseBody.toString()).isEqualTo("{\"valid\":\"true\", \"body\":\"OK\"}");
        assertThat(sessionAttributes.get(SessionAttributeNames.CONFIG_VAR_AMLDAPUSERPASSWD))
                .isEqualTo("agentpassword1");
    }
}
