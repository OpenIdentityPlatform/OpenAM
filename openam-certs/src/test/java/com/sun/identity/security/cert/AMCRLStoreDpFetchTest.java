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

package com.sun.identity.security.cert;

import static com.sun.identity.security.cert.TestHttpServer.recording;
import static com.sun.identity.security.cert.TestHttpServer.redirectAfter;
import static com.sun.identity.security.cert.TestHttpServer.redirectTo;
import static com.sun.identity.security.cert.TestHttpServer.respondChunked;
import static com.sun.identity.security.cert.TestHttpServer.stallAfterHeaders;
import static com.sun.identity.security.cert.TestHttpServer.respond;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.security.KeyPair;
import java.security.Provider;
import java.security.Security;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.LinkedAttribute;
import org.forgerock.opendj.ldap.MemoryBackend;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldif.LDIFEntryReader;
import org.openidentityplatform.bouncycastle.asn1.x500.X500Name;
import org.openidentityplatform.bouncycastle.asn1.x509.GeneralName;
import org.testng.SkipException;
import org.testng.annotations.Test;

import com.sun.identity.common.HttpURLConnectionManager;


/**
 * Verifies that the CRL Distribution Point fetcher only ever connects to an allowed address,
 * and that the response it accepts is bounded and not redirected (GHSA-g5vw-v384-gw9v).
 *
 * <p>The test servers listen on loopback, which the default address policy blocks. Tests that
 * need the fetch to actually happen therefore enable the {@code allow-any-url} escape hatch,
 * so the behaviour under test is isolated from the address check.
 */
public class AMCRLStoreDpFetchTest extends CertTestBase {

    private static final byte[] BODY = "not-really-a-crl".getBytes();

    /**
     * A URL nothing listens on, for the tests that only look at how a connection was configured:
     * {@code openCrlDpConnection} sets the timeouts up but issues no request, so the address is
     * never contacted.
     */
    private static final String UNCONTACTED_URL = "http://127.0.0.1:1/crl";

    private static AMCRLStore newStore() {
        return new AMCRLStore(new AMLDAPCertStoreParameters("localhost", 389));
    }

    private static AMCRLStore storeWithUriParams(String uriParams) {
        AMLDAPCertStoreParameters params = new AMLDAPCertStoreParameters("localhost", 389);
        params.setURIParams(uriParams);
        return new AMCRLStore(params);
    }

    /**
     * Runs {@code action} with {@code HttpURLConnectionManager}'s shared read timeout set to
     * {@code millis}, restoring it afterwards.
     */
    private static void withSharedReadTimeout(int millis, CertAction action) throws Exception {
        withSharedTimeout("READ_TIMEOUT", millis, action);
    }

    /**
     * Runs {@code action} with {@code HttpURLConnectionManager}'s shared connect timeout set to
     * {@code millis}, restoring it afterwards.
     */
    private static void withSharedConnectTimeout(int millis, CertAction action) throws Exception {
        withSharedTimeout("CONNECT_TIMEOUT", millis, action);
    }

    /**
     * Both timeouts are private static fields populated from a system property at
     * class-initialisation time, so they can only be reached by reflection.
     */
    private static void withSharedTimeout(String fieldName, int millis, CertAction action)
            throws Exception {
        Field field = HttpURLConnectionManager.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        int original = (int) field.get(null);
        field.set(null, millis);
        try {
            action.run();
        } finally {
            field.set(null, original);
        }
    }

    @Test
    public void blockedDistributionPointIsNotFetched() throws Exception {
        try (TestHttpServer server = new TestHttpServer(respond(200, BODY))) {
            assertThat(newStore().getCRLByHttpURI(server.url())).isNull();
            assertThat(server.requestCount())
                    .as("no connection may be opened to a blocked distribution point")
                    .isZero();
        }
    }

