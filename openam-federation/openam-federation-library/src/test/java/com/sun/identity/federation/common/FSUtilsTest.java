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

package com.sun.identity.federation.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.mockito.ArgumentCaptor;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.codecs.HTMLEntityCodec;
import org.testng.annotations.Test;

public class FSUtilsTest {

    private static final String XSS = "\"><script>alert(document.cookie)</script>";

    /**
     * A payload whose only interesting character is the space. encodeForHTML and
     * encodeForHTMLAttribute differ by exactly that one character - IMMUNE_HTML holds the space
     * immune, IMMUNE_HTMLATTR does not - so a space free fixture encodes identically under both and
     * cannot hold the sink's choice of encoder in place.
     */
    private static final String SPACED_XSS = "x onmouseover=alert(1)";

    private static final String AUTO_SUBMIT_JSP = "/saml2/jsp/autosubmitaccessrights.jsp";

    private static final String COOKIE_HASH_REDIRECT =
            "com.sun.identity.federation.cookieHashRedirectEnabled";

    /** The assertion consumer the SAML2 auth module posts to, and the URL the bounce rebuilds. */
    private static final String ACS_URL = "https://openam.example.com/openam/AuthConsumer/metaAlias/sp";

    private static final String QUERY_STRING = "SAMLart=AAQAAM0Nc0d";

    /** The six request attributes autosubmitaccessrights.jsp renders. */
    private static final String[] REFLECTED_ATTRIBUTES = {"TARGET_URL", "SAML_MESSAGE_NAME",
            "SAML_MESSAGE_VALUE", "RELAY_STATE_NAME", "RELAY_STATE_VALUE", "SAML_POST_KEY"};

    /** The five of those six whose value comes from the caller rather than from the bundle. */
    private static final String[] CALLER_SUPPLIED_ATTRIBUTES = {"TARGET_URL", "SAML_MESSAGE_NAME",
            "SAML_MESSAGE_VALUE", "RELAY_STATE_NAME", "RELAY_STATE_VALUE"};

    /**
     * Holds the mocks for one postToTarget call and records the attributes it sets, so that each
     * test can assert against what the JSP would have been handed.
     */
    private static final class Forward {

        private final HttpServletRequest request = mock(HttpServletRequest.class);
        private final HttpServletResponse response = mock(HttpServletResponse.class);
        private final RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        private final Map<String, Object> attributes = new HashMap<>();

        private Forward() {
            when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
            doAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                    .when(request).setAttribute(anyString(), any());
        }

        private String attribute(String name) {
            return (String) attributes.get(name);
        }

        /** Asserts the forward went to the JSP the encoding argument is built around. */
        private void verifyForwarded() throws Exception {
            verify(request).getRequestDispatcher(AUTO_SUBMIT_JSP);
            verify(dispatcher).forward(request, response);
        }
    }

    /**
     * autosubmitaccessrights.jsp renders the attributes set by postToTarget with JSP EL, which does
     * not escape HTML, so every reflected value has to leave postToTarget already encoded. All four
     * caller supplied values carry the payload here: needSetLBCookieAndRedirect takes the message
     * and the relay state straight from request parameters, and builds the target URL from
     * request.getQueryString(), which the attacker controls in full.
     */
    @Test
    public void postToTargetEncodesEveryReflectedValue() throws Exception {
        Forward forward = new Forward();

        FSUtils.postToTarget(forward.request, forward.response, XSS, XSS, XSS, XSS,
                "https://openam.example.com/openam/saml2/jsp/spAssertionConsumer.jsp?redirected=1&x=" + XSS);

        forward.verifyForwarded();

        for (String name : REFLECTED_ATTRIBUTES) {
            assertThat(forward.attributes).containsKey(name);
            assertThat(forward.attribute(name))
                    .as("attribute %s must not carry raw HTML metacharacters", name)
                    .doesNotContain("<").doesNotContain(">").doesNotContain("\"").doesNotContain("'");
        }

        // The submit label is the one attribute whose source is the bundle and not the request, so
        // the loop above would pass for it however it was set. Pin the encoder on it explicitly.
        String submitLabel = SAML2Utils.bundle.getString("samlPostKey");
        assertThat(forward.attribute("SAML_POST_KEY"))
                .as("the submit label goes through the encoder like everything else")
                .isEqualTo(ESAPI.encoder().encodeForHTMLAttribute(submitLabel))
                .isNotEqualTo(submitLabel);
    }

