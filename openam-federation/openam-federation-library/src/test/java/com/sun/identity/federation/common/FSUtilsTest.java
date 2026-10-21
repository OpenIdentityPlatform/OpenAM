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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.mockito.ArgumentCaptor;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.codecs.HTMLEntityCodec;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;

public class FSUtilsTest {

    // FSUtils.deploymentURI is a public static read live by isSameContainer and forwardRequest,
    // so it has to be set for these to mean anything. FSUtilsForwardRewriteTest and
    // FSUtilsForwardPathTest set the same value, so all three take FederationStaticsLock rather
    // than relying on surefire running this module single-threaded.
    private String originalDeploymentURI;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private RequestDispatcher dispatcher;
    private Debug originalDebug;
    private Debug debug;

    @BeforeMethod
    public void setUp() {
        FederationStaticsLock.acquire();
        originalDeploymentURI = FSUtils.deploymentURI;
        FSUtils.deploymentURI = "/openam";
        originalDebug = FSUtils.debug;
        debug = mock(Debug.class);
        FSUtils.debug = debug;

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        dispatcher = mock(RequestDispatcher.class);

        when(request.getServerName()).thenReturn("host.example.com");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        try {
            FSUtils.deploymentURI = originalDeploymentURI;
            FSUtils.debug = originalDebug;
            DeploymentIdentityFixture.restore();
        } finally {
            FederationStaticsLock.release();
        }
    }

    @DataProvider(name = "notThisDeployment")
    public Object[][] notThisDeployment() {
        return new Object[][] {
            { "http://host.example.com:9090/openam/landing" },   // another port
            { "https://host.example.com:8080/openam/landing" },  // another scheme
            { "http://host.example.com:8080/other/landing" },    // another application
            { "http://host.example.com:8080/openamx/landing" }   // shares the name as a prefix
        };
    }

    /**
     * {@code isDeploymentURL} is the whole allow-list for every ID-FF entity that configures
     * none, so each probe here differs from the configured http://host.example.com:8080/openam
     * in exactly one part.
     */
    @Test(dataProvider = "notThisDeployment")
    public void shouldNotTreatAnotherOriginOrPathAsThisDeployment(String url) throws Exception {
        DeploymentIdentityFixture.installServer("http", "host.example.com", "8080", "/openam");
        DeploymentIdentityFixture.installServerList((String[]) null);

        assertThat(FSUtils.isDeploymentURL(url)).isFalse();
    }

    @DataProvider(name = "thisDeployment")
    public Object[][] thisDeployment() {
        return new Object[][] {
            // the default port is left out of the URL but written into the configuration
            { "https", "443", "https://host.example.com/openam/landing" },
            // host names are case insensitive, and browsers send them lower cased anyway
            { "http", "8080", "http://HOST.EXAMPLE.COM:8080/openam/landing" }
        };
    }

    @Test(dataProvider = "thisDeployment")
    public void shouldTreatThisDeploymentsOwnURLAsThisDeployment(String protocol, String port,
        String url) throws Exception {

        DeploymentIdentityFixture.installServer(protocol, "host.example.com", port, "/openam");
        DeploymentIdentityFixture.installServerList((String[]) null);

        assertThat(FSUtils.isDeploymentURL(url)).isTrue();
    }

    /**
     * {@code deploymentURI} is read once, when the class loads, which can be before the
     * properties are; the deployment's identity is read when it is asked for.
     */
    @Test
    public void shouldReadThisDeploymentsIdentityWhenAsked() throws Exception {
        DeploymentIdentityFixture.installServer("http", "host.example.com", "8080", "/openam");
        DeploymentIdentityFixture.installServerList((String[]) null);
        FSUtils.isDeploymentURL("http://host.example.com:8080/openam/landing");

        DeploymentIdentityFixture.installServer("http", "moved.example.com", "8080", "/sso");

        assertThat(FSUtils.isDeploymentURL("http://moved.example.com:8080/sso/landing")).isTrue();
    }

    @DataProvider(name = "anotherServerOrSite")
    public Object[][] anotherServerOrSite() {
        return new Object[][] {
            // a site in front of this server: platform entries carry the port explicitly
            { new String[] { "https://sso.example.com:443/openam" },
                "https://sso.example.com/openam/landing" },
            // deployed as /OpenAM, which the list only holds lower cased
            { new String[] { "https://sso.example.com:443/OpenAM" },
                "https://sso.example.com/OpenAM/landing" },
            // an entry is only split on '|' when loaded, so one without a scheme gets in
            { new String[] { "sso.example.com:443/openam", "https://sso.example.com:443/openam" },
                "https://sso.example.com/openam/landing" }
        };
    }

