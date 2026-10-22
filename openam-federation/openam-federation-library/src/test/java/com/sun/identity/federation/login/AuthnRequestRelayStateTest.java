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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.FederationStaticsLock;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.MetaManagerFailureFixture;
import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.services.FSLoginHelper;
import com.sun.identity.federation.services.FSLoginHelperException;
import com.sun.identity.federation.services.FSSessionManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.liberty.LibertyManager;

/**
 * An SP-initiated single sign-on stores the caller's RelayState in the AuthnRequest it sends,
 * and the assertion consumer redirects to whatever comes back after the IdP round trip. The
 * value is checked where it is read, before it is stored, not where it is used a request later.
 */
public class AuthnRequestRelayStateTest {

    private static final String IDP = "https://idp.example.com/idff";
    private static final String OFF_HOST = "https://evil.example/";
    private static final String ALLOWED = "https://trusted.example.com/landing";

    private IDFFMetaManager originalMetaInstance;
    private long originalFailedAt;
    private Debug originalDebug;
    private Map requestsBefore;

    @BeforeMethod
    public void setUp() throws Exception {
        FederationStaticsLock.acquire();
        originalMetaInstance = FSUtils.metaInstance;
        originalFailedAt = MetaManagerFailureFixture.failedAt();
        originalDebug = FSUtils.debug;
        requestsBefore = storedRequestIds();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        try {
            FSUtils.metaInstance = originalMetaInstance;
            MetaManagerFailureFixture.setFailedAt(originalFailedAt);
            FSUtils.debug = originalDebug;
            removeRequestsStoredSince(requestsBefore);
        } finally {
            FederationStaticsLock.release();
        }
    }

    /**
     * {@code FSLoginHelper} captured the meta manager when the class loaded, so an SMS that was
     * not up yet at that moment failed every SP-initiated login until a restart - and a test
     * could not choose the provider it runs against.
     */
    @Test
    public void loginHelperUsesTheMetaManagerAvailableWhenItIsCreated() throws Exception {
        FSUtils.metaInstance = null;
        Throwable unavailable = catchThrowable(() -> createAuthnRequest("https://x.example/"));

        FSUtils.metaInstance = HostedProviderFixture.federatingProviderWithAllowList();
        Map created = createAuthnRequest("https://x.example/");

        assertThat(unavailable).isInstanceOf(FSLoginHelperException.class);
        assertThat(created.get(IFSConstants.AUTH_REQUEST_ID)).isNotNull();
    }

    /** An LRURL the SP's allow-list names is kept; one it does not name is dropped. */
    @DataProvider(name = "federationLRURL")
    public Object[][] federationLRURL() {
        return new Object[][] {
            { ALLOWED, ALLOWED },
            // absent, as when the caller sends none: the consumer uses the provider home URL
            { OFF_HOST, null }
        };
    }

    @Test(dataProvider = "federationLRURL")
    public void federationStoresOnlyAnAllowedLRURL(String lrurl, String stored) throws Exception {
        FSUtils.metaInstance = HostedProviderFixture.federatingProviderWithAllowList();
        HttpServletRequest request = spRequest();
        when(request.getParameter(IFSConstants.LRURL)).thenReturn(lrurl);
        when(request.getParameter(IFSConstants.SELECTEDPROVIDER)).thenReturn(IDP);

        FSAuthnRequest authnRequest = storedDuring(
            () -> new FSPostLogin().doFederation(request, mock(HttpServletResponse.class)));

        assertThat(authnRequest.getRelayState()).isEqualTo(stored);
    }

    /** Not sent, or sent empty: a form field left blank. */
    @DataProvider(name = "absentLRURL")
    public Object[][] absentLRURL() {
        return new Object[][] { { null }, { "" } };
    }

    /** Most SP-initiated logins send no LRURL, and nothing was refused for them. */
    @Test(dataProvider = "absentLRURL")
    public void federationWithoutAnLRURLReportsNoRefusal(String absent) throws Exception {
        FSUtils.metaInstance = HostedProviderFixture.federatingProviderWithAllowList();
        Debug debug = mock(Debug.class);
        when(debug.warningEnabled()).thenReturn(true);
        FSUtils.debug = debug;
        HttpServletRequest request = spRequest();
        when(request.getParameter(IFSConstants.LRURL)).thenReturn(absent);
        when(request.getParameter(IFSConstants.SELECTEDPROVIDER)).thenReturn(IDP);

        FSAuthnRequest authnRequest = storedDuring(
            () -> new FSPostLogin().doFederation(request, mock(HttpServletResponse.class)));

        // passed on as it came: the consumer's own default applies to it
        assertThat(authnRequest.getRelayState()).isEqualTo(absent);
        verify(debug, never()).warning(contains("RelayState"));
    }

    /** A goto the SP's allow-list names is kept; any other falls back to the provider home URL. */
    @DataProvider(name = "preLoginGoto")
    public Object[][] preLoginGoto() {
        return new Object[][] {
            { ALLOWED, ALLOWED },
            { OFF_HOST, HostedProviderFixture.HOME_URL }
        };
    }