    /**
     * The values are rendered inside value="..." and action="..." attributes, so the sink encodes
     * for attribute context. That is what makes an unquoted attribute - a mistake nobody has made in
     * this JSP yet - a non event rather than an injection point, and it is only visible on a value
     * carrying a space.
     * <p>
     * SAML_POST_KEY is left out: it is not fed the fixture, it comes from the bundle, and it would
     * pass here only for as long as the shipped label happens to contain a space. The encoder is
     * pinned on it directly in {@link #postToTargetEncodesEveryReflectedValue} instead.
     */
    @Test
    public void postToTargetEncodesForAttributeContext() throws Exception {
        Forward forward = new Forward();

        FSUtils.postToTarget(forward.request, forward.response, SPACED_XSS, SPACED_XSS, SPACED_XSS,
                SPACED_XSS,
                "https://openam.example.com/openam/saml2/jsp/spAssertionConsumer.jsp?x=" + SPACED_XSS);

        forward.verifyForwarded();

        for (String name : CALLER_SUPPLIED_ATTRIBUTES) {
            assertThat(forward.attribute(name))
                    .as("attribute %s must be encoded for attribute context, spaces included", name)
                    .contains("&#x20;");
        }
    }

    /**
     * Encoding must not corrupt the payload: a browser HTML-decodes the attribute before posting it
     * on, so every value has to survive the round trip byte for byte. Base64 SAML messages make
     * this worth asserting - '+', '/' and '=' all sit outside the ESAPI HTML immune set.
     * <p>
     * The round trip alone would also hold for values that were never encoded, so each value is
     * additionally pinned as having been encoded at all. Together the two halves rule out both a
     * missing and a doubled layer of escaping.
     */
    @Test
    public void postToTargetKeepsValuesIntactForTheBrowser() throws Exception {
        String samlResponse = "PHNhbWxwOlJlc3BvbnNlIHhtbG5zPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6cHJvdG9jb2wiPz4+//+abc=";
        String relayState = "https://sp.example.com/app?x=1&y=2";
        String targetURL = "https://openam.example.com/openam/saml2/jsp/spAssertionConsumer.jsp?redirected=1&foo=bar";

        Forward forward = new Forward();

        FSUtils.postToTarget(forward.request, forward.response, "SAMLResponse", samlResponse,
                "RelayState", relayState, targetURL);

        forward.verifyForwarded();

        HTMLEntityCodec htmlCodec = new HTMLEntityCodec();
        assertRoundTrips(htmlCodec, forward.attribute("SAML_MESSAGE_VALUE"), samlResponse);
        assertRoundTrips(htmlCodec, forward.attribute("RELAY_STATE_VALUE"), relayState);
        assertRoundTrips(htmlCodec, forward.attribute("TARGET_URL"), targetURL);

        // The two parameter names are what make the browser's re-post land as SAMLResponse and
        // RelayState, so they have to arrive intact as well. They carry nothing the encoder touches,
        // so they are compared directly rather than round-tripped: decoding first would also accept
        // a name that came through mangled into an entity, and a mangled name is exactly what would
        // make the re-post land as something else.
        assertThat(forward.attribute("SAML_MESSAGE_NAME")).isEqualTo("SAMLResponse");
        assertThat(forward.attribute("RELAY_STATE_NAME")).isEqualTo("RelayState");
    }

    private void assertRoundTrips(HTMLEntityCodec htmlCodec, String encoded, String raw) {
        assertThat(encoded).as("value must have been encoded at all").isNotEqualTo(raw);
        assertThat(htmlCodec.decode(encoded))
                .as("the browser decodes the attribute before re-posting it").isEqualTo(raw);
    }