    /**
     * The platform server list names every server and site of the deployment, and is only
     * reachable through {@code SystemConfigurationUtil}, which lower cases each entry.
     */
    @Test(dataProvider = "anotherServerOrSite")
    public void shouldTreatAServerOrSiteOfThePlatformListAsThisDeployment(String[] serverList,
        String url) throws Exception {

        DeploymentIdentityFixture.installServer("http", "host.example.com", "8080", "/openam");
        DeploymentIdentityFixture.installServerList(serverList);

        assertThat(FSUtils.isDeploymentURL(url)).isTrue();
    }

    /**
     * {@code getRemoteServiceURLs} removes the sites from the list {@code getServerList} hands
     * out - the shared one - so after its first call a site is only in {@code getSiteList}.
     */
    @Test
    public void shouldTreatASiteAsThisDeploymentAfterTheServerListLostIt() throws Exception {
        DeploymentIdentityFixture.installServer("http", "host.example.com", "8080", "/openam");
        DeploymentIdentityFixture.installServerList(
            "http://host.example.com:8080/openam", "https://sso.example.com:443/openam");
        DeploymentIdentityFixture.installSiteList("https://sso.example.com:443/openam");
        FSUtils.getRemoteServiceURLs(request);

        assertThat(FSUtils.isDeploymentURL("https://sso.example.com/openam/landing")).isTrue();
    }

    @Test
    public void shouldNotTreatAnotherServerAsThisDeploymentWhenTheServerListCannotBeRead()
        throws Exception {

        DeploymentIdentityFixture.installServer("http", "host.example.com", "8080", "/openam");
        DeploymentIdentityFixture.installUnreadableServerList();

        assertThat(FSUtils.isDeploymentURL("https://sso.example.com/openam/landing")).isFalse();
    }

    /** Where the platform list cannot be read, the server's own properties still can. */
    @Test
    public void shouldTreatThisServersOwnURLAsThisDeploymentWhenTheServerListCannotBeRead()
        throws Exception {

        DeploymentIdentityFixture.installServer("http", "host.example.com", "8080", "/openam");
        DeploymentIdentityFixture.installUnreadableServerList();

        assertThat(FSUtils.isDeploymentURL("http://host.example.com:8080/openam/landing"))
            .isTrue();
    }

    @DataProvider(name = "allowedForwardTargets")
    public Object[][] allowedForwardTargets() {
        return new Object[][] {
            { "/wsfederation/jsp/mex.jsp" },
            { "/config/federation/default/cdclogin.jsp" },
            { "/UI/Login?goto=x" },
            { "/index.html" },
            { "/websocket-docs/readme.html" },
            // a correctly encoded literal percent: URLEncDec.decode threw on the second
            // decoding round and answered a legitimate page with a 400
            { "/config/federation/default/100%25done.jsp" },
            { "/jsp/report%25.jsp" },
            // '+' is not a space in a dispatch path, and either reading is harmless here
            { "/jsp/my+page.jsp" },
            // a container strips ';jsessionid' before mapping, so a rewritten URL still has
            // to be forwardable - the guard must not simply refuse every ';'
            { "/jsp/x;jsessionid=ABC" },
            { "/config/federation/default/cdclogin.jsp;jsessionid=ABC?goto=x" }
        };
    }