    @Test(dataProvider = "preLoginGoto")
    public void preLoginStoresOnlyAnAllowedGoto(String gotoURL, String stored) throws Exception {
        FSUtils.metaInstance = HostedProviderFixture.federatingProviderWithAllowList();
        // no fedCookie and no session: the branch that starts a single sign-on
        HttpServletRequest request = spRequest();
        when(request.getParameter(IFSConstants.GOTOKEY)).thenReturn(gotoURL);

        FSAuthnRequest authnRequest = storedDuring(
            () -> new FSPreLogin().doPreLogin(request, mock(HttpServletResponse.class)));

        assertThat(authnRequest.getRelayState()).isEqualTo(stored);
    }

    /**
     * {@code FSPreLogin} captured the meta manager when the class loaded, so an SMS not up yet
     * at that moment sent every /preLogin to the error page until a restart. A refused goto
     * falls back to the home page of the provider configured now.
     */
    @Test
    public void preLoginRecoversWhenTheMetaManagerBecomesAvailableAfterClassLoad()
        throws Exception {

        HttpServletRequest request = spRequest();
        when(request.getParameter(IFSConstants.GOTOKEY)).thenReturn(OFF_HOST);
        FSUtils.metaInstance = null;
        // touches the class with no manager available - what an SMS outage at startup looks like
        new FSPreLogin().doPreLogin(request, mock(HttpServletResponse.class));

        FSUtils.metaInstance = HostedProviderFixture.secondFederatingProvider();
        FSAuthnRequest authnRequest = storedDuring(
            () -> new FSPreLogin().doPreLogin(request, mock(HttpServletResponse.class)));

        assertThat(authnRequest.getRelayState())
            .isEqualTo(HostedProviderFixture.SECOND_HOME_URL);
    }

    /** The common login page's new request: kept if allowed, else the provider home URL. */
    @Test(dataProvider = "preLoginGoto")
    public void commonLoginStoresOnlyAnAllowedLRURL(String lrurl, String stored) throws Exception {
        FSUtils.metaInstance = HostedProviderFixture.federatingProviderWithAllowList();
        HttpServletRequest request = spRequest();
        when(request.getParameter(IFSConstants.LRURL)).thenReturn(lrurl);
        when(request.getSession()).thenReturn(mock(HttpSession.class));

        FSAuthnRequest authnRequest = storedDuring(() -> LibertyManager.getNewRequest(request));

        assertThat(authnRequest.getRelayState()).isEqualTo(stored);
    }

    /** The common login page's new request, the same way. */
    @Test
    public void commonLoginRecoversWhenTheMetaManagerBecomesAvailableAfterClassLoad()
        throws Exception {

        HttpServletRequest request = spRequest();
        when(request.getParameter(IFSConstants.LRURL)).thenReturn(OFF_HOST);
        when(request.getSession()).thenReturn(mock(HttpSession.class));
        FSUtils.metaInstance = null;
        // touches the class with no manager available - what an SMS outage at startup looks like
        LibertyManager.getNewRequest(request);

        FSUtils.metaInstance = HostedProviderFixture.secondFederatingProvider();
        FSAuthnRequest authnRequest = storedDuring(() -> LibertyManager.getNewRequest(request));

        assertThat(authnRequest.getRelayState())
            .isEqualTo(HostedProviderFixture.SECOND_HOME_URL);
    }

    /** The LECP envelope: kept if allowed, else the federation-done page. */
    @DataProvider(name = "envelopeLRURL")
    public Object[][] envelopeLRURL() {
        return new Object[][] {
            { ALLOWED, ALLOWED },
            { OFF_HOST, "http://host.example.com:8080/openam/config/federation/default/"
                + "FederationDone.jsp?metaAlias=/idp" }
        };
    }

    @Test(dataProvider = "envelopeLRURL")
    public void lecpEnvelopeStoresOnlyAnAllowedLRURL(String lrurl, String stored)
        throws Exception {

        FSUtils.metaInstance = HostedProviderFixture.federatingProviderWithAllowList();
        HttpServletRequest request = spRequest();
        when(request.getParameter(IFSConstants.LRURL)).thenReturn(lrurl);

        FSAuthnRequest authnRequest =
            storedDuring(() -> LibertyManager.getAuthnRequestEnvelope(request));

        assertThat(authnRequest.getRelayState()).isEqualTo(stored);
    }

    /**
     * The URL {@code doFederation} returns names the caller's metaAlias as it was sent, and a
     * line break in it must not start a forged entry in the debug file. The request ID in that
     * URL is generated afresh, so the line is matched around it.
     */
    @Test
    public void federationLogsTheReturnUrlOnOneLine() throws Exception {
        FSUtils.metaInstance = metaAliasWithLineBreakResolving();
        Debug debug = installedDebug();

        new FSPostLogin().doFederation(lineBreakMetaAliasRequest(),
            mock(HttpServletResponse.class));

        verify(debug).message(matches("FSPostLogin::doFederation returning with "
            + "http://host\\.example\\.com:8080/openam/IntersiteTransferService\\?.*"
            + "&metaAlias=/idp__X"));
    }

