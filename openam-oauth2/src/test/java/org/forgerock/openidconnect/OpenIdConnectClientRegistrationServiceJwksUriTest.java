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
package org.forgerock.openidconnect;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.AccessTokenVerifier;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.openam.oauth2.validation.JwksUriValidator;
import org.forgerock.openam.oauth2.validation.OpenIDConnectURLValidator;
import org.forgerock.openam.oauth2.validation.SsrfUrlValidator;
import org.forgerock.openidconnect.exceptions.InvalidClientMetadata;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.am.util.SystemProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Covers the {@code jwks_uri} SSRF guard at dynamic client registration
 * (GHSA-g7cv-hh35-cc7c): a URL that the token endpoint would later fetch must be refused
 * before it is stored, not only when it is fetched.
 *
 * <p>{@code SsrfSafeJwksHttpClientTest} covers the fetch-time half of the same guard, and
 * {@code JwksUriValidatorTest} the decision itself; this class pins the wiring between them —
 * that {@code createRegistration} consults the validator at all, and that the escape-hatch
 * property reaches it.
 */
public class OpenIdConnectClientRegistrationServiceJwksUriTest {

    private static final String DEPLOYMENT_URL = "https://am.example.com/openam";
    private static final String INTERNAL_JWKS_URI = "http://127.0.0.1:65535/jwks";
    private static final String REDIRECT_URI = "https://rp.example/callback";

    private OpenIdConnectClientRegistrationService service;
    private ClientDAO clientDAO;
    private OAuth2Request request;
    private String previousAllowAnyAddress;
    private String previousAllowAnySectorIdentifier;
    private HttpServer sectorIdentifierServer;
    private String sectorIdentifierBaseUri;

    @BeforeMethod
    public void setUp() throws Exception {
        previousAllowAnyAddress = SystemProperties.get(JwksUriValidator.ALLOW_ANY_ADDRESS_PROPERTY);
        previousAllowAnySectorIdentifier =
                SystemProperties.get(SsrfUrlValidator.ALLOW_ANY_URL_PROPERTY);
        allowAnyAddress("false");
        startSectorIdentifierServer();

        clientDAO = mock(ClientDAO.class);
        request = mock(OAuth2Request.class);

        final OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);
        given(providerSettings.isOpenDynamicClientRegistrationAllowed()).willReturn(true);
        given(providerSettings.getSupportedScopes()).willReturn(singleton("openid"));
        given(providerSettings.getDefaultScopes()).willReturn(singleton("openid"));

        final OAuth2ProviderSettingsFactory providerSettingsFactory =
                mock(OAuth2ProviderSettingsFactory.class);
        given(providerSettingsFactory.get(request)).willReturn(providerSettings);

