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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.sun.identity.setup.SetupConstants;
import org.openidentityplatform.openam.config.servlet.ConfiguratorContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Model-building coverage for the migrated Step7 (summary) page, matching the old Click
 * ProtectedPage/Step7 behavior this was ported from. Step7 has no {@code ActionLink}/{@code
 * @ConfiguratorAction} handlers at all - step7.htm never posts back to it - so, unlike every
 * other migrated step, there is nothing to dispatch: coverage here is entirely about the {@code
 * onInit()} model built for step7.ftl, exercised by calling it directly and inspecting {@code
 * getModel()}.
 *
 * <p>Every {@code onInit()} call unconditionally computes {@code getAvailablePort(50389/4444/1689)}
 * for the config-store port defaults, even when a session value already exists for that key
 * (Java evaluates the {@code getAttribute(key, default)} default argument eagerly) - this binds
 * real local sockets (see {@code AMSetupUtils.getFirstUnusedPort}), same as the original Click
 * page. {@code request.getServerName()} is stubbed to {@code "localhost"} below purely to make
 * that pre-existing side effect deterministic; see
 * docs/migration/click-to-freemarker/04-implementation-notes.md.
 *
 * <p>The already-configured branch of {@code onSecurityCheck} isn't covered here either, same
 * disproportionate-effort call as every other step since increment 1's Step1Test.
 */
public class Step7Test {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private Map<String, Object> sessionAttributes;
    private Step7 step7;

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
        when(request.getServerName()).thenReturn("localhost");

        StringWriter responseBody = new StringWriter();
        response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        step7 = new Step7();
        step7.setContext(new ConfiguratorContext(request, response));
    }

    @Test
    public void onInitSummarizesEmbeddedConfigStoreWithDefaultUserStore() {
        // Realistic navigation order: the user already visited Step4, which always sets
        // EXT_DATA_STORE before Step7 can be reached - see the dedicated NPE test below.
        sessionAttributes.put(SessionAttributeNames.EXT_DATA_STORE, "false");

        step7.onInit();

        Map<String, Object> model = step7.getModel();
        assertThat(model.get("isEmbedded")).isEqualTo("1");
        // step7.htm/step7.ftl's embedded-only rows guard on "$embedded"/"embedded", a key Step7
        // never populates (it only ever sets "isEmbedded") - a pre-existing template/model key
        // mismatch, not introduced by this port. See the implementation notes.
        assertThat(model).doesNotContainKey("embedded");
        assertThat(model.get("configStoreHost")).isEqualTo("localhost");
        assertThat(model.get("displayConfigStoreSSL")).isEqualTo("No");
        assertThat(model.get("rootSuffix")).isEqualTo(Wizard.defaultRootSuffix);
        assertThat(model.get("configStoreLoginId")).isEqualTo(Wizard.defaultUserName);
        assertThat(model.get("firstInstance")).isEqualTo("1");
        assertThat(model).doesNotContainKey("configDirectory");
        assertThat(model).doesNotContainKey("displayUserHostName");
        assertThat(model).doesNotContainKey("loadBalancerHost");
        assertThat(model).doesNotContainKey("loadBalancerPort");
    }

    @Test
    public void onInitSummarizesExternalConfigStoreAndSslEnabled() {
        sessionAttributes.put(SessionAttributeNames.CONFIG_VAR_DATA_STORE, SetupConstants.SMS_DS_DATASTORE);
        sessionAttributes.put("configStoreHost", "ext-config.example.com");
        sessionAttributes.put("configStoreSSL", "SSL");
        sessionAttributes.put(SessionAttributeNames.CONFIG_DIR, "/opt/openam/config");
        sessionAttributes.put(SessionAttributeNames.EXT_DATA_STORE, "false");

        step7.onInit();

        Map<String, Object> model = step7.getModel();
        assertThat(model).doesNotContainKey("isEmbedded");
        assertThat(model.get("configStoreHost")).isEqualTo("ext-config.example.com");
        assertThat(model.get("displayConfigStoreSSL")).isEqualTo("Yes");
        assertThat(model.get("configDirectory")).isEqualTo("/opt/openam/config");
    }

    @Test
    public void onInitOmitsUserStoreSummaryWhenEmbeddedReplicationFlagSet() {
        sessionAttributes.put(SetupConstants.DS_EMB_REPL_FLAG, SetupConstants.DS_EMP_REPL_FLAG_VAL);

        step7.onInit();

        Map<String, Object> model = step7.getModel();
        assertThat(model).doesNotContainKey("firstInstance");
        assertThat(model).doesNotContainKey("displayUserHostName");
    }

    @Test
    public void onInitSummarizesExternalUserStore() {
        sessionAttributes.put(SessionAttributeNames.EXT_DATA_STORE, "true");
        sessionAttributes.put(SessionAttributeNames.USER_STORE_HOST, "userstore.example.com");
        sessionAttributes.put(SessionAttributeNames.USER_STORE_SSL, "SSL");
        sessionAttributes.put(SessionAttributeNames.USER_STORE_PORT, "1389");
        sessionAttributes.put(SessionAttributeNames.USER_STORE_ROOT_SUFFIX, "dc=example,dc=com");
        sessionAttributes.put(SessionAttributeNames.USER_STORE_LOGIN_ID, "cn=admin");
        sessionAttributes.put(SessionAttributeNames.USER_STORE_TYPE, "LDAPv3ForAD");

        step7.onInit();

        Map<String, Object> model = step7.getModel();
        assertThat(model.get("firstInstance")).isEqualTo("1");
        assertThat(model.get("displayUserHostName")).isEqualTo("userstore.example.com");
        assertThat(model.get("xuserHostSSL")).isEqualTo("Yes");
        assertThat(model.get("userHostPort")).isEqualTo("1389");
        assertThat(model.get("userRootSuffix")).isEqualTo("dc=example,dc=com");
        assertThat(model.get("userLoginID")).isEqualTo("cn=admin");
        assertThat(model.get("userStoreType")).isEqualTo("Active Directory with Host and Port");
    }

    @Test
    public void onInitSummarizesLoadBalancerWhenConfigured() {
        sessionAttributes.put(SessionAttributeNames.EXT_DATA_STORE, "false");
        sessionAttributes.put(SessionAttributeNames.LB_SITE_NAME, "site1");
        sessionAttributes.put(SessionAttributeNames.LB_PRIMARY_URL, "https://lb.example.com:8443");

        step7.onInit();

        Map<String, Object> model = step7.getModel();
        assertThat(model.get("loadBalancerHost")).isEqualTo("site1");
        assertThat(model.get("loadBalancerPort")).isEqualTo("https://lb.example.com:8443");
    }

    @Test
    public void onInitThrowsIfExtDataStoreNeverSet() {
        // Pre-existing behavior, not introduced by this port: EXT_DATA_STORE is only ever written
        // by Step4.onInit(). If Step7 is somehow rendered before that has happened once for this
        // session, `tmp.equals("true")` NPEs on the still-null session attribute. In real usage the
        // wizard shell only lazy-loads step7.htm when the user reaches tab 7, by which point every
        // earlier tab (including 4) has already run onInit() at least once - so this isn't reachable
        // through normal next-button navigation. Locked in here so it isn't silently "fixed" as a
        // side effect of some future change. See the implementation notes.
        assertThatThrownBy(() -> step7.onInit()).isInstanceOf(NullPointerException.class);
    }
}