    /** ... and so does the handler that redirects to it. */
    @Test
    public void federationHandlerLogsTheReturnUrlOnOneLine() throws Exception {
        FSUtils.metaInstance = metaAliasWithLineBreakResolving();
        Debug debug = installedDebug();

        new FSFederationHandler().processRequest(lineBreakMetaAliasRequest(),
            mock(HttpServletResponse.class));

        verify(debug).message(matches("FSFederationHandler:: Redirecting to"
            + "http://host\\.example\\.com:8080/openam/IntersiteTransferService\\?.*"
            + "&metaAlias=/idp__X after postLogin"));
    }

    /** The federating provider, reached through a metaAlias that carries a line break. */
    private static IDFFMetaManager metaAliasWithLineBreakResolving() throws Exception {
        IDFFMetaManager metaManager = HostedProviderFixture.federatingProviderWithAllowList();
        when(metaManager.getProviderRoleByMetaAlias("/idp\r\nX")).thenReturn(IFSConstants.SP);
        when(metaManager.getEntityIDByMetaAlias("/idp\r\nX"))
            .thenReturn(HostedProviderFixture.ENTITY);
        return metaManager;
    }

    /** A federation request naming the IdP, through a metaAlias that carries a line break. */
    private static HttpServletRequest lineBreakMetaAliasRequest() {
        HttpServletRequest request = spRequest();
        when(request.getParameter(IFSConstants.META_ALIAS)).thenReturn("/idp\r\nX");
        when(request.getParameter(IFSConstants.SELECTEDPROVIDER)).thenReturn(IDP);
        return request;
    }

    /** A debug log with messages on, installed as the one FSUtils writes to. */
    private static Debug installedDebug() {
        Debug debug = mock(Debug.class);
        when(debug.messageEnabled()).thenReturn(true);
        FSUtils.debug = debug;
        return debug;
    }

    private interface Action {
        void run() throws Exception;
    }

    /** The one AuthnRequest the action stored for the SP. */
    private static FSAuthnRequest storedDuring(Action action) throws Exception {
        Map stored = authnRequests();
        Set before = new HashSet(stored.keySet());
        action.run();
        Set added = new HashSet(stored.keySet());
        added.removeAll(before);
        assertThat(added).hasSize(1);
        return (FSAuthnRequest) stored.get(added.iterator().next());
    }

    private static Map authnRequests() throws Exception {
        return requestMap(FSSessionManager.getInstance(HostedProviderFixture.META_ALIAS));
    }

    private static Map requestMap(FSSessionManager sessionManager) throws Exception {
        Field field = FSSessionManager.class.getDeclaredField("idAuthnRequestMap");
        field.setAccessible(true);
        return (Map) field.get(sessionManager);
    }

    /**
     * The session managers created so far, by metaAlias. Read from the class's table rather than
     * through {@code getInstance}, whose constructor asks for the meta manager - and with none in
     * place records a failure that sends the next lookup down another branch.
     */
    private static Map sessionManagers() throws Exception {
        Field field = FSSessionManager.class.getDeclaredField("instanceMap");
        field.setAccessible(true);
        return new HashMap((Map) field.get(null));
    }

    /** The IDs of the AuthnRequests each session manager holds, by metaAlias. */
    private static Map storedRequestIds() throws Exception {
        Map ids = new HashMap();
        for (Object entry : sessionManagers().entrySet()) {
            Map.Entry manager = (Map.Entry) entry;
            ids.put(manager.getKey(),
                new HashSet(requestMap((FSSessionManager) manager.getValue()).keySet()));
        }
        return ids;
    }

    /**
     * Takes out the AuthnRequests stored since {@code before}: nothing removes them in the
     * library outside a server, so they would stay for the life of the JVM.
     */
    private static void removeRequestsStoredSince(Map before) throws Exception {
        for (Object entry : sessionManagers().entrySet()) {
            Map.Entry manager = (Map.Entry) entry;
            FSSessionManager sessionManager = (FSSessionManager) manager.getValue();
            Set added = new HashSet(requestMap(sessionManager).keySet());
            Set held = (Set) before.get(manager.getKey());
            if (held != null) {
                added.removeAll(held);
            }
            for (Object requestID : added) {
                sessionManager.removeAuthnRequest((String) requestID);
            }
        }
    }

    private static Map createAuthnRequest(String relayState) throws Exception {
        return new FSLoginHelper(spRequest()).createAuthnRequest(new HashMap(), relayState, null,
            HostedProviderFixture.META_ALIAS, IDP, true);
    }

    /** A request to the SP's federation endpoints at http://host.example.com:8080/openam. */
    private static HttpServletRequest spRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getHeaderNames())
            .thenReturn(Collections.enumeration(Collections.<String>emptyList()));
        when(request.getHeader("Host")).thenReturn("host.example.com:8080");
        when(request.getRequestURL())
            .thenReturn(new StringBuffer("http://host.example.com:8080/openam/federation"));
        when(request.getRequestURI()).thenReturn("/openam/federation");
        when(request.getServerName()).thenReturn("host.example.com");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getParameter(IFSConstants.META_ALIAS))
            .thenReturn(HostedProviderFixture.META_ALIAS);
        return request;
    }
}