    @Test
    public void allowedDistributionPointIsFetched() throws Exception {
        try (TestHttpServer server = new TestHttpServer(respond(200, BODY))) {
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () -> {
                assertThat(newStore().getCRLByHttpURI(server.url())).isEqualTo(BODY);
                assertThat(server.requestCount()).isEqualTo(1);
            });
        }
    }

    @Test
    public void nonOkResponseIsNotParsed() throws Exception {
        try (TestHttpServer server = new TestHttpServer(respond(404, BODY))) {
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () -> {
                assertThat(newStore().getCRLByHttpURI(server.url()))
                        .as("an error page is not a CRL")
                        .isNull();
                assertThat(server.requestCount()).isEqualTo(1);
            });
        }
    }

    @Test
    public void aRedirectToAnAllowedLocationIsFollowed() throws Exception {
        // CAs do front their distribution points with a CDN or an http-to-https redirector.
        // Refusing every 3xx leaves those deployments with no way to refresh a CRL at all.
        try (TestHttpServer target = new TestHttpServer(respond(200, BODY));
             TestHttpServer redirector = new TestHttpServer(redirectTo(target.url()))) {
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () -> {
                assertThat(newStore().getCRLByHttpURI(redirector.url())).isEqualTo(BODY);
                assertThat(redirector.requestCount()).isEqualTo(1);
                assertThat(target.requestCount()).isEqualTo(1);
            });
        }
    }

    @Test
    public void theAddressPolicyIsAppliedToTheRedirectTargetAndNotOnlyToTheFirstUrl()
            throws Exception {
        // Both servers are on loopback, which the default policy blocks and the escape hatch
        // allows, so the only way to have the first URL accepted and the second refused is to
        // drop the escape hatch while the redirect is in flight. The redirector does that as it
        // answers, before the fetcher has seen the Location.
        try (TestHttpServer target = new TestHttpServer(respond(200, BODY))) {
            try (TestHttpServer redirector = new TestHttpServer(exchange -> {
                System.setProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "false");
                exchange.getResponseHeaders().set("Location", target.url());
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
            })) {
                withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () -> {
                    assertThat(newStore().getCRLByHttpURI(redirector.url())).isNull();
                    assertThat(redirector.requestCount()).isEqualTo(1);
                    assertThat(target.requestCount())
                            .as("the address the redirect points at must be checked too")
                            .isZero();
                });
            }
        }
    }

    @Test
    public void aRedirectLoopIsAbandoned() throws Exception {
        AtomicReference<String> location = new AtomicReference<>();
        try (TestHttpServer server = new TestHttpServer(exchange -> {
            exchange.getResponseHeaders().set("Location", location.get());
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        })) {
            location.set(server.url());
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () -> {
                assertThat(newStore().getCRLByHttpURI(server.url())).isNull();
                assertThat(server.requestCount())
                        .as("a distribution point that redirects to itself must not be chased")
                        .isLessThanOrEqualTo(4);
            });
        }
    }

    @Test(timeOut = 30000)
    public void theDownloadBudgetIsSpentAcrossRedirectsRatherThanRefilledAtEachHop()
            throws Exception {
        // Every hop stalls ahead of its status line, so it costs a read timeout rather than
        // nothing. Handing each hop the full budget makes one location cost
        // (MAX_CRL_DP_REDIRECTS + 1) budgets — four stalls here instead of one — and the search
        // then pays that again for every distribution point the certificate names.
        AtomicReference<String> location = new AtomicReference<>();
        try (TestHttpServer server = new TestHttpServer(redirectAfter(600, location::get))) {
            location.set(server.url());
            withSharedReadTimeout(0, () ->
                    withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () ->
                            withSystemProperty(AMCRLStore.CRL_DP_TIMEOUT, "1000", () -> {
                                assertThat(newStore().getCRLByHttpURI(server.url())).isNull();
                                // The hop count carries the claim, not the wall clock: a budget
                                // per hop lets all four hops through at 600ms each, a budget for
                                // the chain runs out part way through the second. A clock
                                // assertion able to tell 1000ms from 4x600ms apart would have
                                // under a second of slack on a loaded agent, so the bound on the
                                // time itself is the method's own timeOut.
                                assertThat(server.requestCount())
                                        .as("the budget covers the whole chain, so it runs out "
                                                + "part way through the second hop")
                                        .isLessThanOrEqualTo(2);
                            })));
        }
    }

    @Test
    public void theConnectTimeoutIsBoundedByTheBudgetWhenTheSharedSettingIsUnlimited()
            throws Exception {
        // org.forgerock.openam.url.connectTimeout may legitimately be 0 just as the read timeout
        // may, and 0 there parks the authentication thread on a distribution point that
        // blackholes SYNs for as long as the OS keeps retrying.
        withSharedConnectTimeout(0, () -> {
            HttpURLConnection con = newStore().openCrlDpConnection(UNCONTACTED_URL, null, 1000L);
            assertThat(con.getConnectTimeout())
                    .as("the budget has to bound the connect side by itself")
                    .isEqualTo(1000);
            assertThat(con.getReadTimeout()).isEqualTo(1000);
        });
    }

    @Test
    public void theSharedConnectTimeoutIsNeverRelaxedByTheBudget() throws Exception {
        // The clamp may only ever tighten: at the 1500ms default and a 10s budget the connect
        // timeout stays exactly what the shared setting asked for.
        withSharedConnectTimeout(1500, () ->
                assertThat(newStore().openCrlDpConnection(UNCONTACTED_URL, null, 10000L)
                        .getConnectTimeout()).isEqualTo(1500));
    }

    @Test
    public void aLaterHopGetsWhatIsLeftOfTheBudgetRatherThanAllOfIt() throws Exception {
        // What getCRLByHttpURI passes down once a hop has already been paid for.
        withSharedConnectTimeout(1500, () -> withSharedReadTimeout(3000, () -> {
            HttpURLConnection con = newStore().openCrlDpConnection(UNCONTACTED_URL, null, 400L);
            assertThat(con.getConnectTimeout()).isEqualTo(400);
            assertThat(con.getReadTimeout()).isEqualTo(400);
        }));
    }

    @Test
    public void oversizedResponseIsRejected() throws Exception {
        byte[] large = new byte[4096];
        Arrays.fill(large, (byte) 'x');
        try (TestHttpServer server = new TestHttpServer(respond(200, large))) {
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () ->
                    withSystemProperty(AMCRLStore.CRL_DP_MAX_SIZE, "1024", () ->
                            assertThat(newStore().getCRLByHttpURI(server.url())).isNull()));
        }
    }

    @Test
    public void responseWithinSizeLimitIsAccepted() throws Exception {
        byte[] body = new byte[1024];
        Arrays.fill(body, (byte) 'x');
        try (TestHttpServer server = new TestHttpServer(respond(200, body))) {
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () ->
                    withSystemProperty(AMCRLStore.CRL_DP_MAX_SIZE, "1024", () ->
                            assertThat(newStore().getCRLByHttpURI(server.url())).isEqualTo(body)));
        }
    }

    @Test
    public void oversizedChunkedResponseIsRejected() throws Exception {
        // A chunked response carries no Content-Length, so the header pre-check cannot see it
        // coming and only the read loop's own accounting can stop it. This is the case the size
        // cap exists for: a hostile distribution point simply omits the header.
        byte[] large = new byte[8192];
        Arrays.fill(large, (byte) 'x');
        try (TestHttpServer server = new TestHttpServer(respondChunked(200, large))) {
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () ->
                    withSystemProperty(AMCRLStore.CRL_DP_MAX_SIZE, "1024", () ->
                            assertThat(newStore().getCRLByHttpURI(server.url())).isNull()));
        }
    }

    @Test(timeOut = 30000)
    public void stalledDistributionPointIsAbandonedWhenTheSharedReadTimeoutIsDisabled()
            throws Exception {
        // HttpURLConnectionManager's read timeout comes from com.sun.identity.url.readTimeout,
        // a property tuned for PLL clients that may legitimately be 0 — meaning no timeout at
        // all. The download budget has to bound the fetch by itself, or an authentication thread
        // parks on a drip-feeding distribution point forever.
        try (TestHttpServer stalling = new TestHttpServer(stallAfterHeaders(6000))) {
            withSharedReadTimeout(0, () ->
                    withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () ->
                            withSystemProperty(AMCRLStore.CRL_DP_TIMEOUT, "1000", () -> {
                                long start = System.currentTimeMillis();
                                assertThat(newStore().getCRLByHttpURI(stalling.url())).isNull();
                                assertThat(System.currentTimeMillis() - start)
                                        .as("the fetch must give up on its own budget rather "
                                                + "than wait for the peer")
                                        .isLessThan(5000L);
                            })));
        }
    }

    @Test
    public void distributionPointParametersAreNotPostedToABlockedHost() throws Exception {
        // getURIParams() is administrator configuration POSTed to the distribution point. The
        // attacker controls the destination, not the body, so the address policy is what keeps
        // that configuration from being sent somewhere it should not go.
        try (TestHttpServer server = new TestHttpServer(respond(200, BODY))) {
            assertThat(storeWithUriParams("secret=value").getCRLByHttpURI(server.url())).isNull();
            assertThat(server.requestCount()).isZero();
        }
    }

    @Test
    public void distributionPointParametersAreNotPostedToAHostThatIsNotListed() throws Exception {
        // The address policy only decides whether the server may connect at all. These
        // parameters are the deployment's own configuration and routinely carry a credential for
        // its CRL endpoint, while the URL comes out of the certificate under validation — so
        // reaching an allowed address is not on its own a reason to hand them over. The fetch
        // itself still happens; only the body does not.
        try (TestHttpServer server = new TestHttpServer(respond(200, BODY))) {
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () -> {
                AMCRLStore store = storeWithUriParams("secret=value");
                assertThat(store.getCRLByHttpURI(server.url())).isEqualTo(BODY);
                assertThat(server.requestCount()).isEqualTo(1);
                assertThat(store.paramsFor(server.url()))
                        .as("nothing may be posted to a host param-hosts does not name")
                        .isNull();
            });
        }
    }

    @Test
    public void distributionPointParametersArePostedToAListedHost() throws Exception {
        try (TestHttpServer server = new TestHttpServer(respond(200, BODY))) {
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () ->
                    withSystemProperty(AMCRLStore.CRL_DP_PARAM_HOSTS, "127.0.0.1", () -> {
                        AMCRLStore store = storeWithUriParams("secret=value");
                        assertThat(store.getCRLByHttpURI(server.url())).isEqualTo(BODY);
                        assertThat(server.requestCount()).isEqualTo(1);
                        assertThat(store.paramsFor(server.url())).isEqualTo("secret=value");
                    }));
        }
    }

    @Test
    public void distributionPointParametersFollowTheSameUrlOntoAnotherPort() throws Exception {
        // An http-to-https redirector in front of the deployment's own CRL endpoint: the same
        // URL on another scheme and port, so the same host and the same answer from the list.
        // The test servers speak plain http only, so the port is what changes here; the scheme
        // half is pinned by theOnlyRedirectThatCarriesTheParametersIsOneThatChangesTheTransport.
        for (int status : new int[] {301, 302}) {
            List<String> seen = Collections.synchronizedList(new ArrayList<>());
            try (TestHttpServer target = new TestHttpServer(recording(seen, respond(200, BODY)));
                 TestHttpServer redirector = new TestHttpServer(redirectTo(status, target.url()))) {
                withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () ->
                        withSystemProperty(AMCRLStore.CRL_DP_PARAM_HOSTS, "127.0.0.1", () ->
                                assertThat(storeWithUriParams("secret=value")
                                        .getCRLByHttpURI(redirector.url())).isEqualTo(BODY)));
            }
            assertThat(seen).as("after a %d", status).containsExactly("POST secret=value");
        }
    }

    @Test
    public void aRedirectToAnotherPathOnTheListedHostIsFollowedAsAGet() throws Exception {
        // Post/Redirect/Get: the endpoint takes the POST and points at the result, which is
        // commonly a static resource that answers a POST with 405. Every mainstream client turns
        // the POST into a GET here, and so did this code before the parameter list existed.
        List<String> seen = Collections.synchronizedList(new ArrayList<>());
        try (TestHttpServer target = new TestHttpServer(recording(seen, respond(200, BODY)));
             TestHttpServer redirector = new TestHttpServer(
                     redirectTo(302, target.url().replace("/crl", "/done")))) {
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () ->
                    withSystemProperty(AMCRLStore.CRL_DP_PARAM_HOSTS, "127.0.0.1", () ->
                            assertThat(storeWithUriParams("secret=value")
                                    .getCRLByHttpURI(redirector.url())).isEqualTo(BODY)));
        }
        assertThat(seen).containsExactly("GET ");
    }

    @Test
    public void distributionPointParametersDoNotFollowARedirectToAHostThatIsNotListed()
            throws Exception {
        // The Location is whatever the listed host answered with, and it can name anyone.
        List<String> atRedirector = Collections.synchronizedList(new ArrayList<>());
        List<String> atTarget = Collections.synchronizedList(new ArrayList<>());
        try (TestHttpServer target = new TestHttpServer(recording(atTarget, respond(200, BODY)));
             TestHttpServer redirector = new TestHttpServer(
                     recording(atRedirector, redirectTo(301, target.url())))) {
            // The same loopback address under two names, only one of which is listed.
            String listedUrl = redirector.url().replace("127.0.0.1", "localhost");
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () ->
                    withSystemProperty(AMCRLStore.CRL_DP_PARAM_HOSTS, "localhost", () ->
                            assertThat(storeWithUriParams("secret=value")
                                    .getCRLByHttpURI(listedUrl)).isEqualTo(BODY)));
        }
        assertThat(atRedirector).as("the listed host is sent them")
                .containsExactly("POST secret=value");
        assertThat(atTarget).as("the host it redirects to is not").containsExactly("GET ");
    }

    @Test
    public void aRedirectFromAnUnlistedHostDoesNotStartSendingTheParameters() throws Exception {
        // The distribution point is a third party's, typically plain http, so whoever answers it
        // — or sits on the path to it — picks the Location. Naming a listed host there must not
        // turn the GET into a POST of the deployment's credential, least of all over plain http
        // to a port of the attacker's choosing.
        List<String> atRedirector = Collections.synchronizedList(new ArrayList<>());
        List<String> atTarget = Collections.synchronizedList(new ArrayList<>());
        try (TestHttpServer target = new TestHttpServer(recording(atTarget, respond(200, BODY)));
             TestHttpServer redirector = new TestHttpServer(recording(atRedirector,
                     redirectTo(302, target.url().replace("127.0.0.1", "localhost"))))) {
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () ->
                    withSystemProperty(AMCRLStore.CRL_DP_PARAM_HOSTS, "localhost", () ->
                            assertThat(storeWithUriParams("secret=value")
                                    .getCRLByHttpURI(redirector.url())).isEqualTo(BODY)));
        }
        assertThat(atRedirector).containsExactly("GET ");
        assertThat(atTarget).as("a listed host, reached through an unlisted one")
                .containsExactly("GET ");
    }

    @Test
    public void aSeeOtherRedirectIsFollowedWithoutTheParameters() throws Exception {
        // 303 is the answer to a POST, not a new home for it: it says to fetch another resource
        // with a GET, so replaying the body there would be wrong even on the same host.
        List<String> seen = Collections.synchronizedList(new ArrayList<>());
        try (TestHttpServer target = new TestHttpServer(recording(seen, respond(200, BODY)));
             TestHttpServer redirector = new TestHttpServer(redirectTo(303, target.url()))) {
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () ->
                    withSystemProperty(AMCRLStore.CRL_DP_PARAM_HOSTS, "127.0.0.1", () ->
                            assertThat(storeWithUriParams("secret=value")
                                    .getCRLByHttpURI(redirector.url())).isEqualTo(BODY)));
        }
        assertThat(seen).containsExactly("GET ");
    }

    @Test
    public void theParametersDroppedAtASeeOtherStayDroppedForTheRestOfTheChain()
            throws Exception {
        // After the 303 the request is a GET; a 302 further on keeps it one, even to the same URL
        // on another port.
        List<String> seen = Collections.synchronizedList(new ArrayList<>());
        try (TestHttpServer target = new TestHttpServer(recording(seen, respond(200, BODY)));
             TestHttpServer middle = new TestHttpServer(redirectTo(302, target.url()));
             TestHttpServer redirector = new TestHttpServer(redirectTo(303, middle.url()))) {
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () ->
                    withSystemProperty(AMCRLStore.CRL_DP_PARAM_HOSTS, "127.0.0.1", () ->
                            assertThat(storeWithUriParams("secret=value")
                                    .getCRLByHttpURI(redirector.url())).isEqualTo(BODY)));
        }
        assertThat(seen).containsExactly("GET ");
    }

    @Test
    public void theOnlyRedirectThatCarriesTheParametersIsOneThatChangesTheTransport() {
        String from = "http://pki.example.com/ca.crl";

        assertThat(AMCRLStore.onlySchemeOrPortDiffers(from, "https://pki.example.com/ca.crl"))
                .isTrue();
        assertThat(AMCRLStore.onlySchemeOrPortDiffers(from, "https://pki.example.com:8443/ca.crl"))
                .isTrue();
        assertThat(AMCRLStore.onlySchemeOrPortDiffers(from, "https://PKI.EXAMPLE.COM/ca.crl"))
                .as("host names are case-insensitive").isTrue();

        assertThat(AMCRLStore.onlySchemeOrPortDiffers(from, "https://pki.example.com/other.crl"))
                .as("another path").isFalse();
        assertThat(AMCRLStore.onlySchemeOrPortDiffers(from, "https://pki.example.com/ca.crl?x=1"))
                .as("another query").isFalse();
        assertThat(AMCRLStore.onlySchemeOrPortDiffers(from, "https://crl.example.com/ca.crl"))
                .as("another host, even one the list might also name").isFalse();
        assertThat(AMCRLStore.onlySchemeOrPortDiffers(from, "https://pki.example.com./ca.crl"))
                .as("a trailing dot is another spelling, and not worth trusting").isFalse();
        assertThat(AMCRLStore.onlySchemeOrPortDiffers(from, "https://u@pki.example.com/ca.crl"))
                .as("user info").isFalse();
        assertThat(AMCRLStore.onlySchemeOrPortDiffers(from, "not a url")).isFalse();
    }

    @Test
    public void theParameterHostListMatchesExactHostsAndDomains() throws Exception {
        AMCRLStore store = storeWithUriParams("secret=value");
        withSystemProperty(AMCRLStore.CRL_DP_PARAM_HOSTS, "pki.example.com, .corp.example.net",
                () -> {
                    assertThat(store.paramsFor("http://pki.example.com/crl"))
                            .isEqualTo("secret=value");
                    assertThat(store.paramsFor("http://PKI.EXAMPLE.COM/crl"))
                            .as("host names are case-insensitive").isEqualTo("secret=value");
                    assertThat(store.paramsFor("http://ca.corp.example.net/crl"))
                            .as("a leading dot covers the domain below it")
                            .isEqualTo("secret=value");

                    assertThat(store.paramsFor("http://corp.example.net/crl"))
                            .as(".corp.example.net does not cover corp.example.net itself")
                            .isNull();
                    assertThat(store.paramsFor("http://notpki.example.com/crl"))
                            .as("an entry is a whole host, not a suffix").isNull();
                    assertThat(store.paramsFor("http://pki.example.com.attacker.test/crl"))
                            .isNull();
                    assertThat(store.paramsFor("http://attacker.test/crl")).isNull();
                });
    }

    @Test
    public void aMalformedTrailingParameterLeavesNoStraySeparator() throws Exception {
        assertThat(new String(AMCRLStore.encodeUriParams("user=svc,pass=secret"), "UTF-8"))
                .isEqualTo("user=svc&pass=secret");
        assertThat(new String(AMCRLStore.encodeUriParams("user=svc,pass=secret,bogus"), "UTF-8"))
                .as("a token with no value must not leave behind the separator written for it")
                .isEqualTo("user=svc&pass=secret");
        assertThat(new String(AMCRLStore.encodeUriParams("bogus,user=svc"), "UTF-8"))
                .as("nor must a malformed leading token put one in front")
                .isEqualTo("user=svc");
        assertThat(AMCRLStore.encodeUriParams(null)).isEmpty();
        assertThat(AMCRLStore.encodeUriParams("bogus")).isEmpty();
    }

    @Test
    public void nothingIsPostedWhenNoParametersAreConfigured() throws Exception {
        withSystemProperty(AMCRLStore.CRL_DP_PARAM_HOSTS, "pki.example.com", () -> {
            assertThat(newStore().paramsFor("http://pki.example.com/crl")).isNull();
            assertThat(storeWithUriParams("  ").paramsFor("http://pki.example.com/crl")).isNull();
        });
    }

    @Test
    public void aRedirectMayNotDropOutOfHttps() {
        // An on-path attacker, or a misconfigured front end, answering 302 with a plaintext
        // Location would move the fetch onto a transport where the CRL can be replaced
        // wholesale — and validateFetchedCrl catches that only when an issuer key is reachable.
        assertThat(AMCRLStore.isSchemeDowngrade("https://pki.example.com/ca.crl", "http"))
                .isTrue();
        assertThat(AMCRLStore.isSchemeDowngrade("HTTPS://pki.example.com/ca.crl", "http"))
                .as("the scheme of the URL being left is case-insensitive too").isTrue();

        assertThat(AMCRLStore.isSchemeDowngrade("https://pki.example.com/ca.crl", "https"))
                .isFalse();
        assertThat(AMCRLStore.isSchemeDowngrade("http://pki.example.com/ca.crl", "https"))
                .as("an http-to-https front end is ordinary and stays allowed").isFalse();
        assertThat(AMCRLStore.isSchemeDowngrade("http://pki.example.com/ca.crl", "http"))
                .isFalse();
    }

    @Test
    public void aLocationRefusedByTheAddressPolicyDoesNotCostOneFromTheBudget() throws Exception {
        // An AD CS certificate lists several unreachable spellings of its CRL before the one that
        // works. Charging the budget before the address check let those exhaust it, so the
        // location that would have answered was never tried and the CRL never refreshed.
        try (TestHttpServer good = new TestHttpServer(respond(200, BODY))) {
            X509Certificate cert = buildCertificateWithMultiNameDp("dp-holder",
                    newRsaKeyPair().getPublic(), CA_CN, CA_KEY_PAIR.getPrivate(),
                    BigInteger.valueOf(46),
                    // Host-less, so refused whatever the address policy says — the escape hatch
                    // relaxes which addresses are allowed, not the need for one.
                    new GeneralName(GeneralName.uniformResourceIdentifier, "http:///crl"),
                    new GeneralName(GeneralName.uniformResourceIdentifier, "http:///crl2"),
                    new GeneralName(GeneralName.uniformResourceIdentifier, good.url()));
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () ->
                    withSystemProperty(AMCRLStore.CRL_DP_MAX_LOCATIONS, "1", () -> {
                        AMCRLStore store = newStore();
                        // The body is not a CRL, so generateCRL fails and the search returns
                        // null; that the server was contacted at all is the point.
                        store.getUpdateCRLFromCrlDP(store.getCRLDPExt(cert));
                        assertThat(good.requestCount())
                                .as("the one location in the budget must be spent on the "
                                        + "location that was actually contacted")
                                .isEqualTo(1);
                    }));
        }
    }

    @Test
    public void aConnectionThatFailsWhileItIsBeingOpenedIsStillDisconnected() throws Exception {
        // The parameter body is written before openCrlDpConnection returns, so a distribution
        // point that accepts the connection and then resets during the write throws out of it
        // with the caller's own variable still null — and the caller's finally block, which owns
        // the disconnect, then sees null and closes nothing. One socket per location per
        // certificate left to the collector, on the authentication path.
        List<RecordingConnection> opened = new ArrayList<>();
        AMLDAPCertStoreParameters params = new AMLDAPCertStoreParameters("localhost", 389);
        AMCRLStore store = new AMCRLStore(params) {
            @Override
            HttpURLConnection newCrlDpConnection(URL url) {
                RecordingConnection con = new RecordingConnection(url);
                opened.add(con);
                return con;
            }
        };

        try {
            store.openCrlDpConnection(UNCONTACTED_URL, "realm=/", 1000L);
            org.testng.Assert.fail("openCrlDpConnection should have propagated the write failure");
        } catch (IOException expected) {
            // the peer reset while the parameter body was being written
        }
        assertThat(opened).hasSize(1);
        assertThat(opened.get(0).disconnected)
                .as("a connection that never reached its caller has to be disconnected here")
                .isTrue();
    }

    /** Fails where the parameter body is written, and records whether it was disconnected. */
    private static final class RecordingConnection extends HttpURLConnection {

        private boolean disconnected;

        RecordingConnection(URL url) {
            super(url);
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new IOException("connection reset by peer");
        }

        @Override
        public void disconnect() {
            disconnected = true;
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() {
        }
    }

    @Test
    public void aTimeoutTooLargeToExpressIsClampedRatherThanWrapped() throws Exception {
        // MILLISECONDS.toNanos saturates at Long.MAX_VALUE past ~292 years, and System.nanoTime()
        // plus a saturated value wraps the deadline negative. Both reads of it are subtractions,
        // which stay exact across that wrap, so the budget is not spent on the spot — it is the
        // other way round: it bounds nothing at all, which is the opposite of what the setting is
        // for, and remainingMillis() then hands down a number no socket timeout can express.
        withSystemProperty(AMCRLStore.CRL_DP_TIMEOUT, "99999999999999", () -> {
            AMCRLStore.FetchBudget budget = AMCRLStore.FetchBudget.withLocations(1);
            assertThat(budget.isExpired())
                    .as("an unusable timeout must not refuse every fetch either")
                    .isFalse();
            assertThat(budget.remainingMillis())
                    .as("what is left of the budget stays a number a socket timeout can hold")
                    .isBetween(1L, (long) Integer.MAX_VALUE);
        });
    }

    @Test
    public void anLdapsLocationWhoseSslContextFailsDoesNotCostALocation() throws Exception {
        // The third way an LDAP location is dropped before anything is contacted, and the one the
        // charge used to be made above: an ldaps:// distribution point whose SSL context cannot
        // be built. Active Directory Certificate Services lists several unusable spellings of a
        // CRL ahead of the one that works, and with dp.max-locations at 1 a location charged for
        // a connection nobody opened is the working location never being tried.
        Provider jsse = Security.getProvider("SunJSSE");
        if (jsse == null) {
            throw new SkipException("SunJSSE is not installed, so removing it breaks nothing");
        }
        // Put back where it was, not appended: every later test in this JVM that builds an
        // SSLContext, KeyStore or TrustManagerFactory would otherwise resolve another provider
        // first, and fail or pass depending on whether this class ran before it.
        int position = Arrays.asList(Security.getProviders()).indexOf(jsse) + 1;
        Security.removeProvider("SunJSSE");
        try {
            AMCRLStore.FetchBudget budget = AMCRLStore.FetchBudget.withLocations(1);
            // A public literal address, so the address policy allows it and the time budget is
            // untouched: the SSL context is what stops this location.
            assertThat(newStore().getCRLByLdapURI(
                    "ldaps://198.51.100.7/cn=crl,dc=example,dc=com", budget)).isNull();
            assertThat(budget.hasLocationsLeft())
                    .as("a location whose SSL context could not be built was never contacted")
                    .isTrue();
        } finally {
            Security.insertProviderAt(jsse, position);
        }
    }

    @Test
    public void anLdapLocationRefusedByTheSpentTimeBudgetDoesNotCostOneFromIt() throws Exception {
        // The same invariant as the test above, on the LDAP sink: a location that was never
        // contacted must not consume one of the few a certificate is allowed to try. The address
        // check and the time budget are the two ways a location is dropped before any connection
        // is opened, and only the first was covered.
        withSystemProperty(AMCRLStore.CRL_DP_TIMEOUT, "1", () -> {
            AMCRLStore.FetchBudget budget = AMCRLStore.FetchBudget.withLocations(1);
            // Deterministic rather than timed: the budget is one millisecond, so this returns as
            // soon as the monotonic clock has moved past it.
            while (!budget.isExpired()) {
                Thread.yield();
            }
            // A public literal address, so the address policy allows it and the time budget is
            // what stops the fetch. Nothing is contacted: the check comes before the connection.
            assertThat(newStore().getCRLByLdapURI("ldap://198.51.100.7/cn=crl,dc=example,dc=com",
                    budget)).isNull();
            assertThat(budget.hasLocationsLeft())
                    .as("a location the spent time budget stopped was never contacted")
                    .isTrue();
        });
    }

    @Test
    public void aDistributionPointThatNamesNoLocationIsSkipped() throws Exception {
        // RFC 5280 allows a distribution point that carries only a cRLIssuer, or a
        // nameRelativeToCRLIssuer; getFullName() is null for both. Dereferencing that threw an
        // NPE out of getCRL's outer catch and abandoned the whole CRL update.
        try (TestHttpServer good = new TestHttpServer(respond(200, BODY))) {
            X509Certificate cert = buildCertificateWithRawDps("dp-holder",
                    newRsaKeyPair().getPublic(), CA_CN, CA_KEY_PAIR.getPrivate(),
                    BigInteger.valueOf(47),
                    crlIssuerOnlyDp("CRL Signer"), uriDp(good.url()));
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () -> {
                AMCRLStore store = newStore();
                store.getUpdateCRLFromCrlDP(store.getCRLDPExt(cert));
                assertThat(good.requestCount())
                        .as("a point that names no location must be skipped, not throw, and the "
                                + "point after it must still be tried")
                        .isEqualTo(1);
            });
        }
    }

    @Test
    public void aReplacementIsFetchedFromTheExpiredCrlsOwnDistributionPoint() throws Exception {
        // The first of the two searches getCRL runs. X509CRL.getExtensionValue() returns the DER
        // OCTET STRING wrapping the extension, and handing that wrapper to
        // IssuingDistributionPointExtension threw "Invalid encoding" for every CRL ever passed —
        // so the extension read as absent whether or not it was there and this search never ran.
        X509CRL replacement = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(2));
        try (TestHttpServer server = new TestHttpServer(respond(200, replacement.getEncoded()))) {
            X509CRL expired = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.ONE,
                    new Date(System.currentTimeMillis() - 1000), uri(server.url()));

            assertThat(AMCRLStore.issuingDistributionPointOf(expired))
                    .as("the extension is there, so it has to be readable")
                    .isNotNull();
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () -> {
                assertThat(newStore().getUpdateCRLFromCrlIDP(
                        AMCRLStore.issuingDistributionPointOf(expired)))
                        .isEqualTo(replacement);
                assertThat(server.requestCount()).isEqualTo(1);
            });
        }
    }

    @Test
    public void aFetchedCrlIsNotParsedWithTheSharedCertificateFactory() throws Exception {
        // AMCertStore's static factory is used by every thread in the server at once, and
        // CertificateFactory does not promise to be safe for that. Parsing through it here could
        // turn a concurrent login into an intermittent parse failure, and a CRL that fails to
        // parse is a CRL that is not refreshed.
        X509CRL replacement = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(2));
        try (TestHttpServer server = new TestHttpServer(respond(200, replacement.getEncoded()))) {
            X509Certificate cert = buildCertificateWithDps("dp-holder",
                    newRsaKeyPair().getPublic(), CA_CN, CA_KEY_PAIR.getPrivate(),
                    BigInteger.valueOf(60), false, uri(server.url()));
            X509CRL expired = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.ONE,
                    new Date(System.currentTimeMillis() - 1000), uri(server.url()));

            withUnusableSharedCertificateFactory(() ->
                    withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () -> {
                        AMCRLStore store = newStore();
                        assertThat(store.getUpdateCRLFromCrlDP(store.getCRLDPExt(cert)))
                                .as("from the certificate's distribution point")
                                .isEqualTo(replacement);
                        assertThat(store.getUpdateCRLFromCrlIDP(
                                AMCRLStore.issuingDistributionPointOf(expired)))
                                .as("from the expired CRL's own").isEqualTo(replacement);
                    }));
        }
    }

    @Test
    public void theCrlsOwnDistributionPointGetsTheSameAddressPolicy() throws Exception {
        // It is a second sink into the same fetcher, reached from a CRL rather than from a
        // certificate — and now that the extension is readable at all, it needs saying.
        try (TestHttpServer server = new TestHttpServer(respond(200, BODY))) {
            X509CRL expired = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.ONE,
                    new Date(System.currentTimeMillis() - 1000), uri(server.url()));

            assertThat(newStore().getUpdateCRLFromCrlIDP(
                    AMCRLStore.issuingDistributionPointOf(expired))).isNull();
            assertThat(server.requestCount())
                    .as("a loopback distribution point must not be contacted, wherever it "
                            + "was named")
                    .isZero();
        }
    }

    @Test
    public void aCrlWithNoIssuingDistributionPointNamesNoReplacement() throws Exception {
        X509CRL crl = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.ONE);

        assertThat(AMCRLStore.issuingDistributionPointOf(crl)).isNull();
        assertThat(AMCRLStore.issuingDistributionPointOf(null)).isNull();
        assertThat(newStore().getUpdateCRLFromCrlIDP(null)).isNull();
    }

    @Test(timeOut = 30000)
    public void theTimeBudgetIsSharedAcrossLocationsRatherThanRefilledForEach() throws Exception {
        // The budget belongs to the certificate. Refilling it per location multiplied the wait
        // by CRL_DP_MAX_LOCATIONS, and Cert.doJCERevocationValidation runs the whole search once
        // per certificate in the chain — minutes of a parked authentication thread from one
        // certificate listing a handful of stalling distribution points.
        // As many locations as CRL_DP_MAX_LOCATIONS defaults to, so that a budget refilled for
        // each of them costs five read timeouts against the one a shared budget costs — a gap
        // the wall clock below can be read against with room to spare on a loaded agent.
        try (TestHttpServer first = new TestHttpServer(stallAfterHeaders(5000));
             TestHttpServer second = new TestHttpServer(stallAfterHeaders(5000));
             TestHttpServer third = new TestHttpServer(stallAfterHeaders(5000));
             TestHttpServer fourth = new TestHttpServer(stallAfterHeaders(5000));
             TestHttpServer fifth = new TestHttpServer(stallAfterHeaders(5000))) {
            X509Certificate cert = buildCertificateWithMultiNameDp("dp-holder",
                    newRsaKeyPair().getPublic(), CA_CN, CA_KEY_PAIR.getPrivate(),
                    BigInteger.valueOf(48),
                    new GeneralName(GeneralName.uniformResourceIdentifier, first.url()),
                    new GeneralName(GeneralName.uniformResourceIdentifier, second.url()),
                    new GeneralName(GeneralName.uniformResourceIdentifier, third.url()),
                    new GeneralName(GeneralName.uniformResourceIdentifier, fourth.url()),
                    new GeneralName(GeneralName.uniformResourceIdentifier, fifth.url()));
            withSharedReadTimeout(0, () ->
                    withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () ->
                            withSystemProperty(AMCRLStore.CRL_DP_TIMEOUT, "1000", () -> {
                                long start = System.currentTimeMillis();
                                AMCRLStore store = newStore();
                                assertThat(store.getUpdateCRLFromCrlDP(store.getCRLDPExt(cert)))
                                        .isNull();
                                assertThat(System.currentTimeMillis() - start)
                                        .as("one budget covers every location of the "
                                                + "certificate, not one budget each")
                                        .isLessThan(3000L);
                                assertThat(first.requestCount() + second.requestCount()
                                        + third.requestCount() + fourth.requestCount()
                                        + fifth.requestCount())
                                        .as("the budget runs out part way through, so the later "
                                                + "locations are never reached")
                                        .isLessThanOrEqualTo(2);
                            })));
        }
    }

    @Test
    public void rfc822NameCarryingHttpUrlIsIgnored() throws Exception {
        // GeneralName.toString() is scanned for a scheme, so an e-mail name whose local part
        // embeds a URL used to reach the HTTP sink. Only uniformResourceIdentifier names may.
        try (TestHttpServer server = new TestHttpServer(respond(200, BODY))) {
            X509Certificate cert = certificateWithDps(
                    new GeneralName(GeneralName.rfc822Name, "user@" + server.url()));
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () -> {
                AMCRLStore store = newStore();
                assertThat(store.getUpdateCRLFromCrlDP(store.getCRLDPExt(cert))).isNull();
                assertThat(server.requestCount())
                        .as("a non-URI general name must not reach the HTTP fetcher")
                        .isZero();
            });
        }
    }

    @Test
    public void secondNameOfADistributionPointIsTriedWhenTheFirstFails() throws Exception {
        X509CRL expected = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.ONE);
        try (TestHttpServer broken = new TestHttpServer(respond(500, BODY));
             TestHttpServer good = new TestHttpServer(respond(200, expected.getEncoded()))) {
            KeyPair keyPair = newRsaKeyPair();
            X509Certificate cert = buildCertificateWithMultiNameDp("dp-holder",
                    keyPair.getPublic(), CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(43),
                    new GeneralName(GeneralName.uniformResourceIdentifier, broken.url()),
                    new GeneralName(GeneralName.uniformResourceIdentifier, good.url()));
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () -> {
                AMCRLStore store = newStore();
                assertThat(store.getUpdateCRLFromCrlDP(store.getCRLDPExt(cert)))
                        .as("a failed first location must not stop the search")
                        .isEqualTo(expected);
                assertThat(broken.requestCount()).isEqualTo(1);
                assertThat(good.requestCount()).isEqualTo(1);
            });
        }
    }

    @Test
    public void searchStopsAtTheFirstUsableLocation() throws Exception {
        X509CRL expected = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.ONE);
        try (TestHttpServer good = new TestHttpServer(respond(200, expected.getEncoded()));
             TestHttpServer broken = new TestHttpServer(respond(500, BODY))) {
            KeyPair keyPair = newRsaKeyPair();
            X509Certificate cert = buildCertificateWithMultiNameDp("dp-holder",
                    keyPair.getPublic(), CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(44),
                    new GeneralName(GeneralName.uniformResourceIdentifier, good.url()),
                    new GeneralName(GeneralName.uniformResourceIdentifier, broken.url()));
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () -> {
                AMCRLStore store = newStore();
                assertThat(store.getUpdateCRLFromCrlDP(store.getCRLDPExt(cert)))
                        .as("a later failing location must not overwrite a CRL already found")
                        .isEqualTo(expected);
                assertThat(broken.requestCount())
                        .as("no further location may be contacted once a CRL was retrieved")
                        .isZero();
            });
        }
    }

    @Test
    public void searchStopsAfterTheConfiguredNumberOfLocations() throws Exception {
        // Every location gets its own connect and download budget, and the whole search runs once
        // per certificate in the chain, so an unbounded list of distribution points is a way to
        // tie up an authentication thread for as long as the attacker likes.
        try (TestHttpServer first = new TestHttpServer(respond(500, BODY));
             TestHttpServer second = new TestHttpServer(respond(500, BODY));
             TestHttpServer third = new TestHttpServer(respond(500, BODY))) {
            X509Certificate cert = buildCertificateWithMultiNameDp("dp-holder",
                    newRsaKeyPair().getPublic(), CA_CN, CA_KEY_PAIR.getPrivate(),
                    BigInteger.valueOf(45),
                    new GeneralName(GeneralName.uniformResourceIdentifier, first.url()),
                    new GeneralName(GeneralName.uniformResourceIdentifier, second.url()),
                    new GeneralName(GeneralName.uniformResourceIdentifier, third.url()));
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () ->
                    withSystemProperty(AMCRLStore.CRL_DP_MAX_LOCATIONS, "2", () -> {
                        AMCRLStore store = newStore();
                        assertThat(store.getUpdateCRLFromCrlDP(store.getCRLDPExt(cert))).isNull();
                        assertThat(first.requestCount() + second.requestCount()
                                + third.requestCount())
                                .as("no more than the configured number of locations is tried")
                                .isEqualTo(2);
                    }));
        }
    }

    @Test
    public void blockedLdapDistributionPointIsNotContacted() throws Exception {
        // ldap:// distribution points are a second sink into the same extension, useful for
        // scanning internal ports; they get the same address policy.
        try (ConnectionDetector listener = new ConnectionDetector()) {
            String uri = "ldap://127.0.0.1:" + listener.port() + "/cn=crl,dc=example,dc=com";
            assertThat(newStore().getCRLByLdapURI(uri)).isNull();
            assertThat(listener.sawConnection())
                    .as("no connection may be opened to a blocked LDAP distribution point")
                    .isFalse();
        }
    }

    @Test
    public void directoryNameCarryingHttpUrlIsIgnored() throws Exception {
        // The other general name type that could carry a scheme past the old indexOf("http")
        // scan: a directoryName whose CN is itself a URL.
        try (TestHttpServer server = new TestHttpServer(respond(200, BODY))) {
            X509Certificate cert = certificateWithDps(new GeneralName(GeneralName.directoryName,
                    new X500Name("CN=" + server.url())));
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () -> {
                AMCRLStore store = newStore();
                assertThat(store.getUpdateCRLFromCrlDP(store.getCRLDPExt(cert))).isNull();
                assertThat(server.requestCount())
                        .as("a directoryName must not reach the HTTP fetcher")
                        .isZero();
            });
        }
    }

    @Test
    public void theLocationBudgetIsCarriedBetweenSearchesRatherThanRefilled() throws Exception {
        // getCRL searches for a replacement CRL twice — once from the expired CRL's own
        // IssuingDistributionPoint, once from the certificate's cRLDistributionPoints — and the
        // budget belongs to the certificate, not to either search. Passing an already-spent
        // budget must therefore stop the second search dead rather than start it over.
        try (TestHttpServer dp = new TestHttpServer(respond(500, BODY))) {
            X509Certificate cert = certificateWithDps(
                    new GeneralName(GeneralName.uniformResourceIdentifier, dp.url()));
            AMCRLStore.FetchBudget alreadySpent = AMCRLStore.FetchBudget.withLocations(0);

            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () -> {
                AMCRLStore store = newStore();
                assertThat(store.getUpdateCRLFromCrlDP(store.getCRLDPExt(cert), alreadySpent))
                        .isNull();
                assertThat(dp.requestCount())
                        .as("a search handed an exhausted budget must not contact anything")
                        .isZero();
            });
        }
    }

    @Test(timeOut = 20000)
    public void stalledLdapDistributionPointIsAbandonedWithinTheBudget() throws Exception {
        // The ldap:// distribution point is the same attacker-named input as the http:// one, and
        // needs the same transfer budget. OpenDJ's default request timeout is unlimited, so a peer
        // that completes the TCP handshake and then says nothing holds an authentication thread
        // for as long as it cares to.
        try (SilentListener silent = new SilentListener()) {
            String uri = "ldap://127.0.0.1:" + silent.port() + "/cn=TestCA,dc=example,dc=com";
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () ->
                    withSystemProperty(AMCRLStore.CRL_DP_TIMEOUT, "2000", () -> {
                        long start = System.currentTimeMillis();
                        assertThat(newStore().getCRLByLdapURI(uri)).isNull();
                        assertThat(System.currentTimeMillis() - start)
                                .as("the search must give up on its own budget")
                                .isLessThan(10000L);
                    }));
        }
    }

    @Test
    public void anLdapDistributionPointCannotWidenTheSearchBeyondTheEntryItNames()
            throws Exception {
        // The address policy decides whether the server may connect; the rest of the URL — base,
        // scope and filter — comes out of the certificate too. Handed to the directory verbatim,
        // "??sub?(...)" turns one certificate into an anonymous, unindexed whole-subtree search
        // of whatever directory sits at an allowed address, and site-local ones are allowed by
        // default. The entry below the named one holds a CRL, so honouring the URL's scope and
        // filter is the only way this fetch can come back with anything.
        try (TestLdapServer dp = new TestLdapServer(directoryWithACrlBelowTheNamedEntry())) {
            String uri = "ldap://127.0.0.1:" + dp.port() + "/dc=corp,dc=example??sub?(cn=leak)";
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () -> {
                assertThat(newStore().getCRLByLdapURI(uri))
                        .as("only the entry the URL names may be read")
                        .isNull();
                assertThat(dp.searches()).hasSize(1);
                SearchRequest search = dp.searches().get(0);
                assertThat(search.getName().toString()).isEqualTo("dc=corp,dc=example");
                assertThat(search.getScope()).isEqualTo(SearchScope.BASE_OBJECT);
                assertThat(search.getFilter().toString())
                        .as("the certificate's filter is not the directory's business")
                        .isEqualTo("(objectClass=*)");
            });
        }
    }

    @Test
    public void anLdapDistributionPointIsAskedForTheCrlAttributesOnly() throws Exception {
        // An empty attribute list is every user attribute of the entry; a list the certificate
        // chose is whatever it wants read. Only the CRL is ever looked at.
        try (TestLdapServer dp = new TestLdapServer(directoryWithACrlBelowTheNamedEntry())) {
            String uri = "ldap://127.0.0.1:" + dp.port()
                    + "/cn=leak,dc=corp,dc=example?userPassword,description?base";
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () -> {
                assertThat(newStore().getCRLByLdapURI(uri)).isEqualTo(LEAKED_CRL);
                assertThat(dp.searches()).hasSize(1);
                assertThat(dp.searches().get(0).getAttributes())
                        .containsExactlyInAnyOrder("certificaterevocationlist",
                                "certificaterevocationlist;binary");
            });
        }
    }

    @Test
    public void aDirectoryDistributionPointInTheUsualFormStillYieldsItsCrl() throws Exception {
        // What Active Directory Certificate Services and most directory-backed CAs publish: the
        // entry holding the CRL, one attribute, base scope, and a filter on its object class.
        // Confining the search must not lose this one.
        X509CRL crl = buildCrl(CA_CN, CA_KEY_PAIR.getPrivate(), BigInteger.ONE);
        try (TestLdapServer dp = new TestLdapServer(new MemoryBackend(new LDIFEntryReader(
                "dn: dc=example,dc=com",
                "objectClass: top",
                "objectClass: domain",
                "dc: example",
                "",
                "dn: cn=" + CA_CN + ",dc=example,dc=com",
                "objectClass: top",
                "objectClass: cRLDistributionPoint",
                "cn: " + CA_CN,
                "certificateRevocationList;binary:: "
                        + Base64.getEncoder().encodeToString(crl.getEncoded()))))) {
            String uri = "ldap://127.0.0.1:" + dp.port() + "/cn=" + CA_CN + ",dc=example,dc=com"
                    + "?certificateRevocationList;binary?base?objectClass=cRLDistributionPoint";
            withSystemProperty(AMCRLStore.CRL_DP_ALLOW_ANY_URL, "true", () ->
                    assertThat(newStore().getCRLByLdapURI(uri)).isEqualTo(crl.getEncoded()));
        }
    }

    private static final byte[] LEAKED_CRL = "crl-of-an-entry-nobody-named".getBytes();

    /**
     * An internal directory: a container with nothing of interest on it, and below it an entry
     * carrying a CRL next to attributes no distribution point has any business reading.
     */
    private static MemoryBackend directoryWithACrlBelowTheNamedEntry() throws Exception {
        return new MemoryBackend(new LDIFEntryReader(
                "dn: dc=corp,dc=example",
                "objectClass: top",
                "objectClass: domain",
                "dc: corp",
                "",
                "dn: cn=leak,dc=corp,dc=example",
                "objectClass: top",
                "objectClass: cRLDistributionPoint",
                "objectClass: extensibleObject",
                "cn: leak",
                "description: internal",
                "userPassword: secret",
                "certificateRevocationList;binary:: "
                        + Base64.getEncoder().encodeToString(LEAKED_CRL)));
    }

    @Test
    public void anOversizedCrlAttributeFromAnLdapDistributionPointIsRejected() throws Exception {
        // The LDAP sink reads an attribute from a server the certificate named, so its value is
        // as untrusted as an HTTP response body and gets the same size cap.
        Attribute huge = new LinkedAttribute(AMCRLStore.CERTIFICATE_REVOCATION_LIST_BINARY,
                ByteString.wrap(new byte[4096]));

        withSystemProperty(AMCRLStore.CRL_DP_MAX_SIZE, "1024", () ->
                assertThat(AMCRLStore.crlBytesFrom(huge)).isNull());
        assertThat(AMCRLStore.crlBytesFrom(huge))
                .as("the same value is within the default 10MB cap")
                .hasSize(4096);
    }

    private static X509Certificate certificateWithDps(GeneralName... dps) throws Exception {
        KeyPair keyPair = newRsaKeyPair();
        return buildCertificateWithDps("dp-holder", keyPair.getPublic(), CA_CN,
                CA_KEY_PAIR.getPrivate(), BigInteger.valueOf(42), false, dps);
    }

    /**
     * Accepts connections and then says nothing at all, holding them open — a distribution point
     * that answers the TCP handshake and stops there.
     */
    private static final class SilentListener implements Closeable {

        private final ServerSocket listener;
        private final List<Socket> accepted = Collections.synchronizedList(new ArrayList<Socket>());
        private volatile boolean stopped;

        SilentListener() throws IOException {
            listener = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
            Thread acceptor = new Thread(() -> {
                while (!stopped) {
                    try {
                        accepted.add(listener.accept());
                    } catch (IOException e) {
                        return;
                    }
                }
            });
            acceptor.setDaemon(true);
            acceptor.start();
        }

        int port() {
            return listener.getLocalPort();
        }

        @Override
        public void close() throws IOException {
            stopped = true;
            synchronized (accepted) {
                for (Socket socket : accepted) {
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                        // best effort; the test is finished with them
                    }
                }
            }
            listener.close();
        }
    }

}
