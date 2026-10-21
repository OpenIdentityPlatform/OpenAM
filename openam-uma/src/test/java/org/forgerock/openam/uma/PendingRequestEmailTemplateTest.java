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

package org.forgerock.openam.uma;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.forgerock.http.Client;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.openam.utils.OpenAMSettings;
import org.forgerock.util.promise.Promises;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.am.util.SystemProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Covers the SSRF hardening of the scope description fetch (GHSA-7q42-6xr7-9rmw): a scope that is
 * a URI is only fetched when it uses {@code http} or {@code https} and does not resolve to an
 * internal address; otherwise it is shown as it is, and the server never sends the request.
 */
public class PendingRequestEmailTemplateTest {

    private static final String REALM = "/";
    private static final String USER = "alice";

    private final List<String> requestedUris = new ArrayList<>();
    private String previousAllowAnyAddress;
    private PendingRequestEmailTemplate template;
    private String propertyBeforeAnyTest;
    private boolean metadataUriAllowedBeforeAnyTest;

    /**
     * Captures the stock behaviour before {@link #setUp()} writes the property: SystemProperties has
     * no removal API, so the absent state cannot be restored once a test has set it.
     */
    @BeforeClass
    public void captureDecisionBeforeAnyTestWritesTheProperty() {
        propertyBeforeAnyTest =
                SystemProperties.get(PendingRequestEmailTemplate.ALLOW_ANY_SCOPE_DESCRIPTION_ADDRESS);
        metadataUriAllowedBeforeAnyTest = PendingRequestEmailTemplate.isScopeDescriptionUriAllowed(
                URI.create("http://169.254.169.254/latest/meta-data/"));
    }

    @BeforeMethod
    public void setUp() throws Exception {
        previousAllowAnyAddress =
                SystemProperties.get(PendingRequestEmailTemplate.ALLOW_ANY_SCOPE_DESCRIPTION_ADDRESS);
        allowInternalAddresses("false");
        requestedUris.clear();

        // Client is final, so a real one is driven through a recording handler.
        Client client = new Client((context, request) -> {
            requestedUris.add(request.getUri().toString());
            Response response = new Response(Status.OK);
            response.setEntity(object(field("name", "Read photos")));
            return Promises.newResultPromise(response);
        });

        template = newTemplate(client);
    }

    @AfterMethod
    public void tearDown() {
        // SystemProperties has no removal API; an absent property is restored as "false", the
        // documented default and so indistinguishable from absent to every reader of it.
        allowInternalAddresses(previousAllowAnyAddress == null ? "false" : previousAllowAnyAddress);
    }

    /** The advisory itself: the cloud metadata endpoint planted as a scope is never requested. */
    @Test
    public void doesNotFetchLinkLocalMetadataEndpoint() {
        String scope = "http://169.254.169.254/latest/meta-data/iam/info";

        assertThat(resolve(scope)).isEqualTo(scope);
        assertThat(requestedUris).isEmpty();
    }

    /** The advisory's fail-closed default: setUp writes "false", so only this capture sees it unset. */
    @Test
    public void doesNotAllowInternalAddressWhenPropertyIsAbsent() {
        assertThat(propertyBeforeAnyTest).isNull();
        assertThat(metadataUriAllowedBeforeAnyTest).isFalse();
    }

    /** The IP literal keeps DNS out of the test. */
    @Test
    public void fetchesPublicScopeDescriptionByDefault() {
        String scope = "http://8.8.8.8/scopes/read";

        assertThat(resolve(scope)).isEqualTo("Read photos");
        assertThat(requestedUris).containsExactly(scope);
    }

    @Test
    public void doesNotFetchLoopbackAddress() {
        String scope = "http://127.0.0.1:8080/openam/json/serverinfo";

        assertThat(resolve(scope)).isEqualTo(scope);
        assertThat(requestedUris).isEmpty();
    }

    @Test
    public void showsPlainScopeNameWithoutFetching() {
        assertThat(resolve("read")).isEqualTo("read");
        assertThat(requestedUris).isEmpty();
    }

    /** A scope that is not a valid URI at all used to abort the pending request creation. */
    @Test
    public void showsUnparsableScopeWithoutFetching() {
        assertThat(resolve("read photos")).isEqualTo("read photos");
        assertThat(requestedUris).isEmpty();
    }

    @Test
    public void fetchesInternalScopeDescriptionOnceInternalAddressesAreAllowed() {
        allowInternalAddresses("true");
        String scope = "http://127.0.0.1:8080/scopes/read";

        assertThat(resolve(scope)).isEqualTo("Read photos");
        assertThat(requestedUris).containsExactly(scope);
    }

    /** Lifting the address check never re-enables non-HTTP schemes. */
    @Test
    public void doesNotFetchNonHttpSchemeEvenWhenInternalAddressesAreAllowed() {
        allowInternalAddresses("true");
        String scope = "file:///etc/passwd";

        assertThat(resolve(scope)).isEqualTo(scope);
        assertThat(requestedUris).isEmpty();
    }

    /**
     * The {@code UMA} client keeps at most two pooled connections per host, so a fetch that left its
     * connection leased would make the third fetch to the same host wait for one. The 404 leaves its
     * body unread; the 200 leaves the stream short of its end, after the JSON value.
     */
    @Test(timeOut = 60_000)
    public void releasesTheConnectionOfEveryFetch() throws Exception {
        allowInternalAddresses("true");
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/scope", exchange -> respond(exchange, 200, "{\"name\":\"Read photos\"}\n"));
        server.createContext("/missing", exchange -> respond(exchange, 404, "Not found"));
        server.start();
        ScopeDescriptionClientProvider.ApacheClient apacheClient = new ScopeDescriptionClientProvider.ApacheClient(
                ScopeDescriptionClientProvider.newApacheClient(SystemDefaultDnsResolver.INSTANCE, false));
        try {
            PendingRequestEmailTemplate fetchingTemplate = newTemplate(apacheClient.asClient());
            String base = "http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":"
                    + server.getAddress().getPort();

            for (int i = 0; i < 3; i++) {
                assertThat(fetchingTemplate.buildScopeString(singletonList(base + "/missing"), USER, REALM))
                        .isEqualTo(base + "/missing");
                assertThat(fetchingTemplate.buildScopeString(singletonList(base + "/scope"), USER, REALM))
                        .isEqualTo("Read photos");
            }
        } finally {
            apacheClient.close();
            server.stop(0);
        }
    }

    private String resolve(String scope) {
        return template.buildScopeString(singletonList(scope), USER, REALM);
    }

    private static PendingRequestEmailTemplate newTemplate(Client client) throws Exception {
        UmaProviderSettingsFactory settingsFactory = mock(UmaProviderSettingsFactory.class);
        when(settingsFactory.get(anyString())).thenReturn(mock(UmaProviderSettings.class));
        return new PendingRequestEmailTemplate(settingsFactory, client, mock(OpenAMSettings.class));
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void allowInternalAddresses(String value) {
        SystemProperties.initializeProperties(PendingRequestEmailTemplate.ALLOW_ANY_SCOPE_DESCRIPTION_ADDRESS,
                value);
    }
}
