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
 * Byte-exact response and session side-effect coverage for the migrated Step4 page, matching
 * the old Click AjaxPage/ProtectedPage/Step4 behavior these handlers were ported from.
 *
 * <p>{@code validateUMHost}/{@code validateUMDomainName} are not covered here: both require a
 * live LDAP (and, for the AD path, DNS SRV) connection, which would make this suite slow and
 * environment-dependent - the same disproportionate-effort call already made for the
 * already-configured branch of {@code onSecurityCheck} in increment 1's Step1Test. See
 * docs/migration/click-to-freemarker/04-implementation-notes.md.
 */
public class Step4Test {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter responseBody;
    private Map<String, Object> sessionAttributes;
    private Step4 step4;

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

        step4 = new Step4();
        step4.setContext(new ConfiguratorContext(request, response));
    }

    private void param(String name, String value) {
        when(request.getParameter(name)).thenReturn(value);
    }

    @Test
    public void setSSLDefaultsToSimpleWhenMissing() {
        step4.setSSL();

        assertThat(responseBody.toString()).isEqualTo("ok");
        assertThat(sessionAttributes.get(SessionAttributeNames.USER_STORE_SSL)).isEqualTo("SIMPLE");
    }

    @Test
    public void setSSLAcceptsExplicitValue() {
        param("ssl", "SSL");

        step4.setSSL();

        assertThat(responseBody.toString()).isEqualTo("ok");
        assertThat(sessionAttributes.get(SessionAttributeNames.USER_STORE_SSL)).isEqualTo("SSL");
    }

    @Test
    public void setDomainNameRejectsMissingValue() {
        step4.setDomainName();

        assertThat(responseBody.toString()).isEqualTo("Missing Domain Name");
        assertThat(sessionAttributes).isEmpty();
    }

    @Test
    public void setDomainNameAcceptsValue() {
        param("domainname", "corp.example.com");

        step4.setDomainName();

        assertThat(responseBody.toString()).isEqualTo("ok");
        assertThat(sessionAttributes.get(SessionAttributeNames.USER_STORE_DOMAINNAME)).isEqualTo("corp.example.com");
        assertThat(sessionAttributes.get(SessionAttributeNames.EXT_DATA_STORE)).isEqualTo("true");
    }

    @Test
    public void setHostRejectsMissingValue() {
        step4.setHost();

        assertThat(responseBody.toString()).isEqualTo("Missing Host Name");
        assertThat(sessionAttributes).isEmpty();
    }

    @Test
    public void setHostAcceptsValue() {
        param("host", "ds.example.com");

        step4.setHost();

        assertThat(responseBody.toString()).isEqualTo("ok");
        assertThat(sessionAttributes.get(SessionAttributeNames.USER_STORE_HOST)).isEqualTo("ds.example.com");
    }

    @Test
    public void setUMEmbeddedWritesNoResponseBody() {
        step4.setUMEmbedded();

        assertThat(responseBody.toString()).isEmpty();
        assertThat(sessionAttributes.get(SessionAttributeNames.EXT_DATA_STORE)).isEqualTo("false");
    }

    @Test
    public void resetUMEmbeddedWritesNoResponseBody() {
        step4.resetUMEmbedded();

        assertThat(responseBody.toString()).isEmpty();
        assertThat(sessionAttributes.get(SessionAttributeNames.EXT_DATA_STORE)).isEqualTo("true");
    }

    @Test
    public void setPortRejectsMissingValue() {
        step4.setPort();

        assertThat(responseBody.toString()).isEqualTo("Missing Port Number");
        assertThat(sessionAttributes).isEmpty();
    }

    @Test
    public void setPortRejectsOutOfRangeValue() {
        param("port", "70000");

        step4.setPort();

        assertThat(responseBody.toString())
                .isEqualTo("Port must be greater than or equal to 1 and less than or equal to 65535");
        assertThat(sessionAttributes).isEmpty();
    }

    @Test
    public void setPortAcceptsValidValue() {
        param("port", "1389");

        step4.setPort();

        assertThat(responseBody.toString()).isEqualTo("ok");
        assertThat(sessionAttributes.get(SessionAttributeNames.USER_STORE_PORT)).isEqualTo("1389");
    }

    @Test
    public void setLoginIDRejectsMissingValue() {
        step4.setLoginID();

        assertThat(responseBody.toString()).isEqualTo("Missing Login ID");
        assertThat(sessionAttributes).isEmpty();
    }

    @Test
    public void setLoginIDAcceptsValue() {
        param("dn", "cn=Directory Manager");

        step4.setLoginID();

        assertThat(responseBody.toString()).isEqualTo("ok");
        assertThat(sessionAttributes.get(SessionAttributeNames.USER_STORE_LOGIN_ID)).isEqualTo("cn=Directory Manager");
    }

    @Test
    public void setPasswordRejectsMissingValue() {
        step4.setPassword();

        assertThat(responseBody.toString()).isEqualTo("Missing Password");
        assertThat(sessionAttributes).isEmpty();
    }

    @Test
    public void setPasswordAcceptsValue() {
        param("password", "secret123");

        step4.setPassword();

        assertThat(responseBody.toString()).isEqualTo("ok");
        assertThat(sessionAttributes.get(SessionAttributeNames.USER_STORE_LOGIN_PWD)).isEqualTo("secret123");
    }

    @Test
    public void setRootSuffixRejectsMissingValue() {
        step4.setRootSuffix();

        assertThat(responseBody.toString()).isEqualTo("Missing Root Suffix");
        assertThat(sessionAttributes).isEmpty();
    }

    @Test
    public void setRootSuffixRejectsInvalidDN() {
        param("rootsuffix", "not a dn");

        step4.setRootSuffix();

        assertThat(responseBody.toString()).isEqualTo("Invalid Distinguished Name");
        assertThat(sessionAttributes).isEmpty();
    }

    @Test
    public void setRootSuffixAcceptsValidDN() {
        param("rootsuffix", "dc=example,dc=com");

        step4.setRootSuffix();

        assertThat(responseBody.toString()).isEqualTo("ok");
        assertThat(sessionAttributes.get(SessionAttributeNames.USER_STORE_ROOT_SUFFIX)).isEqualTo("dc=example,dc=com");
    }

    @Test
    public void setStoreTypeWritesRawResponseStringWithoutLocalization() {
        param("type", "LDAPv3ForAD");

        step4.setStoreType();

        assertThat(responseBody.toString()).isEqualTo("ok");
        assertThat(sessionAttributes.get(SessionAttributeNames.USER_STORE_TYPE)).isEqualTo("LDAPv3ForAD");
    }

    @Test
    public void setStoreTypeIgnoresMissingType() {
        step4.setStoreType();

        assertThat(responseBody.toString()).isEqualTo("ok");
        assertThat(sessionAttributes).isEmpty();
    }
}
