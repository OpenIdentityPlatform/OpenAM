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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sun.identity.federation.common.DeploymentIdentityFixture;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.FederationStaticsLock;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.MetaManagerFailureFixture;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.jaxb.entityconfig.IDPDescriptorConfigElement;
import com.sun.identity.federation.jaxb.entityconfig.SPDescriptorConfigElement;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;

/**
 * Covers the fail-closed RelayState guard that mirrors {@code SAML2Utils.isRelayStateURLValid}:
 * an unresolved provider must never be treated as a valid target, so an absent allow-list cannot
 * be abused through a {@code metaAlias} that does not resolve to a hosted entity.
 */
public class FSPostLoginTest {

    private static final String REALM = "/";
    private static final String ENTITY = "https://sp.example.com/idff";
    private static final String OFF_HOST = "https://evil.example/";
    private static final String ALLOWED = "https://trusted.example.com/landing";
    private static final String SAME_DEPLOYMENT =
        "http://host.example.com:8080/openam/landing";
    /** A RelayState the allow-list admits, 328 characters long ... */
    private static final String LONG_ALLOWED = "https://trusted.example.com/" + "a".repeat(300);
    /** ... and what of it reaches the debug log. */
    private static final String LONG_ALLOWED_LOGGED =
        "https://trusted.example.com/" + "a".repeat(228) + "...[truncated, 328 characters]";
    /** The signing mode, which FSServiceUtils reads on every call. */
    private static final String SIGNING_ON = "com.sun.identity.federation.services.signingOn";

    private IDFFMetaManager originalMetaInstance;
    private long originalFailedAt;
    private String originalDeploymentURI;
    private Debug originalDebug;
    private String originalSigningOn;

    @BeforeMethod
    public void setUp() throws Exception {
        FederationStaticsLock.acquire();
        originalMetaInstance = FSUtils.metaInstance;
        originalFailedAt = MetaManagerFailureFixture.failedAt();
        originalDeploymentURI = FSUtils.deploymentURI;
        originalDebug = FSUtils.debug;
        originalSigningOn = SystemPropertiesManager.get(SIGNING_ON);
        FSUtils.deploymentURI = "/openam";
    }

    @AfterMethod
    public void tearDown() throws Exception {
        try {
            FSUtils.metaInstance = originalMetaInstance;
            MetaManagerFailureFixture.setFailedAt(originalFailedAt);
            FSUtils.deploymentURI = originalDeploymentURI;
            FSUtils.debug = originalDebug;
            // The store can be overridden but not cleared; an unset mode reads as optional.
            SystemPropertiesManager.initializeProperties(SIGNING_ON,
                originalSigningOn == null ? IFSConstants.OPTIONAL : originalSigningOn);
            DeploymentIdentityFixture.restore();
        } finally {
            FederationStaticsLock.release();
        }
    }

    /**
     * A metaAlias whose entity ID did not resolve. The list the check would read with it names
     * the RelayState, so the refusal is the unresolved provider's alone.
     */
    @Test
    public void unresolvedProviderRejectsARelayStateTheListWouldPermit() throws Exception {
        FSUtils.metaInstance = HostedProviderFixture.providerWithAllowList();

        assertThat(FSPostLogin.isRelayStateURLValid(REALM, null, IFSConstants.SP, ALLOWED))
            .isFalse();
    }

    /** Without a role the check would read the IDP configuration, so that one names the list. */
    @Test
    public void nullRoleRejectsARelayStateTheListWouldPermit() throws Exception {
        IDFFMetaManager metaManager = HostedProviderFixture.providerWithAllowList();
        List<?> allowList = metaManager
            .getSPDescriptorConfig(REALM, HostedProviderFixture.ENTITY).getValue().getAttribute();
        BaseConfigType idpBaseConfig = mock(BaseConfigType.class);
        doReturn(allowList).when(idpBaseConfig).getAttribute();
        IDPDescriptorConfigElement idpConfig = new IDPDescriptorConfigElement(idpBaseConfig);
        when(metaManager.getIDPDescriptorConfig(REALM, HostedProviderFixture.ENTITY))
            .thenReturn(idpConfig);
        FSUtils.metaInstance = metaManager;

        assertThat(FSPostLogin.isRelayStateURLValid(
            REALM, HostedProviderFixture.ENTITY, null, ALLOWED)).isFalse();
    }

