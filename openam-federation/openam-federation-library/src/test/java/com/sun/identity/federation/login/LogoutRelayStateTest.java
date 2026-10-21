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

package com.sun.identity.federation.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.FederationException;
import com.sun.identity.federation.common.FederationStaticsLock;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.message.FSAuthnResponse;
import com.sun.identity.federation.message.FSFederationTerminationNotification;
import com.sun.identity.federation.message.FSLogoutNotification;
import com.sun.identity.federation.message.FSLogoutResponse;
import com.sun.identity.federation.message.FSNameRegistrationRequest;
import com.sun.identity.federation.message.FSNameRegistrationResponse;
import com.sun.identity.federation.message.FSResponse;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.plugins.FederationSPAdapter;
import com.sun.identity.federation.services.logout.FSProcessLogoutServlet;
import com.sun.identity.multiprotocol.SingleLogoutManager;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationListener;
import com.sun.identity.plugin.configuration.ConfigurationManager;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.shared.debug.Debug;

/**
 * A logout the user starts at {@code /ProcessLogout} ends with a redirect to the RelayState the
 * request named, after the session is gone. Unchecked that is a logout CSRF that lands on any
 * page the sender chose; the value is checked where the servlet reads it.
 */
public class LogoutRelayStateTest {

    private static final String OFF_HOST = "https://evil.example/";
    private static final String ALLOWED = "https://trusted.example.com/landing";
    private static final String LOGOUT_DONE =
        "http://host.example.com:8080/openam/config/federation/default/LogoutDone.jsp"
            + "?metaAlias=/idp";

    private static final Object SESSION = new Object();

    private IDFFMetaManager originalMetaInstance;
    private Debug originalDebug;
    private Object originalServletMetaManager;
    private SessionProvider originalSessionProvider;

    @BeforeMethod
    public void setUp() throws Exception {
        FederationStaticsLock.acquire();
        originalMetaInstance = FSUtils.metaInstance;
        originalDebug = FSUtils.debug;
        originalServletMetaManager = servletMetaManagerField().get(null);
        originalSessionProvider = (SessionProvider) sessionProviderField().get(null);
        RecordingSPAdapter.logoutResponse = null;
        initialiseSingleLogoutManager();

        SessionProvider sessions = mock(SessionProvider.class);
        when(sessions.getSession(any(HttpServletRequest.class))).thenReturn(SESSION);
        when(sessions.isValid(SESSION)).thenReturn(true);
        // read as a DN when the logout looks for a provider to answer
        when(sessions.getPrincipalName(SESSION)).thenReturn("uid=user,ou=people,dc=example,dc=com");
        sessionProviderField().set(null, sessions);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        try {
            FSUtils.metaInstance = originalMetaInstance;
            FSUtils.debug = originalDebug;
            servletMetaManagerField().set(null, originalServletMetaManager);
            sessionProviderField().set(null, originalSessionProvider);
            RecordingSPAdapter.logoutResponse = null;
        } finally {
            FederationStaticsLock.release();
        }
    }

    /** A RelayState the provider's allow-list names is still the page the logout ends on. */
    @DataProvider(name = "localLogout")
    public Object[][] localLogout() {
        return new Object[][] {
            { ALLOWED, ALLOWED + "?logoutStatus=logoutSuccess" },
            { OFF_HOST, LOGOUT_DONE + "&logoutStatus=logoutSuccess" }
        };
    }

    @Test(dataProvider = "localLogout")
    public void localLogoutEndsOnlyOnAnAllowedRelayState(String relayState, String landing)
        throws Exception {

        useProvider(HostedProviderFixture.federatingProviderWithAllowList());
        HttpServletRequest request = logoutRequest("local", relayState);
        HttpServletResponse response = mock(HttpServletResponse.class);

        new FSProcessLogoutServlet().doGet(request, response);

        verify(response).sendRedirect(landing);
    }

