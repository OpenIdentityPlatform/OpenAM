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
 * Copyright 2026 3A Systems LLC.
 */
package org.forgerock.openam.oauth2;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import org.forgerock.jaspi.modules.openid.resolvers.JWKOpenIdResolverImpl;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.oauth2.core.OAuth2Jwt;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.PEMDecoder;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailureFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.idm.AMIdentity;
import com.sun.net.httpserver.HttpServer;

/**
 * End-to-end coverage of the GHSA-f2cx-463q-7m2c fixes for {@code private_key_jwt} clients
 * whose public keys live behind a {@code jwks_uri}.
 *
 * <p>Unlike a mock-based test, this suite exercises the real
 * {@link JWKOpenIdResolverImpl}: a small in-process HTTP server publishes a JWKS containing
 * the public key used to sign the JWTs, so {@code BaseOpenIdResolver.verifyIssuer},
 * {@code verifyExpiration} and {@code verifySignature} all run end-to-end.
 *
 * <p>This is the regression that the previous mock-only test missed: passing the cache key
 * ({@code clientId|url}) as the resolver's bound issuer would have broken every legitimate
 * request, because {@code verifyIssuer} compares the resolver's issuer string against the
 * JWT {@code iss} claim.
 */
public class OpenAMClientRegistrationJwksUriIntegrationTest {

    private static final String CLIENT_ID = "client-A";
    private static final String OTHER_CLIENT_ID = "client-B";

    private HttpServer jwksServer;
    private String jwksUri;
    private KeyPair clientKeyPair;
    private String clientKid;

    private OpenIdResolverServiceUnusedMock resolverServiceMock;
    private OAuth2ProviderSettings providerSettings;
    private ClientAuthenticationFailureFactory failureFactory;

    @BeforeMethod
    public void setUp() throws Exception {
        ClientJwksResolverCache.resetForTest();
        clientKeyPair = generateRsaKeyPair();
        clientKid = UUID.randomUUID().toString();
        startJwksServer();
        // The injected resolverService is unused on the byJWKsURI path now, but the
        // production constructor still requires a non-null reference.
        resolverServiceMock = new OpenIdResolverServiceUnusedMock();
        providerSettings = mock(OAuth2ProviderSettings.class);
        failureFactory = mock(ClientAuthenticationFailureFactory.class);
    }

    @AfterMethod
    public void tearDown() {
        if (jwksServer != null) {
            jwksServer.stop(0);
        }
        ClientJwksResolverCache.resetForTest();
    }

    /**
     * Positive path: a well-formed assertion ({@code iss == sub == client_id}) signed with a
     * key whose public component is in the registered {@code jwks_uri} validates
     * successfully. This is the case the previous patch would have broken because it
     * would have stored {@code clientId|url} as the resolver's bound issuer.
     */
    @Test
    public void validAssertionIsAccepted() throws Exception {
        OpenAMClientRegistration reg = newRegistration(CLIENT_ID, jwksUri);
        OAuth2Jwt jwt = buildAssertion(CLIENT_ID /*iss*/, CLIENT_ID /*sub*/);

        assertThat(invokeByJwksUri(reg, jwt))
                .as("legitimate iss==sub==client_id assertion must verify against the registered jwks_uri")
                .isTrue();
        assertThat(ClientJwksResolverCache.contains(CLIENT_ID + "|" + jwksUri)).isTrue();
    }

    /**
     * The cache must be keyed by clientId, not by jwt.iss: a second registration which
     * happens to share the same JWT issuer (the attacker's hand-crafted iss) must NOT hit
     * the entry that the first registration installed.
     */
    @Test
    public void twoRegistrationsSharingJwtIssuerGetIndependentCacheEntries() throws Exception {
        // Both clients sign with the SAME key pair (so the attacker's keys would, on the
        // pre-fix code, also validate the victim's assertions), but each registration
        // points at its own jwks_uri.
        OpenAMClientRegistration clientA = newRegistration(CLIENT_ID, jwksUri);
        OpenAMClientRegistration clientB = newRegistration(OTHER_CLIENT_ID, jwksUri);

        // Both legitimately authenticate as themselves.
        assertThat(invokeByJwksUri(clientA, buildAssertion(CLIENT_ID, CLIENT_ID))).isTrue();
        assertThat(invokeByJwksUri(clientB, buildAssertion(OTHER_CLIENT_ID, OTHER_CLIENT_ID))).isTrue();

        assertThat(ClientJwksResolverCache.size())
                .as("each registration owns its own cache entry")
                .isEqualTo(2);
        assertThat(ClientJwksResolverCache.contains(CLIENT_ID + "|" + jwksUri)).isTrue();
        assertThat(ClientJwksResolverCache.contains(OTHER_CLIENT_ID + "|" + jwksUri)).isTrue();
    }