    /**
     * The auto submit page carries a bearer assertion, so it must not be cached.
     */
    @Test
    public void postToTargetSetsNoCacheHeaders() throws Exception {
        Forward forward = new Forward();

        FSUtils.postToTarget(forward.request, forward.response, "SAMLResponse", "abc", "RelayState",
                "state", "https://openam.example.com/openam/saml2/jsp/spAssertionConsumer.jsp");

        verify(forward.response).setHeader("Pragma", "no-cache");
        verify(forward.response).setHeader("Cache-Control", "no-cache,no-store");
    }

    /**
     * A null relay state is legitimate - needSetLBCookieAndRedirect passes on whatever the RelayState
     * parameter was - and has to stay null so that the JSP's own null check suppresses the input
     * rather than rendering the string "null".
     */
    @Test
    public void postToTargetAcceptsNullRelayState() throws Exception {
        Forward forward = new Forward();

        FSUtils.postToTarget(forward.request, forward.response, "SAMLResponse", "abc", "RelayState",
                null, "https://openam.example.com/openam/saml2/jsp/spAssertionConsumer.jsp");

        forward.verifyForwarded();
        assertThat(forward.attributes).containsKey("RELAY_STATE_VALUE");
        assertThat(forward.attribute("RELAY_STATE_VALUE")).isNull();
    }

    /**
     * Both callers of postToTarget depend on a failed forward arriving as a SAML2Exception, since
     * that is the only checked exception they catch besides IOException.
     */
    @Test
    public void postToTargetReportsAServletExceptionFromTheForwardAsSaml2Exception() throws Exception {
        Forward forward = new Forward();
        doThrow(new ServletException("boom")).when(forward.dispatcher).forward(forward.request, forward.response);

        assertThatThrownBy(() -> FSUtils.postToTarget(forward.request, forward.response, "SAMLResponse",
                "abc", "RelayState", null, "https://openam.example.com/openam/saml2/jsp/spAssertionConsumer.jsp"))
                .isInstanceOf(SAML2Exception.class);
    }

    @Test
    public void postToTargetReportsAnIoExceptionFromTheForwardAsSaml2Exception() throws Exception {
        Forward forward = new Forward();
        doThrow(new IOException("boom")).when(forward.dispatcher).forward(forward.request, forward.response);

        assertThatThrownBy(() -> FSUtils.postToTarget(forward.request, forward.response, "SAMLResponse",
                "abc", "RelayState", null, "https://openam.example.com/openam/saml2/jsp/spAssertionConsumer.jsp"))
                .isInstanceOf(SAML2Exception.class);
    }