    @DataProvider(name = "deniedForwardTargets")
    public Object[][] deniedForwardTargets() {
        return new Object[][] {
            { "/notifications" },
            { "/notifications?x=1" },
            { "//notifications" },
            { "/./notifications" },
            { "/foo/../notifications" },
            { "/NOTIFICATIONS" },
            { "/%6eotifications" },
            { "/foo/..//./notifications" },
            { "\\notifications" },
            { "/ws" },
            { "/ws/" },
            { "/ws/Test?a=b" },
            { "/foo/../ws/Test" },
            { "/../../ws/Test" },
            { "/notifications;jsessionid=abc" },
            { "/ws;a=b/Test" },
            { "/%252e%252e/ws/Test" },
            { "/foo/%252e%252e/notifications" },
            // a percent-encoded delimiter becomes a real one once decoded, so the
            // query/fragment strip has to run again after every decode round
            { "/notifications%3Fx=1" },
            { "/notifications%3fx=1" },
            { "/notifications%23frag" },
            { "/ws%3Fa=b" },
            { "/%6eotifications%3Fx=1" },
            // a decoded delimiter must not swallow the traversal that follows it
            { "/x%3F/../notifications" },
            { "/x%23/../notifications" },
            { "/%3F/../notifications" },
            { "/foo%3F/../ws/Test" },
            { "/a%3f/../ws" },
            { "/a%3F/..%2fnotifications" },
            { "/foo%253F/../../ws/Test" },
            // ... and the traversal must still be caught when a real query
            // string follows it, i.e. when the fold has to happen before the split
            { "/x%3F/../notifications%3Fq=1" },
            { "/x%3F/../notifications?q=1" },
            { "/x%3F/..%2fnotifications#f" },
            { "/x%3F/%2e%2e/notifications?q=1" },
            // JAXRPCRequestFilter authenticates /jaxrpc/*; it is REQUEST-only by default, so
            // a forward there would reach the endpoint with the auth filter skipped
            { "/jaxrpc" },
            { "/jaxrpc/" },
            { "/jaxrpc/IdentityManagementServices" },
            { "/foo/../jaxrpc/x" },
            // a malformed escape is not a valid dispatch path: UDecoder throws out of
            // getRequestDispatcher and leaves the response uncommitted, so refuse it here
            // and answer 400 rather than let the container commit a blank 200
            { "/notifications%" },
            { "/ws%/Test" },
            { "/%6eotifications%" },
            // verified against Tomcat 10.1.41's getRequestDispatcher pipeline ('?'-cut,
            // stripPathParams, UDecoder, normalize): each of these maps into an internal
            // root, while a guard that folds ';' per segment reads them as harmless
            { "/notifications;\\.." },
            { "/;\\a/notifications" },
            { "/a#/../notifications?/.." },
            { "/ws%5c..%3b" },
            { "/jaxrpc%2F..%3bx" },
            { "/jaxrpc;x" },
            { "/ws;a/b" },
            // one row per alternative reading the guard takes, each refused by that reading
            // alone. No container is known to read them that way; the readings are kept so
            // that a container which does is covered too, and a row fails if its reading goes.
            // Parameters left in place:
            { "/ws/..;x" },
            // parameters cut to the end of the segment, after decoding and before it:
            { "/a/..%3by/ws/%252e%252e/..%25/..%25" },
            { "/w%73/;%2f..%2f..\\y/.." },
            // a stray '%' dropped, parameters then cut to the next '/' and to the segment end:
            { "/w%25s;\\.." },
            { "/x/..;y\\w%25s" },
            // '+' read as a space:
            { "/ws/%252e%252e/..%25/..%25/+%3F/../../../../.." },
            // the target cut at its fragment, and not cut at all:
            { "/ws#/.." },
            { "/x?/../ws" },
            // a fragment that only surfaced during decoding, cut after folding:
            { "/x%23/../ws%23" },
            // '\' ending a segment's parameters as '/' does:
            { "/;x\\ws" },
            // a '%' not followed by two hex digits, whatever else it decodes to
            { "/index%z4.html" },
            { "/index%4z.html" },
            { "/index%zz.html" },
            // a decoded NUL: containers disagree on whether it truncates the path
            { "/notifications%00" },
            { "/notifications%2500" },
            // ... as many times encoded as the guard decodes
            { "/notifications%25252500" },
            // DEL is a control character too
            { "/index%7f.html" },
            { null }
        };
    }

    @Test(dataProvider = "allowedForwardTargets")
    public void shouldAllowLegitimateForwardTargets(String resource) {
        assertThat(FSUtils.isForwardTargetAllowed(resource)).isTrue();
    }

    @Test(dataProvider = "deniedForwardTargets")
    public void shouldDenyInternalForwardTargets(String resource) {
        assertThat(FSUtils.isForwardTargetAllowed(resource)).isFalse();
    }

    /**
     * One row per root whose filter-mappings in web.xml name no dispatcher, so the filter runs
     * on a REQUEST dispatch only and a forward reaches the endpoint without it.
     */
    @DataProvider(name = "requestOnlyFilteredTargets")
    public Object[][] requestOnlyFilteredTargets() {
        return new Object[][] {
            // FQDNValidationFilter, CacheForFiveMinutes, CacheForAMonth
            { "/XUI/" },
            // FQDNValidationFilter, CORSFilter
            { "/oauth2/authorize?client_id=x" },
            // CORSFilter
            { "/json/users" },
            // JatoAuditFilter
            { "/service/x" },
            { "/federation/x" },
            { "/realm/x" },
            { "/agentconfig/x" },
            { "/sts/x" },
            { "/delegation/x" },
            // JatoAuditFilter, XUIFilter
            { "/idm/EndUser" },
            { "/Debug.jsp" },
            // SsoAdmJspAuditFilter
            { "/ssoadm.jsp" },
            // the container maps these into /XUI and /json once it has decoded the path and
            // stripped its parameters
            { "/%58UI/" },
            { "/%2558UI/" },
            { "/XUI;x/index.html" },
            { "/foo/..%2fjson/users" }
        };
    }