    /** The refusal names the value, on one line whatever the sender put in it. */
    @Test
    public void localLogoutReportsTheRefusedRelayState() throws Exception {
        useProvider(HostedProviderFixture.federatingProviderWithAllowList());
        Debug debug = mock(Debug.class);
        when(debug.warningEnabled()).thenReturn(true);
        FSUtils.debug = debug;

        new FSProcessLogoutServlet().doGet(
            logoutRequest("local", "https://evil.example/\r\nforged"),
            mock(HttpServletResponse.class));

        verify(debug).warning("FSPostLogin::allowedRelayState RelayState "
            + "\"https://evil.example/__forged\" is not permitted by the provider's RelayState "
            + "allow-list; treating it as absent");
    }

    /**
     * An IdP answering an SP's logout sends the RelayState back on the LogoutResponse, and the
     * SP reads that response for its SP adapter: the value it reads is the request parameter
     * the servlet has already checked, and must not replace the checked one.
     */
    @DataProvider(name = "logoutResponse")
    public Object[][] logoutResponse() {
        return localLogout();
    }

    @Test(dataProvider = "logoutResponse")
    public void logoutResponseEndsOnlyOnAnAllowedRelayState(String relayState, String landing)
        throws Exception {

        useProvider(
            HostedProviderFixture.federatingProviderWithSPAdapter(RecordingSPAdapter.class));
        HttpServletRequest request = logoutRequest("remote", relayState);
        when(request.getParameter(IFSConstants.MAJOR_VERSION)).thenReturn("1");
        when(request.getParameter(IFSConstants.MINOR_VERSION)).thenReturn("2");
        when(request.getParameter(IFSConstants.RESPONSE_ID)).thenReturn("response-1");
        when(request.getParameter(IFSConstants.ISSUE_INSTANT)).thenReturn("2026-09-25T10:00:00Z");
        when(request.getParameter(IFSConstants.PROVIDER_ID))
            .thenReturn("https://idp.example.com/idff");
        when(request.getParameter(IFSConstants.VALUE)).thenReturn(IFSConstants.SAML_SUCCESS);
        HttpServletResponse response = mock(HttpServletResponse.class);

        new FSProcessLogoutServlet().doGet(request, response);

        verify(response).sendRedirect(landing);
        // the response was read, and carried the value: a parse that failed would leave nothing
        // to take the RelayState from, and the refused row would pass for that reason alone
        assertThat(RecordingSPAdapter.logoutResponse).isNotNull();
        assertThat(RecordingSPAdapter.logoutResponse.getRelayState()).isEqualTo(relayState);
    }

    private static void useProvider(IDFFMetaManager metaManager) throws Exception {
        FSUtils.metaInstance = metaManager;
        // the servlet reads it once, in init
        servletMetaManagerField().set(null, metaManager);
    }