    /**
     * A forward that fails before committing leaves the response usable, so the caller keeps
     * ownership of it and carries on as it always did - but whatever the JSP managed to buffer has
     * to be dropped first, or the caller's page would be appended to a half rendered auto submit
     * form that still carries the SAML message and still auto submits.
     */
    @Test
    public void aFailedBounceOnAnUntouchedResponseLetsTheCallerCarryOnWithACleanBuffer() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.isCommitted()).thenReturn(false);

        assertThat(FSUtils.requireStopAfterFailedBounce(response, new SAML2Exception("boom"))).isFalse();
        verify(response).resetBuffer();
    }

    /**
     * Once the JSP has flushed part of the auto submit page the response is spoiled: carrying on
     * would consume the one-time-use assertion and then try to sendError or sendRedirect on a
     * committed response. The caller has to stop instead.
     */
    @Test
    public void aFailedBounceOnACommittedResponseStopsTheCaller() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.isCommitted()).thenReturn(true);

        assertThat(FSUtils.requireStopAfterFailedBounce(response, new SAML2Exception("boom"))).isTrue();
        // resetBuffer() on a committed response is an IllegalStateException, and there is nothing
        // left to drop anyway.
        verify(response, never()).resetBuffer();
    }

    /**
     * The whole load balancer cookie bounce hangs off this one flag, and it is off unless a
     * deployment turns it on - which is what bounds the blast radius of repairing the bounce.
     */
    @Test
    public void requireRedirectIsOffUnlessCookieHashRedirectIsEnabled() {
        // The surefire fork shares one process wide property map, so record what was there and put
        // it back. initializeProperties only ever adds, so an unset flag is restored as "false" -
        // requireRedirect treats the two identically. That also means this test cannot demand an
        // unset flag on entry: a second execution in the same JVM (rerunFailingTestsCount, a suite
        // listing this class twice) would then fail on its own fixture and read like a regression.
        String original = SystemPropertiesManager.get(COOKIE_HASH_REDIRECT);
        assertThat(original).as("the flag is not enabled before this test runs").isIn(null, "false");

        HttpServletRequest request = mock(HttpServletRequest.class);
        try {
            assertThat(FSUtils.requireRedirect(request)).as("off by default").isFalse();
            verifyZeroInteractions(request);

            SystemPropertiesManager.initializeProperties(COOKIE_HASH_REDIRECT, "true");
            assertThat(FSUtils.requireRedirect(request)).as("on once the flag is set").isTrue();

            when(request.getParameter("redirected")).thenReturn("1");
            assertThat(FSUtils.requireRedirect(request)).as("a request already bounced once").isFalse();
        } finally {
            SystemPropertiesManager.initializeProperties(COOKIE_HASH_REDIRECT,
                    original == null ? "false" : original);
        }
    }

    /**
     * A request arriving at one node of a multi server site without the load balancer cookie, which
     * is the only shape the bounce acts on. The second server of the site comes from
     * {@link StubPlatformConfiguration}, which the module's test FederationConfig.properties
     * registers as the configuration plugin.
     */
    private HttpServletRequest requestWithoutLbCookie(String method) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("openam.example.com");
        when(request.getServerPort()).thenReturn(443);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURL()).thenReturn(new StringBuffer(ACS_URL));
        when(request.getQueryString()).thenReturn(QUERY_STRING);
        return request;
    }

    /**
     * Runs the bounce with the deployment flag on and puts the flag back however the call ended.
     * initializeProperties can only add keys, so "off" is restored as "false", which requireRedirect
     * treats exactly as it treats an unset flag.
     */
    private boolean bounce(HttpServletRequest request, HttpServletResponse response) {
        SystemPropertiesManager.initializeProperties(COOKIE_HASH_REDIRECT, "true");
        try {
            return FSUtils.needSetLBCookieAndRedirect(request, response, false);
        } finally {
            SystemPropertiesManager.initializeProperties(COOKIE_HASH_REDIRECT, "false");
        }
    }

    /**
     * The gate as the thirteen call sites meet it: on a deployment that has not turned the flag on,
     * a cookie-less request on a multi server site is never bounced - the caller keeps the response
     * and carries on as it always did.
     * <p>
     * Not "nothing happens": setlbCookie runs before requireRedirect is consulted, so a deployment
     * with the flag off still has the load balancer cookie added to such a response. That is the
     * point of the flag - it gates the redirect, not the cookie. Nothing lands on this mock only
     * because {@link StubPlatformConfiguration} declares no cookie domains, which is why the
     * assertions below are about the bounce and not about the cookie.
     */
    @Test
    public void theBounceDoesNothingUntilTheDeploymentTurnsItOn() throws Exception {
        HttpServletRequest request = requestWithoutLbCookie("GET");
        HttpServletResponse response = mock(HttpServletResponse.class);

        assertThat(FSUtils.needSetLBCookieAndRedirect(request, response, false)).isFalse();

        verify(response, never()).sendRedirect(any());
        verify(request, never()).getRequestDispatcher(anyString());
    }

    /**
     * The GET half of the bounce: the browser is sent back to the same URL with redirected=1 so that
     * the load balancer can route it to the node that owns the request, and the caller is told the
     * request has been answered.
     */
    @Test
    public void aGetBounceAnswersTheRequestAndTellsTheCallerToStop() throws Exception {
        HttpServletRequest request = requestWithoutLbCookie("GET");
        HttpServletResponse response = mock(HttpServletResponse.class);

        assertThat(bounce(request, response)).isTrue();

        ArgumentCaptor<String> redirect = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirect.capture());
        assertThat(redirect.getValue()).isEqualTo(ACS_URL + "?redirected=1&" + QUERY_STRING);
    }

    /**
     * The POST half: the SAML message is re-posted through the auto submit JSP, aimed at the same
     * bounced URL, and the caller is told to stop. This is the path the advisory is about, driven
     * from the entry point the thirteen call sites use rather than from postToTarget directly.
     */
    @Test
    public void aPostBounceRePostsThroughTheAutoSubmitJspAndTellsTheCallerToStop() throws Exception {
        String samlResponse = "PHNhbWxwOlJlc3BvbnNlLz4=";
        HttpServletRequest request = requestWithoutLbCookie("POST");
        when(request.getParameter("SAMLResponse")).thenReturn(samlResponse);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
        Map<String, Object> attributes = new HashMap<>();
        doAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        HttpServletResponse response = mock(HttpServletResponse.class);

        assertThat(bounce(request, response)).isTrue();

        verify(request).getRequestDispatcher(AUTO_SUBMIT_JSP);
        verify(dispatcher).forward(request, response);
        assertThat(attributes.get("SAML_MESSAGE_NAME")).isEqualTo("SAMLResponse");
        assertThat(attributes.get("SAML_MESSAGE_VALUE"))
                .isEqualTo(ESAPI.encoder().encodeForHTMLAttribute(samlResponse));
        assertThat(attributes.get("TARGET_URL")).isEqualTo(
                ESAPI.encoder().encodeForHTMLAttribute(ACS_URL + "?redirected=1&" + QUERY_STRING));
    }

    /**
     * The failure this advisory closes, driven through the entry point: the forward fails after the
     * JSP has flushed part of the auto submit page, and needSetLBCookieAndRedirect has to route the
     * SAML2Exception its own catch block sees into a "stop" rather than the "carry on" every caller
     * reads a false as.
     */
    @Test
    public void aFailedPostBounceOnACommittedResponseStopsTheCaller() throws Exception {
        HttpServletRequest request = requestWithoutLbCookie("POST");
        when(request.getParameter("SAMLResponse")).thenReturn("PHNhbWxwOlJlc3BvbnNlLz4=");
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
        HttpServletResponse response = mock(HttpServletResponse.class);
        doThrow(new ServletException("boom")).when(dispatcher).forward(request, response);
        when(response.isCommitted()).thenReturn(true);

        assertThat(bounce(request, response)).isTrue();

        verify(dispatcher).forward(request, response);
        // resetBuffer() on a committed response is an IllegalStateException.
        verify(response, never()).resetBuffer();
    }

    /**
     * The same for the GET half, whose failure arrives as an IOException out of sendRedirect and
     * lands in the other catch block.
     */
    @Test
    public void aFailedGetBounceOnACommittedResponseStopsTheCaller() throws Exception {
        HttpServletRequest request = requestWithoutLbCookie("GET");
        HttpServletResponse response = mock(HttpServletResponse.class);
        doThrow(new IOException("client gone")).when(response).sendRedirect(anyString());
        when(response.isCommitted()).thenReturn(true);

        assertThat(bounce(request, response)).isTrue();

        verify(response, never()).resetBuffer();
    }

    /**
     * A bounce that failed with its output still buffered leaves the caller in charge - and hands it
     * back a response with the half rendered auto submit form dropped.
     */
    @Test
    public void aFailedGetBounceOnAnUncommittedResponseLeavesTheCallerInCharge() throws Exception {
        HttpServletRequest request = requestWithoutLbCookie("GET");
        HttpServletResponse response = mock(HttpServletResponse.class);
        doThrow(new IOException("client gone")).when(response).sendRedirect(anyString());
        when(response.isCommitted()).thenReturn(false);

        assertThat(bounce(request, response)).isFalse();

        verify(response).resetBuffer();
    }
}