    @Test(dataProvider = "requestOnlyFilteredTargets")
    public void shouldTellATargetWhoseFiltersRunOnRequestOnly(String resource) {
        assertThat(FSUtils.hasRequestOnlyFilters(resource)).isTrue();
    }

    /** Every filter mapped on these runs on a forward as well, or runs on /* for the request. */
    @DataProvider(name = "fullyFilteredTargets")
    public Object[][] fullyFilteredTargets() {
        return new Object[][] {
            { "/config/federation/default/LogoutDone.jsp" },
            { "/wsfederation/jsp/mex.jsp" },
            // XUIFilter declares FORWARD on /UI/*
            { "/UI/Login?goto=x" },
            { "/index.html" },
            // a root is a whole segment
            { "/jsonp/x" },
            { "/xuix/" },
            { "/Debug.jspx" }
        };
    }

    @Test(dataProvider = "fullyFilteredTargets")
    public void shouldNotTellAFullyFilteredTargetApart(String resource) {
        assertThat(FSUtils.hasRequestOnlyFilters(resource)).isFalse();
    }

    /**
     * FQDNValidationFilter is mapped on /oauth2/authorize for REQUEST only, so a forward there
     * reaches the endpoint without it. A redirect is a request of the browser's own, which the
     * filter sees.
     */
    @Test
    public void shouldRedirectRatherThanForwardPastARequestOnlyFilter() throws Exception {
        String target = "http://host.example.com:8080/openam/oauth2/authorize?client_id=x";

        FSUtils.forwardRequest(request, response, target);

        verify(response).sendRedirect(target);
        verify(dispatcher, never()).forward(any(), any());
    }

    @Test
    public void shouldRefuseForwardToInternalResource() throws Exception {
        FSUtils.forwardRequest(request, response,
            "http://host.example.com:8080/openam/notifications");

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        verify(dispatcher, never()).forward(any(), any());
    }

    /**
     * A 400 that cannot be written is logged where it failed. Left to the catch around the
     * dispatch, it was followed by a 500 on the same response.
     */
    @Test
    public void shouldNotWriteTwiceWhenTheRefusalCannotBeWritten() throws Exception {
        doThrow(new IOException("gone")).when(response).sendError(anyInt());

        FSUtils.forwardRequest(request, response,
            "http://host.example.com:8080/openam/notifications");

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        verify(response, times(1)).sendError(anyInt());
    }

    @Test
    public void shouldForwardToAllowedResource() throws Exception {
        FSUtils.forwardRequest(request, response,
            "http://host.example.com:8080/openam/wsfederation/jsp/mex.jsp");

        verify(dispatcher).forward(request, response);
        verify(response, never()).sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    /**
     * Several of the values passed through here are query parameters on unauthenticated
     * endpoints, logged before any length limit is applied to them - a loop of requests
     * carrying an 8KB RelayState would otherwise write 8KB apiece to the debug file.
     */
    @Test
    public void shouldBoundTheLengthOfALoggedValue() {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < 8192; i++) {
            value.append('a');
        }

        String logged = FSUtils.forLog(value.toString());

        assertThat(logged.length()).isLessThan(512);
        assertThat(logged).startsWith("aaaa").contains("8192");
    }

    @Test
    public void shouldLeaveAShortValueIntactApartFromLineBreaks() {
        assertThat(FSUtils.forLog("https://trusted.example.com/a\nb"))
            .isEqualTo("https://trusted.example.com/a_b");
        assertThat(FSUtils.forLog("https://trusted.example.com/a\rb"))
            .isEqualTo("https://trusted.example.com/a_b");
        assertThat(FSUtils.forLog(null)).isEqualTo("null");
    }

    /** A CR or LF in a refused target reaches the log line that says why it was refused. */
    @Test
    public void shouldLogARefusedNonAbsoluteTargetOnOneLine() throws Exception {
        FSUtils.forwardRequest(request, response, "/console\r\nX");

        verify(debug).error("FSUtils.forwardRequest: target is not an absolute URL:/console__X");
    }