    /** A logout the browser of a signed-in user sends to the SP's /ProcessLogout. */
    private static HttpServletRequest logoutRequest(String logoutSource, String relayState) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getHeader("Host")).thenReturn("host.example.com:8080");
        when(request.getRequestURL())
            .thenReturn(new StringBuffer("http://host.example.com:8080/openam/ProcessLogout"));
        when(request.getRequestURI()).thenReturn("/openam/ProcessLogout");
        when(request.getServerName()).thenReturn("host.example.com");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getParameter(IFSConstants.META_ALIAS))
            .thenReturn(HostedProviderFixture.META_ALIAS);
        when(request.getParameter("logoutSource")).thenReturn(logoutSource);
        when(request.getParameter(IFSConstants.LOGOUT_RELAY_STATE)).thenReturn(relayState);
        return request;
    }

    private static Field servletMetaManagerField() throws Exception {
        Field field = FSProcessLogoutServlet.class.getDeclaredField("metaManager");
        field.setAccessible(true);
        return field;
    }

    /**
     * Every logout asks {@code SingleLogoutManager} whether another protocol shares the session,
     * and its class initialiser fails - for the life of the JVM - without a configuration to
     * read. The configuration is named for that one initialisation only: any other class
     * initialised while it is named reads it as well.
     */
    private static void initialiseSingleLogoutManager() throws Exception {
        Field configClass = configClassField();
        Object original = configClass.get(null);
        configClass.set(null, NoOtherProtocols.class.getName());
        try {
            SingleLogoutManager.getInstance();
        } finally {
            configClass.set(null, original);
        }
    }

    private static Field configClassField() throws Exception {
        Field field = ConfigurationManager.class.getDeclaredField("configClass");
        field.setAccessible(true);
        return field;
    }

    /** The provider {@code SessionManager.getProvider} hands out, resolved once at class load. */
    private static Field sessionProviderField() throws Exception {
        Field field = SessionManager.class.getDeclaredField("sp");
        field.setAccessible(true);
        return field;
    }

    /** A configuration with no multi-protocol logout handlers. Public, with a no-arg constructor. */
    public static class NoOtherProtocols implements ConfigurationInstance {

        public void init(String componentName, Object session) {
        }

        public Map getConfiguration(String realm, String configName) {
            return Collections.emptyMap();
        }

        public void setConfiguration(String realm, String configName, Map avPairs) {
        }

        public void createConfiguration(String realm, String configName, Map avPairs) {
        }

        public void deleteConfiguration(String realm, String configName, Set attributes) {
        }

        public Set getAllConfigurationNames(String realm) {
            return Collections.emptySet();
        }

        public String addListener(ConfigurationListener listener) {
            return null;
        }

        public void removeListener(String listenerID) {
        }
    }

    /**
     * An SP adapter that keeps the logout response it is handed and does nothing else. Public,
     * with a no-arg constructor: the handler creates its own instance by class name on each
     * call, so the response is kept in a static field.
     */
    public static class RecordingSPAdapter implements FederationSPAdapter {

        static FSLogoutResponse logoutResponse;

        public void initialize(String hostedEntityID, Set initParams) {
        }

        public void preSSOFederationRequest(String hostedEntityID, String idpEntityID,
            HttpServletRequest request, HttpServletResponse response,
            FSAuthnRequest authnRequest) {
        }

        public void preSSOFederationProcess(String hostedEntityID, HttpServletRequest request,
            HttpServletResponse response, FSAuthnRequest authnRequest,
            FSAuthnResponse authnResponse, FSResponse samlResponse) throws FederationException {
        }

        public boolean postSSOFederationSuccess(String hostedEntityID,
            HttpServletRequest request, HttpServletResponse response, Object ssoToken,
            FSAuthnRequest authnRequest, FSAuthnResponse authnResponse, FSResponse samlResponse)
            throws FederationException {
            return false;
        }

        public boolean postSSOFederationFailure(String hostedEntityID,
            HttpServletRequest request, HttpServletResponse response,
            FSAuthnRequest authnRequest, FSAuthnResponse authnResponse, FSResponse samlResponse,
            int failureCode) {
            return false;
        }

        public void postRegisterNameIdentifierSuccess(String hostedEntityID,
            HttpServletRequest request, HttpServletResponse response, String userDN,
            FSNameRegistrationRequest regRequest, FSNameRegistrationResponse regResponse,
            String regProfile) {
        }

        public void postTerminationNotificationSuccess(String hostedEntityID,
            HttpServletRequest request, HttpServletResponse response, String userDN,
            FSFederationTerminationNotification notification, String termProfile) {
        }

        public void preSingleLogoutProcess(String hostedEntityID, HttpServletRequest request,
            HttpServletResponse response, String userDN, FSLogoutNotification logoutRequest,
            FSLogoutResponse logoutResponse, String sloProfile) {
            RecordingSPAdapter.logoutResponse = logoutResponse;
        }

        public void postSingleLogoutSuccess(String hostedEntityID, HttpServletRequest request,
            HttpServletResponse response, String userDN, FSLogoutNotification logoutRequest,
            FSLogoutResponse logoutResponse, String sloProfile) {
        }
    }
}