    /**
     * Defence-in-depth check at the {@code byJWKsURI} layer: even if the caller bypassed
     * the {@code iss == sub} guard upstream, a JWT whose {@code iss} does not equal the
     * resolver's bound issuer (= the registration's clientId after this fix) is rejected
     * by {@code BaseOpenIdResolver.verifyIssuer}.
     */
    @Test
    public void assertionWithForeignIssuerIsRejected() throws Exception {
        OpenAMClientRegistration reg = newRegistration(CLIENT_ID, jwksUri);
        OAuth2Jwt jwt = buildAssertion("https://attacker.example/issuer" /*iss*/, CLIENT_ID /*sub*/);

        assertThat(invokeByJwksUri(reg, jwt))
                .as("verifyIssuer must reject iss != bound issuer")
                .isFalse();
    }

    // --- helpers ---------------------------------------------------------------------------

    private OpenAMClientRegistration newRegistration(String clientId, String jwksUri) throws Exception {
        AMIdentity id = mock(AMIdentity.class);
        given(id.getName()).willReturn(clientId);
        given(id.getAttribute(OAuth2Constants.OAuth2Client.JWKS_URI)).willReturn(singleton(jwksUri));
        return new OpenAMClientRegistration(id, new PEMDecoder(), resolverServiceMock,
                providerSettings, failureFactory);
    }

    private OAuth2Jwt buildAssertion(String iss, String sub) {
        JwtClaimsSet claims = new JwtBuilderFactory().claims()
                .iss(iss)
                .sub(sub)
                .aud(Arrays.asList("https://am.example.com/oauth2/access_token"))
                .exp(new Date(System.currentTimeMillis() + 60_000L))
                .iat(new Date())
                .build();
        SigningHandler signer = new SigningManager()
                .newRsaSigningHandler((RSAPrivateKey) clientKeyPair.getPrivate());
        String compact = new JwtBuilderFactory()
                .jws(signer)
                .headers().alg(JwsAlgorithm.RS256).kid(clientKid).done()
                .claims(claims)
                .build();
        return OAuth2Jwt.create(compact);
    }

    private static boolean invokeByJwksUri(OpenAMClientRegistration reg, OAuth2Jwt jwt) throws Exception {
        Method m = OpenAMClientRegistration.class.getDeclaredMethod("byJWKsURI", OAuth2Jwt.class);
        m.setAccessible(true);
        try {
            return (Boolean) m.invoke(reg, jwt);
        } catch (java.lang.reflect.InvocationTargetException ite) {
            throw (Exception) ite.getCause();
        }
    }

    private void startJwksServer() throws IOException {
        jwksServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        final String body = buildJwksJson((RSAPublicKey) clientKeyPair.getPublic(), clientKid);
        jwksServer.createContext("/jwks", exchange -> {
            byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        jwksServer.start();
        int port = jwksServer.getAddress().getPort();
        jwksUri = "http://127.0.0.1:" + port + "/jwks";
    }

    private static String buildJwksJson(RSAPublicKey pk, String kid) {
        // Build the JWK by hand — RsaJWK's constructor signatures vary between releases
        // and we only need an RSA public key entry.
        String n = base64UrlUnsigned(pk.getModulus());
        String e = base64UrlUnsigned(pk.getPublicExponent());
        return "{\"keys\":[{"
                + "\"kty\":\"RSA\","
                + "\"use\":\"sig\","
                + "\"alg\":\"RS256\","
                + "\"kid\":\"" + kid + "\","
                + "\"n\":\"" + n + "\","
                + "\"e\":\"" + e + "\""
                + "}]}";
    }

    private static String base64UrlUnsigned(java.math.BigInteger bi) {
        byte[] full = bi.toByteArray();
        // Strip the leading 0x00 sign byte if present.
        if (full.length > 1 && full[0] == 0) {
            byte[] trimmed = new byte[full.length - 1];
            System.arraycopy(full, 1, trimmed, 0, trimmed.length);
            full = trimmed;
        }
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(full);
    }

    private static KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        return gen.generateKeyPair();
    }

    /** Stand-in for the (now-unused) shared {@code OpenIdResolverService} dependency. */
    private static class OpenIdResolverServiceUnusedMock
            implements org.forgerock.jaspi.modules.openid.resolvers.service.OpenIdResolverService {
        @Override public org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver
                getResolverForIssuer(String issuer) { return null; }
        @Override public boolean configureResolverWithKey(String i, String a, String f, String t, String p) {
            return false;
        }
        @Override public boolean configureResolverWithSecret(String i, String s) { return false; }
        @Override public boolean configureResolverWithJWK(String i, java.net.URL u) { return false; }
        @Override public boolean configureResolverWithWellKnownOpenIdConfiguration(java.net.URL u) {
            return false;
        }
    }
}



