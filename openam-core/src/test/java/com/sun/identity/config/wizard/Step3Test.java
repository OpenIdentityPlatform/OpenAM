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
import com.sun.identity.config.pojos.LDAPStore;
import com.sun.identity.setup.SetupConstants;
import org.openidentityplatform.openam.config.servlet.ConfiguratorContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Byte-exact response and session side-effect coverage for the migrated Step3 page and its base
 * {@link LDAPStoreWizardPage}, matching the old Click AjaxPage/ProtectedPage/LDAPStoreWizardPage/
 * Step3 behavior these handlers were ported from.
 *
 * <p>{@code validateHostName}/{@code validateSMHost} are not covered here: both require live
 * network access (a remote-server HTTP call and an LDAP connection, respectively), the same
 * disproportionate-effort call already made for the LDAP-dependent handlers skipped in Step4Test.
 * The "resolvable host" branch of {@code validateConfigStoreHost} is exercised with {@code
 * localhost} (resolvable without real network); the {@code UnknownHostException} branch is not,
 * since DNS behavior for a deliberately-invalid hostname is not reliably reproducible across
 * environments. See docs/migration/click-to-freemarker/04-implementation-notes.md.
 */
public class Step3Test {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter responseBody;
    private Map<String, Object> sessionAttributes;
    private Step3 step3;

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