    /**
     * An unchecked failure while the list is read, past what the extractor wraps: on this
     * unauthenticated endpoint it is a refusal, not a 500.
     */
    @Test
    public void allowListThatFailsToReadRejectsRatherThanThrows() throws Exception {
        IDFFMetaManager metaManager = HostedProviderFixture.providerWithAllowList();
        BaseConfigType unreadable = mock(BaseConfigType.class);
        when(unreadable.getAttribute()).thenThrow(new IllegalStateException("unreadable"));
        SPDescriptorConfigElement config = new SPDescriptorConfigElement(unreadable);
        when(metaManager.getSPDescriptorConfig(nullable(String.class), nullable(String.class)))
            .thenReturn(config);
        FSUtils.metaInstance = metaManager;

        assertThat(FSPostLogin.isRelayStateURLValid(
            REALM, HostedProviderFixture.ENTITY, IFSConstants.SP, ALLOWED)).isFalse();
    }

    @DataProvider(name = "nonAbsoluteRelayStates")
    public Object[][] nonAbsoluteRelayStates() {
        return new Object[][] {
            { "/console" },
            { "console/index.html" },
            // protocol relative: the delegate does consult the allow-list for this one, so a
            // configured list already refuses it - but an unconfigured list reads as "anything
            // goes", and it would then reach sendRedirect as a 302 to another host
            { "//evil.example/x" },
            { "\\\\evil.example\\x" },
            { "ftp://evil.example/x" }
        };
    }

    /**
     * {@code RedirectUrlValidator.isRedirectUrlValid} returns true for any non-absolute URL
     * before the allow-list is consulted, so without an absolute-URL requirement here every
     * relative RelayState skipped the list outright.
     */
    @Test(dataProvider = "nonAbsoluteRelayStates")
    public void nonAbsoluteRelayStateIsRejected(String relayState) throws Exception {
        // A readable allow-list, otherwise every RelayState is refused because the extractor
        // fails closed and the refusal says nothing about the guard under test.
        FSUtils.metaInstance = HostedProviderFixture.providerWithAllowList();

        assertThat(FSPostLogin.isRelayStateURLValid(
            REALM, HostedProviderFixture.ENTITY, IFSConstants.SP, relayState)).isFalse();
        // the same allow-list does admit the URL it names, so the refusals above are the
        // absolute-URL requirement and not a configuration that refuses everything
        assertThat(FSPostLogin.isRelayStateURLValid(REALM, HostedProviderFixture.ENTITY,
            IFSConstants.SP, "https://trusted.example.com/landing")).isTrue();
    }

    /**
     * An entity that configures no {@code relayStateUrlList} is the state every ID-FF entity is
     * in before an admin fills one in, and the caller names the entity whose list is consulted
     * through a {@code metaAlias} request parameter. Reading an absent list as "unrestricted",
     * the way {@code RedirectUrlValidator} does on its own, would therefore leave the guard off
     * across the deployment and let one unconfigured entity re-open every endpoint.
     */
    @Test
    public void unconfiguredAllowListRejectsOffHostRelayState() throws Exception {
        FSUtils.metaInstance = HostedProviderFixture.providerWithoutAllowList();
        DeploymentIdentityFixture.installServer("http", "host.example.com", "8080", "/openam");
        DeploymentIdentityFixture.installServerList((String[]) null);

        assertThat(FSPostLogin.isRelayStateURLValid(REALM, HostedProviderFixture.ENTITY,
            IFSConstants.SP, OFF_HOST)).isFalse();
    }

    /**
     * ... while a target this deployment serves itself needs no configuration to be safe. What
     * "this deployment" is comes from its configuration, so no request is needed to decide it.
     */
    @Test
    public void unconfiguredAllowListPermitsTheConfiguredDeployment() throws Exception {
        FSUtils.metaInstance = HostedProviderFixture.providerWithoutAllowList();
        DeploymentIdentityFixture.installServer("http", "host.example.com", "8080", "/openam");
        DeploymentIdentityFixture.installServerList((String[]) null);

        assertThat(FSPostLogin.isRelayStateURLValid(REALM, HostedProviderFixture.ENTITY,
            IFSConstants.SP, SAME_DEPLOYMENT)).isTrue();
    }