    @Test
    public void shouldLogARefusedInternalTargetOnOneLine() throws Exception {
        when(debug.warningEnabled()).thenReturn(true);

        FSUtils.forwardRequest(request, response,
            "http://host.example.com:8080/openam/notifications\r\nX");

        verify(debug).warning(
            "FSUtils.forwardRequest: refused forward to internal resource: /notifications__X");
    }

    /**
     * A target that gets as far as the dispatcher carries no CR or LF - those are refused
     * first - so on the lines below all forLog has left to do is bound its length.
     */
    private static final String LONG_TARGET =
        "http://host.example.com:8080/openam/wsfederation/jsp/mex.jsp?x=" + "a".repeat(300);
    private static final String LONG_RESOURCE_LOGGED =
        "/wsfederation/jsp/mex.jsp?x=" + "a".repeat(228) + "...[truncated, 328 characters]";

    @Test
    public void shouldBoundTheLoggedLengthOfAForwardedTarget() throws Exception {
        when(debug.messageEnabled()).thenReturn(true);

        FSUtils.forwardRequest(request, response, LONG_TARGET);

        verify(debug).message("FSUtils.forwardRequest: Forwarding to :" + LONG_RESOURCE_LOGGED);
    }

    @Test
    public void shouldBoundTheLoggedLengthOfATargetWithNoDispatcher() throws Exception {
        when(request.getRequestDispatcher(anyString())).thenReturn(null);

        FSUtils.forwardRequest(request, response, LONG_TARGET);

        verify(debug).error("FSUtils.forwardRequest: container returned no dispatcher for "
            + "resource:" + LONG_RESOURCE_LOGGED);
    }

    @Test
    public void shouldBoundTheLoggedLengthOfATargetWhoseForwardFailed() throws Exception {
        ServletException failure = new ServletException("boom");
        doThrow(failure).when(dispatcher).forward(any(), any());

        FSUtils.forwardRequest(request, response, LONG_TARGET);

        verify(debug).error(eq("FSUtils.forwardRequest: Exception occured while trying to "
            + "forward to resource:" + LONG_RESOURCE_LOGGED), eq(failure));
    }

    /**
     * A FORWARD dispatch runs only the filters that declare
     * {@code <dispatcher>FORWARD</dispatcher>}, and the {@code /*} security filters in web.xml
     * - AuditContextFilter, amSetupFilter, SecurityHeaders, ResponseValidationFilter - do not.
     * So the forward/redirect decision reads the port exactly as written: a same-host target
     * with no port has always been redirected to, and reading it as the scheme's default so
     * that URL rewriting keeps working must not move it onto the forward branch.
     */
    @Test
    public void shouldRedirectRatherThanForwardWhenTheTargetWritesNoPort() throws Exception {
        String target = "http://host.example.com/openam/wsfederation/jsp/mex.jsp";
        when(request.getServerPort()).thenReturn(80);

        FSUtils.forwardRequest(request, response, target);

        verify(response).sendRedirect(target);
        verify(dispatcher, never()).forward(any(), any());
    }

    @DataProvider(name = "offContainerTargets")
    public Object[][] offContainerTargets() {
        return new Object[][] {
            { "https://evil.example/openam/landing" },   // other host
            { "http://host.example.com:9090/openam/x" }, // other port
            { "http://host.example.com:8080/other/x" },  // other deployment URI
            { "not a url" }
        };
    }

    /**
     * {@code forwardRequest} gates {@code SessionProvider.rewriteURL} on this predicate, so a
     * false here is what keeps a URL-rewritten session token off a third party's host. The
     * wiring itself is covered by {@link FSUtilsForwardRewriteTest}.
     */
    @Test(dataProvider = "offContainerTargets")
    public void shouldNotTreatOffContainerTargetAsSameContainer(String url) {
        assertThat(FSUtils.isSameContainer(request, url)).isFalse();
    }

    @Test
    public void shouldTreatSameContainerTargetAsSameContainer() {
        assertThat(FSUtils.isSameContainer(request,
            "http://host.example.com:8080/openam/wsfederation/jsp/mex.jsp")).isTrue();
    }

    @DataProvider(name = "implicitPortTargets")
    public Object[][] implicitPortTargets() {
        return new Object[][] {
            { 80, "http://host.example.com/openam/landing" },
            { 443, "https://host.example.com/openam/landing" }
        };
    }