        step3 = new Step3();
        step3.setContext(new ConfiguratorContext(request, response));
    }

    private void param(String name, String value) {
        when(request.getParameter(name)).thenReturn(value);
    }

    // --- LDAPStoreWizardPage.clearStore() -----------------------------------------------------

    @Test
    public void clearStoreRemovesSessionAttributeAndWritesNoResponseBody() {
        sessionAttributes.put("customConfigStore", new LDAPStore());

        step3.clearStore();

        assertThat(responseBody.toString()).isEmpty();
        assertThat(sessionAttributes).doesNotContainKey("customConfigStore");
    }

    // --- setConfigType() / setReplication() ---------------------------------------------------
    // NOTE: the old Click ActionLinks returned `true` here with no setPath(null)/writeToResponse
    // call; ConfiguratorServlet's actionLink dispatch never renders regardless of the handler's
    // return value, and nothing reads this fire-and-forget call's response body either way - see
    // the NOTE in Step3.setConfigType() and 04-implementation-notes.md. Only the session
    // side-effects are asserted here.

    @Test
    public void setConfigTypeEmbeddedDefaultsHostToLocalhost() {
        param("type", "embedded");

        step3.setConfigType();

        assertThat(responseBody.toString()).isEmpty();
        assertThat(sessionAttributes.get(SessionAttributeNames.CONFIG_STORE_HOST)).isEqualTo("localhost");
        assertThat(sessionAttributes.get(SessionAttributeNames.CONFIG_VAR_DATA_STORE))
                .isEqualTo(SetupConstants.SMS_EMBED_DATASTORE);
    }

    @Test
    public void setConfigTypeRemoteLeavesHostUntouched() {
        param("type", "remote");

        step3.setConfigType();

        assertThat(sessionAttributes).doesNotContainKey(SessionAttributeNames.CONFIG_STORE_HOST);
        assertThat(sessionAttributes.get(SessionAttributeNames.CONFIG_VAR_DATA_STORE))
                .isEqualTo(SetupConstants.SMS_DS_DATASTORE);
    }

    @Test
    public void setReplicationEnableSetsReplicationFlag() {
        param("multi", "enable");

        step3.setReplication();

        assertThat(responseBody.toString()).isEmpty();
        assertThat(sessionAttributes.get(SetupConstants.DS_EMB_REPL_FLAG))
                .isEqualTo(SetupConstants.DS_EMP_REPL_FLAG_VAL);
    }

    @Test
    public void setReplicationOtherValuePassesThroughRaw() {
        param("multi", "disable");

        step3.setReplication();

        assertThat(sessionAttributes.get(SetupConstants.DS_EMB_REPL_FLAG)).isEqualTo("disable");
    }

    // --- validateRootSuffix() ------------------------------------------------------------------

    @Test
    public void validateRootSuffixRejectsMissingValue() {
        step3.validateRootSuffix();

        // Pre-existing quirk ported verbatim: the missing-value branch doesn't return early, so
        // execution falls through into the isDN() check too, and both messages get written to the
        // response back-to-back. See Step3.validateRootSuffix().
        assertThat(responseBody.toString()).isEqualTo("Missing Required FieldInvalid Distinguished Name");
        assertThat(sessionAttributes).doesNotContainKey(SessionAttributeNames.CONFIG_STORE_ROOT_SUFFIX);
    }

    @Test
    public void validateRootSuffixRejectsInvalidDN() {
        param("rootSuffix", "not a dn");

        step3.validateRootSuffix();

        assertThat(responseBody.toString()).isEqualTo("Invalid Distinguished Name");
        assertThat(sessionAttributes).doesNotContainKey(SessionAttributeNames.CONFIG_STORE_ROOT_SUFFIX);
    }

    @Test
    public void validateRootSuffixAcceptsValidDN() {
        param("rootSuffix", "dc=example,dc=com");

        step3.validateRootSuffix();

        assertThat(responseBody.toString()).isEqualTo("true");
        assertThat(sessionAttributes.get(SessionAttributeNames.CONFIG_STORE_ROOT_SUFFIX)).isEqualTo("dc=example,dc=com");
    }

    // --- validateLocalPort() / validateLocalAdminPort() / validateLocalJmxPort() --------------
    // The "port not in use" happy path calls AMSetupUtils.isPortInUse(), which binds a real socket
    // - environment-dependent, so only the parameter-validation branches and the external-store
    // bypass (which never calls isPortInUse()) are covered here.

    @Test
    public void validateLocalPortRejectsMissingValue() {
        step3.validateLocalPort();

        assertThat(responseBody.toString()).isEqualTo("Missing Required Field");
        assertThat(sessionAttributes).isEmpty();
    }

    @Test
    public void validateLocalPortRejectsNonNumericValue() {
        param("port", "notanumber");

        step3.validateLocalPort();

        assertThat(responseBody.toString())
                .isEqualTo("Port must be greater than or equal to 1 and less than or equal to 65535");
        assertThat(sessionAttributes).isEmpty();
    }

    @Test
    public void validateLocalPortRejectsOutOfRangeValue() {
        param("port", "70000");

        step3.validateLocalPort();

        assertThat(responseBody.toString())
                .isEqualTo("Port must be greater than or equal to 1 and less than or equal to 65535");
        assertThat(sessionAttributes).isEmpty();
    }

    @Test
    public void validateLocalPortAcceptsValueForExternalDataStore() {
        sessionAttributes.put(SetupConstants.CONFIG_VAR_DATA_STORE, SetupConstants.SMS_DS_DATASTORE);
        param("port", "1389");

        step3.validateLocalPort();

        assertThat(responseBody.toString()).isEqualTo("ok");
        assertThat(sessionAttributes.get("configStorePort")).isEqualTo("1389");
    }

    @Test
    public void validateLocalAdminPortAcceptsValueForExternalDataStore() {
        sessionAttributes.put(SetupConstants.CONFIG_VAR_DATA_STORE, SetupConstants.SMS_DS_DATASTORE);
        param("port", "4444");

        step3.validateLocalAdminPort();

        assertThat(responseBody.toString()).isEqualTo("ok");
        assertThat(sessionAttributes.get("configStoreAdminPort")).isEqualTo("4444");
    }

    @Test
    public void validateLocalJmxPortAcceptsValueForExternalDataStore() {
        sessionAttributes.put(SetupConstants.CONFIG_VAR_DATA_STORE, SetupConstants.SMS_DS_DATASTORE);
        param("port", "1689");

        step3.validateLocalJmxPort();

        assertThat(responseBody.toString()).isEqualTo("ok");
        assertThat(sessionAttributes.get("configStoreJmxPort")).isEqualTo("1689");
    }

    // --- validateEncKey() ----------------------------------------------------------------------

    @Test
    public void validateEncKeyRejectsMissingValue() {
        step3.validateEncKey();

        assertThat(responseBody.toString()).isEqualTo("Missing Required Field");
        assertThat(sessionAttributes).isEmpty();
    }

    @Test
    public void validateEncKeyRejectsShortKey() {
        param("encKey", "short");

        step3.validateEncKey();

        assertThat(responseBody.toString())
                .isEqualTo("System can be insecured as key is less than 10 characters long.");
        assertThat(sessionAttributes.get(SessionAttributeNames.ENCRYPTION_KEY)).isEqualTo("short");
    }

    @Test
    public void validateEncKeyAcceptsLongEnoughKey() {
        param("encKey", "longenoughkey123");

        step3.validateEncKey();

        assertThat(responseBody.toString()).isEqualTo("true");
        assertThat(sessionAttributes.get(SessionAttributeNames.ENCRYPTION_KEY)).isEqualTo("longenoughkey123");
    }

    // --- validateConfigStoreHost() -------------------------------------------------------------

    @Test
    public void validateConfigStoreHostRejectsMissingValue() {
        step3.validateConfigStoreHost();

        assertThat(responseBody.toString()).isEqualTo("Missing Required Field");
        assertThat(sessionAttributes).isEmpty();
    }

    @Test
    public void validateConfigStoreHostAcceptsResolvableHost() {
        param("configStoreHost", "localhost");

        step3.validateConfigStoreHost();

        assertThat(responseBody.toString()).isEqualTo("ok");
        assertThat(sessionAttributes.get("configStoreHost")).isEqualTo("localhost");
    }
}