        service = new OpenIdConnectClientRegistrationService(clientDAO, providerSettingsFactory,
                mock(AccessTokenVerifier.class), mock(TokenStore.class),
                OpenIDConnectURLValidator.getInstance(), SsrfUrlValidator.getInstance(),
                JwksUriValidator.getInstance());
    }

    @AfterMethod
    public void tearDown() {
        if (sectorIdentifierServer != null) {
            sectorIdentifierServer.stop(0);
        }
        // SystemProperties is a process-wide holder; there is no removal API, so a property that
        // was absent is handed back as "false" — the documented default of both escape hatches.
        allowAnyAddress(previousAllowAnyAddress == null ? "false" : previousAllowAnyAddress);
        allowAnySectorIdentifierUrl(previousAllowAnySectorIdentifier == null
                ? "false"
                : previousAllowAnySectorIdentifier);
    }

    /**
     * The heart of the advisory: {@code file:///etc/passwd} is a well-formed URL, and the fetch
     * that the token endpoint later performs goes through {@code URLConnection}, which reads it
     * off the server's own disk.
     */
    @Test
    public void rejectsNonHttpJwksUri() {
        givenRegistrationRequestFor("file:///etc/passwd");

        assertThatThrownBy(this::register)
                .isInstanceOf(InvalidClientMetadata.class)
                .hasMessageContaining("Invalid jwks_uri requested.");
    }

    @Test
    public void rejectsJwksUriOnBlockedAddress() {
        givenRegistrationRequestFor(INTERNAL_JWKS_URI);

        assertThatThrownBy(this::register)
                .isInstanceOf(InvalidClientMetadata.class)
                .hasMessageContaining("Invalid jwks_uri requested.");
    }

    /**
     * The SSRF check is additional to the well-formedness check that was already there, and must
     * not swallow its more specific message.
     */
    @Test
    public void rejectsMalformedJwksUriAsMalformed() {
        givenRegistrationRequestFor("this is not a url");

        assertThatThrownBy(this::register)
                .isInstanceOf(InvalidClientMetadata.class)
                .hasMessageContaining("jwks_uri must be a valid URL.");
    }

    /**
     * The escape hatch has to reach registration as well as the fetch, or a deployment whose
     * relying parties publish their JWK Sets internally could no longer register them.
     */
    @Test
    public void allowAnyAddressPropertyPermitsInternalJwksUri() throws Exception {
        allowAnyAddress("true");
        givenRegistrationRequestFor(INTERNAL_JWKS_URI);

        final JsonValue response = register();

        assertThat(response.get("jwks_uri").asString()).isEqualTo(INTERNAL_JWKS_URI);
        final ArgumentCaptor<Client> stored = ArgumentCaptor.forClass(Client.class);
        verify(clientDAO).create(stored.capture(), org.mockito.ArgumentMatchers.eq(request));
        assertThat(stored.getValue().getJwksUri()).isEqualTo(INTERNAL_JWKS_URI);
    }

    /**
     * The {@code sector_identifier_uri} document is fetched from an endpoint the registering
     * client chooses, so its size has to be the server's decision, not the endpoint's. The
     * listing served here is well-formed and does contain the client's {@code redirect_uris} —
     * only its size is wrong — so this fails for no reason other than the ceiling.
     */
    @Test
    public void rejectsOversizedSectorIdentifierDocument() {
        allowAnySectorIdentifierUrl("true");
        givenRegistrationRequestWithSectorIdentifier("/oversized");

        assertThatThrownBy(this::register)
                .isInstanceOf(InvalidClientMetadata.class)
                .hasMessageContaining("Invalid sector_identifier_uri requested.");
    }

    /** Control for {@link #rejectsOversizedSectorIdentifierDocument()}: same listing, sane size. */
    @Test
    public void acceptsSectorIdentifierDocumentWithinTheCeiling() throws Exception {
        allowAnySectorIdentifierUrl("true");
        givenRegistrationRequestWithSectorIdentifier("/listing");

        assertThat(register().get("redirect_uris").asList(String.class)).contains(REDIRECT_URI);
    }

    // --- helpers ---------------------------------------------------------------------------

    private void givenRegistrationRequestWithSectorIdentifier(String path) {
        final Map<String, Object> body = new HashMap<>();
        body.put("redirect_uris", singletonList(REDIRECT_URI));
        body.put("sector_identifier_uri", sectorIdentifierBaseUri + path);
        given(request.getBody()).willReturn(new JsonValue(body));
    }

    private void startSectorIdentifierServer() throws IOException {
        sectorIdentifierServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        sectorIdentifierServer.createContext("/listing",
                exchange -> respond(exchange, redirectUriListing(0)));
        // Comfortably past the service's 512K ceiling.
        sectorIdentifierServer.createContext("/oversized",
                exchange -> respond(exchange, redirectUriListing(20_000)));
        sectorIdentifierServer.start();
        sectorIdentifierBaseUri = "http://127.0.0.1:" + sectorIdentifierServer.getAddress().getPort();
    }

    /** A valid listing of the client's redirect URIs, padded with {@code filler} further entries. */
    private static String redirectUriListing(int filler) {
        final StringBuilder json = new StringBuilder("[\"").append(REDIRECT_URI).append('"');
        for (int i = 0; i < filler; i++) {
            json.append(",\"https://rp.example/padding/").append(i).append('"');
        }
        return json.append(']').toString();
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void givenRegistrationRequestFor(String jwksUri) {
        final Map<String, Object> body = new HashMap<>();
        body.put("jwks_uri", jwksUri);
        given(request.getBody()).willReturn(new JsonValue(body));
    }

    private JsonValue register() throws Exception {
        return service.createRegistration("registration-access-token", DEPLOYMENT_URL, request);
    }

    private static void allowAnyAddress(String value) {
        SystemProperties.initializeProperties(JwksUriValidator.ALLOW_ANY_ADDRESS_PROPERTY, value);
    }

    /**
     * The {@code sector_identifier_uri} guard requires https and a public address, neither of
     * which an in-process test server can offer; its escape hatch is the only way to reach the
     * fetch itself, which is what the size ceiling lives on.
     */
    private static void allowAnySectorIdentifierUrl(String value) {
        SystemProperties.initializeProperties(SsrfUrlValidator.ALLOW_ANY_URL_PROPERTY, value);
    }
}