    /**
     * The request's server name and port are its Host header, so for any name that resolves to
     * the deployment a comparison with the request compares the caller's string with itself.
     * The deployment is named by its configuration instead.
     */
    @Test
    public void unconfiguredAllowListIgnoresTheRequestsHostHeader() throws Exception {
        FSUtils.metaInstance = HostedProviderFixture.providerWithoutAllowList();
        DeploymentIdentityFixture.installServer("http", "host.example.com", "8080", "/openam");
        DeploymentIdentityFixture.installServerList((String[]) null);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(IFSConstants.META_ALIAS))
            .thenReturn(HostedProviderFixture.META_ALIAS);
        when(request.getServerName()).thenReturn("evil.example");
        when(request.getServerPort()).thenReturn(443);

        assertThat(FSPostLogin.checkedRelayState(request, "https://evil.example/openam/x"))
            .isEqualTo(HostedProviderFixture.HOME_URL);
    }

    /**
     * A configured list still decides on its own: it admits what it names off-host, and refuses
     * a same-deployment URL it does not name. The fallback above applies only to an entity with
     * no list at all.
     */
    @Test
    public void configuredAllowListIsNotWidenedByTheSameDeploymentFallback() throws Exception {
        FSUtils.metaInstance = HostedProviderFixture.providerWithAllowList();
        // the deployment the fallback would admit SAME_DEPLOYMENT for
        DeploymentIdentityFixture.installServer("http", "host.example.com", "8080", "/openam");
        DeploymentIdentityFixture.installServerList((String[]) null);

        assertThat(FSPostLogin.isRelayStateURLValid(REALM, HostedProviderFixture.ENTITY,
            IFSConstants.SP, "https://trusted.example.com/landing")).isTrue();
        assertThat(FSPostLogin.isRelayStateURLValid(REALM, HostedProviderFixture.ENTITY,
            IFSConstants.SP, SAME_DEPLOYMENT)).isFalse();
    }

    @Test
    public void emptyRelayStateIsRejected() {
        assertThat(FSPostLogin.isRelayStateURLValid(REALM, ENTITY, IFSConstants.SP, "")).isFalse();
    }

    /**
     * FSConsentHandler and FSFederationHandler forward an LRURL straight from a request
     * parameter, so they route it through {@code checkedRelayState} first. A provider that
     * cannot be resolved yields no home URL either, which leaves the caller with nothing to
     * forward to rather than the attacker's URL.
     */
    @Test
    public void checkedRelayStateRejectsWhenTheProviderCannotBeResolved() throws Exception {
        IDFFMetaManager metaManager = HostedProviderFixture.providerWithAllowList();
        when(metaManager.getEntityIDByMetaAlias(HostedProviderFixture.META_ALIAS))
            .thenThrow(new IDFFMetaException("unreadable"));
        FSUtils.metaInstance = metaManager;
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(IFSConstants.META_ALIAS))
            .thenReturn(HostedProviderFixture.META_ALIAS);

        assertThat(FSPostLogin.checkedRelayState(request, ALLOWED)).isNull();
    }

    /** Without a metaAlias parameter, the provider is the one the request path names. */
    @Test
    public void checkedRelayStateResolvesTheProviderFromTheRequestPath() throws Exception {
        FSUtils.metaInstance = HostedProviderFixture.providerWithAllowList();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI())
            .thenReturn("/openam/preLogin/metaAlias" + HostedProviderFixture.META_ALIAS);

        assertThat(FSPostLogin.checkedRelayState(request, ALLOWED)).isEqualTo(ALLOWED);
    }

    /**
     * A refused RelayState is the caller's string, logged on an unauthenticated endpoint: a line
     * break in it must not start a forged entry in the debug file.
     */
    @Test
    public void refusedNonAbsoluteRelayStateIsLoggedOnOneLine() {
        Debug debug = installedDebug();

        FSPostLogin.isRelayStateURLValid(REALM, ENTITY, IFSConstants.SP, "/x\r\ny");

        verify(debug).warning("FSPostLogin::isRelayStateURLValid RelayState \"/x__y\" is not an "
            + "absolute http(s) URL; rejecting");
    }

    /**
     * The entity is named by the caller's metaAlias, and an absolute RelayState cannot carry a
     * line break past the URL parse, so what is left to bound for it is its length.
     */
    @Test
    public void refusalForAnUnconfiguredAllowListIsLoggedOnOneBoundedLine() throws Exception {
        FSUtils.metaInstance = HostedProviderFixture.providerWithoutAllowList();
        DeploymentIdentityFixture.installServer("http", "host.example.com", "8080", "/openam");
        DeploymentIdentityFixture.installServerList((String[]) null);
        Debug debug = installedDebug();

        FSPostLogin.isRelayStateURLValid(REALM, ENTITY + "\r\nX", IFSConstants.SP,
            "https://evil.example/" + "a".repeat(300));

        verify(debug).warning("FSPostLogin::isRelayStateURLValid entity "
            + "https://sp.example.com/idff__X configures no relayStateUrlList, so only this "
            + "deployment's own targets are permitted; rejecting \"https://evil.example/"
            + "a".repeat(235) + "...[truncated, 321 characters]\"");
    }

    @Test
    public void relayStateRefusedForARequestsMetaAliasIsLoggedOnOneLine() throws Exception {
        FSUtils.metaInstance = HostedProviderFixture.providerWithAllowList();
        Debug debug = installedDebug();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(IFSConstants.META_ALIAS)).thenReturn("/idp\r\nX");

        FSPostLogin.checkedRelayState(request, "https://evil.example/\r\nX");

        verify(debug).warning("FSPostLogin::checkedRelayState RelayState "
            + "\"https://evil.example/__X\" rejected for metaAlias \"/idp__X\"; using provider "
            + "home URL");
    }

    @Test
    public void metaAliasThatFailsToResolveIsLoggedOnOneLine() throws Exception {
        IDFFMetaManager metaManager = HostedProviderFixture.providerWithAllowList();
        when(metaManager.getEntityIDByMetaAlias("/idp\r\nX"))
            .thenThrow(new IDFFMetaException("unreadable"));
        FSUtils.metaInstance = metaManager;
        Debug debug = installedDebug();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(IFSConstants.META_ALIAS)).thenReturn("/idp\r\nX");

        FSPostLogin.checkedRelayState(request, ALLOWED);

        verify(debug).error(eq("FSPostLogin::resolveProvider could not resolve the provider for "
            + "metaAlias \"/idp__X\""), any(IDFFMetaException.class));
    }

    /**
     * {@code FSPostLogin} captured the meta manager when the class loaded, so an SMS not up yet
     * at that moment left every RelayState check without a provider until a restart. The
     * provider a request names is resolved with the manager configured now.
     */
    @Test
    public void checkedRelayStateRecoversWhenTheMetaManagerBecomesAvailableAfterClassLoad()
        throws Exception {

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(IFSConstants.META_ALIAS))
            .thenReturn(HostedProviderFixture.META_ALIAS);
        FSUtils.metaInstance = null;
        // touches the class with no manager available - what an SMS outage at startup looks like
        FSPostLogin.checkedRelayState(request, null);

        FSUtils.metaInstance = HostedProviderFixture.secondFederatingProvider();

        assertThat(FSPostLogin.checkedRelayState(request, null))
            .isEqualTo(HostedProviderFixture.SECOND_HOME_URL);
    }

    /** So is the allow-list the check reads. */
    @Test
    public void allowListRecoversWhenTheMetaManagerBecomesAvailableAfterClassLoad()
        throws Exception {

        String allowed = "https://trusted2.example.com/landing";
        FSUtils.metaInstance = null;
        // touches the class with no manager available - what an SMS outage at startup looks like
        FSPostLogin.isRelayStateURLValid(
            REALM, HostedProviderFixture.SECOND_ENTITY, IFSConstants.SP, allowed);

        FSUtils.metaInstance = HostedProviderFixture.secondFederatingProvider();

        assertThat(FSPostLogin.isRelayStateURLValid(
            REALM, HostedProviderFixture.SECOND_ENTITY, IFSConstants.SP, allowed)).isTrue();
    }

    @Test
    public void unreadableAllowListRejectsOffHostRelayState() {
        // No SMS is running under test, so the extractor cannot read the allow-list. That must
        // reject rather than fall through to RedirectUrlValidator's "no patterns => allow", and
        // must not escape as a RuntimeException on this unauthenticated endpoint.
        assertThat(FSPostLogin.isRelayStateURLValid(REALM, ENTITY, IFSConstants.SP, OFF_HOST))
            .isFalse();
    }

    @Test
    public void consentToIntroRejectsOffHostLrurl() throws Exception {
        // The introduction URL only resolves for a known, active COT. When it does not - which
        // an attacker gets for free by supplying a bogus cotkey - the LRURL used to come back
        // out of doConsentToIntro unchanged and be forwarded to. With a provider that resolves,
        // the refused RelayState is replaced by that provider's home URL.
        FSUtils.metaInstance = HostedProviderFixture.providerWithAllowList();

        assertThat(new FSPostLogin().doConsentToIntro(consentRequest(OFF_HOST)))
            .isEqualTo(HostedProviderFixture.HOME_URL);
    }

    /**
     * No RelayState at all. The check only ran when one was present, and the private overload
     * appends {@code URLEncDec.encode(targetURL)} to the introduction URL whenever the COT
     * resolves, so a null escaped {@code FSConsentHandler.processRequest} as a 500 with a stack
     * trace - on the same unauthenticated endpoint that answers 400 for a refused RelayState.
     * The provider home URL is the substitution {@code doPostLogin} already makes for it.
     */
    @Test
    public void consentToIntroSubstitutesTheHomeUrlForAnAbsentLrurl() throws Exception {
        FSUtils.metaInstance = HostedProviderFixture.providerWithAllowList();

        assertThat(new FSPostLogin().doConsentToIntro(consentRequest(null)))
            .isEqualTo(HostedProviderFixture.HOME_URL);
    }

    /**
     * The stock consent page, ListOfCOTs.jsp, writes its field as <code>LRURL</code> rather than
     * <code>RelayState</code>, so its Cancel never sends one. That is the case
     * {@code doConsentToIntro} maps to the provider home URL; the cancel branch answered 400.
     */
    @Test
    public void consentCancelWithoutARelayStateGoesToTheHomeUrl() throws Exception {
        FSUtils.metaInstance = HostedProviderFixture.providerWithAllowList();
        HttpServletRequest request = consentRequest(null);
        when(request.getParameter(IFSConstants.USERACTION)).thenReturn(IFSConstants.CANCEL);
        HttpServletResponse response = mock(HttpServletResponse.class);

        new FSConsentHandler().processRequest(request, response);

        verify(response).sendRedirect(HostedProviderFixture.HOME_URL);
    }

    /** Not sent, or sent empty: a form field left blank. */
    @DataProvider(name = "absentRelayState")
    public Object[][] absentRelayState() {
        return new Object[][] { { null }, { "" } };
    }

    /** Nothing was refused on that Cancel, so nothing is reported as refused. */
    @Test(dataProvider = "absentRelayState")
    public void consentCancelWithoutARelayStateReportsNoRefusal(String absent) throws Exception {
        FSUtils.metaInstance = HostedProviderFixture.providerWithAllowList();
        Debug debug = mock(Debug.class);
        when(debug.warningEnabled()).thenReturn(true);
        FSUtils.debug = debug;
        HttpServletRequest request = consentRequest(absent);
        when(request.getParameter(IFSConstants.USERACTION)).thenReturn(IFSConstants.CANCEL);

        new FSConsentHandler().processRequest(request, mock(HttpServletResponse.class));

        verify(debug, never()).warning(contains("RelayState"));
    }

    /** A /consentHandler request naming the hosted provider, with the given LRURL. */
    private static HttpServletRequest consentRequest(String lrurl) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(IFSConstants.META_ALIAS))
            .thenReturn(HostedProviderFixture.META_ALIAS);
        when(request.getParameter(IFSConstants.LRURL)).thenReturn(lrurl);
        return request;
    }

    @Test
    public void consentHandlerRefusesWhenNoSafeTargetResolves() throws Exception {
        HttpServletRequest request = cancelRequestWithLrurl();
        HttpServletResponse response = mock(HttpServletResponse.class);

        new FSConsentHandler().processRequest(request, response);

        // an unwritten response commits an empty 200, which reads as a successful cancel
        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        verify(response, never()).sendRedirect(OFF_HOST);
    }

    @Test
    public void federationHandlerRefusesWhenNoSafeTargetResolves() throws Exception {
        HttpServletRequest request = cancelRequestWithLrurl();
        HttpServletResponse response = mock(HttpServletResponse.class);

        new FSFederationHandler().processRequest(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        verify(response, never()).sendRedirect(OFF_HOST);
    }

    /**
     * The road on which {@code doFederation} produces no URL without throwing: a LECP request
     * whose AuthnRequestEnvelope cannot be built - here the SP signs its AuthnRequests and no
     * signing key alias is configured - leaves {@code createAuthnRequest} nothing to return.
     * Nothing has been written, so the handler has to answer, and nothing else does.
     */
    @Test
    public void federationHandlerRefusesWhenTheLecpEnvelopeCannotBeBuilt() throws Exception {
        IDFFMetaManager metaManager = HostedProviderFixture.federatingProviderWithAllowList();
        SPDescriptorType signing = mock(SPDescriptorType.class);
        when(signing.isAuthnRequestsSigned()).thenReturn(true);
        when(metaManager.getSPDescriptor(nullable(String.class), nullable(String.class)))
            .thenReturn(signing);
        FSUtils.metaInstance = metaManager;
        SystemPropertiesManager.initializeProperties(SIGNING_ON, IFSConstants.TRUE);
        HttpServletResponse response = containerResponse(new ByteArrayOutputStream());

        new FSFederationHandler().processRequest(lecpRequest(), response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        verify(response, never()).getOutputStream();
    }

    /**
     * A LECP request is answered with the AuthnRequestEnvelope in the body rather than with a
     * URL, so the null URL is the expected outcome there and the answer is already committed.
     */
    @Test
    public void federationHandlerAnswersALecpRequestWithTheEnvelopeAlone() throws Exception {
        FSUtils.metaInstance = HostedProviderFixture.federatingProviderWithAllowList();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        HttpServletResponse response = containerResponse(body);

        new FSFederationHandler().processRequest(lecpRequest(), response);

        assertThat(body.toString(StandardCharsets.UTF_8.name()))
            .startsWith("<lib:AuthnRequestEnvelope ");
        verify(response, never()).sendError(anyInt());
    }

    @Test
    public void federationReportsAnEnvelopeItCannotWrite() throws Exception {
        FSUtils.metaInstance = HostedProviderFixture.federatingProviderWithAllowList();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenThrow(new IOException("gone"));

        Throwable thrown =
            catchThrowable(() -> new FSPostLogin().doFederation(lecpRequest(), response));

        assertThat(thrown).isInstanceOf(FSPostLoginException.class);
    }

    /** The envelope could not be written and nothing was sent: the handler answers instead. */
    @Test
    public void federationHandlerRefusesWhenTheLecpEnvelopeCannotBeWritten() throws Exception {
        FSUtils.metaInstance = HostedProviderFixture.federatingProviderWithAllowList();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenThrow(new IOException("gone"));

        new FSFederationHandler().processRequest(lecpRequest(), response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    /**
     * The client went away while the envelope was being flushed: the response is committed
     * before the write fails, so a status would go nowhere and the container would throw.
     */
    @Test
    public void federationHandlerLeavesACommittedAnswerAloneWhenTheEnvelopeWriteFails()
        throws Exception {

        FSUtils.metaInstance = HostedProviderFixture.federatingProviderWithAllowList();
        HttpServletResponse response =
            containerResponse(new ByteArrayOutputStream(), new IOException("Broken pipe"));

        new FSFederationHandler().processRequest(lecpRequest(), response);

        verify(response).flushBuffer();
        verify(response, never()).sendError(anyInt());
    }

    /**
     * {@code Federate.jsp} always fills its RelayState, so a cancel without one did not come from
     * it; there is no page the user asked to return to, and the handler answers 400 rather than
     * picking one.
     */
    @Test
    public void federationCancelWithoutARelayStateIsRefused() throws Exception {
        FSUtils.metaInstance = HostedProviderFixture.providerWithAllowList();
        HttpServletRequest request = consentRequest(null);
        when(request.getParameter(IFSConstants.USERACTION)).thenReturn(IFSConstants.CANCEL);
        HttpServletResponse response = mock(HttpServletResponse.class);

        new FSFederationHandler().processRequest(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        verify(response, never()).sendRedirect(anyString());
    }

    /**
     * A permitted RelayState is still the caller's string, of any length the allow-list's
     * pattern admits; only its first part reaches the log.
     */
    @Test
    public void federationCancelLogsABoundedTarget() throws Exception {
        FSUtils.metaInstance = HostedProviderFixture.providerWithAllowList();
        Debug debug = installedDebug();
        HttpServletRequest request = consentRequest(LONG_ALLOWED);
        when(request.getParameter(IFSConstants.USERACTION)).thenReturn(IFSConstants.CANCEL);

        new FSFederationHandler().processRequest(request, mock(HttpServletResponse.class));

        verify(debug).message("FSFederationHandler::Redirecting to " + LONG_ALLOWED_LOGGED);
    }

    @Test
    public void consentCancelLogsABoundedTarget() throws Exception {
        FSUtils.metaInstance = HostedProviderFixture.providerWithAllowList();
        Debug debug = installedDebug();
        HttpServletRequest request = consentRequest(LONG_ALLOWED);
        when(request.getParameter(IFSConstants.USERACTION)).thenReturn(IFSConstants.CANCEL);

        new FSConsentHandler().processRequest(request, mock(HttpServletResponse.class));

        verify(debug).message("FSConsentHandler:: Redirecting to  " + LONG_ALLOWED_LOGGED);
    }

    /** A debug log with warnings and messages on, installed as the one FSUtils writes to. */
    private static Debug installedDebug() {
        Debug debug = mock(Debug.class);
        when(debug.warningEnabled()).thenReturn(true);
        when(debug.messageEnabled()).thenReturn(true);
        FSUtils.debug = debug;
        return debug;
    }

    /** A LECP client's request to the SP's /federation endpoint. */
    private static HttpServletRequest lecpRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getHeaderNames()).thenReturn(
            Collections.enumeration(Collections.singletonList(IFSConstants.LECP_HEADER_NAME)));
        when(request.getHeader(IFSConstants.LECP_HEADER_NAME))
            .thenReturn("urn:liberty:iff:2003-08");
        when(request.getHeader("Host")).thenReturn("host.example.com:8080");
        when(request.getRequestURL())
            .thenReturn(new StringBuffer("http://host.example.com:8080/openam/federation"));
        when(request.getRequestURI()).thenReturn("/openam/federation");
        when(request.getParameter(IFSConstants.META_ALIAS))
            .thenReturn(HostedProviderFixture.META_ALIAS);
        when(request.getParameter(IFSConstants.SELECTEDPROVIDER))
            .thenReturn("https://idp.example.com/idff");
        return request;
    }

    /** A response that records its body and, as a container's does, commits when flushed. */
    private static HttpServletResponse containerResponse(ByteArrayOutputStream body)
        throws IOException {

        return containerResponse(body, null);
    }

    /**
     * The response of {@link #containerResponse(ByteArrayOutputStream)} whose flush, when
     * {@code flushFailure} is not null, commits and then throws it, as it does once the client
     * has gone away.
     */
    private static HttpServletResponse containerResponse(ByteArrayOutputStream body,
        IOException flushFailure) throws IOException {

        HttpServletResponse response = mock(HttpServletResponse.class);
        boolean[] committed = { false };
        when(response.getOutputStream()).thenReturn(new ServletOutputStream() {
            @Override
            public void write(int b) {
                body.write(b);
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener listener) {
            }
        });
        doAnswer(invocation -> {
            committed[0] = true;
            if (flushFailure != null) {
                throw flushFailure;
            }
            return null;
        }).when(response).flushBuffer();
        when(response.isCommitted()).thenAnswer(invocation -> committed[0]);
        return response;
    }

    /**
     * A cancel request carrying an off-host LRURL and a metaAlias that resolves to nothing:
     * the shape both handlers used to forward straight to.
     */
    private static HttpServletRequest cancelRequestWithLrurl() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(IFSConstants.USERACTION)).thenReturn(IFSConstants.CANCEL);
        when(request.getParameter(IFSConstants.LRURL)).thenReturn(OFF_HOST);
        when(request.getParameter(IFSConstants.META_ALIAS)).thenReturn("/idp");
        return request;
    }
}