    /**
     * {@code URL.getPort()} is -1 when the port is not written out, while
     * {@code getServerPort()} always reports a number. Comparing the two raw read the normal
     * way an admin writes a home or RelayState URL as a different container, which skipped
     * {@code rewriteURL} and dropped the session token for cookie-less clients.
     */
    @Test(dataProvider = "implicitPortTargets")
    public void shouldTreatImplicitPortTargetAsSameContainer(int serverPort, String url) {
        when(request.getServerPort()).thenReturn(serverPort);

        assertThat(FSUtils.isSameContainer(request, url)).isTrue();
    }

    @Test
    public void shouldNotTreatImplicitPortTargetOnAnotherPortAsSameContainer() {
        // the request is on 8080, the target's implicit port is 80
        assertThat(FSUtils.isSameContainer(request,
            "http://host.example.com/openam/landing")).isFalse();
    }

    @DataProvider(name = "nonAbsoluteTargets")
    public Object[][] nonAbsoluteTargets() {
        return new Object[][] {
            { "/console" },
            { "console/index.html" },
            { "//evil.example/openam/landing" }
        };
    }

    /**
     * A relative target is neither dispatchable nor safe to redirect to - "//host/x" is a 302
     * to another host - and {@code new URL} used to throw here, leaving the container to
     * commit an empty 200.
     */
    @Test(dataProvider = "nonAbsoluteTargets")
    public void shouldRefuseNonAbsoluteTarget(String url) throws Exception {
        FSUtils.forwardRequest(request, response, url);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        verify(response, never()).sendRedirect(anyString());
        verify(dispatcher, never()).forward(any(), any());
    }

    @Test
    public void shouldLeaveACommittedResponseAloneWhenRefusingANonAbsoluteTarget()
        throws Exception {

        when(response.isCommitted()).thenReturn(true);

        FSUtils.forwardRequest(request, response, "/console");

        verify(response, never()).sendError(anyInt());
    }

    /**
     * A container returns null from {@code getRequestDispatcher} for a path it will not map -
     * Tomcat does so when the decoded path normalizes differently, e.g. "/%2e%2e/x". Forwarding
     * on null NPEs into the catch below and leaves an empty 200 committed.
     * <p>
     * {@link ForwardPathValidator} now refuses that form before a dispatcher is asked for, so
     * the probe is a plain path and the mock stands in for a container declining to map it.
     */
    @Test
    public void shouldRefuseWhenContainerReturnsNoDispatcher() throws Exception {
        when(request.getRequestDispatcher(anyString())).thenReturn(null);

        FSUtils.forwardRequest(request, response,
            "http://host.example.com:8080/openam/wsfederation/jsp/mex.jsp");

        verify(request).getRequestDispatcher("/wsfederation/jsp/mex.jsp");
        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void shouldNotWriteTwiceWhenTheMissingDispatcherCannotBeReported() throws Exception {
        when(request.getRequestDispatcher(anyString())).thenReturn(null);
        doThrow(new IOException("gone")).when(response).sendError(anyInt());

        FSUtils.forwardRequest(request, response,
            "http://host.example.com:8080/openam/wsfederation/jsp/mex.jsp");

        verify(request).getRequestDispatcher("/wsfederation/jsp/mex.jsp");
        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        verify(response, times(1)).sendError(anyInt());
    }

    /**
     * A forward that throws before anything was written must not leave the container to commit
     * an empty 200 either.
     */
    @Test
    public void shouldAnswerErrorWhenForwardFailsBeforeCommit() throws Exception {
        doThrow(new ServletException("boom")).when(dispatcher).forward(any(), any());
        when(response.isCommitted()).thenReturn(false);

        FSUtils.forwardRequest(request, response,
            "http://host.example.com:8080/openam/wsfederation/jsp/mex.jsp");

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void shouldAnswerErrorWhenARedirectFailsBeforeCommit() throws Exception {
        doThrow(new IOException("gone")).when(response).sendRedirect(anyString());

        FSUtils.forwardRequest(request, response, "https://trusted.example.com/landing");

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void shouldLeaveACommittedResponseAloneWhenForwardFails() throws Exception {
        doThrow(new ServletException("boom")).when(dispatcher).forward(any(), any());
        when(response.isCommitted()).thenReturn(true);

        FSUtils.forwardRequest(request, response,
            "http://host.example.com:8080/openam/wsfederation/jsp/mex.jsp");

        verify(response, never()).sendError(anyInt());
    }

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
